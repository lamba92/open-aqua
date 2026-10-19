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
data class RgbColor(
    val r: Int,
    val g: Int,
    val b: Int,
) {
    init {
        require(r in 0..255) { "Red component must be 0..255 (got $r)" }
        require(g in 0..255) { "Green component must be 0..255 (got $g)" }
        require(b in 0..255) { "Blue component must be 0..255 (got $b)" }
    }
}

@Serializable
enum class RgbEffectMode {
    @SerialName("off")
    OFF,

    @SerialName("static")
    STATIC,

    @SerialName("breathing")
    BREATHING,

    @SerialName("rainbow")
    RAINBOW,

    @SerialName("color-flow")
    COLOR_FLOW,

    @SerialName("temp-reactive")
    TEMP_REACTIVE,
}

@Serializable
data class RgbpxPortConfig(
    val port: Int,
    val enabled: Boolean = true,
    val ledCount: Int,
    val brightnessPercent: Int,
    val mode: RgbEffectMode = RgbEffectMode.STATIC,
    val primaryColor: RgbColor? = null,
    val secondaryColor: RgbColor? = null,
    val sensorIndex: Int? = null,
    val speed: Int? = null,
) {
    init {
        require(port >= 0) { "Port must be non-negative (got $port)" }
        require(ledCount in 0..90) { "LED count must be 0..90 (got $ledCount)" }
        require(brightnessPercent in 0..100) { "Brightness must be 0..100% (got $brightnessPercent)" }
    }
}

@Serializable
sealed interface DeviceConfig

@Serializable
@SerialName("octo")
data class OctoConfig(
    val fans: List<FanChannelConfig>,
    val sensorOffsets: List<SensorOffset>,
    val rgbpx: List<RgbpxPortConfig> = emptyList(),
) : DeviceConfig

@Serializable
@SerialName("quadro")
data class QuadroConfig(
    val fans: List<FanChannelConfig>,
    val sensorOffsets: List<SensorOffset>,
    val flowPulsesPerLiter: Int? = null,
    val rgbpx: List<RgbpxPortConfig> = emptyList(),
) : DeviceConfig

@Serializable
@SerialName("d5next")
data class D5NextConfig(
    val pump: FanChannelConfig,
    val fan: FanChannelConfig,
    val coolantOffset: SensorOffset,
    val display: DisplayConfig? = null,
    val rgbpx: List<RgbpxPortConfig> = emptyList(),
) : DeviceConfig

@Serializable
sealed interface FlashConfigRequest

@Serializable
@SerialName("octo")
data class FlashOctoConfig(
    val fans: Map<Int, FanChannelConfig>? = null,
    val sensorOffsets: Map<Int, Double>? = null,
    val rgbpx: Map<Int, RgbpxPortConfig>? = null,
) : FlashConfigRequest {
    init {
        fans?.keys?.forEach { require(it in 0..7) { "Invalid fan channel $it for OCTO (must be 0..7)" } }
        sensorOffsets?.keys?.forEach { require(it in 0..3) { "Invalid sensor channel $it for OCTO (must be 0..3)" } }
        rgbpx?.keys?.forEach { require(it in 0..1) { "Invalid RGBpx port $it for OCTO (must be 0..1)" } }
    }
}

@Serializable
@SerialName("quadro")
data class FlashQuadroConfig(
    val fans: Map<Int, FanChannelConfig>? = null,
    val sensorOffsets: Map<Int, Double>? = null,
    val flowPulsesPerLiter: Int? = null,
    val rgbpx: Map<Int, RgbpxPortConfig>? = null,
) : FlashConfigRequest {
    init {
        fans?.keys?.forEach { require(it in 0..3) { "Invalid fan channel $it for QUADRO (must be 0..3)" } }
        sensorOffsets?.keys?.forEach { require(it in 0..3) { "Invalid sensor channel $it for QUADRO (must be 0..3)" } }
        rgbpx?.keys?.forEach { require(it == 0) { "Invalid RGBpx port $it for QUADRO (must be 0)" } }
    }
}

@Serializable
@SerialName("d5next")
data class FlashD5NextConfig(
    val pump: FanChannelConfig? = null,
    val fan: FanChannelConfig? = null,
    val coolantOffset: Double? = null,
    val display: DisplayConfig? = null,
    val rgbpx: Map<Int, RgbpxPortConfig>? = null,
) : FlashConfigRequest {
    init {
        rgbpx?.keys?.forEach { require(it == 0) { "Invalid RGBpx port $it for D5 NEXT (must be 0)" } }
    }
}

@Serializable
data class FeedVirtualSensorsRequest(
    val sensors: Map<Int, Double>,
)

@Serializable
@SerialName("error")
data class ErrorResponse(
    val message: String,
) : EngineResponse
