package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.openaqua.engine.hardware.scanDevices
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DevicesCommand(
    private val json: Json,
) : CliktCommand(
        name = "devices",
    ) {
    override fun help(context: Context): String = "List detected Aquacomputer devices"

    override fun run() {
        val devices = scanDevices()
        println(json.encodeToString(devices))
    }
}
