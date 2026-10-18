package com.openaqua.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface EngineResponse

@Serializable
@SerialName("devices")
data class DevicesResponse(
    val devices: List<DetectedHardwareDevice>,
) : EngineResponse

@Serializable
data class DetectedHardwareDevice(
    val id: String,
    val alias: String,
    val type: DeviceType,
    val name: String,
    val serial: String? = null,
    val firmwareVersion: Int? = null,
)

@Serializable
sealed interface FanChannelConfig {
    val channel: Int
}

@Serializable
@SerialName("curve")
data class CurveFanConfig(
    override val channel: Int,
    val sensorIndex: Int,
    val points: List<CurvePoint>,
) : FanChannelConfig

@Serializable
@SerialName("fixed")
data class FixedFanConfig(
    override val channel: Int,
    val powerPercent: Double,
) : FanChannelConfig

@Serializable
@SerialName("target-temp")
data class TargetTempFanConfig(
    override val channel: Int,
    val sensorIndex: Int,
    val targetTempCelsius: Double,
) : FanChannelConfig

@Serializable
@SerialName("two-point")
data class TwoPointFanConfig(
    override val channel: Int,
    val sensorIndex: Int,
) : FanChannelConfig

@Serializable
@SerialName("unconfigured")
data class UnconfiguredFanConfig(
    override val channel: Int,
    val rawMode: Int,
) : FanChannelConfig

@Serializable
@SerialName("unknown")
data class UnknownFanConfig(
    override val channel: Int,
    val rawMode: Int,
) : FanChannelConfig

typealias FanCurve = FanChannelConfig

@Serializable
@SerialName("read-curves")
data class DeviceCurves(
    val deviceId: String,
    val curves: List<FanChannelConfig>,
) : EngineResponse

@Serializable
@SerialName("flash-fan")
data class FlashFanResponse(
    val status: String,
    val deviceId: String,
    val channel: Int,
    val config: FanChannelConfig,
    val bytesWritten: Int,
    val verified: Boolean,
    val error: String? = null,
) : EngineResponse

@Serializable
data class SensorOffset(
    val channel: Int,
    val offsetCelsius: Double,
)

@Serializable
@SerialName("read-offsets")
data class SensorOffsetsResponse(
    val deviceId: String,
    val offsets: List<SensorOffset>,
) : EngineResponse

@Serializable
@SerialName("flash-offsets")
data class SetSensorOffsetResponse(
    val status: String,
    val deviceId: String,
    val channel: Int,
    val offsetCelsius: Double,
    val verified: Boolean = true,
    val error: String? = null,
) : EngineResponse

@Serializable
@SerialName("error")
data class ErrorResponse(
    val message: String,
) : EngineResponse
