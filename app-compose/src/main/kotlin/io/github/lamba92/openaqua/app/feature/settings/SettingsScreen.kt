package io.github.lamba92.openaqua.app.feature.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.model.AppLanguage
import io.github.lamba92.openaqua.app.model.stringResource
import io.github.lamba92.openaqua.app.theme.AppFontFamily
import io.github.lamba92.openaqua.app.theme.AppMetricsScale
import io.github.lamba92.openaqua.app.theme.FontScaleConstants
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.theme.calculateFontScaleFactor
import io.github.lamba92.openaqua.app.theme.stringResource
import io.github.lamba92.openaqua.app.theme.toComposeColor
import io.github.lamba92.openaqua.app.ui.components.ConfirmationDialog
import io.github.lamba92.openaqua.app.ui.components.OpenAquaOutlinedButton
import io.github.lamba92.openaqua.app.ui.components.OpenAquaVerticalScrollbar
import io.github.lamba92.openaqua.app.ui.components.ScreenHeader
import io.github.lamba92.openaqua.app.ui.components.SectionHeader
import io.github.lamba92.openaqua.app.ui.components.scrollFadingEdge
import io.github.lamba92.openaqua.theme.AppColorScheme
import io.github.lamba92.openaqua.theme.ThemePalette
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.font_preset_extra_large
import open_aqua.app_compose.generated.resources.font_preset_large
import open_aqua.app_compose.generated.resources.font_preset_small
import open_aqua.app_compose.generated.resources.font_preset_standard
import open_aqua.app_compose.generated.resources.ic_folder_open
import open_aqua.app_compose.generated.resources.ic_restart_alt
import open_aqua.app_compose.generated.resources.ic_sensors
import open_aqua.app_compose.generated.resources.ic_settings
import open_aqua.app_compose.generated.resources.ic_tune
import open_aqua.app_compose.generated.resources.ic_warning
import open_aqua.app_compose.generated.resources.settings_app_data_title
import open_aqua.app_compose.generated.resources.settings_appearance_desc
import open_aqua.app_compose.generated.resources.settings_appearance_title
import open_aqua.app_compose.generated.resources.settings_channel_visibility_desc
import open_aqua.app_compose.generated.resources.settings_channel_visibility_title
import open_aqua.app_compose.generated.resources.settings_color_scheme_desc
import open_aqua.app_compose.generated.resources.settings_color_scheme_title
import open_aqua.app_compose.generated.resources.settings_font_family_desc
import open_aqua.app_compose.generated.resources.settings_font_family_title
import open_aqua.app_compose.generated.resources.settings_font_scale_desc
import open_aqua.app_compose.generated.resources.settings_font_scale_title
import open_aqua.app_compose.generated.resources.settings_hide_disconnected_fans_desc
import open_aqua.app_compose.generated.resources.settings_hide_disconnected_fans_title
import open_aqua.app_compose.generated.resources.settings_hide_disconnected_sensors_desc
import open_aqua.app_compose.generated.resources.settings_hide_disconnected_sensors_in_fans_desc
import open_aqua.app_compose.generated.resources.settings_hide_disconnected_sensors_in_fans_title
import open_aqua.app_compose.generated.resources.settings_hide_disconnected_sensors_title
import open_aqua.app_compose.generated.resources.settings_hide_unconfigured_virtual_sensors_desc
import open_aqua.app_compose.generated.resources.settings_hide_unconfigured_virtual_sensors_title
import open_aqua.app_compose.generated.resources.settings_language_desc
import open_aqua.app_compose.generated.resources.settings_language_title
import open_aqua.app_compose.generated.resources.settings_metrics_scale_desc
import open_aqua.app_compose.generated.resources.settings_metrics_scale_title
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
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Top-level fixed header
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = metrics.spacingXxl)
                            .padding(top = metrics.spacingXxl, bottom = metrics.spacingLg),
                    verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
                ) {
                    ScreenHeader(
                        title = stringResource(Res.string.settings_screen_title),
                        subtitle = stringResource(Res.string.settings_screen_subtitle),
                    )
                }

                HorizontalDivider(
                    color = colors.borderOutline,
                    modifier = Modifier.padding(horizontal = metrics.spacingXxl),
                )
            }

            val scrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }

            // Scrolling content with internal padding and top fading edge
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .scrollFadingEdge(
                            color = colors.background,
                            scrollState = scrollState,
                            height = 36.dp,
                        ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = metrics.spacingXxl)
                            .padding(top = metrics.spacingLg, bottom = metrics.spacingXxl),
                    verticalArrangement = Arrangement.spacedBy(metrics.spacingXl),
                ) {
                    // Appearance & Scaling section
                    AppearanceSection(
                        selectedColorScheme = state.colorScheme,
                        selectedMetricsScale = state.metricsScale,
                        selectedFontFamily = state.fontFamily,
                        selectedFontSp = state.fontSp,
                        selectedLanguage = state.language,
                        onSelectColorScheme = { viewModel.onEvent(SettingsEvent.SelectColorScheme(it)) },
                        onSelectMetricsScale = { viewModel.onEvent(SettingsEvent.SelectMetricsScale(it)) },
                        onSelectFontFamily = { viewModel.onEvent(SettingsEvent.SelectFontFamily(it)) },
                        onSetFontSp = { viewModel.onEvent(SettingsEvent.SetFontSp(it)) },
                        onSelectLanguage = { viewModel.onEvent(SettingsEvent.SelectLanguage(it)) },
                    )

                    HorizontalDivider(color = colors.borderOutline)

                    // Channel Visibility section
                    ChannelVisibilitySection(
                        hideDisconnectedFans = state.hideDisconnectedFans,
                        hideDisconnectedSensorsInFans = state.hideDisconnectedSensorsInFans,
                        hideDisconnectedSensors = state.hideDisconnectedSensors,
                        hideUnconfiguredVirtualSensors = state.hideUnconfiguredVirtualSensors,
                        onToggleHideDisconnectedFans = {
                            viewModel.onEvent(SettingsEvent.ToggleHideDisconnectedFans(it))
                        },
                        onToggleHideDisconnectedSensorsInFans = {
                            viewModel.onEvent(SettingsEvent.ToggleHideDisconnectedSensorsInFans(it))
                        },
                        onToggleHideDisconnectedSensors = {
                            viewModel.onEvent(SettingsEvent.ToggleHideDisconnectedSensors(it))
                        },
                        onToggleHideUnconfiguredVirtualSensors = {
                            viewModel.onEvent(SettingsEvent.ToggleHideUnconfiguredVirtualSensors(it))
                        },
                    )

                    HorizontalDivider(color = colors.borderOutline)

                    // App data vertical section
                    AppDataSection(
                        dataFolderPath = state.dataFolderPath,
                        onOpenDataFolder = { viewModel.onEvent(SettingsEvent.OpenDataFolder) },
                        onRestoreDefaults = { viewModel.onEvent(SettingsEvent.RequestRestoreDefaults) },
                    )
                }

                OpenAquaVerticalScrollbar(
                    scrollState = scrollState,
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp),
                )
            }
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
private fun AppearanceSection(
    selectedColorScheme: AppColorScheme,
    selectedMetricsScale: AppMetricsScale,
    selectedFontFamily: AppFontFamily,
    selectedFontSp: Int,
    selectedLanguage: AppLanguage,
    onSelectColorScheme: (AppColorScheme) -> Unit,
    onSelectMetricsScale: (AppMetricsScale) -> Unit,
    onSelectFontFamily: (AppFontFamily) -> Unit,
    onSetFontSp: (Int) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(metrics.radiusLarge))
                .background(colors.surfaceLow)
                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusLarge))
                .padding(metrics.spacingXl),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
    ) {
        SectionHeader(
            icon = painterResource(Res.drawable.ic_tune),
            title = stringResource(Res.string.settings_appearance_title),
        )

        Text(
            text = stringResource(Res.string.settings_appearance_desc),
            color = colors.textMuted,
            fontSize = 12.sp,
        )

        // Color scheme chooser
        Column(verticalArrangement = Arrangement.spacedBy(metrics.spacingSm)) {
            Text(
                text = stringResource(Res.string.settings_color_scheme_title),
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.settings_color_scheme_desc),
                color = colors.textMuted,
                fontSize = 12.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
            ) {
                AppColorScheme.entries.forEach { scheme ->
                    val palette = ThemePalette.fromColorScheme(scheme)
                    val isSelected = scheme == selectedColorScheme
                    ThemeOptionChip(
                        text = stringResource(scheme.stringResource),
                        isSelected = isSelected,
                        onClick = { onSelectColorScheme(scheme) },
                        previewColors =
                            listOf(
                                palette.primary.toComposeColor(),
                                palette.secondary.toComposeColor(),
                                palette.surfaceHigh.toComposeColor(),
                            ),
                    )
                }
            }
        }

        HorizontalDivider(color = colors.borderOutline)

        // Interface density chooser
        Column(verticalArrangement = Arrangement.spacedBy(metrics.spacingSm)) {
            Text(
                text = stringResource(Res.string.settings_metrics_scale_title),
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.settings_metrics_scale_desc),
                color = colors.textMuted,
                fontSize = 12.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
            ) {
                AppMetricsScale.entries.forEach { scale ->
                    val isSelected = scale == selectedMetricsScale
                    ThemeOptionChip(
                        text = stringResource(scale.stringResource),
                        isSelected = isSelected,
                        onClick = { onSelectMetricsScale(scale) },
                    )
                }
            }
        }

        HorizontalDivider(color = colors.borderOutline)

        // Font family chooser
        Column(verticalArrangement = Arrangement.spacedBy(metrics.spacingSm)) {
            Text(
                text = stringResource(Res.string.settings_font_family_title),
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.settings_font_family_desc),
                color = colors.textMuted,
                fontSize = 12.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
            ) {
                AppFontFamily.entries.forEach { family ->
                    val isSelected = family == selectedFontFamily
                    ThemeOptionChip(
                        text = stringResource(family.stringResource),
                        isSelected = isSelected,
                        onClick = { onSelectFontFamily(family) },
                    )
                }
            }
        }

        HorizontalDivider(color = colors.borderOutline)

        // Font size / scale in sp chooser
        FontSizeSection(
            selectedFontSp = selectedFontSp,
            onSetFontSp = onSetFontSp,
        )

        HorizontalDivider(color = colors.borderOutline)

        // Language selector row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(Res.string.settings_language_title),
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.settings_language_desc),
                    color = colors.textMuted,
                    fontSize = 12.sp,
                )
            }

            LanguageDropdown(
                selectedLanguage = selectedLanguage,
                onSelectLanguage = onSelectLanguage,
            )
        }
    }
}

@Composable
private fun LanguageDropdown(
    selectedLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Box(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(metrics.radiusSmall))
                    .background(colors.surfaceLowest)
                    .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusSmall))
                    .clickable { expanded = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(selectedLanguage.stringResource),
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(18.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier =
                Modifier
                    .background(colors.surfaceHigh)
                    .border(metrics.strokeThin, colors.borderOutline),
        ) {
            AppLanguage.entries.forEach { lang ->
                val isSelected = lang == selectedLanguage
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(lang.stringResource),
                            color = if (isSelected) colors.primaryBright else colors.textPrimary,
                            fontSize = 13.sp,
                            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onSelectLanguage(lang)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ChannelVisibilitySection(
    hideDisconnectedFans: Boolean,
    hideDisconnectedSensorsInFans: Boolean,
    hideDisconnectedSensors: Boolean,
    hideUnconfiguredVirtualSensors: Boolean,
    onToggleHideDisconnectedFans: (Boolean) -> Unit,
    onToggleHideDisconnectedSensorsInFans: (Boolean) -> Unit,
    onToggleHideDisconnectedSensors: (Boolean) -> Unit,
    onToggleHideUnconfiguredVirtualSensors: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(metrics.radiusLarge))
                .background(colors.surfaceLow)
                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusLarge))
                .padding(metrics.spacingXl),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
    ) {
        SectionHeader(
            icon = painterResource(Res.drawable.ic_sensors),
            title = stringResource(Res.string.settings_channel_visibility_title),
        )

        Text(
            text = stringResource(Res.string.settings_channel_visibility_desc),
            color = colors.textMuted,
            fontSize = 12.sp,
        )

        SettingToggleRow(
            title = stringResource(Res.string.settings_hide_disconnected_fans_title),
            description = stringResource(Res.string.settings_hide_disconnected_fans_desc),
            checked = hideDisconnectedFans,
            onCheckedChange = onToggleHideDisconnectedFans,
        )

        HorizontalDivider(color = colors.borderOutline)

        SettingToggleRow(
            title = stringResource(Res.string.settings_hide_disconnected_sensors_in_fans_title),
            description = stringResource(Res.string.settings_hide_disconnected_sensors_in_fans_desc),
            checked = hideDisconnectedSensorsInFans,
            onCheckedChange = onToggleHideDisconnectedSensorsInFans,
        )

        HorizontalDivider(color = colors.borderOutline)

        SettingToggleRow(
            title = stringResource(Res.string.settings_hide_disconnected_sensors_title),
            description = stringResource(Res.string.settings_hide_disconnected_sensors_desc),
            checked = hideDisconnectedSensors,
            onCheckedChange = onToggleHideDisconnectedSensors,
        )

        HorizontalDivider(color = colors.borderOutline)

        SettingToggleRow(
            title = stringResource(Res.string.settings_hide_unconfigured_virtual_sensors_title),
            description = stringResource(Res.string.settings_hide_unconfigured_virtual_sensors_desc),
            checked = hideUnconfiguredVirtualSensors,
            onCheckedChange = onToggleHideUnconfiguredVirtualSensors,
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = metrics.spacingLg),
            verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
        ) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                color = colors.textMuted,
                fontSize = 12.sp,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = colors.surfaceLowest,
                    checkedTrackColor = colors.primaryBright,
                    checkedBorderColor = colors.primaryBright,
                    uncheckedThumbColor = colors.textMuted,
                    uncheckedTrackColor = colors.surfaceHigh,
                    uncheckedBorderColor = colors.borderOutline,
                ),
        )
    }
}

@Composable
private fun FontSizeSection(
    selectedFontSp: Int,
    onSetFontSp: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    val percentage = (calculateFontScaleFactor(selectedFontSp) * 100).roundToInt()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingSm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(Res.string.settings_font_scale_title),
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text =
                        stringResource(
                            Res.string.settings_font_scale_desc,
                            FontScaleConstants.MIN_SP,
                            FontScaleConstants.MAX_SP,
                        ),
                    color = colors.textMuted,
                    fontSize = 12.sp,
                )
            }

            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceHigh)
                        .border(metrics.strokeThin, colors.primaryBright, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "$selectedFontSp sp ($percentage%)",
                    color = colors.primaryBright,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                )
            }
        }

        // Quick presets
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
        ) {
            val presets =
                listOf(
                    11 to Res.string.font_preset_small,
                    13 to Res.string.font_preset_standard,
                    15 to Res.string.font_preset_large,
                    18 to Res.string.font_preset_extra_large,
                )
            presets.forEach { (sp, labelRes) ->
                val isSelected = selectedFontSp == sp
                ThemeOptionChip(
                    text = stringResource(labelRes),
                    isSelected = isSelected,
                    onClick = { onSetFontSp(sp) },
                )
            }
        }

        // Slider with stepper buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconButton(
                onClick = { onSetFontSp(selectedFontSp - 1) },
                enabled = selectedFontSp > FontScaleConstants.MIN_SP,
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease font size",
                    tint = if (selectedFontSp > FontScaleConstants.MIN_SP) colors.primaryBright else colors.textMuted,
                )
            }

            Text(
                text = "${FontScaleConstants.MIN_SP} sp",
                color = colors.textMuted,
                fontSize = 11.sp,
                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
            )

            Slider(
                value = selectedFontSp.toFloat(),
                onValueChange = { onSetFontSp(it.roundToInt()) },
                valueRange = FontScaleConstants.MIN_SP.toFloat()..FontScaleConstants.MAX_SP.toFloat(),
                steps = FontScaleConstants.MAX_SP - FontScaleConstants.MIN_SP - 1,
                modifier = Modifier.weight(1f),
                colors =
                    SliderDefaults.colors(
                        thumbColor = colors.primaryBright,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.surfaceContainer,
                        activeTickColor = colors.background,
                        inactiveTickColor = colors.borderOutline,
                    ),
            )

            Text(
                text = "${FontScaleConstants.MAX_SP} sp",
                color = colors.textMuted,
                fontSize = 11.sp,
                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
            )

            IconButton(
                onClick = { onSetFontSp(selectedFontSp + 1) },
                enabled = selectedFontSp < FontScaleConstants.MAX_SP,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase font size",
                    tint = if (selectedFontSp < FontScaleConstants.MAX_SP) colors.primaryBright else colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    previewColors: List<Color>? = null,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    val shape = RoundedCornerShape(metrics.radiusSmall)
    val borderColor = if (isSelected) colors.primaryBright else colors.borderOutline
    val containerColor = if (isSelected) colors.surfaceHigh else colors.surfaceLowest
    val textColor = if (isSelected) colors.primaryBright else colors.textSecondary

    Row(
        modifier =
            modifier
                .clip(shape)
                .background(containerColor)
                .border(metrics.strokeThin, borderColor, shape)
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (previewColors != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                previewColors.forEach { color ->
                    Box(
                        modifier =
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color),
                    )
                }
            }
        }
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
        )
    }
}

@Composable
private fun AppDataSection(
    dataFolderPath: String,
    onOpenDataFolder: () -> Unit,
    onRestoreDefaults: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(metrics.radiusLarge))
                .background(colors.surfaceLow)
                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusLarge))
                .padding(metrics.spacingXl),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
    ) {
        SectionHeader(
            icon = painterResource(Res.drawable.ic_settings),
            title = stringResource(Res.string.settings_app_data_title),
        )

        // Open Data Folder Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = metrics.spacingLg),
                verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
            ) {
                Text(
                    text = stringResource(Res.string.settings_open_data_folder_title),
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.settings_open_data_folder_desc),
                    color = colors.textMuted,
                    fontSize = 12.sp,
                )
                Text(
                    text = dataFolderPath,
                    color = colors.primary,
                    fontSize = 11.sp,
                    fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            OpenAquaOutlinedButton(
                text = stringResource(Res.string.settings_open_data_folder_button),
                icon = painterResource(Res.drawable.ic_folder_open),
                onClick = onOpenDataFolder,
            )
        }

        HorizontalDivider(color = colors.borderOutline)

        // Restore Defaults Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = metrics.spacingLg),
                verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
            ) {
                Text(
                    text = stringResource(Res.string.settings_restore_defaults_title),
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.settings_restore_defaults_desc),
                    color = colors.textMuted,
                    fontSize = 12.sp,
                )
            }

            OpenAquaOutlinedButton(
                text = stringResource(Res.string.settings_restore_defaults_button),
                icon = painterResource(Res.drawable.ic_restart_alt),
                borderColor = colors.statusWarning,
                contentColor = colors.statusWarning,
                iconTint = colors.statusWarning,
                onClick = onRestoreDefaults,
            )
        }
    }
}
