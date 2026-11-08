package io.github.lamba92.openaqua.model.device

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Kind of display panel integrated into or connected to the device.
 */
@Serializable
enum class DisplayKind {
    /** OLED display panel (e.g., Aquacomputer D5 NEXT, Aquaero 6 PRO/XT). */
    OLED,

    /** Classic graphic or character LCD panel (e.g., Aquaero 5). */
    LCD,
}

/**
 * Hardware channel descriptor for a cooling output (fan header or pump controller).
 *
 * Provides the byte offsets within HID control and status reports for reading telemetry
 * and configuring control modes.
 *
 * @property channel Zero-based logical channel index on the device (e.g., 0..7 for OCTO, 0..3 for QUADRO).
 * @property ctrlOffset Byte offset in the HID control report where channel control parameters (mode, duty/RPM, curve) start.
 * @property statusOffset Byte offset in the HID status report where live telemetry (RPM, power, voltage, current) starts.
 * @property kind Physical role of the cooling channel (fan header or integrated pump).
 */
@Serializable
sealed interface FanChannelDescriptor {
    val channel: Int
    val ctrlOffset: Int
    val statusOffset: Int
    val kind: CoolingChannelKind

    /**
     * Descriptor for fan channels that support hardware minimum/maximum power limits and start-boost setup.
     *
     * @property channel Zero-based channel index.
     * @property ctrlOffset Byte offset in the HID control report for mode and operational settings.
     * @property statusOffset Byte offset in the HID status report for telemetry.
     * @property setupOffset Byte offset in the HID control report where the limits/setup block is encoded.
     * @property kind Channel role, defaults to [CoolingChannelKind.FAN].
     */
    @Serializable
    data class WithLimits(
        override val channel: Int,
        override val ctrlOffset: Int,
        override val statusOffset: Int,
        val setupOffset: Int,
        override val kind: CoolingChannelKind = CoolingChannelKind.FAN,
    ) : FanChannelDescriptor

    /**
     * Descriptor for fan channels without configurable hardware power limit setup blocks.
     *
     * @property channel Zero-based channel index.
     * @property ctrlOffset Byte offset in the HID control report for mode and operational settings.
     * @property statusOffset Byte offset in the HID status report for telemetry.
     * @property kind Channel role, defaults to [CoolingChannelKind.FAN].
     */
    @Serializable
    data class Basic(
        override val channel: Int,
        override val ctrlOffset: Int,
        override val statusOffset: Int,
        override val kind: CoolingChannelKind = CoolingChannelKind.FAN,
    ) : FanChannelDescriptor
}

/**
 * Layout specification for decoding cooling channel telemetry fields from HID status reports.
 *
 * @property pwmOffset Relative byte offset of 16-bit BE PWM centi-percent (0..10000).
 * @property voltageOffset Relative byte offset of 16-bit BE voltage in centi-volts.
 * @property currentOffset Relative byte offset of 16-bit BE current in milli-amps.
 * @property powerOffset Relative byte offset of 16-bit BE power in centi-watts.
 * @property rpmOffset Relative byte offset of 16-bit BE fan rotational speed in RPM.
 * @property statusOffset Relative byte offset of channel status flags.
 * @property statusDisconnectedMask Bitmask within status byte indicating channel stall or disconnection (e.g. `0x0C`).
 */
@Serializable
data class FanTelemetryLayout(
    val pwmOffset: Int = 0,
    val voltageOffset: Int = 2,
    val currentOffset: Int = 4,
    val powerOffset: Int = 6,
    val rpmOffset: Int = 8,
    val statusOffset: Int = 12,
    val statusDisconnectedMask: Int = 0x0C,
) {
    companion object {
        /** Default telemetry layout present on modern Aquacomputer devices. */
        val Standard = FanTelemetryLayout()
    }
}

/**
 * Layout specification for encoding and decoding fan power limits in the HID control report.
 *
 * @property flagsOffset Relative byte offset of limits flags byte. Defaults to `0`.
 * @property holdMinPowerMask Bitmask for hold min power flag. Defaults to `0x01`.
 * @property startBoostMask Bitmask for start boost flag. Defaults to `0x02`.
 * @property minPowerOffset Relative byte offset of 16-bit BE minimum power centi-percent. Defaults to `1`.
 * @property maxPowerOffset Relative byte offset of 16-bit BE maximum power centi-percent. Defaults to `3`.
 * @property fallbackPowerOffset Relative byte offset of 16-bit BE fallback power centi-percent. Defaults to `5`.
 */
@Serializable
data class FanLimitsLayout(
    val flagsOffset: Int = 0,
    val holdMinPowerMask: Int = 0x01,
    val startBoostMask: Int = 0x02,
    val minPowerOffset: Int = 1,
    val maxPowerOffset: Int = 3,
    val fallbackPowerOffset: Int = 5,
) {
    companion object {
        /** Default limits layout present on modern Aquacomputer devices. */
        val Standard = FanLimitsLayout()
    }
}

/**
 * Describes an individual temperature sensor port or integrated thermal probe on the device.
 *
 * @property channel Zero-based channel index of the sensor on this device.
 * @property hwmonIndex 1-based index typically assigned in Linux hwmon sysfs nodes.
 * @property statusOffset Byte offset in the HID status report containing the raw 16-bit signed integer temperature in centi-degrees Celsius.
 * @property kind Nature of the temperature probe (physical NTC probe, virtual software feed, or integrated coolant sensor).
 */
@Serializable
data class SensorDescriptor(
    val channel: Int,
    val hwmonIndex: Int,
    val statusOffset: Int,
    val kind: SensorChannelKind = SensorChannelKind.PHYSICAL_NTC,
)

/**
 * Hardware control mode capability supported by fan channels on a given device.
 */
@Serializable
sealed interface FanCapability {
    /** Relative byte offset where the operating mode byte is encoded inside the channel control block. */
    val modeOffset: Int

    /**
     * Fixed-power (manual PWM or voltage) mode.
     *
     * @property modeByte Raw mode identifier byte encoded into the HID control report. Defaults to `0x00`.
     * @property pwmOffset Relative byte offset of 16-bit BE duty/power value. Defaults to `1`.
     * @property modeOffset Relative byte offset of mode identifier byte. Defaults to `0`.
     */
    @Serializable
    data class Fixed(
        val modeByte: Int = 0x00,
        val pwmOffset: Int = 1,
        override val modeOffset: Int = 0,
    ) : FanCapability

    /**
     * Multi-point temperature-dependent curve mode (typically a 16-point lookup curve).
     *
     * @property modeByte Raw mode identifier byte encoded into the HID control report. Defaults to `0x02`.
     * @property sensorIndexOffset Relative byte offset of 16-bit BE source sensor slot index. Defaults to `3`.
     * @property tempsOffset Relative byte offset of curve temperatures array. Defaults to `0x15` (21).
     * @property pwmsOffset Relative byte offset of curve PWM percentages array. Defaults to `0x35` (53).
     * @property pointsCount Number of calibration points in the curve. Defaults to `16`.
     * @property modeOffset Relative byte offset of mode identifier byte. Defaults to `0`.
     */
    @Serializable
    data class Curve(
        val modeByte: Int = 0x02,
        val sensorIndexOffset: Int = 3,
        val tempsOffset: Int = 0x15,
        val pwmsOffset: Int = 0x35,
        val pointsCount: Int = 16,
        override val modeOffset: Int = 0,
    ) : FanCapability

    /**
     * Target temperature regulation mode with PID or setpoint control.
     *
     * @property modeByte Raw mode identifier byte encoded into the HID control report. Defaults to `0x01`.
     * @property sensorIndexOffset Relative byte offset of 16-bit BE source sensor slot index. Defaults to `3`.
     * @property targetTempOffset Relative byte offset of 16-bit BE target temperature centidegrees. Defaults to `5`.
     * @property modeOffset Relative byte offset of mode identifier byte. Defaults to `0`.
     */
    @Serializable
    data class TargetTemp(
        val modeByte: Int = 0x01,
        val sensorIndexOffset: Int = 3,
        val targetTempOffset: Int = 5,
        override val modeOffset: Int = 0,
    ) : FanCapability

    /**
     * Two-point step control mode (supported on legacy devices such as Aquaero).
     *
     * @property modeByte Raw mode identifier byte encoded into the HID control report. Defaults to `0x48`.
     * @property sensorIndexOffset Relative byte offset of 16-bit BE source sensor slot index. Defaults to `3`.
     * @property modeOffset Relative byte offset of mode identifier byte. Defaults to `0`.
     */
    @Serializable
    data class TwoPoint(
        val modeByte: Int = 0x48,
        val sensorIndexOffset: Int = 3,
        override val modeOffset: Int = 0,
    ) : FanCapability

    /**
     * Mode allowing a fan channel to copy/mirror output from another fan channel.
     *
     * In the Aquacomputer HID protocol, follow mode byte values are contiguous:
     * `startModeByteInclusive + sourceChannel`.
     *
     * @property startModeByteInclusive Mode byte value representing follow mode for source channel 0 (e.g., `0x03`).
     * @property endModeByteInclusive Mode byte value representing follow mode for the highest supported source channel.
     * @property sensorIndexOffset Relative byte offset of source sensor slot index. Defaults to `3`.
     * @property unassignedSensorSentinel 16-bit sentinel written to sensor slot when in follow mode. Defaults to `0xFFFF`.
     * @property modeOffset Relative byte offset of mode identifier byte. Defaults to `0`.
     */
    @Serializable
    data class Follow(
        val startModeByteInclusive: Int = 0x03,
        val endModeByteInclusive: Int = 0x06,
        val sensorIndexOffset: Int = 3,
        val unassignedSensorSentinel: Int = 0xFFFF,
        override val modeOffset: Int = 0,
    ) : FanCapability {
        init {
            require(startModeByteInclusive <= endModeByteInclusive) {
                "startModeByteInclusive ($startModeByteInclusive) cannot be greater than endModeByteInclusive ($endModeByteInclusive)"
            }
        }
    }
}

sealed interface DeviceCapability {
    /**
     * Fan and cooling control capability.
     *
     * @property channels List of cooling channels (fan headers, pump controller) available on the device.
     * @property capabilities List of control modes supported across these channels.
     * @property telemetryLayout Telemetry layout specification for cooling channels on this device.
     * @property limitsLayout Layout specification for fan power limits and start-boost on this device.
     */
    @Serializable
    data class Fans(
        val channels: List<FanChannelDescriptor>,
        val capabilities: List<FanCapability> = defaultCapabilities,
        val telemetryLayout: FanTelemetryLayout = FanTelemetryLayout.Standard,
        val limitsLayout: FanLimitsLayout = FanLimitsLayout.Standard,
    ) : DeviceCapability {
        companion object {
            /** Standard fan control modes present on modern Aquacomputer controllers (Fixed, Curve, TargetTemp). */
            val defaultCapabilities =
                listOf(
                    FanCapability.Fixed(),
                    FanCapability.Curve(),
                    FanCapability.TargetTemp(),
                )
        }
    }

    /**
     * Temperature sensor input capability.
     *
     * @property sensors Physical and integrated temperature sensor descriptors.
     * @property disconnectedSentinelsWhenReading Set of 16-bit signed integer sentinels indicating an open-circuit or disconnected temperature sensor.
     */
    @Serializable
    sealed interface TemperatureSensors : DeviceCapability {
        val sensors: List<SensorDescriptor>
        val disconnectedSentinelsWhenReading: Set<Short>

        companion object {
            /** Standard open-circuit sentinels (0x7FFF / 32767 on modern devices, -32768 on legacy Aquaero). */
            val defaultDisconnectedSentinels: Set<Short> =
                setOf(0x7FFF.toShort(), (-32768).toShort())
        }

        /**
         * Temperature sensors that support hardware offset calibration written to device memory.
         *
         * @property sensors List of temperature sensors.
         * @property ctrlOffset Byte offset in the HID control report where calibration offsets are written.
         * @property disconnectedSentinelsWhenReading Raw sentinels indicating a disconnected probe (defaults to 0x7FFF and -32768).
         */
        @Serializable
        data class Calibrated(
            override val sensors: List<SensorDescriptor>,
            val ctrlOffset: Int,
            override val disconnectedSentinelsWhenReading: Set<Short> = defaultDisconnectedSentinels,
        ) : TemperatureSensors

        /**
         * Temperature sensors that provide read-only telemetry without onboard offset adjustment.
         *
         * @property sensors List of temperature sensors.
         * @property disconnectedSentinelsWhenReading Raw sentinels indicating a disconnected probe (defaults to 0x7FFF and -32768).
         */
        @Serializable
        data class ReadOnly(
            override val sensors: List<SensorDescriptor>,
            override val disconnectedSentinelsWhenReading: Set<Short> = defaultDisconnectedSentinels,
        ) : TemperatureSensors
    }

    /**
     * Software virtual temperature sensors feed capability.
     *
     * Allows host software (e.g. CPU/GPU temperature daemons) to continuously transmit
     * software sensor readings over USB Bulk Endpoint 0x02 to the controller for use in hardware fan curves.
     *
     * @property count Number of virtual sensor slots supported by the device (e.g. 16 on OCTO, 8 on D5 Next).
     * @property statusOffset Byte offset in the HID status report (Report 0x01) where live virtual sensor readings start.
     * @property reportId USB report ID used when transmitting bulk feed packets (defaults to `0x04`).
     * @property reportSize Full size of the virtual sensor feed packet in bytes (51 or 67).
     * @property valuesOffset Byte offset in the bulk feed packet where the 16-bit temperature values start (defaults to `1`).
     * @property typesOffset Byte offset in the bulk feed packet where the 8-bit sensor type flags start (defaults to `33`).
     * @property maxSlots Total slot positions in the wire packet (defaults to `16`).
     * @property reportSigner Strategy used to sign and checksum outgoing bulk reports.
     * @property disconnectedSentinelWhenWriting Sentinel value written into inactive/disabled slots when constructing bulk feed packets (defaults to 0x7FFF).
     * @property disconnectedSentinelsWhenReading Set of sentinel values indicating disconnected/inactive status when reading telemetry.
     */
    @Serializable
    sealed interface VirtualSensors : DeviceCapability {
        val count: Int
        val statusOffset: Int
        val reportId: Int
        val reportSize: Int
        val valuesOffset: Int
        val typesOffset: Int
        val maxSlots: Int
        val reportSigner: ReportSigner
        val disconnectedSentinelWhenWriting: Short
        val disconnectedSentinelsWhenReading: Set<Short>

        companion object {
            val defaultDisconnectedSentinels: Set<Short> =
                setOf(0x7FFF.toShort(), (-32768).toShort())
        }

        /**
         * Standard 51-byte bulk report without padding (e.g. OCTO, Farbwerk 360, Leakshield).
         *
         * @property count Number of virtual sensor slots supported by the device.
         * @property statusOffset Byte offset in the HID status report where live virtual sensor readings start.
         * @property reportId USB report ID (defaults to `0x04`).
         * @property valuesOffset Byte offset for temperature values (defaults to `1`).
         * @property typesOffset Byte offset for sensor type flags (defaults to `33`).
         * @property maxSlots Total slot positions in the wire packet (defaults to `16`).
         * @property reportSigner Report checksum/signing strategy.
         * @property disconnectedSentinelWhenWriting Sentinel value written into inactive slots (defaults to 0x7FFF).
         * @property disconnectedSentinelsWhenReading Set of sentinels indicating inactive status in telemetry.
         */
        @Serializable
        data class Standard51(
            override val count: Int,
            override val statusOffset: Int,
            override val reportId: Int = 0x04,
            override val valuesOffset: Int = 1,
            override val typesOffset: Int = 33,
            override val maxSlots: Int = 16,
            override val reportSigner: ReportSigner = ReportSigner.Crc16Usb,
            override val disconnectedSentinelWhenWriting: Short = 0x7FFF.toShort(),
            override val disconnectedSentinelsWhenReading: Set<Short> = defaultDisconnectedSentinels,
        ) : VirtualSensors {
            override val reportSize: Int get() = 51
        }

        /**
         * 67-byte bulk report requiring a 16-byte padding block before the CRC (e.g. QUADRO, D5 Next).
         *
         * @property count Number of virtual sensor slots supported by the device.
         * @property statusOffset Byte offset in the HID status report where live virtual sensor readings start.
         * @property reportId USB report ID (defaults to `0x04`).
         * @property valuesOffset Byte offset for temperature values (defaults to `1`).
         * @property typesOffset Byte offset for sensor type flags (defaults to `33`).
         * @property maxSlots Total slot positions in the wire packet (defaults to `16`).
         * @property paddingOffset Byte offset where the padding block starts (defaults to `49`).
         * @property paddingLength Length in bytes of the padding block (defaults to `16`).
         * @property paddingByte Byte value used to fill the padding block (defaults to `0x64u` / 100).
         * @property reportSigner Report checksum/signing strategy.
         * @property disconnectedSentinelWhenWriting Sentinel value written into inactive slots (defaults to 0x7FFF).
         * @property disconnectedSentinelsWhenReading Set of sentinels indicating inactive status in telemetry.
         */
        @Serializable
        data class Padded67(
            override val count: Int,
            override val statusOffset: Int,
            override val reportId: Int = 0x04,
            override val valuesOffset: Int = 1,
            override val typesOffset: Int = 33,
            override val maxSlots: Int = 16,
            val paddingOffset: Int = 49,
            val paddingLength: Int = 16,
            val paddingByte: UByte = 0x64u.toUByte(),
            override val reportSigner: ReportSigner = ReportSigner.Crc16Usb,
            override val disconnectedSentinelWhenWriting: Short = 0x7FFF.toShort(),
            override val disconnectedSentinelsWhenReading: Set<Short> = defaultDisconnectedSentinels,
        ) : VirtualSensors {
            override val reportSize: Int get() = 67
        }
    }

    /**
     * Flow sensor input capability.
     *
     * @property statusOffset Byte offset in the HID status report containing the raw flow rate reading.
     * @property portCount Number of flow sensor ports.
     */
    @Serializable
    sealed interface FlowSensor : DeviceCapability {
        val statusOffset: Int
        val portCount: Int

        /**
         * Flow sensor port supporting impulse calibration (pulses per liter) written to the device.
         *
         * @property statusOffset Byte offset in the HID status report for flow telemetry.
         * @property pulsesCtrlOffset Byte offset in the HID control report for the calibration impulse count (16-bit BE).
         * @property hwSensorSlot Internal hardware sensor slot index assigned to the flow sensor (used to distinguish
         *                         physical temperature sensors from flow pulses in fan curve source selection).
         * @property portCount Number of flow sensor ports (defaults to 1).
         */
        @Serializable
        data class Calibrated(
            override val statusOffset: Int,
            val pulsesCtrlOffset: Int,
            val hwSensorSlot: Int,
            override val portCount: Int = 1,
        ) : FlowSensor

        /**
         * Direct flow sensor reading without configurable pulses-per-liter calibration in control reports.
         *
         * @property statusOffset Byte offset in the HID status report for flow telemetry.
         * @property portCount Number of flow sensor ports (defaults to 1).
         */
        @Serializable
        data class Direct(
            override val statusOffset: Int,
            override val portCount: Int = 1,
        ) : FlowSensor
    }

    /**
     * Integrated display panel capability (e.g. OLED or LCD).
     *
     * @property kind The type of panel technology used by the display.
     */
    @Serializable
    sealed interface Display : DeviceCapability {
        val kind: DisplayKind

        /**
         * Display with configurable settings (e.g. brightness, orientation) written to device control reports.
         *
         * @property kind Display panel type.
         * @property ctrlOffset Byte offset in the HID control report where display configuration parameters are stored.
         */
        @Serializable
        data class Configurable(
            override val kind: DisplayKind = DisplayKind.OLED,
            val ctrlOffset: Int,
        ) : Display

        /**
         * Display that provides read-only telemetry or fixed hardware status without software control report configuration.
         *
         * @property kind Display panel type.
         */
        @Serializable
        @JvmInline
        value class ReadOnly(
            override val kind: DisplayKind = DisplayKind.OLED,
        ) : Display
    }

    /**
     * RGB and lighting capability.
     */
    @Serializable
    sealed interface Lighting : DeviceCapability {
        /**
         * Addressable RGBpx lighting controller (WS2812B compatible).
         *
         * @property portCount Number of independent RGBpx output ports.
         * @property maxLedsPerPort Maximum addressable LEDs supported per port (typically 90).
         * @property ctrlOffset Optional byte offset in the HID control report for lighting brightness or global mode.
         */
        @Serializable
        data class Rgbpx(
            val portCount: Int,
            val maxLedsPerPort: Int = 90,
            val ctrlOffset: Int? = null,
        ) : Lighting

        /**
         * Classic 12V analog 4-pin RGB channels (e.g. FARBWERK Classic).
         *
         * @property channelCount Number of analog RGB channels (typically 4).
         * @property resolutionBits Resolution of PWM brightness control (typically 13-bit).
         * @property voltage Supply voltage in volts (typically 12V).
         */
        @Serializable
        data class ClassicAnalogRgb(
            val channelCount: Int = 4,
            val resolutionBits: Int = 13,
            val voltage: Int = 12,
        ) : Lighting
    }

    /**
     * Coolant quality and electrical conductivity measurement capability (e.g. HIGH FLOW NEXT).
     *
     * @property conductivityOffset Byte offset in the HID status report for coolant conductivity in µS/cm.
     * @property qualityPercentOffset Byte offset in the HID status report for coolant quality rating percentage (0..100%).
     */
    @Serializable
    data class CoolantQuality(
        val conductivityOffset: Int? = null,
        val qualityPercentOffset: Int? = null,
    ) : DeviceCapability

    /**
     * Chamber pressure and vacuum regulation capability (e.g. LEAKSHIELD).
     *
     * @property pressureOffset Byte offset in the HID status report for current pressure in mbar.
     * @property targetPressureOffset Byte offset in the HID status report for target regulated pressure in mbar.
     * @property pumpCyclesOffset Byte offset in the HID status report for cumulative vacuum pump cycles.
     */
    @Serializable
    data class Pressure(
        val pressureOffset: Int? = null,
        val targetPressureOffset: Int? = null,
        val pumpCyclesOffset: Int? = null,
    ) : DeviceCapability
}
