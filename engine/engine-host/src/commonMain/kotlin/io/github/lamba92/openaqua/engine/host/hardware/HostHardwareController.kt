package io.github.lamba92.openaqua.engine.host.hardware

import io.github.lamba92.openaqua.model.host.HostInfo
import io.github.lamba92.openaqua.model.host.HostTelemetry
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface HostHardwareController {
    fun getHostInfo(): HostInfo

    fun getTelemetryFlow(pollingInterval: Duration = 1.seconds): Flow<HostTelemetry>
}
