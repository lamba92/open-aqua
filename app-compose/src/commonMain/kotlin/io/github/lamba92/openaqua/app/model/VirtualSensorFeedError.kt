package io.github.lamba92.openaqua.app.model

import io.github.lamba92.openaqua.model.device.DeviceId

sealed interface VirtualSensorFeedError {
    val destinationDeviceId: DeviceId
    val destinationChannel: Int

    data class SourceDeviceUnavailable(
        override val destinationDeviceId: DeviceId,
        override val destinationChannel: Int,
        val source: VirtualSensorFeedSource,
    ) : VirtualSensorFeedError

    data class SourceSensorDisconnected(
        override val destinationDeviceId: DeviceId,
        override val destinationChannel: Int,
        val source: VirtualSensorFeedSource,
    ) : VirtualSensorFeedError

    data class DestinationDeviceDisconnected(
        override val destinationDeviceId: DeviceId,
        override val destinationChannel: Int,
    ) : VirtualSensorFeedError

    data class Other(
        override val destinationDeviceId: DeviceId,
        override val destinationChannel: Int,
    ) : VirtualSensorFeedError
}
