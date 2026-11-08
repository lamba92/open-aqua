package io.github.lamba92.openaqua.model.device

import io.github.lamba92.openaqua.model.FeatureState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Functional role of a cooling output channel.
 */
@Serializable
enum class CoolingChannelKind {
    /** Fan header channel. */
    FAN,

    /** Pump motor controller channel. */
    PUMP,
}

/**
 * Origin and physical classification of a temperature sensor input.
 */
@Serializable
enum class SensorChannelKind {
    /** Standard 10k 2-pin NTC thermistor probe connected to a physical device header. */
    PHYSICAL_NTC,

    /** Software-fed virtual temperature sensor fed over USB HID. */
    VIRTUAL,

    /** Dedicated probe factory-integrated into the coolant path (e.g. D5 NEXT, High Flow Next). */
    INTEGRATED_COOLANT,
}

/**
 * Live status report telemetry for an individual temperature sensor.
 *
 * @property channel Zero-based channel index of the sensor.
 * @property kind Sensor classification (physical, virtual, or integrated).
 */
@Serializable
sealed interface SensorTelemetry {
    val channel: Int
    val kind: SensorChannelKind

    /**
     * Connected temperature sensor actively providing readings.
     *
     * @property channel Zero-based channel index.
     * @property kind Sensor classification.
     * @property tempCelsius Temperature measurement in degrees Celsius.
     */
    @Serializable
    @SerialName("connected")
    data class Connected(
        override val channel: Int,
        override val kind: SensorChannelKind = SensorChannelKind.PHYSICAL_NTC,
        val tempCelsius: Double,
    ) : SensorTelemetry

    /**
     * Disconnected or unpopulated temperature sensor channel (probe unplugged or virtual feed inactive).
     *
     * @property channel Zero-based channel index.
     * @property kind Sensor classification.
     */
    @Serializable
    @SerialName("disconnected")
    data class Disconnected(
        override val channel: Int,
        override val kind: SensorChannelKind = SensorChannelKind.PHYSICAL_NTC,
    ) : SensorTelemetry
}

val SensorTelemetry.tempCelsiusOrNull: Double?
    get() = (this as? SensorTelemetry.Connected)?.tempCelsius

/**
 * Live status report telemetry for an individual cooling channel (fan or pump).
 *
 * @property channel Zero-based channel index.
 * @property kind Functional role (fan or pump).
 */
@Serializable
sealed interface CoolingChannelTelemetry {
    val channel: Int
    val kind: CoolingChannelKind

    /**
     * Connected and actively reporting cooling channel telemetry.
     *
     * @property channel Zero-based channel index.
     * @property kind Functional role.
     * @property rpm Measured rotational speed in revolutions per minute.
     * @property powerWatts Estimated power dissipation in Watts.
     * @property voltageVolts Output voltage in Volts.
     * @property currentAmps Output current in Amperes.
     * @property pwmPercent Commanded PWM duty cycle percentage (0..100%).
     */
    @Serializable
    @SerialName("connected")
    data class Connected(
        override val channel: Int,
        override val kind: CoolingChannelKind = CoolingChannelKind.FAN,
        val rpm: Int,
        val powerWatts: Double,
        val voltageVolts: Double,
        val currentAmps: Double,
        val pwmPercent: Double,
    ) : CoolingChannelTelemetry

    /**
     * Disconnected or unpopulated cooling channel (tachometer signal absent).
     *
     * @property channel Zero-based channel index.
     * @property kind Functional role.
     */
    @Serializable
    @SerialName("disconnected")
    data class Disconnected(
        override val channel: Int,
        override val kind: CoolingChannelKind = CoolingChannelKind.FAN,
    ) : CoolingChannelTelemetry
}

/**
 * Live status report telemetry for an integrated display panel.
 *
 * @property brightnessPercent Panel brightness percentage (0..100%).
 * @property orientationDegrees Screen orientation rotation angle in degrees.
 */
@Serializable
data class DisplayTelemetry(
    val brightnessPercent: Int,
    val orientationDegrees: Int = 0,
)

/**
 * Aggregated live telemetry snapshot read from a device's status report.
 *
 * @property fans Telemetry for all cooling channels.
 * @property sensors Telemetry for all physical, integrated, and virtual temperature sensors.
 * @property flowRateDlH Monitored coolant flow rate in deciliters per hour (dL/h, divide by 10 for L/h).
 * @property display Telemetry from the device display panel, if supported.
 */
@Serializable
data class DeviceTelemetry(
    val fans: List<CoolingChannelTelemetry> = emptyList(),
    val sensors: List<SensorTelemetry> = emptyList(),
    val flowRateDlH: FeatureState<Int> = FeatureState.Unsupported,
    val display: FeatureState<DisplayTelemetry> = FeatureState.Unsupported,
)
