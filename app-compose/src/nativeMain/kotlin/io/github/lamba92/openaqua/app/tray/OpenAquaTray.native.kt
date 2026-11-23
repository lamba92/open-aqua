package io.github.lamba92.openaqua.app.tray

import androidx.compose.runtime.Composable
import com.compose.sdl.Tray
import io.github.lamba92.openaqua.app.ApplicationScope
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.ic_tray_icon
import open_aqua.app_compose.generated.resources.tray_menu_exit
import open_aqua.app_compose.generated.resources.tray_menu_open
import open_aqua.app_compose.generated.resources.window_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun ApplicationScope.OpenAquaTray(
    enabled: Boolean,
    onOpen: () -> Unit,
    onExit: () -> Unit,
) {
    if (enabled) {
        Tray(
            icon = painterResource(Res.drawable.ic_tray_icon),
            tooltip = stringResource(Res.string.window_title),
        ) {
            Item(
                text = Res.string.tray_menu_open,
                onClick = onOpen,
            )
            Divider()
            Item(
                text = Res.string.tray_menu_exit,
                onClick = onExit,
            )
        }
    }
}
