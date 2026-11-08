package io.github.lamba92.openaqua.model.device

/**
 * Static registry of standard Aquacomputer device descriptors and USB vendor constants.
 */
object DeviceRegistry {
    /** Aquacomputer GmbH USB Vendor ID (`0x0C70`). */
    const val AQUACOMPUTER_VENDOR_ID = 0x0C70

    /** Descriptor for Aquacomputer OCTO: 8 PWM fan channels, 4 temp inputs, calibrated flow, 16 virtual sensors, 2 RGBpx ports. */
    val OCTO =
        DeviceDescriptor(
            type = DeviceType.OCTO,
            name = "Aquacomputer OCTO",
            productId = 0xF011,
            protocol =
                DeviceProtocol.Flashable(
                    statusReportLength = 0x147,
                    configReportLength = 0x65F,
                    firmwareVersionOffset = 0x0D,
                    commitStrategy = CommitStrategy.SecondaryReport(),
                ),
            capabilities =
                listOf(
                    DeviceCapability.Fans(
                        channels =
                            (0..7).map {
                                FanChannelDescriptor.WithLimits(
                                    channel = it,
                                    ctrlOffset = 0x5A + (it * 0x55),
                                    statusOffset = 0x7D + (it * 0x0D),
                                    kind = CoolingChannelKind.FAN,
                                    setupOffset = 0x12 + (it * 0x09),
                                )
                            },
                        capabilities =
                            DeviceCapability.Fans.defaultCapabilities +
                                FanCapability.Follow(
                                    startModeByteInclusive = 0x03,
                                    endModeByteInclusive = 0x0A,
                                ),
                    ),
                    DeviceCapability.TemperatureSensors.Calibrated(
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
                    DeviceCapability.FlowSensor.Calibrated(
                        statusOffset = 0x7B,
                        pulsesCtrlOffset = 0x06,
                        hwSensorSlot = 4,
                        portCount = 1,
                    ),
                    DeviceCapability.VirtualSensors.Standard51(
                        count = 16,
                        statusOffset = 0x45,
                    ),
                    DeviceCapability.Lighting.Rgbpx(
                        portCount = 2,
                        maxLedsPerPort = 90,
                    ),
                ),
        )

    /** Descriptor for Aquacomputer QUADRO: 4 PWM fan channels, 4 temp inputs, calibrated flow, 16 virtual sensors, 1 RGBpx port. */
    val QUADRO =
        DeviceDescriptor(
            type = DeviceType.QUADRO,
            name = "Aquacomputer QUADRO",
            productId = 0xF00D,
            protocol =
                DeviceProtocol.Flashable(
                    statusReportLength = 0xA5,
                    configReportLength = 0x3C1,
                    firmwareVersionOffset = 0x0D,
                    commitStrategy = CommitStrategy.SecondaryReport(),
                ),
            capabilities =
                listOf(
                    DeviceCapability.Fans(
                        channels =
                            (0..3).map {
                                FanChannelDescriptor.WithLimits(
                                    channel = it,
                                    ctrlOffset = 0x36 + (it * 0x55),
                                    statusOffset = 0x70 + (it * 0x0D),
                                    kind = CoolingChannelKind.FAN,
                                    setupOffset = 0x12 + (it * 0x09),
                                )
                            },
                        capabilities =
                            DeviceCapability.Fans.defaultCapabilities +
                                FanCapability.Follow(
                                    startModeByteInclusive = 0x03,
                                    endModeByteInclusive = 0x06,
                                ),
                    ),
                    DeviceCapability.TemperatureSensors.Calibrated(
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
                    DeviceCapability.FlowSensor.Calibrated(
                        statusOffset = 0x6E,
                        pulsesCtrlOffset = 0x06,
                        hwSensorSlot = 4,
                        portCount = 1,
                    ),
                    DeviceCapability.VirtualSensors.Padded67(
                        count = 16,
                        statusOffset = 0x3C,
                    ),
                    DeviceCapability.Lighting.Rgbpx(
                        portCount = 1,
                        maxLedsPerPort = 90,
                    ),
                ),
        )

    /** Descriptor for Aquacomputer D5 NEXT: pump, fan header, coolant temp, 8 virtual sensors, OLED display, 1 RGBpx port. */
    val D5_NEXT =
        DeviceDescriptor(
            type = DeviceType.D5_NEXT,
            name = "Aquacomputer D5 Next",
            productId = 0xF00E,
            protocol =
                DeviceProtocol.Flashable(
                    statusReportLength = 0x76,
                    configReportLength = 0x329,
                    firmwareVersionOffset = 0x0D,
                    commitStrategy = CommitStrategy.SecondaryReport(),
                ),
            capabilities =
                listOf(
                    DeviceCapability.Fans(
                        channels =
                            listOf(
                                FanChannelDescriptor.WithLimits(
                                    channel = 0,
                                    ctrlOffset = 0x96,
                                    statusOffset = 0x6C,
                                    kind = CoolingChannelKind.PUMP,
                                    setupOffset = 0x38,
                                ),
                                FanChannelDescriptor.WithLimits(
                                    channel = 1,
                                    ctrlOffset = 0x41,
                                    statusOffset = 0x5F,
                                    kind = CoolingChannelKind.FAN,
                                    setupOffset = 0x2F,
                                ),
                            ),
                    ),
                    DeviceCapability.TemperatureSensors.Calibrated(
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
                    DeviceCapability.VirtualSensors.Padded67(
                        count = 8,
                        statusOffset = 0x3F,
                    ),
                    DeviceCapability.Display.ReadOnly(
                        kind = DisplayKind.OLED,
                    ),
                    DeviceCapability.Lighting.Rgbpx(
                        portCount = 1,
                        maxLedsPerPort = 90,
                    ),
                ),
        )

    /** Descriptor for Aquacomputer FARBWERK 360: 4 addressable RGBpx ports (up to 360 LEDs) and 4 temp sensor inputs. */
    val FARBWERK360 =
        DeviceDescriptor(
            type = DeviceType.FARBWERK360,
            name = "Aquacomputer Farbwerk 360",
            productId = 0xF010,
            protocol =
                DeviceProtocol.Flashable(
                    statusReportLength = 0xB6,
                    configReportLength = 0x682,
                    firmwareVersionOffset = 0x0D,
                    commitStrategy = CommitStrategy.SecondaryReport(),
                ),
            capabilities =
                listOf(
                    DeviceCapability.TemperatureSensors.Calibrated(
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
                    DeviceCapability.VirtualSensors.Standard51(
                        count = 16,
                        statusOffset = 0x3A,
                    ),
                    DeviceCapability.Lighting.Rgbpx(
                        portCount = 4,
                        maxLedsPerPort = 90,
                    ),
                ),
        )

    /** Descriptor for classic Aquacomputer FARBWERK: 4 analog 12V RGB channels and 4 temp sensor inputs. */
    val FARBWERK =
        DeviceDescriptor(
            type = DeviceType.FARBWERK,
            name = "Aquacomputer Farbwerk",
            productId = 0xF00A,
            protocol =
                DeviceProtocol.Flashable(
                    statusReportLength = 0xB6,
                    configReportLength = 0xB6,
                    firmwareVersionOffset = 0x0D,
                    commitStrategy = CommitStrategy.SecondaryReport(),
                ),
            capabilities =
                listOf(
                    DeviceCapability.TemperatureSensors.ReadOnly(
                        sensors =
                            (0..3).map {
                                SensorDescriptor(
                                    channel = it,
                                    hwmonIndex = it + 1,
                                    statusOffset = 0x2F + (it * 2),
                                    kind = SensorChannelKind.PHYSICAL_NTC,
                                )
                            },
                    ),
                    DeviceCapability.Lighting.ClassicAnalogRgb(
                        channelCount = 4,
                        resolutionBits = 13,
                        voltage = 12,
                    ),
                ),
        )

    /** Descriptor for Aquacomputer HIGH FLOW NEXT: flow sensor, coolant temp, conductivity/quality meter, OLED, RGBpx. */
    val HIGH_FLOW_NEXT =
        DeviceDescriptor(
            type = DeviceType.HIGH_FLOW_NEXT,
            name = "Aquacomputer High Flow Next",
            productId = 0xF012,
            protocol =
                DeviceProtocol.Flashable(
                    statusReportLength = 0x6F,
                    configReportLength = 0x2AA,
                    firmwareVersionOffset = 0x0D,
                    commitStrategy = CommitStrategy.SecondaryReport(),
                ),
            capabilities =
                listOf(
                    DeviceCapability.TemperatureSensors.Calibrated(
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
                    DeviceCapability.FlowSensor.Direct(
                        statusOffset = 0x51,
                        portCount = 1,
                    ),
                    DeviceCapability.CoolantQuality(
                        conductivityOffset = null,
                        qualityPercentOffset = null,
                    ),
                    DeviceCapability.Display.ReadOnly(
                        kind = DisplayKind.OLED,
                    ),
                    DeviceCapability.Lighting.Rgbpx(
                        portCount = 1,
                        maxLedsPerPort = 90,
                    ),
                ),
        )

    /** Descriptor for Aquacomputer LEAKSHIELD: leak prevention system with vacuum pump, pressure sensor, OLED, RGBpx. */
    val LEAKSHIELD =
        DeviceDescriptor(
            type = DeviceType.LEAKSHIELD,
            name = "Aquacomputer Leakshield",
            productId = 0xF014,
            protocol =
                DeviceProtocol.Flashable(
                    statusReportLength = 0x140,
                    configReportLength = 0x33,
                    firmwareVersionOffset = 0x0D,
                    commitStrategy = CommitStrategy.SecondaryReport(),
                ),
            capabilities =
                listOf(
                    DeviceCapability.TemperatureSensors.ReadOnly(
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
                    ),
                    DeviceCapability.Pressure(
                        pressureOffset = null,
                        targetPressureOffset = null,
                        pumpCyclesOffset = null,
                    ),
                    DeviceCapability.Display.ReadOnly(
                        kind = DisplayKind.OLED,
                    ),
                    DeviceCapability.Lighting.Rgbpx(
                        portCount = 1,
                        maxLedsPerPort = 90,
                    ),
                ),
        )

    /** Descriptor for Aquacomputer Aquastream Ultimate: pump, fan output, OLED display, internal/external temp sensors. */
    val AQUASTREAM_ULTIMATE =
        DeviceDescriptor(
            type = DeviceType.AQUASTREAM_ULTIMATE,
            name = "Aquacomputer Aquastream Ultimate",
            productId = 0xF00B,
            protocol =
                DeviceProtocol.Flashable(
                    statusReportLength = 0x76,
                    configReportLength = 0x329,
                    firmwareVersionOffset = 0x0D,
                    commitStrategy = CommitStrategy.SecondaryReport(),
                ),
            capabilities =
                listOf(
                    DeviceCapability.Fans(
                        channels =
                            listOf(
                                FanChannelDescriptor.Basic(
                                    channel = 0,
                                    ctrlOffset = 0x96,
                                    statusOffset = 0x51,
                                    kind = CoolingChannelKind.PUMP,
                                ),
                                FanChannelDescriptor.Basic(
                                    channel = 1,
                                    ctrlOffset = 0x41,
                                    statusOffset = 0x41,
                                    kind = CoolingChannelKind.FAN,
                                ),
                            ),
                    ),
                    DeviceCapability.TemperatureSensors.ReadOnly(
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
                    ),
                    DeviceCapability.FlowSensor.Direct(
                        statusOffset = 0x37,
                        portCount = 1,
                    ),
                    DeviceCapability.Display.ReadOnly(
                        kind = DisplayKind.OLED,
                    ),
                    DeviceCapability.Lighting.Rgbpx(
                        portCount = 1,
                        maxLedsPerPort = 90,
                    ),
                ),
        )

    /** Descriptor for Aquacomputer Aquaero: 4 fan channels, up to 8 temp sensors, 2 flow sensors, LCD/OLED display. */
    val AQUAERO =
        DeviceDescriptor(
            type = DeviceType.AQUAERO,
            name = "Aquacomputer Aquaero",
            productId = 0xF001,
            protocol =
                DeviceProtocol.Flashable(
                    statusReportLength = 0x195,
                    configReportLength = 0x0A93,
                    configReportId = 0x0B,
                    firmwareVersionOffset = 0x0B,
                    commitStrategy = CommitStrategy.Direct,
                ),
            capabilities =
                listOf(
                    DeviceCapability.Fans(
                        channels =
                            (0..3).map {
                                FanChannelDescriptor.Basic(
                                    channel = it,
                                    ctrlOffset = 0x20C + (it * 0x14),
                                    statusOffset = 0x167 + (it * 0x0C),
                                    kind = CoolingChannelKind.FAN,
                                )
                            },
                        capabilities =
                            listOf(
                                FanCapability.Fixed(0x00),
                                FanCapability.TargetTemp(0x01),
                                FanCapability.Curve(0x02),
                                FanCapability.TwoPoint(0x48),
                            ),
                    ),
                    DeviceCapability.TemperatureSensors.Calibrated(
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
                    DeviceCapability.FlowSensor.Direct(
                        statusOffset = 0xF9,
                        portCount = 2,
                    ),
                    DeviceCapability.VirtualSensors.Padded67(
                        count = 8,
                        statusOffset = 0x85,
                    ),
                    DeviceCapability.Display.ReadOnly(
                        kind = DisplayKind.LCD,
                    ),
                ),
        )

    /** Descriptor for Aquacomputer High Flow USB: dedicated flow and temperature sensor. */
    val HIGH_FLOW_USB =
        DeviceDescriptor(
            type = DeviceType.HIGH_FLOW_USB,
            name = "Aquacomputer High Flow USB",
            productId = 0xF003,
            protocol =
                DeviceProtocol.ReadOnly(
                    statusReportLength = 0x76,
                    firmwareVersionOffset = 0x03,
                ),
            capabilities =
                listOf(
                    DeviceCapability.TemperatureSensors.ReadOnly(
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
                    ),
                    DeviceCapability.FlowSensor.Direct(
                        statusOffset = 0x23,
                        portCount = 1,
                    ),
                ),
        )

    /** Descriptor for Aquacomputer Poweradjust 3: single-channel fan/pump power controller with temp sensor. */
    val POWERADJUST3 =
        DeviceDescriptor(
            type = DeviceType.POWERADJUST3,
            name = "Aquacomputer Poweradjust 3",
            productId = 0xF0BD,
            protocol =
                DeviceProtocol.Flashable(
                    statusReportLength = 0x32,
                    configReportLength = 0x32,
                    firmwareVersionOffset = 0x21,
                    commitStrategy = CommitStrategy.Direct,
                ),
            capabilities =
                listOf(
                    DeviceCapability.Fans(
                        channels =
                            listOf(
                                FanChannelDescriptor.Basic(
                                    channel = 0,
                                    ctrlOffset = 0x08,
                                    statusOffset = 0x0B,
                                    kind = CoolingChannelKind.FAN,
                                ),
                            ),
                    ),
                    DeviceCapability.TemperatureSensors.ReadOnly(
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
                    ),
                    DeviceCapability.FlowSensor.Direct(
                        statusOffset = 0x09,
                        portCount = 1,
                    ),
                ),
        )

    /** Descriptor for classic Aquacomputer Aquastream XT: pump with fan header and temperature sensing. */
    val AQUASTREAM_XT =
        DeviceDescriptor(
            type = DeviceType.AQUASTREAM_XT,
            name = "Aquacomputer Aquastream XT",
            productId = 0xF0B6,
            protocol =
                DeviceProtocol.Flashable(
                    statusReportLength = 0x42,
                    configReportLength = 0x34,
                    firmwareVersionOffset = 0x32,
                    commitStrategy = CommitStrategy.Direct,
                ),
            capabilities =
                listOf(
                    DeviceCapability.Fans(
                        channels =
                            listOf(
                                FanChannelDescriptor.Basic(
                                    channel = 0,
                                    ctrlOffset = 0x08,
                                    statusOffset = 0x13,
                                    kind = CoolingChannelKind.PUMP,
                                ),
                                FanChannelDescriptor.Basic(
                                    channel = 1,
                                    ctrlOffset = 0x1B,
                                    statusOffset = 0x1B,
                                    kind = CoolingChannelKind.FAN,
                                ),
                            ),
                    ),
                    DeviceCapability.TemperatureSensors.ReadOnly(
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
                    ),
                ),
        )

    /** List of all registered default device descriptors. */
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

    /** Finds a device descriptor matching the given USB product ID (PID), or `null` if unrecognized. */
    fun findByProductId(productId: Int): DeviceDescriptor? = ALL.firstOrNull { it.productId == productId }

    /** Resolves a device descriptor for the given [DeviceType] and optional [firmwareVersion]. */
    fun findByType(
        type: DeviceType,
        firmwareVersion: Int? = null,
    ): DeviceDescriptor = DeviceLayoutRegistry.resolveDescriptor(type, firmwareVersion)

    /** Convenience operator to look up a descriptor by [DeviceType]. */
    operator fun get(type: DeviceType): DeviceDescriptor = findByType(type)
}
