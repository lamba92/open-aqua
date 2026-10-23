package io.github.lamba92.openaqua.app.feature.device.common

import io.github.lamba92.openaqua.model.FanChannelConfig
import io.github.lamba92.openaqua.model.FanTelemetry
import io.github.lamba92.openaqua.model.SensorOffset
import io.github.lamba92.openaqua.model.SensorTelemetry

enum class CommonDeviceTab {
    DEVICE_INFO,
    FANS,
    SENSORS,
}

data class FanItem(
    val channel: Int,
    val telemetry: FanTelemetry,
    val config: FanChannelConfig?,
)

enum class SensorChannelKind {
    PHYSICAL_NTC,
    VIRTUAL,
    INTEGRATED_COOLANT,
}

data class SensorItem(
    val channel: Int,
    val kind: SensorChannelKind,
    val telemetry: SensorTelemetry,
    val calibrationOffset: Double = 0.0,
)

fun List<FanTelemetry>.toPwmFanItems(configs: List<FanChannelConfig>): List<FanItem> =
    map { tel ->
        FanItem(
            channel = tel.channel,
            telemetry = tel,
            config = configs.firstOrNull { it.channel == tel.channel },
        )
    }

fun List<SensorTelemetry>.toSensorItems(offsets: List<SensorOffset>): List<SensorItem> =
    map { tel ->
        SensorItem(
            channel = tel.channel,
            kind = if (tel.isVirtual) SensorChannelKind.VIRTUAL else SensorChannelKind.PHYSICAL_NTC,
            telemetry = tel,
            calibrationOffset = offsets.firstOrNull { !tel.isVirtual && it.channel == tel.channel }?.offsetCelsius ?: 0.0,
        )
    }
