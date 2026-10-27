package io.github.lamba92.openaqua.app.feature.device.fans

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.feature.device.common.toPwmFanItems
import io.github.lamba92.openaqua.app.feature.device.common.toSensorItems
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.DeviceId
import io.github.lamba92.openaqua.model.FlashConfigRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FansViewModel(
    private val cacheService: DeviceDataCacheService,
    private val appDataRepository: AppDataRepository,
    scopeProvider: CoroutineScopeProvider,
    private val deviceId: DeviceId,
) : MVIViewModel<FansUiState, FansUiEvent, FansUiEffect> {
    override val viewModelScope: CoroutineScope = scopeProvider.getScope()

    override val state: StateFlow<FansUiState>
        field = MutableStateFlow<FansUiState>(FansUiState.Loading)

    override val effects: SharedFlow<FansUiEffect>
        field = MutableSharedFlow<FansUiEffect>()

    init {
        combine(
            cacheService.getDeviceStateFlow(deviceId),
            appDataRepository.getDeviceDataFlow(deviceId),
        ) { deviceState, storedData ->
            state.update { current ->
                when (deviceState) {
                    is DeviceState.Connecting, is DeviceState.Error -> FansUiState.Loading
                    is DeviceState.Ready -> {
                        val fans = deviceState.telemetry.fans.toPwmFanItems(deviceState.config.fans, storedData)
                        val sensors = deviceState.telemetry.sensors.toSensorItems(deviceState.config.sensorOffsets, storedData)

                        if (current is FansUiState.Ready) {
                            current.copy(
                                fans = fans,
                                sensors = sensors,
                            )
                        } else {
                            FansUiState.Ready(
                                fans = fans,
                                selectedFanChannel = 0,
                                sensors = sensors,
                                fanDrafts = emptyMap(),
                            )
                        }
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    override fun onEvent(event: FansUiEvent) {
        when (event) {
            is FansUiEvent.SelectFan ->
                state.update { current ->
                    if (current is FansUiState.Ready) current.copy(selectedFanChannel = event.channel) else current
                }

            is FansUiEvent.RenameFan ->
                appDataRepository.setFanName(deviceId = deviceId, channel = event.channel, name = event.name)

            is CurveStudioEvent ->
                updateSelectedFanDraft { currentDraft, channel ->
                    when (event) {
                        is CurveStudioEvent.ChangeMode ->
                            when (event.mode) {
                                CurveMode.FIXED ->
                                    currentDraft as? FanDraft.Fixed ?: FanDraft.Fixed(channel)

                                CurveMode.AUTO_CURVE ->
                                    currentDraft as? FanDraft.AutoCurve ?: FanDraft.AutoCurve(channel)

                                CurveMode.TARGET_TEMP ->
                                    currentDraft as? FanDraft.TargetTemp ?: FanDraft.TargetTemp(channel)

                                CurveMode.COPY_FAN ->
                                    currentDraft as? FanDraft.Copy ?: FanDraft.Copy(channel)
                            }

                        is CurveStudioEvent.ChangeFixedPwm ->
                            if (currentDraft is FanDraft.Fixed) {
                                currentDraft.copy(powerPercent = event.pwm)
                            } else {
                                FanDraft.Fixed(channel, event.pwm)
                            }

                        is CurveStudioEvent.ChangeSensorIndex ->
                            when (currentDraft) {
                                is FanDraft.AutoCurve -> currentDraft.copy(sensorIndex = event.sensorIndex)
                                is FanDraft.TargetTemp -> currentDraft.copy(sensorIndex = event.sensorIndex)
                                else -> currentDraft
                            }

                        is CurveStudioEvent.ChangeTargetTemp ->
                            if (currentDraft is FanDraft.TargetTemp) {
                                currentDraft.copy(targetTempCelsius = event.targetTemp)
                            } else {
                                currentDraft
                            }

                        is CurveStudioEvent.ChangeTMin ->
                            if (currentDraft is FanDraft.AutoCurve) {
                                val newTMin = event.tMin
                                val newTMax = if (currentDraft.tMax <= newTMin) (newTMin + 1.0).coerceAtMost(85.0) else currentDraft.tMax
                                currentDraft.copy(tMin = newTMin, tMax = newTMax)
                            } else {
                                currentDraft
                            }

                        is CurveStudioEvent.ChangeTMax ->
                            if (currentDraft is FanDraft.AutoCurve) {
                                val newTMax = event.tMax
                                val newTMin = if (currentDraft.tMin >= newTMax) (newTMax - 1.0).coerceAtLeast(10.0) else currentDraft.tMin
                                currentDraft.copy(tMin = newTMin, tMax = newTMax)
                            } else {
                                currentDraft
                            }

                        is CurveStudioEvent.ChangePMin ->
                            if (currentDraft is FanDraft.AutoCurve) currentDraft.copy(pMin = event.pMin) else currentDraft

                        is CurveStudioEvent.ChangePMax ->
                            if (currentDraft is FanDraft.AutoCurve) currentDraft.copy(pMax = event.pMax) else currentDraft

                        is CurveStudioEvent.ChangeShape ->
                            if (currentDraft is FanDraft.AutoCurve) currentDraft.copy(shape = event.shape) else currentDraft

                        is CurveStudioEvent.ChangeCopySource ->
                            if (currentDraft is FanDraft.Copy) {
                                currentDraft.copy(
                                    sourceChannel = event.sourceChannel,
                                )
                            } else {
                                FanDraft.Copy(channel, event.sourceChannel)
                            }
                    }
                }

            is FansUiEvent.DiscardFanDraft ->
                state.update { current ->
                    if (current is FansUiState.Ready) {
                        current.copy(fanDrafts = current.fanDrafts - current.selectedFanChannel)
                    } else {
                        current
                    }
                }

            is FansUiEvent.RequestFlash ->
                state.update { current ->
                    if (current is FansUiState.Ready) current.copy(flashDialogState = FlashDialogState.Shown) else current
                }

            is FansUiEvent.DismissFlashDialog ->
                state.update { current ->
                    if (current is FansUiState.Ready && current.flashDialogState !is FlashDialogState.Flashing) {
                        current.copy(flashDialogState = FlashDialogState.NotShown)
                    } else {
                        current
                    }
                }

            is FansUiEvent.ConfirmFlash -> {
                val currentState = state.value as? FansUiState.Ready ?: return
                if (currentState.flashDialogState is FlashDialogState.Flashing) return
                val pending = currentState.pendingFans
                if (pending.isEmpty()) {
                    state.update { if (it is FansUiState.Ready) it.copy(flashDialogState = FlashDialogState.NotShown) else it }
                    return
                }

                val updates = pending.associate { it.first.channel to it.second }
                state.update { current ->
                    if (current is FansUiState.Ready) current.copy(flashDialogState = FlashDialogState.Flashing) else current
                }

                viewModelScope.launch {
                    val initialConfig = (cacheService.getDeviceStateFlow(deviceId).first() as? DeviceState.Ready)?.config
                    cacheService.flashConfig(
                        deviceId = deviceId,
                        config = FlashConfigRequest(fans = updates),
                    )

                    // Await next config change from cacheService where updates are applied
                    cacheService
                        .getReadyDeviceStateFlow(deviceId)
                        .first { readyState -> readyState.config != initialConfig }
                    state.update { current ->
                        when (current) {
                            is FansUiState.Ready ->
                                current.copy(
                                    fanDrafts = current.fanDrafts - updates.keys,
                                    flashDialogState = FlashDialogState.NotShown,
                                )

                            else -> current
                        }
                    }
                }
            }
        }
    }

    private fun updateSelectedFanDraft(block: (FanDraft, channel: Int) -> FanDraft) {
        state.update { current ->
            if (current is FansUiState.Ready) {
                val channel = current.selectedFanChannel
                val fan = current.fans.firstOrNull { it.channel == channel }
                val defaultSensor = current.sensors.firstOrNull()?.channel ?: 0
                val existing =
                    current.fanDrafts[channel] ?: fan?.config.toInitialDraft(
                        channel = channel,
                        defaultSensorIndex = defaultSensor,
                    )
                val updated = block(existing, channel)
                current.copy(fanDrafts = current.fanDrafts + (channel to updated))
            } else {
                current
            }
        }
    }
}
