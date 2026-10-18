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
sealed interface DeviceTelemetry

@Serializable
@SerialName("octo")
data class OctoTelemetry(
    val fans: List<FanTelemetry>,
    val sensors: List<SensorTelemetry>,
) : DeviceTelemetry

@Serializable
@SerialName("quadro")
data class QuadroTelemetry(
    val fans: List<FanTelemetry>,
    val sensors: List<SensorTelemetry>,
    val flowRateDlH: Int? = null,
) : DeviceTelemetry

@Serializable
@SerialName("d5next")
data class D5NextTelemetry(
    val pump: FanTelemetry,
    val fan: FanTelemetry,
    val coolantTempCelsius: Double? = null,
) : DeviceTelemetry
