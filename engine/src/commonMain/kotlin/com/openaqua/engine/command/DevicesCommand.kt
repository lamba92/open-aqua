package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.openaqua.engine.hardware.HardwareController
import com.openaqua.model.DevicesResponse
import kotlinx.serialization.json.Json

class DevicesCommand(
    private val controller: HardwareController,
    private val json: Json,
) : CliktCommand(
        name = "devices",
    ) {
    override fun help(context: Context): String = "List detected Aquacomputer devices"

    private val pretty by option("--pretty", help = "Pretty-print JSON output").flag(default = false)

    override fun run() {
        val devices = controller.scanDevices()
        json.printResponse(DevicesResponse(devices), isPretty(pretty))
    }
}
