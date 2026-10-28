package io.github.lamba92.openaqua.model.device

import io.github.lamba92.openaqua.model.FeatureState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CoolingChannelKind {
    FAN,
    PUMP,
}

@Serializable
enum class SensorChannelKind {
    PHYSICAL_NTC,
    VIRTUAL,
    INTEGRATED_COOLANT,
}

@Serializable
sealed interface SensorTelemetry {
    val channel: Int
    val kind: SensorChannelKind

    @Serializable
    @SerialName("connected")
    data class Connected(
        override val channel: Int,
        override val kind: SensorChannelKind = SensorChannelKind.PHYSICAL_NTC,
        val tempCelsius: Double,
    ) : SensorTelemetry

    @Serializable
    @SerialName("disconnected")
    data class Disconnected(
        override val channel: Int,
        override val kind: SensorChannelKind = SensorChannelKind.PHYSICAL_NTC,
    ) : SensorTelemetry
}

val SensorTelemetry.tempCelsiusOrNull: Double?
    get() = (this as? SensorTelemetry.Connected)?.tempCelsius

@Serializable
sealed interface CoolingChannelTelemetry {
    val channel: Int
    val kind: CoolingChannelKind

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

    @Serializable
    @SerialName("disconnected")
    data class Disconnected(
        override val channel: Int,
        override val kind: CoolingChannelKind = CoolingChannelKind.FAN,
    ) : CoolingChannelTelemetry
}

@Serializable
data class DisplayTelemetry(
    val brightnessPercent: Int,
    val orientationDegrees: Int = 0,
)

@Serializable
data class DeviceTelemetry(
    val fans: List<CoolingChannelTelemetry> = emptyList(),
    val sensors: List<SensorTelemetry> = emptyList(),
    val flowRateDlH: FeatureState<Int> = FeatureState.Unsupported,
    val display: FeatureState<DisplayTelemetry> = FeatureState.Unsupported,
)
