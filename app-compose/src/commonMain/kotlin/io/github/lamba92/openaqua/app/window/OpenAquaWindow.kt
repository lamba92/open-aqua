package io.github.lamba92.openaqua.app.window

import androidx.compose.runtime.Composable
import io.github.lamba92.openaqua.app.ApplicationScope
import io.github.lamba92.openaqua.app.model.AppStoredData
import org.kodein.di.DI

@Composable
expect fun ApplicationScope.OpenAquaWindow(
    visible: Boolean,
    onCloseRequest: () -> Unit,
    storedData: AppStoredData,
    di: DI,
    onRestoreWindow: () -> Unit = {},
    content: @Composable () -> Unit,
)
