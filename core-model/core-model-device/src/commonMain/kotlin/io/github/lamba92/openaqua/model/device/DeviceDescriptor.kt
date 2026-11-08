package io.github.lamba92.openaqua.model.device

import kotlinx.serialization.Serializable

@Serializable
enum class DeviceType {
    OCTO,
    QUADRO,
    D5_NEXT,
    FARBWERK360,
    FARBWERK,
    HIGH_FLOW_NEXT,
    LEAKSHIELD,
    AQUASTREAM_ULTIMATE,
    AQUAERO,
    HIGH_FLOW_USB,
    POWERADJUST3,
    AQUASTREAM_XT,
}

@Serializable
enum class DisplayKind {
    OLED,
    LCD,
}

@Serializable
data class FanChannelDescriptor(
    val channel: Int,
    val ctrlOffset: Int,
    val statusOffset: Int,
    val kind: CoolingChannelKind = CoolingChannelKind.FAN,
    val setupOffset: Int? = null,
)

@Serializable
data class SensorDescriptor(
    val channel: Int,
    val hwmonIndex: Int,
    val statusOffset: Int,
    val kind: SensorChannelKind = SensorChannelKind.PHYSICAL_NTC,
)

@Serializable
sealed interface DeviceCapability {
    @Serializable
    data class Fans(
        val channels: List<FanChannelDescriptor>,
    ) : DeviceCapability

    @Serializable
    data class TemperatureSensors(
        val sensors: List<SensorDescriptor>,
        val ctrlOffset: Int? = null,
    ) : DeviceCapability

    @Serializable
    data class VirtualSensors(
        val count: Int,
        val startOffset: Int? = null,
        val reportSize: Int = 0x43,
    ) : DeviceCapability

    @Serializable
    data class FlowSensor(
        val statusOffset: Int? = null,
        val pulsesCtrlOffset: Int? = null,
        val portCount: Int = 1,
    ) : DeviceCapability

    @Serializable
    data class Display(
        val kind: DisplayKind = DisplayKind.OLED,
        val ctrlOffset: Int? = null,
    ) : DeviceCapability

    @Serializable
    sealed interface Lighting : DeviceCapability {
        @Serializable
        data class Rgbpx(
            val portCount: Int,
            val maxLedsPerPort: Int = 90,
            val ctrlOffset: Int? = null,
        ) : Lighting

        @Serializable
        data class ClassicAnalogRgb(
            val channelCount: Int = 4,
            val resolutionBits: Int = 13,
            val voltage: Int = 12,
        ) : Lighting
    }

    @Serializable
    data class CoolantQuality(
        val conductivityOffset: Int? = null,
        val qualityPercentOffset: Int? = null,
    ) : DeviceCapability

    @Serializable
    data class Pressure(
        val pressureOffset: Int? = null,
        val targetPressureOffset: Int? = null,
        val pumpCyclesOffset: Int? = null,
    ) : DeviceCapability
}

@Serializable
data class DeviceDescriptor(
    val type: DeviceType,
    val name: String,
    val productId: Int,
    val ctrlReportLength: Int,
    val statusReportLength: Int,
    val firmwareVersionOffset: Int? = 0x0D,
    val capabilities: List<DeviceCapability> = emptyList(),
)

inline fun <reified T : DeviceCapability> DeviceDescriptor.findCapability(): T? = capabilities.filterIsInstance<T>().firstOrNull()

inline fun <reified T : DeviceCapability> DeviceDescriptor.hasCapability(): Boolean = findCapability<T>() != null

inline fun <reified T : DeviceCapability> DeviceDescriptor.requireCapability(): T =
    findCapability<T>() ?: error("Device $name ($type) does not have capability ${T::class.simpleName}")

inline fun <reified T : DeviceCapability> DeviceDescriptor.findCapabilities(): List<T> = capabilities.filterIsInstance<T>()

val DeviceDescriptor.fanCapability: DeviceCapability.Fans?
    get() = findCapability()

val DeviceDescriptor.fanChannels: List<FanChannelDescriptor>
    get() = fanCapability?.channels ?: emptyList()

val DeviceDescriptor.tempSensorsCapability: DeviceCapability.TemperatureSensors?
    get() = findCapability()

val DeviceDescriptor.tempSensors: List<SensorDescriptor>
    get() = tempSensorsCapability?.sensors ?: emptyList()

val DeviceDescriptor.tempCtrlOffset: Int?
    get() = tempSensorsCapability?.ctrlOffset

val DeviceDescriptor.virtualSensorsCapability: DeviceCapability.VirtualSensors?
    get() = findCapability()

val DeviceDescriptor.virtualSensorsCount: Int
    get() = virtualSensorsCapability?.count ?: 0

val DeviceDescriptor.virtualSensorsStartOffset: Int?
    get() = virtualSensorsCapability?.startOffset

val DeviceDescriptor.virtualSensorsReportSize: Int
    get() = virtualSensorsCapability?.reportSize ?: 0x43

val DeviceDescriptor.flowSensorCapability: DeviceCapability.FlowSensor?
    get() = findCapability()

val DeviceDescriptor.hasFlowSensor: Boolean
    get() = flowSensorCapability != null

val DeviceDescriptor.flowSensorOffset: Int?
    get() = flowSensorCapability?.statusOffset

val DeviceDescriptor.flowPulsesCtrlOffset: Int?
    get() = flowSensorCapability?.pulsesCtrlOffset

val DeviceDescriptor.displayCapability: DeviceCapability.Display?
    get() = findCapability()

val DeviceDescriptor.hasDisplay: Boolean
    get() = displayCapability != null

val DeviceDescriptor.rgbpxCapability: DeviceCapability.Lighting.Rgbpx?
    get() = findCapability()

val DeviceDescriptor.hasRgbpx: Boolean
    get() = rgbpxCapability != null

val DeviceDescriptor.rgbpxPortCount: Int
    get() = rgbpxCapability?.portCount ?: 0

val DeviceDescriptor.classicAnalogRgbCapability: DeviceCapability.Lighting.ClassicAnalogRgb?
    get() = findCapability()

val DeviceDescriptor.hasClassicAnalogRgb: Boolean
    get() = classicAnalogRgbCapability != null

val DeviceDescriptor.coolantQualityCapability: DeviceCapability.CoolantQuality?
    get() = findCapability()

val DeviceDescriptor.pressureCapability: DeviceCapability.Pressure?
    get() = findCapability()

@Serializable
data class FirmwareRange(
    val minInclusive: Int = 0,
    val maxInclusive: Int = Int.MAX_VALUE,
) {
    operator fun contains(version: Int): Boolean = version in minInclusive..maxInclusive

    companion object {
        val ANY = FirmwareRange(0, Int.MAX_VALUE)

        fun upTo(maxInclusive: Int) = FirmwareRange(0, maxInclusive)

        fun from(minInclusive: Int) = FirmwareRange(minInclusive, Int.MAX_VALUE)
    }
}

@Serializable
data class DeviceLayout(
    val fwRange: FirmwareRange,
    val descriptor: DeviceDescriptor,
)

object DeviceRegistry {
    const val AQUACOMPUTER_VENDOR_ID = 0x0C70

    val OCTO =
        DeviceDescriptor(
            type = DeviceType.OCTO,
            name = "Aquacomputer OCTO",
            productId = 0xF011,
            ctrlReportLength = 0x65F,
            statusReportLength = 0x147,
            firmwareVersionOffset = 0x0D,
            capabilities =
                listOf(
                    DeviceCapability.Fans(
                        channels =
                            (0..7).map {
                                FanChannelDescriptor(
                                    channel = it,
                                    ctrlOffset = 0x5A + (it * 0x55),
                                    statusOffset = 0x7D + (it * 0x0D),
                                    kind = CoolingChannelKind.FAN,
                                    setupOffset = 0x12 + (it * 0x09),
                                )
                            },
                    ),
                    DeviceCapability.TemperatureSensors(
                        sensors =
                            (0..3).map {
                                SensorDescriptor(
                                    channel = it,
                                    hwmonIndex = it + 1,
                                    statusOffset = 0x3D + (it * 2),
                                    kind = SensorChannelKind.PHYSICAL_NTC,
                                )
                            },
                        ctrlOffset = 0x0A,
                    ),
                    DeviceCapability.FlowSensor(
                        statusOffset = 0x7B,
                        pulsesCtrlOffset = 0x06,
                        portCount = 1,
                    ),
                    DeviceCapability.VirtualSensors(
                        count = 16,
                        startOffset = 0x45,
                        reportSize = 0x33,
                    ),
                    DeviceCapability.Lighting.Rgbpx(
                        portCount = 2,
                        maxLedsPerPort = 90,
                    ),
                ),
        )

    val QUADRO =
        DeviceDescriptor(
            type = DeviceType.QUADRO,
            name = "Aquacomputer QUADRO",
            productId = 0xF00D,
            ctrlReportLength = 0x3C1,
            statusReportLength = 0xA5,
            firmwareVersionOffset = 0x0D,
            capabilities =
                listOf(
                    DeviceCapability.Fans(
                        channels =
                            (0..3).map {
                                FanChannelDescriptor(
                                    channel = it,
                                    ctrlOffset = 0x36 + (it * 0x55),
                                    statusOffset = 0x70 + (it * 0x0D),
                                    kind = CoolingChannelKind.FAN,
                                    setupOffset = 0x12 + (it * 0x09),
                                )
                            },
                    ),
                    DeviceCapability.TemperatureSensors(
                        sensors =
                            (0..3).map {
                                SensorDescriptor(
                                    channel = it,
                                    hwmonIndex = it + 1,
                                    statusOffset = 0x34 + (it * 2),
                                    kind = SensorChannelKind.PHYSICAL_NTC,
                                )
                            },
                        ctrlOffset = 0x0A,
                    ),
                    DeviceCapability.FlowSensor(
                        statusOffset = 0x6E,
                        pulsesCtrlOffset = 0x06,
                        portCount = 1,
                    ),
                    DeviceCapability.VirtualSensors(
                        count = 16,
                        startOffset = 0x3C,
                        reportSize = 0x43,
                    ),
                    DeviceCapability.Lighting.Rgbpx(
                        portCount = 1,
                        maxLedsPerPort = 90,
                    ),
                ),
        )

    val D5_NEXT =
        DeviceDescriptor(
            type = DeviceType.D5_NEXT,
            name = "Aquacomputer D5 Next",
            productId = 0xF00E,
            ctrlReportLength = 0x329,
            statusReportLength = 0x76,
            firmwareVersionOffset = 0x0D,
            capabilities =
                listOf(
                    DeviceCapability.Fans(
                        channels =
                            listOf(
                                FanChannelDescriptor(
                                    channel = 0,
                                    ctrlOffset = 0x96,
                                    statusOffset = 0x6C,
                                    kind = CoolingChannelKind.PUMP,
                                    setupOffset = 0x38,
                                ),
                                FanChannelDescriptor(
                                    channel = 1,
                                    ctrlOffset = 0x41,
                                    statusOffset = 0x5F,
                                    kind = CoolingChannelKind.FAN,
                                    setupOffset = 0x2F,
                                ),
                            ),
                    ),
                    DeviceCapability.TemperatureSensors(
                        sensors =
                            listOf(
                                SensorDescriptor(
                                    channel = 0,
                                    hwmonIndex = 1,
                                    statusOffset = 0x57,
                                    kind = SensorChannelKind.INTEGRATED_COOLANT,
                                ),
                            ),
                        ctrlOffset = 0x2D,
                    ),
                    DeviceCapability.FlowSensor(
                        statusOffset = null,
                        pulsesCtrlOffset = null,
                        portCount = 1,
                    ),
                    DeviceCapability.VirtualSensors(
                        count = 8,
                        startOffset = 0x3F,
                        reportSize = 0x43,
                    ),
                    DeviceCapability.Display(
                        kind = DisplayKind.OLED,
                    ),
                    DeviceCapability.Lighting.Rgbpx(
                        portCount = 1,
                        maxLedsPerPort = 90,
                    ),
                ),
        )

    val FARBWERK360 =
        DeviceDescriptor(
            type = DeviceType.FARBWERK360,
            name = "Aquacomputer Farbwerk 360",
            productId = 0xF010,
            ctrlReportLength = 0x682,
            statusReportLength = 0xB6,
            firmwareVersionOffset = 0x0D,
            capabilities =
                listOf(
                    DeviceCapability.TemperatureSensors(
                        sensors =
                            (0..3).map {
                                SensorDescriptor(
                                    channel = it,
                                    hwmonIndex = it + 1,
                                    statusOffset = 0x32 + (it * 2),
                                    kind = SensorChannelKind.PHYSICAL_NTC,
                                )
                            },
                        ctrlOffset = 0x08,
                    ),
                    DeviceCapability.VirtualSensors(
                        count = 16,
                        startOffset = 0x3A,
                        reportSize = 0x33,
                    ),
                    DeviceCapability.Lighting.Rgbpx(
                        portCount = 4,
                        maxLedsPerPort = 90,
                    ),
                ),
        )

    val FARBWERK =
        DeviceDescriptor(
            type = DeviceType.FARBWERK,
            name = "Aquacomputer Farbwerk",
            productId = 0xF00A,
            ctrlReportLength = 0xB6,
            statusReportLength = 0xB6,
            firmwareVersionOffset = 0x0D,
            capabilities =
                listOf(
                    DeviceCapability.TemperatureSensors(
                        sensors =
                            (0..3).map {
                                SensorDescriptor(
                                    channel = it,
                                    hwmonIndex = it + 1,
                                    statusOffset = 0x2F + (it * 2),
                                    kind = SensorChannelKind.PHYSICAL_NTC,
                                )
                            },
                        ctrlOffset = null,
                    ),
                    DeviceCapability.Lighting.ClassicAnalogRgb(
                        channelCount = 4,
                        resolutionBits = 13,
                        voltage = 12,
                    ),
                ),
        )

    val HIGH_FLOW_NEXT =
        DeviceDescriptor(
            type = DeviceType.HIGH_FLOW_NEXT,
            name = "Aquacomputer High Flow Next",
            productId = 0xF012,
            ctrlReportLength = 0x2AA,
            statusReportLength = 0x6F,
            firmwareVersionOffset = 0x0D,
            capabilities =
                listOf(
                    DeviceCapability.TemperatureSensors(
                        sensors =
                            listOf(
                                SensorDescriptor(
                                    channel = 0,
                                    hwmonIndex = 1,
                                    statusOffset = 0x55,
                                    kind = SensorChannelKind.INTEGRATED_COOLANT,
                                ),
                                SensorDescriptor(
                                    channel = 1,
                                    hwmonIndex = 2,
                                    statusOffset = 0x57,
                                    kind = SensorChannelKind.PHYSICAL_NTC,
                                ),
                            ),
                        ctrlOffset = 0x0A,
                    ),
                    DeviceCapability.FlowSensor(
                        statusOffset = 0x51,
                        pulsesCtrlOffset = null,
                        portCount = 1,
                    ),
                    DeviceCapability.CoolantQuality(
                        conductivityOffset = null,
                        qualityPercentOffset = null,
                    ),
                    DeviceCapability.Display(
                        kind = DisplayKind.OLED,
                    ),
                    DeviceCapability.Lighting.Rgbpx(
                        portCount = 1,
                        maxLedsPerPort = 90,
                    ),
                ),
        )

    val LEAKSHIELD =
        DeviceDescriptor(
            type = DeviceType.LEAKSHIELD,
            name = "Aquacomputer Leakshield",
            productId = 0xF014,
            ctrlReportLength = 0x33,
            statusReportLength = 0x140,
            firmwareVersionOffset = 0x0D,
            capabilities =
                listOf(
                    DeviceCapability.TemperatureSensors(
                        sensors =
                            listOf(
                                SensorDescriptor(
                                    channel = 0,
                                    hwmonIndex = 1,
                                    statusOffset = 265,
                                    kind = SensorChannelKind.PHYSICAL_NTC,
                                ),
                                SensorDescriptor(
                                    channel = 1,
                                    hwmonIndex = 2,
                                    statusOffset = 287,
                                    kind = SensorChannelKind.PHYSICAL_NTC,
                                ),
                            ),
                        ctrlOffset = null,
                    ),
                    DeviceCapability.Pressure(
                        pressureOffset = null,
                        targetPressureOffset = null,
                        pumpCyclesOffset = null,
                    ),
                    DeviceCapability.Display(
                        kind = DisplayKind.OLED,
                    ),
                    DeviceCapability.Lighting.Rgbpx(
                        portCount = 1,
                        maxLedsPerPort = 90,
                    ),
                ),
        )

    val AQUASTREAM_ULTIMATE =
        DeviceDescriptor(
            type = DeviceType.AQUASTREAM_ULTIMATE,
            name = "Aquacomputer Aquastream Ultimate",
            productId = 0xF00B,
            ctrlReportLength = 0x329,
            statusReportLength = 0x76,
            firmwareVersionOffset = 0x0D,
            capabilities =
                listOf(
                    DeviceCapability.Fans(
                        channels =
                            listOf(
                                FanChannelDescriptor(
                                    channel = 0,
                                    ctrlOffset = 0x96,
                                    statusOffset = 0x51,
                                    kind = CoolingChannelKind.PUMP,
                                ),
                                FanChannelDescriptor(
                                    channel = 1,
                                    ctrlOffset = 0x41,
                                    statusOffset = 0x41,
                                    kind = CoolingChannelKind.FAN,
                                ),
                            ),
                    ),
                    DeviceCapability.TemperatureSensors(
                        sensors =
                            listOf(
                                SensorDescriptor(
                                    channel = 0,
                                    hwmonIndex = 1,
                                    statusOffset = 0x2D,
                                    kind = SensorChannelKind.INTEGRATED_COOLANT,
                                ),
                                SensorDescriptor(
                                    channel = 1,
                                    hwmonIndex = 2,
                                    statusOffset = 0x2F,
                                    kind = SensorChannelKind.PHYSICAL_NTC,
                                ),
                            ),
                        ctrlOffset = null,
                    ),
                    DeviceCapability.FlowSensor(
                        statusOffset = 0x37,
                        pulsesCtrlOffset = null,
                        portCount = 1,
                    ),
                    DeviceCapability.Display(
                        kind = DisplayKind.OLED,
                    ),
                    DeviceCapability.Lighting.Rgbpx(
                        portCount = 1,
                        maxLedsPerPort = 90,
                    ),
                ),
        )

    val AQUAERO =
        DeviceDescriptor(
            type = DeviceType.AQUAERO,
            name = "Aquacomputer Aquaero",
            productId = 0xF001,
            ctrlReportLength = 0x0A93,
            statusReportLength = 0x195,
            firmwareVersionOffset = 0x0B,
            capabilities =
                listOf(
                    DeviceCapability.Fans(
                        channels =
                            (0..3).map {
                                FanChannelDescriptor(
                                    channel = it,
                                    ctrlOffset = 0x20C + (it * 0x14),
                                    statusOffset = 0x167 + (it * 0x0C),
                                    kind = CoolingChannelKind.FAN,
                                )
                            },
                    ),
                    DeviceCapability.TemperatureSensors(
                        sensors =
                            (0..7).map {
                                SensorDescriptor(
                                    channel = it,
                                    hwmonIndex = it + 1,
                                    statusOffset = 0x65 + (it * 2),
                                    kind = SensorChannelKind.PHYSICAL_NTC,
                                )
                            },
                        ctrlOffset = 0xDB,
                    ),
                    DeviceCapability.FlowSensor(
                        statusOffset = 0xF9,
                        pulsesCtrlOffset = null,
                        portCount = 2,
                    ),
                    DeviceCapability.VirtualSensors(
                        count = 8,
                        startOffset = 0x85,
                        reportSize = 0x43,
                    ),
                    DeviceCapability.Display(
                        kind = DisplayKind.LCD,
                    ),
                ),
        )

    val HIGH_FLOW_USB =
        DeviceDescriptor(
            type = DeviceType.HIGH_FLOW_USB,
            name = "Aquacomputer High Flow USB",
            productId = 0xF003,
            ctrlReportLength = 0,
            statusReportLength = 0x76,
            firmwareVersionOffset = 0x03,
            capabilities =
                listOf(
                    DeviceCapability.TemperatureSensors(
                        sensors =
                            listOf(
                                SensorDescriptor(
                                    channel = 0,
                                    hwmonIndex = 1,
                                    statusOffset = 0x2B,
                                    kind = SensorChannelKind.PHYSICAL_NTC,
                                ),
                                SensorDescriptor(
                                    channel = 1,
                                    hwmonIndex = 2,
                                    statusOffset = 0x2D,
                                    kind = SensorChannelKind.PHYSICAL_NTC,
                                ),
                            ),
                        ctrlOffset = null,
                    ),
                    DeviceCapability.FlowSensor(
                        statusOffset = 0x23,
                        pulsesCtrlOffset = null,
                        portCount = 1,
                    ),
                ),
        )

    val POWERADJUST3 =
        DeviceDescriptor(
            type = DeviceType.POWERADJUST3,
            name = "Aquacomputer Poweradjust 3",
            productId = 0xF0BD,
            ctrlReportLength = 0x32,
            statusReportLength = 0x32,
            firmwareVersionOffset = 0x21,
            capabilities =
                listOf(
                    DeviceCapability.Fans(
                        channels =
                            listOf(
                                FanChannelDescriptor(
                                    channel = 0,
                                    ctrlOffset = 0x08,
                                    statusOffset = 0x0B,
                                    kind = CoolingChannelKind.FAN,
                                ),
                            ),
                    ),
                    DeviceCapability.TemperatureSensors(
                        sensors =
                            listOf(
                                SensorDescriptor(
                                    channel = 0,
                                    hwmonIndex = 1,
                                    statusOffset = 0x01,
                                    kind = SensorChannelKind.PHYSICAL_NTC,
                                ),
                                SensorDescriptor(
                                    channel = 1,
                                    hwmonIndex = 2,
                                    statusOffset = 0x03,
                                    kind = SensorChannelKind.PHYSICAL_NTC,
                                ),
                            ),
                        ctrlOffset = null,
                    ),
                    DeviceCapability.FlowSensor(
                        statusOffset = 0x09,
                        pulsesCtrlOffset = null,
                        portCount = 1,
                    ),
                ),
        )

    val AQUASTREAM_XT =
        DeviceDescriptor(
            type = DeviceType.AQUASTREAM_XT,
            name = "Aquacomputer Aquastream XT",
            productId = 0xF0B6,
            ctrlReportLength = 0x34,
            statusReportLength = 0x42,
            firmwareVersionOffset = 0x32,
            capabilities =
                listOf(
                    DeviceCapability.Fans(
                        channels =
                            listOf(
                                FanChannelDescriptor(
                                    channel = 0,
                                    ctrlOffset = 0x08,
                                    statusOffset = 0x13,
                                    kind = CoolingChannelKind.PUMP,
                                ),
                                FanChannelDescriptor(
                                    channel = 1,
                                    ctrlOffset = 0x1B,
                                    statusOffset = 0x1B,
                                    kind = CoolingChannelKind.FAN,
                                ),
                            ),
                    ),
                    DeviceCapability.TemperatureSensors(
                        sensors =
                            listOf(
                                SensorDescriptor(
                                    channel = 0,
                                    hwmonIndex = 1,
                                    statusOffset = 0x0D,
                                    kind = SensorChannelKind.PHYSICAL_NTC,
                                ),
                                SensorDescriptor(
                                    channel = 1,
                                    hwmonIndex = 2,
                                    statusOffset = 0x0F,
                                    kind = SensorChannelKind.PHYSICAL_NTC,
                                ),
                                SensorDescriptor(
                                    channel = 2,
                                    hwmonIndex = 3,
                                    statusOffset = 0x11,
                                    kind = SensorChannelKind.INTEGRATED_COOLANT,
                                ),
                            ),
                        ctrlOffset = null,
                    ),
                ),
        )

    val ALL =
        listOf(
            OCTO,
            QUADRO,
            D5_NEXT,
            FARBWERK360,
            FARBWERK,
            HIGH_FLOW_NEXT,
            LEAKSHIELD,
            AQUASTREAM_ULTIMATE,
            AQUAERO,
            HIGH_FLOW_USB,
            POWERADJUST3,
            AQUASTREAM_XT,
        )

    fun findByProductId(productId: Int): DeviceDescriptor? = ALL.firstOrNull { it.productId == productId }

    fun findByType(
        type: DeviceType,
        firmwareVersion: Int? = null,
    ): DeviceDescriptor = DeviceLayoutRegistry.resolveDescriptor(type, firmwareVersion)

    operator fun get(type: DeviceType): DeviceDescriptor = findByType(type)
}

object DeviceLayoutRegistry {
    val OCTO_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.OCTO,
            ),
        )

    val QUADRO_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.QUADRO,
            ),
        )

    val D5_NEXT_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.D5_NEXT,
            ),
        )

    val FARBWERK360_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.FARBWERK360,
            ),
        )

    val FARBWERK_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.FARBWERK,
            ),
        )

    val HIGH_FLOW_NEXT_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.HIGH_FLOW_NEXT,
            ),
        )

    val LEAKSHIELD_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.LEAKSHIELD,
            ),
        )

    val AQUASTREAM_ULTIMATE_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.AQUASTREAM_ULTIMATE,
            ),
        )

    val AQUAERO_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.AQUAERO,
            ),
        )

    val HIGH_FLOW_USB_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.HIGH_FLOW_USB,
            ),
        )

    val POWERADJUST3_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.POWERADJUST3,
            ),
        )

    val AQUASTREAM_XT_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.AQUASTREAM_XT,
            ),
        )

    val ALL_LAYOUTS: Map<DeviceType, List<DeviceLayout>> =
        mapOf(
            DeviceType.OCTO to OCTO_LAYOUTS,
            DeviceType.QUADRO to QUADRO_LAYOUTS,
            DeviceType.D5_NEXT to D5_NEXT_LAYOUTS,
            DeviceType.FARBWERK360 to FARBWERK360_LAYOUTS,
            DeviceType.FARBWERK to FARBWERK_LAYOUTS,
            DeviceType.HIGH_FLOW_NEXT to HIGH_FLOW_NEXT_LAYOUTS,
            DeviceType.LEAKSHIELD to LEAKSHIELD_LAYOUTS,
            DeviceType.AQUASTREAM_ULTIMATE to AQUASTREAM_ULTIMATE_LAYOUTS,
            DeviceType.AQUAERO to AQUAERO_LAYOUTS,
            DeviceType.HIGH_FLOW_USB to HIGH_FLOW_USB_LAYOUTS,
            DeviceType.POWERADJUST3 to POWERADJUST3_LAYOUTS,
            DeviceType.AQUASTREAM_XT to AQUASTREAM_XT_LAYOUTS,
        )

    fun getLayoutsForType(type: DeviceType): List<DeviceLayout> = ALL_LAYOUTS[type] ?: emptyList()

    fun resolveDescriptor(
        type: DeviceType,
        fwVersion: Int? = null,
    ): DeviceDescriptor {
        val layouts = getLayoutsForType(type)
        if (layouts.isEmpty()) error("Unsupported device type: $type")
        if (fwVersion == null) {
            return layouts.last().descriptor
        }
        return layouts.firstOrNull { fwVersion in it.fwRange }?.descriptor
            ?: layouts.last().descriptor
    }
}

fun DetectedHardwareDevice.descriptor(): DeviceDescriptor = DeviceLayoutRegistry.resolveDescriptor(type, firmwareVersion)

val DeviceDescriptor.virtualSensorsStartChannel: Int
    get() = tempSensors.size

val DeviceDescriptor.virtualSensorsChannelRange: IntRange
    get() =
        when {
            virtualSensorsCount > 0 ->
                virtualSensorsStartChannel until (virtualSensorsStartChannel + virtualSensorsCount)
            else -> IntRange.EMPTY
        }

fun DeviceDescriptor.validateRequestMatchesDevice(request: FlashConfigRequest) {
    request.fans?.forEach { (ch, config) ->
        val fan = fanChannels.firstOrNull { it.channel == ch }
        requireNotNull(fan) {
            "Invalid fan channel $ch for $name"
        }
        require(fan.setupOffset != null) {
            "Fan channel $ch does not support fan limits on $name"
        }
    }
    request.sensorOffsets?.keys?.forEach { ch ->
        require(tempSensors.any { it.channel == ch }) {
            "Invalid sensor channel $ch for $name"
        }
    }
    request.rgbpx?.keys?.forEach { port ->
        require(hasRgbpx && port in 0 until rgbpxPortCount) {
            "Invalid RGBpx port $port for $name"
        }
    }
    if (request.flowPulsesPerLiter != null) {
        require(hasFlowSensor && flowPulsesCtrlOffset != null) {
            "Device $name does not support flow sensor calibration"
        }
    }
    if (request.display != null) {
        require(hasDisplay) {
            "Device $name does not support display configuration"
        }
    }
}
