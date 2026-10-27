package io.github.lamba92.openaqua.app.feature.navigation

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.DeviceId
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
                state.update { currentState -> currentState.updateWith(cacheState) }
            }.launchIn(viewModelScope)
    }

    override fun onEvent(event: DeviceNavEvent) {
        when (event) {
            is DeviceNavEvent.SelectDevice -> selectDevice(event.deviceId)
            is DeviceNavEvent.SelectTab -> selectTab(event.tab)
            DeviceNavEvent.SelectSettings -> selectSettings()
        }
    }

    private fun selectDevice(deviceId: DeviceId) {
        state.update { currentState ->
            when (currentState) {
                is DeviceNavState.Ready -> {
                    val newTab =
                        when (val currentItem = currentState.selectedItem) {
                            is DeviceNavState.Ready.SelectedItem.Device if currentItem.deviceId == deviceId
                                -> currentItem.selectedTab

                            else -> CommonDeviceTab.DEVICE_INFO
                        }
                    currentState.copy(
                        selectedItem =
                            DeviceNavState.Ready.SelectedItem.Device(
                                deviceId = deviceId,
                                selectedTab = newTab,
                            ),
                    )
                }
                else -> currentState
            }
        }
    }

    private fun selectTab(tab: CommonDeviceTab) {
        state.update { currentState ->
            when (currentState) {
                is DeviceNavState.Ready ->
                    when (val currentItem = currentState.selectedItem) {
                        is DeviceNavState.Ready.SelectedItem.Device ->
                            currentState.copy(selectedItem = currentItem.copy(selectedTab = tab))
                        else -> currentState
                    }
                else -> currentState
            }
        }
    }

    private fun selectSettings() {
        state.update { currentState ->
            when (currentState) {
                is DeviceNavState.Ready ->
                    currentState.copy(
                        selectedItem = DeviceNavState.Ready.SelectedItem.Settings,
                    )
                else -> currentState
            }
        }
    }
}

private fun DeviceNavState.updateWith(cacheState: DeviceDataCacheService.State): DeviceNavState =
    when (cacheState) {
        DeviceDataCacheService.State.Loading -> DeviceNavState.Loading
        DeviceDataCacheService.State.NoDevices -> DeviceNavState.NoDevices
        is DeviceDataCacheService.State.Ready -> updateWith(cacheState.deviceStates)
    }

private fun DeviceNavState.updateWith(devices: List<DeviceState>): DeviceNavState.Ready =
    when (this) {
        is DeviceNavState.Ready ->
            copy(
                devices = devices,
                selectedItem = selectedItem.revalidate(devices),
            )
        else ->
            DeviceNavState.Ready(
                devices = devices,
                selectedItem =
                    DeviceNavState.Ready.SelectedItem.Device(
                        deviceId = devices.first().device.id,
                    ),
            )
    }

private fun DeviceNavState.Ready.SelectedItem.revalidate(devices: List<DeviceState>): DeviceNavState.Ready.SelectedItem =
    when (this) {
        is DeviceNavState.Ready.SelectedItem.Device -> {
            val validDeviceId =
                if (devices.any { it.device.id == deviceId }) {
                    deviceId
                } else {
                    devices.first().device.id
                }
            copy(deviceId = validDeviceId)
        }
        DeviceNavState.Ready.SelectedItem.Settings -> this
    }
