package io.github.lamba92.openaqua.app.feature.bugreport

import io.github.lamba92.openaqua.app.build.OpenAquaBuildInfo
import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.LoggingScope
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.logging.AppLogService
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.mvi.viewModelLogger
import io.github.lamba92.openaqua.app.util.openUrlInBrowser
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val GITHUB_REPO_URL = "https://github.com/lamba92/open-aqua"

private const val ISSUE_BODY_TEMPLATE = """### Environment
- **OpenAqua Version:** %OpenAqua Version%
- **Build Date:** %Build Date%
- **OS:** %OS%
- **JVM:** %JVM%
- **Kotlin:** %Kotlin%

### Description
<!-- A clear and concise description of what the bug is. -->

### Steps to Reproduce
1. 
2. 
3. 

### Expected Behavior
<!-- What you expected to happen. -->

#### attach logs and/or screenshots please
<!-- Drag and drop open-aqua.log and/or screenshots here -->"""

class BugReportViewModel(
    private val appLogService: AppLogService,
    scopeProvider: CoroutineScopeProvider,
) : MVIViewModel<BugReportState, BugReportEvent, BugReportEffect> {
    override val viewModelScope: LoggingScope = scopeProvider.getScope()

    private val defaultBuildInfo =
        listOf(
            "OpenAqua Version" to OpenAquaBuildInfo.VERSION,
            "Build Date" to OpenAquaBuildInfo.BUILD_TIMESTAMP,
            "OS" to "${OpenAquaBuildInfo.OS_NAME} (${OpenAquaBuildInfo.OS_ARCH})",
            "JVM" to "${OpenAquaBuildInfo.JVM_VERSION} (${OpenAquaBuildInfo.JVM_VENDOR})",
            "Kotlin" to OpenAquaBuildInfo.KOTLIN_VERSION,
        )

    override val state: StateFlow<BugReportState>
        field = MutableStateFlow(BugReportState(buildInfo = defaultBuildInfo))

    override val effects: SharedFlow<BugReportEffect>
        field = MutableSharedFlow()

    private val issueUrl: String by lazy { createIssueUrl(defaultBuildInfo) }

    override fun onEvent(event: BugReportEvent): Job =
        viewModelScope.launch {
            when (event) {
                BugReportEvent.OpenGitHubIssue -> {
                    viewModelLogger.i { "Opening GitHub issue in browser" }
                    runCatching {
                        openUrlInBrowser(issueUrl)
                    }.onFailure { e ->
                        viewModelLogger.e(e) { "Failed to open GitHub issue URL in browser: $issueUrl" }
                    }
                }

                BugReportEvent.OpenLogsFolder -> {
                    viewModelLogger.i { "Opening logs folder" }
                    appLogService.openLogsFolder()
                }

                BugReportEvent.ViewLogs -> {
                    viewModelLogger.i { "Navigating to logs tab" }
                    effects.emit(BugReportEffect.NavigateToLogs)
                }
            }
        }

    companion object {
        fun createIssueUrl(buildInfo: List<Pair<String, String>>): String {
            val replacements = buildInfo.associate { (label, value) -> "%$label%" to value }
            val populatedBody =
                replacements.entries.fold(ISSUE_BODY_TEMPLATE) { acc, (key, value) ->
                    acc.replace(key, value)
                }
            val encodedBody = URLEncoder.encode(populatedBody, StandardCharsets.UTF_8.name()).replace("+", "%20")
            return "$GITHUB_REPO_URL/issues/new?body=$encodedBody"
        }
    }
}
