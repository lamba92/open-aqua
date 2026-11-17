package io.github.lamba92.openaqua.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.di.LocalDI
import io.github.lamba92.openaqua.app.i18n.AppLocaleProvider
import io.github.lamba92.openaqua.app.model.locale
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
    var isWindowVisible by remember { mutableStateOf(!shouldStartMinimized) }

    OpenAquaTray(
        enabled = isTrayEnabled,
        onOpen = { isWindowVisible = true },
        onExit = {
            mainLogger.i { "Application exiting via tray menu..." }
            scopeProvider.cancelAll()
            exitApplication()
        },
    )

    AppLocaleProvider(storedData.language.locale) {
        OpenAquaWindow(
            visible = isWindowVisible,
            onCloseRequest = {
                if (isTrayEnabled) {
                    mainLogger.i { "Window close requested, hiding to tray..." }
                    isWindowVisible = false
                } else {
                    mainLogger.i { "Application closing, cancelling active scopes..." }
                    scopeProvider.cancelAll()
                    exitApplication()
                }
            },
            storedData = storedData,
            di = di,
            onRestoreWindow = { isWindowVisible = true },
        ) {
            CompositionLocalProvider(LocalDI provides di) {
                OpenAquaThemedApp(storedData)
            }
        }
    }
}
