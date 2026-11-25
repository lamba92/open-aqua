@file:OptIn(NativeRuntimeApi::class)

package io.github.lamba92.openaqua.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.di.LocalDI
import io.github.lamba92.openaqua.app.i18n.AppLocaleProvider
import io.github.lamba92.openaqua.app.instance.ForegroundRequestService
import io.github.lamba92.openaqua.app.model.locale
import io.github.lamba92.openaqua.app.model.toPosition
import io.github.lamba92.openaqua.app.model.toSize
import io.github.lamba92.openaqua.app.model.toStoredData
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.theme.OpenAquaThemedApp
import io.github.lamba92.openaqua.app.tray.OpenAquaTray
import io.github.lamba92.openaqua.app.window.OpenAquaWindow
import kotlinx.coroutines.delay
import org.kodein.di.DI
import org.kodein.di.instance
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi
import kotlin.time.Duration.Companion.seconds

@Composable
fun ApplicationScope.OpenAquaApp(
    di: DI,
    startMinimized: Boolean = false,
) {
    val appDataRepository by di.instance<AppDataRepository>()
    val scopeProvider by di.instance<CoroutineScopeProvider>()
    val foregroundRequestService by di.instance<ForegroundRequestService>()
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

    LaunchedEffect(foregroundRequestService) {
        foregroundRequestService.focusRequests.collect {
            mainLogger.i { "Focus requested by secondary instance, restoring window..." }
            windowState.isMinimized = false
        }
    }

    LaunchedEffect(windowState.isMinimized) {
        delay(2.seconds)
        if (windowState.isMinimized) {
            mainLogger.i { "Window minimized to tray, triggering garbage collection..." }
            GC.collect()
        }
    }

    if (isTrayEnabled) {
        OpenAquaTray(
            enabled = isTrayEnabled,
            onOpen = { windowState.isMinimized = false },
            onExit = {
                mainLogger.i { "Application exiting via tray menu..." }
                appDataRepository.saveWindowStateSync(windowState.toStoredData(storedData.windowState))
                scopeProvider.cancelAll()
                foregroundRequestService.close()
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
                        foregroundRequestService.close()
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
