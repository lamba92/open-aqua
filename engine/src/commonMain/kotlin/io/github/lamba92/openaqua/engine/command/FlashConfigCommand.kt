package io.github.lamba92.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.github.lamba92.openaqua.engine.hardware.HardwareController
import io.github.lamba92.openaqua.engine.hardware.requireTarget
import io.github.lamba92.openaqua.model.FlashConfigRequest
import kotlinx.serialization.json.Json

private val strictJson =
    Json {
        ignoreUnknownKeys = false
    }

class FlashConfigCommand(
    private val controller: HardwareController,
) : CliktCommand(
        name = "flash",
    ) {
    override fun help(context: Context): String = "Flash hardware configuration"

    private val device by option("--device", help = "Device ID or alias (e.g. octo-1)").required()
    private val request by option("--request", help = "Configuration flash patch JSON")
    private val file by option("--file", help = "Path to JSON file containing configuration flash patch")
    private val pretty by option("--pretty", help = "Pretty-print JSON output").flag(default = false)

    override fun run() {
        val target = controller.requireTarget(device)
        val rawJson = readInputPayload(request, file) ?: error("Must specify either --request or --file")

        val flashRequest = strictJson.decodeFromString(FlashConfigRequest.serializer(), rawJson)
        val response = controller.flashConfig(target, flashRequest)
        printSuccessResponse(response, pretty)
    }
}
