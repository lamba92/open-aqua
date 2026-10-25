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
import open_aqua.app_compose.generated.resources.ic_sensor
import open_aqua.app_compose.generated.resources.ic_sensors
import open_aqua.app_compose.generated.resources.ic_tune
import open_aqua.app_compose.generated.resources.metric_live_temperature
import open_aqua.app_compose.generated.resources.metric_offset_calibration
import open_aqua.app_compose.generated.resources.metric_probe_type
import open_aqua.app_compose.generated.resources.sensor_calibration_desc
import open_aqua.app_compose.generated.resources.sensor_calibration_title
import open_aqua.app_compose.generated.resources.sensor_channel_name
import open_aqua.app_compose.generated.resources.sensor_channel_pin
import open_aqua.app_compose.generated.resources.sensor_coolant_name
import open_aqua.app_compose.generated.resources.sensor_coolant_pin
import open_aqua.app_compose.generated.resources.sensor_hardware_channel
import open_aqua.app_compose.generated.resources.sensor_offset_value
import open_aqua.app_compose.generated.resources.sensor_probe_integrated
import open_aqua.app_compose.generated.resources.sensor_probe_ntc
import open_aqua.app_compose.generated.resources.sensor_probe_virtual
import open_aqua.app_compose.generated.resources.sensor_settings_title
import open_aqua.app_compose.generated.resources.sensor_temp_disconnected
import open_aqua.app_compose.generated.resources.sensor_temp_long_value
import open_aqua.app_compose.generated.resources.sensor_temp_short_disconnected
import open_aqua.app_compose.generated.resources.sensor_temp_short_value
import open_aqua.app_compose.generated.resources.sensor_virtual_name
import open_aqua.app_compose.generated.resources.sensor_virtual_pin
import open_aqua.app_compose.generated.resources.sensors_screen_subtitle
import open_aqua.app_compose.generated.resources.sensors_screen_title
import open_aqua.app_compose.generated.resources.temperature_probes_header
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SensorsContent(
    sensors: List<SensorItem>,
    selectedChannel: Int,
    onSelectSensor: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedSensor = sensors.firstOrNull { it.channel == selectedChannel } ?: sensors.firstOrNull()

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
                    sensors.forEach { sensor ->
                        val isSelected = sensor.channel == selectedChannel
                        val temp = sensor.telemetry.tempCelsius
                        val sensorName = sensor.resolveName()
                        val sensorPin = sensor.resolvePinLabel()
                        SquareSelectionButton(
                            name = sensorName,
                            selected = isSelected,
                            onClick = { onSelectSensor(sensor.channel) },
                            badgeText = sensorPin,
                            valueText =
                                temp?.let {
                                    stringResource(Res.string.sensor_temp_short_value, "%.1f".format(it))
                                } ?: stringResource(Res.string.sensor_temp_short_disconnected),
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_sensor),
                                    contentDescription = sensorName,
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
            if (selectedSensor != null) {
                val selectedSensorName = selectedSensor.resolveName()
                val selectedSensorPin = selectedSensor.resolvePinLabel()
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
                                    text = stringResource(Res.string.sensor_settings_title, selectedSensorName),
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = stringResource(Res.string.sensor_hardware_channel, selectedSensorPin),
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }

                    // Live metrics telemetry
                    val temp = selectedSensor.telemetry.tempCelsius
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SensorMetricCard(
                            label = stringResource(Res.string.metric_live_temperature),
                            value =
                                temp?.let {
                                    stringResource(Res.string.sensor_temp_long_value, "%.2f".format(it))
                                } ?: stringResource(Res.string.sensor_temp_disconnected),
                        )
                        SensorMetricCard(
                            label = stringResource(Res.string.metric_probe_type),
                            value = selectedSensor.resolveProbeType(),
                        )
                        SensorMetricCard(
                            label = stringResource(Res.string.metric_offset_calibration),
                            value =
                                stringResource(
                                    Res.string.sensor_offset_value,
                                    "%.1f".format(selectedSensor.calibrationOffset),
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

@Composable
private fun SensorItem.resolveName(): String =
    customName ?: when (kind) {
        SensorChannelKind.PHYSICAL_NTC -> stringResource(Res.string.sensor_channel_name, channel + 1)
        SensorChannelKind.VIRTUAL -> stringResource(Res.string.sensor_virtual_name, channel + 1)
        SensorChannelKind.INTEGRATED_COOLANT -> stringResource(Res.string.sensor_coolant_name)
    }

@Composable
private fun SensorItem.resolvePinLabel(): String =
    when (kind) {
        SensorChannelKind.PHYSICAL_NTC -> stringResource(Res.string.sensor_channel_pin, channel + 1)
        SensorChannelKind.VIRTUAL -> stringResource(Res.string.sensor_virtual_pin, channel + 1)
        SensorChannelKind.INTEGRATED_COOLANT -> stringResource(Res.string.sensor_coolant_pin)
    }

@Composable
private fun SensorItem.resolveProbeType(): String =
    when (kind) {
        SensorChannelKind.PHYSICAL_NTC -> stringResource(Res.string.sensor_probe_ntc)
        SensorChannelKind.VIRTUAL -> stringResource(Res.string.sensor_probe_virtual)
        SensorChannelKind.INTEGRATED_COOLANT -> stringResource(Res.string.sensor_probe_integrated)
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
