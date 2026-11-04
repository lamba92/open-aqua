package io.github.lamba92.openaqua.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface EngineResponse<out T> {
    @Serializable
    @SerialName("error")
    data class Error(
        val message: String,
        val stacktrace: List<String> = emptyList(),
    ) : EngineResponse<Nothing>

    @Serializable
    @SerialName("success")
    data class Success<out T>(
        val value: T,
    ) : EngineResponse<T>
}

fun EngineResponse.Error.formatWithStackTrace(): String =
    when {
        stacktrace.isEmpty() -> message
        else -> "$message\n" + stacktrace.joinToString("\n")
    }
