package io.github.lamba92.openaqua.app.feature.settings.updates

import io.github.lamba92.openaqua.app.build.OpenAquaBuildInfo
import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.LoggingScope
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.platform.platformHttpClientEngine
import io.github.lamba92.openaqua.app.util.openUrlInBrowser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class AppUpdateService(
    scopeProvider: CoroutineScopeProvider,
    private val httpClient: HttpClient = getDefaultClient(),
) : AutoCloseable {
    companion object {
        fun getDefaultClient() =
            HttpClient(platformHttpClientEngine) {
                expectSuccess = false
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                        },
                    )
                }
            }
    }

    private val serviceScope: LoggingScope =
        scopeProvider.getScope { _, throwable ->
            state.update { current ->
                val cached =
                    when (current) {
                        is UpdatesState.UpToDate -> current.releases
                        is UpdatesState.UpdateAvailable -> current.releases
                        is UpdatesState.Error -> current.cachedReleases
                        else -> emptyList()
                    }
                UpdatesState.Error(
                    error = throwable.toUpdateError(),
                    cachedReleases = cached,
                )
            }
        }

    val state: StateFlow<UpdatesState>
        field = MutableStateFlow<UpdatesState>(UpdatesState.Idle)

    init {
        checkForUpdates()
    }

    fun checkForUpdates(): Job =
        serviceScope.launch {
            state.update { UpdatesState.Checking }
            val releases = getReleases()
            val currentSemVer = SemVer.parseOrNull(OpenAquaBuildInfo.VERSION)
            val nonDraftReleases = releases.filter { !it.draft }
            val candidate = nonDraftReleases.firstOrNull { !it.prerelease } ?: nonDraftReleases.firstOrNull()

            val isUpdateAvailable =
                if (candidate != null && currentSemVer != null) {
                    val candidateSemVer = candidate.semVer
                    candidateSemVer != null && candidateSemVer > currentSemVer
                } else {
                    false
                }

            val newState =
                if (isUpdateAvailable && candidate != null) {
                    UpdatesState.UpdateAvailable(
                        currentVersion = OpenAquaBuildInfo.VERSION,
                        latestRelease = candidate,
                        releases = nonDraftReleases,
                    )
                } else {
                    UpdatesState.UpToDate(
                        currentVersion = OpenAquaBuildInfo.VERSION,
                        releases = nonDraftReleases,
                    )
                }
            state.update { newState }
        }

    suspend fun getReleases(): List<GitHubRelease> {
        val response =
            httpClient.get("https://api.github.com/repos/lamba92/open-aqua/releases") {
                header(HttpHeaders.UserAgent, "OpenAqua/${OpenAquaBuildInfo.VERSION}")
                header(HttpHeaders.Accept, "application/vnd.github+json")
            }
        if (response.status == HttpStatusCode.Forbidden || response.status == HttpStatusCode.TooManyRequests) {
            throw UpdateException.RateLimited()
        }
        if (response.status != HttpStatusCode.OK) {
            throw UpdateException.ServerError(response.status)
        }
        return response.body<List<GitHubRelease>>()
    }

    fun openReleaseInBrowser(url: String) {
        openUrlInBrowser(url)
    }

    override fun close() {
        httpClient.close()
    }
}
