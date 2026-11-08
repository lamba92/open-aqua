package io.github.lamba92.openaqua.model.device

import kotlinx.serialization.Serializable

/**
 * An inclusive range of firmware version integers.
 *
 * Used by [DeviceLayout] to associate specific HID packet structures and capability definitions
 * with corresponding firmware releases.
 *
 * @property minInclusive Lower firmware version bound (inclusive).
 * @property maxInclusive Upper firmware version bound (inclusive).
 */
@Serializable
data class FirmwareRange(
    val minInclusive: Int = 0,
    val maxInclusive: Int = Int.MAX_VALUE,
) {
    /**
     * Checks if [version] falls within this firmware range.
     */
    operator fun contains(version: Int): Boolean = version in minInclusive..maxInclusive

    companion object {
        /** Matches any firmware version. */
        val ANY = FirmwareRange(0, Int.MAX_VALUE)

        /** Matches any firmware version up to and including [maxInclusive]. */
        fun upTo(maxInclusive: Int) = FirmwareRange(0, maxInclusive)

        /** Matches any firmware version from [minInclusive] upwards. */
        fun from(minInclusive: Int) = FirmwareRange(minInclusive, Int.MAX_VALUE)
    }
}

/**
 * Associates a [FirmwareRange] with a specific [DeviceDescriptor] layout.
 *
 * When Aquacomputer updates device firmware in a way that alters report lengths or offset mappings,
 * multiple [DeviceLayout] entries can be registered for the same [DeviceType] to guarantee exact protocol fidelity.
 *
 * @property fwRange The firmware versions applicable to this descriptor layout.
 * @property descriptor The device descriptor defining capabilities and offsets for this firmware window.
 */
@Serializable
data class DeviceLayout(
    val fwRange: FirmwareRange,
    val descriptor: DeviceDescriptor,
)

/**
 * Resolves the appropriate [DeviceDescriptor] for this detected hardware device
 * based on its [DeviceType] and detected firmware version.
 */
fun DetectedHardwareDevice.descriptor(): DeviceDescriptor = DeviceLayoutRegistry.resolveDescriptor(type, firmwareVersion)

/**
 * Central registry of all firmware-version-aware device layouts.
 *
 * Supports querying the proper [DeviceDescriptor] given a [DeviceType] and optional firmware version number.
 */
object DeviceLayoutRegistry {
    /** Layouts for Aquacomputer OCTO fan controller. */
    val OCTO_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.OCTO,
            ),
        )

    /** Layouts for Aquacomputer QUADRO fan controller. */
    val QUADRO_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.QUADRO,
            ),
        )

    /** Layouts for Aquacomputer D5 NEXT pump. */
    val D5_NEXT_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.D5_NEXT,
            ),
        )

    /** Layouts for Aquacomputer FARBWERK 360 addressable RGB controller. */
    val FARBWERK360_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.FARBWERK360,
            ),
        )

    /** Layouts for Aquacomputer classic FARBWERK analog RGB controller. */
    val FARBWERK_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.FARBWERK,
            ),
        )

    /** Layouts for Aquacomputer HIGH FLOW NEXT flow and coolant sensor. */
    val HIGH_FLOW_NEXT_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.HIGH_FLOW_NEXT,
            ),
        )

    /** Layouts for Aquacomputer LEAKSHIELD leak prevention system. */
    val LEAKSHIELD_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.LEAKSHIELD,
            ),
        )

    /** Layouts for Aquacomputer Aquastream Ultimate pump. */
    val AQUASTREAM_ULTIMATE_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.AQUASTREAM_ULTIMATE,
            ),
        )

    /** Layouts for Aquacomputer Aquaero 5 and 6 controllers. */
    val AQUAERO_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.AQUAERO,
            ),
        )

    /** Layouts for Aquacomputer High Flow USB sensor. */
    val HIGH_FLOW_USB_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.HIGH_FLOW_USB,
            ),
        )

    /** Layouts for Aquacomputer Poweradjust 3 controller. */
    val POWERADJUST3_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.POWERADJUST3,
            ),
        )

    /** Layouts for Aquacomputer Aquastream XT pump. */
    val AQUASTREAM_XT_LAYOUTS: List<DeviceLayout> =
        listOf(
            DeviceLayout(
                fwRange = FirmwareRange.ANY,
                descriptor = DeviceRegistry.AQUASTREAM_XT,
            ),
        )

    /** Map of all device types to their respective firmware-versioned layout lists. */
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

    /**
     * Resolves the matching [DeviceDescriptor] for the given [type] and optional [firmwareVersion].
     *
     * If no [firmwareVersion] is supplied, or no version-specific rule matches, returns the first registered layout.
     *
     * @param type The [DeviceType] to look up.
     * @param firmwareVersion Optional integer firmware version reported by the device.
     * @return The resolved [DeviceDescriptor].
     * @throws IllegalStateException If no layouts are registered for [type].
     */
    fun resolveDescriptor(
        type: DeviceType,
        firmwareVersion: Int? = null,
    ): DeviceDescriptor {
        val layouts = ALL_LAYOUTS[type] ?: error("No layouts registered for device type $type")
        return when (firmwareVersion) {
            null -> layouts.first().descriptor
            else -> {
                layouts.firstOrNull { firmwareVersion in it.fwRange }?.descriptor
                    ?: layouts.first().descriptor
            }
        }
    }
}
