package io.github.lamba92.openaqua.engine.host.linux.command

import io.github.lamba92.openaqua.model.EngineResponse
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

inline fun <reified T : Any> printSuccessResponse(
    value: T,
    pretty: Boolean,
) {
    val response = EngineResponse.Success(value)
    val serializer = if (pretty) prettyJson else defaultJson
    println(serializer.encodeToString<EngineResponse<T>>(response))
}

fun printErrorResponse(
    message: String,
    pretty: Boolean,
) {
    val response: EngineResponse<Nothing> = EngineResponse.Error(message)
    val serializer = if (pretty) prettyJson else defaultJson
    println(serializer.encodeToString(response))
}
