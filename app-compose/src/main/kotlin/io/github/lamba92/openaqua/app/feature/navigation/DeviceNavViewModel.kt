package io.github.lamba92.openaqua.app.feature.navigation

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.LoggingScope
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.feature.settings.SettingsTab
import io.github.lamba92.openaqua.app.model.NavigationSelectionStoredData
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.mvi.viewModelLogger
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.repository.VirtualSensorFeederService
import io.github.lamba92.openaqua.model.device.DeviceId
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

class DeviceNavViewModel(
    private val dataCacheService: DeviceDataCacheService,
    private val appDataRepository: AppDataRepository,
    private val feederService: VirtualSensorFeederService,
    scopeProvider: CoroutineScopeProvider,
) : MVIViewModel<DeviceNavState, DeviceNavEvent, DeviceNavEffect> {
    override val viewModelScope: LoggingScope = scopeProvider.getScope()

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
        combine(
            dataCacheService.state,
            feederService.feedErrorsState,
        ) { deviceStates, feedErrors ->
            deviceStates to feedErrors
        }.onEach { (deviceStates, feedErrors) ->
            val aqua = deviceStates.filterIsInstance<DeviceState.Aqua>()
            val errorDeviceIds = feedErrors.filterValues { it.isNotEmpty() }.keys
            state.update { currentState ->
                currentState.copy(
                    host = deviceStates.filterIsInstance<DeviceState.Host>().firstOrNull() ?: DeviceState.Host.Loading,
                    devices = aqua,
                    devicesWithFeedErrors = errorDeviceIds,
                    selectedItem =
                        when (val item = currentState.selectedItem) {
                            is DeviceNavState.SelectedItem.Device ->
                                if (aqua.any { it.device.id == item.deviceId }) {
                                    item
                                } else {
                                    null
                                }

                            null,
                            DeviceNavState.SelectedItem.Host,
                            DeviceNavState.SelectedItem.About,
                            is DeviceNavState.SelectedItem.Settings,
                            -> item
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
                            .first()
                            .data
                            .navigationSelection
                            ?: return@withTimeout

                    val initialSelection =
                        when (savedSelection) {
                            NavigationSelectionStoredData.Host -> DeviceNavState.SelectedItem.Host
                            NavigationSelectionStoredData.About -> DeviceNavState.SelectedItem.About
                            is NavigationSelectionStoredData.Settings -> DeviceNavState.SelectedItem.Settings(savedSelection.tab)
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
                        viewModelLogger.i { "Restored navigation selection: $initialSelection" }
                        state.update { currentState ->
                            currentState.copy(selectedItem = currentState.selectedItem ?: initialSelection)
                        }
                    }
                }
            } catch (_: TimeoutCancellationException) {
                viewModelLogger.i { "Navigation selection initialization timed out; defaulting to unselected" }
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
                DeviceNavEvent.SelectAbout -> selectAbout()
                DeviceNavEvent.SelectSettings -> selectSettings()
                is DeviceNavEvent.SelectSettingsTab -> selectSettingsTab(event.tab)
            }
        }

    private fun selectHost() {
        viewModelLogger.i { "Navigating to Host screen" }
        state.update { currentState ->
            currentState.copy(selectedItem = DeviceNavState.SelectedItem.Host)
        }
        appDataRepository.setNavigationSelection(NavigationSelectionStoredData.Host)
    }

    private fun selectDevice(deviceId: DeviceId) {
        viewModelLogger.i { "Navigating to device '$deviceId'" }
        val finalState =
            state.updateAndGet { currentState ->
                currentState.copy(
                    selectedItem =
                        when (currentState.selectedItem) {
                            is DeviceNavState.SelectedItem.Device -> currentState.selectedItem.copy(deviceId = deviceId)
                            else -> DeviceNavState.SelectedItem.Device(deviceId = deviceId, selectedTab = CommonDeviceTab.DEVICE_INFO)
                        },
                )
            }
        appDataRepository.setNavigationSelection(finalState.selectedItem?.toStoredData())
    }

    private fun selectTab(tab: CommonDeviceTab) {
        viewModelLogger.i { "Selected tab '$tab' for device" }
        state.update { currentState ->
            when (val currentItem = currentState.selectedItem) {
                is DeviceNavState.SelectedItem.Device -> {
                    val updated = currentItem.copy(selectedTab = tab)
                    appDataRepository.setNavigationSelection(updated.toStoredData())
                    currentState.copy(selectedItem = updated)
                }

                null,
                DeviceNavState.SelectedItem.Host,
                DeviceNavState.SelectedItem.About,
                is DeviceNavState.SelectedItem.Settings,
                -> currentState
            }
        }
    }

    private fun selectAbout() {
        viewModelLogger.i { "Navigating to About screen" }
        state.update { currentState ->
            currentState.copy(selectedItem = DeviceNavState.SelectedItem.About)
        }
        appDataRepository.setNavigationSelection(NavigationSelectionStoredData.About)
    }

    private fun selectSettings() {
        viewModelLogger.i { "Navigating to Settings screen" }
        val target =
            when (val current = state.value.selectedItem) {
                is DeviceNavState.SelectedItem.Settings -> current
                else -> DeviceNavState.SelectedItem.Settings()
            }
        state.update { currentState ->
            currentState.copy(selectedItem = target)
        }
        appDataRepository.setNavigationSelection(target.toStoredData())
    }

    private fun selectSettingsTab(tab: SettingsTab) {
        viewModelLogger.i { "Selected settings tab '$tab'" }
        val updated = DeviceNavState.SelectedItem.Settings(selectedTab = tab)
        state.update { currentState -> currentState.copy(selectedItem = updated) }
        appDataRepository.setNavigationSelection(updated.toStoredData())
    }
}
