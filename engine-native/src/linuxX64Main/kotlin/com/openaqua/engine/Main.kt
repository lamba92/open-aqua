package com.openaqua.engine

import com.openaqua.engine.controller.HardwareController
import com.openaqua.model.AutoCurveParams
import com.openaqua.model.CurvePoint
import com.openaqua.model.FlashCurveResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json =
    Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

fun main(args: Array<String>) {
    val command = args.firstOrNull() ?: "help"
    val options = parseOptions(args)

    when (command) {
        "devices" -> handleDevices()
        "telemetry" -> handleTelemetry(options)
        "curves" -> handleCurves(options)
        "flash" -> handleFlash(options)
        "help", "--help", "-h" -> printUsage()
        else -> {
            println("""{"error": "Unknown command: $command. Use 'help' for usage."}""")
        }
    }
}

private fun parseOptions(args: Array<String>): Map<String, String> {
    val options = mutableMapOf<String, String>()
    var i = 1
    while (i < args.size) {
        val arg = args[i]
        if (arg.startsWith("--") && i + 1 < args.size) {
            val key = arg.removePrefix("--")
            options[key] = args[i + 1]
            i += 2
        } else {
            i++
        }
    }
    return options
}

private fun handleDevices() {
    val devices = HardwareController.getDetectedDevices()
    println(json.encodeToString(devices))
}

private fun handleTelemetry(options: Map<String, String>) {
    val devices = HardwareController.getDetectedDevices()
    val target =
        options["device"]?.let { devId -> devices.firstOrNull { it.id == devId } }
            ?: devices.firstOrNull()

    if (target == null) {
        println("""{"error": "No Aquacomputer devices found"}""")
        return
    }

    val telemetry = HardwareController.getTelemetry(target)
    println(json.encodeToString(telemetry))
}

private fun handleCurves(options: Map<String, String>) {
    val devices = HardwareController.getDetectedDevices()
    val target =
        options["device"]?.let { devId -> devices.firstOrNull { it.id == devId } }
            ?: devices.firstOrNull()

    if (target == null) {
        println("""{"error": "No Aquacomputer devices found"}""")
        return
    }

    val curves = HardwareController.getCurves(target)
    println(json.encodeToString(curves))
}

private fun handleFlash(options: Map<String, String>) {
    val devices = HardwareController.getDetectedDevices()
    val target =
        options["device"]?.let { devId -> devices.firstOrNull { it.id == devId } }
            ?: devices.firstOrNull()

    val fanId = options["fan"]
    val generateJson = options["generate"]
    val pointsJson = options["points"]
    val sensorIdx = options["sensor"]?.toIntOrNull()

    if (target == null || fanId == null || (generateJson == null && pointsJson == null)) {
        val errorResp =
            FlashCurveResponse(
                status = "error",
                deviceId = target?.id ?: "unknown",
                fanId = fanId ?: "unknown",
                bytesWritten = 0,
                verified = false,
                error = "Required arguments: --fan <fanId> (--generate '<json>' | --points '<json>')",
            )
        println(json.encodeToString(errorResp))
        return
    }

    val response =
        if (generateJson != null) {
            try {
                val params = json.decodeFromString<AutoCurveParams>(generateJson)
                HardwareController.flashCurve(target, fanId, params, sensorIdx)
            } catch (e: Exception) {
                FlashCurveResponse(
                    status = "error",
                    deviceId = target.id,
                    fanId = fanId,
                    bytesWritten = 0,
                    verified = false,
                    error = "Invalid JSON in --generate: ${e.message}",
                )
            }
        } else {
            try {
                val points = json.decodeFromString<List<CurvePoint>>(pointsJson!!)
                HardwareController.flashCurve(target, fanId, points, sensorIdx)
            } catch (e: Exception) {
                FlashCurveResponse(
                    status = "error",
                    deviceId = target.id,
                    fanId = fanId,
                    bytesWritten = 0,
                    verified = false,
                    error = "Invalid JSON in --points: ${e.message}",
                )
            }
        }

    println(json.encodeToString(response))
}

private fun printUsage() {
    println(
        """
        OpenAqua Native Hardware Engine
        Usage: open-aqua-engine <command> [options]

        Commands:
          devices                                             List detected Aquacomputer devices
          telemetry [--device <id>]                           Read live telemetry (fans, temps, voltages)
          curves    [--device <id>]                           Read active 16-point hardware curves
          flash     --fan <id> (--generate '<params>' | --points '<points>') [--device <id>] [--sensor <idx>]
                                                              Flash fan curve to hardware

        All commands output clean JSON to stdout.
        """.trimIndent(),
    )
}
