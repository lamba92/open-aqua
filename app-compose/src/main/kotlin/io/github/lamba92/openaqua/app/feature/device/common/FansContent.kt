package io.github.lamba92.openaqua.app.feature.device.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.theme.BorderOutline
import io.github.lamba92.openaqua.app.theme.CoolantTeal
import io.github.lamba92.openaqua.app.theme.CyanBright
import io.github.lamba92.openaqua.app.theme.ElectricCyan
import io.github.lamba92.openaqua.app.theme.SurfaceContainer
import io.github.lamba92.openaqua.app.theme.SurfaceContainerHigh
import io.github.lamba92.openaqua.app.theme.SurfaceContainerLow
import io.github.lamba92.openaqua.app.theme.TextMuted
import io.github.lamba92.openaqua.app.theme.TextPrimary
import io.github.lamba92.openaqua.app.ui.components.SquareSelectionButton
import open_aqua.app_compose.generated.resources.Res
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
import open_aqua.app_compose.generated.resources.ic_fan
import open_aqua.app_compose.generated.resources.ic_speed
import open_aqua.app_compose.generated.resources.ic_tune
import open_aqua.app_compose.generated.resources.metric_current
import open_aqua.app_compose.generated.resources.metric_power
import open_aqua.app_compose.generated.resources.metric_pwm_duty
import open_aqua.app_compose.generated.resources.metric_speed
import open_aqua.app_compose.generated.resources.metric_voltage
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun FansContent(
    fans: List<FanItem>,
    selectedChannel: Int,
    onSelectFan: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedFan = fans.firstOrNull { it.channel == selectedChannel } ?: fans.firstOrNull()

    FansPageScaffold(
        modifier = modifier,
        selectorRow = {
            fans.forEach { fan ->
                val fanNumber = fan.channel + 1
                FanSelectionTile(
                    name = stringResource(Res.string.fan_channel_name, fanNumber),
                    pinLabel = stringResource(Res.string.fan_channel_pin, fanNumber),
                    rpm = fan.telemetry.rpm,
                    selected = fan.channel == selectedChannel,
                    onClick = { onSelectFan(fan.channel) },
                )
            }
        },
        detailContent = {
            if (selectedFan != null) {
                val fanNumber = selectedFan.channel + 1
                FanChannelDetailCard(
                    name = stringResource(Res.string.fan_channel_name, fanNumber),
                    pinLabel = stringResource(Res.string.fan_channel_pin, fanNumber),
                    fan = selectedFan,
                )
            }
        },
    )
}

@Composable
fun FansPageScaffold(
    selectorRow: @Composable RowScope.() -> Unit,
    detailContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                        text = stringResource(Res.string.fans_screen_title),
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(Res.string.fans_screen_subtitle),
                        color = TextMuted,
                        fontSize = 13.sp,
                    )
                }
            }

            HorizontalDivider(color = BorderOutline)

            // TOP ROW: Square buttons with channel name below
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(Res.string.fan_channels_header),
                    color = ElectricCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    content = selectorRow,
                )
            }

            HorizontalDivider(color = BorderOutline)

            detailContent()
        }
    }
}

@Composable
fun FanSelectionTile(
    name: String,
    pinLabel: String,
    rpm: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SquareSelectionButton(
        name = name,
        selected = selected,
        onClick = onClick,
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_tune),
                    contentDescription = null,
                    tint = CyanBright,
                    modifier = Modifier.size(20.dp),
                )
                Column {
                    Text(
                        text = stringResource(Res.string.fan_settings_title, name),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(Res.string.fan_hardware_pin, pinLabel),
                        color = TextMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        }

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
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainer)
                    .border(1.dp, BorderOutline, RoundedCornerShape(12.dp))
                    .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_speed),
                    contentDescription = null,
                    tint = ElectricCyan.copy(alpha = 0.7f),
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = stringResource(Res.string.fan_curve_studio_title),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.fan_curve_studio_desc),
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun FanMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .width(130.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceContainerHigh)
                .border(1.dp, BorderOutline, RoundedCornerShape(8.dp))
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 11.sp,
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}
