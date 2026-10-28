package io.github.lamba92.openaqua.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.time.Duration

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
