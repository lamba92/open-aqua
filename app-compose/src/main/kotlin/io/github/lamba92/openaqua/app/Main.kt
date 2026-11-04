package io.github.lamba92.openaqua.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.svg.SvgDecoder
import io.github.lamba92.openaqua.app.coroutine.DefaultCoroutineScopeProvider
import io.github.lamba92.openaqua.app.di.LocalDI
import io.github.lamba92.openaqua.app.di.createOpenAquaDI
import io.github.lamba92.openaqua.app.di.rememberInstance
import io.github.lamba92.openaqua.app.feature.about.AboutScreen
import io.github.lamba92.openaqua.app.feature.device.DeviceScreen
import io.github.lamba92.openaqua.app.feature.host.HostScreen
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavEvent
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavState
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavViewModel
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavigationRailScaffold
import io.github.lamba92.openaqua.app.feature.settings.SettingsScreen
import io.github.lamba92.openaqua.app.i18n.AppLocaleProvider
import io.github.lamba92.openaqua.app.logging.AppLogService
import io.github.lamba92.openaqua.app.model.AppStoredData
import io.github.lamba92.openaqua.app.model.locale
import io.github.lamba92.openaqua.app.model.toPosition
import io.github.lamba92.openaqua.app.model.toSize
import io.github.lamba92.openaqua.app.model.toStoredData
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.theme.AppMetrics
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.util.EngineExtractor
import io.github.lamba92.openaqua.theme.ThemePalette
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.runBlocking
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.ic_app_icon
import open_aqua.app_compose.generated.resources.select_a_device_subtitle
import open_aqua.app_compose.generated.resources.select_a_device_title
import open_aqua.app_compose.generated.resources.window_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.kodein.di.instance
import java.awt.Taskbar
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
fun main() {
    val baseAppDataDir = AppDataRepository.defaultAppDataPath().parent ?: Path(".")
    val logsDir = baseAppDataDir.resolve("logs")
    val rootScopeProvider = DefaultCoroutineScopeProvider()
    val appLogService = AppLogService(logsDirectory = logsDir, scopeProvider = rootScopeProvider)
    val appLogger =
        Logger(
            config = loggerConfigInit(CommonWriter(), appLogService.logWriter),
            tag = "OpenAqua",
        )
    val mainLogger = appLogger.withTag("Main")
    mainLogger.i { "Starting OpenAqua..." }
    val engines = EngineExtractor.extract()
    mainLogger.i { "Extracted engine binaries: device='${engines.deviceEngine.fileName}', host='${engines.hostEngine.fileName}'" }
    val appScopeProvider = DefaultCoroutineScopeProvider(baseLogger = appLogger)
    val di =
        createOpenAquaDI(
            deviceEnginePath = engines.deviceEngine,
            hostEnginePath = engines.hostEngine,
            appLogService = appLogService,
            scopeProvider = appScopeProvider,
            logger = appLogger,
        )
    val appDataRepository by di.instance<AppDataRepository>()

    configureTaskbarIcon()

    application {
        setSingletonImageLoaderFactory { context ->
            ImageLoader
                .Builder(context)
                .components {
                    add(SvgDecoder.Factory())
                }.build()
        }

        val appDataState by appDataRepository.state.collectAsState()
        val storedData =
            when (val state = appDataState) {
                AppDataRepository.State.Loading -> return@application
                is AppDataRepository.State.Ready -> state.data
            }

        val windowState =
            rememberWindowState(
                placement = storedData.windowState.placement,
                position = storedData.windowState.toPosition(),
                size = storedData.windowState.toSize(),
            )

        LaunchedEffect(windowState) {
            snapshotFlow { windowState.toStoredData(storedData.windowState) }
                .distinctUntilChanged()
                .debounce(500.milliseconds)
                .collect { newWindowState ->
                    appDataRepository.update { it.copy(windowState = newWindowState) }
                }
        }

        AppLocaleProvider(storedData.language.locale) {
            Window(
                onCloseRequest = {
                    mainLogger.i { "Application closing, cancelling active scopes..." }
                    appDataRepository.saveWindowStateSync(windowState.toStoredData(storedData.windowState))
                    appScopeProvider.cancelAll()
                    rootScopeProvider.cancelAll()
                    exitApplication()
                },
                state = windowState,
                title = stringResource(Res.string.window_title),
                icon = painterResource(Res.drawable.ic_app_icon),
            ) {
                CompositionLocalProvider(LocalDI provides di) {
                    OpenAquaThemedApp(storedData)
                }
            }
        }
    }
}

@Composable
private fun OpenAquaThemedApp(storedData: AppStoredData) {
    val palette =
        remember(storedData.colorScheme) {
            ThemePalette.fromColorScheme(storedData.colorScheme)
        }
    val metrics =
        remember(storedData.metricsScale) {
            AppMetrics.fromScale(storedData.metricsScale)
        }
    val fontFamily = storedData.fontFamily
    val fontSp = storedData.fontSp

    OpenAquaTheme(
        palette = palette,
        metrics = metrics,
        fontFamilyPreference = fontFamily,
        fontSp = fontSp,
        preferences = storedData,
    ) {
        OpenAquaApp()
    }
}

private fun configureTaskbarIcon() {
    runCatching {
        if (Taskbar.isTaskbarSupported()) {
            val taskbar = Taskbar.getTaskbar()
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                val iconBytes = runBlocking { Res.readBytes("drawable/ic_app_icon.png") }
                val awtImage = ImageIO.read(iconBytes.inputStream())
                if (awtImage != null) {
                    taskbar.iconImage = awtImage
                }
            }
        }
    }
}

@Composable
fun OpenAquaApp(navViewModel: DeviceNavViewModel = rememberInstance()) {
    val navState by navViewModel.state.collectAsState()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(OpenAquaTheme.colors.background),
    ) {
        DeviceNavigationRailScaffold(
            state = navState,
            onEvent = navViewModel::onEvent,
        ) {
            when (val item = navState.selectedItem) {
                null -> SelectADeviceScreen()
                DeviceNavState.SelectedItem.About ->
                    AboutScreen()

                is DeviceNavState.SelectedItem.Settings ->
                    SettingsScreen(
                        selectedTab = item.selectedTab,
                        onSelectTab = { navViewModel.onEvent(DeviceNavEvent.SelectSettingsTab(it)) },
                        viewModel = rememberInstance(),
                    )

                DeviceNavState.SelectedItem.Host ->
                    HostScreen(viewModel = rememberInstance())

                is DeviceNavState.SelectedItem.Device ->
                    DeviceScreen(
                        deviceId = item.deviceId,
                        selectedTab = item.selectedTab,
                        onSelectTab = { navViewModel.onEvent(DeviceNavEvent.SelectTab(it)) },
                    )
            }
        }
    }
}

@Composable
fun SelectADeviceScreen(modifier: Modifier = Modifier) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Box(
        modifier = modifier.fillMaxSize().padding(metrics.spacingXxl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
            modifier =
                Modifier
                    .clip(RoundedCornerShape(metrics.radiusLarge))
                    .background(colors.surfaceLow)
                    .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusLarge))
                    .padding(metrics.spacingXxl),
        ) {
            Icon(
                imageVector = Icons.Outlined.TouchApp,
                contentDescription = null,
                tint = colors.primaryBright,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.select_a_device_title),
                color = colors.textPrimary,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.select_a_device_subtitle),
                color = colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
