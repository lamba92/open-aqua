package io.github.lamba92.openaqua.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.lamba92.openaqua.app.theme.BorderOutline
import io.github.lamba92.openaqua.app.theme.BrandPureWhite
import io.github.lamba92.openaqua.app.theme.CyanBright
import io.github.lamba92.openaqua.app.theme.ObsidianBg
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.theme.StatusCritical
import io.github.lamba92.openaqua.app.theme.SurfaceContainerLow
import io.github.lamba92.openaqua.app.theme.TextMuted
import io.github.lamba92.openaqua.app.theme.TextPrimary
import io.github.lamba92.openaqua.app.theme.TextSecondary

@Composable
fun ConfirmationDialog(
    title: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: Painter? = null,
    iconTint: Color = StatusCritical,
    confirmContainerColor: Color = StatusCritical,
    confirmContentColor: Color = BrandPureWhite,
    dismissContainerColor: Color = CyanBright,
    dismissContentColor: Color = ObsidianBg,
    isLoading: Boolean = false,
    loadingText: String? = null,
    enabled: Boolean = true,
    width: Dp = 440.dp,
    content: (@Composable () -> Unit)? = null,
) {
    val dismissFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!isLoading) {
            dismissFocusRequester.requestFocus()
        }
    }

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties =
            DialogProperties(
                dismissOnBackPress = !isLoading,
                dismissOnClickOutside = !isLoading,
            ),
    ) {
        Column(
            modifier =
                modifier
                    .width(width)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, BorderOutline, RoundedCornerShape(16.dp))
                    .padding(24.dp)
                    .onPreviewKeyEvent { event ->
                        if (!isLoading && event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                            onDismiss()
                            true
                        } else {
                            false
                        }
                    },
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (icon != null) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (message != null) {
                Text(
                    text = message,
                    color = TextSecondary,
                    fontSize = 15.sp,
                )
            }

            if (content != null) {
                content()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OpenAquaButton(
                    text = dismissText,
                    onClick = onDismiss,
                    enabled = enabled && !isLoading,
                    containerColor = if (isLoading) OpenAquaTheme.colors.surfaceLowest else dismissContainerColor,
                    contentColor = if (isLoading) TextMuted else dismissContentColor,
                    modifier = Modifier.focusRequester(dismissFocusRequester),
                )

                if (isLoading) {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(OpenAquaTheme.colors.surfaceHigh)
                                .border(1.dp, BorderOutline, RoundedCornerShape(10.dp))
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                strokeWidth = 2.dp,
                                color = confirmContainerColor,
                            )
                            Text(
                                text = loadingText ?: confirmText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                            )
                        }
                    }
                } else {
                    OpenAquaButton(
                        text = confirmText,
                        onClick = onConfirm,
                        enabled = enabled,
                        containerColor = confirmContainerColor,
                        contentColor = confirmContentColor,
                    )
                }
            }
        }
    }
}
