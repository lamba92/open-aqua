package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.findObject
import com.openaqua.model.EngineResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val prettyJson =
    Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

fun CliktCommand.isPretty(localPretty: Boolean): Boolean {
    val parentPretty = currentContext.findObject<Boolean>() ?: false
    return localPretty || parentPretty
}

fun Json.printResponse(
    response: EngineResponse,
    pretty: Boolean,
) {
    val serializer = if (pretty) prettyJson else this
    println(serializer.encodeToString<EngineResponse>(response))
}
