package io.github.lamba92.openaqua.app.feature.device.d5next

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.feature.device.common.DeviceDrawerScaffold
import io.github.lamba92.openaqua.app.feature.device.common.DeviceErrorContent
import io.github.lamba92.openaqua.app.feature.device.common.DeviceInfoContent
import io.github.lamba92.openaqua.app.feature.device.common.DeviceLoadingContent
import io.github.lamba92.openaqua.app.feature.device.common.FanChannelDetailCard
import io.github.lamba92.openaqua.app.feature.device.common.FanItem
import io.github.lamba92.openaqua.app.feature.device.common.FanSelectionTile
import io.github.lamba92.openaqua.app.feature.device.common.FansPageScaffold
import io.github.lamba92.openaqua.app.feature.device.common.SensorsContent
import io.github.lamba92.openaqua.app.ui.components.RenameDialog
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.d5_fan_name
import open_aqua.app_compose.generated.resources.d5_fan_pin
import open_aqua.app_compose.generated.resources.d5_pump_name
import open_aqua.app_compose.generated.resources.d5_pump_pin
import open_aqua.app_compose.generated.resources.ic_edit
import open_aqua.app_compose.generated.resources.rename_dialog_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun D5NextScreen(
    viewModel: D5NextViewModel,
    selectedTab: CommonDeviceTab,
    onSelectTab: (CommonDeviceTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        D5NextState.Loading -> DeviceLoadingContent(modifier)
        is D5NextState.Error -> DeviceErrorContent(s.errorType, modifier)
        is D5NextState.Ready -> {
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
                        D5NextCoolingContent(
                            pump = s.pump,
                            fan = s.fan,
                            selectedTarget = s.selectedCoolingTarget,
                            onSelectTarget = { viewModel.onEvent(D5NextEvent.SelectCoolingTarget(it)) },
                            onRenameFan = { channel, name ->
                                viewModel.onEvent(D5NextEvent.RenameFan(channel = channel, name = name))
                            },
                        )
                    CommonDeviceTab.SENSORS ->
                        SensorsContent(
                            sensors = s.sensors,
                            selectedChannel = s.selectedSensorChannel,
                            onSelectSensor = { viewModel.onEvent(D5NextEvent.SelectSensor(it)) },
                            onRenameSensor = { channel, name ->
                                viewModel.onEvent(D5NextEvent.RenameSensor(channel = channel, name = name))
                            },
                        )
                }
            }
        }
    }
}

@Composable
private fun D5NextCoolingContent(
    pump: FanItem,
    fan: FanItem,
    selectedTarget: D5NextCoolingTarget,
    onSelectTarget: (D5NextCoolingTarget) -> Unit,
    modifier: Modifier = Modifier,
    onRenameFan: ((channel: Int, name: String?) -> Unit)? = null,
) {
    val pumpName = pump.customName ?: stringResource(Res.string.d5_pump_name)
    val pumpPin = stringResource(Res.string.d5_pump_pin)
    val fanName = fan.customName ?: stringResource(Res.string.d5_fan_name)
    val fanPin = stringResource(Res.string.d5_fan_pin)
    var renamingFan by remember { mutableStateOf<FanItem?>(null) }

    FansPageScaffold(
        modifier = modifier,
        selectorRow = {
            FanSelectionTile(
                name = pumpName,
                pinLabel = pumpPin,
                rpm = pump.telemetry.rpm,
                selected = selectedTarget == D5NextCoolingTarget.PUMP,
                onClick = { onSelectTarget(D5NextCoolingTarget.PUMP) },
                onDoubleClick = onRenameFan?.let { { renamingFan = pump } },
            )
            FanSelectionTile(
                name = fanName,
                pinLabel = fanPin,
                rpm = fan.telemetry.rpm,
                selected = selectedTarget == D5NextCoolingTarget.FAN,
                onClick = { onSelectTarget(D5NextCoolingTarget.FAN) },
                onDoubleClick = onRenameFan?.let { { renamingFan = fan } },
            )
        },
        detailContent = {
            when (selectedTarget) {
                D5NextCoolingTarget.PUMP ->
                    FanChannelDetailCard(
                        name = pumpName,
                        pinLabel = pumpPin,
                        fan = pump,
                        onRenameClick = onRenameFan?.let { { renamingFan = pump } },
                    )
                D5NextCoolingTarget.FAN ->
                    FanChannelDetailCard(
                        name = fanName,
                        pinLabel = fanPin,
                        fan = fan,
                        onRenameClick = onRenameFan?.let { { renamingFan = fan } },
                    )
            }
        },
    )

    val activeRenamingFan = renamingFan
    if (activeRenamingFan != null && onRenameFan != null) {
        val defaultName =
            if (activeRenamingFan.channel == pump.channel) {
                stringResource(Res.string.d5_pump_name)
            } else {
                stringResource(Res.string.d5_fan_name)
            }
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
