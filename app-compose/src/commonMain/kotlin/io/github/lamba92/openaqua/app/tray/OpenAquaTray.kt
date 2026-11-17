package io.github.lamba92.openaqua.app.tray

import androidx.compose.runtime.Composable
import io.github.lamba92.openaqua.app.ApplicationScope

@Composable
expect fun ApplicationScope.OpenAquaTray(
    enabled: Boolean,
    onOpen: () -> Unit,
    onExit: () -> Unit,
)
