package io.github.lamba92.openaqua.engine.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

val defaultNdjson: Json =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

/**
 * Continuously reads newline-delimited JSON (NDJSON) objects from standard input (stdin)
 * until EOF (`null`) is encountered, emitting each deserialized instance of [T].
 * Blank or whitespace-only lines are ignored.
 *
 * @param json [Json] configuration to use for decoding (defaults to [defaultNdjson]).
 */
inline fun <reified T> readNdjsonFromStdin(json: Json = defaultNdjson): Flow<T> =
    flow {
        while (true) {
            val line = readlnOrNull() ?: break
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            emit(json.decodeFromString<T>(trimmed))
        }
    }
