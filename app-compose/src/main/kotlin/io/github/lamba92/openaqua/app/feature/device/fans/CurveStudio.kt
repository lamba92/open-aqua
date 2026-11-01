package io.github.lamba92.openaqua.app.feature.device.fans

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.feature.device.common.FanItem
import io.github.lamba92.openaqua.app.feature.device.common.SensorItem
import io.github.lamba92.openaqua.app.feature.device.common.canBeHidden
import io.github.lamba92.openaqua.app.feature.device.common.effectiveTempCelsius
import io.github.lamba92.openaqua.app.feature.device.common.resolveName
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.VisibilityFilterChip
import io.github.lamba92.openaqua.model.device.AutoCurveFanConfig
import io.github.lamba92.openaqua.model.device.CoolingChannelKind
import io.github.lamba92.openaqua.model.device.CurveFanConfig
import io.github.lamba92.openaqua.model.device.CurvePoint
import io.github.lamba92.openaqua.model.device.FanChannelConfig
import io.github.lamba92.openaqua.model.device.FixedFanConfig
import io.github.lamba92.openaqua.model.device.TargetTempFanConfig
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.curve_copy_no_channels
import open_aqua.app_compose.generated.resources.curve_copy_source
import open_aqua.app_compose.generated.resources.curve_fixed_power
import open_aqua.app_compose.generated.resources.curve_fixed_power_desc
import open_aqua.app_compose.generated.resources.curve_legend_current
import open_aqua.app_compose.generated.resources.curve_legend_destination
import open_aqua.app_compose.generated.resources.curve_legend_op_current
import open_aqua.app_compose.generated.resources.curve_legend_op_destination
import open_aqua.app_compose.generated.resources.curve_legend_target
import open_aqua.app_compose.generated.resources.curve_pwm_max
import open_aqua.app_compose.generated.resources.curve_pwm_min
import open_aqua.app_compose.generated.resources.curve_sensor_not_selected
import open_aqua.app_compose.generated.resources.curve_sensor_source
import open_aqua.app_compose.generated.resources.curve_shape
import open_aqua.app_compose.generated.resources.curve_shape_digressive
import open_aqua.app_compose.generated.resources.curve_shape_linear
import open_aqua.app_compose.generated.resources.curve_shape_progressive
import open_aqua.app_compose.generated.resources.curve_studio_title
import open_aqua.app_compose.generated.resources.curve_target_temp
import open_aqua.app_compose.generated.resources.curve_target_temp_desc
import open_aqua.app_compose.generated.resources.curve_target_temp_explanation
import open_aqua.app_compose.generated.resources.curve_target_temp_range_error
import open_aqua.app_compose.generated.resources.curve_temp_max
import open_aqua.app_compose.generated.resources.curve_temp_min
import open_aqua.app_compose.generated.resources.fan_mode_auto_curve
import open_aqua.app_compose.generated.resources.fan_mode_copy_fan
import open_aqua.app_compose.generated.resources.fan_mode_fixed
import open_aqua.app_compose.generated.resources.fan_mode_target_temp
import open_aqua.app_compose.generated.resources.ic_tune
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CurveStudio(
    fan: FanItem,
    draft: FanDraft,
    allFans: List<FanItem>,
    sensors: List<SensorItem>,
    onEvent: (CurveStudioEvent) -> Unit,
    modifier: Modifier = Modifier,
    hideDisconnectedSensors: Boolean = false,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    val hasPump = remember(allFans) { allFans.any { it.telemetry.kind == CoolingChannelKind.PUMP } }
    val otherFans = remember(allFans, fan.channel) { allFans.filter { it.channel != fan.channel } }
    val draftConfig = remember(draft, allFans) { draft.toFanChannelConfig(allFans) }

    val activeSensor =
        remember(sensors, draft, draftConfig) {
            when (draft) {
                is FanDraft.AutoCurve -> sensors.firstOrNull { it.channel == draft.sensorIndex }
                is FanDraft.TargetTemp -> sensors.firstOrNull { it.channel == draft.sensorIndex }
                is FanDraft.Copy -> {
                    val sensorIdx =
                        when (draftConfig) {
                            is AutoCurveFanConfig -> draftConfig.sensorIndex
                            is CurveFanConfig -> draftConfig.sensorIndex
                            is TargetTempFanConfig -> draftConfig.sensorIndex
                            else -> null
                        }
                    sensors.firstOrNull { it.channel == sensorIdx }
                }
                is FanDraft.Fixed -> null
            }
        }

    val liveTemp = activeSensor?.effectiveTempCelsius

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(metrics.radiusLarge))
                .background(colors.surfaceContainer)
                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusLarge))
                .padding(metrics.spacingXl),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_tune),
                    contentDescription = null,
                    tint = colors.primaryBright,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(Res.string.curve_studio_title),
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Mode Selector Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
            ) {
                CurveMode.entries.forEach { mode ->
                    val isSelected = mode == draft.mode
                    val label =
                        when (mode) {
                            CurveMode.FIXED -> stringResource(Res.string.fan_mode_fixed)
                            CurveMode.AUTO_CURVE -> stringResource(Res.string.fan_mode_auto_curve)
                            CurveMode.TARGET_TEMP -> stringResource(Res.string.fan_mode_target_temp)
                            CurveMode.COPY_FAN -> stringResource(Res.string.fan_mode_copy_fan)
                        }
                    CurveModePill(
                        label = label,
                        isSelected = isSelected,
                        onClick = { onEvent(CurveStudioEvent.ChangeMode(mode)) },
                    )
                }
            }
        }

        HorizontalDivider(color = colors.borderOutline.copy(alpha = 0.5f))

        val visualizerGraph = @Composable {
            CurveVisualizerGraph(
                currentConfig = fan.config,
                draftConfig = draftConfig,
                liveTemp = liveTemp,
                modifier = Modifier.fillMaxWidth().height(260.dp),
            )
        }

        // Mode Controls
        when (draft) {
            is FanDraft.Fixed -> {
                FixedModeControls(
                    powerPercent = draft.powerPercent,
                    onPowerChange = { onEvent(CurveStudioEvent.ChangeFixedPwm(it)) },
                )
                visualizerGraph()
            }
            is FanDraft.AutoCurve -> {
                AutoCurveModeControls(
                    sensors = sensors,
                    selectedSensorIndex = draft.sensorIndex,
                    onSelectSensor = { onEvent(CurveStudioEvent.ChangeSensorIndex(it)) },
                    tMin = draft.tMin,
                    tMax = draft.tMax,
                    pMin = draft.pMin,
                    pMax = draft.pMax,
                    shape = draft.shape,
                    onTMinChange = { onEvent(CurveStudioEvent.ChangeTMin(it)) },
                    onTMaxChange = { onEvent(CurveStudioEvent.ChangeTMax(it)) },
                    onPMinChange = { onEvent(CurveStudioEvent.ChangePMin(it)) },
                    onPMaxChange = { onEvent(CurveStudioEvent.ChangePMax(it)) },
                    onShapeChange = { onEvent(CurveStudioEvent.ChangeShape(it)) },
                    graphContent = visualizerGraph,
                    hideDisconnectedSensors = hideDisconnectedSensors,
                    onToggleHideSensors = { onEvent(CurveStudioEvent.ToggleHideDisconnectedSensors) },
                )
            }
            is FanDraft.TargetTemp -> {
                TargetTempModeControls(
                    sensors = sensors,
                    selectedSensorIndex = draft.sensorIndex,
                    onSelectSensor = { onEvent(CurveStudioEvent.ChangeSensorIndex(it)) },
                    targetTemp = draft.targetTempCelsius,
                    onTargetTempChange = { onEvent(CurveStudioEvent.ChangeTargetTemp(it)) },
                    hideDisconnectedSensors = hideDisconnectedSensors,
                    onToggleHideSensors = { onEvent(CurveStudioEvent.ToggleHideDisconnectedSensors) },
                )
            }
            is FanDraft.Copy -> {
                CopyFanModeControls(
                    otherFans = otherFans,
                    selectedSourceChannel = draft.sourceChannel,
                    onSelectSourceChannel = { onEvent(CurveStudioEvent.ChangeCopySource(it)) },
                    hasPump = hasPump,
                )
                visualizerGraph()
            }
        }
    }
}

@Composable
private fun CurveModePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    val shape = RoundedCornerShape(metrics.radiusSmall)
    val borderColor = if (isSelected) colors.primaryBright else colors.borderOutline
    val containerColor = if (isSelected) colors.surfaceHigh else colors.surfaceLowest
    val textColor = if (isSelected) colors.primaryBright else colors.textMuted

    Box(
        modifier =
            modifier
                .clip(shape)
                .background(containerColor)
                .border(metrics.strokeThin, borderColor, shape)
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
        )
    }
}

@Composable
private fun FixedModeControls(
    powerPercent: Double,
    onPowerChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

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
                    text = stringResource(Res.string.curve_fixed_power),
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.curve_fixed_power_desc),
                    color = colors.textMuted,
                    fontSize = 11.sp,
                )
            }

            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(metrics.radiusSmall))
                        .background(colors.surfaceHigh)
                        .border(metrics.strokeThin, colors.primaryBright, RoundedCornerShape(metrics.radiusSmall))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    text = "${powerPercent.roundToInt()}%",
                    color = colors.primaryBright,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                )
            }
        }

        Slider(
            value = powerPercent.toFloat(),
            onValueChange = { onPowerChange(it.toDouble().roundToInt().toDouble()) },
            valueRange = 0f..100f,
            steps = 99,
            colors =
                SliderDefaults.colors(
                    thumbColor = colors.primaryBright,
                    activeTrackColor = colors.primary,
                    inactiveTrackColor = colors.surfaceLowest,
                ),
        )
    }
}

@Composable
private fun AutoCurveModeControls(
    sensors: List<SensorItem>,
    selectedSensorIndex: Int,
    onSelectSensor: (Int) -> Unit,
    onToggleHideSensors: () -> Unit,
    tMin: Double,
    tMax: Double,
    pMin: Double,
    pMax: Double,
    shape: Double,
    onTMinChange: (Double) -> Unit,
    onTMaxChange: (Double) -> Unit,
    onPMinChange: (Double) -> Unit,
    onPMaxChange: (Double) -> Unit,
    onShapeChange: (Double) -> Unit,
    graphContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    hideDisconnectedSensors: Boolean = false,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
    ) {
        // Sensor Selector Dropdown Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(Res.string.curve_sensor_source),
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Source temperature channel driving the auto curve",
                    color = colors.textMuted,
                    fontSize = 11.sp,
                )
            }

            SensorDropdown(
                sensors = sensors,
                selectedIndex = selectedSensorIndex,
                onSelectSensor = onSelectSensor,
                onToggleHideSensors = onToggleHideSensors,
                hideDisconnectedSensors = hideDisconnectedSensors,
            )
        }

        graphContent()

        HorizontalDivider(color = colors.borderOutline.copy(alpha = 0.3f))

        // Sliders Grid: Tmin, Tmax, Pmin, Pmax
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(metrics.spacingLg),
            verticalArrangement = Arrangement.spacedBy(metrics.spacingSm),
        ) {
            // Tmin Slider
            Box(modifier = Modifier.weight(1f)) {
                CurveParamSlider(
                    label = stringResource(Res.string.curve_temp_min),
                    valueText = "${tMin.roundToInt()}°C",
                    value = tMin.toFloat(),
                    valueRange = 10f..70f,
                    steps = 59,
                    onValueChange = { onTMinChange(it.toDouble().roundToInt().toDouble()) },
                )
            }

            // Tmax Slider
            Box(modifier = Modifier.weight(1f)) {
                CurveParamSlider(
                    label = stringResource(Res.string.curve_temp_max),
                    valueText = "${tMax.roundToInt()}°C",
                    value = tMax.toFloat(),
                    valueRange = 15f..85f,
                    steps = 69,
                    onValueChange = { onTMaxChange(it.toDouble().roundToInt().toDouble()) },
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(metrics.spacingLg),
            verticalArrangement = Arrangement.spacedBy(metrics.spacingSm),
        ) {
            // Pmin Slider
            Box(modifier = Modifier.weight(1f)) {
                CurveParamSlider(
                    label = stringResource(Res.string.curve_pwm_min),
                    valueText = "${pMin.roundToInt()}%",
                    value = pMin.toFloat(),
                    valueRange = 0f..100f,
                    steps = 99,
                    onValueChange = { onPMinChange(it.toDouble().roundToInt().toDouble()) },
                )
            }

            // Pmax Slider
            Box(modifier = Modifier.weight(1f)) {
                CurveParamSlider(
                    label = stringResource(Res.string.curve_pwm_max),
                    valueText = "${pMax.roundToInt()}%",
                    value = pMax.toFloat(),
                    valueRange = 0f..100f,
                    steps = 99,
                    onValueChange = { onPMaxChange(it.toDouble().roundToInt().toDouble()) },
                )
            }
        }

        // Shape Slider
        val shapeLabel =
            when {
                abs(shape) < 0.05 -> stringResource(Res.string.curve_shape_linear)
                shape > 0.0 -> stringResource(Res.string.curve_shape_progressive, "%.1f".format(shape))
                else -> stringResource(Res.string.curve_shape_digressive, "%.1f".format(shape))
            }

        CurveParamSlider(
            label = stringResource(Res.string.curve_shape),
            valueText = shapeLabel,
            value = shape.toFloat(),
            valueRange = -5f..5f,
            steps = 99,
            onValueChange = { onShapeChange((it * 10).roundToInt() / 10.0) },
        )
    }
}

@Composable
private fun TargetTempModeControls(
    sensors: List<SensorItem>,
    selectedSensorIndex: Int,
    onSelectSensor: (Int) -> Unit,
    onToggleHideSensors: () -> Unit,
    targetTemp: Double,
    onTargetTempChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    hideDisconnectedSensors: Boolean = false,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    val minTemp = 20.0
    val maxTemp = 50.0

    var textInput by remember(targetTemp) { mutableStateOf(if (targetTemp % 1.0 == 0.0) targetTemp.toInt().toString() else targetTemp.toString()) }
    val parsedValue = textInput.toDoubleOrNull()
    val isOutOfRange = parsedValue != null && (parsedValue !in minTemp..maxTemp)
    val isInvalidFormat = textInput.isNotBlank() && parsedValue == null
    val hasError = isOutOfRange || isInvalidFormat

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
    ) {
        // Explanation Card
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(metrics.radiusSmall))
                    .background(colors.surfaceLowest)
                    .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusSmall))
                    .padding(14.dp),
        ) {
            Text(
                text = stringResource(Res.string.curve_target_temp_explanation),
                color = colors.textSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }

        // Sensor Selector Dropdown Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(Res.string.curve_sensor_source),
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.curve_target_temp_desc),
                    color = colors.textMuted,
                    fontSize = 11.sp,
                )
            }

            SensorDropdown(
                sensors = sensors,
                selectedIndex = selectedSensorIndex,
                onSelectSensor = onSelectSensor,
                onToggleHideSensors = onToggleHideSensors,
                hideDisconnectedSensors = hideDisconnectedSensors,
            )
        }

        HorizontalDivider(color = colors.borderOutline.copy(alpha = 0.3f))

        // Target Temperature Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(Res.string.curve_target_temp),
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${minTemp.toInt()}°C - ${maxTemp.toInt()}°C",
                    color = colors.textMuted,
                    fontSize = 11.sp,
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { input ->
                        val sanitized = input.replace(',', '.').filter { it.isDigit() || it == '.' }
                        if (sanitized.count { it == '.' } <= 1) {
                            textInput = sanitized
                            val parsed = sanitized.toDoubleOrNull()
                            if (parsed != null && parsed in minTemp..maxTemp) {
                                onTargetTempChange(parsed)
                            }
                        }
                    },
                    modifier = Modifier.width(130.dp),
                    singleLine = true,
                    isError = hasError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    suffix = {
                        Text(
                            text = "°C",
                            color = colors.textMuted,
                            fontSize = 13.sp,
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

                Text(
                    text =
                        stringResource(
                            Res.string.curve_target_temp_range_error,
                            minTemp.toInt().toString(),
                            maxTemp.toInt().toString(),
                        ),
                    color = if (hasError) colors.statusCritical else Color.Transparent,
                    fontSize = 11.sp,
                    modifier =
                        if (hasError) {
                            Modifier
                        } else {
                            Modifier.clearAndSetSemantics { }
                        },
                )
            }
        }
    }
}

@Composable
private fun SensorDropdown(
    sensors: List<SensorItem>,
    selectedIndex: Int,
    onSelectSensor: (Int) -> Unit,
    onToggleHideSensors: () -> Unit,
    modifier: Modifier = Modifier,
    hideDisconnectedSensors: Boolean = false,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    val hiddenCount =
        remember(sensors) {
            sensors.count { it.canBeHidden }
        }

    val displayedSensors =
        remember(
            sensors,
            hideDisconnectedSensors,
            selectedIndex,
        ) {
            if (!hideDisconnectedSensors) {
                sensors
            } else {
                sensors.filter { !it.canBeHidden || it.channel == selectedIndex }
            }
        }

    var expanded by remember { mutableStateOf(false) }

    val currentSensor = sensors.firstOrNull { it.channel == selectedIndex }
    val displayLabel =
        if (currentSensor != null) {
            sensorDisplayName(currentSensor)
        } else {
            stringResource(Res.string.curve_sensor_not_selected)
        }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
    ) {
        if (hiddenCount > 0) {
            VisibilityFilterChip(
                hiddenCount = hiddenCount,
                showingAll = !hideDisconnectedSensors,
                onClick = onToggleHideSensors,
            )
        }

        Box {
            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(metrics.radiusSmall))
                        .background(colors.surfaceLowest)
                        .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusSmall))
                        .clickable { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = displayLabel,
                    color = colors.textPrimary,
                    fontSize = 12.sp,
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
                displayedSensors.forEach { sensor ->
                    val isSelected = sensor.channel == selectedIndex
                    val isConnected = sensor.effectiveTempCelsius != null
                    DropdownMenuItem(
                        enabled = isConnected,
                        text = {
                            Text(
                                text = sensorDisplayName(sensor),
                                color =
                                    when {
                                        !isConnected -> colors.textMuted
                                        isSelected -> colors.primaryBright
                                        else -> colors.textPrimary
                                    },
                                fontSize = 12.sp,
                                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            onSelectSensor(sensor.channel)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CopyFanModeControls(
    otherFans: List<FanItem>,
    selectedSourceChannel: Int,
    onSelectSourceChannel: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hasPump: Boolean = false,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    var expanded by remember { mutableStateOf(false) }

    val currentSource = otherFans.firstOrNull { it.channel == selectedSourceChannel }

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
                    text = stringResource(Res.string.curve_copy_source),
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Clone the complete speed curve and settings from another channel",
                    color = colors.textMuted,
                    fontSize = 11.sp,
                )
            }

            if (otherFans.isEmpty()) {
                Text(
                    text = stringResource(Res.string.curve_copy_no_channels),
                    color = colors.textMuted,
                    fontSize = 12.sp,
                )
            } else {
                Box {
                    Row(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(metrics.radiusSmall))
                                .background(colors.surfaceLowest)
                                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusSmall))
                                .clickable { expanded = true }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = currentSource?.resolveName(hasPump) ?: "--",
                            color = colors.textPrimary,
                            fontSize = 12.sp,
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
                        otherFans.forEach { fan ->
                            val isSelected = fan.channel == selectedSourceChannel
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = fan.resolveName(hasPump),
                                        color = if (isSelected) colors.primaryBright else colors.textPrimary,
                                        fontSize = 14.sp,
                                        fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                },
                                onClick = {
                                    onSelectSourceChannel(fan.channel)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurveParamSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = colors.textMuted,
                fontSize = 13.sp,
            )
            Text(
                text = valueText,
                color = colors.primaryBright,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors =
                SliderDefaults.colors(
                    thumbColor = colors.primaryBright,
                    activeTrackColor = colors.primary,
                    inactiveTrackColor = colors.surfaceLowest,
                ),
        )
    }
}

@Composable
private fun CurveVisualizerGraph(
    currentConfig: FanChannelConfig?,
    draftConfig: FanChannelConfig,
    liveTemp: Double?,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    val textMeasurer = rememberTextMeasurer()

    val currentPoints = remember(currentConfig) { currentConfig.evaluatePwmPoints() }
    val draftPoints = remember(draftConfig) { draftConfig.evaluatePwmPoints() }

    // Control nodes for draft
    val draftNodes =
        remember(draftConfig) {
            when (draftConfig) {
                is AutoCurveFanConfig -> draftConfig.toCurveFanConfig().points
                is CurveFanConfig -> draftConfig.points
                else -> emptyList()
            }
        }

    // Infinite breathing animation for live operating marker
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
    )
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 9f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
    )

    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(metrics.radiusMedium))
                .background(colors.surfaceLowest)
                .border(metrics.strokeThin, colors.borderOutline.copy(alpha = 0.5f), RoundedCornerShape(metrics.radiusMedium))
                .padding(metrics.spacingMd),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingSm),
    ) {
        // Graph Legend Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Current (Hardware) Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Canvas(
                        modifier =
                            Modifier
                                .width(28.dp)
                                .height(10.dp),
                    ) {
                        val y = size.height / 2f
                        drawLine(
                            color = colors.secondary,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx()), 0f),
                        )
                    }
                    Text(
                        text = stringResource(Res.string.curve_legend_current),
                        color = colors.secondary,
                        fontSize = 13.sp,
                        fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                    )
                }

                // Destination (Draft) Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Canvas(
                        modifier =
                            Modifier
                                .width(28.dp)
                                .height(10.dp),
                    ) {
                        val y = size.height / 2f
                        drawLine(
                            color = colors.primaryBright,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 2.5.dp.toPx(),
                        )
                    }
                    Text(
                        text =
                            if (draftConfig is TargetTempFanConfig) {
                                stringResource(Res.string.curve_legend_target, "%.1f".format(draftConfig.targetTempCelsius))
                            } else {
                                stringResource(Res.string.curve_legend_destination)
                            },
                        color = colors.primaryBright,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                    )
                }
            }

            // Live Operating point readings (Current Curve vs New Curve at current temp)
            if (liveTemp != null) {
                val currentPwm = currentPoints[liveTemp] ?: evaluatePwmAt(currentConfig, liveTemp)
                val destPwm = draftPoints[liveTemp] ?: evaluatePwmAt(draftConfig, liveTemp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text =
                            stringResource(
                                Res.string.curve_legend_op_current,
                                "%.1f".format(liveTemp),
                                currentPwm.roundToInt().toString(),
                            ),
                        color = colors.secondary,
                        fontSize = 12.sp,
                        fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                    )
                    Text(
                        text =
                            stringResource(
                                Res.string.curve_legend_op_destination,
                                "%.1f".format(liveTemp),
                                destPwm.roundToInt().toString(),
                            ),
                        color = colors.primaryBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                    )
                }
            }
        }

        // Native Canvas with Dual Curves and Alpha Blending
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val padLeft = 40.dp.toPx()
            val padBottom = 22.dp.toPx()
            val padRight = 14.dp.toPx()
            val padTop = 10.dp.toPx()

            val plotLeft = padLeft
            val plotTop = padTop
            val plotRight = size.width - padRight
            val plotBottom = size.height - padBottom
            val plotWidth = plotRight - plotLeft
            val plotHeight = plotBottom - plotTop

            val tempMin = 10.0
            val tempMax = 80.0
            val tempSpan = tempMax - tempMin

            fun tempToX(t: Double): Float = plotLeft + (((t - tempMin) / tempSpan).toFloat().coerceIn(0f, 1f)) * plotWidth

            fun pwmToY(p: Double): Float = plotBottom - ((p / 100.0).toFloat().coerceIn(0f, 1f)) * plotHeight

            // Grid Lines & Axis Labels
            val yTicks = listOf(0, 25, 50, 75, 100)
            val labelStyle =
                TextStyle(
                    color = colors.textMuted,
                    fontSize = 11.sp,
                )

            yTicks.forEach { pwm ->
                val y = pwmToY(pwm.toDouble())
                drawLine(
                    color = colors.borderOutline.copy(alpha = 0.30f),
                    start = Offset(plotLeft, y),
                    end = Offset(plotRight, y),
                    strokeWidth = 1f,
                )
                val layout = textMeasurer.measure("$pwm%", labelStyle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(plotLeft - layout.size.width - 6.dp.toPx(), y - layout.size.height / 2f),
                )
            }

            val xTicks = listOf(10, 20, 30, 40, 50, 60, 70, 80)
            xTicks.forEach { temp ->
                val x = tempToX(temp.toDouble())
                drawLine(
                    color = colors.borderOutline.copy(alpha = 0.20f),
                    start = Offset(x, plotTop),
                    end = Offset(x, plotBottom),
                    strokeWidth = 1f,
                )
                val layout = textMeasurer.measure("$temp°", labelStyle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(x - layout.size.width / 2f, plotBottom + 4.dp.toPx()),
                )
            }

            // Construct Current Area Path
            val currentArea =
                Path().apply {
                    moveTo(tempToX(10.0), plotBottom)
                    for (t in 10..80) {
                        val temp = t.toDouble()
                        lineTo(tempToX(temp), pwmToY(currentPoints[temp] ?: 0.0))
                    }
                    lineTo(tempToX(80.0), plotBottom)
                    close()
                }

            // Construct Destination Area Path
            val destArea =
                Path().apply {
                    moveTo(tempToX(10.0), plotBottom)
                    for (t in 10..80) {
                        val temp = t.toDouble()
                        val pwm = draftPoints[temp] ?: 0.0
                        lineTo(tempToX(temp), pwmToY(pwm))
                    }
                    lineTo(tempToX(80.0), plotBottom)
                    close()
                }

            // 1. Draw Current Area (semi-transparent secondary)
            drawPath(
                path = currentArea,
                color = colors.secondary.copy(alpha = 0.18f),
            )

            // 2. Draw Destination Area (semi-transparent primary)
            // Intersecting regions blend naturally via PorterDuff SrcOver compositing!
            drawPath(
                path = destArea,
                color = colors.primary.copy(alpha = 0.22f),
            )

            // 3. Draw Current Curve Stroke (dashed secondary line)
            val currentStroke =
                Path().apply {
                    moveTo(tempToX(10.0), pwmToY(currentPoints[10.0] ?: 0.0))
                    for (t in 11..80) {
                        val temp = t.toDouble()
                        lineTo(tempToX(temp), pwmToY(currentPoints[temp] ?: 0.0))
                    }
                }
            drawPath(
                path = currentStroke,
                color = colors.secondary,
                style =
                    Stroke(
                        width = 1.8.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f),
                    ),
            )

            // 4. Draw Destination Curve Stroke (solid primaryBright line)
            val destStroke =
                Path().apply {
                    val firstPwm = draftPoints[10.0] ?: 0.0
                    moveTo(tempToX(10.0), pwmToY(firstPwm))
                    for (t in 11..80) {
                        val temp = t.toDouble()
                        val pwm = draftPoints[temp] ?: 0.0
                        lineTo(tempToX(temp), pwmToY(pwm))
                    }
                }
            drawPath(
                path = destStroke,
                color = colors.primaryBright,
                style = Stroke(width = 2.4.dp.toPx()),
            )

            // 5. Draw 16 Control Nodes on Destination Curve
            draftNodes.forEach { node ->
                if (node.temp in 10.0..80.0) {
                    val nx = tempToX(node.temp)
                    val ny = pwmToY(node.pwm)
                    drawCircle(
                        color = colors.primaryBright,
                        radius = 3.5.dp.toPx(),
                        center = Offset(nx, ny),
                    )
                    drawCircle(
                        color = colors.surfaceLowest,
                        radius = 1.8.dp.toPx(),
                        center = Offset(nx, ny),
                    )
                }
            }

            // Target Temperature Setpoint Marker Line
            if (draftConfig is TargetTempFanConfig) {
                val targetT = draftConfig.targetTempCelsius
                if (targetT in 10.0..80.0) {
                    val tx = tempToX(targetT)
                    drawLine(
                        color = colors.primaryBright,
                        start = Offset(tx, plotTop),
                        end = Offset(tx, plotBottom),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f),
                    )
                }
            }

            // 6. Draw Live Operating Point Marker (vertical dashed line + operating point dots)
            if (liveTemp != null && liveTemp in 10.0..80.0) {
                val lx = tempToX(liveTemp)
                val currentPwm = currentPoints[liveTemp] ?: evaluatePwmAt(currentConfig, liveTemp)
                val destPwm = draftPoints[liveTemp] ?: evaluatePwmAt(draftConfig, liveTemp)
                val currentY = pwmToY(currentPwm)
                val destY = pwmToY(destPwm)

                drawLine(
                    color = colors.borderOutline.copy(alpha = 0.5f),
                    start = Offset(lx, plotTop),
                    end = Offset(lx, plotBottom),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()), 0f),
                )

                // Current operating point dot (hardware) on current curve
                drawCircle(
                    color = colors.secondary,
                    radius = 4.dp.toPx(),
                    center = Offset(lx, currentY),
                )
                drawCircle(
                    color = colors.surfaceLowest,
                    radius = 2.dp.toPx(),
                    center = Offset(lx, currentY),
                )

                // Destination operating point dot (pulsing halo + solid center dot) on destination curve
                // Pulsing outer halo
                drawCircle(
                    color = colors.primaryBright.copy(alpha = pulseAlpha * 0.35f),
                    radius = (5 + pulseRadius).dp.toPx(),
                    center = Offset(lx, destY),
                )
                // Solid center dot
                drawCircle(
                    color = colors.primaryBright,
                    radius = 4.5.dp.toPx(),
                    center = Offset(lx, destY),
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(lx, destY),
                )
            }
        }
    }
}

// Helpers for curve points evaluation across 10..80°C
private fun FanChannelConfig?.evaluatePwmPoints(): Map<Double, Double> {
    val result = mutableMapOf<Double, Double>()
    for (t in 10..80) {
        val temp = t.toDouble()
        result[temp] = evaluatePwmAt(this, temp)
    }
    return result
}

private fun evaluatePwmAt(
    config: FanChannelConfig?,
    temp: Double,
): Double =
    when (config) {
        is FixedFanConfig -> config.powerPercent
        is AutoCurveFanConfig -> {
            val points = config.toCurveFanConfig().points
            evaluatePointsAt(points, temp)
        }
        is CurveFanConfig -> evaluatePointsAt(config.points, temp)
        is TargetTempFanConfig -> {
            val diff = temp - config.targetTempCelsius
            (50.0 + diff * 15.0).coerceIn(0.0, 100.0)
        }
        else -> 0.0
    }.coerceIn(0.0, 100.0)

private fun evaluatePointsAt(
    points: List<CurvePoint>,
    temp: Double,
): Double {
    if (points.isEmpty()) return 0.0
    val sorted = points.sortedBy { it.temp }
    if (temp <= sorted.first().temp) return sorted.first().pwm
    if (temp >= sorted.last().temp) return sorted.last().pwm

    for (i in 0 until sorted.size - 1) {
        val p1 = sorted[i]
        val p2 = sorted[i + 1]
        if (temp in p1.temp..p2.temp) {
            val span = p2.temp - p1.temp
            if (span <= 1e-6) return p1.pwm
            val factor = (temp - p1.temp) / span
            return p1.pwm + factor * (p2.pwm - p1.pwm)
        }
    }
    return sorted.last().pwm
}

@Composable
private fun sensorDisplayName(sensor: SensorItem): String {
    val base = sensor.resolveName()
    val temp = sensor.effectiveTempCelsius
    return if (temp != null) {
        "$base (%.1f°C)".format(temp)
    } else {
        base
    }
}
