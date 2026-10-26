package io.github.lamba92.openaqua.app.feature.device.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import io.github.lamba92.openaqua.app.theme.CoolantTeal
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.ChannelDetailHeader
import io.github.lamba92.openaqua.app.ui.components.ChannelPageScaffold
import io.github.lamba92.openaqua.app.ui.components.ChannelSelectionCard
import io.github.lamba92.openaqua.app.ui.components.ChannelSelectionTileDefaults
import io.github.lamba92.openaqua.app.ui.components.FeaturePlaceholderBox
import io.github.lamba92.openaqua.app.ui.components.MetricCard
import io.github.lamba92.openaqua.app.ui.components.OpenAquaOutlinedButton
import io.github.lamba92.openaqua.app.ui.components.RenameDialog
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.action_rename
import open_aqua.app_compose.generated.resources.fan_channel_name
import open_aqua.app_compose.generated.resources.fan_channel_pin
import open_aqua.app_compose.generated.resources.fan_channels_header
import open_aqua.app_compose.generated.resources.fan_current_value
import open_aqua.app_compose.generated.resources.fan_curve_studio_desc
import open_aqua.app_compose.generated.resources.fan_curve_studio_title
import open_aqua.app_compose.generated.resources.fan_disconnected
import open_aqua.app_compose.generated.resources.fan_hardware_pin
import open_aqua.app_compose.generated.resources.fan_power_value
import open_aqua.app_compose.generated.resources.fan_pwm_value
import open_aqua.app_compose.generated.resources.fan_rpm_short_disconnected
import open_aqua.app_compose.generated.resources.fan_rpm_value
import open_aqua.app_compose.generated.resources.fan_settings_title
import open_aqua.app_compose.generated.resources.fan_voltage_value
import open_aqua.app_compose.generated.resources.fans_screen_subtitle
import open_aqua.app_compose.generated.resources.fans_screen_title
import open_aqua.app_compose.generated.resources.ic_edit
import open_aqua.app_compose.generated.resources.ic_fan
import open_aqua.app_compose.generated.resources.ic_speed
import open_aqua.app_compose.generated.resources.ic_tune
import open_aqua.app_compose.generated.resources.metric_current
import open_aqua.app_compose.generated.resources.metric_power
import open_aqua.app_compose.generated.resources.metric_pwm_duty
import open_aqua.app_compose.generated.resources.metric_speed
import open_aqua.app_compose.generated.resources.metric_voltage
import open_aqua.app_compose.generated.resources.rename_dialog_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun FansContent(
    fans: List<FanItem>,
    selectedChannel: Int,
    onSelectFan: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onRenameFan: ((channel: Int, name: String?) -> Unit)? = null,
) {
    val selectedFan = fans.firstOrNull { it.channel == selectedChannel } ?: fans.firstOrNull()
    var renamingFan by remember { mutableStateOf<FanItem?>(null) }

    FansPageScaffold(
        modifier = modifier,
        selectorContent = {
            fans.forEach { fan ->
                val fanNumber = fan.channel + 1
                FanSelectionTile(
                    name = fan.customName ?: stringResource(Res.string.fan_channel_name, fanNumber),
                    pinLabel = stringResource(Res.string.fan_channel_pin, fanNumber),
                    rpm = fan.telemetry.rpm,
                    pwmPercent = fan.telemetry.pwmPercent,
                    selected = fan.channel == selectedChannel,
                    onClick = { onSelectFan(fan.channel) },
                    onDoubleClick = onRenameFan?.let { { renamingFan = fan } },
                )
            }
        },
        detailContent = {
            if (selectedFan != null) {
                val fanNumber = selectedFan.channel + 1
                FanChannelDetailCard(
                    name = selectedFan.customName ?: stringResource(Res.string.fan_channel_name, fanNumber),
                    pinLabel = stringResource(Res.string.fan_channel_pin, fanNumber),
                    fan = selectedFan,
                    onRenameClick = onRenameFan?.let { { renamingFan = selectedFan } },
                )
            }
        },
    )

    val activeRenamingFan = renamingFan
    if (activeRenamingFan != null && onRenameFan != null) {
        val fanNumber = activeRenamingFan.channel + 1
        val defaultName = stringResource(Res.string.fan_channel_name, fanNumber)
        RenameDialog(
            title = stringResource(Res.string.rename_dialog_title, activeRenamingFan.customName ?: defaultName),
            initialValue = activeRenamingFan.customName ?: "",
            icon = painterResource(Res.drawable.ic_edit),
            onConfirm = { newName ->
                onRenameFan(activeRenamingFan.channel, newName)
                renamingFan = null
            },
            onDismiss = { renamingFan = null },
        )
    }
}

@Composable
fun FansPageScaffold(
    detailContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selectorContent: @Composable () -> Unit,
) {
    ChannelPageScaffold(
        title = stringResource(Res.string.fans_screen_title),
        subtitle = stringResource(Res.string.fans_screen_subtitle),
        selectorHeader = stringResource(Res.string.fan_channels_header),
        selectorContent = selectorContent,
        detailContent = detailContent,
        modifier = modifier,
    )
}

@Composable
fun FanSelectionTile(
    name: String,
    pinLabel: String,
    rpm: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pwmPercent: Double? = null,
    onDoubleClick: (() -> Unit)? = null,
) {
    val colors = OpenAquaTheme.colors
    val metrics = ChannelSelectionTileDefaults.metrics()
    val isDisconnected = rpm == null
    val fillFraction =
        if (isDisconnected) {
            0f
        } else {
            (pwmPercent?.let { (it / 100.0).toFloat() } ?: (if (rpm > 0) 0.5f else 0f)).coerceIn(0f, 1f)
        }

    val valueText =
        rpm?.let {
            stringResource(Res.string.fan_rpm_value, it.coerceAtLeast(0))
        } ?: stringResource(Res.string.fan_rpm_short_disconnected)

    val secondaryValueText =
        if (!isDisconnected && pwmPercent != null) {
            stringResource(Res.string.fan_pwm_value, "%.0f".format(pwmPercent))
        } else {
            null
        }

    ChannelSelectionCard(
        name = name,
        badgeText = pinLabel,
        valueText = valueText,
        secondaryValueText = secondaryValueText,
        selected = selected,
        onClick = onClick,
        onDoubleClick = onDoubleClick,
        fillFraction = fillFraction,
        fillColor = colors.primary,
        modifier = modifier,
        metrics = metrics,
        icon = {
            Icon(
                painter = painterResource(Res.drawable.ic_fan),
                contentDescription = name,
                tint = if (selected) colors.primaryBright else (if (!isDisconnected && rpm > 0) CoolantTeal else colors.textMuted),
                modifier = Modifier.size(metrics.iconSize),
            )
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FanChannelDetailCard(
    name: String,
    pinLabel: String,
    fan: FanItem,
    modifier: Modifier = Modifier,
    onRenameClick: (() -> Unit)? = null,
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
        ChannelDetailHeader(
            icon = painterResource(Res.drawable.ic_tune),
            title = stringResource(Res.string.fan_settings_title, name),
            subtitle = stringResource(Res.string.fan_hardware_pin, pinLabel),
            trailingContent =
                onRenameClick?.let {
                    {
                        OpenAquaOutlinedButton(
                            text = stringResource(Res.string.action_rename),
                            icon = painterResource(Res.drawable.ic_edit),
                            onClick = it,
                        )
                    }
                },
        )

        // Live metrics telemetry
        val tel = fan.telemetry
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(metrics.spacingMd),
            verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
        ) {
            FanMetricCard(
                label = stringResource(Res.string.metric_speed),
                value =
                    tel.rpm?.let {
                        stringResource(Res.string.fan_rpm_value, it)
                    } ?: stringResource(Res.string.fan_disconnected),
            )
            FanMetricCard(
                label = stringResource(Res.string.metric_pwm_duty),
                value =
                    stringResource(
                        Res.string.fan_pwm_value,
                        tel.pwmPercent?.let { "%.1f".format(it) } ?: "0.0",
                    ),
            )
            FanMetricCard(
                label = stringResource(Res.string.metric_voltage),
                value =
                    stringResource(
                        Res.string.fan_voltage_value,
                        "%.2f".format(tel.voltageVolts),
                    ),
            )
            FanMetricCard(
                label = stringResource(Res.string.metric_current),
                value =
                    stringResource(
                        Res.string.fan_current_value,
                        "%.3f".format(tel.currentAmps),
                    ),
            )
            FanMetricCard(
                label = stringResource(Res.string.metric_power),
                value =
                    stringResource(
                        Res.string.fan_power_value,
                        "%.2f".format(tel.powerWatts),
                    ),
            )
        }

        // Iteration Placeholder Container
        FeaturePlaceholderBox(
            icon = painterResource(Res.drawable.ic_speed),
            title = stringResource(Res.string.fan_curve_studio_title),
            description = stringResource(Res.string.fan_curve_studio_desc),
        )
    }
}

@Composable
private fun FanMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    MetricCard(
        label = label,
        value = value,
        modifier = modifier,
    )
}
