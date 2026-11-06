package io.github.lamba92.openaqua.app.feature.permission

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.LoggingScope
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.mvi.viewModelLogger
import io.github.lamba92.openaqua.app.permission.DevicePermissionService
import io.github.lamba92.openaqua.app.permission.DevicePermissionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class PermissionViewModel(
    private val permissionService: DevicePermissionService,
    scopeProvider: CoroutineScopeProvider,
) : MVIViewModel<PermissionState, PermissionEvent, PermissionEffect> {
    override val viewModelScope: LoggingScope = scopeProvider.getScope()

    override val state: StateFlow<PermissionState>
        field = MutableStateFlow(PermissionState())

    override val effects: SharedFlow<PermissionEffect>
        field = MutableSharedFlow()

    init {
        permissionService.state
            .onEach { permState ->
                state.update {
                    it.copy(
                        permissionState = permState,
                        isRequesting = if (permState is DevicePermissionState.Granted) false else it.isRequesting,
                    )
                }
            }.launchIn(viewModelScope)
    }

    override fun onEvent(event: PermissionEvent): Job =
        viewModelScope.launch {
            when (event) {
                PermissionEvent.RequestPermissions -> requestPermissions()
            }
        }

    private fun requestPermissions() {
        viewModelLogger.i { "Requesting device permissions" }
        state.update { it.copy(isRequesting = true) }
        permissionService.requestPermissions()
        viewModelScope.launch {
            delay(2.seconds)
            state.update { it.copy(isRequesting = false) }
        }
    }
}
