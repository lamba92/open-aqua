package io.github.lamba92.openaqua.app.feature.navigation

import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.device.DeviceId

data class DeviceNavState(
    val host: DeviceState.Host = DeviceState.Host.Loading,
    val devices: List<DeviceState.Aqua> = emptyList(),
    val selectedItem: SelectedItem? = null,
) {
    sealed interface SelectedItem {
        data object Host : SelectedItem

        data class Device(
            val deviceId: DeviceId,
            val selectedTab: CommonDeviceTab = CommonDeviceTab.DEVICE_INFO,
        ) : SelectedItem

        data object Settings : SelectedItem
    }
}

sealed interface DeviceNavEvent {
    data object SelectHost : DeviceNavEvent

    data class SelectDevice(
        val deviceId: DeviceId,
    ) : DeviceNavEvent

    data class SelectTab(
        val tab: CommonDeviceTab,
    ) : DeviceNavEvent

    data object SelectSettings : DeviceNavEvent
}

sealed interface DeviceNavEffect
