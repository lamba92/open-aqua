package com.openaqua.engine.hardware

import com.openaqua.model.AutoCurveParams
import com.openaqua.model.CurvePoint
import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.DeviceCurves
import com.openaqua.model.DeviceTelemetry
import com.openaqua.model.FlashCurveResponse

expect fun scanDevices(): List<DetectedHardwareDevice>

expect fun readTelemetry(device: DetectedHardwareDevice): DeviceTelemetry

expect fun readCurves(device: DetectedHardwareDevice): DeviceCurves

expect fun flashCurve(
    device: DetectedHardwareDevice,
    fanId: String,
    points: List<CurvePoint>,
    sensorIndex: Int? = null,
): FlashCurveResponse

fun flashCurve(
    device: DetectedHardwareDevice,
    fanId: String,
    params: AutoCurveParams,
    sensorIndex: Int? = null,
): FlashCurveResponse = flashCurve(device, fanId, params.toPoints(), sensorIndex)
