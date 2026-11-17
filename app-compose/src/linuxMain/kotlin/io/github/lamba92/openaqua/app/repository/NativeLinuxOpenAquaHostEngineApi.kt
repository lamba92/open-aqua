package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.engine.host.hardware.HostHardwareController
import io.github.lamba92.openaqua.model.host.HostFanControlRequest
import io.github.lamba92.openaqua.model.host.HostInfo
import io.github.lamba92.openaqua.model.host.HostTelemetry
import kotlinx.coroutines.flow.Flow

class NativeLinuxOpenAquaHostEngineApi(
    private val controller: HostHardwareController,
) : OpenAquaHostEngineApi {
    override suspend fun getHostInfo(): HostInfo = controller.getHostInfo()

    override fun getTelemetryStream(): Flow<HostTelemetry> = controller.getTelemetryFlow()

    override suspend fun streamFanControl(requests: Flow<HostFanControlRequest>) {
        controller.streamFanControl(requests)
    }

    override suspend fun restoreFanDefaults() {
        controller.restoreFanDefaults()
    }
}
