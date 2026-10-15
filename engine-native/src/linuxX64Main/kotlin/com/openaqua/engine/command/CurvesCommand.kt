package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.openaqua.engine.controller.HardwareController
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CurvesCommand(
    private val json: Json,
) : CliktCommand(
        name = "curves",
    ) {
    override fun help(context: Context): String = "Read active 16-point hardware curves"

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

        val curves = HardwareController.getCurves(target)
        println(json.encodeToString(curves))
    }
}
