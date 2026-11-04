package io.github.lamba92.openaqua.app.feature.device

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import io.github.lamba92.openaqua.app.di.rememberInstance
import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.feature.device.common.DeviceDrawerScaffold
import io.github.lamba92.openaqua.app.feature.device.common.DeviceErrorContent
import io.github.lamba92.openaqua.app.feature.device.common.DeviceLoadingContent
import io.github.lamba92.openaqua.app.feature.device.fans.FansTabContent
import io.github.lamba92.openaqua.app.feature.device.info.DeviceInfoContent
import io.github.lamba92.openaqua.app.feature.device.sensors.SensorsTabContent
import io.github.lamba92.openaqua.model.device.DeviceId

@Composable
fun DeviceScreen(
    deviceId: DeviceId,
    selectedTab: CommonDeviceTab,
    onSelectTab: (CommonDeviceTab) -> Unit,
    modifier: Modifier = Modifier,
    deviceViewModel: DeviceViewModel = rememberInstance(arg = deviceId),
) {
    val state by deviceViewModel.state.collectAsState()
    val saveableStateHolder = rememberSaveableStateHolder()

    val infoScrollState =
        rememberSaveable(deviceId.value, saver = ScrollState.Saver) {
            ScrollState(0)
        }
    val fansScrollState =
        rememberSaveable(deviceId.value, saver = ScrollState.Saver) {
            ScrollState(0)
        }
    val sensorsScrollState =
        rememberSaveable(deviceId.value, saver = ScrollState.Saver) {
            ScrollState(0)
        }

    when (val s = state) {
        DeviceUiState.Loading -> {
            DeviceLoadingContent(modifier)
        }

        is DeviceUiState.Error -> {
            DeviceErrorContent(s.errorType, modifier)
        }

        is DeviceUiState.Ready -> {
            DeviceDrawerScaffold(
                device = s.deviceState.device,
                selectedTab = selectedTab,
                onSelectTab = onSelectTab,
                modifier = modifier,
                feederStatus = s.status,
            ) {
                saveableStateHolder.SaveableStateProvider(selectedTab) {
                    when (selectedTab) {
                        CommonDeviceTab.DEVICE_INFO -> {
                            DeviceInfoContent(
                                deviceState = s.deviceState,
                                scrollState = infoScrollState,
                            )
                        }

                        CommonDeviceTab.FANS -> {
                            FansTabContent(
                                deviceId = deviceId,
                                scrollState = fansScrollState,
                            )
                        }

                        CommonDeviceTab.SENSORS -> {
                            SensorsTabContent(
                                deviceId = deviceId,
                                scrollState = sensorsScrollState,
                            )
                        }
                    }
                }
            }
        }
    }
}
