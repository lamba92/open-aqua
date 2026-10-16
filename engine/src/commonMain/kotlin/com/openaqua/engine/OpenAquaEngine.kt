package com.openaqua.engine

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.openaqua.engine.command.CurvesCommand
import com.openaqua.engine.command.DevicesCommand
import com.openaqua.engine.command.FlashCommand
import com.openaqua.engine.command.OpenAquaEngineCommand
import com.openaqua.engine.command.TelemetryCommand
import com.openaqua.model.EngineErrorResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class OpenAquaEngine(
    override val di: DI,
) : DIAware {
    private val json: Json by instance()
    private val rootCommand: OpenAquaEngineCommand by instance()
    private val devicesCommand: DevicesCommand by instance()
    private val telemetryCommand: TelemetryCommand by instance()
    private val curvesCommand: CurvesCommand by instance()
    private val flashCommand: FlashCommand by instance()

    fun execute(args: Array<String>) {
        try {
            rootCommand
                .subcommands(
                    devicesCommand,
                    telemetryCommand,
                    curvesCommand,
                    flashCommand,
                ).main(args)
        } catch (e: Throwable) {
            if (e is CliktError && e.statusCode == 0) {
                return
            }
            val errorResponse =
                EngineErrorResponse(
                    status = "error",
                    error = e.message ?: "Unknown error",
                    type = e::class.simpleName,
                )
            println(json.encodeToString(errorResponse))
        }
    }
}
