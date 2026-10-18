package com.openaqua.engine.prod

import com.openaqua.engine.hardware.HardwareController
import com.openaqua.engine.protocol.ControlReportParser
import com.openaqua.engine.protocol.ControlReportWriter
import com.openaqua.engine.protocol.StatusReportParser
import com.openaqua.model.CurveFanConfig
import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.DeviceCurves
import com.openaqua.model.DeviceDescriptor
import com.openaqua.model.DeviceRegistry
import com.openaqua.model.DeviceTelemetry
import com.openaqua.model.FanChannelConfig
import com.openaqua.model.FixedFanConfig
import com.openaqua.model.FlashFanResponse
import com.openaqua.model.SensorOffset
import com.openaqua.model.SensorOffsetsResponse
import com.openaqua.model.SetSensorOffsetResponse
import com.openaqua.model.TargetTempFanConfig
import com.openaqua.model.TwoPointFanConfig
import com.openaqua.model.UnconfiguredFanConfig
import com.openaqua.model.UnknownFanConfig
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
        val report = readFeatureReport(node.devicePath, 0x03u, descriptor.ctrlReportLength)

        ControlReportWriter.encodeFanChannelConfig(report, fan.ctrlOffset, config)
        ControlReportWriter.finalizeReport(report, descriptor.ctrlReportLength)

        writeFeatureReport(node.devicePath, report)
        writeFeatureReport(node.devicePath, ControlReportWriter.SECONDARY_COMMIT_REPORT)
        Sleep(250u)

        val readback = readFeatureReport(node.devicePath, 0x03u, descriptor.ctrlReportLength)
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

        val node = getScannedNode(device)
        val baseOffset =
            descriptor.tempCtrlOffset
                ?: error("Device ${device.name} does not support direct HID temperature offset configuration")

        val report = readFeatureReport(node.devicePath, 0x03u, descriptor.ctrlReportLength)

        ControlReportWriter.encodeSensorOffset(report, baseOffset, channel, offsetCelsius)
        ControlReportWriter.finalizeReport(report, descriptor.ctrlReportLength)

        writeFeatureReport(node.devicePath, report)
        writeFeatureReport(node.devicePath, ControlReportWriter.SECONDARY_COMMIT_REPORT)
        Sleep(250u)

        val readback = readFeatureReport(node.devicePath, 0x03u, descriptor.ctrlReportLength)
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
