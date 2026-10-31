package io.github.lamba92.openaqua.app.feature.device.sensors

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.di.rememberInstance
import io.github.lamba92.openaqua.app.feature.device.common.DeviceLoadingContent
import io.github.lamba92.openaqua.app.feature.device.common.SensorItem
import io.github.lamba92.openaqua.app.feature.device.common.canBeHidden
import io.github.lamba92.openaqua.app.feature.device.common.effectiveTempCelsius
import io.github.lamba92.openaqua.app.feature.device.common.feederStatus
import io.github.lamba92.openaqua.app.feature.device.common.virtualSensorIndex
import io.github.lamba92.openaqua.app.model.SensorTargetType
import io.github.lamba92.openaqua.app.model.VirtualSensorFeedError
import io.github.lamba92.openaqua.app.model.VirtualSensorFeedSource
import io.github.lamba92.openaqua.app.model.color
import io.github.lamba92.openaqua.app.model.progressFraction
import io.github.lamba92.openaqua.app.theme.CoolantTeal
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.ChannelDetailHeader
import io.github.lamba92.openaqua.app.ui.components.ChannelPageScaffold
import io.github.lamba92.openaqua.app.ui.components.ChannelSelectionCard
import io.github.lamba92.openaqua.app.ui.components.ChannelSelectionTileDefaults
import io.github.lamba92.openaqua.app.ui.components.FeederStatus
import io.github.lamba92.openaqua.app.ui.components.FeederStatusBadge
import io.github.lamba92.openaqua.app.ui.components.FeederStatusBadgeStyle
import io.github.lamba92.openaqua.app.ui.components.MetricCard
import io.github.lamba92.openaqua.app.ui.components.OpenAquaButton
import io.github.lamba92.openaqua.app.ui.components.OpenAquaDropdownMenu
import io.github.lamba92.openaqua.app.ui.components.OpenAquaDropdownMenuItem
import io.github.lamba92.openaqua.app.ui.components.OpenAquaOutlinedButton
import io.github.lamba92.openaqua.app.ui.components.RenameDialog
import io.github.lamba92.openaqua.app.ui.components.VisibilityEmptyState
import io.github.lamba92.openaqua.app.ui.components.VisibilityFilterChip
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.SensorChannelKind
import io.github.lamba92.openaqua.model.device.SensorTelemetry
import io.github.lamba92.openaqua.model.device.tempCelsiusOrNull
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.action_rename
import open_aqua.app_compose.generated.resources.ic_computer
import open_aqua.app_compose.generated.resources.ic_device
import open_aqua.app_compose.generated.resources.ic_edit
import open_aqua.app_compose.generated.resources.ic_sensor
import open_aqua.app_compose.generated.resources.ic_sensors
import open_aqua.app_compose.generated.resources.ic_tune
import open_aqua.app_compose.generated.resources.ic_warning
import open_aqua.app_compose.generated.resources.metric_live_temperature
import open_aqua.app_compose.generated.resources.metric_offset_calibration
import open_aqua.app_compose.generated.resources.metric_probe_type
import open_aqua.app_compose.generated.resources.rename_dialog_title
import open_aqua.app_compose.generated.resources.sensor_channel_name
import open_aqua.app_compose.generated.resources.sensor_channel_pin
import open_aqua.app_compose.generated.resources.sensor_coolant_name
import open_aqua.app_compose.generated.resources.sensor_coolant_pin
import open_aqua.app_compose.generated.resources.sensor_feeder_apply
import open_aqua.app_compose.generated.resources.sensor_feeder_desc
import open_aqua.app_compose.generated.resources.sensor_feeder_error_generic
import open_aqua.app_compose.generated.resources.sensor_feeder_error_host_disconnected
import open_aqua.app_compose.generated.resources.sensor_feeder_error_host_sensor_disconnected
import open_aqua.app_compose.generated.resources.sensor_feeder_error_source_device_unavailable
import open_aqua.app_compose.generated.resources.sensor_feeder_error_source_sensor_disconnected
import open_aqua.app_compose.generated.resources.sensor_feeder_error_target_disconnected
import open_aqua.app_compose.generated.resources.sensor_feeder_error_title
import open_aqua.app_compose.generated.resources.sensor_feeder_forwarding
import open_aqua.app_compose.generated.resources.sensor_feeder_idle_desc
import open_aqua.app_compose.generated.resources.sensor_feeder_no_device_sensors
import open_aqua.app_compose.generated.resources.sensor_feeder_no_devices
import open_aqua.app_compose.generated.resources.sensor_feeder_no_host_sensors
import open_aqua.app_compose.generated.resources.sensor_feeder_source_device
import open_aqua.app_compose.generated.resources.sensor_feeder_source_host
import open_aqua.app_compose.generated.resources.sensor_feeder_source_label
import open_aqua.app_compose.generated.resources.sensor_feeder_source_none
import open_aqua.app_compose.generated.resources.sensor_feeder_status_error
import open_aqua.app_compose.generated.resources.sensor_feeder_status_idle
import open_aqua.app_compose.generated.resources.sensor_feeder_status_streaming
import open_aqua.app_compose.generated.resources.sensor_feeder_target_label
import open_aqua.app_compose.generated.resources.sensor_feeder_title
import open_aqua.app_compose.generated.resources.sensor_hardware_channel
import open_aqua.app_compose.generated.resources.sensor_offset_desc
import open_aqua.app_compose.generated.resources.sensor_offset_flash
import open_aqua.app_compose.generated.resources.sensor_offset_title
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
import kotlin.math.round

@Composable
fun SensorsContent(
    state: SensorsUiState.Ready,
    onEvent: (SensorsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    val isHidingAny = state.hideDisconnectedSensors || state.hideUnconfiguredVirtualSensors

    val isSensorHidden =
        remember<(SensorItem) -> Boolean>(state.hideDisconnectedSensors, state.hideUnconfiguredVirtualSensors) {
            { item ->
                when (item.telemetry.kind) {
                    SensorChannelKind.INTEGRATED_COOLANT -> false
                    SensorChannelKind.PHYSICAL_NTC ->
                        state.hideDisconnectedSensors && item.telemetry is SensorTelemetry.Disconnected
                    SensorChannelKind.VIRTUAL ->
                        state.hideUnconfiguredVirtualSensors &&
                            item.effectiveTempCelsius == null &&
                            item.virtualFeedSource == null
                }
            }
        }

    val hiddenCount =
        remember(state.sensors) {
            state.sensors.count { it.canBeHidden }
        }

    val displayedSensors =
        remember(state.sensors, state.hideDisconnectedSensors, state.hideUnconfiguredVirtualSensors) {
            if (isHidingAny) {
                state.sensors.filterNot(isSensorHidden)
            } else {
                state.sensors
            }
        }

    val selectedSensor =
        displayedSensors.firstOrNull { it.channel == state.selectedSensorChannel }
            ?: displayedSensors.firstOrNull()
            ?: state.sensors.firstOrNull()

    ChannelPageScaffold(
        title = stringResource(Res.string.sensors_screen_title),
        subtitle = stringResource(Res.string.sensors_screen_subtitle),
        selectorHeader = stringResource(Res.string.temperature_probes_header),
        selectorHeaderTrailing =
            if (hiddenCount > 0) {
                {
                    VisibilityFilterChip(
                        hiddenCount = hiddenCount,
                        showingAll = !isHidingAny,
                        onClick = { onEvent(SensorsUiEvent.ToggleShowAllSensors) },
                    )
                }
            } else {
                null
            },
        selectorContent = {
            displayedSensors.forEach { sensor ->
                val isSelected = sensor.channel == selectedSensor?.channel
                val temp = sensor.effectiveTempCelsius
                val sensorName = sensor.resolveName()
                val sensorPin = sensor.resolvePinLabel()
                SensorSelectionTile(
                    name = sensorName,
                    pinLabel = sensorPin,
                    tempCelsius = temp,
                    selected = isSelected,
                    hasPendingChanges = state.isSensorModified(sensor),
                    feederStatus = sensor.feederStatus,
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
                    verticalArrangement = Arrangement.spacedBy(metrics.spacingXl),
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
                    val temp = selectedSensor.effectiveTempCelsius
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
                        if (selectedSensor.telemetry.kind != SensorChannelKind.VIRTUAL) {
                            SensorMetricCard(
                                label = stringResource(Res.string.metric_offset_calibration),
                                value =
                                    stringResource(
                                        Res.string.sensor_offset_value,
                                        "%.1f".format(selectedSensor.calibrationOffset),
                                    ),
                            )
                        }
                    }

                    if (selectedSensor.telemetry.kind != SensorChannelKind.VIRTUAL) {
                        SensorOffsetCalibrationCard(
                            sensor = selectedSensor,
                            offsetValue = state.effectiveOffset(selectedSensor),
                            isModified = state.isSensorOffsetModified(selectedSensor),
                            isFlashing = state.isFlashingOffset,
                            onOffsetChange = { offset ->
                                onEvent(SensorsUiEvent.UpdateOffsetDraft(selectedSensor.channel, offset))
                            },
                            onFlashOffset = { onEvent(SensorsUiEvent.FlashOffset(selectedSensor.channel)) },
                        )
                    } else {
                        VirtualSensorFeederCard(
                            sensor = selectedSensor,
                            draft = state.effectiveVirtualFeedDraft(selectedSensor),
                            isModified = state.isVirtualFeedModified(selectedSensor),
                            isApplying = state.isApplyingVirtualFeed,
                            availableHostSensors = state.availableHostSensors,
                            availableAquaDevices = state.availableAquaDevices,
                            onDraftChange = { draft ->
                                onEvent(SensorsUiEvent.UpdateVirtualFeedDraft(selectedSensor.channel, draft))
                            },
                            onApply = { onEvent(SensorsUiEvent.ApplyVirtualFeed(selectedSensor.channel)) },
                        )
                    }
                }
            }
        },
        scrollState = scrollState,
        modifier = modifier,
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
private fun CardHeaderTitle(
    icon: Painter,
    title: String,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = colors.primaryBright,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SensorDropdownMenuItem(
    name: String,
    temperature: Double?,
    onClick: () -> Unit,
    enabled: Boolean = temperature != null,
) {
    val colors = OpenAquaTheme.colors
    OpenAquaDropdownMenuItem(
        enabled = enabled,
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = name,
                    fontSize = 12.sp,
                    color = if (enabled) colors.textPrimary else colors.textMuted,
                    fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                    modifier = Modifier.weight(1f, fill = false),
                    softWrap = true,
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = temperature?.let { "%.2f °C".format(it) } ?: "--.- °C",
                    fontSize = 12.sp,
                    color = if (enabled) colors.primaryBright else colors.textMuted,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                )
            }
        },
        onClick = onClick,
    )
}

@Composable
fun SensorOffsetCalibrationCard(
    sensor: SensorItem,
    offsetValue: Double,
    isModified: Boolean,
    isFlashing: Boolean,
    onOffsetChange: (Double) -> Unit,
    onFlashOffset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    var textInput by remember(offsetValue) {
        mutableStateOf("%.1f".format(offsetValue))
    }

    val rawTemp = sensor.telemetry.tempCelsiusOrNull?.let { it - sensor.calibrationOffset }
    val calibratedTemp = rawTemp?.let { it + offsetValue }
    val isOutOfRange = offsetValue !in -15.0..15.0

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(metrics.radiusMedium))
                .background(colors.surfaceContainer)
                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusMedium))
                .padding(metrics.spacingLg),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardHeaderTitle(
                icon = painterResource(Res.drawable.ic_tune),
                title = stringResource(Res.string.sensor_offset_title),
            )
        }

        Text(
            text = stringResource(Res.string.sensor_offset_desc),
            color = colors.textMuted,
            fontSize = 12.sp,
        )

        // Offset Editor Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Text Input & Stepper Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { input ->
                        val sanitized = input.replace(',', '.').filter { it.isDigit() || it == '.' || it == '-' || it == '+' }
                        textInput = sanitized
                        val parsed = sanitized.toDoubleOrNull()
                        if (parsed != null) {
                            onOffsetChange(parsed)
                        }
                    },
                    modifier = Modifier.width(110.dp),
                    singleLine = true,
                    isError = isOutOfRange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = {
                        Text(
                            text = "°C",
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                        )
                    },
                    shape = RoundedCornerShape(metrics.radiusSmall),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primaryBright,
                            unfocusedBorderColor = colors.borderOutline,
                            errorBorderColor = colors.statusCritical,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            errorTextColor = colors.statusCritical,
                            cursorColor = colors.primaryBright,
                            focusedContainerColor = colors.surfaceLowest,
                            unfocusedContainerColor = colors.surfaceLowest,
                        ),
                )

                // Steppers
                val steppers = listOf(-1.0, -0.1, 0.0, 0.1, 1.0)
                steppers.forEach { step ->
                    val label =
                        when {
                            step == 0.0 -> "Reset"
                            step > 0 -> "+%.1f".format(step)
                            else -> "%.1f".format(step)
                        }
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(metrics.radiusSmall))
                                .background(colors.surfaceHigh)
                                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusSmall))
                                .clickable {
                                    val nextOffset = if (step == 0.0) 0.0 else round((offsetValue + step) * 10.0) / 10.0
                                    onOffsetChange(nextOffset)
                                    textInput = "%.1f".format(nextOffset)
                                }.padding(horizontal = 8.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = label,
                            color = colors.textPrimary,
                            fontSize = 11.sp,
                            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                        )
                    }
                }
            }

            OpenAquaButton(
                text = stringResource(Res.string.sensor_offset_flash),
                isLoading = isFlashing,
                enabled = isModified && !isFlashing,
                onClick = onFlashOffset,
            )
        }

        // Live Math Preview Box
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(metrics.radiusSmall))
                    .background(colors.surfaceLowest)
                    .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusSmall))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(metrics.spacingMd),
        ) {
            Text(
                text = "Raw Sensor: ${rawTemp?.let { "%.2f °C".format(it) } ?: "--.- °C"}",
                color = colors.textMuted,
                fontSize = 12.sp,
                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
            )
            Text(
                text = "+",
                color = colors.textMuted,
                fontSize = 12.sp,
            )
            Text(
                text = "Offset: ${if (offsetValue >= 0) '+' else ""}${ "%.1f °C".format(offsetValue) }",
                color = colors.primaryBright,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
            )
            Text(
                text = "=",
                color = colors.textMuted,
                fontSize = 12.sp,
            )
            val calibratedColor = SensorTargetType.WATER.color(calibratedTemp)
            Text(
                text = "Calibrated: ${calibratedTemp?.let { "%.2f °C".format(it) } ?: "--.- °C"}",
                color = calibratedColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
            )
        }
    }
}

@Composable
fun VirtualSensorFeederCard(
    sensor: SensorItem,
    draft: VirtualSensorFeedDraft,
    isModified: Boolean,
    isApplying: Boolean,
    availableHostSensors: List<HostSensorOption>,
    availableAquaDevices: List<AquaDeviceOption>,
    onDraftChange: (VirtualSensorFeedDraft) -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    var hostMenuExpanded by remember { mutableStateOf(false) }
    var deviceMenuExpanded by remember { mutableStateOf(false) }
    var deviceSensorMenuExpanded by remember { mutableStateOf(false) }
    var hideDisconnectedSources by remember { mutableStateOf(true) }

    val density = LocalDensity.current
    var hostTriggerWidthDp by remember { mutableStateOf(0.dp) }
    var deviceTriggerWidthDp by remember { mutableStateOf(0.dp) }
    var deviceSensorTriggerWidthDp by remember { mutableStateOf(0.dp) }

    val selectedHostSensorId = (draft as? VirtualSensorFeedDraft.Host)?.sensorId
    val selectedDeviceId = (draft as? VirtualSensorFeedDraft.Device)?.deviceId
    val selectedDeviceChannel = (draft as? VirtualSensorFeedDraft.Device)?.channel

    val currentSelectedDevice =
        availableAquaDevices.firstOrNull { it.deviceId == selectedDeviceId }
            ?: availableAquaDevices.firstOrNull()

    val disconnectedSourceCount =
        when (draft) {
            is VirtualSensorFeedDraft.None -> 0
            is VirtualSensorFeedDraft.Host -> availableHostSensors.count { it.liveTemp == null }
            is VirtualSensorFeedDraft.Device -> currentSelectedDevice?.sensors.orEmpty().count { it.liveTemp == null }
        }

    val displayedHostSensors =
        if (hideDisconnectedSources) {
            availableHostSensors.filter { it.liveTemp != null || it.id == selectedHostSensorId }
        } else {
            availableHostSensors
        }

    val displayedDeviceSensors =
        currentSelectedDevice?.sensors.orEmpty().let { deviceSensors ->
            if (hideDisconnectedSources) {
                deviceSensors.filter { it.liveTemp != null || it.channel == selectedDeviceChannel }
            } else {
                deviceSensors
            }
        }

    val activeSourceTemp =
        when (draft) {
            is VirtualSensorFeedDraft.None -> null
            is VirtualSensorFeedDraft.Host ->
                availableHostSensors.firstOrNull { it.id == draft.sensorId }?.liveTemp
            is VirtualSensorFeedDraft.Device ->
                currentSelectedDevice?.sensors?.firstOrNull { it.channel == draft.channel }?.liveTemp
        }

    val activeSourceName =
        when (draft) {
            is VirtualSensorFeedDraft.None -> stringResource(Res.string.sensor_feeder_source_none)
            is VirtualSensorFeedDraft.Host ->
                availableHostSensors.firstOrNull { it.id == draft.sensorId }?.name
                    ?: draft.sensorId?.value ?: ""
            is VirtualSensorFeedDraft.Device ->
                "${currentSelectedDevice?.name.orEmpty()} → ${currentSelectedDevice?.sensors?.firstOrNull { it.channel == draft.channel }?.name.orEmpty()}"
        }

    val feedError = sensor.feedError
    val feederStatus = sensor.feederStatus
    val statusStyle = FeederStatusBadgeStyle.forStatus(feederStatus)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(metrics.radiusMedium))
                .background(colors.surfaceContainer)
                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusMedium))
                .padding(metrics.spacingLg),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardHeaderTitle(
                icon = painterResource(Res.drawable.ic_sensors),
                title = stringResource(Res.string.sensor_feeder_title),
            )

            // Status Indicator Pill
            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(metrics.radiusSmall))
                        .background(statusStyle.containerColor)
                        .border(metrics.strokeThin, statusStyle.borderColor, RoundedCornerShape(metrics.radiusSmall))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FeederStatusBadge(
                    status = feederStatus,
                    size = 7.dp,
                    style = statusStyle,
                )
                Text(
                    text =
                        when (feederStatus) {
                            FeederStatus.OPERATIONAL -> stringResource(Res.string.sensor_feeder_status_streaming)
                            FeederStatus.ERROR -> stringResource(Res.string.sensor_feeder_status_error)
                            FeederStatus.NONE -> stringResource(Res.string.sensor_feeder_status_idle)
                        },
                    color = statusStyle.contentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Text(
            text = stringResource(Res.string.sensor_feeder_desc),
            color = colors.textMuted,
            fontSize = 12.sp,
        )

        if (feedError != null) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(metrics.radiusSmall))
                        .background(colors.statusCritical.copy(alpha = 0.12f))
                        .border(metrics.strokeThin, colors.statusCritical.copy(alpha = 0.5f), RoundedCornerShape(metrics.radiusSmall))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_warning),
                    contentDescription = null,
                    tint = colors.statusCritical,
                    modifier = Modifier.size(18.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.sensor_feeder_error_title),
                        color = colors.statusCritical,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = feedError.resolveMessage(),
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // Source Type Selector Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isNone = draft is VirtualSensorFeedDraft.None
            val isHost = draft is VirtualSensorFeedDraft.Host
            val isDevice = draft is VirtualSensorFeedDraft.Device

            val pills =
                listOf(
                    Triple(stringResource(Res.string.sensor_feeder_source_none), isNone) {
                        onDraftChange(VirtualSensorFeedDraft.None)
                    },
                    Triple(stringResource(Res.string.sensor_feeder_source_host), isHost) {
                        if (!isHost) {
                            val firstHost =
                                availableHostSensors.firstOrNull { it.liveTemp != null }
                                    ?: availableHostSensors.firstOrNull()
                            onDraftChange(VirtualSensorFeedDraft.Host(firstHost?.id))
                        }
                    },
                    Triple(stringResource(Res.string.sensor_feeder_source_device), isDevice) {
                        if (!isDevice) {
                            val firstDev = availableAquaDevices.firstOrNull()
                            val firstCh =
                                (firstDev?.sensors?.firstOrNull { it.liveTemp != null } ?: firstDev?.sensors?.firstOrNull())
                                    ?.channel
                            onDraftChange(VirtualSensorFeedDraft.Device(firstDev?.deviceId, firstCh))
                        }
                    },
                )

            pills.forEach { (label, isSelected, onClick) ->
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(metrics.radiusSmall))
                            .background(if (isSelected) colors.primaryBright.copy(alpha = 0.15f) else colors.surfaceLowest)
                            .border(
                                metrics.strokeThin,
                                if (isSelected) colors.primaryBright else colors.borderOutline,
                                RoundedCornerShape(metrics.radiusSmall),
                            ).clickable(onClick = onClick)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) colors.primaryBright else colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }

            if (disconnectedSourceCount > 0) {
                Spacer(Modifier.weight(1f))
                VisibilityFilterChip(
                    hiddenCount = disconnectedSourceCount,
                    showingAll = !hideDisconnectedSources,
                    onClick = { hideDisconnectedSources = !hideDisconnectedSources },
                )
            }
        }

        // Source Dropdown Selector based on chosen type
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(metrics.spacingMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (draft) {
                is VirtualSensorFeedDraft.None -> {
                    Text(
                        text = stringResource(Res.string.sensor_feeder_idle_desc),
                        color = colors.textMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                }

                is VirtualSensorFeedDraft.Host -> {
                    Box(modifier = Modifier.weight(1f)) {
                        val selectedOption = availableHostSensors.firstOrNull { it.id == selectedHostSensorId }
                        val displayHostName =
                            selectedOption?.name
                                ?: selectedHostSensorId?.value
                                ?: stringResource(Res.string.sensor_feeder_no_host_sensors)
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { hostTriggerWidthDp = with(density) { it.width.toDp() } }
                                    .clip(RoundedCornerShape(metrics.radiusSmall))
                                    .background(colors.surfaceLowest)
                                    .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusSmall))
                                    .clickable(enabled = availableHostSensors.isNotEmpty()) { hostMenuExpanded = true }
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_computer),
                                    contentDescription = null,
                                    tint = colors.primaryBright,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = displayHostName,
                                    color = colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        if (availableHostSensors.isNotEmpty()) {
                            OpenAquaDropdownMenu(
                                expanded = hostMenuExpanded,
                                onDismissRequest = { hostMenuExpanded = false },
                                modifier = Modifier.width(hostTriggerWidthDp),
                            ) {
                                displayedHostSensors.forEach { item ->
                                    SensorDropdownMenuItem(
                                        name = item.name,
                                        temperature = item.liveTemp,
                                        onClick = {
                                            onDraftChange(VirtualSensorFeedDraft.Host(item.id))
                                            hostMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                is VirtualSensorFeedDraft.Device -> {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Device Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            val deviceName = currentSelectedDevice?.name ?: stringResource(Res.string.sensor_feeder_no_devices)

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .onSizeChanged { deviceTriggerWidthDp = with(density) { it.width.toDp() } }
                                        .clip(RoundedCornerShape(metrics.radiusSmall))
                                        .background(colors.surfaceLowest)
                                        .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusSmall))
                                        .clickable(enabled = availableAquaDevices.isNotEmpty()) { deviceMenuExpanded = true }
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_device),
                                        contentDescription = null,
                                        tint = colors.primaryBright,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = deviceName,
                                        color = colors.textPrimary,
                                        fontSize = 12.sp,
                                        fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            if (availableAquaDevices.isNotEmpty()) {
                                OpenAquaDropdownMenu(
                                    expanded = deviceMenuExpanded,
                                    onDismissRequest = { deviceMenuExpanded = false },
                                    modifier = Modifier.width(deviceTriggerWidthDp),
                                ) {
                                    availableAquaDevices.forEach { dev ->
                                        OpenAquaDropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = dev.name,
                                                    fontSize = 12.sp,
                                                    color = colors.textPrimary,
                                                    fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                                                )
                                            },
                                            onClick = {
                                                val defaultChannel = dev.sensors.firstOrNull()?.channel
                                                onDraftChange(VirtualSensorFeedDraft.Device(dev.deviceId, defaultChannel))
                                                deviceMenuExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        // Sensor Channel Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            val selectedChannelItem = currentSelectedDevice?.sensors?.firstOrNull { it.channel == selectedDeviceChannel }
                            val channelName = selectedChannelItem?.name ?: stringResource(Res.string.sensor_feeder_no_device_sensors)

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .onSizeChanged { deviceSensorTriggerWidthDp = with(density) { it.width.toDp() } }
                                        .clip(RoundedCornerShape(metrics.radiusSmall))
                                        .background(colors.surfaceLowest)
                                        .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusSmall))
                                        .clickable(enabled = currentSelectedDevice?.sensors.orEmpty().isNotEmpty()) {
                                            deviceSensorMenuExpanded = true
                                        }.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = channelName,
                                    color = colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(18.dp),
                                )
                            }

                            if (currentSelectedDevice?.sensors.orEmpty().isNotEmpty()) {
                                OpenAquaDropdownMenu(
                                    expanded = deviceSensorMenuExpanded,
                                    onDismissRequest = { deviceSensorMenuExpanded = false },
                                    modifier = Modifier.width(deviceSensorTriggerWidthDp),
                                ) {
                                    displayedDeviceSensors.forEach { sensorItem ->
                                        SensorDropdownMenuItem(
                                            name = sensorItem.name,
                                            temperature = sensorItem.liveTemp,
                                            onClick = {
                                                val devId = currentSelectedDevice?.deviceId
                                                onDraftChange(VirtualSensorFeedDraft.Device(devId, sensorItem.channel))
                                                deviceSensorMenuExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            OpenAquaButton(
                text = stringResource(Res.string.sensor_feeder_apply),
                isLoading = isApplying,
                enabled = isModified && !isApplying,
                onClick = onApply,
            )
        }

        // Live Feed Pipeline Visualizer
        if (draft !is VirtualSensorFeedDraft.None && activeSourceTemp != null) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(metrics.radiusSmall))
                        .background(colors.surfaceLowest)
                        .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusSmall))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
            ) {
                Text(
                    text = stringResource(Res.string.sensor_feeder_source_label, activeSourceName),
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                )
                Text(
                    text = "→",
                    color = colors.primaryBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(Res.string.sensor_feeder_forwarding, "%.2f".format(activeSourceTemp)),
                    color = colors.primaryBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                )
                Text(
                    text = "→",
                    color = colors.primaryBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(Res.string.sensor_feeder_target_label, sensor.channel + 1),
                    color = colors.textPrimary,
                    fontSize = 11.sp,
                    fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                )
            }
        }
    }
}

@Composable
private fun VirtualSensorFeedError.resolveMessage(): String =
    when (this) {
        is VirtualSensorFeedError.SourceDeviceUnavailable ->
            when (source) {
                is VirtualSensorFeedSource.Host ->
                    stringResource(Res.string.sensor_feeder_error_host_disconnected)

                is VirtualSensorFeedSource.Device ->
                    stringResource(Res.string.sensor_feeder_error_source_device_unavailable, source.deviceId.toString())
            }

        is VirtualSensorFeedError.SourceSensorDisconnected ->
            when (source) {
                is VirtualSensorFeedSource.Host ->
                    stringResource(Res.string.sensor_feeder_error_host_sensor_disconnected, source.sensorId)

                is VirtualSensorFeedSource.Device ->
                    stringResource(
                        Res.string.sensor_feeder_error_source_sensor_disconnected,
                        source.channel + 1,
                        source.deviceId.toString(),
                    )
            }

        is VirtualSensorFeedError.DestinationDeviceDisconnected ->
            stringResource(Res.string.sensor_feeder_error_target_disconnected)

        is VirtualSensorFeedError.Other ->
            stringResource(Res.string.sensor_feeder_error_generic)
    }

@Composable
fun SensorSelectionTile(
    name: String,
    pinLabel: String,
    tempCelsius: Double?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasPendingChanges: Boolean = false,
    feederStatus: FeederStatus = FeederStatus.NONE,
    onDoubleClick: (() -> Unit)? = null,
) {
    val colors = OpenAquaTheme.colors
    val metrics = ChannelSelectionTileDefaults.metrics()
    val isDisconnected = tempCelsius == null
    val fillFraction =
        if (isDisconnected) {
            0f
        } else {
            SensorTargetType.WATER.progressFraction(tempCelsius)
        }

    val tempColor = SensorTargetType.WATER.color(tempCelsius)
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
        hasPendingChanges = hasPendingChanges,
        onClick = onClick,
        onDoubleClick = onDoubleClick,
        fillFraction = fillFraction,
        fillColor = tempColor,
        modifier = modifier,
        metrics = metrics,
        feederStatus = feederStatus,
        icon = {
            Icon(
                painter = painterResource(Res.drawable.ic_sensor),
                contentDescription = name,
                tint =
                    if (selected) {
                        colors.primaryBright
                    } else if (!isDisconnected) {
                        CoolantTeal
                    } else {
                        colors.textMuted
                    },
                modifier = Modifier.size(metrics.iconSize),
            )
        },
    )
}

@Composable
private fun SensorItem.resolveName(): String =
    customName ?: when (telemetry.kind) {
        SensorChannelKind.PHYSICAL_NTC -> stringResource(Res.string.sensor_channel_name, channel + 1)
        SensorChannelKind.VIRTUAL -> stringResource(Res.string.sensor_virtual_name, virtualSensorIndex)
        SensorChannelKind.INTEGRATED_COOLANT -> stringResource(Res.string.sensor_coolant_name)
    }

@Composable
private fun SensorItem.resolvePinLabel(): String =
    when (telemetry.kind) {
        SensorChannelKind.PHYSICAL_NTC -> stringResource(Res.string.sensor_channel_pin, channel + 1)
        SensorChannelKind.VIRTUAL -> stringResource(Res.string.sensor_virtual_pin, virtualSensorIndex)
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
