package io.github.lamba92.openaqua.app.feature.navigation

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class DeviceNavViewModel(
    dataCacheService: DeviceDataCacheService,
    scopeProvider: CoroutineScopeProvider,
) : MVIViewModel<DeviceNavState, DeviceNavEvent, DeviceNavEffect> {
    override val viewModelScope: CoroutineScope = scopeProvider.getScope()

    override val state: StateFlow<DeviceNavState>
        field = MutableStateFlow<DeviceNavState>(DeviceNavState.Loading)

    override val effects: SharedFlow<DeviceNavEffect>
        field = MutableSharedFlow<DeviceNavEffect>()

    init {
        dataCacheService.state
            .onEach { cacheState ->
                state.update { currentState ->
                    when (cacheState) {
                        DeviceDataCacheService.State.Loading -> DeviceNavState.Loading
                        DeviceDataCacheService.State.NoDevices -> DeviceNavState.NoDevices
                        is DeviceDataCacheService.State.Ready -> {
                            val devices = cacheState.deviceStates
                            when (currentState) {
                                is DeviceNavState.Ready -> {
                                    val validSelectedId =
                                        if (devices.any { it.device.id == currentState.selectedDeviceId }) {
                                            currentState.selectedDeviceId
                                        } else {
                                            devices.first().device.id
                                        }
                                    currentState.copy(
                                        devices = devices,
                                        selectedDeviceId = validSelectedId,
                                    )
                                }
                                else ->
                                    DeviceNavState.Ready(
                                        devices = devices,
                                        selectedDeviceId = devices.first().device.id,
                                    )
                            }
                        }
                    }
                }
            }.launchIn(viewModelScope)
    }

    override fun onEvent(event: DeviceNavEvent) {
        when (event) {
            is DeviceNavEvent.SelectDevice -> selectDevice(event.deviceId)
        }
    }

    private fun selectDevice(deviceId: String) {
        state.update { currentState ->
            when (currentState) {
                is DeviceNavState.Ready -> currentState.copy(selectedDeviceId = deviceId)
                else -> currentState
            }
        }
    }
}
