package io.github.lamba92.openaqua.app.window

import androidx.compose.runtime.Composable
import com.compose.sdl.AppWindowIcon
import com.compose.sdl.Window
import io.github.lamba92.openaqua.app.ApplicationScope
import io.github.lamba92.openaqua.app.model.AppStoredData
import org.kodein.di.DI

@Composable
actual fun ApplicationScope.OpenAquaWindow(
    visible: Boolean,
    onCloseRequest: () -> Unit,
    storedData: AppStoredData,
    di: DI,
    onRestoreWindow: () -> Unit,
    content: @Composable () -> Unit,
) {
    Window(
        onCloseRequest = onCloseRequest,
        title = "OpenAqua (Desktop Native - SDL3/Skia)",
        width = 1100,
        height = 760,
        icon = AppWindowIcon(light = listOf("icon/icon-512.rgba")),
    ) {
        content()
    }
}
