package com.openaqua.engine

import com.github.ajalt.clikt.core.BaseCliktCommand
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parsers.CommandLineParser
import com.openaqua.engine.command.OpenAquaEngineCommand
import com.openaqua.model.EngineResponse
import com.openaqua.model.ErrorResponse
import kotlinx.serialization.json.Json

inline fun <T : BaseCliktCommand<T>> CommandLineParser.main(
    command: T,
    json: Json,
    parseAndRun: T.() -> Unit,
) {
    try {
        command.parseAndRun()
    } catch (e: CliktError) {
        command.echoFormattedHelp(e)
        command.currentContext.exitProcess(e.statusCode)
    } catch (e: Throwable) {
        val errorResponse: EngineResponse =
            ErrorResponse(
                message = e.message ?: "Unknown error",
            )
        println(json.encodeToString<EngineResponse>(errorResponse))
        command.currentContext.exitProcess(1)
    }
}

fun CoreCliktCommand.main(
    argv: Array<String>,
    json: Json,
) {
    CommandLineParser.main(this, json) {
        parse(argv)
    }
}

class OpenAquaEngine(
    private val rootCommand: OpenAquaEngineCommand,
    private val subcommands: List<CliktCommand>,
    private val json: Json,
) {
    fun execute(args: Array<String>) {
        rootCommand.subcommands(subcommands).main(args, json)
    }
}
