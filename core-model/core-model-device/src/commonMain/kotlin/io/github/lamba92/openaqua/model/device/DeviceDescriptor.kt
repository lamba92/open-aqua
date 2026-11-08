package io.github.lamba92.openaqua.model.device

import kotlinx.serialization.Serializable

/**
 * Enumeration of supported Aquacomputer USB devices.
 */
@Serializable
enum class DeviceType {
    /** 8-channel PWM fan controller with 4 temp sensor inputs and RGBpx. */
    OCTO,

    /** 4-channel PWM fan controller with 4 temp sensor inputs and RGBpx. */
    QUADRO,

    /** Integrated D5 pump with OLED display, coolant temp sensor, RGBpx, and virtual sensor support. */
    D5_NEXT,

    /** 4-port addressable RGBpx controller with 4 temp sensor inputs. */
    FARBWERK360,

    /** Classic 4-channel analog 12V RGB controller. */
    FARBWERK,

    /** Flow, water temperature, coolant quality, and conductivity meter with OLED display and RGBpx. */
    HIGH_FLOW_NEXT,

    /** Active leak prevention system with vacuum pump, pressure sensor, and OLED display. */
    LEAKSHIELD,

    /** High-end pump with OLED display, fan output, and temperature monitoring. */
    AQUASTREAM_ULTIMATE,

    /** Flagship multi-channel system controller (Aquaero 5 and 6 series). */
    AQUAERO,

    /** Classic USB flow sensor interface. */
    HIGH_FLOW_USB,

    /** Single-channel high-power fan and pump speed controller. */
    POWERADJUST3,

    /** Classic USB-controlled water cooling pump. */
    AQUASTREAM_XT,
}

/**
 * Complete hardware specification and protocol layout for an Aquacomputer device.
 *
 * A descriptor acts as the single source of truth for HID report packet sizes, firmware offsets,
 * protocol communication strategies, and hardware capabilities ([DeviceCapability]).
 *
 * @property type Enum identifying the family of the device.
 * @property name Human-readable marketing and device name.
 * @property productId USB Product ID (PID) under vendor ID `0x0C70`.
 * @property protocol Wire-level USB HID protocol specification (report IDs, lengths, commit strategy).
 * @property capabilities List of hardware subsystems and functional capabilities implemented on this device.
 */
@Serializable
data class DeviceDescriptor(
    val type: DeviceType,
    val name: String,
    val productId: Int,
    val protocol: DeviceProtocol,
    val capabilities: List<DeviceCapability> = emptyList(),
)

/**
 * Retrieves the first capability of type [T] registered on this device, or `null` if not supported.
 */
inline fun <reified T : DeviceCapability> DeviceDescriptor.findCapability(): T? = capabilities.filterIsInstance<T>().firstOrNull()

/**
 * Checks whether this device supports capability [T].
 */
inline fun <reified T : DeviceCapability> DeviceDescriptor.hasCapability(): Boolean = findCapability<T>() != null

/**
 * Retrieves the capability of type [T] registered on this device, or throws an [IllegalStateException] if absent.
 */
inline fun <reified T : DeviceCapability> DeviceDescriptor.requireCapability(): T =
    findCapability<T>() ?: error("Device $name ($type) does not have capability ${T::class.simpleName}")

/**
 * Retrieves all capabilities of type [T] registered on this device.
 */
inline fun <reified T : DeviceCapability> DeviceDescriptor.findCapabilities(): List<T> = capabilities.filterIsInstance<T>()

/**
 * Retrieves a specific fan control mode capability [T] (e.g. [FanCapability.Follow] or [FanCapability.TargetTemp])
 * from the device's [DeviceCapability.Fans] declaration, or `null` if unsupported.
 */
inline fun <reified T : FanCapability> DeviceDescriptor.findFanCapability(): T? =
    findCapability<DeviceCapability.Fans>()?.capabilities?.filterIsInstance<T>()?.firstOrNull()

/**
 * Maps a logical sensor index (0-indexed across user-visible temperature and virtual sensors) to the
 * hardware sensor index used in Aquacomputer fan curve and control report source selection.
 *
 * On devices where the flow sensor occupies an internal hardware sensor slot (e.g. slot 4 on OCTO/QUADRO),
 * any logical sensor at or above that slot index must be shifted by +1 to skip the flow sensor slot.
 *
 * @param logicalIndex User-visible sensor index (0-based).
 * @return Hardware sensor index to write into HID control packets.
 */
fun DeviceDescriptor.logicalToHardwareSensorIndex(logicalIndex: Int): Int =
    when (val flow = findCapability<DeviceCapability.FlowSensor>()) {
        is DeviceCapability.FlowSensor.Calibrated -> {
            when {
                logicalIndex >= flow.hwSensorSlot -> logicalIndex + 1
                else -> logicalIndex
            }
        }
        else -> logicalIndex
    }

/**
 * Maps a hardware sensor index read from an Aquacomputer control report back to the user-visible logical sensor index.
 *
 * @param hardwareIndex Hardware sensor slot index stored on the device.
 * @return Logical sensor index, or `null` if the slot represents a non-temperature source (e.g. flow sensor slot).
 */
fun DeviceDescriptor.hardwareToLogicalSensorIndex(hardwareIndex: Int): Int? =
    when (val flow = findCapability<DeviceCapability.FlowSensor>()) {
        is DeviceCapability.FlowSensor.Calibrated -> {
            when {
                hardwareIndex < flow.hwSensorSlot -> hardwareIndex
                hardwareIndex == flow.hwSensorSlot -> null
                else -> hardwareIndex - 1
            }
        }
        else -> hardwareIndex
    }

/**
 * The logical channel index where virtual sensors start on this device.
 *
 * In OpenAqua's unified channel addressing, physical temperature sensors occupy channels
 * `0 until physicalCount`, and virtual sensors follow immediately after at `physicalCount until physicalCount + virtualCount`.
 */
val DeviceDescriptor.virtualSensorsStartChannel: Int
    get() = findCapability<DeviceCapability.TemperatureSensors>()?.sensors?.size ?: 0

/**
 * The range of logical channel indices representing software virtual sensors on this device.
 */
val DeviceDescriptor.virtualSensorsChannelRange: IntRange
    get() {
        val count = findCapability<DeviceCapability.VirtualSensors>()?.count ?: 0
        return when {
            count > 0 -> virtualSensorsStartChannel until (virtualSensorsStartChannel + count)
            else -> IntRange.EMPTY
        }
    }
