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
import open_aqua.app_compose.generated.resources.ic_edit
import open_aqua.app_compose.generated.resources.ic_sensor
import open_aqua.app_compose.generated.resources.ic_sensors
import open_aqua.app_compose.generated.resources.ic_tune
import open_aqua.app_compose.generated.resources.metric_live_temperature
import open_aqua.app_compose.generated.resources.metric_offset_calibration
import open_aqua.app_compose.generated.resources.metric_probe_type
import open_aqua.app_compose.generated.resources.rename_dialog_title
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
    onRenameSensor: ((channel: Int, name: String?) -> Unit)? = null,
) {
    val selectedSensor = sensors.firstOrNull { it.channel == selectedChannel } ?: sensors.firstOrNull()
    var renamingSensor by remember { mutableStateOf<SensorItem?>(null) }

    ChannelPageScaffold(
        title = stringResource(Res.string.sensors_screen_title),
        subtitle = stringResource(Res.string.sensors_screen_subtitle),
        selectorHeader = stringResource(Res.string.temperature_probes_header),
        modifier = modifier,
        selectorRow = {
            sensors.forEach { sensor ->
                val isSelected = sensor.channel == selectedChannel
                val temp = sensor.telemetry.tempCelsius
                val sensorName = sensor.resolveName()
                val sensorPin = sensor.resolvePinLabel()
                SquareSelectionButton(
                    name = sensorName,
                    selected = isSelected,
                    onClick = { onSelectSensor(sensor.channel) },
                    onDoubleClick = onRenameSensor?.let { { renamingSensor = sensor } },
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
        },
        detailContent = {
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
                    ChannelDetailHeader(
                        icon = painterResource(Res.drawable.ic_tune),
                        title = stringResource(Res.string.sensor_settings_title, selectedSensorName),
                        subtitle = stringResource(Res.string.sensor_hardware_channel, selectedSensorPin),
                        trailingContent =
                            onRenameSensor?.let {
                                {
                                    OpenAquaOutlinedButton(
                                        text = stringResource(Res.string.action_rename),
                                        icon = painterResource(Res.drawable.ic_edit),
                                        onClick = { renamingSensor = selectedSensor },
                                    )
                                }
                            },
                    )

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
                    FeaturePlaceholderBox(
                        icon = painterResource(Res.drawable.ic_sensors),
                        title = stringResource(Res.string.sensor_calibration_title),
                        description = stringResource(Res.string.sensor_calibration_desc),
                    )
                }
            }
        },
    )

    val activeRenamingSensor = renamingSensor
    if (activeRenamingSensor != null && onRenameSensor != null) {
        RenameDialog(
            title = stringResource(Res.string.rename_dialog_title, activeRenamingSensor.resolveName()),
            initialValue = activeRenamingSensor.customName ?: "",
            icon = painterResource(Res.drawable.ic_edit),
            onConfirm = { newName ->
                onRenameSensor(activeRenamingSensor.channel, newName)
                renamingSensor = null
            },
            onDismiss = { renamingSensor = null },
        )
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
    MetricCard(
        label = label,
        value = value,
        width = 160.dp,
        modifier = modifier,
    )
}
