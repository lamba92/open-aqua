package io.github.lamba92.openaqua.engine.device.hardware

import io.github.lamba92.openaqua.engine.device.protocol.ConfigParser
import io.github.lamba92.openaqua.engine.device.protocol.ConfigWriter
import io.github.lamba92.openaqua.engine.device.protocol.StatusReportParser
import io.github.lamba92.openaqua.engine.device.protocol.VirtualSensorsWriter
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.device.DeviceAlias
import io.github.lamba92.openaqua.model.device.DeviceCapability
import io.github.lamba92.openaqua.model.device.DeviceConfig
import io.github.lamba92.openaqua.model.device.DeviceDescriptor
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import io.github.lamba92.openaqua.model.device.DeviceTelemetry
import io.github.lamba92.openaqua.model.device.FlashConfigRequest
import io.github.lamba92.openaqua.model.device.findCapability
import io.github.lamba92.openaqua.model.device.flashableProtocol
import io.github.lamba92.openaqua.model.device.validateRequestMatchesDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

class HardwareController(
    private val transport: RawHidDeviceTransport,
) {
    private fun createDeviceId(raw: RawHidDevice): DeviceId =
        DeviceId.from("${raw.vendorId}-${raw.productId}" + if (raw.serial != null) "-${raw.serial}" else "")

    private fun resolveRawDevice(device: DetectedHardwareDevice): Pair<RawHidDevice, DeviceDescriptor> {
        val rawDevices = transport.scanDevices()
        for (raw in rawDevices) {
            val descriptor = DeviceRegistry.findByProductId(raw.productId) ?: continue
            if (createDeviceId(raw) == device.id) {
                return raw to descriptor
            }
        }
        error("Hardware device '${device.id}' (${device.name}) not found")
    }

    fun scanDevices(): List<DetectedHardwareDevice> {
        val scanned =
            transport
                .scanDevices()
                .mapNotNull { raw ->
                    val descriptor = DeviceRegistry.findByProductId(raw.productId) ?: return@mapNotNull null
                    val id = createDeviceId(raw)
                    Triple(raw, descriptor, id)
                }.distinctBy { it.third }

        return scanned
            .sortedWith(
                compareBy<Triple<RawHidDevice, DeviceDescriptor, DeviceId>> { it.second.type.name }
                    .thenBy { it.first.serial ?: it.first.id },
            ).groupBy { it.second.type }
            .flatMap { (type, nodes) ->
                val typePrefix = type.name.lowercase().replace('_', '-')
                nodes.mapIndexed { index, (raw, descriptor, deviceId) ->
                    val fwVersion = readFirmwareVersion(raw, descriptor)
                    DetectedHardwareDevice(
                        id = deviceId,
                        alias = DeviceAlias.from("$typePrefix-${index + 1}"),
                        type = descriptor.type,
                        name = descriptor.name,
                        serial = raw.serial,
                        firmwareVersion = fwVersion,
                    )
                }
            }
    }

    private fun readFirmwareVersion(
        device: RawHidDevice,
        descriptor: DeviceDescriptor,
    ): Int? =
        try {
            runBlocking {
                withTimeoutOrNull(1200.milliseconds) {
                    transport
                        .streamInputReports(
                            device = device,
                            reportId = descriptor.protocol.statusReportId,
                            reportLength = descriptor.protocol.statusReportLength,
                        ).firstOrNull()
                }
            }?.let { report ->
                val offset = descriptor.protocol.firmwareVersionOffset
                if (report.size >= offset + 2) {
                    val fwHigh = report[offset].toInt() and 0xFF
                    val fwLow = report[offset + 1].toInt() and 0xFF
                    val version = (fwHigh shl 8) or fwLow
                    if (version in 1..65534) version else null
                } else {
                    null
                }
            }
        } catch (_: Throwable) {
            null
        }

    fun streamTelemetry(device: DetectedHardwareDevice): Flow<DeviceTelemetry> {
        val (raw, descriptor) = resolveRawDevice(device)
        return transport
            .streamInputReports(
                device = raw,
                reportId = descriptor.protocol.statusReportId,
                reportLength = descriptor.protocol.statusReportLength,
            ).map { rawReport ->
                StatusReportParser.parse(device, rawReport)
            }
    }

    fun readConfig(device: DetectedHardwareDevice): DeviceConfig {
        val (raw, descriptor) = resolveRawDevice(device)
        val flashProtocol = descriptor.flashableProtocol
        val report =
            transport.readFeatureReport(
                device = raw,
                reportId = flashProtocol.configReportId,
                reportLength = flashProtocol.configReportLength,
            )
        return ConfigParser.parseConfig(descriptor, report)
    }

    fun flashConfig(
        device: DetectedHardwareDevice,
        request: FlashConfigRequest,
    ): DeviceConfig {
        val (raw, descriptor) = resolveRawDevice(device)
        val flashProtocol = descriptor.flashableProtocol
        descriptor.validateRequestMatchesDevice(request)

        val currentReport =
            transport.readFeatureReport(
                device = raw,
                reportId = flashProtocol.configReportId,
                reportLength = flashProtocol.configReportLength,
            )

        val flashReports = ConfigWriter.prepareFlashReports(currentReport, descriptor, request)
        flashReports.forEachIndexed { index, reportToWrite ->
            if (index == 0) {
                transport.writeFeatureReport(raw, reportToWrite)
            } else {
                transport.writeOutputReport(raw, reportToWrite)
            }
        }

        // 250ms flash commit delay for microcontroller EEPROM write
        runBlocking { delay(250.milliseconds) }

        val readback =
            transport.readFeatureReport(
                device = raw,
                reportId = flashProtocol.configReportId,
                reportLength = flashProtocol.configReportLength,
            )
        val readbackConfig = ConfigParser.parseConfig(descriptor, readback)

        ConfigWriter.verifyFlashConfig(expected = request, actual = readbackConfig)

        return readbackConfig
    }

    suspend fun feedVirtualSensors(
        device: DetectedHardwareDevice,
        requests: Flow<Map<Int, Double>>,
    ) {
        val (raw, descriptor) = resolveRawDevice(device)
        descriptor.findCapability<DeviceCapability.VirtualSensors>()
            ?: error("${device.name} does not support virtual sensors")

        val byteReports =
            requests.map { sensorsMap ->
                VirtualSensorsWriter.buildReportFromChannels(descriptor, sensorsMap)
            }
        transport.feedBulkReports(raw, byteReports)
    }
}

fun List<DetectedHardwareDevice>.findTarget(query: String): DetectedHardwareDevice? {
    val trimmed = query.trim()
    firstOrNull {
        it.id.value.equals(trimmed, ignoreCase = true) ||
            it.alias.value.equals(trimmed, ignoreCase = true)
    }?.let { return it }

    val noHyphenQuery = trimmed.replace("-", "").replace("_", "")
    firstOrNull {
        it.alias.value
            .replace("-", "")
            .replace("_", "")
            .equals(noHyphenQuery, ignoreCase = true)
    }?.let { return it }

    val matchesByType =
        filter {
            val typeSlug =
                it.type.name
                    .lowercase()
                    .replace('_', '-')
            typeSlug.equals(trimmed, ignoreCase = true) || it.type.name.equals(trimmed, ignoreCase = true)
        }
    if (matchesByType.size == 1) {
        return matchesByType.first()
    }

    return null
}

fun HardwareController.requireTarget(query: String): DetectedHardwareDevice =
    scanDevices().findTarget(query) ?: error("Device '$query' not found")
