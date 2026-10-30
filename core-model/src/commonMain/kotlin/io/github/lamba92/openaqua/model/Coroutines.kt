package io.github.lamba92.openaqua.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

fun CoroutineScope.every(
    interval: Duration,
    action: suspend () -> Unit,
): Job =
    launch {
        while (true) {
            action()
            delay(interval)
        }
    }

/**
 * Retries collection of this [Flow] up to [retries] consecutive times when an exception occurs,
 * pausing for [delay] between attempts.
 *
 * If the flow emits at least one value, the consecutive failure counter is reset.
 *
 * @param retries Maximum number of consecutive retry attempts (default is 3).
 * @param delay Duration to pause before each retry attempt (default is 1 second).
 * @param predicate Predicate to filter which exceptions should be retried.
 */
fun <T> Flow<T>.retryAtLeast(
    retries: Long = 3,
    delay: Duration = 1.seconds,
    predicate: suspend (cause: Throwable) -> Boolean = { true },
): Flow<T> {
    var consecutiveFailures = 0L
    return onEach { consecutiveFailures = 0L }
        .retryWhen { cause, _ ->
            if (consecutiveFailures < retries && predicate(cause)) {
                consecutiveFailures++
                if (delay > Duration.ZERO) delay(delay)
                true
            } else {
                false
            }
        }
}

/**
 * Retries collection of this [Flow] up to [retries] consecutive times with exponential backoff.
 *
 * If the flow emits at least one value, the consecutive failure counter is reset.
 *
 * @param retries Maximum number of consecutive retry attempts (default is 3).
 * @param initialDelay Initial delay before the first retry (default is 500 milliseconds).
 * @param maxDelay Maximum delay cap between retries (default is 2 seconds).
 * @param factor Multiplier applied to the delay on each subsequent retry (default is 2.0).
 * @param predicate Predicate to filter which exceptions should be retried.
 */
fun <T> Flow<T>.retryWithBackoff(
    retries: Long = 3,
    initialDelay: Duration = 500.milliseconds,
    maxDelay: Duration = 2.seconds,
    factor: Double = 2.0,
    predicate: suspend (cause: Throwable) -> Boolean = { true },
): Flow<T> {
    var consecutiveFailures = 0L
    return onEach { consecutiveFailures = 0L }
        .retryWhen { cause, _ ->
            if (consecutiveFailures < retries && predicate(cause)) {
                val backoffMs =
                    (initialDelay.inWholeMilliseconds * factor.pow(consecutiveFailures.toDouble()))
                        .toLong()
                        .coerceAtMost(maxDelay.inWholeMilliseconds)
                consecutiveFailures++
                delay(backoffMs.milliseconds)
                true
            } else {
                false
            }
        }
}

fun <T> every(
    interval: Duration,
    action: suspend () -> T,
): Flow<T> =
    flow {
        while (true) {
            emit(action())
            delay(interval)
        }
    }
