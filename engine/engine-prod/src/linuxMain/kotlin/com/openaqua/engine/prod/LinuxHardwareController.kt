package com.openaqua.engine.prod

import com.openaqua.engine.hardware.HardwareController
import com.openaqua.engine.util.crc16Usb
import com.openaqua.engine.util.listDirectory
import com.openaqua.engine.util.readTextOrNull
import com.openaqua.model.CurvePoint
import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.DeviceCurves
import com.openaqua.model.DeviceDescriptor
import com.openaqua.model.DeviceRegistry
import com.openaqua.model.DeviceTelemetry
import com.openaqua.model.FanTelemetry
import com.openaqua.model.FlashCurveResponse
import com.openaqua.model.SensorTelemetry
import com.openaqua.model.SetSensorOffsetResponse
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.io.files.Path
import platform.posix.O_NONBLOCK
import platform.posix.O_RDWR
import platform.posix.O_WRONLY
import platform.posix.POLLIN
import platform.posix.close
import platform.posix.ioctl
import platform.posix.open
import platform.posix.poll
import platform.posix.pollfd
import platform.posix.read
import platform.posix.usleep
import platform.posix.write
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

private data class ScannedHardwareNode(
    val id: String,
    val descriptor: DeviceDescriptor,
    val serial: String?,
    val hidrawPath: String,
    val hwmonPath: String?,
)

class LinuxHardwareController : HardwareController {
    private val scannedNodes = mutableMapOf<String, ScannedHardwareNode>()

    private fun getScannedNode(device: DetectedHardwareDevice): ScannedHardwareNode =
        scannedNodes[device.id] ?: run {
            scanDevices()
            scannedNodes[device.id]
                ?: error("Hardware device '${device.id}' (${device.name}) not found")
        }

    override fun scanDevices(): List<DetectedHardwareDevice> {
        val scanned = mutableListOf<ScannedHardwareNode>()
        val hidrawDirs = Path(SYSFS_HIDRAW_DIR).listDirectory()

        for (entry in hidrawDirs.sortedBy { it.name }) {
            val entryName = entry.name
            val ueventPath = hidrawUeventPath(entryName)
            val content = Path(ueventPath).readTextOrNull() ?: continue

            var vendorId: Int? = null
            var productId: Int? = null
            var hidUniq: String? = null

            for (line in content.lines()) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith(UEVENT_HID_ID_PREFIX) -> {
                        val parts = trimmed.substringAfter(UEVENT_HID_ID_PREFIX).split(UEVENT_DELIMITER)
                        if (parts.size >= 3) {
                            vendorId = parts[1].toIntOrNull(16)
                            productId = parts[2].toIntOrNull(16)
                        }
                    }
                    trimmed.startsWith(UEVENT_HID_UNIQ_PREFIX) -> {
                        hidUniq = trimmed.substringAfter(UEVENT_HID_UNIQ_PREFIX).trim().ifEmpty { null }
                    }
                }
            }

            if (vendorId != DeviceRegistry.AQUACOMPUTER_VENDOR_ID || productId == null) {
                continue
            }

            val descriptor = DeviceRegistry.findByProductId(productId) ?: continue
            val id = hidUniq ?: entryName

            val hwmonSubdir = hidrawHwmonSubdir(entryName)

            scanned.add(
                ScannedHardwareNode(
                    id = id,
                    descriptor = descriptor,
                    serial = hidUniq,
                    hidrawPath = hidrawDevPath(entryName),
                    hwmonPath =
                        Path(hwmonSubdir)
                            .listDirectory()
                            .firstOrNull { it.name.startsWith(HWMON_PREFIX) }
                            ?.toString(),
                ),
            )
        }

        scannedNodes.clear()
        for (node in scanned) {
            scannedNodes[node.id] = node
        }

        return scanned
            .sortedWith(
                compareBy<ScannedHardwareNode> { it.descriptor.type.name }
                    .thenBy { it.serial ?: it.hidrawPath },
            ).groupBy { it.descriptor.type }
            .flatMap { (type, nodes) ->
                val typePrefix = type.name.lowercase().replace('_', '-')
                nodes.mapIndexed { index, node ->
                    val fwVersion =
                        node.descriptor.firmwareVersionOffset?.let { offset ->
                            readFirmwareVersion(node.hidrawPath, offset)
                        }
                    DetectedHardwareDevice(
                        id = node.id,
                        alias = "$typePrefix-${index + 1}",
                        type = node.descriptor.type,
                        name = node.descriptor.name,
                        serial = node.serial,
                        firmwareVersion = fwVersion,
                    )
                }
            }
    }

    override fun readTelemetry(device: DetectedHardwareDevice): DeviceTelemetry {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: error("Unsupported device type: ${device.type}")

        val node = getScannedNode(device)
        val hwmon = node.hwmonPath

        val fans =
            descriptor.fanChannels.map { fan ->
                val idx = fan.hwmonIndex
                val rpm =
                    when {
                        hwmon != null ->
                            Path("$hwmon/$HWMON_FAN_PREFIX${idx}$HWMON_INPUT_SUFFIX")
                                .readTextOrNull()
                                ?.trim()
                                ?.toIntOrNull() ?: 0
                        else -> 0
                    }
                val powerWatts =
                    when {
                        hwmon != null ->
                            (
                                Path("$hwmon/$HWMON_POWER_PREFIX${idx}$HWMON_INPUT_SUFFIX")
                                    .readTextOrNull()
                                    ?.trim()
                                    ?.toDoubleOrNull() ?: 0.0
                            ) / 1_000_000.0
                        else -> 0.0
                    }
                val voltageVolts =
                    when {
                        hwmon != null ->
                            (
                                Path("$hwmon/$HWMON_IN_PREFIX${idx - 1}$HWMON_INPUT_SUFFIX")
                                    .readTextOrNull()
                                    ?.trim()
                                    ?.toDoubleOrNull() ?: 0.0
                            ) / 1000.0
                        else -> 0.0
                    }
                val currentAmps =
                    when {
                        hwmon != null ->
                            (
                                Path("$hwmon/$HWMON_CURR_PREFIX${idx}$HWMON_INPUT_SUFFIX")
                                    .readTextOrNull()
                                    ?.trim()
                                    ?.toDoubleOrNull() ?: 0.0
                            ) / 1000.0
                        else -> 0.0
                    }
                val rawPwm =
                    if (hwmon != null) {
                        Path("$hwmon/$HWMON_PWM_PREFIX$idx").readTextOrNull()?.trim()?.toIntOrNull() ?: -1
                    } else {
                        -1
                    }
                val pwmPercent = if (rawPwm >= 0) round((rawPwm * 100.0 / 255.0) * 10.0) / 10.0 else null

                FanTelemetry(
                    id = fan.id,
                    rpm = rpm,
                    powerWatts = round(powerWatts * 100.0) / 100.0,
                    voltageVolts = round(voltageVolts * 100.0) / 100.0,
                    currentAmps = round(currentAmps * 1000.0) / 1000.0,
                    pwmPercent = pwmPercent,
                )
            }

        val sensors =
            descriptor.tempSensors.map { sensor ->
                val rawMilli =
                    if (hwmon != null) {
                        Path("$hwmon/$HWMON_TEMP_PREFIX${sensor.hwmonIndex}$HWMON_INPUT_SUFFIX")
                            .readTextOrNull()
                            ?.trim()
                            ?.toIntOrNull()
                    } else {
                        null
                    }
                val rawOffsetMilli =
                    if (hwmon != null) {
                        Path("$hwmon/$HWMON_TEMP_PREFIX${sensor.hwmonIndex}$HWMON_OFFSET_SUFFIX")
                            .readTextOrNull()
                            ?.trim()
                            ?.toIntOrNull()
                    } else {
                        null
                    }
                val tempCelsius = rawMilli?.let { round((it.toDouble() / 1000.0) * 10.0) / 10.0 }
                val offsetCelsius = rawOffsetMilli?.let { round((it.toDouble() / 1000.0) * 10.0) / 10.0 }

                SensorTelemetry(
                    id = sensor.id,
                    tempCelsius = tempCelsius,
                    offsetCelsius = offsetCelsius,
                    isVirtual = sensor.isVirtual,
                )
            }

        val flowRateDlH =
            if (descriptor.hasFlowSensor && hwmon != null) {
                Path("$hwmon/$HWMON_FLOW_SENSOR_FILE").readTextOrNull()?.trim()?.toIntOrNull()
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

    override fun readCurves(device: DetectedHardwareDevice): DeviceCurves {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: error("Unsupported device type: ${device.type}")

        val node = getScannedNode(device)
        val report = readFeatureReport(node.hidrawPath, 0x03u, descriptor.ctrlReportLength)
        val curvesMap = descriptor.fanChannels.associate { it.id to parseCurvePoints(report, it.ctrlOffset) }

        return DeviceCurves(deviceId = device.id, curves = curvesMap)
    }

    override fun flashCurve(
        device: DetectedHardwareDevice,
        fanId: String,
        points: List<CurvePoint>,
        sensorIndex: Int?,
    ): FlashCurveResponse {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: error("Unsupported device type: ${device.type}")

        val fan =
            descriptor.fanChannels.firstOrNull { it.id == fanId }
                ?: error("Unknown fan ID: $fanId for device ${device.name}")

        require(points.size == 16) {
            "Expected exactly 16 curve points, got ${points.size}"
        }

        val node = getScannedNode(device)
        val report = readFeatureReport(node.hidrawPath, 0x03u, descriptor.ctrlReportLength)
        val base = fan.ctrlOffset

        // Optional temperature source sensor
        if (sensorIndex != null) {
            report[base + 3] = ((sensorIndex ushr 8) and 0xFF).toUByte()
            report[base + 4] = (sensorIndex and 0xFF).toUByte()
            report[base + 0] = 0x02u // Safe to enable curve mode with explicit sensor
        } else {
            val existingSensor = ((report[base + 3].toInt() shl 8) or report[base + 4].toInt()) and 0xFFFF
            if (existingSensor != 0xFFFF) {
                // Channel has a valid sensor assigned, ensure curve mode is active
                report[base + 0] = 0x02u
            }
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
        val crc = crc16Usb(report, crcOffset, crcLen)
        val crcPos = descriptor.ctrlReportLength - 2
        report[crcPos] = ((crc.toInt() ushr 8) and 0xFF).toUByte()
        report[crcPos + 1] = (crc.toInt() and 0xFF).toUByte()

        writeFeatureReport(node.hidrawPath, report)

        // Settle device firmware
        usleep(250_000u)

        // Readback verification
        val readback = readFeatureReport(node.hidrawPath, 0x03u, descriptor.ctrlReportLength)
        val readbackPoints = parseCurvePoints(readback, base)
        for (i in 0 until 16) {
            val expected = points[i]
            val actual = readbackPoints[i]

            check(abs(actual.temp - expected.temp) <= 0.2 && abs(actual.pwm - expected.pwm) <= 0.2) {
                "Verification failed at point $i: expected (${expected.temp}°C, ${expected.pwm}%), read (${actual.temp}°C, ${actual.pwm}%)"
            }
        }

        return FlashCurveResponse(
            status = STATUS_OK,
            deviceId = device.id,
            fanId = fanId,
            bytesWritten = report.size,
            verified = true,
        )
    }

    override fun setSensorOffset(
        device: DetectedHardwareDevice,
        sensorId: String,
        offsetCelsius: Double,
    ): SetSensorOffsetResponse {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: error("Unsupported device type: ${device.type}")

        val sensorIndex = descriptor.tempSensors.indexOfFirst { it.id == sensorId }
        val sensor =
            descriptor.tempSensors.getOrNull(sensorIndex)
                ?: error("Unknown sensor ID: $sensorId for device ${device.name}")

        val node = getScannedNode(device)
        val hwmon = node.hwmonPath
        val sysfsOffsetPath =
            if (hwmon != null) {
                "$hwmon/$HWMON_TEMP_PREFIX${sensor.hwmonIndex}$HWMON_OFFSET_SUFFIX"
            } else {
                null
            }

        val milliCelsius = (offsetCelsius * 1000.0).roundToInt().coerceIn(-15000, 15000)
        var sysfsSuccess = false

        if (sysfsOffsetPath != null) {
            sysfsSuccess = writeSysfsText(sysfsOffsetPath, "$milliCelsius\n")
        }

        if (!sysfsSuccess) {
            val baseOffset =
                descriptor.tempCtrlOffset
                    ?: error("Device ${device.name} does not support direct HID temperature offset configuration")

            val report = readFeatureReport(node.hidrawPath, 0x03u, descriptor.ctrlReportLength)
            val offsetPos = baseOffset + (sensorIndex * 2)
            val centiCelsius = (offsetCelsius * 100.0).roundToInt().coerceIn(-1500, 1500).toShort()

            report[offsetPos] = ((centiCelsius.toInt() ushr 8) and 0xFF).toUByte()
            report[offsetPos + 1] = (centiCelsius.toInt() and 0xFF).toUByte()

            val crcOffset = 1
            val crcLen = descriptor.ctrlReportLength - 3
            val crc = crc16Usb(report, crcOffset, crcLen)
            val crcPos = descriptor.ctrlReportLength - 2
            report[crcPos] = ((crc.toInt() ushr 8) and 0xFF).toUByte()
            report[crcPos + 1] = (crc.toInt() and 0xFF).toUByte()

            writeFeatureReport(node.hidrawPath, report)
            usleep(250_000u)

            val readback = readFeatureReport(node.hidrawPath, 0x03u, descriptor.ctrlReportLength)
            val readbackRaw = ((readback[offsetPos].toInt() shl 8) or readback[offsetPos + 1].toInt()).toShort()
            val readbackOffset = round((readbackRaw.toDouble() / 100.0) * 10.0) / 10.0
            check(abs(readbackOffset - offsetCelsius) <= 0.1) {
                "Verification failed: expected offset $offsetCelsius°C, read $readbackOffset°C"
            }
        }

        return SetSensorOffsetResponse(
            status = STATUS_OK,
            deviceId = device.id,
            sensorId = sensorId,
            offsetCelsius = offsetCelsius,
            verified = true,
        )
    }
}

private fun parseCurvePoints(
    report: UByteArray,
    baseOffset: Int,
): List<CurvePoint> =
    (0 until 16).map { i ->
        val tOffset = baseOffset + 0x15 + (i * 2)
        val pOffset = baseOffset + 0x35 + (i * 2)

        val tRaw = ((report[tOffset].toInt() shl 8) or report[tOffset + 1].toInt()).toShort()
        val pRaw = ((report[pOffset].toInt() shl 8) or report[pOffset + 1].toInt()).toUShort()

        val temp = round((tRaw.toDouble() / 100.0) * 10.0) / 10.0
        val pwm = round((pRaw.toDouble() / 100.0) * 10.0) / 10.0

        CurvePoint(temp = temp, pwm = pwm)
    }

private fun hidiocgfeature(len: Int): ULong = 0xC0000000UL or ((len.toULong() and 0x3FFFUL) shl 16) or 0x4807UL

private fun hidiocsfeature(len: Int): ULong = 0xC0000000UL or ((len.toULong() and 0x3FFFUL) shl 16) or 0x4806UL

private fun readFeatureReport(
    hidrawPath: String,
    reportId: UByte,
    reportPayloadLength: Int,
): UByteArray {
    val totalLen = reportPayloadLength + 1
    val fd = open(hidrawPath, O_RDWR)
    check(fd >= 0) { "Cannot open $hidrawPath for reading" }

    try {
        memScoped {
            val buf = allocArray<UByteVar>(totalLen)
            buf[0] = reportId

            val cmd = hidiocgfeature(totalLen)
            val ret = ioctl(fd, cmd, buf)
            check(ret >= 0) { "ioctl HIDIOCGFEATURE failed on $hidrawPath" }

            val result = UByteArray(totalLen)
            for (i in 0 until totalLen) {
                result[i] = buf[i]
            }
            return result
        }
    } finally {
        close(fd)
    }
}

private fun readFirmwareVersion(
    hidrawPath: String,
    firmwareOffset: Int,
    timeoutMs: Int = DEFAULT_HID_REPORT_TIMEOUT_MS,
): Int? {
    val fd = open(hidrawPath, O_RDWR or O_NONBLOCK)
    if (fd < 0) return null

    try {
        memScoped {
            val pfd = alloc<pollfd>()
            pfd.fd = fd
            pfd.events = POLLIN.toShort()

            val pollRet = poll(pfd.ptr, 1u, timeoutMs)
            if (pollRet <= 0) return null

            val bufSize = 512
            val buf = allocArray<UByteVar>(bufSize)
            val bytesRead = read(fd, buf, bufSize.toULong())
            if (bytesRead < (firmwareOffset + 2).toLong()) return null
            if (buf[0].toInt() != HID_STATUS_REPORT_ID) return null

            val fwHigh = buf[firmwareOffset].toInt() and 0xFF
            val fwLow = buf[firmwareOffset + 1].toInt() and 0xFF
            val version = (fwHigh shl 8) or fwLow

            return if (version in 1..65534) version else null
        }
    } catch (_: Throwable) {
        return null
    } finally {
        close(fd)
    }
}

private fun writeFeatureReport(
    hidrawPath: String,
    reportData: UByteArray,
) {
    val totalLen = reportData.size
    val fd = open(hidrawPath, O_RDWR)
    check(fd >= 0) { "Cannot open $hidrawPath for writing" }

    try {
        memScoped {
            val buf = allocArray<UByteVar>(totalLen)
            for (i in 0 until totalLen) {
                buf[i] = reportData[i]
            }

            usleep(200_000u)

            val cmd = hidiocsfeature(totalLen)
            val ret = ioctl(fd, cmd, buf)
            check(ret >= 0) { "ioctl HIDIOCSFEATURE failed on $hidrawPath" }
        }
    } finally {
        close(fd)
    }
}

private fun writeSysfsText(
    path: String,
    text: String,
): Boolean {
    val fd = open(path, O_WRONLY)
    if (fd < 0) return false
    try {
        memScoped {
            val bytes = text.encodeToByteArray()
            val buf = allocArray<ByteVar>(bytes.size)
            for (i in bytes.indices) {
                buf[i] = bytes[i]
            }
            val written = write(fd, buf, bytes.size.toULong())
            return written >= 0
        }
    } finally {
        close(fd)
    }
}
