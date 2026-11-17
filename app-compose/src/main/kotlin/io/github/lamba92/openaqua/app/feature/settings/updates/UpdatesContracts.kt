package io.github.lamba92.openaqua.app.feature.settings.updates

sealed interface UpdatesState {
    data object Idle : UpdatesState

    data object Checking : UpdatesState

    data class UpToDate(
        val currentVersion: String,
        val releases: List<GitHubRelease>,
    ) : UpdatesState

    data class UpdateAvailable(
        val currentVersion: String,
        val latestRelease: GitHubRelease,
        val releases: List<GitHubRelease>,
    ) : UpdatesState

    data class Error(
        val error: UpdateError = UpdateError.Generic,
        val cachedReleases: List<GitHubRelease> = emptyList(),
    ) : UpdatesState
}

sealed interface UpdatesEvent {
    data object CheckForUpdates : UpdatesEvent

    @JvmInline
    value class OpenReleaseUrl(
        val url: String,
    ) : UpdatesEvent
}

sealed interface UpdatesEffect
