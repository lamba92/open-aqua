package io.github.lamba92.openaqua.app.model

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.theme.AppFontFamily
import io.github.lamba92.openaqua.app.theme.AppMetricsScale
import io.github.lamba92.openaqua.app.theme.FontScaleConstants
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.host.HostSensorId
import io.github.lamba92.openaqua.theme.AppColorScheme
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppStoredData(
    val colorScheme: AppColorScheme = AppColorScheme.HYDRO_DARK,
    val metricsScale: AppMetricsScale = AppMetricsScale.STANDARD,
    val fontSp: Int = FontScaleConstants.DEFAULT_SP,
    val fontFamily: AppFontFamily = AppFontFamily.JETBRAINS_MONO,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val hideDisconnectedFans: Boolean = false,
    val hideDisconnectedSensorsInFans: Boolean = false,
    val hideDisconnectedSensors: Boolean = false,
    val hideUnconfiguredVirtualSensors: Boolean = false,
    val sensorTypes: SensorTypesStoredData = SensorTypesStoredData.DEFAULT,
    val windowState: WindowStateStoredData = WindowStateStoredData.DEFAULT,
    val navigationSelection: NavigationSelectionStoredData? = null,
    val devices: Map<DeviceId, DeviceStoredData> = emptyMap(),
) {
    companion object {
        val DEFAULT = AppStoredData()
    }
}

@Serializable
sealed interface NavigationSelectionStoredData {
    @Serializable
    @SerialName("host")
    data object Host : NavigationSelectionStoredData

    @Serializable
    @SerialName("device")
    data class Device(
        val deviceId: DeviceId,
        val tab: CommonDeviceTab = CommonDeviceTab.DEVICE_INFO,
    ) : NavigationSelectionStoredData

    @Serializable
    @SerialName("settings")
    data object Settings : NavigationSelectionStoredData
}

@Serializable
data class WindowStateStoredData(
    val widthDp: Float = 1380f,
    val heightDp: Float = 880f,
    val xDp: Float? = null,
    val yDp: Float? = null,
    val placement: WindowPlacement = WindowPlacement.Floating,
) {
    companion object {
        val DEFAULT = WindowStateStoredData()
    }
}

fun WindowStateStoredData.toPosition(): WindowPosition =
    when {
        xDp != null && yDp != null -> WindowPosition(xDp.dp, yDp.dp)
        else -> WindowPosition.PlatformDefault
    }

fun WindowStateStoredData.toSize(): DpSize =
    when {
        widthDp > 0f && heightDp > 0f -> DpSize(widthDp.dp, heightDp.dp)
        else -> DpSize(1380.dp, 880.dp)
    }

fun WindowState.toStoredData(previous: WindowStateStoredData = WindowStateStoredData.DEFAULT): WindowStateStoredData {
    val isFloating = placement == WindowPlacement.Floating
    return WindowStateStoredData(
        widthDp = if (isFloating && size.width.value > 0f) size.width.value else previous.widthDp,
        heightDp = if (isFloating && size.height.value > 0f) size.height.value else previous.heightDp,
        xDp = if (isFloating && position.isSpecified) position.x.value else previous.xDp,
        yDp = if (isFloating && position.isSpecified) position.y.value else previous.yDp,
        placement = placement,
    )
}

@Serializable
data class SensorTypesStoredData(
    val host: Map<HostSensorId, SensorTargetType> = emptyMap(),
    val aqua: Map<DeviceId, Map<Int, SensorTargetType>> = emptyMap(),
) {
    companion object {
        val DEFAULT = SensorTypesStoredData()
    }
}

fun SensorTypesStoredData.targetTypeFor(sensorId: HostSensorId): SensorTargetType = host[sensorId] ?: SensorTargetType.DIE

fun SensorTypesStoredData.targetTypeFor(
    deviceId: DeviceId,
    sensorChannel: Int,
): SensorTargetType = aqua[deviceId]?.get(sensorChannel) ?: SensorTargetType.DIE

fun SensorTypesStoredData.withHostSensorType(
    sensorId: HostSensorId,
    type: SensorTargetType,
): SensorTypesStoredData = copy(host = host + (sensorId to type))

fun SensorTypesStoredData.withAquaSensorType(
    deviceId: DeviceId,
    sensorChannel: Int,
    type: SensorTargetType,
): SensorTypesStoredData {
    val currentDeviceSensors = aqua[deviceId] ?: emptyMap()
    val updatedDeviceSensors = currentDeviceSensors + (sensorChannel to type)
    return copy(aqua = aqua + (deviceId to updatedDeviceSensors))
}

@Serializable
sealed interface VirtualSensorFeedSource {
    @Serializable
    @SerialName("host")
    data class Host(
        val sensorId: HostSensorId,
    ) : VirtualSensorFeedSource

    @Serializable
    @SerialName("device")
    data class Device(
        val deviceId: DeviceId,
        val channel: Int,
    ) : VirtualSensorFeedSource
}

@Serializable
data class DeviceStoredData(
    val customName: String? = null,
    val fans: Map<Int, FanStoredData> = emptyMap(),
    val sensors: Map<Int, SensorStoredData> = emptyMap(),
) {
    companion object {
        val DEFAULT = DeviceStoredData()
    }
}

@Serializable
data class FanStoredData(
    val customName: String? = null,
) {
    companion object {
        val DEFAULT = FanStoredData()
    }
}

@Serializable
data class SensorStoredData(
    val customName: String? = null,
    val virtualFeedSource: VirtualSensorFeedSource? = null,
) {
    companion object {
        val DEFAULT = SensorStoredData()
    }
}

fun AppStoredData.withVirtualSensorFeed(
    deviceId: DeviceId,
    sensorChannel: Int,
    feedSource: VirtualSensorFeedSource?,
): AppStoredData {
    val currentDevice = devices[deviceId] ?: DeviceStoredData.DEFAULT
    val currentSensor = currentDevice.sensors[sensorChannel] ?: SensorStoredData.DEFAULT
    val updatedSensors =
        when (val updatedSensor = currentSensor.copy(virtualFeedSource = feedSource)) {
            SensorStoredData.DEFAULT -> currentDevice.sensors - sensorChannel
            else -> currentDevice.sensors + (sensorChannel to updatedSensor)
        }
    val updatedDevice =
        when {
            currentDevice.customName == null && currentDevice.fans.isEmpty() && updatedSensors.isEmpty() -> null
            else -> currentDevice.copy(sensors = updatedSensors)
        }
    val updatedDevices =
        if (updatedDevice != null) {
            devices + (deviceId to updatedDevice)
        } else {
            devices - deviceId
        }
    return copy(devices = updatedDevices)
}
