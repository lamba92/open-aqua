package com.openaqua.engine.prod

import com.openaqua.engine.hardware.HardwareController
import com.openaqua.engine.protocol.StatusReportParser
import com.openaqua.engine.util.crc16Usb
import com.openaqua.model.CurvePoint
import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.DeviceCurves
import com.openaqua.model.DeviceDescriptor
import com.openaqua.model.DeviceRegistry
import com.openaqua.model.DeviceTelemetry
import com.openaqua.model.FanCurve
import com.openaqua.model.FlashCurveResponse
import com.openaqua.model.SensorOffset
import com.openaqua.model.SensorOffsetsResponse
import com.openaqua.model.SetSensorOffsetResponse
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKStringFromUtf16
import kotlinx.cinterop.value
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import platform.windows.CloseHandle
import platform.windows.CreateFileW
import platform.windows.FILE_SHARE_READ
import platform.windows.FILE_SHARE_WRITE
import platform.windows.GENERIC_READ
import platform.windows.GENERIC_WRITE
import platform.windows.INVALID_HANDLE_VALUE
import platform.windows.OPEN_EXISTING
import platform.windows.ReadFile
import platform.windows.Sleep
import winhid.HIDD_ATTRIBUTES
import winhid.HidD_GetAttributes
import winhid.HidD_GetFeature
import winhid.HidD_GetSerialNumberString
import winhid.HidD_SetFeature
import winhid.SP_DEVICE_INTERFACE_DATA
import winhid.SP_DEVICE_INTERFACE_DETAIL_DATA_W
import winhid.SetupDiDestroyDeviceInfoList
import winhid.SetupDiGetDeviceInterfaceDetailW
import winhid.enum_device_interface
import winhid.get_hid_dev_info
import winhid.get_hid_guid
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

private const val HID_STATUS_REPORT_ID = 0x01
private const val STATUS_OK = "ok"

private data class ScannedWindowsNode(
    val id: String,
    val descriptor: DeviceDescriptor,
    val serial: String?,
    val devicePath: String,
)

class WindowsHardwareController : HardwareController {
    private val scannedNodes = mutableMapOf<String, ScannedWindowsNode>()

    private fun getScannedNode(device: DetectedHardwareDevice): ScannedWindowsNode =
        scannedNodes[device.id] ?: run {
            scanDevices()
            scannedNodes[device.id]
                ?: error("Hardware device '${device.id}' (${device.name}) not found")
        }

    override fun scanDevices(): List<DetectedHardwareDevice> {
        val scanned = mutableListOf<ScannedWindowsNode>()

        memScoped {
            val hidGuid = allocArray<UByteVar>(16)
            get_hid_guid(hidGuid)

            val devInfo = get_hid_dev_info(hidGuid)

            if (devInfo != null && devInfo != INVALID_HANDLE_VALUE) {
                try {
                    val ifData = alloc<SP_DEVICE_INTERFACE_DATA>()
                    ifData.cbSize = sizeOf<SP_DEVICE_INTERFACE_DATA>().toUInt()

                    var index = 0u
                    while (enum_device_interface(devInfo, hidGuid, index, ifData.ptr) != 0) {
                        index++

                        val requiredSize = alloc<UIntVar>()
                        SetupDiGetDeviceInterfaceDetailW(devInfo, ifData.ptr, null, 0u, requiredSize.ptr, null)

                        if (requiredSize.value > 0u) {
                            val detailBuffer = allocArray<UByteVar>(requiredSize.value.toInt())
                            val detailData = detailBuffer.reinterpret<SP_DEVICE_INTERFACE_DETAIL_DATA_W>()
                            detailData.pointed.cbSize = if (sizeOf<CPointerVar<*>>() == 8L) 8u else 6u

                            if (SetupDiGetDeviceInterfaceDetailW(
                                    devInfo,
                                    ifData.ptr,
                                    detailData,
                                    requiredSize.value,
                                    null,
                                    null,
                                ) != 0
                            ) {
                                val devicePath = detailData.pointed.DevicePath.toKStringFromUtf16()
                                inspectDevicePath(devicePath)?.let { scanned.add(it) }
                            }
                        }
                    }
                } finally {
                    SetupDiDestroyDeviceInfoList(devInfo)
                }
            }
        }

        scannedNodes.clear()
        for (node in scanned) {
            scannedNodes[node.id] = node
        }

        return scanned
            .sortedWith(
                compareBy<ScannedWindowsNode> { it.descriptor.type.name }
                    .thenBy { it.serial ?: it.devicePath },
            ).groupBy { it.descriptor.type }
            .flatMap { (type, nodes) ->
                val typePrefix = type.name.lowercase().replace('_', '-')
                nodes.mapIndexed { index, node ->
                    val fwVersion =
                        node.descriptor.firmwareVersionOffset?.let { offset ->
                            readFirmwareVersion(node.devicePath, offset)
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

    private fun inspectDevicePath(devicePath: String): ScannedWindowsNode? {
        val handle =
            CreateFileW(
                devicePath,
                0u,
                (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
                null,
                OPEN_EXISTING.toUInt(),
                0u,
                null,
            )
        if (handle == INVALID_HANDLE_VALUE) return null

        try {
            memScoped {
                val attrs = alloc<HIDD_ATTRIBUTES>()
                attrs.Size = sizeOf<HIDD_ATTRIBUTES>().toUInt()

                if (HidD_GetAttributes(handle, attrs.ptr) == 0.toUByte()) return null
                if (attrs.VendorID.toInt() != DeviceRegistry.AQUACOMPUTER_VENDOR_ID) return null

                val descriptor = DeviceRegistry.findByProductId(attrs.ProductID.toInt()) ?: return null

                val serialBuf = allocArray<UShortVar>(128)
                val hasSerial = HidD_GetSerialNumberString(handle, serialBuf, 256u) != 0.toUByte()
                val serial = if (hasSerial) serialBuf.toKStringFromUtf16().ifEmpty { null } else null

                val id = serial ?: devicePath
                return ScannedWindowsNode(
                    id = id,
                    descriptor = descriptor,
                    serial = serial,
                    devicePath = devicePath,
                )
            }
        } finally {
            CloseHandle(handle)
        }
    }

    override fun streamTelemetry(device: DetectedHardwareDevice): Flow<DeviceTelemetry> =
        flow {
            val descriptor =
                DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                    ?: error("Unsupported device type: ${device.type}")

            val node = getScannedNode(device)
            val handle =
                CreateFileW(
                    node.devicePath,
                    GENERIC_READ,
                    (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
                    null,
                    OPEN_EXISTING.toUInt(),
                    0u,
                    null,
                )
            check(handle != INVALID_HANDLE_VALUE) { "Cannot open ${node.devicePath} for reading telemetry" }

            try {
                memScoped {
                    val bufSize = 512
                    val buf = allocArray<UByteVar>(bufSize)
                    val bytesRead = alloc<UIntVar>()

                    while (currentCoroutineContext().isActive) {
                        val ok = ReadFile(handle, buf, bufSize.toUInt(), bytesRead.ptr, null)
                        if (ok != 0 && bytesRead.value >= descriptor.statusReportLength.toUInt()) {
                            if (buf[0].toInt() == HID_STATUS_REPORT_ID) {
                                val report = UByteArray(bytesRead.value.toInt()) { buf[it] }
                                val telemetry = StatusReportParser.parse(device, report)
                                emit(telemetry)
                            }
                        }
                    }
                }
            } finally {
                CloseHandle(handle)
            }
        }

    override fun readCurves(device: DetectedHardwareDevice): DeviceCurves {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: error("Unsupported device type: ${device.type}")

        val node = getScannedNode(device)
        val report = readFeatureReport(node.devicePath, 0x03u, descriptor.ctrlReportLength)
        val curvesList =
            descriptor.fanChannels.map { fan ->
                FanCurve(channel = fan.channel, points = parseCurvePoints(report, fan.ctrlOffset))
            }

        return DeviceCurves(deviceId = device.id, curves = curvesList)
    }

    override fun flashCurve(
        device: DetectedHardwareDevice,
        channel: Int,
        points: List<CurvePoint>,
        sensorIndex: Int?,
    ): FlashCurveResponse {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: error("Unsupported device type: ${device.type}")

        val fan =
            descriptor.fanChannels.firstOrNull { it.channel == channel }
                ?: error("Unknown fan channel: $channel for device ${device.name}")

        require(points.size == 16) {
            "Expected exactly 16 curve points, got ${points.size}"
        }

        val node = getScannedNode(device)
        val report = readFeatureReport(node.devicePath, 0x03u, descriptor.ctrlReportLength)
        val base = fan.ctrlOffset

        if (sensorIndex != null) {
            report[base + 3] = ((sensorIndex ushr 8) and 0xFF).toUByte()
            report[base + 4] = (sensorIndex and 0xFF).toUByte()
            report[base + 0] = 0x02u
        } else {
            val existingSensor = ((report[base + 3].toInt() shl 8) or report[base + 4].toInt()) and 0xFFFF
            if (existingSensor != 0xFFFF) {
                report[base + 0] = 0x02u
            }
        }

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

        val crcOffset = 1
        val crcLen = descriptor.ctrlReportLength - 3
        val crc = crc16Usb(report, crcOffset, crcLen)
        val crcPos = descriptor.ctrlReportLength - 2
        report[crcPos] = ((crc.toInt() ushr 8) and 0xFF).toUByte()
        report[crcPos + 1] = (crc.toInt() and 0xFF).toUByte()

        writeFeatureReport(node.devicePath, report)
        Sleep(250u)

        val readback = readFeatureReport(node.devicePath, 0x03u, descriptor.ctrlReportLength)
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
            channel = channel,
            bytesWritten = report.size,
            verified = true,
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
        val report = readFeatureReport(node.devicePath, 0x03u, descriptor.ctrlReportLength)

        val offsets =
            descriptor.tempSensors.map { sensor ->
                val offsetPos = baseOffset + (sensor.channel * 2)
                val raw = ((report[offsetPos].toInt() shl 8) or report[offsetPos + 1].toInt()).toShort()
                val offset = round((raw.toDouble() / 100.0) * 10.0) / 10.0
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

        val node = getScannedNode(device)
        val baseOffset =
            descriptor.tempCtrlOffset
                ?: error("Device ${device.name} does not support direct HID temperature offset configuration")

        val report = readFeatureReport(node.devicePath, 0x03u, descriptor.ctrlReportLength)
        val offsetPos = baseOffset + (channel * 2)
        val centiCelsius = (offsetCelsius * 100.0).roundToInt().coerceIn(-1500, 1500).toShort()

        report[offsetPos] = ((centiCelsius.toInt() ushr 8) and 0xFF).toUByte()
        report[offsetPos + 1] = (centiCelsius.toInt() and 0xFF).toUByte()

        val crcOffset = 1
        val crcLen = descriptor.ctrlReportLength - 3
        val crc = crc16Usb(report, crcOffset, crcLen)
        val crcPos = descriptor.ctrlReportLength - 2
        report[crcPos] = ((crc.toInt() ushr 8) and 0xFF).toUByte()
        report[crcPos + 1] = (crc.toInt() and 0xFF).toUByte()

        writeFeatureReport(node.devicePath, report)
        Sleep(250u)

        val readback = readFeatureReport(node.devicePath, 0x03u, descriptor.ctrlReportLength)
        val readbackRaw = ((readback[offsetPos].toInt() shl 8) or readback[offsetPos + 1].toInt()).toShort()
        val readbackOffset = round((readbackRaw.toDouble() / 100.0) * 10.0) / 10.0
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

private fun readFeatureReport(
    devicePath: String,
    reportId: UByte,
    reportPayloadLength: Int,
): UByteArray {
    val totalLen = reportPayloadLength + 1
    val handle =
        CreateFileW(
            devicePath,
            (GENERIC_READ or GENERIC_WRITE.toUInt()),
            (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
            null,
            OPEN_EXISTING.toUInt(),
            0u,
            null,
        )
    check(handle != INVALID_HANDLE_VALUE) { "Cannot open $devicePath for reading feature report" }

    try {
        memScoped {
            val buf = allocArray<UByteVar>(totalLen)
            buf[0] = reportId

            val ret = HidD_GetFeature(handle, buf, totalLen.toUInt())
            check(ret != 0.toUByte()) { "HidD_GetFeature failed on $devicePath" }

            val result = UByteArray(totalLen)
            for (i in 0 until totalLen) {
                result[i] = buf[i]
            }
            return result
        }
    } finally {
        CloseHandle(handle)
    }
}

private fun readFirmwareVersion(
    devicePath: String,
    firmwareOffset: Int,
): Int? {
    val handle =
        CreateFileW(
            devicePath,
            GENERIC_READ,
            (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
            null,
            OPEN_EXISTING.toUInt(),
            0u,
            null,
        )
    if (handle == INVALID_HANDLE_VALUE) return null

    try {
        memScoped {
            val bufSize = 512
            val buf = allocArray<UByteVar>(bufSize)
            val bytesRead = alloc<UIntVar>()

            val ok = ReadFile(handle, buf, bufSize.toUInt(), bytesRead.ptr, null)
            if (ok == 0) return null
            if (bytesRead.value < (firmwareOffset + 2).toUInt()) return null
            if (buf[0].toInt() != HID_STATUS_REPORT_ID) return null

            val fwHigh = buf[firmwareOffset].toInt() and 0xFF
            val fwLow = buf[firmwareOffset + 1].toInt() and 0xFF
            val version = (fwHigh shl 8) or fwLow

            return if (version in 1..65534) version else null
        }
    } catch (_: Throwable) {
        return null
    } finally {
        CloseHandle(handle)
    }
}

private fun writeFeatureReport(
    devicePath: String,
    reportData: UByteArray,
) {
    val totalLen = reportData.size
    val handle =
        CreateFileW(
            devicePath,
            (GENERIC_READ or GENERIC_WRITE.toUInt()),
            (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
            null,
            OPEN_EXISTING.toUInt(),
            0u,
            null,
        )
    check(handle != INVALID_HANDLE_VALUE) { "Cannot open $devicePath for writing feature report" }

    try {
        memScoped {
            val buf = allocArray<UByteVar>(totalLen)
            for (i in 0 until totalLen) {
                buf[i] = reportData[i]
            }

            Sleep(200u)

            val ret = HidD_SetFeature(handle, buf, totalLen.toUInt())
            check(ret != 0.toUByte()) { "HidD_SetFeature failed on $devicePath" }
        }
    } finally {
        CloseHandle(handle)
    }
}
