package io.github.lamba92.openaqua.app.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.compose.sdl.AppWindowIcon
import com.compose.sdl.Window
import io.github.lamba92.openaqua.app.ApplicationScope
import io.github.lamba92.openaqua.app.model.AppStoredData
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import org.kodein.di.DI
import org.kodein.di.instance
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

    val initialWidth =
        if (storedData.windowState.widthDp > 0f) {
            storedData.windowState.widthDp.toInt()
        } else {
            1380
        }
    val initialHeight =
        if (storedData.windowState.heightDp > 0f) {
            storedData.windowState.heightDp.toInt()
        } else {
            860
        }

    Window(
        onCloseRequest = onCloseRequest,
        title = "OpenAqua",
        width = initialWidth,
        height = initialHeight,
        visible = visible,
        icon = AppWindowIcon(light = listOf("icon/icon-512.rgba")),
    ) {
        val currentWindow = this.window
        LaunchedEffect(currentWindow) {
            snapshotFlow { currentWindow.width to currentWindow.height }
                .distinctUntilChanged()
                .debounce(500.milliseconds)
                .collect { (w, h) ->
                    if (w > 0 && h > 0) {
                        appDataRepository.update {
                            it.copy(
                                windowState =
                                    it.windowState.copy(
                                        widthDp = w.toFloat(),
                                        heightDp = h.toFloat(),
                                    ),
                            )
                        }
                    }
                }
        }
        content()
    }
}
