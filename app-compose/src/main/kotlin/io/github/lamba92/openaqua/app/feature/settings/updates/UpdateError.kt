package io.github.lamba92.openaqua.app.feature.settings.updates

import io.ktor.http.HttpStatusCode
import java.io.IOException

sealed interface UpdateError {
    data object Network : UpdateError

    data object RateLimited : UpdateError

    data object Generic : UpdateError
}

sealed class UpdateException(
    message: String,
) : RuntimeException(message) {
    class RateLimited : UpdateException("GitHub API rate limit exceeded")

    class ServerError(
        val status: HttpStatusCode,
    ) : UpdateException("GitHub API returned status $status")
}

fun Throwable.toUpdateError(): UpdateError =
    when (this) {
        is UpdateException.RateLimited -> {
            UpdateError.RateLimited
        }

        is IOException -> {
            UpdateError.Network
        }

        else -> {
            var currentCause = cause
            var isNet = false
            while (currentCause != null) {
                if (currentCause is IOException) {
                    isNet = true
                    break
                }
                currentCause = currentCause.cause
            }
            if (isNet) UpdateError.Network else UpdateError.Generic
        }
    }
