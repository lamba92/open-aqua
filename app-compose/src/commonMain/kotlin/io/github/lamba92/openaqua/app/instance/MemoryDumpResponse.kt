package io.github.lamba92.openaqua.app.instance

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface MemoryDumpResponse {
    @Serializable
    @SerialName("success")
    data class Success(
        val file: String,
        val hint: String,
    ) : MemoryDumpResponse

    @Serializable
    @SerialName("error")
    data class Error(
        val message: String,
    ) : MemoryDumpResponse
}

@Serializable
data class ForegroundRequestResponse(
    val status: String,
)
