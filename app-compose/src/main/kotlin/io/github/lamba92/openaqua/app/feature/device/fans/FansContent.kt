package io.github.lamba92.openaqua.app.feature.device.fans

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.di.rememberInstance
import io.github.lamba92.openaqua.app.feature.device.common.DeviceLoadingContent
import io.github.lamba92.openaqua.app.feature.device.common.FanItem
import io.github.lamba92.openaqua.app.feature.device.common.SensorItem
import io.github.lamba92.openaqua.app.theme.BrandPureWhite
import io.github.lamba92.openaqua.app.theme.CoolantTeal
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.ChannelDetailHeader
import io.github.lamba92.openaqua.app.ui.components.ChannelPageScaffold
import io.github.lamba92.openaqua.app.ui.components.ChannelSelectionCard
import io.github.lamba92.openaqua.app.ui.components.ChannelSelectionTileDefaults
import io.github.lamba92.openaqua.app.ui.components.ConfirmationDialog
import io.github.lamba92.openaqua.app.ui.components.MetricCard
import io.github.lamba92.openaqua.app.ui.components.OpenAquaButton
import io.github.lamba92.openaqua.app.ui.components.OpenAquaOutlinedButton
import io.github.lamba92.openaqua.app.ui.components.RenameDialog
import io.github.lamba92.openaqua.app.ui.components.VisibilityEmptyState
import io.github.lamba92.openaqua.app.ui.components.VisibilityFilterChip
import io.github.lamba92.openaqua.model.AutoCurveFanConfig
import io.github.lamba92.openaqua.model.CoolingChannelKind
import io.github.lamba92.openaqua.model.CoolingChannelTelemetry
import io.github.lamba92.openaqua.model.CurveFanConfig
import io.github.lamba92.openaqua.model.DeviceId
import io.github.lamba92.openaqua.model.FanChannelConfig
import io.github.lamba92.openaqua.model.FixedFanConfig
import io.github.lamba92.openaqua.model.SensorChannelKind
import io.github.lamba92.openaqua.model.TargetTempFanConfig
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.action_rename
import open_aqua.app_compose.generated.resources.curve_action_apply
import open_aqua.app_compose.generated.resources.curve_action_discard
import open_aqua.app_compose.generated.resources.curve_action_flash_all
import open_aqua.app_compose.generated.resources.curve_action_flashing
import open_aqua.app_compose.generated.resources.d5_fan_name
import open_aqua.app_compose.generated.resources.d5_fan_pin
import open_aqua.app_compose.generated.resources.d5_pump_name
import open_aqua.app_compose.generated.resources.d5_pump_pin
import open_aqua.app_compose.generated.resources.fan_channel_name
import open_aqua.app_compose.generated.resources.fan_channel_pin
import open_aqua.app_compose.generated.resources.fan_channels_header
import open_aqua.app_compose.generated.resources.fan_current_value
import open_aqua.app_compose.generated.resources.fan_disconnected
import open_aqua.app_compose.generated.resources.fan_hardware_pin
import open_aqua.app_compose.generated.resources.fan_power_value
import open_aqua.app_compose.generated.resources.fan_pwm_value
import open_aqua.app_compose.generated.resources.fan_rpm_short_disconnected
import open_aqua.app_compose.generated.resources.fan_rpm_value
import open_aqua.app_compose.generated.resources.fan_settings_title
import open_aqua.app_compose.generated.resources.fan_voltage_value
import open_aqua.app_compose.generated.resources.fans_screen_subtitle
import open_aqua.app_compose.generated.resources.fans_screen_title
import open_aqua.app_compose.generated.resources.flash_dialog_cancel_btn
import open_aqua.app_compose.generated.resources.flash_dialog_confirm_btn
import open_aqua.app_compose.generated.resources.flash_dialog_desc
import open_aqua.app_compose.generated.resources.flash_dialog_title
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
import open_aqua.app_compose.generated.resources.sensor_channel_name
import open_aqua.app_compose.generated.resources.sensor_coolant_name
import open_aqua.app_compose.generated.resources.sensor_virtual_name
import open_aqua.app_compose.generated.resources.visibility_no_fans_visible
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun FansContent(
    fans: List<FanItem>,
    sensors: List<SensorItem>,
    selectedChannel: Int,
    fanDrafts: Map<Int, FanDraft>,
    isFanModified: (FanItem) -> Boolean,
    pendingFansCount: Int,
    onSelectFan: (Int) -> Unit,
    onCurveStudioEvent: (CurveStudioEvent) -> Unit,
    onDiscardDraft: () -> Unit,
    onRequestFlash: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    onRenameFan: ((channel: Int, name: String?) -> Unit)? = null,
) {
    val preferences = OpenAquaTheme.preferences
    var showAllOverride by remember { mutableStateOf(false) }

    val isFanHidden: (FanItem) -> Boolean = { fan ->
        preferences.hideDisconnectedFans && fan.telemetry is CoolingChannelTelemetry.Disconnected
    }

    val hiddenCount =
        remember(fans, preferences.hideDisconnectedFans) {
            fans.count(isFanHidden)
        }

    val displayedFans =
        remember(fans, showAllOverride, preferences.hideDisconnectedFans) {
            if (showAllOverride || !preferences.hideDisconnectedFans) {
                fans
            } else {
                fans.filterNot(isFanHidden)
            }
        }

    val selectedFan = displayedFans.firstOrNull { it.channel == selectedChannel } ?: displayedFans.firstOrNull()
    var renamingFan by remember { mutableStateOf<FanItem?>(null) }
    val hasPump = remember(fans) { fans.any { it.kind == CoolingChannelKind.PUMP } }

    FansPageScaffold(
        modifier = modifier,
        scrollState = scrollState,
        selectorHeaderTrailing =
            if (hiddenCount > 0 || showAllOverride) {
                {
                    VisibilityFilterChip(
                        hiddenCount = hiddenCount,
                        showingAll = showAllOverride,
                        onClick = { showAllOverride = !showAllOverride },
                    )
                }
            } else {
                null
            },
        selectorContent = {
            displayedFans.forEach { fan ->
                val name = fan.customName ?: defaultCoolingName(fan, hasPump)
                val pinLabel = defaultCoolingPin(fan, hasPump)
                val icon =
                    if (fan.kind == CoolingChannelKind.PUMP) {
                        painterResource(Res.drawable.ic_speed)
                    } else {
                        painterResource(Res.drawable.ic_fan)
                    }
                val connectedTel = fan.telemetry as? CoolingChannelTelemetry.Connected
                val hasPending = isFanModified(fan)
                FanSelectionTile(
                    name = name,
                    pinLabel = pinLabel,
                    rpm = connectedTel?.rpm,
                    pwmPercent = connectedTel?.pwmPercent,
                    icon = icon,
                    selected = fan.channel == selectedFan?.channel,
                    hasPendingChanges = hasPending,
                    onClick = { onSelectFan(fan.channel) },
                    onDoubleClick = onRenameFan?.let { { renamingFan = fan } },
                )
            }
        },
        detailContent = {
            if (displayedFans.isEmpty()) {
                VisibilityEmptyState(
                    message = stringResource(Res.string.visibility_no_fans_visible),
                    onShowAll = { showAllOverride = true },
                )
            } else if (selectedFan != null) {
                val name = selectedFan.customName ?: defaultCoolingName(selectedFan, hasPump)
                val pinLabel = defaultCoolingPin(selectedFan, hasPump)
                val draft =
                    fanDrafts[selectedFan.channel] ?: selectedFan.config.toInitialDraft(
                        channel = selectedFan.channel,
                        defaultSensorIndex = sensors.firstOrNull()?.channel ?: 0,
                    )
                val isModified = isFanModified(selectedFan)

                FanChannelDetailCard(
                    name = name,
                    pinLabel = pinLabel,
                    fan = selectedFan,
                    draft = draft,
                    allFans = fans,
                    sensors = sensors,
                    isModified = isModified,
                    pendingFansCount = pendingFansCount,
                    onCurveStudioEvent = onCurveStudioEvent,
                    onDiscard = onDiscardDraft,
                    onRequestFlash = onRequestFlash,
                    onRenameClick = onRenameFan?.let { { renamingFan = selectedFan } },
                )
            }
        },
    )

    val activeRenamingFan = renamingFan
    if (activeRenamingFan != null && onRenameFan != null) {
        val defaultName = defaultCoolingName(activeRenamingFan, hasPump)
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
private fun defaultCoolingName(
    fan: FanItem,
    hasPump: Boolean,
): String =
    when (fan.kind) {
        CoolingChannelKind.PUMP -> stringResource(Res.string.d5_pump_name)
        CoolingChannelKind.FAN ->
            if (fan.channel == 1 && hasPump) {
                stringResource(Res.string.d5_fan_name)
            } else {
                stringResource(Res.string.fan_channel_name, fan.channel + 1)
            }
    }

@Composable
private fun defaultCoolingPin(
    fan: FanItem,
    hasPump: Boolean,
): String =
    when (fan.kind) {
        CoolingChannelKind.PUMP -> stringResource(Res.string.d5_pump_pin)
        CoolingChannelKind.FAN ->
            if (fan.channel == 1 && hasPump) {
                stringResource(Res.string.d5_fan_pin)
            } else {
                stringResource(Res.string.fan_channel_pin, fan.channel + 1)
            }
    }

@Composable
fun FansPageScaffold(
    detailContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    selectorHeaderTrailing: (@Composable RowScope.() -> Unit)? = null,
    selectorContent: @Composable () -> Unit,
) {
    ChannelPageScaffold(
        title = stringResource(Res.string.fans_screen_title),
        subtitle = stringResource(Res.string.fans_screen_subtitle),
        selectorHeader = stringResource(Res.string.fan_channels_header),
        selectorHeaderTrailing = selectorHeaderTrailing,
        selectorContent = selectorContent,
        detailContent = detailContent,
        scrollState = scrollState,
        modifier = modifier,
    )
}

@Composable
fun FanSelectionTile(
    name: String,
    pinLabel: String,
    rpm: Int?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter = painterResource(Res.drawable.ic_fan),
    pwmPercent: Double?,
    hasPendingChanges: Boolean = false,
    onDoubleClick: (() -> Unit)? = null,
) {
    val colors = OpenAquaTheme.colors
    val metrics = ChannelSelectionTileDefaults.metrics()
    val isDisconnected = rpm == null
    val fillFraction =
        if (isDisconnected) {
            0f
        } else {
            (pwmPercent?.let { (it / 100.0).toFloat() } ?: (if (rpm > 0) 0.5f else 0f)).coerceIn(0f, 1f)
        }

    val valueText =
        rpm?.let {
            stringResource(Res.string.fan_rpm_value, it.coerceAtLeast(0))
        } ?: stringResource(Res.string.fan_rpm_short_disconnected)

    val secondaryValueText =
        if (!isDisconnected && pwmPercent != null) {
            stringResource(Res.string.fan_pwm_value, "%.0f".format(pwmPercent))
        } else {
            null
        }

    ChannelSelectionCard(
        name = name,
        badgeText = pinLabel,
        valueText = valueText,
        secondaryValueText = secondaryValueText,
        selected = selected,
        hasPendingChanges = hasPendingChanges,
        onClick = onClick,
        onDoubleClick = onDoubleClick,
        fillFraction = fillFraction,
        fillColor = colors.primary,
        modifier = modifier,
        metrics = metrics,
        icon = {
            Icon(
                painter = icon,
                contentDescription = name,
                tint =
                    if (selected) {
                        colors.primaryBright
                    } else if (!isDisconnected && rpm > 0) {
                        CoolantTeal
                    } else {
                        colors.textMuted
                    },
                modifier = Modifier.size(metrics.iconSize),
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
    draft: FanDraft,
    allFans: List<FanItem>,
    sensors: List<SensorItem>,
    isModified: Boolean,
    pendingFansCount: Int,
    onCurveStudioEvent: (CurveStudioEvent) -> Unit,
    onDiscard: () -> Unit,
    onRequestFlash: () -> Unit,
    modifier: Modifier = Modifier,
    onRenameClick: (() -> Unit)? = null,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

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
            icon =
                if (fan.kind == CoolingChannelKind.PUMP) {
                    painterResource(Res.drawable.ic_speed)
                } else {
                    painterResource(Res.drawable.ic_tune)
                },
            title = stringResource(Res.string.fan_settings_title, name),
            subtitle = stringResource(Res.string.fan_hardware_pin, pinLabel),
            trailingContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isModified) {
                        OpenAquaOutlinedButton(
                            text = stringResource(Res.string.curve_action_discard),
                            onClick = onDiscard,
                        )
                    }

                    if (pendingFansCount > 0) {
                        val flashLabel =
                            if (pendingFansCount > 1) {
                                stringResource(Res.string.curve_action_flash_all, pendingFansCount)
                            } else {
                                stringResource(Res.string.curve_action_apply)
                            }

                        OpenAquaButton(
                            text = flashLabel,
                            onClick = onRequestFlash,
                        )
                    }

                    if (onRenameClick != null) {
                        OpenAquaOutlinedButton(
                            text = stringResource(Res.string.action_rename),
                            icon = painterResource(Res.drawable.ic_edit),
                            onClick = onRenameClick,
                        )
                    }
                }
            },
        )

        // Live metrics telemetry
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(metrics.spacingMd),
            verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
        ) {
            when (val tel = fan.telemetry) {
                is CoolingChannelTelemetry.Connected -> {
                    FanMetricCard(
                        label = stringResource(Res.string.metric_speed),
                        value = stringResource(Res.string.fan_rpm_value, tel.rpm),
                    )
                    FanMetricCard(
                        label = stringResource(Res.string.metric_pwm_duty),
                        value = stringResource(Res.string.fan_pwm_value, "%.1f".format(tel.pwmPercent)),
                    )
                    FanMetricCard(
                        label = stringResource(Res.string.metric_voltage),
                        value = stringResource(Res.string.fan_voltage_value, "%.2f".format(tel.voltageVolts)),
                    )
                    FanMetricCard(
                        label = stringResource(Res.string.metric_current),
                        value = stringResource(Res.string.fan_current_value, "%.3f".format(tel.currentAmps)),
                    )
                    FanMetricCard(
                        label = stringResource(Res.string.metric_power),
                        value = stringResource(Res.string.fan_power_value, "%.2f".format(tel.powerWatts)),
                    )
                }

                is CoolingChannelTelemetry.Disconnected -> {
                    FanMetricCard(
                        label = stringResource(Res.string.metric_speed),
                        value = stringResource(Res.string.fan_disconnected),
                    )
                }
            }
        }

        // Curve Studio
        CurveStudio(
            fan = fan,
            draft = draft,
            allFans = allFans,
            sensors = sensors,
            onEvent = onCurveStudioEvent,
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
        modifier = modifier,
    )
}

@Composable
fun FansTabContent(
    deviceId: DeviceId,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) = FansTabContent(
    viewModel = rememberInstance(arg = deviceId),
    modifier = modifier,
    scrollState = scrollState,
)

@Composable
fun FansTabContent(
    viewModel: FansViewModel,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        FansUiState.Loading -> DeviceLoadingContent(modifier)
        is FansUiState.Ready -> {
            FansContent(
                fans = s.fans,
                sensors = s.sensors,
                selectedChannel = s.selectedFanChannel,
                fanDrafts = s.fanDrafts,
                isFanModified = { s.isFanModified(it) },
                pendingFansCount = s.pendingFans.size,
                onSelectFan = { viewModel.onEvent(FansUiEvent.SelectFan(it)) },
                onRenameFan = { channel, name ->
                    viewModel.onEvent(FansUiEvent.RenameFan(channel = channel, name = name))
                },
                onCurveStudioEvent = { viewModel.onEvent(it) },
                onDiscardDraft = { viewModel.onEvent(FansUiEvent.DiscardFanDraft) },
                onRequestFlash = { viewModel.onEvent(FansUiEvent.RequestFlash) },
                modifier = modifier,
                scrollState = scrollState,
            )

            if (s.flashDialogState !is FlashDialogState.NotShown) {
                val isFlashing = s.flashDialogState is FlashDialogState.Flashing
                val colors = OpenAquaTheme.colors

                ConfirmationDialog(
                    title = stringResource(Res.string.flash_dialog_title),
                    confirmText = stringResource(Res.string.flash_dialog_confirm_btn),
                    dismissText = stringResource(Res.string.flash_dialog_cancel_btn),
                    isLoading = isFlashing,
                    loadingText = stringResource(Res.string.curve_action_flashing),
                    confirmContainerColor = colors.primary,
                    confirmContentColor = BrandPureWhite,
                    dismissContainerColor = colors.surfaceHigh,
                    dismissContentColor = colors.textPrimary,
                    onConfirm = { viewModel.onEvent(FansUiEvent.ConfirmFlash) },
                    onDismiss = { viewModel.onEvent(FansUiEvent.DismissFlashDialog) },
                    width = 480.dp,
                    content = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.flash_dialog_desc),
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                            )
                            s.pendingFans.forEach { (fan, targetConfig) ->
                                PendingFanDiffCard(
                                    fan = fan,
                                    targetConfig = targetConfig,
                                    sensors = s.sensors,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun PendingFanDiffCard(
    fan: FanItem,
    targetConfig: FanChannelConfig,
    sensors: List<SensorItem>,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    val fanName =
        fan.customName
            ?: when (fan.kind) {
                CoolingChannelKind.PUMP -> stringResource(Res.string.d5_pump_name)
                CoolingChannelKind.FAN ->
                    if (fan.channel == 1) {
                        stringResource(Res.string.d5_fan_name)
                    } else {
                        stringResource(Res.string.fan_channel_name, fan.channel + 1)
                    }
            }

    val icon =
        if (fan.kind == CoolingChannelKind.PUMP) {
            painterResource(Res.drawable.ic_speed)
        } else {
            painterResource(Res.drawable.ic_fan)
        }

    val summary =
        when (targetConfig) {
            is FixedFanConfig -> "Fixed PWM: ${targetConfig.powerPercent.roundToInt()}%"
            is AutoCurveFanConfig -> {
                val sensorName =
                    sensors.firstOrNull { it.channel == targetConfig.sensorIndex }?.let { sensorName(it) }
                        ?: "Sensor ${targetConfig.sensorIndex + 1}"
                "Auto Curve • $sensorName • ${targetConfig.tMin.roundToInt()}°C - ${targetConfig.tMax.roundToInt()}°C (${targetConfig.pMin.roundToInt()}% - ${targetConfig.pMax.roundToInt()}%)"
            }
            is CurveFanConfig -> {
                val sensorName =
                    sensors.firstOrNull { it.channel == targetConfig.sensorIndex }?.let { sensorName(it) }
                        ?: "Sensor ${targetConfig.sensorIndex + 1}"
                "Custom Curve • $sensorName • 16 points"
            }
            is TargetTempFanConfig -> {
                val sensorName =
                    sensors.firstOrNull { it.channel == targetConfig.sensorIndex }?.let { sensorName(it) }
                        ?: "Sensor ${targetConfig.sensorIndex + 1}"
                "Target Temp • $sensorName • ${"%.1f".format(targetConfig.targetTempCelsius)}°C"
            }
            else -> "Custom Config"
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(metrics.radiusSmall))
                .background(colors.surfaceLowest)
                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusSmall))
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = colors.primaryBright,
            modifier = Modifier.size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = fanName,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = summary,
                color = colors.textMuted,
                fontSize = 11.sp,
                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
            )
        }
    }
}

@Composable
private fun sensorName(sensor: SensorItem): String =
    sensor.customName
        ?: when (sensor.kind) {
            SensorChannelKind.INTEGRATED_COOLANT -> stringResource(Res.string.sensor_coolant_name)
            SensorChannelKind.VIRTUAL -> stringResource(Res.string.sensor_virtual_name, sensor.channel + 1)
            else -> stringResource(Res.string.sensor_channel_name, sensor.channel + 1)
        }
