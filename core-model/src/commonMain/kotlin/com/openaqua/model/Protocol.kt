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

@Serializable
data class SensorOffset(
    val channel: Int,
    val offsetCelsius: Double,
)

@Serializable
data class DisplayConfig(
    val brightnessPercent: Int,
    val orientationDegrees: Int = 0,
)

@Serializable
sealed interface DeviceConfig

@Serializable
@SerialName("octo")
data class OctoConfig(
    val fans: List<FanChannelConfig>,
    val sensorOffsets: List<SensorOffset>,
) : DeviceConfig

@Serializable
@SerialName("quadro")
data class QuadroConfig(
    val fans: List<FanChannelConfig>,
    val sensorOffsets: List<SensorOffset>,
    val flowPulsesPerLiter: Int? = null,
) : DeviceConfig

@Serializable
@SerialName("d5next")
data class D5NextConfig(
    val pump: FanChannelConfig,
    val fan: FanChannelConfig,
    val coolantOffset: SensorOffset,
    val display: DisplayConfig? = null,
) : DeviceConfig

@Serializable
sealed interface FlashConfigRequest

@Serializable
@SerialName("octo")
data class FlashOctoConfig(
    val fans: Map<Int, FanChannelConfig>? = null,
    val sensorOffsets: Map<Int, Double>? = null,
) : FlashConfigRequest {
    init {
        fans?.keys?.forEach { require(it in 0..7) { "Invalid fan channel $it for OCTO (must be 0..7)" } }
        sensorOffsets?.keys?.forEach { require(it in 0..3) { "Invalid sensor channel $it for OCTO (must be 0..3)" } }
    }
}

@Serializable
@SerialName("quadro")
data class FlashQuadroConfig(
    val fans: Map<Int, FanChannelConfig>? = null,
    val sensorOffsets: Map<Int, Double>? = null,
    val flowPulsesPerLiter: Int? = null,
) : FlashConfigRequest {
    init {
        fans?.keys?.forEach { require(it in 0..3) { "Invalid fan channel $it for QUADRO (must be 0..3)" } }
        sensorOffsets?.keys?.forEach { require(it in 0..3) { "Invalid sensor channel $it for QUADRO (must be 0..3)" } }
    }
}

@Serializable
@SerialName("d5next")
data class FlashD5NextConfig(
    val pump: FanChannelConfig? = null,
    val fan: FanChannelConfig? = null,
    val coolantOffset: Double? = null,
    val display: DisplayConfig? = null,
) : FlashConfigRequest

@Serializable
data class FeedVirtualSensorsRequest(
    val sensors: Map<Int, Double>,
)

@Serializable
@SerialName("error")
data class ErrorResponse(
    val message: String,
) : EngineResponse
