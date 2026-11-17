package io.github.lamba92.openaqua.app.feature.settings.updates

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme

@Composable
actual fun MarkdownReleaseNotes(
    content: String,
    modifier: Modifier,
) {
    Text(
        text = content,
        color = OpenAquaTheme.colors.textSecondary,
        modifier = modifier.fillMaxWidth(),
    )
}
