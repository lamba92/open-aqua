package io.github.lamba92.openaqua.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.theme.BorderOutline
import io.github.lamba92.openaqua.app.theme.CyanBright
import io.github.lamba92.openaqua.app.theme.StatusCritical
import io.github.lamba92.openaqua.app.theme.SurfaceContainerLow
import io.github.lamba92.openaqua.app.theme.TextMuted
import io.github.lamba92.openaqua.app.theme.TextPrimary
import io.github.lamba92.openaqua.app.theme.TextSecondary
import io.github.lamba92.openaqua.app.ui.components.ConfirmationDialog
import io.github.lamba92.openaqua.app.ui.components.OpenAquaOutlinedButton
import io.github.lamba92.openaqua.app.ui.components.ScreenHeader
import io.github.lamba92.openaqua.app.ui.components.SectionHeader
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
            ScreenHeader(
                title = stringResource(Res.string.settings_screen_title),
                subtitle = stringResource(Res.string.settings_screen_subtitle),
            )

            HorizontalDivider(color = BorderOutline)

            // App data vertical section
            AppDataSection(
                dataFolderPath = state.dataFolderPath,
                onOpenDataFolder = { viewModel.onEvent(SettingsEvent.OpenDataFolder) },
                onRestoreDefaults = { viewModel.onEvent(SettingsEvent.RequestRestoreDefaults) },
            )
        }

        if (state.showResetConfirmationDialog) {
            ConfirmationDialog(
                title = stringResource(Res.string.settings_reset_dialog_title),
                message = stringResource(Res.string.settings_reset_dialog_message),
                confirmText = stringResource(Res.string.settings_reset_dialog_confirm),
                dismissText = stringResource(Res.string.settings_reset_dialog_cancel),
                icon = painterResource(Res.drawable.ic_warning),
                onDismiss = { viewModel.onEvent(SettingsEvent.DismissRestoreDefaults) },
                onConfirm = { viewModel.onEvent(SettingsEvent.ConfirmRestoreDefaults) },
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
        SectionHeader(
            icon = painterResource(Res.drawable.ic_settings),
            title = stringResource(Res.string.settings_app_data_title),
        )

        // Open data folder row
        SettingsActionRow(
            title = stringResource(Res.string.settings_open_data_folder_title),
            description = stringResource(Res.string.settings_open_data_folder_desc),
            buttonText = stringResource(Res.string.settings_open_data_folder_button),
            buttonIcon = painterResource(Res.drawable.ic_folder_open),
            onClick = onOpenDataFolder,
            detailText = dataFolderPath.takeIf { it.isNotEmpty() },
        )

        HorizontalDivider(color = BorderOutline)

        // Restore defaults row
        SettingsActionRow(
            title = stringResource(Res.string.settings_restore_defaults_title),
            description = stringResource(Res.string.settings_restore_defaults_desc),
            buttonText = stringResource(Res.string.settings_restore_defaults_button),
            buttonIcon = painterResource(Res.drawable.ic_restart_alt),
            onClick = onRestoreDefaults,
            buttonBorderColor = StatusCritical.copy(alpha = 0.6f),
            buttonContentColor = StatusCritical,
            buttonIconTint = StatusCritical,
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    description: String,
    buttonText: String,
    buttonIcon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detailText: String? = null,
    buttonBorderColor: Color = BorderOutline,
    buttonContentColor: Color = TextPrimary,
    buttonIconTint: Color = CyanBright,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                color = TextMuted,
                fontSize = 12.sp,
            )
            if (detailText != null) {
                Text(
                    text = detailText,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        OpenAquaOutlinedButton(
            text = buttonText,
            onClick = onClick,
            icon = buttonIcon,
            borderColor = buttonBorderColor,
            contentColor = buttonContentColor,
            iconTint = buttonIconTint,
        )
    }
}
