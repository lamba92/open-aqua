package io.github.lamba92.openaqua.app.feature.device.sensors

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.LoggingScope
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.feature.device.common.ChannelName
import io.github.lamba92.openaqua.app.feature.device.common.toSensorItems
import io.github.lamba92.openaqua.app.model.DeviceStoredData
import io.github.lamba92.openaqua.app.model.SensorTypesStoredData
import io.github.lamba92.openaqua.app.model.withAquaSensorType
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.mvi.viewModelLogger
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.repository.VirtualSensorFeederService
import io.github.lamba92.openaqua.app.repository.resolveFeedSourceValue
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.FlashConfigRequest
import io.github.lamba92.openaqua.model.device.SensorChannelKind
import io.github.lamba92.openaqua.model.device.SensorTelemetry
import io.github.lamba92.openaqua.model.host.HostSensorTelemetry
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

class SensorsViewModel(
    private val cacheService: DeviceDataCacheService,
    private val appDataRepository: AppDataRepository,
    feederService: VirtualSensorFeederService,
    scopeProvider: CoroutineScopeProvider,
    private val deviceId: DeviceId,
) : MVIViewModel<SensorsUiState, SensorsUiEvent, SensorsUiEffect> {
    override val viewModelScope: LoggingScope = scopeProvider.getScope()

    override val state: StateFlow<SensorsUiState>
        field = MutableStateFlow<SensorsUiState>(SensorsUiState.Loading)

    override val effects: SharedFlow<SensorsUiEffect>
        field = MutableSharedFlow<SensorsUiEffect>()

    private data class CombinedPreferences(
        val storedData: DeviceStoredData,
        val allDevicesStored: Map<DeviceId, DeviceStoredData>,
        val sensorTypes: SensorTypesStoredData,
        val hideDisconnectedSensors: Boolean,
        val hideUnconfiguredVirtualSensors: Boolean,
    )

    init {
        val preferencesFlow =
            appDataRepository.state
                .filterIsInstance<AppDataRepository.State.Ready>()
                .map { ready ->
                    CombinedPreferences(
                        storedData = ready.data.devices[deviceId] ?: DeviceStoredData(),
                        allDevicesStored = ready.data.devices,
                        sensorTypes = ready.data.sensorTypes,
                        hideDisconnectedSensors = ready.data.hideDisconnectedSensors,
                        hideUnconfiguredVirtualSensors = ready.data.hideUnconfiguredVirtualSensors,
                    )
                }.distinctUntilChanged()

        val deviceFeedErrorsFlow =
            feederService.feedErrorsState
                .map { it[deviceId].orEmpty() }

        combine(
            cacheService.getAquaDeviceStateFlow(deviceId),
            preferencesFlow,
            deviceFeedErrorsFlow,
            cacheService.state,
        ) { deviceState, preferences, feedErrors, allDevices ->
            state.update { current ->
                when (deviceState) {
                    is DeviceState.Aqua.Connecting, is DeviceState.Aqua.Error -> SensorsUiState.Loading
                    is DeviceState.Aqua.Ready -> {
                        val sensors =
                            deviceState.telemetry.sensors.toSensorItems(
                                offsets = deviceState.config.sensorOffsets,
                                storedData = preferences.storedData,
                                feedErrors = feedErrors,
                                resolveFeedValue = { source -> resolveFeedSourceValue(source, allDevices) },
                                deviceId = deviceId,
                                sensorTypes = preferences.sensorTypes,
                            )

                        val readyHost = allDevices.filterIsInstance<DeviceState.Host.Ready>().firstOrNull()
                        val availableHostSensors =
                            readyHost
                                ?.let { host ->
                                    val telemetryMap =
                                        host.telemetry.sensors.associate {
                                            it.id to (it as? HostSensorTelemetry.Connected)?.tempCelsius
                                        }
                                    host.info.devices.flatMap { dev ->
                                        dev.sensors.map { sensor ->
                                            HostSensorOption(
                                                id = sensor.id,
                                                name = "${dev.name} - ${sensor.name}",
                                                liveTemp = telemetryMap[sensor.id],
                                            )
                                        }
                                    }
                                }.orEmpty()

                        val availableAquaDevices =
                            allDevices.filterIsInstance<DeviceState.Aqua.Ready>().mapNotNull { aqua ->
                                val devStored = preferences.allDevicesStored[aqua.device.id]
                                val devName =
                                    when (val name = devStored?.name ?: ChannelName.Default) {
                                        ChannelName.Default -> aqua.device.name
                                        is ChannelName.Custom -> name.value
                                    }
                                val physicalSensors =
                                    aqua.telemetry.sensors
                                        .filter { it.kind != SensorChannelKind.VIRTUAL }
                                        .map { s ->
                                            val sensorName = devStored?.sensors?.get(s.channel)?.name ?: ChannelName.Default
                                            DeviceSensorOption(
                                                channel = s.channel,
                                                kind = s.kind,
                                                name = sensorName,
                                                liveTemp = (s as? SensorTelemetry.Connected)?.tempCelsius,
                                            )
                                        }
                                if (physicalSensors.isNotEmpty()) {
                                    AquaDeviceOption(
                                        deviceId = aqua.device.id,
                                        name = devName,
                                        sensors = physicalSensors,
                                    )
                                } else {
                                    null
                                }
                            }

                        if (current is SensorsUiState.Ready) {
                            current.copy(
                                sensors = sensors,
                                hideDisconnectedSensors = preferences.hideDisconnectedSensors,
                                hideUnconfiguredVirtualSensors = preferences.hideUnconfiguredVirtualSensors,
                                availableHostSensors = availableHostSensors,
                                availableAquaDevices = availableAquaDevices,
                            )
                        } else {
                            val initialChannel = sensors.firstOrNull { it.feedError != null }?.channel ?: 0
                            SensorsUiState.Ready(
                                sensors = sensors,
                                selectedSensorChannel = initialChannel,
                                hideDisconnectedSensors = preferences.hideDisconnectedSensors,
                                hideUnconfiguredVirtualSensors = preferences.hideUnconfiguredVirtualSensors,
                                availableHostSensors = availableHostSensors,
                                availableAquaDevices = availableAquaDevices,
                            )
                        }
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    override fun onEvent(event: SensorsUiEvent): Job =
        viewModelScope.launch {
            when (event) {
                is SensorsUiEvent.SelectSensor ->
                    state.update { current ->
                        if (current is SensorsUiState.Ready) current.copy(selectedSensorChannel = event.channel) else current
                    }

                is SensorsUiEvent.StartRenamingSensor ->
                    state.update { current ->
                        if (current is SensorsUiState.Ready) current.copy(renamingChannel = event.channel) else current
                    }

                is SensorsUiEvent.DismissRenameDialog ->
                    state.update { current ->
                        if (current is SensorsUiState.Ready) current.copy(renamingChannel = null) else current
                    }

                is SensorsUiEvent.RenameSensor -> {
                    viewModelLogger.i { "Renamed sensor channel ${event.channel} on device '$deviceId' to '${event.name}'" }
                    appDataRepository.setSensorName(deviceId = deviceId, channel = event.channel, name = event.name)
                    state.update { current ->
                        if (current is SensorsUiState.Ready) current.copy(renamingChannel = null) else current
                    }
                }

                SensorsUiEvent.ToggleShowAllSensors -> {
                    val currentState = state.value as? SensorsUiState.Ready ?: return@launch
                    val isHidingAny = currentState.hideDisconnectedSensors || currentState.hideUnconfiguredVirtualSensors
                    viewModelLogger.i { "Toggled sensor visibility on device '$deviceId' (hideDisconnected=${!isHidingAny})" }
                    if (isHidingAny) {
                        appDataRepository.setHideDisconnectedSensors(false)
                        appDataRepository.setHideUnconfiguredVirtualSensors(false)
                    } else {
                        appDataRepository.setHideDisconnectedSensors(true)
                        appDataRepository.setHideUnconfiguredVirtualSensors(true)
                    }
                }

                is SensorsUiEvent.SetSensorTargetType -> {
                    viewModelLogger.i {
                        "Configured sensor channel ${event.channel} on device '$deviceId' as target type '${event.targetType}'"
                    }
                    appDataRepository.update { current ->
                        current.copy(
                            sensorTypes =
                                current.sensorTypes.withAquaSensorType(
                                    deviceId = deviceId,
                                    sensorChannel = event.channel,
                                    type = event.targetType,
                                ),
                        )
                    }
                }

                is SensorsUiEvent.UpdateVirtualFeedDraft ->
                    state.update { current ->
                        if (current is SensorsUiState.Ready) {
                            current.copy(virtualFeedDrafts = current.virtualFeedDrafts + (event.channel to event.draft))
                        } else {
                            current
                        }
                    }

                is SensorsUiEvent.DiscardVirtualFeedDraft ->
                    state.update { current ->
                        if (current is SensorsUiState.Ready) {
                            current.copy(virtualFeedDrafts = current.virtualFeedDrafts - event.channel)
                        } else {
                            current
                        }
                    }

                is SensorsUiEvent.ApplyVirtualFeed -> {
                    val currentState = state.value as? SensorsUiState.Ready ?: return@launch
                    val draft = currentState.virtualFeedDrafts[event.channel] ?: return@launch
                    val source = draft.toFeedSource()
                    viewModelLogger.i { "Applying virtual feed source for channel ${event.channel} on device '$deviceId' to $source" }
                    state.update { current ->
                        if (current is SensorsUiState.Ready) current.copy(isApplyingVirtualFeed = true) else current
                    }
                    appDataRepository.setSensorVirtualFeed(
                        deviceId = deviceId,
                        channel = event.channel,
                        feedSource = source,
                    )
                    state.update { current ->
                        if (current is SensorsUiState.Ready) {
                            current.copy(
                                isApplyingVirtualFeed = false,
                                virtualFeedDrafts = current.virtualFeedDrafts - event.channel,
                            )
                        } else {
                            current
                        }
                    }
                }

                is SensorsUiEvent.UpdateOffsetDraft ->
                    state.update { current ->
                        if (current is SensorsUiState.Ready) {
                            current.copy(offsetDrafts = current.offsetDrafts + (event.channel to event.offset))
                        } else {
                            current
                        }
                    }

                is SensorsUiEvent.DiscardOffsetDraft ->
                    state.update { current ->
                        if (current is SensorsUiState.Ready) {
                            current.copy(offsetDrafts = current.offsetDrafts - event.channel)
                        } else {
                            current
                        }
                    }

                is SensorsUiEvent.FlashOffset -> {
                    val currentState = state.value as? SensorsUiState.Ready ?: return@launch
                    val offset = currentState.offsetDrafts[event.channel] ?: return@launch
                    viewModelLogger.i { "Flashing sensor offset for channel ${event.channel} on device '$deviceId' to $offset °C..." }
                    state.update { current ->
                        if (current is SensorsUiState.Ready) current.copy(isFlashingOffset = true) else current
                    }
                    try {
                        cacheService.flashConfig(
                            deviceId = deviceId,
                            config = FlashConfigRequest(sensorOffsets = mapOf(event.channel to offset)),
                        )
                    } catch (e: Exception) {
                        viewModelLogger.e(e) { "Failed to flash sensor offset: ${e.message}" }
                    } finally {
                        state.update { current ->
                            if (current is SensorsUiState.Ready) {
                                current.copy(
                                    isFlashingOffset = false,
                                    offsetDrafts = current.offsetDrafts - event.channel,
                                )
                            } else {
                                current
                            }
                        }
                    }
                }
            }
        }
}
