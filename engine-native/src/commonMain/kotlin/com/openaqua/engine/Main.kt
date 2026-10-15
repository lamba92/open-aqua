package com.openaqua.engine

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.openaqua.engine.command.CurvesCommand
import com.openaqua.engine.command.DevicesCommand
import com.openaqua.engine.command.FlashCommand
import com.openaqua.engine.command.OpenAquaEngineCommand
import com.openaqua.engine.command.TelemetryCommand
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    val json =
        Json {
            prettyPrint = false
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    OpenAquaEngineCommand()
        .subcommands(
            DevicesCommand(json),
            TelemetryCommand(json),
            CurvesCommand(json),
            FlashCommand(json),
        ).main(args)
}
