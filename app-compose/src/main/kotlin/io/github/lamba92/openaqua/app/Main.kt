package io.github.lamba92.openaqua.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.svg.SvgDecoder
import io.github.lamba92.openaqua.app.coroutine.DefaultCoroutineScopeProvider
import io.github.lamba92.openaqua.app.di.LocalDI
import io.github.lamba92.openaqua.app.di.createOpenAquaDI
import io.github.lamba92.openaqua.app.di.rememberInstance
import io.github.lamba92.openaqua.app.feature.device.DeviceScreen
import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavEvent
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavState
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavViewModel
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavigationRailScaffold
import io.github.lamba92.openaqua.app.feature.settings.SettingsScreen
import io.github.lamba92.openaqua.app.model.AppStoredData
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.theme.AppMetrics
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.util.EngineExtractor
import io.github.lamba92.openaqua.theme.ThemePalette
import kotlinx.coroutines.runBlocking
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.ic_app_icon
import open_aqua.app_compose.generated.resources.no_devices_found
import open_aqua.app_compose.generated.resources.scanning_hardware
import open_aqua.app_compose.generated.resources.window_title
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Taskbar
import javax.imageio.ImageIO

fun main() {
    val enginePath = runBlocking { EngineExtractor.extract() }
    val scopeProvider = DefaultCoroutineScopeProvider()
    val di = createOpenAquaDI(enginePath, scopeProvider)

    configureTaskbarIcon()

    application {
        val windowState =
            rememberWindowState(
                size = DpSize(1380.dp, 880.dp),
            )

        setSingletonImageLoaderFactory { context ->
            ImageLoader
                .Builder(context)
                .components {
                    add(SvgDecoder.Factory())
                }.build()
        }

        Window(
            onCloseRequest = {
                scopeProvider.cancelAll()
                exitApplication()
            },
            state = windowState,
            title = stringResource(Res.string.window_title),
            icon = painterResource(Res.drawable.ic_app_icon),
        ) {
            CompositionLocalProvider(LocalDI provides di) {
                OpenAquaThemedApp()
            }
        }
    }
}

@Composable
private fun OpenAquaThemedApp(appDataRepository: AppDataRepository = rememberInstance()) {
    val appDataState by appDataRepository.state.collectAsState()
    val storedData = (appDataState as? AppDataRepository.State.Ready)?.data ?: AppStoredData()
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

@OptIn(ExperimentalResourceApi::class)
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
        when (val state = navState) {
            DeviceNavState.Loading -> LoadingIndicator()
            DeviceNavState.NoDevices -> NoDevicesDetected()
            is DeviceNavState.Ready -> DeviceReady(state, navViewModel)
        }
    }
}

@Composable
private fun DeviceReady(
    state: DeviceNavState.Ready,
    navViewModel: DeviceNavViewModel,
) {
    val selectedDevice = state.selectedDeviceState.device

    DeviceNavigationRailScaffold(
        state = state,
        onEvent = navViewModel::onEvent,
    ) {
        when {
            state.isSettingsSelected ->
                SettingsScreen(
                    viewModel = rememberInstance(),
                )
            else ->
                DeviceScreen(
                    viewModel = rememberInstance(arg = selectedDevice.id),
                    selectedTab = state.selectedTab,
                    onSelectTab = { navViewModel.onEvent(DeviceNavEvent.SelectTab(it)) },
                )
        }
    }
}

@Composable
private fun NoDevicesDetected() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = OpenAquaTheme.colors.statusWarning,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.no_devices_found),
                color = OpenAquaTheme.colors.textPrimary,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp),
        ) {
            CircularProgressIndicator(
                color = OpenAquaTheme.colors.primaryBright,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.scanning_hardware),
                color = OpenAquaTheme.colors.textSecondary,
                fontSize = 14.sp,
            )
        }
    }
}
