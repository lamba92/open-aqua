package io.github.lamba92.openaqua.screenshots.model

import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.feature.device.fans.FansUiState
import io.github.lamba92.openaqua.app.feature.device.sensors.SensorsUiState
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavState
import io.github.lamba92.openaqua.app.model.AppStoredData
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.ui.components.FeederStatus
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice
import io.github.lamba92.openaqua.screenshots.mock.createMockNavState
import io.github.lamba92.openaqua.screenshots.mock.mockAquaDeviceState
import io.github.lamba92.openaqua.screenshots.mock.mockOctoFansUiState
import io.github.lamba92.openaqua.screenshots.mock.mockOctoHardwareDevice
import io.github.lamba92.openaqua.screenshots.mock.mockOctoSensorsUiState

/**
 * Encapsulates the visual environment and viewport configuration for taking a screenshot.
 */
data class ScreenshotState(
    val name: String,
    val scene: Scene,
    val logicalWidth: Int = 1440,
    val logicalHeight: Int = 2000,
    val scale: Float = 2f,
    val storedData: AppStoredData = AppStoredData.DEFAULT,
) {
    val filename: String
        get() = "${name.lowercase().replace(' ', '_')}.jpg"
}

/**
 * Sealed hierarchy identifying what screen and data state to render.
 */
sealed interface Scene {
    sealed interface DeviceScreen : Scene {
        val navState: DeviceNavState
        val device: DetectedHardwareDevice get() = mockOctoHardwareDevice
        val feederStatus: FeederStatus get() = FeederStatus.OPERATIONAL

        data class Fans(
            val fansState: FansUiState.Ready = mockOctoFansUiState,
            override val device: DetectedHardwareDevice = mockOctoHardwareDevice,
            override val feederStatus: FeederStatus = FeederStatus.OPERATIONAL,
            override val navState: DeviceNavState =
                createMockNavState(
                    selectedItem =
                        DeviceNavState.SelectedItem.Device(
                            deviceId = device.id,
                            selectedTab = CommonDeviceTab.FANS,
                        ),
                ),
        ) : DeviceScreen

        data class Sensors(
            val sensorsState: SensorsUiState.Ready = mockOctoSensorsUiState,
            override val device: DetectedHardwareDevice = mockOctoHardwareDevice,
            override val feederStatus: FeederStatus = FeederStatus.OPERATIONAL,
            override val navState: DeviceNavState =
                createMockNavState(
                    selectedItem =
                        DeviceNavState.SelectedItem.Device(
                            deviceId = device.id,
                            selectedTab = CommonDeviceTab.SENSORS,
                        ),
                ),
        ) : DeviceScreen

        data class DeviceInfo(
            val deviceState: DeviceState.Aqua = mockAquaDeviceState,
            override val device: DetectedHardwareDevice = mockOctoHardwareDevice,
            override val feederStatus: FeederStatus = FeederStatus.OPERATIONAL,
            override val navState: DeviceNavState =
                createMockNavState(
                    selectedItem =
                        DeviceNavState.SelectedItem.Device(
                            deviceId = device.id,
                            selectedTab = CommonDeviceTab.DEVICE_INFO,
                        ),
                ),
        ) : DeviceScreen
    }

    data class About(
        val navState: DeviceNavState =
            createMockNavState(
                selectedItem = DeviceNavState.SelectedItem.About,
            ),
    ) : Scene
}
