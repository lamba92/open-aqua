package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.openaqua.engine.controller.HardwareController
import com.openaqua.model.AutoCurveParams
import com.openaqua.model.CurvePoint
import com.openaqua.model.FlashCurveResponse
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
        val devices = HardwareController.getDetectedDevices()
        val target =
            device?.let { devId -> devices.firstOrNull { it.id == devId } }
                ?: devices.firstOrNull()

        if (target == null) {
            val errorResp =
                FlashCurveResponse(
                    status = "error",
                    deviceId = "unknown",
                    fanId = fan,
                    bytesWritten = 0,
                    verified = false,
                    error = "No Aquacomputer devices found",
                )
            println(json.encodeToString(errorResp))
            return
        }

        if (generate == null && points == null) {
            val errorResp =
                FlashCurveResponse(
                    status = "error",
                    deviceId = target.id,
                    fanId = fan,
                    bytesWritten = 0,
                    verified = false,
                    error = "Either --generate '<json>' or --points '<json>' must be provided",
                )
            println(json.encodeToString(errorResp))
            return
        }

        val response =
            if (generate != null) {
                try {
                    val params = json.decodeFromString<AutoCurveParams>(generate!!)
                    HardwareController.flashCurve(target, fan, params, sensor)
                } catch (e: Exception) {
                    FlashCurveResponse(
                        status = "error",
                        deviceId = target.id,
                        fanId = fan,
                        bytesWritten = 0,
                        verified = false,
                        error = "Invalid JSON in --generate: ${e.message}",
                    )
                }
            } else {
                try {
                    val pts = json.decodeFromString<List<CurvePoint>>(points!!)
                    HardwareController.flashCurve(target, fan, pts, sensor)
                } catch (e: Exception) {
                    FlashCurveResponse(
                        status = "error",
                        deviceId = target.id,
                        fanId = fan,
                        bytesWritten = 0,
                        verified = false,
                        error = "Invalid JSON in --points: ${e.message}",
                    )
                }
            }

        println(json.encodeToString(response))
    }
}
