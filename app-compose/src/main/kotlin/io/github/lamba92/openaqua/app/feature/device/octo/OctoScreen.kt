package io.github.lamba92.openaqua.app.feature.device.octo

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
fun OctoScreen(
    viewModel: OctoViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        OctoState.Loading -> DeviceLoadingContent(modifier)
        is OctoState.Error -> DeviceErrorContent(s.errorType, modifier)
        is OctoState.Ready -> {
            DeviceDrawerScaffold(
                device = s.deviceState.device,
                selectedTab = s.selectedTab,
                onSelectTab = { viewModel.onEvent(OctoEvent.SelectTab(it)) },
                modifier = modifier,
            ) {
                when (s.selectedTab) {
                    CommonDeviceTab.DEVICE_INFO ->
                        DeviceInfoContent(
                            deviceState = s.deviceState,
                        )
                    CommonDeviceTab.FANS ->
                        FansContent(
                            fans = s.fans,
                            selectedChannel = s.selectedFanChannel,
                            onSelectFan = { viewModel.onEvent(OctoEvent.SelectFan(it)) },
                        )
                    CommonDeviceTab.SENSORS ->
                        SensorsContent(
                            sensors = s.sensors,
                            selectedChannel = s.selectedSensorChannel,
                            onSelectSensor = { viewModel.onEvent(OctoEvent.SelectSensor(it)) },
                        )
                }
            }
        }
    }
}
