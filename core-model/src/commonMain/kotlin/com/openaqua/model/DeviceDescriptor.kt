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
    val id: String,
    val name: String,
    val ctrlOffset: Int,
    val hwmonIndex: Int,
)

@Serializable
data class SensorDescriptor(
    val id: String,
    val name: String,
    val hwmonIndex: Int,
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
    val hasFlowSensor: Boolean = false,
    val hasRgbpx: Boolean = false,
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
                    FanDescriptor("fan1", "Fan 1 (Pump)", 0x5A, 1),
                    FanDescriptor("fan2", "Fan 2 (Upper)", 0xAF, 2),
                    FanDescriptor("fan3", "Fan 3 (Front)", 0x104, 3),
                    FanDescriptor("fan4", "Fan 4 (Rear)", 0x159, 4),
                    FanDescriptor("fan5", "Fan 5", 0x1AE, 5),
                    FanDescriptor("fan6", "Fan 6", 0x203, 6),
                    FanDescriptor("fan7", "Fan 7", 0x258, 7),
                    FanDescriptor("fan8", "Fan 8", 0x2AD, 8),
                ),
            tempSensors =
                listOf(
                    SensorDescriptor("temp1", "Sensor 1", 1),
                    SensorDescriptor("temp2", "Sensor 2", 2),
                    SensorDescriptor("temp3", "Sensor 3", 3),
                    SensorDescriptor("temp4", "Sensor 4", 4),
                ),
            hasFlowSensor = false,
            hasRgbpx = true,
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
                    FanDescriptor("fan1", "Fan 1", 0x36, 1),
                    FanDescriptor("fan2", "Fan 2", 0x8B, 2),
                    FanDescriptor("fan3", "Fan 3", 0xE0, 3),
                    FanDescriptor("fan4", "Fan 4", 0x135, 4),
                ),
            tempSensors =
                listOf(
                    SensorDescriptor("temp1", "Sensor 1", 1),
                    SensorDescriptor("temp2", "Sensor 2", 2),
                    SensorDescriptor("temp3", "Sensor 3", 3),
                    SensorDescriptor("temp4", "Sensor 4", 4),
                ),
            hasFlowSensor = true,
            hasRgbpx = true,
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
                    FanDescriptor("pump", "Pump", 0x96, 1),
                    FanDescriptor("fan", "Fan", 0x41, 2),
                ),
            tempSensors =
                listOf(
                    SensorDescriptor("temp1", "Liquid Temperature", 1),
                ),
            hasFlowSensor = true,
            hasRgbpx = true,
        )

    val ALL = listOf(OCTO, QUADRO, D5_NEXT)

    fun findByProductId(productId: Int): DeviceDescriptor? = ALL.firstOrNull { it.productId == productId }
}
