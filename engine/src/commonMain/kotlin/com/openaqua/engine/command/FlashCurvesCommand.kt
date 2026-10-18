package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.openaqua.engine.hardware.HardwareController
import com.openaqua.engine.hardware.findTarget
import com.openaqua.engine.hardware.flashCurve
import com.openaqua.model.AutoCurveParams
import com.openaqua.model.CurvePoint
import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.FlashCurveResponse

abstract class BaseFlashCurvesCommand(
    name: String,
    protected val controller: HardwareController,
) : CliktCommand(name = name) {
    private val device by option("--device", help = "Device ID or alias (e.g. octo-1)").required()
    private val channel by option("--channel", "--fan", help = "Fan channel index (0-indexed)").int().required()
    private val sensor by option("--sensor", help = "Sensor index").int()
    private val pretty by option("--pretty", help = "Pretty-print JSON output").flag(default = false)

    override fun run() {
        val devices = controller.scanDevices()
        val target =
            devices.findTarget(device)
                ?: error("Device '$device' not found")

        val response = executeFlash(target, channel, sensor)
        printResponse(response, isPretty(pretty))
    }

    protected abstract fun executeFlash(
        target: DetectedHardwareDevice,
        channel: Int,
        sensor: Int?,
    ): FlashCurveResponse
}

class FlashCurveParamsCommand(
    controller: HardwareController,
) : BaseFlashCurvesCommand(
        name = "curve-params",
        controller = controller,
    ) {
    override fun help(context: Context): String = "Flash curve generated from parameters"

    private val curveParams by option("--curve-params", help = "AutoCurveParams JSON").required()

    override fun executeFlash(
        target: DetectedHardwareDevice,
        channel: Int,
        sensor: Int?,
    ): FlashCurveResponse {
        val params = defaultJson.decodeFromString<AutoCurveParams>(curveParams)
        return controller.flashCurve(target, channel, params, sensor)
    }
}

class FlashPointsCommand(
    controller: HardwareController,
) : BaseFlashCurvesCommand(
        name = "points",
        controller = controller,
    ) {
    override fun help(context: Context): String = "Flash explicit 16-point curve"

    private val points by option("--points", help = "List of CurvePoint JSON").required()

    override fun executeFlash(
        target: DetectedHardwareDevice,
        channel: Int,
        sensor: Int?,
    ): FlashCurveResponse {
        val pts = defaultJson.decodeFromString<List<CurvePoint>>(points)
        return controller.flashCurve(target, channel, pts, sensor)
    }
}

class FlashCurvesCommand(
    controller: HardwareController,
) : CliktCommand(
        name = "flash-curves",
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
