package io.github.lamba92.openaqua.model.host

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class HostDeviceId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "HostDeviceId cannot be blank" }
    }

    override fun toString(): String = value

    companion object {
        fun from(string: String): HostDeviceId = HostDeviceId(string.trim())
    }
}

@Serializable
@JvmInline
value class HostSensorId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "HostSensorId cannot be blank" }
    }

    override fun toString(): String = value

    companion object {
        fun from(string: String): HostSensorId = HostSensorId(string.trim())
    }
}

@Serializable
@JvmInline
value class HostFanId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "HostFanId cannot be blank" }
    }

    override fun toString(): String = value

    companion object {
        fun from(string: String): HostFanId = HostFanId(string.trim())
    }
}

@Serializable
enum class HostDeviceType {
    CPU,
    MOTHERBOARD,
    GPU,
    RAM,
    STORAGE,
    OTHER,
}

@Serializable
data class HostSensorDescriptor(
    val id: HostSensorId,
    val name: String,
)

@Serializable
sealed interface HostFanDescriptor {
    val id: HostFanId
    val name: String

    @Serializable
    @SerialName("controllable")
    data class Controllable(
        override val id: HostFanId,
        override val name: String,
    ) : HostFanDescriptor

    @Serializable
    @SerialName("read-only")
    data class ReadOnly(
        override val id: HostFanId,
        override val name: String,
    ) : HostFanDescriptor
}

val HostFanDescriptor.isControllable: Boolean
    get() = this is HostFanDescriptor.Controllable

@Serializable
data class HostDevice(
    val id: HostDeviceId,
    val name: String,
    val type: HostDeviceType,
    val sensors: List<HostSensorDescriptor> = emptyList(),
    val fans: List<HostFanDescriptor> = emptyList(),
)

@Serializable
@SerialName("host-info")
data class HostInfo(
    val devices: List<HostDevice>,
)

@Serializable
sealed interface HostSensorTelemetry {
    val id: HostSensorId

    @Serializable
    @SerialName("connected")
    data class Connected(
        override val id: HostSensorId,
        val tempCelsius: Double,
    ) : HostSensorTelemetry

    @Serializable
    @SerialName("disconnected")
    data class Disconnected(
        override val id: HostSensorId,
    ) : HostSensorTelemetry
}

@Serializable
sealed interface HostFanTelemetry {
    val id: HostFanId

    @Serializable
    @SerialName("controllable")
    data class Controllable(
        override val id: HostFanId,
        val rpm: Int,
        val pwmPercent: Double,
        val isFollowingBiosCurve: Boolean,
    ) : HostFanTelemetry

    @Serializable
    @SerialName("read-only")
    data class ReadOnly(
        override val id: HostFanId,
        val rpm: Int,
    ) : HostFanTelemetry

    @Serializable
    @SerialName("disconnected")
    data class Disconnected(
        override val id: HostFanId,
    ) : HostFanTelemetry
}

@Serializable
@SerialName("host-telemetry")
data class HostTelemetry(
    val sensors: List<HostSensorTelemetry> = emptyList(),
    val fans: List<HostFanTelemetry> = emptyList(),
)

@Serializable
data class HostFanControlRequest(
    val channel: HostFanId,
    val pwmPercent: Double,
) {
    init {
        require(pwmPercent in 0.0..100.0) { "pwmPercent must be between 0.0 and 100.0, was $pwmPercent" }
    }
}
