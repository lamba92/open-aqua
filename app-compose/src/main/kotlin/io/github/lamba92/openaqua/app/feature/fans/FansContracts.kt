package io.github.lamba92.openaqua.app.feature.fans

import io.github.lamba92.openaqua.model.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.FanTelemetry

data class FanItem(
    val channel: Int,
    val name: String,
    val pinLabel: String,
    val telemetry: FanTelemetry? = null,
)

sealed interface FansState {
    data object Loading : FansState

    data class Ready(
        val device: DetectedHardwareDevice,
        val fans: List<FanItem>,
        val selectedChannel: Int,
        val isRefreshing: Boolean = false,
    ) : FansState {
        val selectedFan: FanItem?
            get() = fans.firstOrNull { it.channel == selectedChannel } ?: fans.firstOrNull()
    }

    data class Error(
        val message: String,
    ) : FansState
}

sealed interface FansEvent {
    data class LoadFans(
        val device: DetectedHardwareDevice,
    ) : FansEvent

    data class SelectFan(
        val channel: Int,
    ) : FansEvent

    data object RefreshTelemetry : FansEvent
}

sealed interface FansEffect {
    data class FanSelected(
        val channel: Int,
    ) : FansEffect

    data class Notification(
        val message: String,
    ) : FansEffect
}
