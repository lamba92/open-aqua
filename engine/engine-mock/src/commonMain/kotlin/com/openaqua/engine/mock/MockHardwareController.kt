package com.openaqua.engine.mock

import com.openaqua.engine.hardware.HardwareController
import com.openaqua.model.CurvePoint
import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.DeviceCurves
import com.openaqua.model.DeviceRegistry
import com.openaqua.model.DeviceTelemetry
import com.openaqua.model.DeviceType
import com.openaqua.model.FanTelemetry
import com.openaqua.model.FlashCurveResponse
import com.openaqua.model.SensorTelemetry
import com.openaqua.model.SetSensorOffsetResponse

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
                hidrawPath = "/dev/hidraw_mock",
                hwmonPath = "/sys/class/hwmon/mock",
            ),
        )

    override fun readTelemetry(device: DetectedHardwareDevice): DeviceTelemetry =
        DeviceTelemetry(
            deviceId = device.id,
            deviceType = device.type,
            name = device.name,
            sensors =
                listOf(
                    SensorTelemetry("temp1", 32.5, 0.0),
                    SensorTelemetry("temp2", 22.8, -0.2),
                    SensorTelemetry("temp3", 27.4, 0.0),
                    SensorTelemetry("temp4", 43.1, 0.5),
                ),
            fans =
                listOf(
                    FanTelemetry(
                        id = "fan1",
                        rpm = 2450,
                        powerWatts = 8.4,
                        voltageVolts = 12.0,
                        currentAmps = 0.7,
                        pwmPercent = 45.0,
                    ),
                    FanTelemetry(
                        id = "fan2",
                        rpm = 850,
                        powerWatts = 1.2,
                        voltageVolts = 12.0,
                        currentAmps = 0.1,
                        pwmPercent = 30.0,
                    ),
                    FanTelemetry(
                        id = "fan3",
                        rpm = 920,
                        powerWatts = 1.6,
                        voltageVolts = 12.0,
                        currentAmps = 0.13,
                        pwmPercent = 35.0,
                    ),
                    FanTelemetry(
                        id = "fan4",
                        rpm = 650,
                        powerWatts = 0.8,
                        voltageVolts = 12.0,
                        currentAmps = 0.07,
                        pwmPercent = 25.0,
                    ),
                    FanTelemetry(
                        id = "fan5",
                        rpm = 0,
                        powerWatts = 0.0,
                        voltageVolts = 0.0,
                        currentAmps = 0.0,
                        pwmPercent = 0.0,
                    ),
                    FanTelemetry(
                        id = "fan6",
                        rpm = 0,
                        powerWatts = 0.0,
                        voltageVolts = 0.0,
                        currentAmps = 0.0,
                        pwmPercent = 0.0,
                    ),
                    FanTelemetry(
                        id = "fan7",
                        rpm = 0,
                        powerWatts = 0.0,
                        voltageVolts = 0.0,
                        currentAmps = 0.0,
                        pwmPercent = 0.0,
                    ),
                    FanTelemetry(
                        id = "fan8",
                        rpm = 0,
                        powerWatts = 0.0,
                        voltageVolts = 0.0,
                        currentAmps = 0.0,
                        pwmPercent = 0.0,
                    ),
                ),
            flowRateDlH = null,
        )

    override fun readCurves(device: DetectedHardwareDevice): DeviceCurves =
        DeviceCurves(
            deviceId = device.id,
            curves =
                mapOf(
                    "fan1" to List(16) { i -> CurvePoint(temp = 20.0 + i * 2.5, pwm = 30.0 + i * 4.5) },
                    "fan2" to List(16) { i -> CurvePoint(temp = 20.0 + i * 2.5, pwm = 20.0 + i * 5.0) },
                    "fan3" to List(16) { i -> CurvePoint(temp = 20.0 + i * 2.5, pwm = 20.0 + i * 5.0) },
                    "fan4" to List(16) { i -> CurvePoint(temp = 20.0 + i * 2.5, pwm = 15.0 + i * 5.5) },
                ),
        )

    override fun flashCurve(
        device: DetectedHardwareDevice,
        fanId: String,
        points: List<CurvePoint>,
        sensorIndex: Int?,
    ): FlashCurveResponse =
        FlashCurveResponse(
            status = "ok",
            deviceId = device.id,
            fanId = fanId,
            bytesWritten = DeviceRegistry.OCTO.ctrlReportLength,
            verified = true,
        )

    override fun setSensorOffset(
        device: DetectedHardwareDevice,
        sensorId: String,
        offsetCelsius: Double,
    ): SetSensorOffsetResponse =
        SetSensorOffsetResponse(
            status = "ok",
            deviceId = device.id,
            sensorId = sensorId,
            offsetCelsius = offsetCelsius,
            verified = true,
        )
}
