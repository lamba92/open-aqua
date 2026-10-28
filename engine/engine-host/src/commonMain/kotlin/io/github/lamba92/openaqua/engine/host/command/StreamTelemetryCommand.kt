package io.github.lamba92.openaqua.engine.host.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import io.github.lamba92.openaqua.engine.host.hardware.HostHardwareController
import kotlinx.coroutines.runBlocking

class StreamTelemetryCommand(
    private val controller: HostHardwareController,
) : CliktCommand(
        name = "stream-telemetry",
    ) {
    override fun help(context: Context): String = "Continuously stream live host telemetry as NDJSON at 1 Hz"

    override fun run() {
        runBlocking {
            controller.getTelemetryFlow().collect { telemetry ->
                printSuccessResponse(telemetry, pretty = false)
            }
        }
    }
}
