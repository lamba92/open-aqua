package io.github.lamba92.openaqua.engine.device.prod

import kotlinx.serialization.Serializable

/**
 * Declares which hardware components on a physical device are allowed to be
 * tested or modified during integration/hardware tests.
 *
 * Any component omitted or set to false is strictly protected and will not be
 * modified or asserted as write-safe.
 */
@Serializable
data class DeviceAllowedSpecs(
    val fans: Map<Int, Boolean> = emptyMap(),
    val sensorOffsets: Map<Int, Boolean> = emptyMap(),
    val virtualSensors: Map<Int, Boolean> = emptyMap(),
    val rgbpx: Map<Int, Boolean> = emptyMap(),
    val flowPulsesPerLiterAllowed: Boolean = false,
    val displayAllowed: Boolean = false,
)

fun DeviceAllowedSpecs.isFanAllowed(channel: Int): Boolean = fans[channel] == true

fun DeviceAllowedSpecs.isSensorOffsetAllowed(channel: Int): Boolean = sensorOffsets[channel] == true

fun DeviceAllowedSpecs.isVirtualSensorAllowed(channel: Int): Boolean = virtualSensors[channel] == true

fun DeviceAllowedSpecs.isRgbpxAllowed(port: Int): Boolean = rgbpx[port] == true
