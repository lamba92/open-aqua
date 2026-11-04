package io.github.lamba92.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.core.parse
import io.github.lamba92.openaqua.engine.util.readTextOrNull
import io.github.lamba92.openaqua.model.EngineResponse
import io.github.lamba92.openaqua.model.EngineResponse.Error
import kotlinx.io.files.Path
import kotlinx.serialization.json.Json

val defaultJson =
    Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

val prettyJson =
    Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

inline fun <reified T : Any> printSuccessResponse(
    value: T,
    pretty: Boolean,
) {
    val response = EngineResponse.Success(value)
    val serializer = if (pretty) prettyJson else defaultJson
    println(serializer.encodeToString<EngineResponse<T>>(response))
}

fun printErrorResponse(
    throwable: Throwable,
    pretty: Boolean,
) {
    val message = throwable.message ?: "Unknown error"
    val stacktrace = throwable.stackTraceToString().lines()
    val response: EngineResponse<Nothing> = Error(message = message, stacktrace = stacktrace)
    val serializer = if (pretty) prettyJson else defaultJson
    println(serializer.encodeToString<EngineResponse<Nothing>>(response))
}

fun readInputPayload(
    inline: String?,
    file: String?,
    allowEmpty: Boolean = false,
): String? =
    when {
        inline != null && file != null -> error("Cannot specify both inline argument and --file")
        inline != null -> inline
        file != null && file != "-" -> Path(file).readTextOrNull() ?: error("Cannot read file '$file'")
        allowEmpty -> null
        else -> error("Must specify either inline argument or --file")
    }

fun CoreCliktCommand.safeParse(argv: Array<String>) {
    try {
        parse(argv)
    } catch (e: CliktError) {
        echoFormattedHelp(e)
        currentContext.exitProcess(e.statusCode)
    }
}
