package io.github.lamba92.openaqua.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.lamba92.openaqua.app.theme.BorderOutline
import io.github.lamba92.openaqua.app.theme.CyanBright
import io.github.lamba92.openaqua.app.theme.SurfaceContainer
import io.github.lamba92.openaqua.app.theme.SurfaceContainerLow
import io.github.lamba92.openaqua.app.theme.TextMuted
import io.github.lamba92.openaqua.app.theme.TextPrimary
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.rename_dialog_cancel
import open_aqua.app_compose.generated.resources.rename_dialog_label
import open_aqua.app_compose.generated.resources.rename_dialog_placeholder
import open_aqua.app_compose.generated.resources.rename_dialog_save
import org.jetbrains.compose.resources.stringResource

@Composable
fun RenameDialog(
    title: String,
    initialValue: String,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(Res.string.rename_dialog_label),
    placeholder: String = stringResource(Res.string.rename_dialog_placeholder),
    confirmText: String = stringResource(Res.string.rename_dialog_save),
    dismissText: String = stringResource(Res.string.rename_dialog_cancel),
    icon: Painter? = null,
    iconTint: Color = CyanBright,
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = initialValue, selection = TextRange(0, initialValue.length)))
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun submit() {
        onConfirm(textFieldValue.text.trim().takeIf { it.isNotEmpty() })
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
    ) {
        Column(
            modifier =
                modifier
                    .width(440.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, BorderOutline, RoundedCornerShape(16.dp))
                    .padding(24.dp)
                    .onPreviewKeyEvent { event ->
                        when (event.type) {
                            KeyEventType.KeyDown ->
                                when (event.key) {
                                    Key.Escape -> {
                                        onDismiss()
                                        true
                                    }
                                    Key.Enter, Key.NumPadEnter -> {
                                        submit()
                                        true
                                    }
                                    else -> false
                                }
                            else -> false
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                label = { Text(text = label) },
                placeholder = { Text(text = placeholder, color = TextMuted) },
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanBright,
                        unfocusedBorderColor = BorderOutline,
                        focusedLabelColor = CyanBright,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = CyanBright,
                        focusedContainerColor = SurfaceContainer,
                        unfocusedContainerColor = SurfaceContainer,
                    ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OpenAquaOutlinedButton(
                    text = dismissText,
                    onClick = onDismiss,
                )

                OpenAquaButton(
                    text = confirmText,
                    onClick = { submit() },
                )
            }
        }
    }
}
