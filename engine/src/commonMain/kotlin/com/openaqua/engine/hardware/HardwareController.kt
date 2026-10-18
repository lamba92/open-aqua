package com.openaqua.engine.hardware

import com.openaqua.model.AutoCurveParams
import com.openaqua.model.CurvePoint
import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.DeviceCurves
import com.openaqua.model.DeviceTelemetry
import com.openaqua.model.FanChannelConfig
import com.openaqua.model.FlashCurveResponse
import com.openaqua.model.FlashFanResponse
import com.openaqua.model.SensorOffsetsResponse
import com.openaqua.model.SetSensorOffsetResponse
import kotlinx.coroutines.flow.Flow

interface HardwareController {
    fun scanDevices(): List<DetectedHardwareDevice>

    fun streamTelemetry(device: DetectedHardwareDevice): Flow<DeviceTelemetry>

    fun readCurves(device: DetectedHardwareDevice): DeviceCurves

    fun flashFan(
        device: DetectedHardwareDevice,
        config: FanChannelConfig,
    ): FlashFanResponse

    fun flashCurve(
        device: DetectedHardwareDevice,
        channel: Int,
        points: List<CurvePoint>,
        sensorIndex: Int? = null,
    ): FlashCurveResponse

    fun readSensorOffsets(device: DetectedHardwareDevice): SensorOffsetsResponse

    fun setSensorOffset(
        device: DetectedHardwareDevice,
        channel: Int,
        offsetCelsius: Double,
    ): SetSensorOffsetResponse
}

fun HardwareController.flashCurve(
    device: DetectedHardwareDevice,
    channel: Int,
    params: AutoCurveParams,
    sensorIndex: Int? = null,
): FlashCurveResponse = flashCurve(device, channel, params.toPoints(), sensorIndex)

fun List<DetectedHardwareDevice>.findTarget(query: String): DetectedHardwareDevice? {
    val trimmed = query.trim()
    firstOrNull { it.id.equals(trimmed, ignoreCase = true) || it.alias.equals(trimmed, ignoreCase = true) }?.let { return it }

    val noHyphenQuery = trimmed.replace("-", "").replace("_", "")
    firstOrNull {
        it.alias
            .replace("-", "")
            .replace("_", "")
            .equals(noHyphenQuery, ignoreCase = true)
    }?.let { return it }

    val matchesByType =
        filter {
            val typeSlug =
                it.type.name
                    .lowercase()
                    .replace('_', '-')
            typeSlug.equals(trimmed, ignoreCase = true) || it.type.name.equals(trimmed, ignoreCase = true)
        }
    if (matchesByType.size == 1) {
        return matchesByType.first()
    }

    return null
}
