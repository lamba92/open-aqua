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
)

@Serializable
data class FanCurve(
    val channel: Int,
    val points: List<CurvePoint>,
)

@Serializable
@SerialName("read-curves")
data class DeviceCurves(
    val deviceId: String,
    val curves: List<FanCurve>,
) : EngineResponse

@Serializable
data class FlashCurveRequest(
    val deviceId: String,
    val channel: Int,
    val points: List<CurvePoint>? = null,
    val autoCurve: AutoCurveParams? = null,
    val sensorIndex: Int? = null,
)

@Serializable
@SerialName("flash-curves")
data class FlashCurveResponse(
    val status: String,
    val deviceId: String,
    val channel: Int,
    val bytesWritten: Int,
    val verified: Boolean,
    val error: String? = null,
) : EngineResponse

@Serializable
data class SensorOffset(
    val channel: Int,
    val offsetCelsius: Double,
)

@Serializable
@SerialName("read-offsets")
data class SensorOffsetsResponse(
    val deviceId: String,
    val offsets: List<SensorOffset>,
) : EngineResponse

@Serializable
@SerialName("flash-offsets")
data class SetSensorOffsetResponse(
    val status: String,
    val deviceId: String,
    val channel: Int,
    val offsetCelsius: Double,
    val verified: Boolean = true,
    val error: String? = null,
) : EngineResponse

@Serializable
@SerialName("error")
data class ErrorResponse(
    val message: String,
) : EngineResponse
