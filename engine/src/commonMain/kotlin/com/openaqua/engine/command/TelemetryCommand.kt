package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.openaqua.engine.hardware.HardwareController
import kotlinx.serialization.json.Json

class TelemetryCommand(
    private val controller: HardwareController,
    private val json: Json,
) : CliktCommand(
        name = "telemetry",
    ) {
    override fun help(context: Context): String = "Read live telemetry (fans, temps, voltages)"

    private val device by option("--device", help = "Device ID").required()

    override fun run() {
        val devices = controller.scanDevices()
        val target =
            devices.firstOrNull { it.id == device }
                ?: error("Device '$device' not found")

        val telemetry = controller.readTelemetry(target)
        println(json.encodeToString(telemetry))
    }
}
