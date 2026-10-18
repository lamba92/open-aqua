package com.openaqua.engine.mock

import com.openaqua.engine.hardware.HardwareController
import com.openaqua.model.CurveFanConfig
import com.openaqua.model.CurvePoint
import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.DeviceCurves
import com.openaqua.model.DeviceRegistry
import com.openaqua.model.DeviceTelemetry
import com.openaqua.model.DeviceType
import com.openaqua.model.FanChannelConfig
import com.openaqua.model.FanTelemetry
import com.openaqua.model.FixedFanConfig
import com.openaqua.model.FlashFanResponse
import com.openaqua.model.SensorOffset
import com.openaqua.model.SensorOffsetsResponse
import com.openaqua.model.SensorTelemetry
import com.openaqua.model.SetSensorOffsetResponse
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

class MockHardwareController : HardwareController {
    override fun scanDevices(): List<DetectedHardwareDevice> =
        listOf(
            DetectedHardwareDevice(
                id = "mock_octo_0",
                alias = "octo-1",
                type = DeviceType.OCTO,
                name = DeviceRegistry.OCTO.name,
                serial = "13235-54067-MOCK",
                firmwareVersion = 1020,
            ),
        )

    override fun streamTelemetry(device: DetectedHardwareDevice): Flow<DeviceTelemetry> =
        flow {
            while (currentCoroutineContext().isActive) {
                emit(
                    DeviceTelemetry(
                        deviceId = device.id,
                        deviceType = device.type,
                        name = device.name,
                        sensors =
                            listOf(
                                SensorTelemetry(0, 32.5),
                                SensorTelemetry(1, 22.8),
                                SensorTelemetry(2, 27.4),
                                SensorTelemetry(3, 43.1),
                            ),
                        fans =
                            listOf(
                                FanTelemetry(
                                    channel = 0,
                                    rpm = 2450,
                                    powerWatts = 8.4,
                                    voltageVolts = 12.0,
                                    currentAmps = 0.7,
                                    pwmPercent = 45.0,
                                ),
                                FanTelemetry(
                                    channel = 1,
                                    rpm = 850,
                                    powerWatts = 1.2,
                                    voltageVolts = 12.0,
                                    currentAmps = 0.1,
                                    pwmPercent = 30.0,
                                ),
                                FanTelemetry(
                                    channel = 2,
                                    rpm = 920,
                                    powerWatts = 1.6,
                                    voltageVolts = 12.0,
                                    currentAmps = 0.13,
                                    pwmPercent = 35.0,
                                ),
                                FanTelemetry(
                                    channel = 3,
                                    rpm = 650,
                                    powerWatts = 0.8,
                                    voltageVolts = 12.0,
                                    currentAmps = 0.07,
                                    pwmPercent = 25.0,
                                ),
                                FanTelemetry(
                                    channel = 4,
                                    rpm = 0,
                                    powerWatts = 0.0,
                                    voltageVolts = 0.0,
                                    currentAmps = 0.0,
                                    pwmPercent = 0.0,
                                ),
                                FanTelemetry(
                                    channel = 5,
                                    rpm = 0,
                                    powerWatts = 0.0,
                                    voltageVolts = 0.0,
                                    currentAmps = 0.0,
                                    pwmPercent = 0.0,
                                ),
                                FanTelemetry(
                                    channel = 6,
                                    rpm = 0,
                                    powerWatts = 0.0,
                                    voltageVolts = 0.0,
                                    currentAmps = 0.0,
                                    pwmPercent = 0.0,
                                ),
                                FanTelemetry(
                                    channel = 7,
                                    rpm = 0,
                                    powerWatts = 0.0,
                                    voltageVolts = 0.0,
                                    currentAmps = 0.0,
                                    pwmPercent = 0.0,
                                ),
                            ),
                        flowRateDlH = null,
                    ),
                )
                delay(1000)
            }
        }

    override fun readCurves(device: DetectedHardwareDevice): DeviceCurves =
        DeviceCurves(
            deviceId = device.id,
            curves =
                listOf(
                    CurveFanConfig(
                        channel = 0,
                        sensorIndex = 0,
                        points =
                            List(16) { i ->
                                CurvePoint(
                                    temp = 20.0 + i * 2.5,
                                    pwm =
                                        30.0 + i * 4.5,
                                )
                            },
                    ),
                    CurveFanConfig(
                        channel = 1,
                        sensorIndex = 0,
                        points =
                            List(16) { i ->
                                CurvePoint(
                                    temp = 20.0 + i * 2.5,
                                    pwm =
                                        20.0 + i * 5.0,
                                )
                            },
                    ),
                    FixedFanConfig(
                        channel = 2,
                        powerPercent = 50.0,
                    ),
                    CurveFanConfig(
                        channel = 3,
                        sensorIndex = 1,
                        points =
                            List(16) { i ->
                                CurvePoint(
                                    temp = 20.0 + i * 2.5,
                                    pwm =
                                        15.0 + i * 5.5,
                                )
                            },
                    ),
                ),
        )

    override fun flashFan(
        device: DetectedHardwareDevice,
        config: FanChannelConfig,
    ): FlashFanResponse =
        FlashFanResponse(
            status = "ok",
            deviceId = device.id,
            channel = config.channel,
            config = config,
            bytesWritten = 1632,
            verified = true,
        )

    override fun readSensorOffsets(device: DetectedHardwareDevice): SensorOffsetsResponse =
        SensorOffsetsResponse(
            deviceId = device.id,
            offsets =
                listOf(
                    SensorOffset(channel = 0, offsetCelsius = 0.0),
                    SensorOffset(channel = 1, offsetCelsius = -0.2),
                    SensorOffset(channel = 2, offsetCelsius = 0.0),
                    SensorOffset(channel = 3, offsetCelsius = 0.5),
                ),
        )

    override fun setSensorOffset(
        device: DetectedHardwareDevice,
        channel: Int,
        offsetCelsius: Double,
    ): SetSensorOffsetResponse =
        SetSensorOffsetResponse(
            status = "ok",
            deviceId = device.id,
            channel = channel,
            offsetCelsius = offsetCelsius,
            verified = true,
        )
}
