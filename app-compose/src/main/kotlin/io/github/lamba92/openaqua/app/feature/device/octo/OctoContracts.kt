package io.github.lamba92.openaqua.app.feature.device.octo

import io.github.lamba92.openaqua.app.feature.device.common.FanItem
import io.github.lamba92.openaqua.app.feature.device.common.SensorItem
import io.github.lamba92.openaqua.app.repository.DeviceErrorType
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.DetectedHardwareDevice

sealed interface OctoState {
    data object Loading : OctoState

    data class Error(
        val device: DetectedHardwareDevice,
        val errorType: DeviceErrorType,
    ) : OctoState

    data class Ready(
        val deviceState: DeviceState.Octo,
        val fans: List<FanItem>,
        val selectedFanChannel: Int = 0,
        val sensors: List<SensorItem>,
        val selectedSensorChannel: Int = 0,
    ) : OctoState
}

sealed interface OctoEvent {
    data class SelectFan(
        val channel: Int,
    ) : OctoEvent

    data class SelectSensor(
        val channel: Int,
    ) : OctoEvent

    data class RenameFan(
        val channel: Int,
        val name: String?,
    ) : OctoEvent

    data class RenameSensor(
        val channel: Int,
        val name: String?,
    ) : OctoEvent
}

sealed interface OctoEffect
