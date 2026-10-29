package io.github.lamba92.openaqua.app.feature.device.common

import io.github.lamba92.openaqua.app.model.DeviceStoredData
import io.github.lamba92.openaqua.model.device.CoolingChannelTelemetry
import io.github.lamba92.openaqua.model.device.FanChannelConfig
import io.github.lamba92.openaqua.model.device.SensorChannelKind
import io.github.lamba92.openaqua.model.device.SensorOffset
import io.github.lamba92.openaqua.model.device.SensorTelemetry
import kotlinx.serialization.Serializable

@Serializable
enum class CommonDeviceTab {
    DEVICE_INFO,
    FANS,
    SENSORS,
}

data class FanItem(
    val channel: Int,
    val customName: String? = null,
    val telemetry: CoolingChannelTelemetry,
    val config: FanChannelConfig,
)

data class SensorItem(
    val channel: Int,
    val customName: String? = null,
    val telemetry: SensorTelemetry,
    val calibrationOffset: Double = 0.0,
)

val SensorItem.canBeHidden: Boolean
    get() =
        when (telemetry.kind) {
            SensorChannelKind.INTEGRATED_COOLANT -> false
            SensorChannelKind.PHYSICAL_NTC, SensorChannelKind.VIRTUAL ->
                telemetry is SensorTelemetry.Disconnected
        }

fun List<CoolingChannelTelemetry>.toPwmFanItems(
    configs: List<FanChannelConfig>,
    storedData: DeviceStoredData = DeviceStoredData(),
): List<FanItem> =
    map { tel ->
        FanItem(
            channel = tel.channel,
            customName = storedData.fans[tel.channel]?.customName,
            telemetry = tel,
            config = configs.firstOrNull { it.channel == tel.channel } ?: error("No config for channel ${tel.channel}"),
        )
    }

fun List<SensorTelemetry>.toSensorItems(
    offsets: List<SensorOffset>,
    storedData: DeviceStoredData = DeviceStoredData(),
): List<SensorItem> =
    map { tel ->
        SensorItem(
            channel = tel.channel,
            customName = storedData.sensors[tel.channel]?.customName,
            telemetry = tel,
            calibrationOffset = offsets.firstOrNull { it.channel == tel.channel }?.offsetCelsius ?: 0.0,
        )
    }
