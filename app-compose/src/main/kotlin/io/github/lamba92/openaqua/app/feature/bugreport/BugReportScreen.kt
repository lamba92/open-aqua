package io.github.lamba92.openaqua.app.feature.bugreport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.di.rememberInstance
import io.github.lamba92.openaqua.app.feature.about.BuildInfoRow
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.OpenAquaButton
import io.github.lamba92.openaqua.app.ui.components.OpenAquaOutlinedButton
import io.github.lamba92.openaqua.app.ui.components.ScreenCard
import io.github.lamba92.openaqua.app.ui.components.SectionHeader
import io.github.lamba92.openaqua.app.ui.components.StandardScreenScaffold
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.bug_report_attach_hint
import open_aqua.app_compose.generated.resources.bug_report_card_desc
import open_aqua.app_compose.generated.resources.bug_report_card_title
import open_aqua.app_compose.generated.resources.bug_report_environment_title
import open_aqua.app_compose.generated.resources.bug_report_github_button
import open_aqua.app_compose.generated.resources.bug_report_screen_subtitle
import open_aqua.app_compose.generated.resources.bug_report_screen_title
import open_aqua.app_compose.generated.resources.bug_report_view_logs
import open_aqua.app_compose.generated.resources.ic_bug_report
import open_aqua.app_compose.generated.resources.ic_info
import open_aqua.app_compose.generated.resources.settings_logs_open_folder
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun BugReportScreen(
    onNavigateToLogs: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BugReportViewModel = rememberInstance(),
) {
    val state by viewModel.state.collectAsState()
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                BugReportEffect.NavigateToLogs -> onNavigateToLogs()
            }
        }
    }

    StandardScreenScaffold(
        title = stringResource(Res.string.bug_report_screen_title),
        subtitle = stringResource(Res.string.bug_report_screen_subtitle),
        modifier = modifier,
    ) {
        // Action Card
        ScreenCard {
            SectionHeader(
                icon = painterResource(Res.drawable.ic_bug_report),
                title = stringResource(Res.string.bug_report_card_title),
            )

            Text(
                text = stringResource(Res.string.bug_report_card_desc),
                color = colors.textPrimary,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )

            Text(
                text = stringResource(Res.string.bug_report_attach_hint),
                color = colors.primaryBright,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(metrics.spacingMd),
            ) {
                OpenAquaButton(
                    text = stringResource(Res.string.bug_report_github_button),
                    icon = painterResource(Res.drawable.ic_bug_report),
                    onClick = { viewModel.onEvent(BugReportEvent.OpenGitHubIssue) },
                )

                OpenAquaOutlinedButton(
                    text = stringResource(Res.string.settings_logs_open_folder),
                    icon = rememberVectorPainter(Icons.Outlined.FolderOpen),
                    onClick = { viewModel.onEvent(BugReportEvent.OpenLogsFolder) },
                )

                OpenAquaOutlinedButton(
                    text = stringResource(Res.string.bug_report_view_logs),
                    icon = painterResource(Res.drawable.ic_info),
                    onClick = { viewModel.onEvent(BugReportEvent.ViewLogs) },
                )
            }
        }

        // Environment / Build Info Card
        ScreenCard {
            SectionHeader(
                icon = painterResource(Res.drawable.ic_info),
                title = stringResource(Res.string.bug_report_environment_title),
            )

            Column(verticalArrangement = Arrangement.spacedBy(metrics.spacingSm)) {
                state.buildInfo.forEach { (label, value) ->
                    BuildInfoRow(label = label, value = value)
                }
            }
        }
    }
}
