package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.openaqua.engine.hardware.HardwareController
import com.openaqua.engine.hardware.flashCurve
import com.openaqua.model.AutoCurveParams
import com.openaqua.model.CurvePoint
import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.FlashCurveResponse

abstract class BaseFlashCommand(
    name: String,
    protected val controller: HardwareController,
) : CliktCommand(name = name) {
    private val device by option("--device", help = "Device ID").required()
    private val fan by option("--fan", help = "Fan channel ID (e.g. fan1)").required()
    private val sensor by option("--sensor", help = "Sensor index").int()
    private val pretty by option("--pretty", help = "Pretty-print JSON output").flag(default = false)

    override fun run() {
        val devices = controller.scanDevices()
        val target =
            devices.firstOrNull { it.id == device }
                ?: error("Device '$device' not found")

        val response = executeFlash(target, fan, sensor)
        printResponse(response, isPretty(pretty))
    }

    protected abstract fun executeFlash(
        target: DetectedHardwareDevice,
        fan: String,
        sensor: Int?,
    ): FlashCurveResponse
}

class FlashCurveParamsCommand(
    controller: HardwareController,
) : BaseFlashCommand(
        name = "curve-params",
        controller = controller,
    ) {
    override fun help(context: Context): String = "Flash curve generated from parameters"

    private val curveParams by option("--curve-params", help = "AutoCurveParams JSON").required()

    override fun executeFlash(
        target: DetectedHardwareDevice,
        fan: String,
        sensor: Int?,
    ): FlashCurveResponse {
        val params = defaultJson.decodeFromString<AutoCurveParams>(curveParams)
        return controller.flashCurve(target, fan, params, sensor)
    }
}

class FlashPointsCommand(
    controller: HardwareController,
) : BaseFlashCommand(
        name = "points",
        controller = controller,
    ) {
    override fun help(context: Context): String = "Flash explicit 16-point curve"

    private val points by option("--points", help = "List of CurvePoint JSON").required()

    override fun executeFlash(
        target: DetectedHardwareDevice,
        fan: String,
        sensor: Int?,
    ): FlashCurveResponse {
        val pts = defaultJson.decodeFromString<List<CurvePoint>>(points)
        return controller.flashCurve(target, fan, pts, sensor)
    }
}

class FlashCommand(
    controller: HardwareController,
) : CliktCommand(
        name = "flash",
    ) {
    override fun help(context: Context): String = "Flash fan curve to hardware"

    private val pretty by option("--pretty", help = "Pretty-print JSON output").flag(default = false)

    init {
        subcommands(
            FlashCurveParamsCommand(controller),
            FlashPointsCommand(controller),
        )
    }

    override fun run() {
        if (pretty) {
            currentContext.findOrSetObject { true }
        }
    }
}
