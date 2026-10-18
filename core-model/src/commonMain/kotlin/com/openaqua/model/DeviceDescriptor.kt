package com.openaqua.model

import kotlinx.serialization.Serializable

@Serializable
enum class DeviceType(
    val displayName: String,
) {
    OCTO("Aquacomputer OCTO"),
    QUADRO("Aquacomputer QUADRO"),
    D5_NEXT("Aquacomputer D5 NEXT"),
    FARBWERK_360("Aquacomputer Farbwerk 360"),
    HIGH_FLOW_NEXT("Aquacomputer High Flow NEXT"),
    UNKNOWN("Unknown Aquacomputer Device"),
}

@Serializable
data class FanDescriptor(
    val channel: Int,
    val ctrlOffset: Int,
    val hwmonIndex: Int = channel + 1,
)

@Serializable
data class SensorDescriptor(
    val channel: Int,
    val hwmonIndex: Int = channel + 1,
    val isVirtual: Boolean = false,
)

@Serializable
data class DeviceDescriptor(
    val type: DeviceType,
    val name: String,
    val vendorId: Int = 0x0C70,
    val productId: Int,
    val ctrlReportLength: Int,
    val statusReportLength: Int,
    val fanChannels: List<FanDescriptor>,
    val tempSensors: List<SensorDescriptor>,
    val tempCtrlOffset: Int? = null,
    val firmwareVersionOffset: Int? = 0x0D,
    val hasFlowSensor: Boolean = false,
    val hasRgbpx: Boolean = false,
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
            ctrlReportLength = 0x65F, // 1631 bytes (+1 ID)
            statusReportLength = 0x147,
            fanChannels =
                listOf(
                    FanDescriptor(0, 0x5A, 1),
                    FanDescriptor(1, 0xAF, 2),
                    FanDescriptor(2, 0x104, 3),
                    FanDescriptor(3, 0x159, 4),
                    FanDescriptor(4, 0x1AE, 5),
                    FanDescriptor(5, 0x203, 6),
                    FanDescriptor(6, 0x258, 7),
                    FanDescriptor(7, 0x2AD, 8),
                ),
            tempSensors =
                listOf(
                    SensorDescriptor(0, 1),
                    SensorDescriptor(1, 2),
                    SensorDescriptor(2, 3),
                    SensorDescriptor(3, 4),
                ),
            tempCtrlOffset = 0x0A,
            firmwareVersionOffset = 0x0D,
            hasFlowSensor = false,
            hasRgbpx = true,
            virtualSensorsCount = 16,
            virtualSensorsStartOffset = 0x45,
        )

    val QUADRO =
        DeviceDescriptor(
            type = DeviceType.QUADRO,
            name = "Aquacomputer QUADRO",
            productId = 0xF00D,
            ctrlReportLength = 0x3C1,
            statusReportLength = 0xDC,
            fanChannels =
                listOf(
                    FanDescriptor(0, 0x36, 1),
                    FanDescriptor(1, 0x8B, 2),
                    FanDescriptor(2, 0xE0, 3),
                    FanDescriptor(3, 0x135, 4),
                ),
            tempSensors =
                listOf(
                    SensorDescriptor(0, 1),
                    SensorDescriptor(1, 2),
                    SensorDescriptor(2, 3),
                    SensorDescriptor(3, 4),
                ),
            tempCtrlOffset = 0x0A,
            firmwareVersionOffset = 0x0D,
            hasFlowSensor = true,
            hasRgbpx = true,
            virtualSensorsCount = 16,
            virtualSensorsStartOffset = 0x3C,
        )

    val D5_NEXT =
        DeviceDescriptor(
            type = DeviceType.D5_NEXT,
            name = "Aquacomputer D5 NEXT",
            productId = 0xF00E,
            ctrlReportLength = 0x329,
            statusReportLength = 0x9E,
            fanChannels =
                listOf(
                    FanDescriptor(0, 0x96, 1),
                    FanDescriptor(1, 0x41, 2),
                ),
            tempSensors =
                listOf(
                    SensorDescriptor(0, 1),
                ),
            tempCtrlOffset = 0x2D,
            firmwareVersionOffset = 0x0D,
            hasFlowSensor = true,
            hasRgbpx = true,
            virtualSensorsCount = 8,
            virtualSensorsStartOffset = 0x3F,
        )

    val ALL = listOf(OCTO, QUADRO, D5_NEXT)

    fun findByProductId(productId: Int): DeviceDescriptor? = ALL.firstOrNull { it.productId == productId }
}
