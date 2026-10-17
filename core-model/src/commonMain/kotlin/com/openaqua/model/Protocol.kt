package com.openaqua.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface EngineResponse

@Serializable
@SerialName("devices")
data class DevicesResponse(
    val devices: List<DetectedHardwareDevice>,
) : EngineResponse

@Serializable
data class DetectedHardwareDevice(
    val id: String,
    val alias: String,
    val type: DeviceType,
    val name: String,
    val serial: String? = null,
    val firmwareVersion: Int? = null,
    val hidrawPath: String,
    val hwmonPath: String? = null,
)

@Serializable
@SerialName("curves")
data class DeviceCurves(
    val deviceId: String,
    val curves: Map<String, List<CurvePoint>>,
) : EngineResponse

@Serializable
data class FlashCurveRequest(
    val deviceId: String,
    val fanId: String,
    val points: List<CurvePoint>? = null,
    val autoCurve: AutoCurveParams? = null,
    val sensorIndex: Int? = null,
)

@Serializable
@SerialName("flash")
data class FlashCurveResponse(
    val status: String,
    val deviceId: String,
    val fanId: String,
    val bytesWritten: Int,
    val verified: Boolean,
    val error: String? = null,
) : EngineResponse

@Serializable
@SerialName("set-sensor-offset")
data class SetSensorOffsetResponse(
    val status: String,
    val deviceId: String,
    val sensorId: String,
    val offsetCelsius: Double,
    val verified: Boolean = true,
    val error: String? = null,
) : EngineResponse

@Serializable
@SerialName("error")
data class ErrorResponse(
    val message: String,
) : EngineResponse
