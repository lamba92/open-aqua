package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.engine.device.hardware.HardwareController
import io.github.lamba92.openaqua.model.device.DescriptorsResponse
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.device.DeviceConfig
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import io.github.lamba92.openaqua.model.device.DeviceTelemetry
import io.github.lamba92.openaqua.model.device.DevicesResponse
import io.github.lamba92.openaqua.model.device.FeedVirtualSensorsRequest
import io.github.lamba92.openaqua.model.device.FlashConfigRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NativeOpenAquaDeviceEngineApi(
    private val hardwareController: HardwareController,
) : OpenAquaDeviceEngineApi {
    private fun requireDevice(deviceId: DeviceId): DetectedHardwareDevice =
        hardwareController.scanDevices().firstOrNull { it.id == deviceId }
            ?: error("Device '${deviceId.value}' not found")

    override suspend fun listDevices(): DevicesResponse = DevicesResponse(hardwareController.scanDevices())

    override suspend fun listDescriptors(): DescriptorsResponse = DescriptorsResponse(DeviceRegistry.ALL)

    override fun getTelemetryStream(deviceId: DeviceId): Flow<DeviceTelemetry> = hardwareController.streamTelemetry(requireDevice(deviceId))

    override suspend fun getDeviceConfig(deviceId: DeviceId): DeviceConfig = hardwareController.readConfig(requireDevice(deviceId))

    override suspend fun flashConfig(
        deviceId: DeviceId,
        config: FlashConfigRequest,
    ): DeviceConfig = hardwareController.flashConfig(requireDevice(deviceId), config)

    override suspend fun feedVirtualSensors(
        deviceId: DeviceId,
        source: Flow<FeedVirtualSensorsRequest>,
    ) {
        hardwareController.feedVirtualSensors(
            device = requireDevice(deviceId),
            requests = source.map { it.sensors },
        )
    }
}
