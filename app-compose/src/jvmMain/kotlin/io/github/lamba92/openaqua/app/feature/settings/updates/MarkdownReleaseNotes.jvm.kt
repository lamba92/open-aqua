package io.github.lamba92.openaqua.app.feature.settings.updates

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme

@Composable
actual fun MarkdownReleaseNotes(
    content: String,
    modifier: Modifier,
) {
    val colors = OpenAquaTheme.colors
    Markdown(
        content = content,
        colors =
            markdownColor(
                text = colors.textSecondary,
                codeBackground = colors.surfaceHigh,
                inlineCodeBackground = colors.surfaceHigh,
                dividerColor = colors.borderOutline,
            ),
        modifier = modifier.fillMaxWidth(),
    )
}
