package io.github.lamba92.openaqua.app.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
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
) = flow {
    while (true) {
        emit(action())
        delay(interval)
    }
}

/**
 * Rate-limits emissions from this [Flow] so that downstream collectors receive at most one
 * element per [period], while always delivering the most recent value without queuing or
 * backpressuring the upstream producer.
 *
 * Internally, this combines [conflate] with [transform]:
 * 1. **Decoupled producer ([conflate]):** Places a 1-element conflated buffer between upstream
 *    and downstream. If the upstream emits faster than [period], intermediate emissions overwrite
 *    each other in the buffer so only the freshest value is retained.
 * 2. **Immediate leading edge + cooldown ([transform]):** The very first element is emitted
 *    immediately with zero initial delay (unlike [kotlinx.coroutines.flow.sample]), followed by
 *    a suspension of [period] before the next conflated value is pulled.
 *
 * ### Timeline Example (`period = 1.seconds`)
 * ```text
 * Time (s)   : 0.0   0.2   0.5   0.8   1.0   1.4   1.9   2.0
 * Upstream   : ─A─────B─────C─────D───────────E─────F─────────>
 * Buffer     : [A]   [B ->  C ->  D]         [E ->  F]
 * Downstream : ─A───────────────────────D─────────────────F───>
 * ```
 *
 * ### Usage Example
 * ```kotlin
 * val uiState: StateFlow<UiState> =
 *     highFrequencyTelemetryFlow
 *         .throttleLatest(1.seconds)
 *         .stateIn(scope, SharingStarted.Eagerly, UiState.Loading)
 * ```
 *
 * @param T The type of elements emitted by the flow.
 * @param period The minimum time interval between consecutive downstream emissions.
 * @return A rate-limited [Flow] that emits immediately on the first element and at most once per [period] thereafter.
 */
fun <T> Flow<T>.throttleLatest(period: Duration): Flow<T> =
    conflate()
        .transform { value ->
            emit(value)
            delay(period)
        }

/**
 * Dynamically maps each element in an upstream [Flow] of lists into an individual child [Flow]
 * via [itemFlow], combining the latest emissions of all active child flows into a single [List]
 * that preserves the upstream ordering.
 *
 * Unlike [kotlinx.coroutines.flow.flatMapLatest] combined with [combine], which cancels and
 * restarts **all** child flows whenever the upstream list changes, [combineByKey] diffs the
 * incoming items using [keySelector] and manages child coroutine lifecycles incrementally:
 *
 * - **Unchanged keys:** Their existing child flows continue running uninterrupted without restarting
 *   (preventing expensive teardown/setup such as restarting OS processes or network connections).
 * - **Added keys:** A new child flow is launched via [itemFlow] only for the newly appeared key.
 * - **Removed keys:** The child coroutine for the removed key is cancelled immediately and its
 *   cached value is evicted.
 * - **Empty list:** When the upstream emits an empty list, an empty list is emitted immediately.
 * - **Initial readiness:** Downstream emissions wait until all currently active keys have emitted
 *   at least one value.
 *
 * ### Example 1: Hardware Device Telemetry Multiplexing
 * ```kotlin
 * val deviceStates: Flow<List<DeviceState>> =
 *     pollConnectedDevices() // Flow<List<DetectedHardwareDevice>>
 *         .combineByKey(
 *             keySelector = { device -> device.id },
 *             itemFlow = { device ->
 *                 // Opens a long-running USB telemetry stream for this specific device.
 *                 // Plugging in a second device won't restart the first device's stream!
 *                 device.observeTelemetryAndConfig()
 *             }
 *         )
 * ```
 *
 * ### Example 2: Timeline of Emissions
 * ```text
 * Upstream List : ──[A]──────────────[A, B]─────────────────[B]──────>
 * Child Flow A  : ────a1────a2──────────a3────────a4─(cancelled)
 * Child Flow B  : ─────────────────────────b1────────b2────────b3────>
 *
 * Output List   : ────[a1]──[a2]───────────[a3, b1]──[a4, b2]──[b2]──[b3]─>
 * ```
 * *(Note: When `[A, B]` arrives, emission pauses until `B` produces its first value `b1`,
 * while `A` continues running in place and updates to `a3`.)*
 *
 * @param T The element type of the upstream list.
 * @param K The unique key type used to identify and diff elements across upstream emissions.
 * @param R The emission type of each per-item child flow.
 * @param keySelector Extracts a unique, stable key [K] from an item [T].
 * @param itemFlow Factory creating the cold or hot [Flow] for a newly discovered item [T].
 * @return A [Flow] emitting the combined [List] of latest [R] values in the order of the most recent upstream list.
 */
fun <T, K, R> Flow<List<T>>.combineByKey(
    keySelector: (T) -> K,
    itemFlow: (T) -> Flow<R>,
): Flow<List<R>> =
    channelFlow {
        val activeJobs = mutableMapOf<K, Job>()
        val latestValues = MutableStateFlow<Map<K, R>>(emptyMap())
        val currentOrder = MutableStateFlow<List<K>?>(null)

        // 1. Upstream collector: diffs keys and starts/cancels ONLY added/removed devices
        launch {
            distinctUntilChanged().collect { items ->
                val newKeys = items.map(keySelector)
                val newKeySet = newKeys.toSet()

                // Cancel removed devices
                val removedKeys = activeJobs.keys - newKeySet
                for (key in removedKeys) {
                    activeJobs.remove(key)?.cancel()
                }
                latestValues.update { it - removedKeys }

                // Launch flows ONLY for newly added devices (existing ones keep running!)
                for (item in items) {
                    val key = keySelector(item)
                    if (key !in activeJobs) {
                        activeJobs[key] =
                            launch {
                                itemFlow(item).collect { value ->
                                    latestValues.update { it + (key to value) }
                                }
                            }
                    }
                }

                currentOrder.value = newKeys
            }
        }

        // 2. Emit the combined list in order whenever all active keys have emitted at least once
        combine(currentOrder.filterNotNull(), latestValues) { keys, valuesMap ->
            when {
                keys.isEmpty() -> emptyList()
                keys.all { it in valuesMap } -> keys.map { valuesMap.getValue(it) }
                else -> null // Still waiting for initial emission from a newly connected device
            }
        }.filterNotNull()
            .collect { send(it) }
    }
