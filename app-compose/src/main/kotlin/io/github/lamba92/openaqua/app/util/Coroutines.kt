package io.github.lamba92.openaqua.app.util

import io.github.lamba92.openaqua.model.retryAtLeast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Emits the latest value from [this] flow, dropping intermediate values, with at least [period]
 * between emissions.
 */
fun <T> Flow<T>.throttleLatest(period: Duration): Flow<T> =
    flow {
        var lastEmission = 0L
        collect { value ->
            val now = System.currentTimeMillis()
            val timeSinceLast = now - lastEmission
            if (timeSinceLast >= period.inWholeMilliseconds) {
                lastEmission = now
                emit(value)
            } else {
                delay(period.inWholeMilliseconds - timeSinceLast)
                lastEmission = System.currentTimeMillis()
                emit(value)
            }
        }
    }

/**
 * Dynamically combines a list of items into a single combined list of child flow outputs,
 * maintaining item lifecycles based on their unique keys.
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

        suspend fun updateDownstreamEmission() {
            val snapshot =
                synchronized(latestListLock) {
                    currentItems.mapNotNull { item ->
                        val key = keySelector(item)
                        activeChildren[key]?.stateFlow?.value
                    }
                }
            if (snapshot.size == currentItems.size) {
                send(snapshot)
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
                                        updateDownstreamEmission()
                                    }
                            }
                        activeChildren[key] = ChildFlowEntry(item, job, stateFlow)
                    }
                }
            }

            if (newItems.isEmpty()) {
                send(emptyList())
            } else {
                updateDownstreamEmission()
            }
        }
    }
