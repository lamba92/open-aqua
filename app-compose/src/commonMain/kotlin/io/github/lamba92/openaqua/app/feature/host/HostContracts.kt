package io.github.lamba92.openaqua.app.feature.host

import io.github.lamba92.openaqua.app.model.SensorTargetType
import io.github.lamba92.openaqua.app.model.SensorTypesStoredData
import io.github.lamba92.openaqua.app.model.targetTypeFor
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.host.HostSensorId

data class HostState(
    val hostState: DeviceState.Host = DeviceState.Host.Loading,
    val sensorTypes: SensorTypesStoredData = SensorTypesStoredData.DEFAULT,
)

fun HostState.targetTypeFor(sensorId: HostSensorId): SensorTargetType = sensorTypes.targetTypeFor(sensorId)

sealed interface HostEvent {
    data class SetSensorTargetType(
        val sensorId: HostSensorId,
        val targetType: SensorTargetType,
    ) : HostEvent
}

sealed interface HostEffect
