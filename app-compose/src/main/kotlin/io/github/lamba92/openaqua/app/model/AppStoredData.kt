package io.github.lamba92.openaqua.app.model

import io.github.lamba92.openaqua.model.DeviceId
import kotlinx.serialization.Serializable

@Serializable
data class AppStoredData(
    val devices: Map<DeviceId, DeviceStoredData> = emptyMap(),
)

@Serializable
data class DeviceStoredData(
    val customName: String? = null,
    val fans: Map<Int, FanStoredData> = emptyMap(),
    val sensors: Map<Int, SensorStoredData> = emptyMap(),
)

@Serializable
data class FanStoredData(
    val customName: String? = null,
)

@Serializable
data class SensorStoredData(
    val customName: String? = null,
)
