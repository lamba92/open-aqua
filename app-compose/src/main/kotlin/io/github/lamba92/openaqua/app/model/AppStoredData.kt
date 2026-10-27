package io.github.lamba92.openaqua.app.model

import io.github.lamba92.openaqua.app.theme.AppFontFamily
import io.github.lamba92.openaqua.app.theme.AppMetricsScale
import io.github.lamba92.openaqua.app.theme.FontScaleConstants
import io.github.lamba92.openaqua.model.DeviceId
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
    val devices: Map<DeviceId, DeviceStoredData> = emptyMap(),
) {
    companion object {
        val DEFAULT = AppStoredData()
    }
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
