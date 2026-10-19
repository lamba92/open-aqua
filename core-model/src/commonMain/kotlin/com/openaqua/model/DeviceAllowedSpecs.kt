package com.openaqua.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Declares which hardware components on a physical device are allowed to be
 * tested or modified during integration/hardware tests.
 *
 * Any component omitted or set to false is strictly protected and will not be
 * modified or asserted as write-safe.
 */
@Serializable
sealed interface DeviceAllowedSpecs {
    val type: DeviceType
}

@Serializable
@SerialName("octo")
data class OctoAllowedSpecs(
    val fans: Map<Int, Boolean> = emptyMap(),
    val sensorOffsets: Map<Int, Boolean> = emptyMap(),
    val virtualSensors: Map<Int, Boolean> = emptyMap(),
    val rgbpx: Map<Int, Boolean> = emptyMap(),
) : DeviceAllowedSpecs {
    override val type: DeviceType get() = DeviceType.OCTO

    fun isFanAllowed(channel: Int): Boolean = fans[channel] == true

    fun isSensorOffsetAllowed(channel: Int): Boolean = sensorOffsets[channel] == true

    fun isVirtualSensorAllowed(channel: Int): Boolean = virtualSensors[channel] == true

    fun isRgbpxAllowed(port: Int): Boolean = rgbpx[port] == true
}

@Serializable
@SerialName("quadro")
data class QuadroAllowedSpecs(
    val fans: Map<Int, Boolean> = emptyMap(),
    val sensorOffsets: Map<Int, Boolean> = emptyMap(),
    val virtualSensors: Map<Int, Boolean> = emptyMap(),
    val rgbpx: Map<Int, Boolean> = emptyMap(),
) : DeviceAllowedSpecs {
    override val type: DeviceType get() = DeviceType.QUADRO

    fun isFanAllowed(channel: Int): Boolean = fans[channel] == true

    fun isSensorOffsetAllowed(channel: Int): Boolean = sensorOffsets[channel] == true

    fun isVirtualSensorAllowed(channel: Int): Boolean = virtualSensors[channel] == true

    fun isRgbpxAllowed(port: Int): Boolean = rgbpx[port] == true
}

@Serializable
@SerialName("d5next")
data class D5NextAllowedSpecs(
    val pumpAllowed: Boolean = false,
    val fanAllowed: Boolean = false,
    val coolantOffsetAllowed: Boolean = false,
    val virtualSensors: Map<Int, Boolean> = emptyMap(),
    val rgbpx: Map<Int, Boolean> = emptyMap(),
) : DeviceAllowedSpecs {
    override val type: DeviceType get() = DeviceType.D5_NEXT

    fun isVirtualSensorAllowed(channel: Int): Boolean = virtualSensors[channel] == true

    fun isRgbpxAllowed(port: Int): Boolean = rgbpx[port] == true
}
