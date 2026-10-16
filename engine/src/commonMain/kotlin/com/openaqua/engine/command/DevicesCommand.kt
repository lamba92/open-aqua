package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.openaqua.engine.hardware.HardwareController
import com.openaqua.model.DevicesResponse
import com.openaqua.model.EngineResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DevicesCommand(
    private val controller: HardwareController,
    private val json: Json,
) : CliktCommand(
        name = "devices",
    ) {
    override fun help(context: Context): String = "List detected Aquacomputer devices"

    override fun run() {
        val devices = controller.scanDevices()
        println(json.encodeToString<EngineResponse>(DevicesResponse(devices)))
    }
}
