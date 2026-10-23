package io.github.lamba92.openaqua.app.feature.device.d5next

import io.github.lamba92.openaqua.app.feature.device.common.FanItem
import io.github.lamba92.openaqua.app.feature.device.common.SensorItem
import io.github.lamba92.openaqua.app.repository.DeviceErrorType
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.DetectedHardwareDevice

enum class D5NextCoolingTarget {
    PUMP,
    FAN,
}

sealed interface D5NextState {
    data object Loading : D5NextState

    data class Error(
        val device: DetectedHardwareDevice,
        val errorType: DeviceErrorType,
    ) : D5NextState

    data class Ready(
        val deviceState: DeviceState.D5Next,
        val pump: FanItem,
        val fan: FanItem,
        val selectedCoolingTarget: D5NextCoolingTarget = D5NextCoolingTarget.PUMP,
        val sensors: List<SensorItem>,
        val selectedSensorChannel: Int = 0,
    ) : D5NextState
}

sealed interface D5NextEvent {
    data class SelectCoolingTarget(
        val target: D5NextCoolingTarget,
    ) : D5NextEvent

    data class SelectSensor(
        val channel: Int,
    ) : D5NextEvent
}

sealed interface D5NextEffect
