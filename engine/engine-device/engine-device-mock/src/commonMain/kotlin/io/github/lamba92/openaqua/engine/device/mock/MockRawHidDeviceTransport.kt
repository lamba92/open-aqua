package io.github.lamba92.openaqua.engine.device.mock

import io.github.lamba92.openaqua.engine.device.hardware.RawHidDevice
import io.github.lamba92.openaqua.engine.device.hardware.RawHidDeviceTransport
import io.github.lamba92.openaqua.engine.device.protocol.ConfigWriter
import io.github.lamba92.openaqua.model.FeatureState
import io.github.lamba92.openaqua.model.device.DeviceCapability
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import io.github.lamba92.openaqua.model.device.FlashConfigRequest
import io.github.lamba92.openaqua.model.device.descriptor
import io.github.lamba92.openaqua.model.device.findCapability
import io.github.lamba92.openaqua.model.device.flashableProtocol
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

class MockRawHidDeviceTransport : RawHidDeviceTransport {
    private val reports = mutableMapOf<String, UByteArray>()

    override fun scanDevices(): List<RawHidDevice> {
        val storage = loadMockStorage()
        return storage.devices.map { dev ->
            RawHidDevice(
                id = dev.id.value,
                vendorId = DeviceRegistry.AQUACOMPUTER_VENDOR_ID,
                productId = dev.descriptor().productId,
                serial = dev.serial,
            )
        }
    }

    private fun checkDeviceAccessible(device: RawHidDevice) {
        if (device.id.contains("broken", ignoreCase = true) ||
            (device.serial?.contains("broken", ignoreCase = true) == true)
        ) {
            error("Cannot open /dev/hidraw for reading")
        }
    }

    override fun readFeatureReport(
        device: RawHidDevice,
        reportId: Int,
        reportLength: Int,
    ): UByteArray {
        checkDeviceAccessible(device)
        return reports
            .getOrPut(device.id) {
                val storage = loadMockStorage()
                val desc =
                    DeviceRegistry.findByProductId(device.productId)
                        ?: error("Unknown product ID: ${device.productId}")
                val config =
                    storage.configs[device.id]
                        ?: (
                            desc.flashableProtocol.let {
                                storage.configs[
                                    desc.type.name
                                        .lowercase()
                                        .replace('_', '-'),
                                ]
                            }
                        )
                        ?: defaultOctoConfig()
                val initial = UByteArray(reportLength) { if (it == 0) reportId.toUByte() else 0u }
                val flowPulses = (config.flowPulsesPerLiter as? FeatureState.Active)?.value
                ConfigWriter.prepareFlashReports(
                    report = initial,
                    descriptor = desc,
                    request =
                        FlashConfigRequest(
                            fans = config.fans.associateBy { it.channel },
                            sensorOffsets = config.sensorOffsets.associate { it.channel to it.offsetCelsius },
                            flowPulsesPerLiter = flowPulses,
                        ),
                )[0]
            }.copyOf()
    }

    override fun writeFeatureReport(
        device: RawHidDevice,
        report: UByteArray,
    ) {
        checkDeviceAccessible(device)
        reports[device.id] = report.copyOf()
    }

    override fun writeOutputReport(
        device: RawHidDevice,
        report: UByteArray,
    ) {
        checkDeviceAccessible(device)
    }

    override fun streamInputReports(
        device: RawHidDevice,
        reportId: Int,
        reportLength: Int,
    ): Flow<UByteArray> =
        flow {
            checkDeviceAccessible(device)
            val desc =
                DeviceRegistry.findByProductId(device.productId)
                    ?: error("Unknown product ID: ${device.productId}")

            while (currentCoroutineContext().isActive) {
                val report = UByteArray(reportLength)
                report[0] = reportId.toUByte()

                // Encode firmware version
                val fwOffset = desc.protocol.firmwareVersionOffset
                if (report.size >= fwOffset + 2) {
                    report[fwOffset] = 0x03u
                    report[fwOffset + 1] = 0xFCu // 1020
                }

                // Populate temps if available
                desc.findCapability<DeviceCapability.TemperatureSensors>()?.sensors?.forEach { sensor ->
                    val offset = sensor.statusOffset
                    if (report.size >= offset + 2) {
                        val tempVal = 3000 // 30.0°C in centi-degrees
                        report[offset] = (tempVal shr 8).toUByte()
                        report[offset + 1] = (tempVal and 0xFF).toUByte()
                    }
                }

                // Populate virtual sensors from storage
                val virtCap = desc.findCapability<DeviceCapability.VirtualSensors>()
                if (virtCap != null) {
                    val virtStorage = loadMockVirtualSensorsStorage()
                    val virtMap = virtStorage[device.id] ?: emptyMap()
                    val tempCap = desc.findCapability<DeviceCapability.TemperatureSensors>()
                    val startCh = tempCap?.sensors?.size ?: 0
                    for (i in 0 until virtCap.count) {
                        val ch = startCh + i
                        val temp = virtMap[ch]
                        val offset = virtCap.statusOffset + i * 2
                        if (report.size >= offset + 2) {
                            if (temp != null) {
                                val tVal = (temp * 100).toInt()
                                report[offset] = (tVal shr 8).toUByte()
                                report[offset + 1] = (tVal and 0xFF).toUByte()
                            } else {
                                report[offset] = 0x7Fu
                                report[offset + 1] = 0xFFu // Disconnected
                            }
                        }
                    }
                }

                // Populate fans
                desc.findCapability<DeviceCapability.Fans>()?.channels?.forEach { fan ->
                    val offset = fan.statusOffset
                    if (report.size >= offset + 6) {
                        val rpm = 1200
                        val pwm = 5000 // 50.00%
                        val voltage = 1200 // 12.00V
                        report[offset] = (rpm shr 8).toUByte()
                        report[offset + 1] = (rpm and 0xFF).toUByte()
                        report[offset + 2] = (pwm shr 8).toUByte()
                        report[offset + 3] = (pwm and 0xFF).toUByte()
                        report[offset + 4] = (voltage shr 8).toUByte()
                        report[offset + 5] = (voltage and 0xFF).toUByte()
                    }
                }

                emit(report)
                delay(200.milliseconds)
            }
        }

    override suspend fun feedBulkReports(
        device: RawHidDevice,
        reports: Flow<UByteArray>,
    ) {
        checkDeviceAccessible(device)
        val desc =
            DeviceRegistry.findByProductId(device.productId)
                ?: error("Unknown product ID: ${device.productId}")
        val virtCap =
            desc.findCapability<DeviceCapability.VirtualSensors>()
                ?: error("${desc.name} does not support virtual sensors")

        val tempCap = desc.findCapability<DeviceCapability.TemperatureSensors>()
        val startChannel = tempCap?.sensors?.size ?: 0

        reports.collect { reportData ->
            val storage = loadMockVirtualSensorsStorage().toMutableMap()
            val deviceSensors = (storage[device.id] ?: emptyMap()).toMutableMap()

            for (slot in 0 until virtCap.count) {
                val offset = 2 + slot * 2
                if (reportData.size >= offset + 2) {
                    val rawVal = (reportData[offset].toInt() shl 8) or reportData[offset + 1].toInt()
                    if (rawVal != 0x7FFF) {
                        val tempC = rawVal / 100.0
                        deviceSensors[startChannel + slot] = tempC
                    }
                }
            }
            storage[device.id] = deviceSensors
            saveMockVirtualSensorsStorage(storage)
        }
    }
}
