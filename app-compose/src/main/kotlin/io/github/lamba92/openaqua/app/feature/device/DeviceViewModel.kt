package io.github.lamba92.openaqua.app.feature.device

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.device.DeviceId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DeviceViewModel(
    cacheService: DeviceDataCacheService,
    scopeProvider: CoroutineScopeProvider,
    deviceId: DeviceId,
) : MVIViewModel<DeviceUiState, DeviceUiEvent, DeviceUiEffect> {
    override val viewModelScope: CoroutineScope = scopeProvider.getScope()

    override val state: StateFlow<DeviceUiState>
        field = MutableStateFlow<DeviceUiState>(DeviceUiState.Loading)

    override val effects: SharedFlow<DeviceUiEffect>
        field = MutableSharedFlow<DeviceUiEffect>()

    init {
        cacheService
            .getDeviceStateFlow(deviceId)
            .onEach { deviceState ->
                state.value =
                    when (deviceState) {
                        is DeviceState.Connecting -> DeviceUiState.Loading
                        is DeviceState.Error ->
                            DeviceUiState.Error(
                                device = deviceState.device,
                                errorType = deviceState.errorType,
                            )

                        is DeviceState.Ready ->
                            DeviceUiState.Ready(
                                deviceState = deviceState,
                            )
                    }
            }.launchIn(viewModelScope)
    }

    override fun onEvent(event: DeviceUiEvent) {
        // Device-level events if needed in the future
    }
}
