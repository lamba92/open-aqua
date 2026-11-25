package io.github.lamba92.openaqua.app.mock

import io.github.lamba92.openaqua.engine.device.hardware.RawHidDevice
import io.github.lamba92.openaqua.engine.device.hardware.RawHidDeviceTransport
import io.github.lamba92.openaqua.engine.device.protocol.ConfigParser
import io.github.lamba92.openaqua.engine.device.protocol.ConfigWriter
import io.github.lamba92.openaqua.model.device.AutoCurveFanConfig
import io.github.lamba92.openaqua.model.device.CurveFanConfig
import io.github.lamba92.openaqua.model.device.DeviceCapability
import io.github.lamba92.openaqua.model.device.DeviceProtocol
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import io.github.lamba92.openaqua.model.device.DeviceType
import io.github.lamba92.openaqua.model.device.FanChannelDescriptor
import io.github.lamba92.openaqua.model.device.FixedFanConfig
import io.github.lamba92.openaqua.model.device.FlashConfigRequest
import io.github.lamba92.openaqua.model.device.findCapability
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

val MOCK_SUPPORTED_DEVICE_TYPES: List<DeviceType> =
    listOf(
        DeviceType.OCTO,
        DeviceType.QUADRO,
        DeviceType.D5_NEXT,
        DeviceType.FARBWERK_360,
    )

val MOCK_RAW_HID_DEVICES: List<RawHidDevice> =
    MOCK_SUPPORTED_DEVICE_TYPES.mapIndexed { index, type ->
        val desc = DeviceRegistry[type]
        RawHidDevice(
            id = "mock_${type.name.lowercase()}_0",
            vendorId = DeviceRegistry.AQUACOMPUTER_VENDOR_ID,
            productId = desc.productId,
            serial = "0000-0000-${(index + 1).toString().padStart(4, '0')}",
        )
    } +
        RawHidDevice(
            id = "mock_broken_0",
            vendorId = DeviceRegistry.AQUACOMPUTER_VENDOR_ID,
            productId = DeviceRegistry.OCTO.productId,
            serial = "BROKEN-0001",
        )

object MockRawHidDeviceTransport : RawHidDeviceTransport {
    private val reports = mutableMapOf<String, UByteArray>()
    private val virtualSensors = mutableMapOf<String, MutableMap<Int, Double>>()

    override fun scanDevices(): List<RawHidDevice> = MOCK_RAW_HID_DEVICES

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
                val desc =
                    DeviceRegistry.findByProductId(device.productId)
                        ?: error("Unknown product ID: ${device.productId}")
                val initial = UByteArray(reportLength) { if (it == 0) reportId.toUByte() else 0u }
                val flashableProtocol = desc.protocol as? DeviceProtocol.Flashable
                if (flashableProtocol != null) {
                    val fansCap = desc.findCapability<DeviceCapability.Fans>()
                    val defaultFans =
                        fansCap
                            ?.channels
                            ?.filterIsInstance<FanChannelDescriptor.WithLimits>()
                            ?.associate { fan ->
                                fan.channel to FixedFanConfig(channel = fan.channel, powerPercent = 50.0)
                            } ?: emptyMap()

                    val tempCap = desc.findCapability<DeviceCapability.TemperatureSensors.Calibrated>()
                    val defaultOffsets =
                        tempCap?.sensors?.associate { sensor ->
                            sensor.channel to 0.0
                        } ?: emptyMap()

                    val flowPulses =
                        when (desc.findCapability<DeviceCapability.FlowSensor>()) {
                            is DeviceCapability.FlowSensor.Calibrated -> 169
                            else -> null
                        }

                    val request =
                        FlashConfigRequest(
                            fans = defaultFans,
                            sensorOffsets = defaultOffsets,
                            flowPulsesPerLiter = flowPulses,
                        )
                    ConfigWriter.prepareFlashReports(
                        report = initial,
                        descriptor = desc,
                        request = request,
                    )[0]
                } else {
                    initial
                }
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
                        val baseTemp = 32.0 + (sensor.channel * 2.5)
                        val jitter = Random.nextDouble(-0.3, 0.3)
                        val tempVal = ((baseTemp + jitter) * 100).toInt()
                        report[offset] = (tempVal shr 8).toUByte()
                        report[offset + 1] = (tempVal and 0xFF).toUByte()
                    }
                }

                // Populate virtual sensors from memory
                val virtCap = desc.findCapability<DeviceCapability.VirtualSensors>()
                if (virtCap != null) {
                    val virtMap = virtualSensors[device.id] ?: emptyMap()
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
                                report[offset + 1] = 0xFFu // Disconnected sentinel
                            }
                        }
                    }
                }

                // Populate fans
                val fansCap = desc.findCapability<DeviceCapability.Fans>()
                if (fansCap != null) {
                    val currentConfig =
                        try {
                            reports[device.id]?.let { ConfigParser.parseConfig(desc, it) }
                        } catch (_: Throwable) {
                            null
                        }

                    fansCap.channels.forEach { fan ->
                        val offset = fan.statusOffset
                        val layout = fansCap.telemetryLayout
                        if (report.size >= offset + 14) {
                            val fanConfig = currentConfig?.fans?.find { it.channel == fan.channel }
                            val pwmPercent =
                                when (fanConfig) {
                                    is FixedFanConfig -> fanConfig.powerPercent
                                    is CurveFanConfig -> 60.0
                                    is AutoCurveFanConfig -> 55.0
                                    else -> 50.0
                                }
                            val baseRpm = (pwmPercent / 100.0 * 2000.0).toInt()
                            val jitter = if (baseRpm > 0) Random.nextInt(-15, 16) else 0
                            val rpm = (baseRpm + jitter).coerceAtLeast(0)
                            val rawPwm = (pwmPercent * 100).toInt()
                            val rawVoltage = 1200 // 12.00V
                            val rawCurrent = ((rpm / 2000.0) * 300).toInt() // 0.300A
                            val rawPower = ((rpm / 2000.0) * 360).toInt() // 3.60W

                            fun writeU16(
                                relOffset: Int,
                                value: Int,
                            ) {
                                val pos = offset + relOffset
                                if (pos + 1 < report.size) {
                                    report[pos] = (value shr 8).toUByte()
                                    report[pos + 1] = (value and 0xFF).toUByte()
                                }
                            }

                            writeU16(layout.pwmOffset, rawPwm)
                            writeU16(layout.voltageOffset, rawVoltage)
                            writeU16(layout.currentOffset, rawCurrent)
                            writeU16(layout.powerOffset, rawPower)
                            writeU16(layout.rpmOffset, rpm)
                            if (offset + layout.statusOffset < report.size) {
                                report[offset + layout.statusOffset] = 0u
                            }
                        }
                    }
                }

                // Flow sensor
                val flowCap = desc.findCapability<DeviceCapability.FlowSensor>()
                if (flowCap != null && report.size >= flowCap.statusOffset + 2) {
                    val flowVal = 1690 // 169.0 dL/h
                    report[flowCap.statusOffset] = (flowVal shr 8).toUByte()
                    report[flowCap.statusOffset + 1] = (flowVal and 0xFF).toUByte()
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
            val deviceSensors = virtualSensors.getOrPut(device.id) { mutableMapOf() }

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
        }
    }
}
