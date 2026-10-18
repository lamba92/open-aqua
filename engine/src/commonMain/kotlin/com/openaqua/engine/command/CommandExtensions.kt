package com.openaqua.engine.command

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
