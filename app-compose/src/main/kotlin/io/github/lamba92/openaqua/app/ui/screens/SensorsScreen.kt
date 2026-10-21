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
import io.github.lamba92.openaqua.app.feature.sensors.SensorsEvent
import io.github.lamba92.openaqua.app.feature.sensors.SensorsState
import io.github.lamba92.openaqua.app.feature.sensors.SensorsViewModel
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
import open_aqua.app_compose.generated.resources.ic_refresh
import open_aqua.app_compose.generated.resources.ic_sensor
import open_aqua.app_compose.generated.resources.ic_sensors
import open_aqua.app_compose.generated.resources.metric_live_temperature
import open_aqua.app_compose.generated.resources.metric_offset_calibration
import open_aqua.app_compose.generated.resources.metric_probe_type
import open_aqua.app_compose.generated.resources.refresh_telemetry
import open_aqua.app_compose.generated.resources.sensor_calibration_desc
import open_aqua.app_compose.generated.resources.sensor_calibration_title
import open_aqua.app_compose.generated.resources.sensor_hardware_channel
import open_aqua.app_compose.generated.resources.sensor_settings_title_suffix
import open_aqua.app_compose.generated.resources.sensors_screen_subtitle
import open_aqua.app_compose.generated.resources.sensors_screen_title
import open_aqua.app_compose.generated.resources.temperature_probes_header
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SensorsScreen(
    device: DetectedHardwareDevice,
    viewModel: SensorsViewModel,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(device) {
        viewModel.onEvent(SensorsEvent.LoadSensors(device))
    }

    val state by viewModel.state.collectAsState()

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
    ) {
        when (val s = state) {
            SensorsState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = CyanBright,
                )
            }
            is SensorsState.Error -> {
                Text(
                    text = s.message,
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is SensorsState.Ready -> {
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
                                text = stringResource(Res.string.sensors_screen_title),
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(Res.string.sensors_screen_subtitle),
                                color = TextMuted,
                                fontSize = 13.sp,
                            )
                        }

                        IconButton(onClick = { viewModel.onEvent(SensorsEvent.RefreshTelemetry) }) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_refresh),
                                contentDescription = stringResource(Res.string.refresh_telemetry),
                                tint = CyanBright,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    HorizontalDivider(color = BorderOutline)

                    // TOP ROW: Square buttons with sensor name below
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.temperature_probes_header),
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
                            s.sensors.forEach { sensor ->
                                val isSelected = sensor.channel == s.selectedChannel
                                val temp = sensor.telemetry?.tempCelsius
                                SquareSelectionButton(
                                    name = sensor.name,
                                    selected = isSelected,
                                    onClick = { viewModel.onEvent(SensorsEvent.SelectSensor(sensor.channel)) },
                                    badgeText = sensor.pinLabel,
                                    valueText = temp?.let { "${"%.1f".format(it)}°C" } ?: "--.-°C",
                                    icon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_sensor),
                                            contentDescription = sensor.name,
                                            tint = if (isSelected) CyanBright else (if (temp != null) CoolantTeal else TextMuted),
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = BorderOutline)

                    // SETTINGS PAGE BELOW (For selected sensor)
                    val selectedSensor = s.selectedSensor
                    if (selectedSensor != null) {
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
                                Column {
                                    Text(
                                        text = "${selectedSensor.name} ${stringResource(Res.string.sensor_settings_title_suffix)}",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = stringResource(Res.string.sensor_hardware_channel, selectedSensor.pinLabel),
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                    )
                                }
                            }

                            // Live metrics telemetry
                            val temp = selectedSensor.telemetry?.tempCelsius
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SensorMetricCard(
                                    label = stringResource(Res.string.metric_live_temperature),
                                    value = temp?.let { "${"%.2f".format(it)} °C" } ?: "Disconnected",
                                )
                                SensorMetricCard(
                                    label = stringResource(Res.string.metric_probe_type),
                                    value = if (selectedSensor.isVirtual) "Virtual Slot" else "NTC 10k \u03a9",
                                )
                                SensorMetricCard(
                                    label = stringResource(Res.string.metric_offset_calibration),
                                    value = "${"%.1f".format(selectedSensor.calibrationOffset)} °C",
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
                                        painter = painterResource(Res.drawable.ic_sensors),
                                        contentDescription = null,
                                        tint = ElectricCyan.copy(alpha = 0.7f),
                                        modifier = Modifier.size(32.dp),
                                    )
                                    Text(
                                        text = stringResource(Res.string.sensor_calibration_title),
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = stringResource(Res.string.sensor_calibration_desc),
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
private fun SensorMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .width(160.dp)
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
