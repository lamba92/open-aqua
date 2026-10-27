package io.github.lamba92.openaqua.app.feature.device

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.feature.device.common.DeviceDrawerScaffold
import io.github.lamba92.openaqua.app.feature.device.common.DeviceErrorContent
import io.github.lamba92.openaqua.app.feature.device.common.DeviceInfoContent
import io.github.lamba92.openaqua.app.feature.device.common.DeviceLoadingContent
import io.github.lamba92.openaqua.app.feature.device.common.FanItem
import io.github.lamba92.openaqua.app.feature.device.common.FansContent
import io.github.lamba92.openaqua.app.feature.device.common.SensorItem
import io.github.lamba92.openaqua.app.feature.device.common.SensorsContent
import io.github.lamba92.openaqua.app.theme.BrandPureWhite
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.ConfirmationDialog
import io.github.lamba92.openaqua.model.AutoCurveFanConfig
import io.github.lamba92.openaqua.model.CoolingChannelKind
import io.github.lamba92.openaqua.model.CurveFanConfig
import io.github.lamba92.openaqua.model.FanChannelConfig
import io.github.lamba92.openaqua.model.FixedFanConfig
import io.github.lamba92.openaqua.model.SensorChannelKind
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.curve_action_flashing
import open_aqua.app_compose.generated.resources.d5_fan_name
import open_aqua.app_compose.generated.resources.d5_pump_name
import open_aqua.app_compose.generated.resources.fan_channel_name
import open_aqua.app_compose.generated.resources.flash_dialog_cancel_btn
import open_aqua.app_compose.generated.resources.flash_dialog_confirm_btn
import open_aqua.app_compose.generated.resources.flash_dialog_desc
import open_aqua.app_compose.generated.resources.flash_dialog_title
import open_aqua.app_compose.generated.resources.ic_fan
import open_aqua.app_compose.generated.resources.ic_speed
import open_aqua.app_compose.generated.resources.sensor_channel_name
import open_aqua.app_compose.generated.resources.sensor_coolant_name
import open_aqua.app_compose.generated.resources.sensor_virtual_name
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun DeviceScreen(
    viewModel: DeviceViewModel,
    selectedTab: CommonDeviceTab,
    onSelectTab: (CommonDeviceTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        DeviceUiState.Loading -> DeviceLoadingContent(modifier)
        is DeviceUiState.Error -> DeviceErrorContent(s.errorType, modifier)
        is DeviceUiState.Ready -> {
            DeviceDrawerScaffold(
                device = s.deviceState.device,
                selectedTab = selectedTab,
                onSelectTab = onSelectTab,
                modifier = modifier,
            ) {
                when (selectedTab) {
                    CommonDeviceTab.DEVICE_INFO ->
                        DeviceInfoContent(
                            deviceState = s.deviceState,
                        )
                    CommonDeviceTab.FANS ->
                        FansContent(
                            fans = s.fans,
                            sensors = s.sensors,
                            selectedChannel = s.selectedFanChannel,
                            fanDrafts = s.fanDrafts,
                            isFanModified = { s.isFanModified(it) },
                            pendingFansCount = s.pendingFans.size,
                            onSelectFan = { viewModel.onEvent(DeviceUiEvent.SelectFan(it)) },
                            onRenameFan = { channel, name ->
                                viewModel.onEvent(DeviceUiEvent.RenameFan(channel = channel, name = name))
                            },
                            onCurveStudioEvent = { viewModel.onEvent(it) },
                            onDiscardDraft = {
                                viewModel.onEvent(DeviceUiEvent.DiscardFanDraft)
                            },
                            onRequestFlash = {
                                viewModel.onEvent(DeviceUiEvent.RequestFlash)
                            },
                        )
                    CommonDeviceTab.SENSORS ->
                        SensorsContent(
                            sensors = s.sensors,
                            selectedChannel = s.selectedSensorChannel,
                            onSelectSensor = { viewModel.onEvent(DeviceUiEvent.SelectSensor(it)) },
                            onRenameSensor = { channel, name ->
                                viewModel.onEvent(DeviceUiEvent.RenameSensor(channel = channel, name = name))
                            },
                        )
                }
            }

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
                    onConfirm = { viewModel.onEvent(DeviceUiEvent.ConfirmFlash) },
                    onDismiss = { viewModel.onEvent(DeviceUiEvent.DismissFlashDialog) },
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
