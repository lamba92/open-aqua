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
sealed interface DeviceAllowedSpecs

@Serializable
@SerialName("octo")
data class OctoAllowedSpecs(
    val fans: Map<Int, Boolean> = emptyMap(),
    val sensorOffsets: Map<Int, Boolean> = emptyMap(),
    val virtualSensors: Map<Int, Boolean> = emptyMap(),
    val rgbpx: Map<Int, Boolean> = emptyMap(),
) : DeviceAllowedSpecs

@Serializable
@SerialName("quadro")
data class QuadroAllowedSpecs(
    val fans: Map<Int, Boolean> = emptyMap(),
    val sensorOffsets: Map<Int, Boolean> = emptyMap(),
    val virtualSensors: Map<Int, Boolean> = emptyMap(),
    val rgbpx: Map<Int, Boolean> = emptyMap(),
) : DeviceAllowedSpecs

@Serializable
@SerialName("d5next")
data class D5NextAllowedSpecs(
    val pumpAllowed: Boolean = false,
    val fanAllowed: Boolean = false,
    val coolantOffsetAllowed: Boolean = false,
    val virtualSensors: Map<Int, Boolean> = emptyMap(),
    val rgbpx: Map<Int, Boolean> = emptyMap(),
) : DeviceAllowedSpecs

fun OctoAllowedSpecs.isFanAllowed(channel: Int): Boolean = fans[channel] == true

fun OctoAllowedSpecs.isSensorOffsetAllowed(channel: Int): Boolean = sensorOffsets[channel] == true

fun OctoAllowedSpecs.isVirtualSensorAllowed(channel: Int): Boolean = virtualSensors[channel] == true

fun OctoAllowedSpecs.isRgbpxAllowed(port: Int): Boolean = rgbpx[port] == true

fun QuadroAllowedSpecs.isFanAllowed(channel: Int): Boolean = fans[channel] == true

fun QuadroAllowedSpecs.isSensorOffsetAllowed(channel: Int): Boolean = sensorOffsets[channel] == true

fun QuadroAllowedSpecs.isVirtualSensorAllowed(channel: Int): Boolean = virtualSensors[channel] == true

fun QuadroAllowedSpecs.isRgbpxAllowed(port: Int): Boolean = rgbpx[port] == true

fun D5NextAllowedSpecs.isVirtualSensorAllowed(channel: Int): Boolean = virtualSensors[channel] == true

fun D5NextAllowedSpecs.isRgbpxAllowed(port: Int): Boolean = rgbpx[port] == true
