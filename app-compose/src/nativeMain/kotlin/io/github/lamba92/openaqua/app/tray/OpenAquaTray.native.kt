package io.github.lamba92.openaqua.app.tray

import androidx.compose.runtime.Composable
import io.github.lamba92.openaqua.app.ApplicationScope

@Composable
actual fun ApplicationScope.OpenAquaTray(
    enabled: Boolean,
    onOpen: () -> Unit,
    onExit: () -> Unit,
) {
    // No-op for native desktop for now
}
