package io.github.lamba92.openaqua.app.model

import io.github.lamba92.openaqua.app.theme.AppFontFamily
import io.github.lamba92.openaqua.app.theme.AppMetricsScale
import io.github.lamba92.openaqua.app.theme.FontScaleConstants
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.host.HostSensorId
import io.github.lamba92.openaqua.theme.AppColorScheme
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
    val devices: Map<DeviceId, DeviceStoredData> = emptyMap(),
) {
    companion object {
        val DEFAULT = AppStoredData()
    }
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
) {
    companion object {
        val DEFAULT = SensorStoredData()
    }
}
