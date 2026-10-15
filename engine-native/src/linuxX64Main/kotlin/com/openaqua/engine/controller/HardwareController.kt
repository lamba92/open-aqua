package com.openaqua.engine.controller

import com.openaqua.engine.driver.Crc16
import com.openaqua.engine.driver.HidrawDriver
import com.openaqua.engine.scanner.DeviceScanner
import com.openaqua.engine.util.readDouble
import com.openaqua.engine.util.readFile
import com.openaqua.engine.util.readInt
import com.openaqua.model.AutoCurveParams
import com.openaqua.model.CurvePoint
import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.DeviceCurves
import com.openaqua.model.DeviceRegistry
import com.openaqua.model.DeviceTelemetry
import com.openaqua.model.FanTelemetry
import com.openaqua.model.FlashCurveResponse
import com.openaqua.model.SensorTelemetry
import platform.posix.usleep
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

object HardwareController {
    fun getDetectedDevices(): List<DetectedHardwareDevice> = DeviceScanner.scanDevices()

    fun getTelemetry(device: DetectedHardwareDevice): DeviceTelemetry {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: return DeviceTelemetry(
                    deviceId = device.id,
                    deviceType = device.type,
                    name = device.name,
                    sensors = emptyList(),
                    fans = emptyList(),
                )

        val hwmon = device.hwmonPath

        val fans =
            descriptor.fanChannels.map { fan ->
                val idx = fan.hwmonIndex
                val rpm = if (hwmon != null) readInt("$hwmon/fan${idx}_input", 0) else 0
                val powerWatts = if (hwmon != null) readDouble("$hwmon/power${idx}_input", 0.0) / 1_000_000.0 else 0.0
                val voltageVolts = if (hwmon != null) readDouble("$hwmon/in${idx - 1}_input", 0.0) / 1000.0 else 0.0
                val currentAmps = if (hwmon != null) readDouble("$hwmon/curr${idx}_input", 0.0) / 1000.0 else 0.0
                val rawPwm = if (hwmon != null) readInt("$hwmon/pwm$idx", -1) else -1
                val pwmPercent = if (rawPwm >= 0) round((rawPwm * 100.0 / 255.0) * 10.0) / 10.0 else null

                FanTelemetry(
                    id = fan.id,
                    name = fan.name,
                    rpm = rpm,
                    powerWatts = round(powerWatts * 100.0) / 100.0,
                    voltageVolts = round(voltageVolts * 100.0) / 100.0,
                    currentAmps = round(currentAmps * 1000.0) / 1000.0,
                    pwmPercent = pwmPercent,
                )
            }

        val sensors =
            descriptor.tempSensors.map { sensor ->
                val rawMilli = if (hwmon != null) readFile("$hwmon/temp${sensor.hwmonIndex}_input")?.trim()?.toIntOrNull() else null
                val tempCelsius = rawMilli?.let { round((it.toDouble() / 1000.0) * 10.0) / 10.0 }

                SensorTelemetry(
                    id = sensor.id,
                    name = sensor.name,
                    tempCelsius = tempCelsius,
                    isVirtual = sensor.isVirtual,
                )
            }

        val flowRateDlH =
            if (descriptor.hasFlowSensor && hwmon != null) {
                readInt("$hwmon/fan5_input", 0)
            } else {
                null
            }

        return DeviceTelemetry(
            deviceId = device.id,
            deviceType = device.type,
            name = device.name,
            sensors = sensors,
            fans = fans,
            flowRateDlH = flowRateDlH,
        )
    }

    fun getCurves(device: DetectedHardwareDevice): DeviceCurves {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: return DeviceCurves(deviceId = device.id, curves = emptyMap())

        val report =
            HidrawDriver.readFeatureReport(device.hidrawPath, 0x03u, descriptor.ctrlReportLength)
                ?: return DeviceCurves(deviceId = device.id, curves = emptyMap())

        val curvesMap = mutableMapOf<String, List<CurvePoint>>()

        for (fan in descriptor.fanChannels) {
            val base = fan.ctrlOffset
            val points = mutableListOf<CurvePoint>()

            for (i in 0 until 16) {
                val tOffset = base + 0x15 + (i * 2)
                val pOffset = base + 0x35 + (i * 2)

                val tRaw = ((report[tOffset].toInt() shl 8) or report[tOffset + 1].toInt()).toShort()
                val pRaw = ((report[pOffset].toInt() shl 8) or report[pOffset + 1].toInt()).toUShort()

                val temp = round((tRaw.toDouble() / 100.0) * 10.0) / 10.0
                val pwm = round((pRaw.toDouble() / 100.0) * 10.0) / 10.0

                points.add(CurvePoint(temp = temp, pwm = pwm))
            }

            curvesMap[fan.id] = points
        }

        return DeviceCurves(deviceId = device.id, curves = curvesMap)
    }

    fun flashCurve(
        device: DetectedHardwareDevice,
        fanId: String,
        points: List<CurvePoint>,
        sensorIndex: Int? = null,
    ): FlashCurveResponse {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: return FlashCurveResponse(
                    status = "error",
                    deviceId = device.id,
                    fanId = fanId,
                    bytesWritten = 0,
                    verified = false,
                    error = "Unsupported device type: ${device.type}",
                )

        val fan =
            descriptor.fanChannels.firstOrNull { it.id == fanId }
                ?: return FlashCurveResponse(
                    status = "error",
                    deviceId = device.id,
                    fanId = fanId,
                    bytesWritten = 0,
                    verified = false,
                    error = "Unknown fan ID: $fanId",
                )

        if (points.size != 16) {
            return FlashCurveResponse(
                status = "error",
                deviceId = device.id,
                fanId = fanId,
                bytesWritten = 0,
                verified = false,
                error = "Expected exactly 16 curve points, got ${points.size}",
            )
        }

        val report =
            HidrawDriver.readFeatureReport(device.hidrawPath, 0x03u, descriptor.ctrlReportLength)
                ?: return FlashCurveResponse(
                    status = "error",
                    deviceId = device.id,
                    fanId = fanId,
                    bytesWritten = 0,
                    verified = false,
                    error = "Failed to read feature report 0x03 from ${device.hidrawPath}",
                )

        val base = fan.ctrlOffset
        // Set curve mode
        report[base + 0] = 0x02u
        report[base + 1] = 0x02u

        // Optional temperature source sensor
        if (sensorIndex != null) {
            report[base + 3] = ((sensorIndex ushr 8) and 0xFF).toUByte()
            report[base + 4] = (sensorIndex and 0xFF).toUByte()
        }

        // Write 16 points
        for (i in 0 until 16) {
            val pt = points[i]
            val tRaw = (pt.temp * 100.0).roundToInt().coerceIn(-5000, 15000).toShort()
            val pRaw = (pt.pwm * 100.0).roundToInt().coerceIn(0, 10000).toUShort()

            val tOffset = base + 0x15 + (i * 2)
            val pOffset = base + 0x35 + (i * 2)

            report[tOffset] = ((tRaw.toInt() ushr 8) and 0xFF).toUByte()
            report[tOffset + 1] = (tRaw.toInt() and 0xFF).toUByte()

            report[pOffset] = ((pRaw.toInt() ushr 8) and 0xFF).toUByte()
            report[pOffset + 1] = (pRaw.toInt() and 0xFF).toUByte()
        }

        // Recalculate CRC-16-USB
        val crcOffset = 1
        val crcLen = descriptor.ctrlReportLength - 3
        val crc = Crc16.crc16Usb(report, crcOffset, crcLen)
        val crcPos = descriptor.ctrlReportLength - 2
        report[crcPos] = ((crc.toInt() ushr 8) and 0xFF).toUByte()
        report[crcPos + 1] = (crc.toInt() and 0xFF).toUByte()

        val written = HidrawDriver.writeFeatureReport(device.hidrawPath, report)
        if (!written) {
            return FlashCurveResponse(
                status = "error",
                deviceId = device.id,
                fanId = fanId,
                bytesWritten = 0,
                verified = false,
                error = "ioctl HIDIOCSFEATURE failed",
            )
        }

        // Settle device firmware
        usleep(250_000u)

        // Readback verification
        val readback = HidrawDriver.readFeatureReport(device.hidrawPath, 0x03u, descriptor.ctrlReportLength)
        var verified = true
        var mismatchDetail: String? = null

        if (readback != null) {
            for (i in 0 until 16) {
                val pt = points[i]
                val tOffset = base + 0x15 + (i * 2)
                val pOffset = base + 0x35 + (i * 2)

                val tRaw = ((readback[tOffset].toInt() shl 8) or readback[tOffset + 1].toInt()).toShort()
                val pRaw = ((readback[pOffset].toInt() shl 8) or readback[pOffset + 1].toInt()).toUShort()

                val readT = tRaw.toDouble() / 100.0
                val readP = pRaw.toDouble() / 100.0

                if (abs(readT - pt.temp) > 0.2 || abs(readP - pt.pwm) > 0.2) {
                    verified = false
                    mismatchDetail = "Point $i: expected (${pt.temp}°C, ${pt.pwm}%), read ($readT°C, $readP%)"
                    break
                }
            }
        } else {
            verified = false
            mismatchDetail = "Readback feature report returned null"
        }

        return FlashCurveResponse(
            status = if (verified) "ok" else "warning",
            deviceId = device.id,
            fanId = fanId,
            bytesWritten = report.size,
            verified = verified,
            error = mismatchDetail,
        )
    }

    fun flashCurve(
        device: DetectedHardwareDevice,
        fanId: String,
        params: AutoCurveParams,
        sensorIndex: Int? = null,
    ): FlashCurveResponse = flashCurve(device, fanId, params.toPoints(), sensorIndex)
}
