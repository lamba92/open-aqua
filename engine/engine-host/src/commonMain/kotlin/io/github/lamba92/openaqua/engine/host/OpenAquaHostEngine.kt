package io.github.lamba92.openaqua.engine.host

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.subcommands
import io.github.lamba92.openaqua.engine.host.command.OpenAquaHostEngineCommand
import io.github.lamba92.openaqua.engine.host.command.printErrorResponse
import io.github.lamba92.openaqua.engine.host.di.hostEngineCoreModule
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

fun runOpenAquaHostEngine(
    args: Array<String>,
    init: DI.MainBuilder.() -> Unit = {},
) {
    val di =
        DI {
            import(hostEngineCoreModule)
            init()
        }
    val rootCommand by di.instance<OpenAquaHostEngineCommand>()
    val subcommands by di.instance<Set<CliktCommand>>()
    val configuredRoot = rootCommand.subcommands(subcommands)
    try {
        configuredRoot.safeParse(args)
    } catch (e: Throwable) {
        val pretty = args.contains("--pretty")
        printErrorResponse(e.message ?: "Unknown error", pretty)
        configuredRoot.currentContext.exitProcess(1)
    }
}
