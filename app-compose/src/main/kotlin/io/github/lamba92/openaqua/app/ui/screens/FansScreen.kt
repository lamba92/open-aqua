package io.github.lamba92.openaqua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.feature.fans.FansEvent
import io.github.lamba92.openaqua.app.feature.fans.FansState
import io.github.lamba92.openaqua.app.feature.fans.FansViewModel
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
import io.github.lamba92.openaqua.model.DetectedHardwareDevice
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.fan_channels_header
import open_aqua.app_compose.generated.resources.fan_curve_studio_desc
import open_aqua.app_compose.generated.resources.fan_curve_studio_title
import open_aqua.app_compose.generated.resources.fan_hardware_pin
import open_aqua.app_compose.generated.resources.fan_settings_title_suffix
import open_aqua.app_compose.generated.resources.fans_screen_subtitle
import open_aqua.app_compose.generated.resources.fans_screen_title
import open_aqua.app_compose.generated.resources.ic_fan
import open_aqua.app_compose.generated.resources.ic_refresh
import open_aqua.app_compose.generated.resources.ic_speed
import open_aqua.app_compose.generated.resources.ic_tune
import open_aqua.app_compose.generated.resources.metric_current
import open_aqua.app_compose.generated.resources.metric_power
import open_aqua.app_compose.generated.resources.metric_pwm_duty
import open_aqua.app_compose.generated.resources.metric_speed
import open_aqua.app_compose.generated.resources.metric_voltage
import open_aqua.app_compose.generated.resources.refresh_telemetry
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FansScreen(
    device: DetectedHardwareDevice,
    viewModel: FansViewModel,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(device) {
        viewModel.onEvent(FansEvent.LoadFans(device))
    }

    val state by viewModel.state.collectAsState()

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
    ) {
        when (val s = state) {
            FansState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = CyanBright,
                )
            }
            is FansState.Error -> {
                Text(
                    text = s.message,
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is FansState.Ready -> {
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

                        IconButton(onClick = { viewModel.onEvent(FansEvent.RefreshTelemetry) }) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_refresh),
                                contentDescription = stringResource(Res.string.refresh_telemetry),
                                tint = CyanBright,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    HorizontalDivider(color = BorderOutline)

                    // TOP ROW: Square buttons with fan name below
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
                        ) {
                            s.fans.forEach { fan ->
                                val isSelected = fan.channel == s.selectedChannel
                                val rpm = fan.telemetry?.rpm ?: 0
                                SquareSelectionButton(
                                    name = fan.name,
                                    selected = isSelected,
                                    onClick = { viewModel.onEvent(FansEvent.SelectFan(fan.channel)) },
                                    badgeText = fan.pinLabel,
                                    valueText = if (rpm > 0) "$rpm RPM" else "0 RPM",
                                    icon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_fan),
                                            contentDescription = fan.name,
                                            tint = if (isSelected) CyanBright else (if (rpm > 0) CoolantTeal else TextMuted),
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = BorderOutline)

                    // SETTINGS PAGE BELOW (For selected fan)
                    val selectedFan = s.selectedFan
                    if (selectedFan != null) {
                        Column(
                            modifier =
                                Modifier
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
                                            text = "${selectedFan.name} ${stringResource(Res.string.fan_settings_title_suffix)}",
                                            color = TextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = stringResource(Res.string.fan_hardware_pin, selectedFan.pinLabel),
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }

                            // Live metrics telemetry
                            val tel = selectedFan.telemetry
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                MetricCard(
                                    label = stringResource(Res.string.metric_speed),
                                    value = "${tel?.rpm ?: 0} RPM",
                                )
                                MetricCard(
                                    label = stringResource(Res.string.metric_pwm_duty),
                                    value = "${tel?.pwmPercent?.let { "%.1f".format(it) } ?: "0.0"}%",
                                )
                                MetricCard(
                                    label = stringResource(Res.string.metric_voltage),
                                    value = "${tel?.voltageVolts?.let { "%.2f".format(it) } ?: "0.00"} V",
                                )
                                MetricCard(
                                    label = stringResource(Res.string.metric_current),
                                    value = "${tel?.currentAmps?.let { "%.3f".format(it) } ?: "0.000"} A",
                                )
                                MetricCard(
                                    label = stringResource(Res.string.metric_power),
                                    value = "${tel?.powerWatts?.let { "%.2f".format(it) } ?: "0.00"} W",
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
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
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
