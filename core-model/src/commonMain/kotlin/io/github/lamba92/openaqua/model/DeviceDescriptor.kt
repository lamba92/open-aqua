package io.github.lamba92.openaqua.model

import kotlinx.serialization.Serializable

@Serializable
enum class DeviceType {
    OCTO,
    QUADRO,
    D5_NEXT,
    FARBWERK_360,
    HIGH_FLOW_NEXT,
}

@Serializable
data class FanChannelDescriptor(
    val channel: Int,
    val ctrlOffset: Int,
    val statusOffset: Int,
)

@Serializable
data class SensorDescriptor(
    val channel: Int,
    val hwmonIndex: Int,
    val isVirtual: Boolean = false,
)

@Serializable
data class DeviceDescriptor(
    val type: DeviceType,
    val name: String,
    val productId: Int,
    val ctrlReportLength: Int,
    val statusReportLength: Int,
    val fanChannels: List<FanChannelDescriptor> = emptyList(),
    val tempSensors: List<SensorDescriptor> = emptyList(),
    val tempCtrlOffset: Int? = null,
    val firmwareVersionOffset: Int? = 0x0D,
    val hasFlowSensor: Boolean = false,
    val flowSensorOffset: Int? = null,
    val flowPulsesCtrlOffset: Int? = null,
    val hasRgbpx: Boolean = false,
    val rgbpxPortCount: Int = 0,
    val virtualSensorsCount: Int = 0,
    val virtualSensorsStartOffset: Int? = null,
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
            fanChannels =
                (0..7).map {
                    FanChannelDescriptor(
                        channel = it,
                        ctrlOffset = 0x5A + (it * 0x55),
                        statusOffset = 0x7D + (it * 0x0D),
                    )
                },
            tempSensors =
                (0..3).map {
                    SensorDescriptor(
                        channel = it,
                        hwmonIndex = it + 1,
                    )
                },
            tempCtrlOffset = 0x0A,
            firmwareVersionOffset = 0x0D,
            hasFlowSensor = true,
            flowSensorOffset = 0x7B,
            flowPulsesCtrlOffset = 0x06,
            hasRgbpx = true,
            rgbpxPortCount = 2,
            virtualSensorsCount = 16,
            virtualSensorsStartOffset = 0x45,
        )

    val QUADRO =
        DeviceDescriptor(
            type = DeviceType.QUADRO,
            name = "Aquacomputer QUADRO",
            productId = 0xF00D,
            ctrlReportLength = 0x3C1,
            statusReportLength = 0xA5,
            fanChannels =
                (0..3).map {
                    FanChannelDescriptor(
                        channel = it,
                        ctrlOffset = 0x36 + (it * 0x55),
                        statusOffset = 0x70 + (it * 0x0D),
                    )
                },
            tempSensors =
                (0..3).map {
                    SensorDescriptor(
                        channel = it,
                        hwmonIndex = it + 1,
                    )
                },
            tempCtrlOffset = 0x0A,
            firmwareVersionOffset = 0x0D,
            hasFlowSensor = true,
            flowSensorOffset = 0x6E,
            flowPulsesCtrlOffset = 0x06,
            hasRgbpx = true,
            rgbpxPortCount = 1,
            virtualSensorsCount = 16,
            virtualSensorsStartOffset = 0x3C,
        )

    val D5_NEXT =
        DeviceDescriptor(
            type = DeviceType.D5_NEXT,
            name = "Aquacomputer D5 Next",
            productId = 0xF00E,
            ctrlReportLength = 0x329,
            statusReportLength = 0x76,
            fanChannels =
                listOf(
                    FanChannelDescriptor(0, 0x36, 0x6C),
                    FanChannelDescriptor(1, 0x8B, 0x5F),
                ),
            tempSensors =
                listOf(
                    SensorDescriptor(0, 1),
                ),
            tempCtrlOffset = 0x2D,
            firmwareVersionOffset = 0x0D,
            hasFlowSensor = true,
            flowSensorOffset = null,
            flowPulsesCtrlOffset = null,
            hasRgbpx = true,
            rgbpxPortCount = 1,
            virtualSensorsCount = 8,
            virtualSensorsStartOffset = 0x3F,
        )

    val ALL = listOf(OCTO, QUADRO, D5_NEXT)

    fun findByProductId(productId: Int): DeviceDescriptor? = ALL.firstOrNull { it.productId == productId }

    fun findByType(type: DeviceType): DeviceDescriptor = ALL.firstOrNull { it.type == type } ?: error("Unsupported device type: $type")
}

fun DetectedHardwareDevice.descriptor(): DeviceDescriptor = DeviceRegistry.findByType(type)
