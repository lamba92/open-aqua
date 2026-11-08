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
     * @property setupOffset Byte offset in the HID control report where the 7-byte limits/setup block is encoded
     *                       (flags, min power, max power, fallback power).
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
    /**
     * Fixed-power (manual PWM or voltage) mode.
     *
     * @property modeByte Raw mode identifier byte encoded into the HID control report. Defaults to `0x00`.
     */
    @Serializable
    @JvmInline
    value class Fixed(
        val modeByte: Int = 0x00,
    ) : FanCapability

    /**
     * Multi-point temperature-dependent curve mode (typically a 16-point lookup curve).
     *
     * @property modeByte Raw mode identifier byte encoded into the HID control report. Defaults to `0x02`.
     */
    @Serializable
    @JvmInline
    value class Curve(
        val modeByte: Int = 0x02,
    ) : FanCapability

    /**
     * Target temperature regulation mode with PID or setpoint control.
     *
     * @property modeByte Raw mode identifier byte encoded into the HID control report. Defaults to `0x01`.
     */
    @Serializable
    @JvmInline
    value class TargetTemp(
        val modeByte: Int = 0x01,
    ) : FanCapability

    /**
     * Two-point step control mode (supported on legacy devices such as Aquaero).
     *
     * @property modeByte Raw mode identifier byte encoded into the HID control report. Defaults to `0x48`.
     */
    @Serializable
    @JvmInline
    value class TwoPoint(
        val modeByte: Int = 0x48,
    ) : FanCapability

    /**
     * Mode allowing a fan channel to copy/mirror output from another fan channel.
     *
     * In the Aquacomputer HID protocol, follow mode byte values are contiguous:
     * `startModeByteInclusive + sourceChannel`.
     *
     * @property startModeByteInclusive Mode byte value representing follow mode for source channel 0 (e.g., `0x03`).
     * @property endModeByteInclusive Mode byte value representing follow mode for the highest supported source channel.
     */
    @Serializable
    data class Follow(
        val startModeByteInclusive: Int = 0x03,
        val endModeByteInclusive: Int = 0x06,
    ) : FanCapability {
        init {
            require(startModeByteInclusive <= endModeByteInclusive) {
                "startModeByteInclusive ($startModeByteInclusive) cannot be greater than endModeByteInclusive ($endModeByteInclusive)"
            }
        }
    }
}

/**
 * Represents a hardware capability or subsystem present on an Aquacomputer device.
 *
 * Capabilities encapsulate both layout details (byte offsets, counts) and supported features,
 * enabling protocol parsers and writers to operate polymorphically without guessing or null-checking.
 */
@Serializable
sealed interface DeviceCapability {

    /**
     * Fan and cooling control capability.
     *
     * @property channels List of cooling channels (fan headers, pump controller) available on the device.
     * @property capabilities List of control modes supported across these channels.
     */
    @Serializable
    data class Fans(
        val channels: List<FanChannelDescriptor>,
        val capabilities: List<FanCapability> = defaultCapabilities,
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
     */
    @Serializable
    sealed interface TemperatureSensors : DeviceCapability {
        val sensors: List<SensorDescriptor>

        /**
         * Temperature sensors that support hardware offset calibration written to device memory.
         *
         * @property sensors List of temperature sensors.
         * @property ctrlOffset Byte offset in the HID control report where calibration offsets are written.
         */
        @Serializable
        data class Calibrated(
            override val sensors: List<SensorDescriptor>,
            val ctrlOffset: Int,
        ) : TemperatureSensors

        /**
         * Temperature sensors that provide read-only telemetry without onboard offset adjustment.
         *
         * @property sensors List of temperature sensors.
         */
        @Serializable
        @JvmInline
        value class ReadOnly(
            override val sensors: List<SensorDescriptor>,
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

        /**
         * Standard 51-byte bulk report without padding (e.g. OCTO, Farbwerk 360, Leakshield).
         *
         * @property count Number of virtual sensor slots supported by the device.
         * @property statusOffset Byte offset in the HID status report where live virtual sensor readings start.
         * @property reportId USB report ID (defaults to `0x04`).
         * @property valuesOffset Byte offset for temperature values (defaults to `1`).
         * @property typesOffset Byte offset for sensor type flags (defaults to `33`).
         * @property maxSlots Total slot positions in the wire packet (defaults to `16`).
         */
        @Serializable
        data class Standard51(
            override val count: Int,
            override val statusOffset: Int,
            override val reportId: Int = 0x04,
            override val valuesOffset: Int = 1,
            override val typesOffset: Int = 33,
            override val maxSlots: Int = 16,
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
         * @property paddingOffset Byte offset where the 16-byte padding block starts (defaults to `49`).
         * @property paddingByte Byte value used to fill the padding block (defaults to `0x64u` / 100).
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
            val paddingByte: UByte = 0x64u.toUByte(),
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
