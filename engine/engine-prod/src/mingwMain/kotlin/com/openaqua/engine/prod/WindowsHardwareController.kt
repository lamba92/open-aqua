package com.openaqua.engine.prod

import com.openaqua.engine.hardware.HardwareController
import com.openaqua.engine.protocol.ConfigParser
import com.openaqua.engine.protocol.ConfigWriter
import com.openaqua.engine.protocol.StatusReportParser
import com.openaqua.engine.protocol.VirtualSensorsWriter
import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.DeviceConfig
import com.openaqua.model.DeviceDescriptor
import com.openaqua.model.DeviceRegistry
import com.openaqua.model.DeviceTelemetry
import com.openaqua.model.DeviceType
import com.openaqua.model.FlashConfigRequest
import com.openaqua.model.FlashD5NextConfig
import com.openaqua.model.FlashOctoConfig
import com.openaqua.model.FlashQuadroConfig
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
import platform.windows.WriteFile
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

private const val HID_STATUS_REPORT_ID = 0x01

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

    override fun readConfig(device: DetectedHardwareDevice): DeviceConfig {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: error("Unsupported device type: ${device.type}")

        val node = getScannedNode(device)
        val report = readFeatureReport(node.devicePath, 0x03u, descriptor.ctrlReportLength)

        return ConfigParser.parseConfig(device, descriptor, report)
    }

    override fun flashConfig(
        device: DetectedHardwareDevice,
        request: FlashConfigRequest,
    ): DeviceConfig {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: error("Unsupported device type: ${device.type}")

        validateRequestMatchesDevice(device.type, request)

        val node = getScannedNode(device)
        val report = readFeatureReport(node.devicePath, 0x03u, descriptor.ctrlReportLength)

        ConfigWriter.applyFlashConfig(report, descriptor, request)
        ConfigWriter.finalizeReport(report, descriptor.ctrlReportLength)

        writeFeatureReport(node.devicePath, report)
        writeFeatureReport(node.devicePath, ConfigWriter.SECONDARY_COMMIT_REPORT)
        Sleep(250u)

        val readback = readFeatureReport(node.devicePath, 0x03u, descriptor.ctrlReportLength)
        val readbackReport = ConfigParser.parseConfig(device, descriptor, readback)

        ConfigWriter.verifyFlashConfig(expected = request, actual = readbackReport)

        return readbackReport
    }

    override fun feedVirtualSensors(
        device: DetectedHardwareDevice,
        sensors: Map<Int, Double>,
    ) {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: error("Unsupported device type: ${device.type}")

        check(descriptor.virtualSensorsCount > 0) {
            "${device.name} does not support virtual sensors"
        }

        val startChannel = if (device.type == DeviceType.D5_NEXT) 1 else 4
        val slotMap = mutableMapOf<Int, Double>()
        sensors.forEach { (channel, temp) ->
            val slot = channel - startChannel
            require(slot in 0 until descriptor.virtualSensorsCount) {
                "Channel $channel is not a valid virtual sensor for ${device.name} (valid channels: $startChannel..${startChannel + descriptor.virtualSensorsCount - 1})"
            }
            slotMap[slot] = temp
        }

        val report = VirtualSensorsWriter.buildReport(descriptor, slotMap)
        val node = getScannedNode(device)

        writeOutputOrFeatureReport(node.devicePath, report)
    }
}

private fun validateRequestMatchesDevice(
    type: DeviceType,
    request: FlashConfigRequest,
) {
    when (type) {
        DeviceType.OCTO ->
            check(request is FlashOctoConfig) {
                "Expected FlashOctoConfig for device type OCTO, got ${request::class.simpleName}"
            }
        DeviceType.QUADRO ->
            check(request is FlashQuadroConfig) {
                "Expected FlashQuadroConfig for device type QUADRO, got ${request::class.simpleName}"
            }
        DeviceType.D5_NEXT ->
            check(request is FlashD5NextConfig) {
                "Expected FlashD5NextConfig for device type D5_NEXT, got ${request::class.simpleName}"
            }
        else -> error("Unsupported device type: $type")
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

private fun writeOutputOrFeatureReport(
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
    check(handle != INVALID_HANDLE_VALUE) { "Cannot open $devicePath for writing virtual sensors report" }

    try {
        memScoped {
            val buf = allocArray<UByteVar>(totalLen)
            for (i in 0 until totalLen) {
                buf[i] = reportData[i]
            }

            val bytesWritten = alloc<UIntVar>()
            val ok = WriteFile(handle, buf, totalLen.toUInt(), bytesWritten.ptr, null)
            if (ok == 0 || bytesWritten.value != totalLen.toUInt()) {
                val ret = HidD_SetFeature(handle, buf, totalLen.toUInt())
                check(ret != 0.toUByte()) { "Failed to write report to $devicePath" }
            }
        }
    } finally {
        CloseHandle(handle)
    }
}
