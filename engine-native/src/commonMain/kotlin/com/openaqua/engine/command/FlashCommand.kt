package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.openaqua.engine.hardware.flashCurve
import com.openaqua.engine.hardware.scanDevices
import com.openaqua.model.AutoCurveParams
import com.openaqua.model.CurvePoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FlashCommand(
    private val json: Json,
) : CliktCommand(
        name = "flash",
    ) {
    override fun help(context: Context): String = "Flash fan curve to hardware"

    private val device by option("--device", help = "Device ID")
    private val fan by option("--fan", help = "Fan channel ID (e.g. fan1)").required()
    private val generate by option("--generate", help = "AutoCurveParams JSON")
    private val points by option("--points", help = "List of CurvePoint JSON")
    private val sensor by option("--sensor", help = "Sensor index").int()

    override fun run() {
        val devices = scanDevices()
        val target =
            if (device != null) {
                devices.firstOrNull { it.id == device }
                    ?: error("Device '$device' not found")
            } else {
                devices.firstOrNull()
                    ?: error("No Aquacomputer devices found")
            }

        require(generate != null || points != null) {
            "Either --generate '<json>' or --points '<json>' must be provided"
        }

        val response =
            if (generate != null) {
                val params = json.decodeFromString<AutoCurveParams>(generate!!)
                flashCurve(target, fan, params, sensor)
            } else {
                val pts = json.decodeFromString<List<CurvePoint>>(points!!)
                flashCurve(target, fan, pts, sensor)
            }

        println(json.encodeToString(response))
    }
}
