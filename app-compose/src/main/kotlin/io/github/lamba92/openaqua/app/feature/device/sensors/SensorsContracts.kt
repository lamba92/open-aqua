package io.github.lamba92.openaqua.app.feature.device.sensors

import androidx.compose.runtime.Composable
import io.github.lamba92.openaqua.app.feature.device.common.ChannelName
import io.github.lamba92.openaqua.app.feature.device.common.SensorItem
import io.github.lamba92.openaqua.app.feature.device.common.resolveSensorName
import io.github.lamba92.openaqua.app.model.SensorTargetType
import io.github.lamba92.openaqua.app.model.VirtualSensorFeedSource
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.SensorChannelKind
import io.github.lamba92.openaqua.model.host.HostSensorId

sealed interface VirtualSensorFeedDraft {
    data object None : VirtualSensorFeedDraft

    data class Host(
        val sensorId: HostSensorId? = null,
    ) : VirtualSensorFeedDraft

    data class Device(
        val deviceId: DeviceId? = null,
        val channel: Int? = null,
    ) : VirtualSensorFeedDraft
}

fun VirtualSensorFeedDraft.toFeedSource(): VirtualSensorFeedSource? =
    when (this) {
        VirtualSensorFeedDraft.None -> {
            null
        }

        is VirtualSensorFeedDraft.Host -> {
            sensorId?.let { VirtualSensorFeedSource.Host(it) }
        }

        is VirtualSensorFeedDraft.Device -> {
            when {
                deviceId != null && channel != null -> VirtualSensorFeedSource.Device(deviceId, channel)
                else -> null
            }
        }
    }

fun VirtualSensorFeedSource?.toDraft(): VirtualSensorFeedDraft =
    when (this) {
        null -> VirtualSensorFeedDraft.None
        is VirtualSensorFeedSource.Host -> VirtualSensorFeedDraft.Host(sensorId)
        is VirtualSensorFeedSource.Device -> VirtualSensorFeedDraft.Device(deviceId, channel)
    }

data class HostSensorOption(
    val id: HostSensorId,
    val name: String,
    val liveTemp: Double?,
)

data class DeviceSensorOption(
    val channel: Int,
    val kind: SensorChannelKind,
    val name: ChannelName = ChannelName.Default,
    val liveTemp: Double?,
)

@Composable
fun DeviceSensorOption.resolveName(): String = name.resolveSensorName(channel = channel, kind = kind)

data class AquaDeviceOption(
    val deviceId: DeviceId,
    val name: String,
    val sensors: List<DeviceSensorOption>,
)

sealed interface SensorsUiState {
    data object Loading : SensorsUiState

    data class Ready(
        val sensors: List<SensorItem>,
        val selectedSensorChannel: Int,
        val hideDisconnectedSensors: Boolean = false,
        val hideUnconfiguredVirtualSensors: Boolean = false,
        val availableHostSensors: List<HostSensorOption> = emptyList(),
        val availableAquaDevices: List<AquaDeviceOption> = emptyList(),
        val virtualFeedDrafts: Map<Int, VirtualSensorFeedDraft> = emptyMap(),
        val offsetDrafts: Map<Int, Double> = emptyMap(),
        val isFlashingOffset: Boolean = false,
        val isApplyingVirtualFeed: Boolean = false,
    ) : SensorsUiState {
        val selectedSensor: SensorItem?
            get() = sensors.firstOrNull { it.channel == selectedSensorChannel }

        fun isSensorOffsetModified(sensor: SensorItem): Boolean {
            if (sensor.telemetry.kind == SensorChannelKind.VIRTUAL) return false
            val draft = offsetDrafts[sensor.channel] ?: return false
            return draft != sensor.calibrationOffset
        }

        fun isVirtualFeedModified(sensor: SensorItem): Boolean {
            if (sensor.telemetry.kind != SensorChannelKind.VIRTUAL) return false
            val draft = virtualFeedDrafts[sensor.channel] ?: return false
            return draft.toFeedSource() != sensor.virtualFeedSource
        }

        fun isSensorModified(sensor: SensorItem): Boolean = isSensorOffsetModified(sensor) || isVirtualFeedModified(sensor)

        fun effectiveOffset(sensor: SensorItem): Double = offsetDrafts[sensor.channel] ?: sensor.calibrationOffset

        fun effectiveVirtualFeedDraft(sensor: SensorItem): VirtualSensorFeedDraft = virtualFeedDrafts[sensor.channel] ?: sensor.virtualFeedSource.toDraft()
    }
}

sealed interface SensorsUiEvent {
    @JvmInline
    value class SelectSensor(
        val channel: Int,
    ) : SensorsUiEvent

    data class RenameSensor(
        val channel: Int,
        val name: ChannelName,
    ) : SensorsUiEvent

    data object ToggleShowAllSensors : SensorsUiEvent

    data class SetSensorTargetType(
        val channel: Int,
        val targetType: SensorTargetType,
    ) : SensorsUiEvent

    data class UpdateVirtualFeedDraft(
        val channel: Int,
        val draft: VirtualSensorFeedDraft,
    ) : SensorsUiEvent

    @JvmInline
    value class ApplyVirtualFeed(
        val channel: Int,
    ) : SensorsUiEvent

    @JvmInline
    value class DiscardVirtualFeedDraft(
        val channel: Int,
    ) : SensorsUiEvent

    data class UpdateOffsetDraft(
        val channel: Int,
        val offset: Double,
    ) : SensorsUiEvent

    @JvmInline
    value class FlashOffset(
        val channel: Int,
    ) : SensorsUiEvent

    @JvmInline
    value class DiscardOffsetDraft(
        val channel: Int,
    ) : SensorsUiEvent
}

sealed interface SensorsUiEffect
