package io.github.lamba92.openaqua.app.feature.device.quadro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.feature.device.common.DeviceDrawerScaffold
import io.github.lamba92.openaqua.app.feature.device.common.DeviceErrorContent
import io.github.lamba92.openaqua.app.feature.device.common.DeviceInfoContent
import io.github.lamba92.openaqua.app.feature.device.common.DeviceLoadingContent
import io.github.lamba92.openaqua.app.feature.device.common.FansContent
import io.github.lamba92.openaqua.app.feature.device.common.SensorsContent

@Composable
fun QuadroScreen(
    viewModel: QuadroViewModel,
    selectedTab: CommonDeviceTab,
    onSelectTab: (CommonDeviceTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        QuadroState.Loading -> DeviceLoadingContent(modifier)
        is QuadroState.Error -> DeviceErrorContent(s.errorType, modifier)
        is QuadroState.Ready -> {
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
                            selectedChannel = s.selectedFanChannel,
                            onSelectFan = { viewModel.onEvent(QuadroEvent.SelectFan(it)) },
                            onRenameFan = { channel, name ->
                                viewModel.onEvent(QuadroEvent.RenameFan(channel = channel, name = name))
                            },
                        )
                    CommonDeviceTab.SENSORS ->
                        SensorsContent(
                            sensors = s.sensors,
                            selectedChannel = s.selectedSensorChannel,
                            onSelectSensor = { viewModel.onEvent(QuadroEvent.SelectSensor(it)) },
                            onRenameSensor = { channel, name ->
                                viewModel.onEvent(QuadroEvent.RenameSensor(channel = channel, name = name))
                            },
                        )
                }
            }
        }
    }
}
