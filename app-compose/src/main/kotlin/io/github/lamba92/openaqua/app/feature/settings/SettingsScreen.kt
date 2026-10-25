package io.github.lamba92.openaqua.app.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.lamba92.openaqua.app.theme.BorderOutline
import io.github.lamba92.openaqua.app.theme.BrandPureWhite
import io.github.lamba92.openaqua.app.theme.CyanBright
import io.github.lamba92.openaqua.app.theme.ObsidianBg
import io.github.lamba92.openaqua.app.theme.StatusCritical
import io.github.lamba92.openaqua.app.theme.SurfaceContainerHigh
import io.github.lamba92.openaqua.app.theme.SurfaceContainerLow
import io.github.lamba92.openaqua.app.theme.TextMuted
import io.github.lamba92.openaqua.app.theme.TextPrimary
import io.github.lamba92.openaqua.app.theme.TextSecondary
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.ic_folder_open
import open_aqua.app_compose.generated.resources.ic_restart_alt
import open_aqua.app_compose.generated.resources.ic_settings
import open_aqua.app_compose.generated.resources.ic_warning
import open_aqua.app_compose.generated.resources.settings_app_data_title
import open_aqua.app_compose.generated.resources.settings_open_data_folder_button
import open_aqua.app_compose.generated.resources.settings_open_data_folder_desc
import open_aqua.app_compose.generated.resources.settings_open_data_folder_title
import open_aqua.app_compose.generated.resources.settings_reset_dialog_cancel
import open_aqua.app_compose.generated.resources.settings_reset_dialog_confirm
import open_aqua.app_compose.generated.resources.settings_reset_dialog_message
import open_aqua.app_compose.generated.resources.settings_reset_dialog_title
import open_aqua.app_compose.generated.resources.settings_restore_defaults_button
import open_aqua.app_compose.generated.resources.settings_restore_defaults_desc
import open_aqua.app_compose.generated.resources.settings_restore_defaults_title
import open_aqua.app_compose.generated.resources.settings_screen_subtitle
import open_aqua.app_compose.generated.resources.settings_screen_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.settings_screen_title),
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(Res.string.settings_screen_subtitle),
                        color = TextMuted,
                        fontSize = 13.sp,
                    )
                }
            }

            HorizontalDivider(color = BorderOutline)

            // App data vertical section
            AppDataSection(
                dataFolderPath = state.dataFolderPath,
                onOpenDataFolder = { viewModel.onEvent(SettingsEvent.OpenDataFolder) },
                onRestoreDefaults = { viewModel.onEvent(SettingsEvent.RequestRestoreDefaults) },
            )
        }

        if (state.showResetConfirmationDialog) {
            RestoreDefaultsConfirmationDialog(
                onDismiss = { viewModel.onEvent(SettingsEvent.DismissRestoreDefaults) },
                onConfirmReset = { viewModel.onEvent(SettingsEvent.ConfirmRestoreDefaults) },
            )
        }
    }
}

@Composable
private fun AppDataSection(
    dataFolderPath: String,
    onOpenDataFolder: () -> Unit,
    onRestoreDefaults: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceContainerLow)
                .border(1.dp, BorderOutline, RoundedCornerShape(16.dp))
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_settings),
                contentDescription = null,
                tint = CyanBright,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(Res.string.settings_app_data_title),
                color = CyanBright,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // Open data folder row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(Res.string.settings_open_data_folder_title),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.settings_open_data_folder_desc),
                    color = TextMuted,
                    fontSize = 12.sp,
                )
                if (dataFolderPath.isNotEmpty()) {
                    Text(
                        text = dataFolderPath,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            OutlinedButton(
                onClick = onOpenDataFolder,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, BorderOutline),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceContainerHigh,
                        contentColor = TextPrimary,
                    ),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_folder_open),
                    contentDescription = null,
                    tint = CyanBright,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.settings_open_data_folder_button),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        HorizontalDivider(color = BorderOutline)

        // Restore defaults row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(Res.string.settings_restore_defaults_title),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.settings_restore_defaults_desc),
                    color = TextMuted,
                    fontSize = 12.sp,
                )
            }

            OutlinedButton(
                onClick = onRestoreDefaults,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, StatusCritical.copy(alpha = 0.6f)),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceContainerHigh,
                        contentColor = StatusCritical,
                    ),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_restart_alt),
                    contentDescription = null,
                    tint = StatusCritical,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.settings_restore_defaults_button),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun RestoreDefaultsConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirmReset: () -> Unit,
) {
    val cancelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        cancelFocusRequester.requestFocus()
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
                Modifier
                    .width(440.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, BorderOutline, RoundedCornerShape(16.dp))
                    .padding(24.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
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
                Icon(
                    painter = painterResource(Res.drawable.ic_warning),
                    contentDescription = null,
                    tint = StatusCritical,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(Res.string.settings_reset_dialog_title),
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = stringResource(Res.string.settings_reset_dialog_message),
                color = TextSecondary,
                fontSize = 13.sp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = CyanBright,
                            contentColor = ObsidianBg,
                        ),
                    modifier =
                        Modifier
                            .focusRequester(cancelFocusRequester)
                            .onPreviewKeyEvent { event ->
                                if (
                                    event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    onDismiss()
                                    true
                                } else {
                                    false
                                }
                            },
                ) {
                    Text(
                        text = stringResource(Res.string.settings_reset_dialog_cancel),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Button(
                    onClick = onConfirmReset,
                    shape = RoundedCornerShape(10.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = StatusCritical,
                            contentColor = BrandPureWhite,
                        ),
                    modifier =
                        Modifier.onPreviewKeyEvent { event ->
                            if (
                                event.type == KeyEventType.KeyDown &&
                                (event.key == Key.Enter || event.key == Key.NumPadEnter)
                            ) {
                                onConfirmReset()
                                true
                            } else {
                                false
                            }
                        },
                ) {
                    Text(
                        text = stringResource(Res.string.settings_reset_dialog_confirm),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
