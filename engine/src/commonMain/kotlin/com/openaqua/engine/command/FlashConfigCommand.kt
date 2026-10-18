package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.openaqua.engine.hardware.HardwareController
import com.openaqua.engine.hardware.requireTarget
import com.openaqua.model.DeviceType
import com.openaqua.model.FlashConfigRequest
import com.openaqua.model.FlashD5NextConfig
import com.openaqua.model.FlashOctoConfig
import com.openaqua.model.FlashQuadroConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

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

        val jsonElement = strictJson.parseToJsonElement(rawJson)
        val hasTopLevelType = jsonElement is JsonObject && jsonElement.containsKey("type")

        val flashRequest: FlashConfigRequest =
            if (hasTopLevelType) {
                val parsed = strictJson.decodeFromJsonElement<FlashConfigRequest>(jsonElement)
                when (target.type) {
                    DeviceType.OCTO ->
                        require(parsed is FlashOctoConfig) {
                            "Expected 'octo' request for device type OCTO, got '${parsed::class.simpleName}'"
                        }
                    DeviceType.QUADRO ->
                        require(parsed is FlashQuadroConfig) {
                            "Expected 'quadro' request for device type QUADRO, got '${parsed::class.simpleName}'"
                        }
                    DeviceType.D5_NEXT ->
                        require(parsed is FlashD5NextConfig) {
                            "Expected 'd5next' request for device type D5_NEXT, got '${parsed::class.simpleName}'"
                        }
                    else -> error("Unsupported device type: ${target.type}")
                }
                parsed
            } else {
                when (target.type) {
                    DeviceType.OCTO -> strictJson.decodeFromJsonElement(FlashOctoConfig.serializer(), jsonElement)
                    DeviceType.QUADRO -> strictJson.decodeFromJsonElement(FlashQuadroConfig.serializer(), jsonElement)
                    DeviceType.D5_NEXT -> strictJson.decodeFromJsonElement(FlashD5NextConfig.serializer(), jsonElement)
                    else -> error("Unsupported device type: ${target.type}")
                }
            }

        val response = controller.flashConfig(target, flashRequest)
        printResponse(response, pretty)
    }
}
