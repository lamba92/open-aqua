package io.github.lamba92.openaqua.app.feature.fans

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.repository.OpenAquaEngineApi
import io.github.lamba92.openaqua.model.D5NextTelemetry
import io.github.lamba92.openaqua.model.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.DeviceRegistry
import io.github.lamba92.openaqua.model.DeviceTelemetry
import io.github.lamba92.openaqua.model.FanTelemetry
import io.github.lamba92.openaqua.model.OctoTelemetry
import io.github.lamba92.openaqua.model.QuadroTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FansViewModel(
    private val repository: OpenAquaEngineApi,
    scopeProvider: CoroutineScopeProvider,
) : MVIViewModel<FansState, FansEvent, FansEffect> {
    override val viewModelScope: CoroutineScope = scopeProvider.getScope()

    override val state: StateFlow<FansState>
        field = MutableStateFlow<FansState>(FansState.Loading)

    override val effects: SharedFlow<FansEffect>
        field = MutableSharedFlow<FansEffect>(extraBufferCapacity = 64)

    private var telemetryJob: Job? = null

    override fun onEvent(event: FansEvent) {
        when (event) {
            is FansEvent.LoadFans -> loadFans(event.device)
            is FansEvent.SelectFan -> selectFan(event.channel)
            FansEvent.RefreshTelemetry -> refreshTelemetry()
        }
    }

    private fun loadFans(device: DetectedHardwareDevice) {
        telemetryJob?.cancel()
        val descriptor = DeviceRegistry.findByType(device.type)

        val fans =
            descriptor.fanChannels.map { fanDesc ->
                val ch = fanDesc.channel
                FanItem(
                    channel = ch,
                    name = "Fan ${ch + 1}",
                    pinLabel = "PWM CH ${ch + 1}",
                    telemetry = null,
                )
            }

        val currentSelected = (state.value as? FansState.Ready)?.selectedChannel ?: 0
        val validSelected =
            if (fans.any { it.channel == currentSelected }) currentSelected else (fans.firstOrNull()?.channel ?: 0)

        state.update {
            FansState.Ready(
                device = device,
                fans = fans,
                selectedChannel = validSelected,
            )
        }

        telemetryJob =
            viewModelScope.launch {
                repository
                    .getTelemetryStream(device.id)
                    .collect { telemetry ->
                        val telemetryFans = extractFanTelemetry(telemetry)
                        state.update { current ->
                            if (current is FansState.Ready) {
                                val updatedFans =
                                    current.fans.map { fanItem ->
                                        val itemTelemetry =
                                            telemetryFans.firstOrNull { it.channel == fanItem.channel }
                                                ?: fanItem.telemetry
                                        fanItem.copy(telemetry = itemTelemetry)
                                    }
                                current.copy(fans = updatedFans, isRefreshing = false)
                            } else {
                                current
                            }
                        }
                    }
            }
    }

    private fun selectFan(channel: Int) {
        val current = state.value
        if (current is FansState.Ready) {
            state.update { current.copy(selectedChannel = channel) }
            viewModelScope.launch {
                effects.emit(FansEffect.FanSelected(channel))
            }
        }
    }

    private fun refreshTelemetry() {
        val current = state.value
        if (current is FansState.Ready) {
            state.update { current.copy(isRefreshing = true) }
        }
    }

    private fun extractFanTelemetry(telemetry: DeviceTelemetry?): List<FanTelemetry> =
        when (telemetry) {
            is OctoTelemetry -> telemetry.fans
            is QuadroTelemetry -> telemetry.fans
            is D5NextTelemetry -> listOf(telemetry.pump, telemetry.fan)
            null -> emptyList()
        }
}
