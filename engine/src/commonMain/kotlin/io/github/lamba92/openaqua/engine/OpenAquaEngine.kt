package io.github.lamba92.openaqua.engine

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.subcommands
import io.github.lamba92.openaqua.engine.command.OpenAquaEngineCommand
import io.github.lamba92.openaqua.engine.command.printResponse
import io.github.lamba92.openaqua.engine.di.engineCoreModule
import io.github.lamba92.openaqua.model.ErrorResponse
import org.kodein.di.DI
import org.kodein.di.instance

private fun CoreCliktCommand.safeParse(argv: Array<String>) {
    try {
        parse(argv)
    } catch (e: CliktError) {
        echoFormattedHelp(e)
        currentContext.exitProcess(e.statusCode)
    }
}

fun runOpenAquaEngine(
    args: Array<String>,
    init: DI.MainBuilder.() -> Unit = {},
) {
    val di =
        DI {
            import(engineCoreModule)
            init()
        }
    val rootCommand by di.instance<OpenAquaEngineCommand>()
    val subcommands by di.instance<Set<CliktCommand>>()
    val configuredRoot = rootCommand.subcommands(subcommands)
    try {
        configuredRoot.safeParse(args)
    } catch (e: Throwable) {
        val errorResponse =
            ErrorResponse(
                message = e.message ?: "Unknown error",
            )
        val pretty = args.contains("--pretty")
        printResponse(errorResponse, pretty)
        configuredRoot.currentContext.exitProcess(1)
    }
}
