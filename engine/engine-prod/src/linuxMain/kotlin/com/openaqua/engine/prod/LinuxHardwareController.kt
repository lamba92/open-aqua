package com.openaqua.engine.prod

import com.openaqua.engine.hardware.HardwareController
import com.openaqua.engine.protocol.ControlReportParser
import com.openaqua.engine.protocol.ControlReportWriter
import com.openaqua.engine.protocol.StatusReportParser
import com.openaqua.engine.util.listDirectory
import com.openaqua.engine.util.readTextOrNull
import com.openaqua.model.CurveFanConfig
import com.openaqua.model.CurvePoint
import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.DeviceCurves
import com.openaqua.model.DeviceDescriptor
import com.openaqua.model.DeviceRegistry
import com.openaqua.model.DeviceTelemetry
import com.openaqua.model.FanChannelConfig
import com.openaqua.model.FixedFanConfig
import com.openaqua.model.FlashCurveResponse
import com.openaqua.model.FlashFanResponse
import com.openaqua.model.SensorOffset
import com.openaqua.model.SensorOffsetsResponse
import com.openaqua.model.SetSensorOffsetResponse
import com.openaqua.model.TargetTempFanConfig
import com.openaqua.model.TwoPointFanConfig
import com.openaqua.model.UnconfiguredFanConfig
import com.openaqua.model.UnknownFanConfig
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.io.files.Path
import platform.posix.O_NONBLOCK
import platform.posix.O_RDWR
import platform.posix.POLLIN
import platform.posix.close
import platform.posix.ioctl
import platform.posix.open
import platform.posix.poll
import platform.posix.pollfd
import platform.posix.read
import platform.posix.usleep
import kotlin.math.abs

private data class ScannedHardwareNode(
    val id: String,
    val descriptor: DeviceDescriptor,
    val serial: String?,
    val hidrawPath: String,
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

            scanned.add(
                ScannedHardwareNode(
                    id = id,
                    descriptor = descriptor,
                    serial = hidUniq,
                    hidrawPath = hidrawDevPath(entryName),
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

    override fun streamTelemetry(device: DetectedHardwareDevice): Flow<DeviceTelemetry> =
        flow {
            val descriptor =
                DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                    ?: error("Unsupported device type: ${device.type}")

            val node = getScannedNode(device)
            val fd = open(node.hidrawPath, O_RDWR or O_NONBLOCK)
            check(fd >= 0) { "Cannot open ${node.hidrawPath} for reading" }

            try {
                memScoped {
                    val pfd = alloc<pollfd>()
                    pfd.fd = fd
                    pfd.events = POLLIN.toShort()

                    val bufSize = 512
                    val buf = allocArray<UByteVar>(bufSize)

                    while (currentCoroutineContext().isActive) {
                        val pollRet = poll(pfd.ptr, 1u, DEFAULT_HID_REPORT_TIMEOUT_MS)
                        if (pollRet > 0) {
                            val bytesRead = read(fd, buf, bufSize.toULong())
                            if (bytesRead >= descriptor.statusReportLength.toLong() && buf[0].toInt() == HID_STATUS_REPORT_ID) {
                                val report = UByteArray(bytesRead.toInt()) { buf[it] }
                                val telemetry = StatusReportParser.parse(device, report)
                                emit(telemetry)
                            }
                        }
                    }
                }
            } finally {
                close(fd)
            }
        }

    override fun readCurves(device: DetectedHardwareDevice): DeviceCurves {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: error("Unsupported device type: ${device.type}")

        val node = getScannedNode(device)
        val report = readFeatureReport(node.hidrawPath, 0x03u, descriptor.ctrlReportLength)
        val curvesList =
            descriptor.fanChannels.map { fan ->
                ControlReportParser.parseFanChannelConfig(report, fan.ctrlOffset, fan.channel)
            }

        return DeviceCurves(deviceId = device.id, curves = curvesList)
    }

    override fun flashFan(
        device: DetectedHardwareDevice,
        config: FanChannelConfig,
    ): FlashFanResponse {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: error("Unsupported device type: ${device.type}")

        val fan =
            descriptor.fanChannels.firstOrNull { it.channel == config.channel }
                ?: error("Unknown fan channel: ${config.channel} for device ${device.name}")

        val node = getScannedNode(device)
        val report = readFeatureReport(node.hidrawPath, 0x03u, descriptor.ctrlReportLength)

        ControlReportWriter.encodeFanChannelConfig(report, fan.ctrlOffset, config)
        ControlReportWriter.finalizeReport(report, descriptor.ctrlReportLength)

        writeFeatureReport(node.hidrawPath, report)
        writeFeatureReport(node.hidrawPath, ControlReportWriter.SECONDARY_COMMIT_REPORT)
        usleep(250_000u)

        val readback = readFeatureReport(node.hidrawPath, 0x03u, descriptor.ctrlReportLength)
        val readbackConfig = ControlReportParser.parseFanChannelConfig(readback, fan.ctrlOffset, config.channel)

        verifyFanChannelConfig(expected = config, actual = readbackConfig)

        return FlashFanResponse(
            status = STATUS_OK,
            deviceId = device.id,
            channel = config.channel,
            config = readbackConfig,
            bytesWritten = report.size,
            verified = true,
        )
    }

    override fun flashCurve(
        device: DetectedHardwareDevice,
        channel: Int,
        points: List<CurvePoint>,
        sensorIndex: Int?,
    ): FlashCurveResponse {
        val sIndex =
            sensorIndex ?: run {
                val curves = readCurves(device)
                val existing = curves.curves.firstOrNull { it.channel == channel }
                when (existing) {
                    is CurveFanConfig -> existing.sensorIndex
                    is TargetTempFanConfig -> existing.sensorIndex
                    is TwoPointFanConfig -> existing.sensorIndex
                    else -> 0
                }
            }
        val config = CurveFanConfig(channel = channel, sensorIndex = sIndex, points = points)
        val res = flashFan(device, config)
        return FlashCurveResponse(
            status = res.status,
            deviceId = res.deviceId,
            channel = res.channel,
            bytesWritten = res.bytesWritten,
            verified = res.verified,
            error = res.error,
        )
    }

    override fun readSensorOffsets(device: DetectedHardwareDevice): SensorOffsetsResponse {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: error("Unsupported device type: ${device.type}")

        val baseOffset =
            descriptor.tempCtrlOffset
                ?: error("Device ${device.name} does not support temperature offset configuration")

        val node = getScannedNode(device)
        val report = readFeatureReport(node.hidrawPath, 0x03u, descriptor.ctrlReportLength)

        val offsets =
            descriptor.tempSensors.map { sensor ->
                val offset = ControlReportParser.parseSensorOffset(report, baseOffset, sensor.channel)
                SensorOffset(sensor.channel, offset)
            }

        return SensorOffsetsResponse(deviceId = device.id, offsets = offsets)
    }

    override fun setSensorOffset(
        device: DetectedHardwareDevice,
        channel: Int,
        offsetCelsius: Double,
    ): SetSensorOffsetResponse {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: error("Unsupported device type: ${device.type}")

        descriptor.tempSensors.firstOrNull { it.channel == channel }
            ?: error("Unknown sensor channel: $channel for device ${device.name}")

        val baseOffset =
            descriptor.tempCtrlOffset
                ?: error("Device ${device.name} does not support direct HID temperature offset configuration")

        val node = getScannedNode(device)
        val report = readFeatureReport(node.hidrawPath, 0x03u, descriptor.ctrlReportLength)

        ControlReportWriter.encodeSensorOffset(report, baseOffset, channel, offsetCelsius)
        ControlReportWriter.finalizeReport(report, descriptor.ctrlReportLength)

        writeFeatureReport(node.hidrawPath, report)
        writeFeatureReport(node.hidrawPath, ControlReportWriter.SECONDARY_COMMIT_REPORT)
        usleep(250_000u)

        val readback = readFeatureReport(node.hidrawPath, 0x03u, descriptor.ctrlReportLength)
        val readbackOffset = ControlReportParser.parseSensorOffset(readback, baseOffset, channel)
        check(abs(readbackOffset - offsetCelsius) <= 0.1) {
            "Verification failed: expected offset $offsetCelsius°C, read $readbackOffset°C"
        }

        return SetSensorOffsetResponse(
            status = STATUS_OK,
            deviceId = device.id,
            channel = channel,
            offsetCelsius = offsetCelsius,
            verified = true,
        )
    }
}

private fun verifyFanChannelConfig(
    expected: FanChannelConfig,
    actual: FanChannelConfig,
) {
    when (expected) {
        is FixedFanConfig -> {
            check(actual is FixedFanConfig) {
                "Verification failed: expected FixedFanConfig, got ${actual::class.simpleName}"
            }
            check(abs(actual.powerPercent - expected.powerPercent) <= 0.2) {
                "Verification failed: expected power ${expected.powerPercent}%, read ${actual.powerPercent}%"
            }
        }
        is CurveFanConfig -> {
            check(actual is CurveFanConfig) {
                "Verification failed: expected CurveFanConfig, got ${actual::class.simpleName}"
            }
            check(actual.sensorIndex == expected.sensorIndex) {
                "Verification failed: expected sensorIndex ${expected.sensorIndex}, read ${actual.sensorIndex}"
            }
            for (i in 0 until 16) {
                val expPt = expected.points[i]
                val actPt = actual.points[i]
                check(abs(actPt.temp - expPt.temp) <= 0.2 && abs(actPt.pwm - expPt.pwm) <= 0.2) {
                    "Verification failed at point $i: expected (${expPt.temp}°C, ${expPt.pwm}%), read (${actPt.temp}°C, ${actPt.pwm}%)"
                }
            }
        }
        is TargetTempFanConfig -> {
            check(actual is TargetTempFanConfig) {
                "Verification failed: expected TargetTempFanConfig, got ${actual::class.simpleName}"
            }
            check(actual.sensorIndex == expected.sensorIndex) {
                "Verification failed: expected sensorIndex ${expected.sensorIndex}, read ${actual.sensorIndex}"
            }
            check(abs(actual.targetTempCelsius - expected.targetTempCelsius) <= 0.2) {
                "Verification failed: expected targetTemp ${expected.targetTempCelsius}°C, read ${actual.targetTempCelsius}°C"
            }
        }
        is TwoPointFanConfig -> {
            check(actual is TwoPointFanConfig) {
                "Verification failed: expected TwoPointFanConfig, got ${actual::class.simpleName}"
            }
            check(actual.sensorIndex == expected.sensorIndex) {
                "Verification failed: expected sensorIndex ${expected.sensorIndex}, read ${actual.sensorIndex}"
            }
        }
        is UnconfiguredFanConfig, is UnknownFanConfig -> Unit
    }
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
