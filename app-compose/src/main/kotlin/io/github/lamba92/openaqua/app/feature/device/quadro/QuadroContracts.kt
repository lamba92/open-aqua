package io.github.lamba92.openaqua.app.feature.device.quadro

import io.github.lamba92.openaqua.app.feature.device.common.FanItem
import io.github.lamba92.openaqua.app.feature.device.common.SensorItem
import io.github.lamba92.openaqua.app.repository.DeviceErrorType
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.DetectedHardwareDevice

sealed interface QuadroState {
    data object Loading : QuadroState

    data class Error(
        val device: DetectedHardwareDevice,
        val errorType: DeviceErrorType,
    ) : QuadroState

    data class Ready(
        val deviceState: DeviceState.Quadro,
        val fans: List<FanItem>,
        val selectedFanChannel: Int = 0,
        val sensors: List<SensorItem>,
        val selectedSensorChannel: Int = 0,
    ) : QuadroState
}

sealed interface QuadroEvent {
    data class SelectFan(
        val channel: Int,
    ) : QuadroEvent

    data class SelectSensor(
        val channel: Int,
    ) : QuadroEvent

    data class RenameFan(
        val channel: Int,
        val name: String?,
    ) : QuadroEvent

    data class RenameSensor(
        val channel: Int,
        val name: String?,
    ) : QuadroEvent
}

sealed interface QuadroEffect
