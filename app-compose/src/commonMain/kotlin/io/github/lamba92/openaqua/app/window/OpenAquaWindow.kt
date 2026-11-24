package io.github.lamba92.openaqua.app.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.window.WindowState
import com.compose.sdl.AppWindowIcon
import com.compose.sdl.Window
import io.github.lamba92.openaqua.app.ApplicationScope
import io.github.lamba92.openaqua.app.model.AppStoredData
import io.github.lamba92.openaqua.app.model.toStoredData
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import org.kodein.di.DI
import org.kodein.di.instance
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@Composable
fun ApplicationScope.OpenAquaWindow(
    state: WindowState,
    onCloseRequest: () -> Unit,
    storedData: AppStoredData,
    di: DI,
    content: @Composable () -> Unit,
) {
    val appDataRepository by di.instance<AppDataRepository>()

    LaunchedEffect(state) {
        snapshotFlow { state.toStoredData(storedData.windowState) }
            .distinctUntilChanged()
            .debounce(500.milliseconds)
            .collect { newWindowState ->
                appDataRepository.update { it.copy(windowState = newWindowState) }
            }
    }

    Window(
        onCloseRequest = onCloseRequest,
        state = state,
        title = "OpenAqua",
        icon = AppWindowIcon(light = listOf("icon/icon-512.rgba")),
    ) {
        content()
    }
}
