package io.github.lamba92.openaqua.app.feature.device.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.unit.dp
import io.github.lamba92.openaqua.app.theme.BorderOutline
import io.github.lamba92.openaqua.app.theme.CoolantTeal
import io.github.lamba92.openaqua.app.theme.CyanBright
import io.github.lamba92.openaqua.app.theme.SurfaceContainerLow
import io.github.lamba92.openaqua.app.theme.TextMuted
import io.github.lamba92.openaqua.app.ui.components.ChannelDetailHeader
import io.github.lamba92.openaqua.app.ui.components.ChannelPageScaffold
import io.github.lamba92.openaqua.app.ui.components.FeaturePlaceholderBox
import io.github.lamba92.openaqua.app.ui.components.MetricCard
import io.github.lamba92.openaqua.app.ui.components.OpenAquaOutlinedButton
import io.github.lamba92.openaqua.app.ui.components.RenameDialog
import io.github.lamba92.openaqua.app.ui.components.SquareSelectionButton
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.action_rename
import open_aqua.app_compose.generated.resources.fan_channel_name
import open_aqua.app_compose.generated.resources.fan_channel_pin
import open_aqua.app_compose.generated.resources.fan_channels_header
import open_aqua.app_compose.generated.resources.fan_current_value
import open_aqua.app_compose.generated.resources.fan_curve_studio_desc
import open_aqua.app_compose.generated.resources.fan_curve_studio_title
import open_aqua.app_compose.generated.resources.fan_hardware_pin
import open_aqua.app_compose.generated.resources.fan_power_value
import open_aqua.app_compose.generated.resources.fan_pwm_value
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
        selectorRow = {
            fans.forEach { fan ->
                val fanNumber = fan.channel + 1
                FanSelectionTile(
                    name = fan.customName ?: stringResource(Res.string.fan_channel_name, fanNumber),
                    pinLabel = stringResource(Res.string.fan_channel_pin, fanNumber),
                    rpm = fan.telemetry.rpm,
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
    selectorRow: @Composable RowScope.() -> Unit,
    detailContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    ChannelPageScaffold(
        title = stringResource(Res.string.fans_screen_title),
        subtitle = stringResource(Res.string.fans_screen_subtitle),
        selectorHeader = stringResource(Res.string.fan_channels_header),
        selectorRow = selectorRow,
        detailContent = detailContent,
        modifier = modifier,
    )
}

@Composable
fun FanSelectionTile(
    name: String,
    pinLabel: String,
    rpm: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDoubleClick: (() -> Unit)? = null,
) {
    SquareSelectionButton(
        name = name,
        selected = selected,
        onClick = onClick,
        onDoubleClick = onDoubleClick,
        badgeText = pinLabel,
        valueText = stringResource(Res.string.fan_rpm_value, rpm.coerceAtLeast(0)),
        modifier = modifier,
        icon = {
            Icon(
                painter = painterResource(Res.drawable.ic_fan),
                contentDescription = name,
                tint = if (selected) CyanBright else (if (rpm > 0) CoolantTeal else TextMuted),
                modifier = Modifier.size(24.dp),
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FanMetricCard(
                label = stringResource(Res.string.metric_speed),
                value = stringResource(Res.string.fan_rpm_value, tel.rpm),
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
        width = 130.dp,
        modifier = modifier,
    )
}
