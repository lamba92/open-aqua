package io.github.lamba92.openaqua.app.feature.navigation

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.model.NavigationSelectionStoredData
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.device.DeviceId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

class DeviceNavViewModel(
    private val dataCacheService: DeviceDataCacheService,
    private val appDataRepository: AppDataRepository,
    scopeProvider: CoroutineScopeProvider,
) : MVIViewModel<DeviceNavState, DeviceNavEvent, DeviceNavEffect> {
    override val viewModelScope: CoroutineScope = scopeProvider.getScope()

    override val state: StateFlow<DeviceNavState>
        field = MutableStateFlow(DeviceNavState())

    override val effects: SharedFlow<DeviceNavEffect>
        field = MutableSharedFlow()

    init {
        // Wait for devices to settle, then restore the saved navigation selection once
        initializeNavigationSelection()
        observeDeviceStates()
    }

    private fun observeDeviceStates() {
        dataCacheService.state
            .onEach { deviceStates ->
                state.update { currentState ->
                    val aqua = deviceStates.filterIsInstance<DeviceState.Aqua>()
                    currentState.copy(
                        host = deviceStates.filterIsInstance<DeviceState.Host>().firstOrNull() ?: DeviceState.Host.Loading,
                        devices = aqua,
                        selectedItem =
                            when (val item = currentState.selectedItem) {
                                is DeviceNavState.SelectedItem.Device ->
                                    if (aqua.any { it.device.id == item.deviceId }) item else null

                                else -> item
                            },
                    )
                }
            }.launchIn(viewModelScope)
    }

    private fun initializeNavigationSelection() {
        viewModelScope.launch {
            try {
                withTimeout(5.seconds) {
                    val savedSelection =
                        appDataRepository.state
                            .filterIsInstance<AppDataRepository.State.Ready>()
                            .map { it.data.navigationSelection }
                            .first()
                            ?: return@withTimeout

                    val initialSelection =
                        when (savedSelection) {
                            NavigationSelectionStoredData.Host -> DeviceNavState.SelectedItem.Host
                            NavigationSelectionStoredData.Settings -> DeviceNavState.SelectedItem.Settings
                            is NavigationSelectionStoredData.Device -> {
                                val aquaDevices: List<DeviceState.Aqua> =
                                    dataCacheService.state
                                        .map { it.filterIsInstance<DeviceState.Aqua>() }
                                        .filter { it.isNotEmpty() }
                                        .first { it.all { it is DeviceState.Aqua.Ready || it is DeviceState.Aqua.Error } }

                                if (aquaDevices.any { it.device.id == savedSelection.deviceId }) {
                                    DeviceNavState.SelectedItem.Device(savedSelection.deviceId, savedSelection.tab)
                                } else {
                                    null
                                }
                            }
                        }

                    if (initialSelection != null) {
                        state.update { currentState ->
                            currentState.copy(selectedItem = currentState.selectedItem ?: initialSelection)
                        }
                    }
                }
            } catch (_: TimeoutCancellationException) {
                appDataRepository.update { it.copy(navigationSelection = null) }
            }
        }
    }

    override fun onEvent(event: DeviceNavEvent): Job =
        viewModelScope.launch {
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
        appDataRepository.setNavigationSelection(NavigationSelectionStoredData.Host)
    }

    private fun selectDevice(deviceId: DeviceId) {
        val selectedItem =
            DeviceNavState.SelectedItem.Device(
                deviceId = deviceId,
                selectedTab =
                    when (val currentSelectedItem = state.value.selectedItem) {
                        is DeviceNavState.SelectedItem.Device if currentSelectedItem.deviceId == deviceId ->
                            currentSelectedItem.selectedTab

                        else -> CommonDeviceTab.DEVICE_INFO
                    },
            )
        state.update { currentState ->
            currentState.copy(selectedItem = selectedItem)
        }
        appDataRepository.setNavigationSelection(selectedItem.toStoredData())
    }

    private fun selectTab(tab: CommonDeviceTab) {
        state.update { currentState ->
            when (val currentItem = currentState.selectedItem) {
                is DeviceNavState.SelectedItem.Device -> {
                    val updated = currentItem.copy(selectedTab = tab)
                    appDataRepository.setNavigationSelection(updated.toStoredData())
                    currentState.copy(selectedItem = updated)
                }

                null,
                DeviceNavState.SelectedItem.Host,
                DeviceNavState.SelectedItem.Settings,
                -> currentState
            }
        }
    }

    private fun selectSettings() {
        state.update { currentState ->
            currentState.copy(selectedItem = DeviceNavState.SelectedItem.Settings)
        }
        appDataRepository.setNavigationSelection(NavigationSelectionStoredData.Settings)
    }
}
