package com.openaqua.engine.hardware

import com.openaqua.model.AutoCurveParams
import com.openaqua.model.CurvePoint
import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.DeviceCurves
import com.openaqua.model.DeviceTelemetry
import com.openaqua.model.FlashCurveResponse

interface HardwareController {
    fun scanDevices(): List<DetectedHardwareDevice>

    fun readTelemetry(device: DetectedHardwareDevice): DeviceTelemetry

    fun readCurves(device: DetectedHardwareDevice): DeviceCurves

    fun flashCurve(
        device: DetectedHardwareDevice,
        fanId: String,
        points: List<CurvePoint>,
        sensorIndex: Int? = null,
    ): FlashCurveResponse
}

fun HardwareController.flashCurve(
    device: DetectedHardwareDevice,
    fanId: String,
    params: AutoCurveParams,
    sensorIndex: Int? = null,
): FlashCurveResponse = flashCurve(device, fanId, params.toPoints(), sensorIndex)

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
