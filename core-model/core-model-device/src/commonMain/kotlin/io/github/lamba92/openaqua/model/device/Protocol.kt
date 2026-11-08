package io.github.lamba92.openaqua.model.device

import io.github.lamba92.openaqua.model.FeatureState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Unique identifier for a detected hardware device.
 *
 * @property value String identifier (e.g. USB sysfs path, serial number, or mock device tag).
 */
@Serializable
@JvmInline
value class DeviceId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "DeviceId cannot be blank" }
    }

    override fun toString(): String = value

    companion object {
        fun from(string: String): DeviceId = DeviceId(string.trim())
    }
}

/**
 * User-assigned friendly alias for a device.
 *
 * @property value Cleaned string alias used in UI and configuration profiles.
 */
@Serializable
@JvmInline
value class DeviceAlias(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "DeviceAlias cannot be blank" }
    }

    override fun toString(): String = value

    companion object {
        fun from(string: String): DeviceAlias = DeviceAlias(string.trim())
    }
}

/**
 * API response container listing all currently connected hardware devices.
 *
 * @property devices List of detected devices.
 */
@Serializable
@SerialName("devices")
data class DevicesResponse(
    val devices: List<DetectedHardwareDevice>,
)

/**
 * API response container listing supported device descriptors and their capability definitions.
 *
 * @property descriptors List of device descriptors.
 */
@Serializable
@SerialName("descriptors")
data class DescriptorsResponse(
    val descriptors: List<DeviceDescriptor>,
)

/**
 * Represents an Aquacomputer device discovered on the system bus.
 *
 * @property id Unique identifier for this device instance.
 * @property alias Friendly display alias.
 * @property type Family classification of the device.
 * @property name Human-readable marketing name.
 * @property serial Optional hardware serial number string.
 * @property firmwareVersion Optional raw firmware version reported by the device.
 */
@Serializable
data class DetectedHardwareDevice(
    val id: DeviceId,
    val alias: DeviceAlias,
    val type: DeviceType,
    val name: String,
    val serial: String? = null,
    val firmwareVersion: Int? = null,
)

/**
 * Common configuration interface for an individual cooling or fan channel.
 *
 * @property channel Zero-based channel index on the device.
 * @property limits Hardware power limits, start boost, and fallback safety parameters.
 */
@Serializable
sealed interface FanChannelConfig {
    val channel: Int
    val limits: FanLimits
}

/**
 * Fan channel configured with a multi-point temperature lookup curve.
 *
 * @property channel Zero-based channel index on the device.
 * @property sensorIndex Logical index of the temperature source controlling this curve.
 * @property points Discrete temperature-to-PWM percentage points.
 * @property limits Hardware power limits and start-boost configuration.
 */
@Serializable
@SerialName("curve")
data class CurveFanConfig(
    override val channel: Int,
    val sensorIndex: Int,
    val points: List<CurvePoint>,
    override val limits: FanLimits = FanLimits.Standard,
) : FanChannelConfig

/**
 * Parametric temperature curve configuration that generates discrete [CurvePoint] entries.
 *
 * @property channel Zero-based channel index on the device.
 * @property sensorIndex Logical index of the temperature sensor source.
 * @property tMin Temperature in Celsius corresponding to [pMin].
 * @property tMax Temperature in Celsius corresponding to [pMax].
 * @property pMin Minimum fan PWM duty percentage (0..100%).
 * @property pMax Maximum fan PWM duty percentage (0..100%).
 * @property shape Curve curvature exponent (-10.0..10.0), where 0.0 is linear, >0.0 is progressive, and <0.0 is digressive.
 * @property limits Hardware power limits and start-boost configuration.
 */
@Serializable
@SerialName("auto-curve")
data class AutoCurveFanConfig(
    override val channel: Int,
    val sensorIndex: Int,
    val tMin: Double,
    val tMax: Double,
    val pMin: Double,
    val pMax: Double,
    val shape: Double = 0.0,
    override val limits: FanLimits = FanLimits.Standard,
) : FanChannelConfig {
    init {
        require(shape in -10.0..10.0) { "Shape must be in range -10.0..10.0 (got $shape)" }
        require(tMin < tMax) { "tMin ($tMin) must be less than tMax ($tMax)" }
        require(pMin in 0.0..100.0 && pMax in 0.0..100.0) { "PWM values must be 0..100%" }
    }

    /**
     * Materializes this parametric curve into a discrete [CurveFanConfig] with 16 points.
     */
    fun toCurveFanConfig(): CurveFanConfig =
        CurveFanConfig(
            channel = channel,
            sensorIndex = sensorIndex,
            points =
                CurveMath.generateCurve(
                    tMin = tMin,
                    tMax = tMax,
                    pMin = pMin,
                    pMax = pMax,
                    shape = shape,
                ),
            limits = limits,
        )
}

/**
 * Fan channel configured with a static, fixed power level.
 *
 * @property channel Zero-based channel index.
 * @property powerPercent Fixed output percentage (0..100%).
 * @property limits Hardware power limits and start-boost configuration.
 */
@Serializable
@SerialName("fixed")
data class FixedFanConfig(
    override val channel: Int,
    val powerPercent: Double,
    override val limits: FanLimits = FanLimits.Standard,
) : FanChannelConfig

/**
 * Fan channel configured in setpoint target-temperature regulation mode.
 *
 * @property channel Zero-based channel index.
 * @property sensorIndex Logical index of the temperature source monitored by the PID controller.
 * @property targetTempCelsius Target setpoint temperature in degrees Celsius.
 * @property limits Hardware power limits and start-boost configuration.
 */
@Serializable
@SerialName("target-temp")
data class TargetTempFanConfig(
    override val channel: Int,
    val sensorIndex: Int,
    val targetTempCelsius: Double,
    override val limits: FanLimits = FanLimits.Standard,
) : FanChannelConfig

/**
 * Fan channel configured in two-point step control mode.
 *
 * @property channel Zero-based channel index.
 * @property sensorIndex Logical index of the temperature source.
 * @property limits Hardware power limits and start-boost configuration.
 */
@Serializable
@SerialName("two-point")
data class TwoPointFanConfig(
    override val channel: Int,
    val sensorIndex: Int,
    override val limits: FanLimits = FanLimits.Standard,
) : FanChannelConfig

/**
 * Fan channel configured to mirror another fan channel's output.
 *
 * @property channel Zero-based channel index of this fan.
 * @property sourceChannel Zero-based channel index of the fan channel being mirrored.
 * @property limits Hardware power limits and start-boost configuration.
 */
@Serializable
@SerialName("copy")
data class CopyFanConfig(
    override val channel: Int,
    val sourceChannel: Int,
    override val limits: FanLimits = FanLimits.Standard,
) : FanChannelConfig {
    init {
        require(channel != sourceChannel) { "Fan channel $channel cannot copy from itself" }
    }
}

/**
 * Fan channel reading where the firmware mode byte does not match known OpenAqua modes.
 *
 * @property channel Zero-based channel index.
 * @property rawMode Unrecognized mode byte value read from device memory.
 * @property limits Hardware power limits and start-boost configuration.
 */
@Serializable
@SerialName("unconfigured")
data class UnconfiguredFanConfig(
    override val channel: Int,
    val rawMode: Int,
    override val limits: FanLimits = FanLimits.Standard,
) : FanChannelConfig

/**
 * Fan channel configuration specifying only hardware limits without altering the active control mode.
 *
 * @property channel Zero-based channel index.
 * @property limits Hardware power limits and start-boost configuration.
 */
@Serializable
@SerialName("limits-only")
data class LimitsOnlyFanConfig(
    override val channel: Int,
    override val limits: FanLimits,
) : FanChannelConfig

/**
 * Returns a copy of this [FanChannelConfig] with updated [FanLimits].
 */
fun FanChannelConfig.withLimits(limits: FanLimits): FanChannelConfig =
    when (this) {
        is CurveFanConfig -> copy(limits = limits)
        is AutoCurveFanConfig -> copy(limits = limits)
        is FixedFanConfig -> copy(limits = limits)
        is TargetTempFanConfig -> copy(limits = limits)
        is TwoPointFanConfig -> copy(limits = limits)
        is CopyFanConfig -> copy(limits = limits)
        is UnconfiguredFanConfig -> copy(limits = limits)
        is LimitsOnlyFanConfig -> copy(limits = limits)
    }

/**
 * Temperature sensor calibration offset.
 *
 * @property channel Zero-based sensor channel index.
 * @property offsetCelsius Signed temperature adjustment in degrees Celsius applied by hardware firmware.
 */
@Serializable
data class SensorOffset(
    val channel: Int,
    val offsetCelsius: Double,
)

/**
 * Display configuration settings.
 *
 * @property brightnessPercent Panel brightness level (0..100%).
 * @property orientationDegrees Display orientation in degrees (typically 0, 90, 180, or 270).
 */
@Serializable
data class DisplayConfig(
    val brightnessPercent: Int,
    val orientationDegrees: Int = 0,
)

/**
 * 24-bit RGB color representation.
 *
 * @property r Red channel intensity (0..255).
 * @property g Green channel intensity (0..255).
 * @property b Blue channel intensity (0..255).
 */
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

/**
 * Lighting effect modes supported by Aquacomputer RGBpx controllers.
 */
@Serializable
enum class RgbEffectMode {
    /** Output disabled. */
    @SerialName("off")
    OFF,

    /** Static solid color illumination. */
    @SerialName("static")
    STATIC,

    /** Pulsing brightness breathing effect. */
    @SerialName("breathing")
    BREATHING,

    /** Animated multi-color rainbow sweep. */
    @SerialName("rainbow")
    RAINBOW,

    /** Smooth color gradient transition. */
    @SerialName("color-flow")
    COLOR_FLOW,

    /** Color changing based on monitored temperature sensor value. */
    @SerialName("temp-reactive")
    TEMP_REACTIVE,
}

/**
 * Configuration for an individual addressable RGBpx port.
 *
 * @property port Zero-based RGBpx port index.
 * @property enabled Whether lighting output is active on this port.
 * @property ledCount Number of addressable LEDs connected (0..90).
 * @property brightnessPercent Overall port brightness percentage (0..100%).
 * @property mode Lighting pattern or reactive effect mode.
 * @property primaryColor Primary RGB color for static, breathing, or gradient baseline.
 * @property secondaryColor Secondary RGB color for gradients or temperature alert thresholds.
 * @property sensorIndex Optional temperature sensor index used when in [RgbEffectMode.TEMP_REACTIVE] mode.
 * @property speed Optional animation speed parameter.
 */
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

/**
 * Hardware power limits and startup safety configuration for a fan channel.
 *
 * @property minPowerPercent Minimum allowed output power percentage (0..100%).
 * @property maxPowerPercent Maximum allowed output power percentage (0..100%).
 * @property startBoost When `true`, hardware briefly pulses 100% power upon startup to guarantee fan spin-up.
 * @property holdMinPower When `true`, hardware will never drop output below [minPowerPercent], preventing stall.
 * @property fallbackPowerPercent Safety power percentage applied by device firmware when USB communication is lost.
 */
@Serializable
data class FanLimits(
    val minPowerPercent: Double,
    val maxPowerPercent: Double,
    val startBoost: Boolean = true,
    val holdMinPower: Boolean = false,
    val fallbackPowerPercent: Double = 100.0,
) {
    init {
        require(minPowerPercent in 0.0..100.0) { "minPowerPercent must be between 0 and 100 (got $minPowerPercent)" }
        require(maxPowerPercent in 0.0..100.0) { "maxPowerPercent must be between 0 and 100 (got $maxPowerPercent)" }
        require(minPowerPercent <= maxPowerPercent) {
            "minPowerPercent ($minPowerPercent) cannot exceed maxPowerPercent ($maxPowerPercent)"
        }
        require(fallbackPowerPercent in 0.0..100.0) { "fallbackPowerPercent must be between 0 and 100 (got $fallbackPowerPercent)" }
    }

    companion object {
        /** Standard unconstrained fan limits (0% min, 100% max, start boost enabled). */
        val Standard =
            FanLimits(
                minPowerPercent = 0.0,
                maxPowerPercent = 100.0,
                startBoost = true,
                holdMinPower = false,
                fallbackPowerPercent = 100.0,
            )
    }
}

/**
 * Complete device configuration state parsed from hardware memory.
 *
 * @property fans List of active cooling channel configurations.
 * @property sensorOffsets List of temperature sensor calibration adjustments.
 * @property flowPulsesPerLiter Flow sensor calibration state (pulses per liter).
 * @property display Display panel configuration state.
 * @property rgbpx RGBpx lighting configurations.
 */
@Serializable
data class DeviceConfig(
    val fans: List<FanChannelConfig> = emptyList(),
    val sensorOffsets: List<SensorOffset> = emptyList(),
    val flowPulsesPerLiter: FeatureState<Int> = FeatureState.Unsupported,
    val display: FeatureState<DisplayConfig> = FeatureState.Unsupported,
    val rgbpx: List<RgbpxPortConfig> = emptyList(),
)

/**
 * Payload sent to write configuration changes to device flash/EEPROM memory.
 *
 * @property fans Map of channel index to desired [FanChannelConfig].
 * @property sensorOffsets Map of sensor channel index to calibration offset in degrees Celsius.
 * @property flowPulsesPerLiter Impulses per liter for flow meter calibration.
 * @property display Desired display settings.
 * @property rgbpx Map of port index to desired [RgbpxPortConfig].
 */
@Serializable
data class FlashConfigRequest(
    val fans: Map<Int, FanChannelConfig>? = null,
    val sensorOffsets: Map<Int, Double>? = null,
    val flowPulsesPerLiter: Int? = null,
    val display: DisplayConfig? = null,
    val rgbpx: Map<Int, RgbpxPortConfig>? = null,
)

/**
 * Periodic payload sent by background host services to supply live software temperature values to device virtual sensors.
 *
 * @property sensors Map of virtual sensor channel index to temperature in degrees Celsius.
 */
@Serializable
data class FeedVirtualSensorsRequest(
    val sensors: Map<Int, Double>,
)
