package io.github.lamba92.openaqua.app.feature.device.common

import io.github.lamba92.openaqua.app.model.DeviceStoredData
import io.github.lamba92.openaqua.app.model.VirtualSensorFeedError
import io.github.lamba92.openaqua.app.model.VirtualSensorFeedSource
import io.github.lamba92.openaqua.app.ui.components.FeederStatus
import io.github.lamba92.openaqua.model.device.CoolingChannelTelemetry
import io.github.lamba92.openaqua.model.device.FanChannelConfig
import io.github.lamba92.openaqua.model.device.SensorChannelKind
import io.github.lamba92.openaqua.model.device.SensorOffset
import io.github.lamba92.openaqua.model.device.SensorTelemetry
import io.github.lamba92.openaqua.model.device.tempCelsiusOrNull
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
    val virtualFeedSource: VirtualSensorFeedSource? = null,
    val feedError: VirtualSensorFeedError? = null,
    val feedValue: Double? = null,
)

val SensorItem.virtualSensorIndex: Int
    get() = if (channel >= 4) channel - 3 else channel

val SensorItem.effectiveTempCelsius: Double?
    get() =
        when (telemetry.kind) {
            SensorChannelKind.VIRTUAL -> telemetry.tempCelsiusOrNull ?: (if (feedError == null) feedValue else null)
            else -> telemetry.tempCelsiusOrNull
        }

val SensorItem.feederStatus: FeederStatus
    get() =
        when {
            telemetry.kind != SensorChannelKind.VIRTUAL -> FeederStatus.NONE
            feedError != null -> FeederStatus.ERROR
            virtualFeedSource != null -> FeederStatus.OPERATIONAL
            else -> FeederStatus.NONE
        }

val SensorItem.canBeHidden: Boolean
    get() =
        when (telemetry.kind) {
            SensorChannelKind.INTEGRATED_COOLANT -> false
            SensorChannelKind.PHYSICAL_NTC -> telemetry is SensorTelemetry.Disconnected
            SensorChannelKind.VIRTUAL -> effectiveTempCelsius == null && virtualFeedSource == null
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
    feedErrors: Map<Int, VirtualSensorFeedError> = emptyMap(),
    resolveFeedValue: (VirtualSensorFeedSource) -> Double? = { null },
): List<SensorItem> =
    map { tel ->
        val feedSource = storedData.sensors[tel.channel]?.virtualFeedSource
        SensorItem(
            channel = tel.channel,
            customName = storedData.sensors[tel.channel]?.customName,
            telemetry = tel,
            calibrationOffset = offsets.firstOrNull { it.channel == tel.channel }?.offsetCelsius ?: 0.0,
            virtualFeedSource = feedSource,
            feedError = feedErrors[tel.channel],
            feedValue = feedSource?.let(resolveFeedValue),
        )
    }
