package io.github.lamba92.openaqua.app.feature.navigation

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.repository.OpenAquaEngineApi
import io.github.lamba92.openaqua.model.DetectedHardwareDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class DeviceNavViewModel(
    private val repository: OpenAquaEngineApi,
    scopeProvider: CoroutineScopeProvider,
) : MVIViewModel<DeviceNavState, DeviceNavEvent, DeviceNavEffect> {
    override val viewModelScope: CoroutineScope = scopeProvider.getScope()

    override val state: StateFlow<DeviceNavState>
        field = MutableStateFlow<DeviceNavState>(DeviceNavState.Loading)

    override val effects: SharedFlow<DeviceNavEffect>
        field = MutableSharedFlow<DeviceNavEffect>(extraBufferCapacity = 64)

    init {
        viewModelScope.launch {
            while (isActive) {
                scanDevices()
                delay(2.seconds)
            }
        }
    }

    override fun onEvent(event: DeviceNavEvent) {
        when (event) {
            DeviceNavEvent.LoadDevices -> viewModelScope.launch { scanDevices() }
            DeviceNavEvent.RefreshDevices -> refreshDevices()
            is DeviceNavEvent.SelectDevice -> selectDevice(event.device)
            is DeviceNavEvent.SelectComponent -> selectComponent(event.component)
        }
    }

    private suspend fun scanDevices() {
        val devices = repository.listDevices().devices
        if (devices.isNotEmpty()) {
            val currentSelected =
                (state.value as? DeviceNavState.Ready)
                    ?.selectedDevice
                    ?.let { sel -> devices.firstOrNull { it.id == sel.id } }
                    ?: devices.first()

            state.update { current ->
                val currentComp =
                    (current as? DeviceNavState.Ready)?.selectedComponent
                        ?: DeviceNavComponent.DEVICE_INFO
                DeviceNavState.Ready(
                    devices = devices,
                    selectedDevice = currentSelected,
                    selectedComponent = currentComp,
                    isRefreshing = false,
                )
            }
        } else {
            state.update { DeviceNavState.Error("No Aquacomputer devices detected") }
        }
    }

    private fun refreshDevices() {
        val current = state.value
        if (current is DeviceNavState.Ready) {
            state.update { current.copy(isRefreshing = true) }
        }
        viewModelScope.launch {
            scanDevices()
        }
    }

    private fun selectDevice(device: DetectedHardwareDevice) {
        val current = state.value
        if (current is DeviceNavState.Ready) {
            state.update { current.copy(selectedDevice = device) }
            viewModelScope.launch {
                effects.emit(DeviceNavEffect.DeviceChanged(device))
            }
        }
    }

    private fun selectComponent(component: DeviceNavComponent) {
        val current = state.value
        if (current is DeviceNavState.Ready) {
            state.update { current.copy(selectedComponent = component) }
            viewModelScope.launch {
                effects.emit(DeviceNavEffect.ComponentChanged(component))
            }
        }
    }
}
