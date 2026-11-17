package io.github.lamba92.openaqua.app.feature.settings.updates

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme

@Composable
expect fun MarkdownReleaseNotes(
    content: String,
    modifier: Modifier = Modifier,
)
