package io.github.lamba92.openaqua.app.feature.device.common

import androidx.compose.runtime.Composable
import io.github.lamba92.openaqua.app.model.DeviceStoredData
import io.github.lamba92.openaqua.app.model.SensorTargetType
import io.github.lamba92.openaqua.app.model.SensorTypesStoredData
import io.github.lamba92.openaqua.app.model.VirtualSensorFeedError
import io.github.lamba92.openaqua.app.model.VirtualSensorFeedSource
import io.github.lamba92.openaqua.app.model.targetTypeFor
import io.github.lamba92.openaqua.app.ui.components.FeederStatus
import io.github.lamba92.openaqua.model.device.CoolingChannelKind
import io.github.lamba92.openaqua.model.device.CoolingChannelTelemetry
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.FanChannelConfig
import io.github.lamba92.openaqua.model.device.SensorChannelKind
import io.github.lamba92.openaqua.model.device.SensorOffset
import io.github.lamba92.openaqua.model.device.SensorTelemetry
import io.github.lamba92.openaqua.model.device.tempCelsiusOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.d5_fan_name
import open_aqua.app_compose.generated.resources.d5_pump_name
import open_aqua.app_compose.generated.resources.fan_channel_name
import open_aqua.app_compose.generated.resources.sensor_channel_name
import open_aqua.app_compose.generated.resources.sensor_coolant_name
import open_aqua.app_compose.generated.resources.sensor_virtual_name
import org.jetbrains.compose.resources.stringResource

@Serializable
enum class CommonDeviceTab {
    DEVICE_INFO,
    FANS,
    SENSORS,
}

@Serializable
sealed interface ChannelName {
    @Serializable
    @SerialName("default")
    data object Default : ChannelName

    @Serializable(with = Custom.Serializer::class)
    @SerialName("custom")
    @JvmInline
    value class Custom(
        val value: String,
    ) : ChannelName {
        init {
            require(value.isNotBlank()) { "Custom channel name cannot be blank" }
        }

        object Serializer : KSerializer<Custom> {
            override val descriptor: SerialDescriptor =
                buildClassSerialDescriptor("custom") {
                    element<String>("value")
                }

            override fun serialize(
                encoder: Encoder,
                value: Custom,
            ) {
                encoder.encodeStructure(descriptor) {
                    encodeStringElement(descriptor, 0, value.value)
                }
            }

            override fun deserialize(decoder: Decoder): Custom =
                decoder.decodeStructure(descriptor) {
                    var str = ""
                    while (true) {
                        when (val index = decodeElementIndex(descriptor)) {
                            0 -> str = decodeStringElement(descriptor, 0)
                            CompositeDecoder.DECODE_DONE -> break
                            else -> error("Unexpected index: $index")
                        }
                    }
                    Custom(str)
                }
        }
    }
}

fun String.toChannelName(): ChannelName {
    val trimmed = trim()
    return if (trimmed.isEmpty()) ChannelName.Default else ChannelName.Custom(trimmed)
}

data class FanItem(
    val channel: Int,
    val name: ChannelName = ChannelName.Default,
    val telemetry: CoolingChannelTelemetry,
    val config: FanChannelConfig,
)

data class SensorItem(
    val channel: Int,
    val name: ChannelName = ChannelName.Default,
    val telemetry: SensorTelemetry,
    val calibrationOffset: Double = 0.0,
    val virtualFeedSource: VirtualSensorFeedSource? = null,
    val feedError: VirtualSensorFeedError? = null,
    val feedValue: Double? = null,
    val targetType: SensorTargetType = SensorTargetType.DIE,
)

fun virtualSensorIndexFor(channel: Int): Int = if (channel >= 4) channel - 3 else channel

val SensorItem.virtualSensorIndex: Int
    get() = virtualSensorIndexFor(channel)

@Composable
fun ChannelName.resolveSensorName(
    channel: Int,
    kind: SensorChannelKind,
): String =
    when (this) {
        is ChannelName.Custom -> value
        ChannelName.Default ->
            when (kind) {
                SensorChannelKind.PHYSICAL_NTC -> stringResource(Res.string.sensor_channel_name, channel + 1)
                SensorChannelKind.VIRTUAL -> stringResource(Res.string.sensor_virtual_name, virtualSensorIndexFor(channel))
                SensorChannelKind.INTEGRATED_COOLANT -> stringResource(Res.string.sensor_coolant_name)
            }
    }

@Composable
fun SensorItem.resolveName(): String = name.resolveSensorName(channel = channel, kind = telemetry.kind)

@Composable
fun ChannelName.resolveFanName(
    channel: Int,
    kind: CoolingChannelKind,
    hasPump: Boolean = false,
): String =
    when (this) {
        is ChannelName.Custom -> value
        ChannelName.Default ->
            when (kind) {
                CoolingChannelKind.PUMP -> stringResource(Res.string.d5_pump_name)
                CoolingChannelKind.FAN ->
                    if (channel == 1 && hasPump) {
                        stringResource(Res.string.d5_fan_name)
                    } else {
                        stringResource(Res.string.fan_channel_name, channel + 1)
                    }
            }
    }

@Composable
fun FanItem.resolveName(hasPump: Boolean = false): String =
    name.resolveFanName(
        channel = channel,
        kind = telemetry.kind,
        hasPump = hasPump,
    )

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
            name = storedData.fans[tel.channel]?.name ?: ChannelName.Default,
            telemetry = tel,
            config = configs.firstOrNull { it.channel == tel.channel } ?: error("No config for channel ${tel.channel}"),
        )
    }

fun List<SensorTelemetry>.toSensorItems(
    offsets: List<SensorOffset>,
    storedData: DeviceStoredData = DeviceStoredData(),
    feedErrors: Map<Int, VirtualSensorFeedError> = emptyMap(),
    resolveFeedValue: (VirtualSensorFeedSource) -> Double? = { null },
    deviceId: DeviceId? = null,
    sensorTypes: SensorTypesStoredData = SensorTypesStoredData.DEFAULT,
): List<SensorItem> =
    map { tel ->
        val feedSource = storedData.sensors[tel.channel]?.virtualFeedSource
        val targetType =
            when {
                tel.kind == SensorChannelKind.VIRTUAL ->
                    when (feedSource) {
                        is VirtualSensorFeedSource.Host -> sensorTypes.targetTypeFor(feedSource.sensorId)
                        is VirtualSensorFeedSource.Device -> sensorTypes.targetTypeFor(feedSource.deviceId, feedSource.channel)
                        null -> deviceId?.let { sensorTypes.targetTypeFor(it, tel.channel) } ?: SensorTargetType.DIE
                    }
                deviceId != null -> sensorTypes.targetTypeFor(deviceId, tel.channel)
                else -> SensorTargetType.DIE
            }
        SensorItem(
            channel = tel.channel,
            name = storedData.sensors[tel.channel]?.name ?: ChannelName.Default,
            telemetry = tel,
            calibrationOffset = offsets.firstOrNull { it.channel == tel.channel }?.offsetCelsius ?: 0.0,
            virtualFeedSource = feedSource,
            feedError = feedErrors[tel.channel],
            feedValue = feedSource?.let(resolveFeedValue),
            targetType = targetType,
        )
    }
