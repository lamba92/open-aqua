package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.model.host.HostFanControlRequest
import io.github.lamba92.openaqua.model.host.HostInfo
import io.github.lamba92.openaqua.model.host.HostTelemetry
import kotlinx.coroutines.flow.Flow

interface OpenAquaHostEngineApi {
    suspend fun getHostInfo(): HostInfo

    fun getTelemetryStream(): Flow<HostTelemetry>

    suspend fun streamFanControl(requests: Flow<HostFanControlRequest>)

    suspend fun restoreFanDefaults()
}
