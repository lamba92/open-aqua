package io.github.lamba92.openaqua.app.mvi

import co.touchlab.kermit.Logger
import io.github.lamba92.openaqua.app.coroutine.LoggingScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface MVIViewModel<STATE, EVENT, EFFECT> {
    val viewModelScope: LoggingScope
    val state: StateFlow<STATE>
    val effects: SharedFlow<EFFECT>

    fun onEvent(event: EVENT): Job
}

val <STATE, EVENT, EFFECT> MVIViewModel<STATE, EVENT, EFFECT>.viewModelLogger: Logger
    get() = viewModelScope.logger
