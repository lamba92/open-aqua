package com.openaqua.model

import kotlinx.serialization.Serializable

@Serializable
data class DetectedHardwareDevice(
    val id: String,
    val type: DeviceType,
    val name: String,
    val serial: String? = null,
    val firmwareVersion: Int? = null,
    val hidrawPath: String,
    val hwmonPath: String? = null,
)

@Serializable
data class DeviceCurves(
    val deviceId: String,
    val curves: Map<String, List<CurvePoint>>,
)

@Serializable
data class FlashCurveRequest(
    val deviceId: String,
    val fanId: String,
    val points: List<CurvePoint>? = null,
    val autoCurve: AutoCurveParams? = null,
    val sensorIndex: Int? = null,
)

@Serializable
data class FlashCurveResponse(
    val status: String,
    val deviceId: String,
    val fanId: String,
    val bytesWritten: Int,
    val verified: Boolean,
    val error: String? = null,
)

@Serializable
data class EngineErrorResponse(
    val status: String = "error",
    val error: String,
    val type: String? = null,
)
