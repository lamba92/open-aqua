package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.openaqua.engine.controller.HardwareController
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TelemetryCommand(
    private val json: Json,
) : CliktCommand(
        name = "telemetry",
    ) {
    override fun help(context: Context): String = "Read live telemetry (fans, temps, voltages)"

    private val device by option("--device", help = "Device ID")

    override fun run() {
        val devices = HardwareController.getDetectedDevices()
        val target =
            device?.let { devId -> devices.firstOrNull { it.id == devId } }
                ?: devices.firstOrNull()

        if (target == null) {
            println("""{"error": "No Aquacomputer devices found"}""")
            return
        }

        val telemetry = HardwareController.getTelemetry(target)
        println(json.encodeToString(telemetry))
    }
}
