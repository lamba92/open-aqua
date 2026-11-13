package io.github.lamba92.openaqua.app.feature.host.fans

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.LoggingScope
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.feature.device.common.ChannelName
import io.github.lamba92.openaqua.app.feature.device.common.toChannelName
import io.github.lamba92.openaqua.app.model.DeviceStoredData
import io.github.lamba92.openaqua.app.model.HostFanConfig
import io.github.lamba92.openaqua.app.model.HostFanStoredData
import io.github.lamba92.openaqua.app.model.VirtualSensorFeedSource
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.device.CurveMath
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.SensorTelemetry
import io.github.lamba92.openaqua.model.host.HostFanId
import io.github.lamba92.openaqua.model.host.HostFanTelemetry
import io.github.lamba92.openaqua.model.host.HostSensorTelemetry
import io.github.lamba92.openaqua.model.host.isControllable
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

class HostFansViewModel(
    private val cacheService: DeviceDataCacheService,
    private val appDataRepository: AppDataRepository,
    scopeProvider: CoroutineScopeProvider,
) : MVIViewModel<HostFansUiState, HostFansUiEvent, HostFansUiEffect> {
    override val viewModelScope: LoggingScope = scopeProvider.getScope()

    override val state: StateFlow<HostFansUiState>
        field = MutableStateFlow<HostFansUiState>(HostFansUiState.Loading)

    override val effects: SharedFlow<HostFansUiEffect>
        field = MutableSharedFlow()

    private data class Preferences(
        val hostFans: Map<HostFanId, HostFanStoredData>,
        val hideDisconnectedFans: Boolean,
        val hideDisconnectedSensors: Boolean,
        val devices: Map<DeviceId, DeviceStoredData>,
    )

    init {
        val preferencesFlow =
            appDataRepository.state
                .filterIsInstance<AppDataRepository.State.Ready>()
                .map { ready ->
                    Preferences(
                        hostFans = ready.data.hostFans,
                        hideDisconnectedFans = ready.data.hideDisconnectedFans,
                        hideDisconnectedSensors = ready.data.hideDisconnectedSensorsInFans,
                        devices = ready.data.devices,
                    )
                }.distinctUntilChanged()

        combine(
            cacheService.getHostStateFlow(),
            preferencesFlow,
            cacheService.state,
        ) { hostState, preferences, allDeviceStates ->
            state.update { current ->
                when (hostState) {
                    is DeviceState.Host.Loading,
                    is DeviceState.Host.Connecting,
                    is DeviceState.Host.Error,
                    -> {
                        HostFansUiState.Loading
                    }

                    is DeviceState.Host.Ready -> {
                        val fans =
                            hostState.info.devices.flatMap { device ->
                                device.fans.map { fanDesc ->
                                    val telemetry =
                                        hostState.telemetry.fans.firstOrNull { it.id == fanDesc.id }
                                            ?: HostFanTelemetry.Disconnected(fanDesc.id)
                                    val stored = preferences.hostFans[fanDesc.id] ?: HostFanStoredData.DEFAULT
                                    HostFanItem(
                                        id = fanDesc.id,
                                        defaultName = fanDesc.name,
                                        customName = stored.name,
                                        telemetry = telemetry,
                                        config = stored.config,
                                        deviceName = device.name,
                                        isControllable = fanDesc.isControllable,
                                    )
                                }
                            }

                        val hostSensors =
                            hostState.info.devices.flatMap { device ->
                                device.sensors.map { sensorDesc ->
                                    val temp =
                                        (
                                            hostState.telemetry.sensors.firstOrNull { it.id == sensorDesc.id }
                                                as? HostSensorTelemetry.Connected
                                        )?.tempCelsius
                                    AvailableSensorItem(
                                        source = VirtualSensorFeedSource.Host(sensorDesc.id),
                                        displayName = "${device.name} - ${sensorDesc.name}",
                                        currentTempCelsius = temp,
                                    )
                                }
                            }

                        val deviceSensors =
                            allDeviceStates
                                .filterIsInstance<DeviceState.Aqua.Ready>()
                                .flatMap { aquaReady ->
                                    val deviceName =
                                        aquaReady.device.name
                                            .removePrefix("Aquacomputer")
                                            .trim()
                                    val deviceStored = preferences.devices[aquaReady.device.id] ?: DeviceStoredData.DEFAULT
                                    aquaReady.telemetry.sensors.mapNotNull { sensorTel ->
                                        val temp = (sensorTel as? SensorTelemetry.Connected)?.tempCelsius
                                        val customName = deviceStored.sensors[sensorTel.channel]?.name
                                        val defaultName = "Sensor ${sensorTel.channel + 1}"
                                        val label =
                                            when (customName) {
                                                is ChannelName.Custom -> customName.value
                                                else -> defaultName
                                            }
                                        AvailableSensorItem(
                                            source = VirtualSensorFeedSource.Device(aquaReady.device.id, sensorTel.channel),
                                            displayName = "$deviceName - $label",
                                            currentTempCelsius = temp,
                                        )
                                    }
                                }

                        val availableSensors = hostSensors + deviceSensors

                        if (current is HostFansUiState.Ready) {
                            val selectedFanId =
                                if (fans.any { it.id == current.selectedFanId }) {
                                    current.selectedFanId
                                } else {
                                    fans.firstOrNull()?.id
                                }
                            current.copy(
                                fans = fans,
                                selectedFanId = selectedFanId,
                                availableSensors = availableSensors,
                                hideDisconnectedFans = preferences.hideDisconnectedFans,
                                hideDisconnectedSensors = preferences.hideDisconnectedSensors,
                            )
                        } else {
                            HostFansUiState.Ready(
                                fans = fans,
                                selectedFanId = fans.firstOrNull()?.id,
                                availableSensors = availableSensors,
                                hideDisconnectedFans = preferences.hideDisconnectedFans,
                                hideDisconnectedSensors = preferences.hideDisconnectedSensors,
                            )
                        }
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    override fun onEvent(event: HostFansUiEvent): Job =
        viewModelScope.launch {
            when (event) {
                is HostFansUiEvent.SelectFan -> {
                    state.update { current ->
                        if (current is HostFansUiState.Ready) {
                            current.copy(selectedFanId = event.id)
                        } else {
                            current
                        }
                    }
                }

                is HostFansUiEvent.RenameFan -> {
                    val name = event.name?.toChannelName() ?: ChannelName.Default
                    appDataRepository.setHostFanName(event.id, name)
                }

                is HostFansUiEvent.UpdateDraftConfig -> {
                    state.update { current ->
                        if (current is HostFansUiState.Ready) {
                            val fan = current.fans.firstOrNull { it.id == event.id }
                            if (fan == null || !fan.isControllable) {
                                current
                            } else {
                                val sanitizedConfig =
                                    when (val cfg = event.config) {
                                        is HostFanConfig.AutoCurve -> {
                                            if (cfg.tMin >= cfg.tMax) {
                                                val (min, max) = CurveMath.adjustTMin(cfg.tMin, cfg.tMax, maxAllowed = 95.0)
                                                cfg.copy(tMin = min, tMax = max)
                                            } else {
                                                cfg
                                            }
                                        }

                                        else -> {
                                            cfg
                                        }
                                    }
                                current.copy(fanDrafts = current.fanDrafts + (event.id to sanitizedConfig))
                            }
                        } else {
                            current
                        }
                    }
                }

                is HostFansUiEvent.DiscardFanDraft -> {
                    state.update { current ->
                        if (current is HostFansUiState.Ready) {
                            current.copy(fanDrafts = current.fanDrafts - event.id)
                        } else {
                            current
                        }
                    }
                }

                HostFansUiEvent.DiscardAllDrafts -> {
                    state.update { current ->
                        if (current is HostFansUiState.Ready) {
                            current.copy(fanDrafts = emptyMap())
                        } else {
                            current
                        }
                    }
                }

                HostFansUiEvent.ApplyChanges -> {
                    val current = state.value as? HostFansUiState.Ready ?: return@launch
                    val controllableIds =
                        current.fans
                            .filter { it.isControllable }
                            .map { it.id }
                            .toSet()
                    val draftsToApply = current.fanDrafts.filterKeys { it in controllableIds }
                    if (draftsToApply.isNotEmpty()) {
                        appDataRepository.setHostFanConfigs(draftsToApply)
                        state.update {
                            if (it is HostFansUiState.Ready) it.copy(fanDrafts = emptyMap()) else it
                        }
                    }
                }

                HostFansUiEvent.ToggleHideDisconnectedFans -> {
                    val currentHide = (state.value as? HostFansUiState.Ready)?.hideDisconnectedFans ?: false
                    appDataRepository.setHideDisconnectedFans(!currentHide)
                }

                HostFansUiEvent.ToggleHideDisconnectedSensors -> {
                    val currentHide = (state.value as? HostFansUiState.Ready)?.hideDisconnectedSensors ?: false
                    appDataRepository.setHideDisconnectedSensorsInFans(!currentHide)
                }
            }
        }
}
