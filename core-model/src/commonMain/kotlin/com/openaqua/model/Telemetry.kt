package com.openaqua.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SensorTelemetry(
    val channel: Int,
    val tempCelsius: Double?,
    val isVirtual: Boolean = false,
)

@Serializable
data class FanTelemetry(
    val channel: Int,
    val rpm: Int,
    val powerWatts: Double,
    val voltageVolts: Double,
    val currentAmps: Double,
    val pwmPercent: Double? = null,
)

@Serializable
@SerialName("telemetry")
data class DeviceTelemetry(
    val deviceId: String,
    val deviceType: DeviceType,
    val name: String,
    val sensors: List<SensorTelemetry>,
    val fans: List<FanTelemetry>,
    val flowRateDlH: Int? = null,
) : EngineResponse
