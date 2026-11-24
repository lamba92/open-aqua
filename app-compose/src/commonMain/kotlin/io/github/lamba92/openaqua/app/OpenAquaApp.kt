package io.github.lamba92.openaqua.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.di.LocalDI
import io.github.lamba92.openaqua.app.i18n.AppLocaleProvider
import io.github.lamba92.openaqua.app.model.locale
import io.github.lamba92.openaqua.app.model.toPosition
import io.github.lamba92.openaqua.app.model.toSize
import io.github.lamba92.openaqua.app.model.toStoredData
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.theme.OpenAquaThemedApp
import io.github.lamba92.openaqua.app.tray.OpenAquaTray
import io.github.lamba92.openaqua.app.window.OpenAquaWindow
import org.kodein.di.DI
import org.kodein.di.instance

@Composable
fun ApplicationScope.OpenAquaApp(
    di: DI,
    startMinimized: Boolean = false,
) {
    val appDataRepository by di.instance<AppDataRepository>()
    val scopeProvider by di.instance<CoroutineScopeProvider>()
    val mainLogger by di.instance<Logger>(tag = "Main")

    val appDataState by appDataRepository.state.collectAsState()
    val storedData =
        when (val state = appDataState) {
            AppDataRepository.State.Loading -> return
            is AppDataRepository.State.Ready -> state.data
        }

    val isTrayEnabled = storedData.minimizeToTrayOnClose || startMinimized
    val shouldStartMinimized = isTrayEnabled && (startMinimized || storedData.startMinimized)
    val windowState =
        rememberWindowState(
            placement = storedData.windowState.placement,
            isMinimized = shouldStartMinimized,
            position = storedData.windowState.toPosition(),
            size = storedData.windowState.toSize(),
        )
    val isWindowVisible = !windowState.isMinimized

    if (isTrayEnabled) {
        OpenAquaTray(
            enabled = isTrayEnabled,
            onOpen = { windowState.isMinimized = false },
            onExit = {
                mainLogger.i { "Application exiting via tray menu..." }
                appDataRepository.saveWindowStateSync(windowState.toStoredData(storedData.windowState))
                scopeProvider.cancelAll()
                exitApplication()
            },
        )
    }

    if (isWindowVisible) {
        AppLocaleProvider(storedData.language.locale) {
            OpenAquaWindow(
                state = windowState,
                onCloseRequest = {
                    appDataRepository.saveWindowStateSync(windowState.toStoredData(storedData.windowState))
                    if (isTrayEnabled) {
                        mainLogger.i { "Window close requested, hiding to tray..." }
                        windowState.isMinimized = true
                    } else {
                        mainLogger.i { "Application closing, cancelling active scopes..." }
                        scopeProvider.cancelAll()
                        exitApplication()
                    }
                },
                storedData = storedData,
                di = di,
            ) {
                CompositionLocalProvider(LocalDI provides di) {
                    OpenAquaThemedApp(storedData)
                }
            }
        }
    }
}
