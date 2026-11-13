package io.github.lamba92.openaqua.app.feature.host.fans

import io.github.lamba92.openaqua.app.feature.device.common.ChannelName
import io.github.lamba92.openaqua.app.model.HostFanConfig
import io.github.lamba92.openaqua.app.model.SensorSource
import io.github.lamba92.openaqua.model.host.HostFanId
import io.github.lamba92.openaqua.model.host.HostFanTelemetry

data class AvailableSensorItem(
    val source: SensorSource,
    val displayName: String,
    val currentTempCelsius: Double?,
)

data class HostFanItem(
    val id: HostFanId,
    val defaultName: String,
    val customName: ChannelName,
    val telemetry: HostFanTelemetry,
    val config: HostFanConfig,
    val deviceName: String = "",
    val isControllable: Boolean = true,
)

fun HostFanItem.resolveName(): String =
    when (customName) {
        is ChannelName.Custom -> customName.value
        ChannelName.Default -> defaultName
    }

sealed interface HostFansUiState {
    data object Loading : HostFansUiState

    data class Ready(
        val fans: List<HostFanItem>,
        val selectedFanId: HostFanId?,
        val availableSensors: List<AvailableSensorItem>,
        val fanDrafts: Map<HostFanId, HostFanConfig> = emptyMap(),
        val hideDisconnectedFans: Boolean = false,
        val hideDisconnectedSensors: Boolean = false,
    ) : HostFansUiState
}

fun HostFansUiState.Ready.isFanModified(fan: HostFanItem): Boolean {
    if (!fan.isControllable) return false
    val draft = fanDrafts[fan.id] ?: return false
    return draft != fan.config
}

val HostFansUiState.Ready.pendingFans: Map<HostFanItem, HostFanConfig>
    get() =
        fans
            .mapNotNull { fan ->
                if (!fan.isControllable) return@mapNotNull null
                val draft = fanDrafts[fan.id] ?: return@mapNotNull null
                if (draft != fan.config) fan to draft else null
            }.toMap()

sealed interface HostFansUiEvent {
    @JvmInline
    value class SelectFan(
        val id: HostFanId,
    ) : HostFansUiEvent

    data class RenameFan(
        val id: HostFanId,
        val name: String?,
    ) : HostFansUiEvent

    data class UpdateDraftConfig(
        val id: HostFanId,
        val config: HostFanConfig,
    ) : HostFansUiEvent

    @JvmInline
    value class DiscardFanDraft(
        val id: HostFanId,
    ) : HostFansUiEvent

    data object DiscardAllDrafts : HostFansUiEvent

    data object ApplyChanges : HostFansUiEvent

    data object ToggleHideDisconnectedFans : HostFansUiEvent

    data object ToggleHideDisconnectedSensors : HostFansUiEvent
}

sealed interface HostFansUiEffect
