package io.github.lamba92.openaqua.app.feature.navigation

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.LoggingScope
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.feature.about.AboutTab
import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.feature.settings.SettingsTab
import io.github.lamba92.openaqua.app.model.HostTab
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.mvi.viewModelLogger
import io.github.lamba92.openaqua.app.permission.DevicePermissionService
import io.github.lamba92.openaqua.app.permission.DevicePermissionState
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.repository.VirtualSensorFeederService
import io.github.lamba92.openaqua.model.device.DeviceId
import kotlinx.coroutines.Job
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
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class DeviceNavViewModel(
    private val dataCacheService: DeviceDataCacheService,
    private val appDataRepository: AppDataRepository,
    private val feederService: VirtualSensorFeederService,
    private val permissionService: DevicePermissionService,
    scopeProvider: CoroutineScopeProvider,
) : MVIViewModel<DeviceNavState, DeviceNavEvent, DeviceNavEffect> {
    override val viewModelScope: LoggingScope = scopeProvider.getScope()

    override val state: StateFlow<DeviceNavState>
        field = MutableStateFlow(DeviceNavState())

    override val effects: SharedFlow<DeviceNavEffect>
        field = MutableSharedFlow()

    init {
        initializeNavigationSelection()
        observeNavigationState()
        observeNavigationRailExpanded()
    }

    private fun observeNavigationState() {
        combine(
            permissionService.state,
            dataCacheService.state,
            feederService.state,
        ) { permState, deviceStates, feedErrors ->
            val newTargets = resolveNavigationTargets(permState, deviceStates)

            state.update { currentState ->
                val newSelectedItem = resolveSelectedItem(newTargets, currentState.selectedItem)
                currentState.copy(
                    targets = newTargets,
                    devicesWithFeedErrors = feedErrors.filterValues { it.isNotEmpty() }.keys,
                    selectedItem = newSelectedItem,
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun resolveNavigationTargets(
        permState: DevicePermissionState,
        deviceStates: List<DeviceState>,
    ): NavigationTargets =
        when (permState) {
            DevicePermissionState.Initializing -> {
                NavigationTargets.Loading
            }

            is DevicePermissionState.Denied -> {
                NavigationTargets.PermissionError(permState)
            }

            DevicePermissionState.Granted -> {
                val host =
                    deviceStates.filterIsInstance<DeviceState.Host>().firstOrNull()
                        ?: DeviceState.Host.Loading
                val aqua = deviceStates.filterIsInstance<DeviceState.Aqua>()
                NavigationTargets.Ready(host = host, devices = aqua)
            }
        }

    private fun resolveSelectedItem(
        newTargets: NavigationTargets,
        currentSelection: DeviceNavState.SelectedItem?,
    ): DeviceNavState.SelectedItem? =
        when (newTargets) {
            is NavigationTargets.PermissionError -> {
                when (currentSelection) {
                    null,
                    is DeviceNavState.SelectedItem.Host,
                    is DeviceNavState.SelectedItem.Device,
                    -> DeviceNavState.SelectedItem.PermissionError

                    else -> currentSelection
                }
            }

            is NavigationTargets.Ready -> {
                when (currentSelection) {
                    DeviceNavState.SelectedItem.PermissionError -> {
                        DeviceNavState.SelectedItem.Host()
                    }

                    is DeviceNavState.SelectedItem.Device -> {
                        if (newTargets.devices.any { it.device.id == currentSelection.deviceId }) {
                            currentSelection
                        } else {
                            null
                        }
                    }

                    else -> {
                        currentSelection
                    }
                }
            }

            NavigationTargets.Loading -> {
                if (currentSelection is DeviceNavState.SelectedItem.PermissionError) {
                    null
                } else {
                    currentSelection
                }
            }
        }

    private fun initializeNavigationSelection() {
        viewModelScope.launch {
            val timeoutResult =
                withTimeoutOrNull(5.seconds) {
                    val savedSelection =
                        appDataRepository.state
                            .filterIsInstance<AppDataRepository.State.Ready>()
                            .first()
                            .data
                            .navigationSelection
                            ?: return@withTimeoutOrNull Unit

                    val initialSelection =
                        when (savedSelection) {
                            is DeviceNavState.SelectedItem.Host,
                            DeviceNavState.SelectedItem.BugReport,
                            is DeviceNavState.SelectedItem.About,
                            is DeviceNavState.SelectedItem.Settings,
                            -> {
                                savedSelection
                            }

                            DeviceNavState.SelectedItem.PermissionError -> {
                                null
                            }

                            is DeviceNavState.SelectedItem.Device -> {
                                val aquaDevices: List<DeviceState.Aqua> =
                                    dataCacheService.state
                                        .map { it.filterIsInstance<DeviceState.Aqua>() }
                                        .filter { it.isNotEmpty() }
                                        .first { it.all { it is DeviceState.Aqua.Ready || it is DeviceState.Aqua.Error } }

                                if (aquaDevices.any { it.device.id == savedSelection.deviceId }) {
                                    savedSelection
                                } else {
                                    null
                                }
                            }
                        }

                    if (initialSelection != null) {
                        viewModelLogger.i { "Restored navigation selection: $initialSelection" }
                        state.update { currentState ->
                            if (currentState.selectedItem == null) {
                                currentState.copy(selectedItem = initialSelection)
                            } else {
                                currentState
                            }
                        }
                    }
                }
            if (timeoutResult == null) {
                viewModelLogger.i { "Navigation selection initialization timed out; defaulting to unselected" }
                appDataRepository.update { it.copy(navigationSelection = null) }
            }
        }
    }

    override fun onEvent(event: DeviceNavEvent): Job =
        viewModelScope.launch {
            when (event) {
                DeviceNavEvent.SelectHost -> selectHost()
                is DeviceNavEvent.SelectHostTab -> selectHostTab(event.tab)
                is DeviceNavEvent.SelectDevice -> selectDevice(event.deviceId)
                is DeviceNavEvent.SelectTab -> selectTab(event.tab)
                DeviceNavEvent.SelectPermissionError -> selectPermissionError()
                DeviceNavEvent.SelectBugReport -> selectBugReport()
                DeviceNavEvent.SelectAbout -> selectAbout()
                is DeviceNavEvent.SelectAboutTab -> selectAboutTab(event.tab)
                DeviceNavEvent.SelectSettings -> selectSettings()
                is DeviceNavEvent.SelectSettingsTab -> selectSettingsTab(event.tab)
                DeviceNavEvent.ToggleNavigationRailExpanded -> toggleNavigationRailExpanded()
            }
        }

    private fun selectHost(tab: HostTab? = null) {
        viewModelLogger.i { "Navigating to Host screen" }
        val finalState =
            state.updateAndGet { currentState ->
                val currentTab = (currentState.selectedItem as? DeviceNavState.SelectedItem.Host)?.selectedTab ?: HostTab.SENSORS
                currentState.copy(selectedItem = DeviceNavState.SelectedItem.Host(tab ?: currentTab))
            }
        finalState.selectedItem?.let { appDataRepository.setNavigationSelection(it) }
    }

    private fun selectHostTab(tab: HostTab) {
        viewModelLogger.i { "Selected tab '$tab' for host" }
        val finalState =
            state.updateAndGet { currentState ->
                currentState.copy(selectedItem = DeviceNavState.SelectedItem.Host(tab))
            }
        finalState.selectedItem?.let { appDataRepository.setNavigationSelection(it) }
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
        finalState.selectedItem?.let { appDataRepository.setNavigationSelection(it) }
    }

    private fun selectTab(tab: CommonDeviceTab) {
        viewModelLogger.i { "Selected tab '$tab' for device" }
        state.update { currentState ->
            when (val currentItem = currentState.selectedItem) {
                is DeviceNavState.SelectedItem.Device -> {
                    val updated = currentItem.copy(selectedTab = tab)
                    appDataRepository.setNavigationSelection(updated)
                    currentState.copy(selectedItem = updated)
                }

                null,
                is DeviceNavState.SelectedItem.Host,
                DeviceNavState.SelectedItem.PermissionError,
                DeviceNavState.SelectedItem.BugReport,
                is DeviceNavState.SelectedItem.About,
                is DeviceNavState.SelectedItem.Settings,
                -> {
                    currentState
                }
            }
        }
    }

    private fun selectPermissionError() {
        viewModelLogger.i { "Navigating to Permission Error screen" }
        state.update { currentState ->
            currentState.copy(selectedItem = DeviceNavState.SelectedItem.PermissionError)
        }
    }

    private fun selectBugReport() {
        viewModelLogger.i { "Navigating to Bug Report screen" }
        state.update { currentState ->
            currentState.copy(selectedItem = DeviceNavState.SelectedItem.BugReport)
        }
        appDataRepository.setNavigationSelection(DeviceNavState.SelectedItem.BugReport)
    }

    private fun selectAbout() {
        viewModelLogger.i { "Navigating to About screen" }
        state.update { currentState ->
            currentState.copy(selectedItem = DeviceNavState.SelectedItem.About(AboutTab.OPEN_AQUA))
        }
        appDataRepository.setNavigationSelection(DeviceNavState.SelectedItem.About(AboutTab.OPEN_AQUA))
    }

    private fun selectAboutTab(tab: AboutTab) {
        viewModelLogger.i { "Navigating to About tab '$tab'" }
        state.update { currentState ->
            currentState.copy(selectedItem = DeviceNavState.SelectedItem.About(tab))
        }
        appDataRepository.setNavigationSelection(DeviceNavState.SelectedItem.About(tab))
    }

    private fun selectSettings() {
        viewModelLogger.i { "Navigating to Settings screen" }
        state.update { currentState ->
            currentState.copy(selectedItem = DeviceNavState.SelectedItem.Settings(SettingsTab.GENERAL))
        }
        appDataRepository.setNavigationSelection(DeviceNavState.SelectedItem.Settings(SettingsTab.GENERAL))
    }

    private fun selectSettingsTab(tab: SettingsTab) {
        viewModelLogger.i { "Navigating to Settings tab '$tab'" }
        state.update { currentState ->
            currentState.copy(selectedItem = DeviceNavState.SelectedItem.Settings(tab))
        }
        appDataRepository.setNavigationSelection(DeviceNavState.SelectedItem.Settings(tab))
    }

    private fun toggleNavigationRailExpanded() {
        val newState = !state.value.isExpanded
        viewModelLogger.i { "Toggling navigation rail expanded: $newState" }
        state.update { currentState ->
            currentState.copy(isExpanded = newState)
        }
        appDataRepository.update { it.copy(navigationRailExpanded = newState) }
    }

    private fun observeNavigationRailExpanded() {
        appDataRepository.state
            .filterIsInstance<AppDataRepository.State.Ready>()
            .map { it.data.navigationRailExpanded }
            .onEach { expanded ->
                state.update { it.copy(isExpanded = expanded) }
            }.launchIn(viewModelScope)
    }
}
