package io.github.lamba92.openaqua.app.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface MVIViewModel<STATE, EVENT, EFFECT> {
    val viewModelScope: CoroutineScope
    val state: StateFlow<STATE>
    val effects: SharedFlow<EFFECT>

    fun onEvent(event: EVENT)
}
