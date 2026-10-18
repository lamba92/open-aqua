package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.openaqua.engine.hardware.HardwareController
import com.openaqua.engine.hardware.requireTarget
import kotlinx.coroutines.runBlocking

class StreamTelemetryCommand(
    private val controller: HardwareController,
) : CliktCommand(
        name = "stream-telemetry",
    ) {
    override fun help(context: Context): String = "Continuously stream live telemetry as NDJSON (one line per report)"

    private val device by option("--device", help = "Device ID or alias (e.g. octo-1)").required()

    override fun run() {
        val target = controller.requireTarget(device)
        runBlocking {
            controller.streamTelemetry(target).collect { telemetry ->
                printResponse(telemetry, pretty = false)
            }
        }
    }
}
