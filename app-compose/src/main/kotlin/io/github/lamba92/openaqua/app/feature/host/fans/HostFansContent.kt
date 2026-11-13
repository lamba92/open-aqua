package io.github.lamba92.openaqua.app.feature.host.fans

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.di.rememberInstance
import io.github.lamba92.openaqua.app.feature.device.fans.FanSelectionTile
import io.github.lamba92.openaqua.app.model.HostFanConfig
import io.github.lamba92.openaqua.app.model.SensorSource
import io.github.lamba92.openaqua.app.model.VirtualSensorFeedSource
import io.github.lamba92.openaqua.app.theme.CoolantTeal
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.ChannelDetailHeader
import io.github.lamba92.openaqua.app.ui.components.ChannelPageScaffold
import io.github.lamba92.openaqua.app.ui.components.ChannelSelectorSection
import io.github.lamba92.openaqua.app.ui.components.MetricCard
import io.github.lamba92.openaqua.app.ui.components.OpenAquaButton
import io.github.lamba92.openaqua.app.ui.components.OpenAquaOutlinedButton
import io.github.lamba92.openaqua.app.ui.components.RenameDialog
import io.github.lamba92.openaqua.app.ui.components.VisibilityEmptyState
import io.github.lamba92.openaqua.app.ui.components.VisibilityFilterChip
import io.github.lamba92.openaqua.model.device.CurveMath
import io.github.lamba92.openaqua.model.device.CurvePoint
import io.github.lamba92.openaqua.model.host.HostFanTelemetry
import io.github.lamba92.openaqua.model.host.HostSensorId
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.action_rename
import open_aqua.app_compose.generated.resources.curve_action_discard
import open_aqua.app_compose.generated.resources.curve_fixed_power
import open_aqua.app_compose.generated.resources.curve_legend_current
import open_aqua.app_compose.generated.resources.curve_legend_destination
import open_aqua.app_compose.generated.resources.curve_legend_op_current
import open_aqua.app_compose.generated.resources.curve_legend_op_destination
import open_aqua.app_compose.generated.resources.curve_pwm_max
import open_aqua.app_compose.generated.resources.curve_pwm_min
import open_aqua.app_compose.generated.resources.curve_sensor_not_selected
import open_aqua.app_compose.generated.resources.curve_sensor_source
import open_aqua.app_compose.generated.resources.curve_shape
import open_aqua.app_compose.generated.resources.curve_shape_digressive
import open_aqua.app_compose.generated.resources.curve_shape_linear
import open_aqua.app_compose.generated.resources.curve_shape_progressive
import open_aqua.app_compose.generated.resources.curve_studio_title
import open_aqua.app_compose.generated.resources.curve_temp_max
import open_aqua.app_compose.generated.resources.curve_temp_min
import open_aqua.app_compose.generated.resources.fan_disconnected
import open_aqua.app_compose.generated.resources.fan_hardware_pin
import open_aqua.app_compose.generated.resources.fan_mode_auto_curve
import open_aqua.app_compose.generated.resources.fan_mode_fixed
import open_aqua.app_compose.generated.resources.fan_rpm_short_disconnected
import open_aqua.app_compose.generated.resources.fan_rpm_value
import open_aqua.app_compose.generated.resources.fan_settings_title
import open_aqua.app_compose.generated.resources.host_fan_action_apply
import open_aqua.app_compose.generated.resources.host_fan_action_apply_all
import open_aqua.app_compose.generated.resources.host_fan_mode_follow_bios
import open_aqua.app_compose.generated.resources.host_fan_mode_follow_bios_desc
import open_aqua.app_compose.generated.resources.host_fan_read_only_badge
import open_aqua.app_compose.generated.resources.host_fan_read_only_notice
import open_aqua.app_compose.generated.resources.host_fan_status_bios
import open_aqua.app_compose.generated.resources.host_fan_status_read_only
import open_aqua.app_compose.generated.resources.host_fan_status_software
import open_aqua.app_compose.generated.resources.host_fans_channels_header
import open_aqua.app_compose.generated.resources.host_fans_screen_subtitle
import open_aqua.app_compose.generated.resources.host_fans_screen_title
import open_aqua.app_compose.generated.resources.ic_edit
import open_aqua.app_compose.generated.resources.ic_speed
import open_aqua.app_compose.generated.resources.ic_tune
import open_aqua.app_compose.generated.resources.metric_pwm_duty
import open_aqua.app_compose.generated.resources.metric_speed
import open_aqua.app_compose.generated.resources.rename_dialog_title
import open_aqua.app_compose.generated.resources.visibility_no_fans_visible
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HostFansTabContent(
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier,
    viewModel: HostFansViewModel = rememberInstance(),
) {
    val state by viewModel.state.collectAsState()
    when (val s = state) {
        HostFansUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = OpenAquaTheme.colors.primaryBright,
                    modifier = Modifier.size(48.dp),
                )
            }
        }

        is HostFansUiState.Ready -> {
            HostFansContent(
                state = s,
                onEvent = viewModel::onEvent,
                scrollState = scrollState,
                modifier = modifier,
            )
        }
    }
}

@Composable
fun HostFansContent(
    state: HostFansUiState.Ready,
    onEvent: (HostFansUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val isFanHidden =
        remember<(HostFanItem) -> Boolean>(state.hideDisconnectedFans) {
            { item -> state.hideDisconnectedFans && item.telemetry is HostFanTelemetry.Disconnected }
        }

    val hiddenCount =
        remember(state.fans) {
            state.fans.count { it.telemetry is HostFanTelemetry.Disconnected }
        }

    val displayedFans =
        remember(state.fans, state.hideDisconnectedFans) {
            when {
                state.hideDisconnectedFans -> state.fans.filterNot(isFanHidden)
                else -> state.fans
            }
        }

    val selectedFan =
        displayedFans.firstOrNull { it.id == state.selectedFanId }
            ?: displayedFans.firstOrNull()
            ?: state.fans.firstOrNull()

    var renamingFan by remember { mutableStateOf<HostFanItem?>(null) }

    ChannelPageScaffold(
        title = stringResource(Res.string.host_fans_screen_title),
        subtitle = stringResource(Res.string.host_fans_screen_subtitle),
        modifier = modifier,
        scrollState = scrollState,
        selectorContent = {
            ChannelSelectorSection(
                title = stringResource(Res.string.host_fans_channels_header),
                trailing =
                    if (hiddenCount > 0) {
                        {
                            VisibilityFilterChip(
                                hiddenCount = hiddenCount,
                                showingAll = !state.hideDisconnectedFans,
                                onClick = { onEvent(HostFansUiEvent.ToggleHideDisconnectedFans) },
                            )
                        }
                    } else {
                        null
                    },
                content = {
                    displayedFans.forEach { fan ->
                        val name = fan.resolveName()
                        val pinLabel = fan.deviceName.ifBlank { fan.id.value }
                        val (rpm, pwmPercent) =
                            when (val tel = fan.telemetry) {
                                is HostFanTelemetry.Controllable -> tel.rpm to tel.pwmPercent
                                is HostFanTelemetry.ReadOnly -> tel.rpm to null
                                is HostFanTelemetry.Disconnected -> null to null
                            }
                        val hasPending = state.isFanModified(fan)
                        FanSelectionTile(
                            name = name,
                            pinLabel = pinLabel,
                            rpm = rpm,
                            pwmPercent = pwmPercent,
                            selected = fan.id == selectedFan?.id,
                            hasPendingChanges = hasPending,
                            onClick = { onEvent(HostFansUiEvent.SelectFan(fan.id)) },
                            onDoubleClick = { renamingFan = fan },
                        )
                    }
                },
            )
        },
        detailContent = {
            if (displayedFans.isEmpty()) {
                VisibilityEmptyState(
                    message = stringResource(Res.string.visibility_no_fans_visible),
                    onShowAll = { onEvent(HostFansUiEvent.ToggleHideDisconnectedFans) },
                )
            } else if (selectedFan != null) {
                val effectiveConfig = state.fanDrafts[selectedFan.id] ?: selectedFan.config
                val isModified = state.isFanModified(selectedFan)

                HostFanDetailCard(
                    fan = selectedFan,
                    activeConfig = effectiveConfig,
                    availableSensors = state.availableSensors,
                    isModified = isModified,
                    pendingFansCount = state.pendingFans.size,
                    onUpdateConfig = { onEvent(HostFansUiEvent.UpdateDraftConfig(selectedFan.id, it)) },
                    onDiscard = { onEvent(HostFansUiEvent.DiscardFanDraft(selectedFan.id)) },
                    onApply = { onEvent(HostFansUiEvent.ApplyChanges) },
                    onRenameClick = { renamingFan = selectedFan },
                    hideDisconnectedSensors = state.hideDisconnectedSensors,
                    onToggleHideSensors = { onEvent(HostFansUiEvent.ToggleHideDisconnectedSensors) },
                )
            }
        },
    )

    val activeRenamingFan = renamingFan
    if (activeRenamingFan != null) {
        RenameDialog(
            title = stringResource(Res.string.rename_dialog_title, activeRenamingFan.resolveName()),
            initialValue = activeRenamingFan.customName,
            icon = painterResource(Res.drawable.ic_edit),
            onConfirm = { newName ->
                val nameString = if (newName is io.github.lamba92.openaqua.app.feature.device.common.ChannelName.Custom) newName.value else null
                onEvent(HostFansUiEvent.RenameFan(activeRenamingFan.id, nameString))
                renamingFan = null
            },
            onDismiss = { renamingFan = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HostFanDetailCard(
    fan: HostFanItem,
    activeConfig: HostFanConfig,
    availableSensors: List<AvailableSensorItem>,
    isModified: Boolean,
    pendingFansCount: Int,
    onUpdateConfig: (HostFanConfig) -> Unit,
    onDiscard: () -> Unit,
    onApply: () -> Unit,
    onRenameClick: () -> Unit,
    modifier: Modifier = Modifier,
    hideDisconnectedSensors: Boolean = false,
    onToggleHideSensors: () -> Unit = {},
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    val displayName = fan.resolveName()
    val pinLabel = fan.deviceName.ifBlank { fan.id.value }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(metrics.radiusLarge))
                .background(colors.surfaceLow)
                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusLarge))
                .padding(metrics.spacingXxl),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
    ) {
        ChannelDetailHeader(
            icon = painterResource(Res.drawable.ic_tune),
            title = stringResource(Res.string.fan_settings_title, displayName),
            subtitle = stringResource(Res.string.fan_hardware_pin, pinLabel),
            trailingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (fan.isControllable) {
                        if (isModified) {
                            OpenAquaOutlinedButton(
                                text = stringResource(Res.string.curve_action_discard),
                                onClick = onDiscard,
                            )
                        }

                        if (pendingFansCount > 0) {
                            val applyLabel =
                                if (pendingFansCount > 1) {
                                    stringResource(Res.string.host_fan_action_apply_all, pendingFansCount)
                                } else {
                                    stringResource(Res.string.host_fan_action_apply)
                                }
                            OpenAquaButton(
                                text = applyLabel,
                                onClick = onApply,
                            )
                        }
                    }

                    OpenAquaOutlinedButton(
                        text = stringResource(Res.string.action_rename),
                        icon = painterResource(Res.drawable.ic_edit),
                        onClick = onRenameClick,
                    )
                }
            },
        )

        // Live Metrics Row
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(metrics.spacingMd),
            verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
        ) {
            val (rpmText, pwmText, modeBadge) =
                when (val tel = fan.telemetry) {
                    is HostFanTelemetry.Controllable -> {
                        val rpm = stringResource(Res.string.fan_rpm_value, tel.rpm)
                        val pwm = "${tel.pwmPercent.roundToInt()}%"
                        val mode =
                            if (tel.isFollowingBiosCurve) {
                                stringResource(Res.string.host_fan_status_bios)
                            } else {
                                stringResource(Res.string.host_fan_status_software)
                            }
                        Triple(rpm, pwm, mode)
                    }

                    is HostFanTelemetry.ReadOnly -> {
                        val rpm = stringResource(Res.string.fan_rpm_value, tel.rpm)
                        Triple(rpm, null, stringResource(Res.string.host_fan_status_read_only))
                    }

                    is HostFanTelemetry.Disconnected -> {
                        val status =
                            if (!fan.isControllable) {
                                stringResource(Res.string.host_fan_status_read_only)
                            } else {
                                stringResource(Res.string.fan_disconnected)
                            }
                        Triple(stringResource(Res.string.fan_rpm_short_disconnected), null, status)
                    }
                }

            MetricCard(
                label = stringResource(Res.string.metric_speed),
                value = rpmText,
            )

            if (pwmText != null) {
                MetricCard(
                    label = stringResource(Res.string.metric_pwm_duty),
                    value = pwmText,
                )
            }

            MetricCard(
                label = "Hardware Mode",
                value = modeBadge,
            )
        }

        HorizontalDivider(color = colors.borderOutline.copy(alpha = 0.5f))

        // Read-only notice or Curve Studio
        if (!fan.isControllable) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(metrics.radiusSmall))
                        .background(colors.surfaceLowest)
                        .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusSmall))
                        .padding(metrics.spacingLg),
            ) {
                Text(
                    text = stringResource(Res.string.host_fan_read_only_notice),
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                )
            }
        } else {
            HostCurveStudio(
                fan = fan,
                currentConfig = fan.config,
                activeConfig = activeConfig,
                availableSensors = availableSensors,
                onUpdateConfig = onUpdateConfig,
                hideDisconnectedSensors = hideDisconnectedSensors,
                onToggleHideSensors = onToggleHideSensors,
            )
        }
    }
}

@Composable
private fun HostCurveStudio(
    fan: HostFanItem,
    currentConfig: HostFanConfig,
    activeConfig: HostFanConfig,
    availableSensors: List<AvailableSensorItem>,
    onUpdateConfig: (HostFanConfig) -> Unit,
    modifier: Modifier = Modifier,
    hideDisconnectedSensors: Boolean = false,
    onToggleHideSensors: () -> Unit = {},
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    var preferredSensorSource by remember(fan.id) {
        val initialSource =
            (currentConfig as? HostFanConfig.AutoCurve)?.sensorSource
                ?: availableSensors.firstOrNull { it.currentTempCelsius != null }?.source
                ?: availableSensors.firstOrNull()?.source
        mutableStateOf(initialSource)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
    ) {
        // Mode Selector Pills
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

            Row(horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm)) {
                HostCurveModePill(
                    label = stringResource(Res.string.host_fan_mode_follow_bios),
                    isSelected = activeConfig is HostFanConfig.FollowBios,
                    onClick = { onUpdateConfig(HostFanConfig.FollowBios) },
                )
                HostCurveModePill(
                    label = stringResource(Res.string.fan_mode_fixed),
                    isSelected = activeConfig is HostFanConfig.Fixed,
                    onClick = {
                        val fixed = (activeConfig as? HostFanConfig.Fixed) ?: HostFanConfig.Fixed(50.0)
                        onUpdateConfig(fixed)
                    },
                )
                HostCurveModePill(
                    label = stringResource(Res.string.fan_mode_auto_curve),
                    isSelected = activeConfig is HostFanConfig.AutoCurve,
                    onClick = {
                        val currentAuto = activeConfig as? HostFanConfig.AutoCurve
                        val defaultSource =
                            preferredSensorSource
                                ?: availableSensors.firstOrNull { it.currentTempCelsius != null }?.source
                                ?: availableSensors.firstOrNull()?.source
                                ?: VirtualSensorFeedSource.Host(HostSensorId("default"))
                        onUpdateConfig(currentAuto ?: HostFanConfig.AutoCurve(sensorSource = defaultSource))
                    },
                )
            }
        }

        HorizontalDivider(color = colors.borderOutline.copy(alpha = 0.5f))

        when (activeConfig) {
            HostFanConfig.FollowBios -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(metrics.radiusSmall))
                            .background(colors.surfaceLowest)
                            .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusSmall))
                            .padding(metrics.spacingLg),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(Res.string.host_fan_mode_follow_bios),
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = stringResource(Res.string.host_fan_mode_follow_bios_desc),
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            is HostFanConfig.Fixed -> {
                val fixedLiveSensor =
                    (currentConfig as? HostFanConfig.AutoCurve)?.let { auto ->
                        availableSensors.firstOrNull { it.source == auto.sensorSource }
                    } ?: availableSensors.firstOrNull { it.source == preferredSensorSource }
                        ?: availableSensors.firstOrNull { it.currentTempCelsius != null }
                        ?: availableSensors.firstOrNull()

                Column(verticalArrangement = Arrangement.spacedBy(metrics.spacingMd)) {
                    Text(
                        text = stringResource(Res.string.curve_fixed_power),
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Slider(
                            value = activeConfig.pwmPercent.toFloat(),
                            onValueChange = { onUpdateConfig(HostFanConfig.Fixed(it.toDouble().roundToInt().toDouble())) },
                            valueRange = 0f..100f,
                            steps = 99,
                            colors =
                                SliderDefaults.colors(
                                    thumbColor = colors.primaryBright,
                                    activeTrackColor = colors.primary,
                                    inactiveTrackColor = colors.surfaceLowest,
                                ),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${activeConfig.pwmPercent.roundToInt()}%",
                            color = colors.primaryBright,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                        )
                    }
                    HostCurveVisualizerGraph(
                        currentConfig = currentConfig,
                        draftConfig = activeConfig,
                        liveTemp = fixedLiveSensor?.currentTempCelsius,
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                    )
                }
            }

            is HostFanConfig.AutoCurve -> {
                val selectedSensor = availableSensors.firstOrNull { it.source == activeConfig.sensorSource }
                Column(verticalArrangement = Arrangement.spacedBy(metrics.spacingMd)) {
                    // Sensor dropdown
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
                                text = "Source temperature sensor driving the software fan curve",
                                color = colors.textMuted,
                                fontSize = 11.sp,
                            )
                        }

                        HostSensorDropdown(
                            availableSensors = availableSensors,
                            selectedSource = activeConfig.sensorSource,
                            onSelectSensor = {
                                preferredSensorSource = it
                                onUpdateConfig(activeConfig.copy(sensorSource = it))
                            },
                            onToggleHideSensors = onToggleHideSensors,
                            hideDisconnectedSensors = hideDisconnectedSensors,
                        )
                    }

                    HostCurveVisualizerGraph(
                        currentConfig = currentConfig,
                        draftConfig = activeConfig,
                        liveTemp = selectedSensor?.currentTempCelsius,
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                    )

                    HorizontalDivider(color = colors.borderOutline.copy(alpha = 0.3f))

                    // Sliders
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.spacingLg),
                        verticalArrangement = Arrangement.spacedBy(metrics.spacingSm),
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            HostCurveParamSlider(
                                label = stringResource(Res.string.curve_temp_min),
                                valueText = "${activeConfig.tMin.roundToInt()}°C",
                                value = activeConfig.tMin.toFloat(),
                                valueRange = 10f..70f,
                                steps = 59,
                                onValueChange = {
                                    val newTMin = it.toDouble().roundToInt().toDouble()
                                    val (min, max) = CurveMath.adjustTMin(newTMin, activeConfig.tMax, maxAllowed = 95.0)
                                    onUpdateConfig(activeConfig.copy(tMin = min, tMax = max))
                                },
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            HostCurveParamSlider(
                                label = stringResource(Res.string.curve_temp_max),
                                valueText = "${activeConfig.tMax.roundToInt()}°C",
                                value = activeConfig.tMax.toFloat(),
                                valueRange = 15f..95f,
                                steps = 79,
                                onValueChange = {
                                    val newTMax = it.toDouble().roundToInt().toDouble()
                                    val (min, max) = CurveMath.adjustTMax(newTMax, activeConfig.tMin, minAllowed = 10.0)
                                    onUpdateConfig(activeConfig.copy(tMin = min, tMax = max))
                                },
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.spacingLg),
                        verticalArrangement = Arrangement.spacedBy(metrics.spacingSm),
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            HostCurveParamSlider(
                                label = stringResource(Res.string.curve_pwm_min),
                                valueText = "${activeConfig.pMin.roundToInt()}%",
                                value = activeConfig.pMin.toFloat(),
                                valueRange = 0f..100f,
                                steps = 99,
                                onValueChange = { onUpdateConfig(activeConfig.copy(pMin = it.toDouble().roundToInt().toDouble())) },
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            HostCurveParamSlider(
                                label = stringResource(Res.string.curve_pwm_max),
                                valueText = "${activeConfig.pMax.roundToInt()}%",
                                value = activeConfig.pMax.toFloat(),
                                valueRange = 0f..100f,
                                steps = 99,
                                onValueChange = { onUpdateConfig(activeConfig.copy(pMax = it.toDouble().roundToInt().toDouble())) },
                            )
                        }
                    }

                    val shapeLabel =
                        when {
                            abs(activeConfig.shape) < 0.05 -> stringResource(Res.string.curve_shape_linear)
                            activeConfig.shape > 0.0 -> stringResource(Res.string.curve_shape_progressive, "%.1f".format(activeConfig.shape))
                            else -> stringResource(Res.string.curve_shape_digressive, "%.1f".format(activeConfig.shape))
                        }

                    HostCurveParamSlider(
                        label = stringResource(Res.string.curve_shape),
                        valueText = shapeLabel,
                        value = activeConfig.shape.toFloat(),
                        valueRange = -5f..5f,
                        steps = 99,
                        onValueChange = { onUpdateConfig(activeConfig.copy(shape = (it * 10).roundToInt() / 10.0)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HostSensorDropdown(
    availableSensors: List<AvailableSensorItem>,
    selectedSource: SensorSource,
    onSelectSensor: (SensorSource) -> Unit,
    onToggleHideSensors: () -> Unit,
    hideDisconnectedSensors: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    var expanded by remember { mutableStateOf(false) }

    val hiddenCount =
        remember(availableSensors) {
            availableSensors.count { it.currentTempCelsius == null }
        }

    val displayedSensors =
        remember(availableSensors, hideDisconnectedSensors, selectedSource) {
            if (!hideDisconnectedSensors) {
                availableSensors
            } else {
                availableSensors.filter { it.currentTempCelsius != null || it.source == selectedSource }
            }
        }

    val currentSensor = availableSensors.firstOrNull { it.source == selectedSource }
    val displayLabel =
        if (currentSensor != null) {
            val temp = currentSensor.currentTempCelsius
            if (temp != null) "${currentSensor.displayName} (%.1f°C)".format(temp) else currentSensor.displayName
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
                    val isSelected = sensor.source == selectedSource
                    val label =
                        sensor.currentTempCelsius?.let {
                            "${sensor.displayName} (%.1f°C)".format(it)
                        } ?: sensor.displayName
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                color = if (isSelected) colors.primaryBright else colors.textPrimary,
                                fontSize = 12.sp,
                                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            onSelectSensor(sensor.source)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HostCurveParamSlider(
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
                color = colors.textSecondary,
                fontSize = 12.sp,
            )
            Text(
                text = valueText,
                color = colors.primaryBright,
                fontSize = 12.sp,
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
private fun HostCurveModePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    val shape = RoundedCornerShape(metrics.radiusSmall)
    Box(
        modifier =
            modifier
                .clip(shape)
                .background(if (isSelected) colors.primary.copy(alpha = 0.2f) else colors.surfaceLowest)
                .border(
                    width = metrics.strokeThin,
                    color = if (isSelected) colors.primaryBright else colors.borderOutline,
                    shape = shape,
                ).clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isSelected) colors.primaryBright else colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun HostCurveVisualizerGraph(
    currentConfig: HostFanConfig,
    draftConfig: HostFanConfig,
    liveTemp: Double?,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    val textMeasurer = rememberTextMeasurer()

    val currentPoints = remember(currentConfig) { currentConfig.evaluatePoints() }
    val draftPoints = remember(draftConfig) { draftConfig.evaluatePoints() }

    // When following bios, of course we display only the destination curve.
    // When currentConfig is Fixed or AutoCurve, we display the current curve as well if modified!
    val showCurrentCurve = currentConfig !is HostFanConfig.FollowBios && currentConfig != draftConfig

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (showCurrentCurve) {
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
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f),
                            )
                        }
                        Text(
                            text = stringResource(Res.string.curve_legend_current),
                            color = colors.secondary,
                            fontSize = 12.sp,
                            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                        )
                    }
                }

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
                        text = stringResource(Res.string.curve_legend_destination),
                        color = colors.primaryBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                    )
                }
            }

            if (liveTemp != null) {
                val currentPwm = currentPoints[liveTemp] ?: currentConfig.evaluateAt(liveTemp)
                val destPwm = draftPoints[liveTemp] ?: draftConfig.evaluateAt(liveTemp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showCurrentCurve) {
                        Text(
                            text =
                                stringResource(
                                    Res.string.curve_legend_op_current,
                                    "%.1f".format(liveTemp),
                                    currentPwm.roundToInt().toString(),
                                ),
                            color = colors.secondary,
                            fontSize = 11.sp,
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                        )
                    } else {
                        Text(
                            text =
                                stringResource(
                                    Res.string.curve_legend_op_destination,
                                    "%.1f".format(liveTemp),
                                    destPwm.roundToInt().toString(),
                                ),
                            color = colors.primaryBright,
                            fontSize = 11.sp,
                            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clearAndSetSemantics {},
        ) {
            val leftMargin = 38.dp.toPx()
            val bottomMargin = 22.dp.toPx()
            val plotRight = size.width - 12.dp.toPx()
            val plotTop = 10.dp.toPx()
            val plotBottom = size.height - bottomMargin

            val minT = 10.0
            val maxT = 90.0

            fun tempToX(t: Double): Float {
                val fraction = ((t - minT) / (maxT - minT)).coerceIn(0.0, 1.0).toFloat()
                return leftMargin + fraction * (plotRight - leftMargin)
            }

            fun pwmToY(p: Double): Float {
                val fraction = (p / 100.0).coerceIn(0.0, 1.0).toFloat()
                return plotBottom - fraction * (plotBottom - plotTop)
            }

            // Grid lines
            for (pwm in listOf(0.0, 25.0, 50.0, 75.0, 100.0)) {
                val y = pwmToY(pwm)
                drawLine(
                    color = colors.borderOutline.copy(alpha = 0.25f),
                    start = Offset(leftMargin, y),
                    end = Offset(plotRight, y),
                    strokeWidth = 1.dp.toPx(),
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "${pwm.toInt()}%",
                    topLeft = Offset(2.dp.toPx(), y - 7.dp.toPx()),
                    style = TextStyle(color = colors.textMuted, fontSize = 9.sp),
                )
            }

            for (temp in listOf(20.0, 40.0, 60.0, 80.0)) {
                val x = tempToX(temp)
                drawLine(
                    color = colors.borderOutline.copy(alpha = 0.25f),
                    start = Offset(x, plotTop),
                    end = Offset(x, plotBottom),
                    strokeWidth = 1.dp.toPx(),
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "${temp.toInt()}°C",
                    topLeft = Offset(x - 12.dp.toPx(), plotBottom + 4.dp.toPx()),
                    style = TextStyle(color = colors.textMuted, fontSize = 9.sp),
                )
            }

            // Colored Area Paths (under the curves)
            // 1. Current Area (if showCurrentCurve)
            if (showCurrentCurve) {
                val currentArea =
                    Path().apply {
                        moveTo(tempToX(minT), plotBottom)
                        for (t in minT.toInt()..maxT.toInt()) {
                            val temp = t.toDouble()
                            val pwm = currentPoints[temp] ?: currentConfig.evaluateAt(temp)
                            lineTo(tempToX(temp), pwmToY(pwm))
                        }
                        lineTo(tempToX(maxT), plotBottom)
                        close()
                    }
                drawPath(
                    path = currentArea,
                    color = colors.secondary.copy(alpha = 0.18f),
                )
            }

            // 2. Destination Area
            val destArea =
                Path().apply {
                    moveTo(tempToX(minT), plotBottom)
                    for (t in minT.toInt()..maxT.toInt()) {
                        val temp = t.toDouble()
                        val pwm = draftPoints[temp] ?: draftConfig.evaluateAt(temp)
                        lineTo(tempToX(temp), pwmToY(pwm))
                    }
                    lineTo(tempToX(maxT), plotBottom)
                    close()
                }
            drawPath(
                path = destArea,
                color = colors.primary.copy(alpha = 0.22f),
            )

            // Curve Strokes
            // 3. Current Curve Stroke (dashed)
            if (showCurrentCurve) {
                val currentStroke =
                    Path().apply {
                        val firstPwm = currentPoints[minT] ?: currentConfig.evaluateAt(minT)
                        moveTo(tempToX(minT), pwmToY(firstPwm))
                        for (t in 11..90) {
                            val temp = t.toDouble()
                            val pwm = currentPoints[temp] ?: currentConfig.evaluateAt(temp)
                            lineTo(tempToX(temp), pwmToY(pwm))
                        }
                    }
                drawPath(
                    path = currentStroke,
                    color = colors.secondary,
                    style =
                        Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f),
                        ),
                )
            }

            // 4. Destination Curve Stroke (solid)
            val destStroke =
                Path().apply {
                    val firstPwm = draftPoints[minT] ?: draftConfig.evaluateAt(minT)
                    moveTo(tempToX(minT), pwmToY(firstPwm))
                    for (t in 11..90) {
                        val temp = t.toDouble()
                        val pwm = draftPoints[temp] ?: draftConfig.evaluateAt(temp)
                        lineTo(tempToX(temp), pwmToY(pwm))
                    }
                }
            drawPath(
                path = destStroke,
                color = colors.primaryBright,
                style = Stroke(width = 2.4.dp.toPx()),
            )

            // Live operating point markers
            if (liveTemp != null && liveTemp in minT..maxT) {
                val lx = tempToX(liveTemp)
                val destPwm = draftPoints[liveTemp] ?: draftConfig.evaluateAt(liveTemp)
                val destY = pwmToY(destPwm)

                drawLine(
                    color = colors.borderOutline.copy(alpha = 0.5f),
                    start = Offset(lx, plotTop),
                    end = Offset(lx, plotBottom),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()), 0f),
                )

                // Current operating point dot (hardware) on current curve
                if (showCurrentCurve) {
                    val currentPwm = currentPoints[liveTemp] ?: currentConfig.evaluateAt(liveTemp)
                    val currentY = pwmToY(currentPwm)
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
                }

                // Destination operating point dot (pulsing halo + solid center dot)
                drawCircle(
                    color = colors.primaryBright.copy(alpha = pulseAlpha * 0.35f),
                    radius = (5 + pulseRadius).dp.toPx(),
                    center = Offset(lx, destY),
                )
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

private fun HostFanConfig.evaluatePoints(): Map<Double, Double> {
    val result = mutableMapOf<Double, Double>()
    for (t in 10..90) {
        val temp = t.toDouble()
        result[temp] = evaluateAt(temp)
    }
    return result
}

private fun HostFanConfig.evaluateAt(temp: Double): Double =
    when (this) {
        HostFanConfig.FollowBios -> {
            50.0
        }

        is HostFanConfig.Fixed -> {
            pwmPercent
        }

        is HostFanConfig.AutoCurve -> {
            val points =
                CurveMath.generateCurve(
                    tMin = tMin,
                    tMax = tMax,
                    pMin = pMin,
                    pMax = pMax,
                    shape = shape,
                )
            evaluatePointsAt(points, temp)
        }
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
