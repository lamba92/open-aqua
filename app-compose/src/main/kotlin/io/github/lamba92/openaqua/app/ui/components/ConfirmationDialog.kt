package io.github.lamba92.openaqua.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.isSpecified
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
import io.github.lamba92.openaqua.app.theme.BrandPureWhite
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme

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
    iconTint: Color = Color.Unspecified,
    confirmContainerColor: Color = Color.Unspecified,
    confirmContentColor: Color = Color.Unspecified,
    dismissContainerColor: Color = Color.Unspecified,
    dismissContentColor: Color = Color.Unspecified,
    isLoading: Boolean = false,
    loadingText: String? = null,
    enabled: Boolean = true,
    width: Dp = 440.dp,
    height: Dp? = null,
    maxHeight: Dp? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val dismissFocusRequester = remember { FocusRequester() }
    val colors = OpenAquaTheme.colors
    val resolvedIconTint = if (iconTint.isSpecified) iconTint else colors.statusCritical
    val resolvedConfirmContainerColor = if (confirmContainerColor.isSpecified) confirmContainerColor else colors.primaryBright
    val resolvedConfirmContentColor =
        if (confirmContentColor.isSpecified) {
            confirmContentColor
        } else if (confirmContainerColor.isSpecified) {
            BrandPureWhite
        } else {
            colors.background
        }

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
                usePlatformDefaultWidth = false,
            ),
    ) {
        Column(
            modifier =
                modifier
                    .width(width)
                    .then(
                        if (height != null) {
                            Modifier.height(height)
                        } else if (maxHeight != null) {
                            Modifier.heightIn(max = maxHeight)
                        } else {
                            Modifier
                        },
                    ).clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceLow)
                    .border(1.dp, colors.borderOutline, RoundedCornerShape(16.dp))
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
                        tint = resolvedIconTint,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (message != null) {
                Text(
                    text = message,
                    color = colors.textSecondary,
                    fontSize = 15.sp,
                )
            }

            if (content != null) {
                if (height != null) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        content()
                    }
                } else if (maxHeight != null) {
                    Box(modifier = Modifier.weight(1f, fill = false).fillMaxWidth()) {
                        content()
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        content()
                    }
                }
            } else if (height != null) {
                Spacer(modifier = Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OpenAquaOutlinedButton(
                    text = dismissText,
                    onClick = onDismiss,
                    enabled = enabled && !isLoading,
                    containerColor = dismissContainerColor,
                    contentColor = dismissContentColor,
                    modifier = Modifier.focusRequester(dismissFocusRequester),
                )

                OpenAquaButton(
                    text = confirmText,
                    onClick = onConfirm,
                    enabled = enabled,
                    isLoading = isLoading,
                    loadingText = loadingText,
                    containerColor = resolvedConfirmContainerColor,
                    contentColor = resolvedConfirmContentColor,
                )
            }
        }
    }
}
