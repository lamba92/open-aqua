package io.github.lamba92.openaqua.app.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import io.github.lamba92.openaqua.app.ApplicationScope
import io.github.lamba92.openaqua.app.instance.ForegroundRequestService
import io.github.lamba92.openaqua.app.model.AppStoredData
import io.github.lamba92.openaqua.app.model.toPosition
import io.github.lamba92.openaqua.app.model.toSize
import io.github.lamba92.openaqua.app.model.toStoredData
import io.github.lamba92.openaqua.app.model.toWindowPlacement
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.util.restoreAndFocus
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.ic_app_icon
import open_aqua.app_compose.generated.resources.window_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.kodein.di.DI
import org.kodein.di.instance
import org.kodein.di.instanceOrNull
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@Composable
actual fun ApplicationScope.OpenAquaWindow(
    visible: Boolean,
    onCloseRequest: () -> Unit,
    storedData: AppStoredData,
    di: DI,
    onRestoreWindow: () -> Unit,
    content: @Composable () -> Unit,
) {
    val appDataRepository by di.instance<AppDataRepository>()
    val foregroundRequestService by di.instanceOrNull<ForegroundRequestService>()
    val mainLogger by di.instance<Logger>(tag = "Main")

    val windowState =
        rememberWindowState(
            placement = storedData.windowState.placement.toWindowPlacement(),
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

    foregroundRequestService?.let { service ->
        LaunchedEffect(service) {
            service.focusRequests.collect {
                mainLogger.i { "Focus requested by secondary instance, restoring window..." }
                windowState.isMinimized = false
                onRestoreWindow()
            }
        }
        DisposableEffect(service) {
            onDispose {
                service.close()
            }
        }
    }

    Window(
        visible = visible,
        onCloseRequest = {
            appDataRepository.saveWindowStateSync(windowState.toStoredData(storedData.windowState))
            onCloseRequest()
        },
        state = windowState,
        title = stringResource(Res.string.window_title),
        icon = painterResource(Res.drawable.ic_app_icon),
    ) {
        LaunchedEffect(visible) {
            if (visible) {
                window.restoreAndFocus(windowState)
            }
        }
        content()
    }
}
