package io.github.lamba92.openaqua.app.feature.navigation

import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.device.DeviceId

sealed interface DeviceNavState {
    data object Loading : DeviceNavState

    data object NoDevices : DeviceNavState

    data class Ready(
        val devices: List<DeviceState>,
        val selectedItem: SelectedItem,
    ) : DeviceNavState {
        sealed interface SelectedItem {
            data class Device(
                val deviceId: DeviceId,
                val selectedTab: CommonDeviceTab = CommonDeviceTab.DEVICE_INFO,
            ) : SelectedItem

            data object Settings : SelectedItem
        }
    }
}

sealed interface DeviceNavEvent {
    data class SelectDevice(
        val deviceId: DeviceId,
    ) : DeviceNavEvent

    data class SelectTab(
        val tab: CommonDeviceTab,
    ) : DeviceNavEvent

    data object SelectSettings : DeviceNavEvent
}

sealed interface DeviceNavEffect
