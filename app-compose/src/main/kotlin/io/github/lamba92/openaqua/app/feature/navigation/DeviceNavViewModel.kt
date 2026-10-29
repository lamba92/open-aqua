package io.github.lamba92.openaqua.app.feature.navigation

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
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
import kotlinx.coroutines.flow.update

class DeviceNavViewModel(
    dataCacheService: DeviceDataCacheService,
    scopeProvider: CoroutineScopeProvider,
) : MVIViewModel<DeviceNavState, DeviceNavEvent, DeviceNavEffect> {
    override val viewModelScope: CoroutineScope = scopeProvider.getScope()

    override val state: StateFlow<DeviceNavState>
        field = MutableStateFlow(DeviceNavState())

    override val effects: SharedFlow<DeviceNavEffect>
        field = MutableSharedFlow()

    init {
        dataCacheService.state
            .onEach { deviceStates ->
                state.update { currentState ->
                    currentState.copy(
                        host = deviceStates.filterIsInstance<DeviceState.Host>().firstOrNull() ?: DeviceState.Host.Loading,
                        devices = deviceStates.filterIsInstance<DeviceState.Aqua>(),
                        selectedItem = currentState.selectedItem?.revalidate(deviceStates.filterIsInstance<DeviceState.Aqua>()),
                    )
                }
            }.launchIn(viewModelScope)
    }

    override fun onEvent(event: DeviceNavEvent) {
        when (event) {
            DeviceNavEvent.SelectHost -> selectHost()
            is DeviceNavEvent.SelectDevice -> selectDevice(event.deviceId)
            is DeviceNavEvent.SelectTab -> selectTab(event.tab)
            DeviceNavEvent.SelectSettings -> selectSettings()
        }
    }

    private fun selectHost() {
        state.update { currentState ->
            currentState.copy(selectedItem = DeviceNavState.SelectedItem.Host)
        }
    }

    private fun selectDevice(deviceId: DeviceId) {
        state.update { currentState ->
            currentState.copy(
                selectedItem =
                    DeviceNavState.SelectedItem.Device(
                        deviceId = deviceId,
                        selectedTab =
                            when (val currentSelectedItem = currentState.selectedItem) {
                                is DeviceNavState.SelectedItem.Device if currentSelectedItem.deviceId == deviceId ->
                                    currentSelectedItem.selectedTab
                                else -> CommonDeviceTab.DEVICE_INFO
                            },
                    ),
            )
        }
    }

    private fun selectTab(tab: CommonDeviceTab) {
        state.update { currentState ->
            when (val currentItem = currentState.selectedItem) {
                is DeviceNavState.SelectedItem.Device ->
                    currentState.copy(selectedItem = currentItem.copy(selectedTab = tab))
                else -> currentState
            }
        }
    }

    private fun selectSettings() {
        state.update { currentState ->
            currentState.copy(
                selectedItem = DeviceNavState.SelectedItem.Settings,
            )
        }
    }
}

private fun DeviceNavState.SelectedItem.revalidate(aquaDevices: List<DeviceState.Aqua>): DeviceNavState.SelectedItem? =
    when (this) {
        is DeviceNavState.SelectedItem.Device -> {
            when {
                aquaDevices.any { it.device.id == deviceId } -> this
                else -> null
            }
        }
        DeviceNavState.SelectedItem.Host,
        DeviceNavState.SelectedItem.Settings,
        -> this
    }
