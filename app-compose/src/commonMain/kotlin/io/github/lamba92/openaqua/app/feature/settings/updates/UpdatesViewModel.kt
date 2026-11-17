package io.github.lamba92.openaqua.app.feature.settings.updates

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.LoggingScope
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UpdatesViewModel(
    private val updateService: AppUpdateService,
    scopeProvider: CoroutineScopeProvider,
) : MVIViewModel<UpdatesState, UpdatesEvent, UpdatesEffect> {
    override val viewModelScope: LoggingScope = scopeProvider.getScope()

    override val state: StateFlow<UpdatesState> = updateService.state

    override val effects: SharedFlow<UpdatesEffect>
        field = MutableSharedFlow()

    override fun onEvent(event: UpdatesEvent): Job =
        viewModelScope.launch {
            when (event) {
                UpdatesEvent.CheckForUpdates -> updateService.checkForUpdates()
                is UpdatesEvent.OpenReleaseUrl -> updateService.openReleaseInBrowser(event.url)
            }
        }
}
