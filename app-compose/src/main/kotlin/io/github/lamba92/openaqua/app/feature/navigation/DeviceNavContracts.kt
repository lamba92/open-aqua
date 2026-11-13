package io.github.lamba92.openaqua.app.feature.navigation

import io.github.lamba92.openaqua.app.feature.about.AboutTab
import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.feature.settings.SettingsTab
import io.github.lamba92.openaqua.app.model.HostTab
import io.github.lamba92.openaqua.app.permission.DevicePermissionState
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.device.DeviceId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface NavigationTargets {
    data object Loading : NavigationTargets

    @JvmInline
    value class PermissionError(
        val error: DevicePermissionState.Denied,
    ) : NavigationTargets

    data class Ready(
        val host: DeviceState.Host = DeviceState.Host.Loading,
        val devices: List<DeviceState.Aqua> = emptyList(),
    ) : NavigationTargets
}

data class DeviceNavState(
    val targets: NavigationTargets = NavigationTargets.Loading,
    val devicesWithFeedErrors: Set<DeviceId> = emptySet(),
    val selectedItem: SelectedItem? = null,
    val isExpanded: Boolean = false,
) {
    @Serializable
    sealed interface SelectedItem {
        @Serializable
        @SerialName("host")
        data class Host(
            @SerialName("tab")
            val selectedTab: HostTab = HostTab.SENSORS,
        ) : SelectedItem

        @Serializable
        @SerialName("device")
        data class Device(
            val deviceId: DeviceId,
            @SerialName("tab")
            val selectedTab: CommonDeviceTab = CommonDeviceTab.DEVICE_INFO,
        ) : SelectedItem

        @Serializable
        @SerialName("permission_error")
        data object PermissionError : SelectedItem

        @Serializable
        @SerialName("bug_report")
        data object BugReport : SelectedItem

        @Serializable
        @SerialName("about")
        data class About(
            @SerialName("tab")
            val selectedTab: AboutTab = AboutTab.OPEN_AQUA,
        ) : SelectedItem

        @Serializable
        @SerialName("settings")
        data class Settings(
            @SerialName("tab")
            val selectedTab: SettingsTab = SettingsTab.GENERAL,
        ) : SelectedItem
    }
}

sealed interface DeviceNavEvent {
    data object SelectHost : DeviceNavEvent

    @JvmInline
    value class SelectHostTab(
        val tab: HostTab,
    ) : DeviceNavEvent

    @JvmInline
    value class SelectDevice(
        val deviceId: DeviceId,
    ) : DeviceNavEvent

    @JvmInline
    value class SelectTab(
        val tab: CommonDeviceTab,
    ) : DeviceNavEvent

    data object SelectPermissionError : DeviceNavEvent

    data object SelectBugReport : DeviceNavEvent

    data object SelectAbout : DeviceNavEvent

    @JvmInline
    value class SelectAboutTab(
        val tab: AboutTab,
    ) : DeviceNavEvent

    data object SelectSettings : DeviceNavEvent

    @JvmInline
    value class SelectSettingsTab(
        val tab: SettingsTab,
    ) : DeviceNavEvent

    data object ToggleNavigationRailExpanded : DeviceNavEvent
}

sealed interface DeviceNavEffect
