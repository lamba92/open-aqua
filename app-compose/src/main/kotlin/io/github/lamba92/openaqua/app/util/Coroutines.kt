package io.github.lamba92.openaqua.app.util

import io.github.lamba92.openaqua.model.retryAtLeast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlin.time.Duration

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
 * ### Timeline Example (`period = 1.seconds`)\n * ```text
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
 *     detectedDevicesFlow
 *         .combineByKey(
 *             keySelector = { it.id },
 *             itemFlow = { descriptor ->
 *                 deviceEngineApi.streamTelemetry(descriptor.id)
 *                     .map { telemetry -> DeviceState.Connected(descriptor, telemetry) }
 *                     .catch { e -> emit(DeviceState.Error(descriptor, e.message ?: "Unknown error")) }
 *                     .onStart { emit(DeviceState.Connecting(descriptor)) }
 *             }
 *         )
 * ```
 *
 * ### Example 2: Dynamic Chat Room Presence Tracking
 * ```kotlin
 * val userCards: Flow<List<UserCard>> =
 *     activeUserIdsFlow
 *         .combineByKey(
 *             keySelector = { it },
 *             itemFlow = { userId ->
 *                 presenceService.observeUserPresence(userId)
 *                     .map { presence -> UserCard(userId, presence.status, presence.avatarUrl) }
 *             }
 *         )
 * ```
 *
 * @param T The type of items in the upstream list.
 * @param K The stable, unique identifier type for each item. Must implement proper equality and hash code.
 * @param R The output type produced for each item by [itemFlow].
 * @param keySelector Extracts the unique key [K] from an item [T].
 * @param itemFlow Creates the reactive [Flow] of [R] for an item [T]. Called exactly once per unique key.
 * @return A [Flow] emitting the updated list of [R] preserving upstream order whenever any child flow emits.
 */
fun <T, K, R> Flow<List<T>>.combineByKey(
    keySelector: (T) -> K,
    itemFlow: (T) -> Flow<R>,
): Flow<List<R>> =
    channelFlow {
        class ChildFlowEntry(
            val item: T,
            val job: Job,
            val stateFlow: MutableStateFlow<R?>,
        )

        val activeChildren = mutableMapOf<K, ChildFlowEntry>()
        val latestListLock = Any()
        var currentItems: List<T> = emptyList()

        fun updateDownstreamEmission() {
            val snapshot =
                synchronized(latestListLock) {
                    currentItems.mapNotNull { item ->
                        val key = keySelector(item)
                        activeChildren[key]?.stateFlow?.value
                    }
                }
            if (snapshot.size == currentItems.size) {
                trySend(snapshot)
            }
        }

        collect { newItems ->
            val newKeys = newItems.map(keySelector).toSet()

            synchronized(latestListLock) {
                currentItems = newItems

                val iterator = activeChildren.iterator()
                while (iterator.hasNext()) {
                    val (key, entry) = iterator.next()
                    if (key !in newKeys) {
                        entry.job.cancel()
                        iterator.remove()
                    }
                }

                for (item in newItems) {
                    val key = keySelector(item)
                    if (key !in activeChildren) {
                        val stateFlow = MutableStateFlow<R?>(null)
                        val job =
                            launch {
                                itemFlow(item)
                                    .retryAtLeast(3)
                                    .collect { value ->
                                        stateFlow.value = value
                                    }
                            }
                        activeChildren[key] = ChildFlowEntry(item, job, stateFlow)
                    }
                }
            }

            if (newItems.isEmpty()) {
                trySend(emptyList())
            } else {
                updateDownstreamEmission()
            }
        }
    }
