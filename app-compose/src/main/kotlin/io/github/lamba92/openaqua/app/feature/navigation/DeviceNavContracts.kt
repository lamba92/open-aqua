package io.github.lamba92.openaqua.app.feature.navigation

import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.model.NavigationSelectionStoredData
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.device.DeviceId

data class DeviceNavState(
    val host: DeviceState.Host = DeviceState.Host.Loading,
    val devices: List<DeviceState.Aqua> = emptyList(),
    val devicesWithFeedErrors: Set<DeviceId> = emptySet(),
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

fun DeviceNavState.SelectedItem.toStoredData(): NavigationSelectionStoredData =
    when (this) {
        DeviceNavState.SelectedItem.Host -> NavigationSelectionStoredData.Host
        is DeviceNavState.SelectedItem.Device -> NavigationSelectionStoredData.Device(deviceId, selectedTab)
        DeviceNavState.SelectedItem.Settings -> NavigationSelectionStoredData.Settings
    }

fun NavigationSelectionStoredData.toSelectedItem(): DeviceNavState.SelectedItem =
    when (this) {
        NavigationSelectionStoredData.Host -> DeviceNavState.SelectedItem.Host
        is NavigationSelectionStoredData.Device -> DeviceNavState.SelectedItem.Device(deviceId, tab)
        NavigationSelectionStoredData.Settings -> DeviceNavState.SelectedItem.Settings
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
