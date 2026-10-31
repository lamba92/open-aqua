package io.github.lamba92.openaqua.app.feature.device

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.LoggingScope
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.mvi.viewModelLogger
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.repository.VirtualSensorFeederService
import io.github.lamba92.openaqua.app.ui.components.FeederStatus
import io.github.lamba92.openaqua.model.device.DeviceId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DeviceViewModel(
    cacheService: DeviceDataCacheService,
    feederService: VirtualSensorFeederService,
    appDataRepository: AppDataRepository,
    scopeProvider: CoroutineScopeProvider,
    deviceId: DeviceId,
) : MVIViewModel<DeviceUiState, DeviceUiEvent, DeviceUiEffect> {
    override val viewModelScope: LoggingScope = scopeProvider.getScope()

    override val state: StateFlow<DeviceUiState>
        field = MutableStateFlow<DeviceUiState>(DeviceUiState.Loading)

    override val effects: SharedFlow<DeviceUiEffect>
        field = MutableSharedFlow<DeviceUiEffect>()

    init {
        val hasVirtualFeedsFlow =
            appDataRepository
                .state
                .filterIsInstance<AppDataRepository.State.Ready>()
                .map { ready ->
                    ready
                        .data
                        .devices[deviceId]
                        ?.sensors
                        ?.any { (_, s) -> s.virtualFeedSource != null } == true
                }

        val deviceErrorsFlow =
            feederService
                .feedErrorsState
                .map { it[deviceId].orEmpty() }

        combine(
            cacheService.getAquaDeviceStateFlow(deviceId),
            deviceErrorsFlow,
            hasVirtualFeedsFlow,
        ) { deviceState, errors, hasVirtualFeeds ->
            when (deviceState) {
                is DeviceState.Aqua.Connecting -> DeviceUiState.Loading
                is DeviceState.Aqua.Error ->
                    DeviceUiState.Error(
                        device = deviceState.device,
                        errorType = deviceState.errorType,
                    )

                is DeviceState.Aqua.Ready -> {
                    val status =
                        when {
                            errors.isNotEmpty() -> FeederStatus.ERROR
                            hasVirtualFeeds -> FeederStatus.OPERATIONAL
                            else -> FeederStatus.NONE
                        }
                    DeviceUiState.Ready(
                        deviceState = deviceState,
                        status = status,
                    )
                }
            }
        }.onEach { state.value = it }
            .launchIn(viewModelScope)

        cacheService
            .getAquaDeviceStateFlow(deviceId)
            .distinctUntilChangedBy { it::class }
            .onEach { deviceState ->
                when (deviceState) {
                    is DeviceState.Aqua.Connecting -> viewModelLogger.i { "Device '$deviceId' is connecting..." }
                    is DeviceState.Aqua.Error -> viewModelLogger.e { "Device '$deviceId' error: ${deviceState.errorType}" }
                    is DeviceState.Aqua.Ready -> viewModelLogger.i { "Device '$deviceId' is ready" }
                }
            }.launchIn(viewModelScope)
    }

    override fun onEvent(event: DeviceUiEvent): Job =
        viewModelScope.launch {
            // Device-level events if needed in the future
        }
}
