package io.github.lamba92.openaqua.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.svg.SvgDecoder
import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.di.LocalDI
import io.github.lamba92.openaqua.app.di.createOpenAquaDI
import io.github.lamba92.openaqua.app.i18n.AppLocaleProvider
import io.github.lamba92.openaqua.app.instance.ForegroundRequestService
import io.github.lamba92.openaqua.app.instance.acquireOrNotify
import io.github.lamba92.openaqua.app.model.locale
import io.github.lamba92.openaqua.app.model.toPosition
import io.github.lamba92.openaqua.app.model.toSize
import io.github.lamba92.openaqua.app.model.toStoredData
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.theme.OpenAquaThemedApp
import io.github.lamba92.openaqua.app.util.restoreAndFocus
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.runBlocking
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.ic_app_icon
import open_aqua.app_compose.generated.resources.window_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.kodein.di.DI
import org.kodein.di.instance
import java.awt.Taskbar
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.milliseconds

fun main() {
    startOpenAquaApplication(createOpenAquaDI())
}

@OptIn(FlowPreview::class)
fun startOpenAquaApplication(di: DI = createOpenAquaDI()) {
    acquireOrNotify()
    configureTaskbarIcon()

    val appDataRepository by di.instance<AppDataRepository>()
    val scopeProvider by di.instance<CoroutineScopeProvider>()
    val foregroundRequestService by di.instance<ForegroundRequestService>()
    val mainLogger by di.instance<Logger>(tag = "Main")

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
                    scopeProvider.cancelAll()
                    foregroundRequestService.close()
                    exitApplication()
                },
                state = windowState,
                title = stringResource(Res.string.window_title),
                icon = painterResource(Res.drawable.ic_app_icon),
            ) {
                LaunchedEffect(Unit) {
                    foregroundRequestService.focusRequests.collect {
                        mainLogger.i { "Focus requested by secondary instance, restoring window..." }
                        window.restoreAndFocus(windowState)
                    }
                }

                CompositionLocalProvider(LocalDI provides di) {
                    OpenAquaThemedApp(storedData)
                }
            }
        }
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
