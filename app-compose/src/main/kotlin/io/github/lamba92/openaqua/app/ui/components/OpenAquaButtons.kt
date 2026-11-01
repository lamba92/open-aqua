package io.github.lamba92.openaqua.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme

@Composable
fun OpenAquaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null,
    isLoading: Boolean = false,
    loadingText: String? = null,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    iconTint: Color = Color.Unspecified,
) {
    val colors = OpenAquaTheme.colors
    val resolvedContainerColor = if (containerColor.isSpecified) containerColor else colors.primaryBright
    val resolvedContentColor = if (contentColor.isSpecified) contentColor else colors.background
    val resolvedIconTint = if (iconTint.isSpecified) iconTint else resolvedContentColor

    Button(
        onClick = { if (!isLoading) onClick() },
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(10.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = resolvedContainerColor,
                contentColor = resolvedContentColor,
                disabledContainerColor = if (isLoading) colors.surfaceContainer else colors.surfaceContainer.copy(alpha = 0.5f),
                disabledContentColor = if (isLoading) colors.textPrimary else colors.textMuted,
            ),
        modifier =
            modifier.onPreviewKeyEvent { event ->
                if (
                    enabled &&
                    !isLoading &&
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            },
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = resolvedContentColor,
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = resolvedIconTint,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = if (isLoading && loadingText != null) loadingText else text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun OpenAquaOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null,
    borderColor: Color = Color.Unspecified,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    iconTint: Color = Color.Unspecified,
) {
    val colors = OpenAquaTheme.colors
    val resolvedBorderColor = if (borderColor.isSpecified) borderColor else colors.borderOutline
    val resolvedContainerColor = if (containerColor.isSpecified) containerColor else colors.surfaceHigh
    val resolvedContentColor = if (contentColor.isSpecified) contentColor else colors.textPrimary
    val resolvedIconTint = if (iconTint.isSpecified) iconTint else colors.primaryBright

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (enabled) resolvedBorderColor else colors.borderOutline.copy(alpha = 0.4f)),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = resolvedContainerColor,
                contentColor = resolvedContentColor,
                disabledContainerColor = colors.surfaceLow,
                disabledContentColor = colors.textMuted,
            ),
        modifier =
            modifier.onPreviewKeyEvent { event ->
                if (
                    enabled &&
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            },
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = if (enabled) resolvedIconTint else colors.textMuted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun OpenAquaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null,
    contentColor: Color = Color.Unspecified,
    iconTint: Color = Color.Unspecified,
) {
    val colors = OpenAquaTheme.colors
    val resolvedContentColor = if (contentColor.isSpecified) contentColor else colors.textSecondary
    val resolvedIconTint = if (iconTint.isSpecified) iconTint else colors.primaryBright

    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors =
            ButtonDefaults.textButtonColors(
                contentColor = resolvedContentColor,
                disabledContentColor = colors.textMuted.copy(alpha = 0.5f),
            ),
        modifier =
            modifier.onPreviewKeyEvent { event ->
                if (
                    enabled &&
                    event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else {
                    false
                }
            },
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = if (enabled) resolvedIconTint else colors.textMuted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
