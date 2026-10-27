package io.github.lamba92.openaqua.app.feature.device.sensors

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.lamba92.openaqua.app.di.rememberInstance
import io.github.lamba92.openaqua.app.feature.device.common.DeviceLoadingContent
import io.github.lamba92.openaqua.app.feature.device.common.SensorItem
import io.github.lamba92.openaqua.app.feature.device.common.canBeHidden
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.ChannelDetailHeader
import io.github.lamba92.openaqua.app.ui.components.ChannelPageScaffold
import io.github.lamba92.openaqua.app.ui.components.ChannelSelectionCard
import io.github.lamba92.openaqua.app.ui.components.ChannelSelectionTileDefaults
import io.github.lamba92.openaqua.app.ui.components.FeaturePlaceholderBox
import io.github.lamba92.openaqua.app.ui.components.MetricCard
import io.github.lamba92.openaqua.app.ui.components.OpenAquaOutlinedButton
import io.github.lamba92.openaqua.app.ui.components.RenameDialog
import io.github.lamba92.openaqua.app.ui.components.VisibilityEmptyState
import io.github.lamba92.openaqua.app.ui.components.VisibilityFilterChip
import io.github.lamba92.openaqua.app.ui.components.sensorTemperatureColor
import io.github.lamba92.openaqua.model.DeviceId
import io.github.lamba92.openaqua.model.SensorChannelKind
import io.github.lamba92.openaqua.model.tempCelsiusOrNull
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
import open_aqua.app_compose.generated.resources.visibility_no_sensors_visible
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SensorsContent(
    state: SensorsUiState.Ready,
    onEvent: (SensorsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val isSensorHidden: (SensorItem) -> Boolean = { sensor ->
        when (sensor.telemetry.kind) {
            SensorChannelKind.PHYSICAL_NTC -> state.hideDisconnectedSensors && sensor.canBeHidden
            SensorChannelKind.VIRTUAL -> state.hideUnconfiguredVirtualSensors && sensor.canBeHidden
            SensorChannelKind.INTEGRATED_COOLANT -> false
        }
    }

    val canBeHiddenCount =
        remember(state.sensors) {
            state.sensors.count { it.canBeHidden }
        }

    val actuallyHiddenCount =
        remember(state.sensors, state.hideDisconnectedSensors, state.hideUnconfiguredVirtualSensors) {
            state.sensors.count(isSensorHidden)
        }

    val displayedSensors =
        remember(
            state.sensors,
            state.hideDisconnectedSensors,
            state.hideUnconfiguredVirtualSensors,
        ) {
            state.sensors.filterNot(isSensorHidden)
        }

    val selectedSensor =
        displayedSensors.firstOrNull { it.channel == state.selectedSensorChannel }
            ?: displayedSensors.firstOrNull()
            ?: state.sensors.firstOrNull()

    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    val isShowingAll = !state.hideDisconnectedSensors && !state.hideUnconfiguredVirtualSensors

    ChannelPageScaffold(
        title = stringResource(Res.string.sensors_screen_title),
        subtitle = stringResource(Res.string.sensors_screen_subtitle),
        selectorHeader = stringResource(Res.string.temperature_probes_header),
        modifier = modifier,
        scrollState = scrollState,
        selectorHeaderTrailing =
            if (canBeHiddenCount > 0) {
                {
                    VisibilityFilterChip(
                        hiddenCount = if (isShowingAll) canBeHiddenCount else actuallyHiddenCount,
                        showingAll = isShowingAll,
                        onClick = { onEvent(SensorsUiEvent.ToggleShowAllSensors) },
                    )
                }
            } else {
                null
            },
        selectorContent = {
            displayedSensors.forEach { sensor ->
                val isSelected = sensor.channel == selectedSensor?.channel
                val temp = sensor.telemetry.tempCelsiusOrNull
                val sensorName = sensor.resolveName()
                val sensorPin = sensor.resolvePinLabel()
                SensorSelectionTile(
                    name = sensorName,
                    pinLabel = sensorPin,
                    tempCelsius = temp,
                    selected = isSelected,
                    onClick = { onEvent(SensorsUiEvent.SelectSensor(sensor.channel)) },
                    onDoubleClick = { onEvent(SensorsUiEvent.StartRenamingSensor(sensor.channel)) },
                )
            }
        },
        detailContent = {
            if (displayedSensors.isEmpty()) {
                VisibilityEmptyState(
                    message = stringResource(Res.string.visibility_no_sensors_visible),
                    onShowAll = { onEvent(SensorsUiEvent.ToggleShowAllSensors) },
                )
            } else if (selectedSensor != null) {
                val selectedSensorName = selectedSensor.resolveName()
                val selectedSensorPin = selectedSensor.resolvePinLabel()
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(metrics.radiusLarge))
                            .background(colors.surfaceLow)
                            .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusLarge))
                            .padding(metrics.spacingXxl),
                    verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
                ) {
                    ChannelDetailHeader(
                        icon = painterResource(Res.drawable.ic_tune),
                        title = stringResource(Res.string.sensor_settings_title, selectedSensorName),
                        subtitle = stringResource(Res.string.sensor_hardware_channel, selectedSensorPin),
                        trailingContent = {
                            OpenAquaOutlinedButton(
                                text = stringResource(Res.string.action_rename),
                                icon = painterResource(Res.drawable.ic_edit),
                                onClick = { onEvent(SensorsUiEvent.StartRenamingSensor(selectedSensor.channel)) },
                            )
                        },
                    )

                    // Live metrics telemetry
                    val temp = selectedSensor.telemetry.tempCelsiusOrNull
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.spacingMd),
                        verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
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

    val activeRenamingSensor = state.renamingSensor
    if (activeRenamingSensor != null) {
        RenameDialog(
            title = stringResource(Res.string.rename_dialog_title, activeRenamingSensor.resolveName()),
            initialValue = activeRenamingSensor.customName ?: "",
            icon = painterResource(Res.drawable.ic_edit),
            onConfirm = { newName ->
                onEvent(SensorsUiEvent.RenameSensor(activeRenamingSensor.channel, newName))
            },
            onDismiss = { onEvent(SensorsUiEvent.DismissRenameDialog) },
        )
    }
}

@Composable
fun SensorSelectionTile(
    name: String,
    pinLabel: String,
    tempCelsius: Double?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDoubleClick: (() -> Unit)? = null,
) {
    val colors = OpenAquaTheme.colors
    val metrics = ChannelSelectionTileDefaults.metrics()
    val isDisconnected = tempCelsius == null
    val fillFraction =
        if (isDisconnected) {
            0f
        } else {
            ((tempCelsius - 20.0) / 40.0).toFloat().coerceIn(0f, 1f)
        }

    val tempColor = sensorTemperatureColor(tempCelsius)
    val valueText =
        tempCelsius?.let {
            stringResource(Res.string.sensor_temp_short_value, "%.1f".format(it))
        } ?: stringResource(Res.string.sensor_temp_short_disconnected)

    ChannelSelectionCard(
        name = name,
        badgeText = pinLabel,
        valueText = valueText,
        secondaryValueText = null,
        selected = selected,
        onClick = onClick,
        onDoubleClick = onDoubleClick,
        fillFraction = fillFraction,
        fillColor = tempColor,
        modifier = modifier,
        metrics = metrics,
        icon = {
            Icon(
                painter = painterResource(Res.drawable.ic_sensor),
                contentDescription = name,
                tint = if (selected) colors.primaryBright else (if (!isDisconnected) tempColor else colors.textMuted),
                modifier = Modifier.size(metrics.iconSize),
            )
        },
    )
}

@Composable
private fun SensorItem.resolveName(): String =
    customName ?: when (telemetry.kind) {
        SensorChannelKind.PHYSICAL_NTC -> stringResource(Res.string.sensor_channel_name, channel + 1)
        SensorChannelKind.VIRTUAL -> stringResource(Res.string.sensor_virtual_name, channel + 1)
        SensorChannelKind.INTEGRATED_COOLANT -> stringResource(Res.string.sensor_coolant_name)
    }

@Composable
private fun SensorItem.resolvePinLabel(): String =
    when (telemetry.kind) {
        SensorChannelKind.PHYSICAL_NTC -> stringResource(Res.string.sensor_channel_pin, channel + 1)
        SensorChannelKind.VIRTUAL -> stringResource(Res.string.sensor_virtual_pin, channel + 1)
        SensorChannelKind.INTEGRATED_COOLANT -> stringResource(Res.string.sensor_coolant_pin)
    }

@Composable
private fun SensorItem.resolveProbeType(): String =
    when (telemetry.kind) {
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

@Composable
fun SensorsTabContent(
    deviceId: DeviceId,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) = SensorsTabContent(
    viewModel = rememberInstance(arg = deviceId),
    modifier = modifier,
    scrollState = scrollState,
)

@Composable
fun SensorsTabContent(
    viewModel: SensorsViewModel,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        SensorsUiState.Loading -> DeviceLoadingContent(modifier)
        is SensorsUiState.Ready -> {
            SensorsContent(
                state = s,
                onEvent = viewModel::onEvent,
                modifier = modifier,
                scrollState = scrollState,
            )
        }
    }
}
