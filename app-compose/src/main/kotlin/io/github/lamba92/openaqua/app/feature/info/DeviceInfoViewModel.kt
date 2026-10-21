package io.github.lamba92.openaqua.app.feature.info

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.repository.OpenAquaEngineApi
import io.github.lamba92.openaqua.model.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.DeviceRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class DeviceInfoViewModel(
    @Suppress("unused") private val repository: OpenAquaEngineApi,
    scopeProvider: CoroutineScopeProvider,
) : MVIViewModel<DeviceInfoState, DeviceInfoEvent, DeviceInfoEffect> {
    override val viewModelScope: CoroutineScope = scopeProvider.getScope()

    override val state: StateFlow<DeviceInfoState>
        field = MutableStateFlow<DeviceInfoState>(DeviceInfoState.Idle)

    override val effects: SharedFlow<DeviceInfoEffect>
        field = MutableSharedFlow<DeviceInfoEffect>(extraBufferCapacity = 64)

    override fun onEvent(event: DeviceInfoEvent) {
        when (event) {
            is DeviceInfoEvent.SetDevice -> setDevice(event.device)
            DeviceInfoEvent.RefreshInfo -> refreshInfo()
        }
    }

    private fun setDevice(device: DetectedHardwareDevice) {
        val descriptor = DeviceRegistry.findByType(device.type)
        state.update { DeviceInfoState.Loaded(device, descriptor) }
    }

    private fun refreshInfo() {
        val current = state.value
        if (current is DeviceInfoState.Loaded) {
            setDevice(current.device)
        }
    }
}
