package io.github.lamba92.openaqua.app.feature.host

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.LoggingScope
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.model.SensorTargetType
import io.github.lamba92.openaqua.app.model.withHostSensorType
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.mvi.viewModelLogger
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.model.host.HostSensorId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HostViewModel(
    dataCacheService: DeviceDataCacheService,
    private val appDataRepository: AppDataRepository,
    scopeProvider: CoroutineScopeProvider,
) : MVIViewModel<HostState, HostEvent, HostEffect> {
    override val viewModelScope: LoggingScope = scopeProvider.getScope()

    override val state: StateFlow<HostState>
        field = MutableStateFlow(HostState())

    override val effects: SharedFlow<HostEffect>
        field = MutableSharedFlow()

    init {
        val preferencesFlow =
            appDataRepository.state
                .filterIsInstance<AppDataRepository.State.Ready>()
                .map { it.data.sensorTypes }
                .distinctUntilChanged()

        combine(
            dataCacheService.getHostStateFlow(),
            preferencesFlow,
        ) { hostState, sensorTypes ->
            state.update {
                it.copy(
                    hostState = hostState,
                    sensorTypes = sensorTypes,
                )
            }
        }.launchIn(viewModelScope)
    }

    override fun onEvent(event: HostEvent): Job =
        viewModelScope.launch {
            when (event) {
                is HostEvent.SetSensorTargetType -> setSensorTargetType(event.sensorId, event.targetType)
            }
        }

    private fun setSensorTargetType(
        sensorId: HostSensorId,
        targetType: SensorTargetType,
    ) {
        viewModelLogger.i { "Configured host sensor '$sensorId' as target type '$targetType'" }
        appDataRepository.update { current ->
            current.copy(
                sensorTypes = current.sensorTypes.withHostSensorType(sensorId, targetType),
            )
        }
    }
}
