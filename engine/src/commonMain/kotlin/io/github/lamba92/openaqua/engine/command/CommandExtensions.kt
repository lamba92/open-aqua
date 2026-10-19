package io.github.lamba92.openaqua.engine.command

import io.github.lamba92.openaqua.engine.util.readTextOrNull
import kotlinx.io.files.Path
import kotlinx.serialization.encodeToString
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

inline fun <reified T : Any> printResponse(
    response: T,
    pretty: Boolean,
) {
    val serializer = if (pretty) prettyJson else defaultJson
    println(serializer.encodeToString(response))
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
