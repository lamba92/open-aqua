package io.github.lamba92.openaqua.app.feature.bugreport

data class BugReportState(
    val buildInfo: List<Pair<String, String>>,
)

sealed interface BugReportEvent {
    data object OpenGitHubIssue : BugReportEvent

    data object OpenLogsFolder : BugReportEvent

    data object ViewLogs : BugReportEvent
}

sealed interface BugReportEffect {
    data object NavigateToLogs : BugReportEffect
}
