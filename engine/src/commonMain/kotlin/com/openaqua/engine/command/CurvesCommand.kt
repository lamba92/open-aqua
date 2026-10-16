package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.openaqua.engine.hardware.HardwareController
import com.openaqua.model.EngineResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CurvesCommand(
    private val controller: HardwareController,
    private val json: Json,
) : CliktCommand(
        name = "curves",
    ) {
    override fun help(context: Context): String = "Read active 16-point hardware curves"

    private val device by option("--device", help = "Device ID").required()

    override fun run() {
        val devices = controller.scanDevices()
        val target =
            devices.firstOrNull { it.id == device }
                ?: error("Device '$device' not found")

        val curves = controller.readCurves(target)
        println(json.encodeToString<EngineResponse>(curves))
    }
}
