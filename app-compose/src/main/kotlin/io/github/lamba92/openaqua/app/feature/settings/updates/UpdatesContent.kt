package io.github.lamba92.openaqua.app.feature.settings.updates

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import io.github.lamba92.openaqua.app.build.OpenAquaBuildInfo
import io.github.lamba92.openaqua.app.di.rememberInstance
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.OpenAquaButton
import io.github.lamba92.openaqua.app.ui.components.OpenAquaOutlinedButton
import io.github.lamba92.openaqua.app.ui.components.OpenAquaVerticalScrollbar
import io.github.lamba92.openaqua.app.ui.components.ScreenHeaderSection
import io.github.lamba92.openaqua.app.ui.components.SectionHeader
import io.github.lamba92.openaqua.app.ui.components.scrollFadingEdge
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.ic_info
import open_aqua.app_compose.generated.resources.ic_refresh
import open_aqua.app_compose.generated.resources.settings_updates_available_desc
import open_aqua.app_compose.generated.resources.settings_updates_available_title
import open_aqua.app_compose.generated.resources.settings_updates_check_button
import open_aqua.app_compose.generated.resources.settings_updates_checking
import open_aqua.app_compose.generated.resources.settings_updates_current_version
import open_aqua.app_compose.generated.resources.settings_updates_download_button
import open_aqua.app_compose.generated.resources.settings_updates_error_generic
import open_aqua.app_compose.generated.resources.settings_updates_error_network
import open_aqua.app_compose.generated.resources.settings_updates_error_rate_limited
import open_aqua.app_compose.generated.resources.settings_updates_release_history_title
import open_aqua.app_compose.generated.resources.settings_updates_retry_button
import open_aqua.app_compose.generated.resources.settings_updates_subtitle
import open_aqua.app_compose.generated.resources.settings_updates_title
import open_aqua.app_compose.generated.resources.settings_updates_up_to_date_desc
import open_aqua.app_compose.generated.resources.settings_updates_view_on_github
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun UpdatesContent(
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier,
    viewModel: UpdatesViewModel = rememberInstance(),
) {
    val updates by viewModel.state.collectAsState()
    UpdatesContent(
        updates = updates,
        onEvent = viewModel::onEvent,
        scrollState = scrollState,
        modifier = modifier,
    )
}

@Composable
fun UpdatesContent(
    updates: UpdatesState,
    onEvent: (UpdatesEvent) -> Unit,
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeaderSection(
                title = stringResource(Res.string.settings_updates_title),
                subtitle = stringResource(Res.string.settings_updates_subtitle),
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .scrollFadingEdge(
                            color = colors.background,
                            scrollState = scrollState,
                            height = 36.dp,
                        ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = metrics.spacingXxl)
                            .padding(top = metrics.spacingLg, bottom = metrics.spacingXxl),
                    verticalArrangement = Arrangement.spacedBy(metrics.spacingXl),
                ) {
                    CurrentVersionStatusSection(
                        updates = updates,
                        onCheckForUpdates = { onEvent(UpdatesEvent.CheckForUpdates) },
                    )

                    if (updates is UpdatesState.UpdateAvailable) {
                        UpdateAvailableHighlightCard(
                            release = updates.latestRelease,
                            onOpenReleaseUrl = { onEvent(UpdatesEvent.OpenReleaseUrl(it)) },
                        )
                    }

                    if (updates is UpdatesState.Error) {
                        UpdateErrorCard(
                            error = updates.error,
                            onRetry = { onEvent(UpdatesEvent.CheckForUpdates) },
                        )
                    }

                    val releases =
                        when (updates) {
                            is UpdatesState.UpToDate -> updates.releases
                            is UpdatesState.UpdateAvailable -> updates.releases
                            is UpdatesState.Error -> updates.cachedReleases
                            else -> emptyList()
                        }

                    if (releases.isNotEmpty()) {
                        HorizontalDivider(color = colors.borderOutline)
                        SectionHeader(
                            icon = painterResource(Res.drawable.ic_info),
                            title = stringResource(Res.string.settings_updates_release_history_title),
                        )
                        releases.forEach { release ->
                            ReleaseHistoryCard(
                                release = release,
                                isLatest = updates is UpdatesState.UpdateAvailable && release == updates.latestRelease,
                                onOpenReleaseUrl = { onEvent(UpdatesEvent.OpenReleaseUrl(it)) },
                            )
                        }
                    }
                }

                OpenAquaVerticalScrollbar(
                    scrollState = scrollState,
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun CurrentVersionStatusSection(
    updates: UpdatesState,
    onCheckForUpdates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceLow)
                .border(1.dp, colors.borderOutline, RoundedCornerShape(12.dp))
                .padding(metrics.spacingLg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(metrics.spacingXs)) {
                Text(
                    text = stringResource(Res.string.settings_updates_current_version, OpenAquaBuildInfo.VERSION),
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when (updates) {
                        UpdatesState.Idle -> {
                            Text(
                                text = "-",
                                color = colors.textMuted,
                                fontSize = 13.sp,
                            )
                        }

                        UpdatesState.Checking -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = colors.primaryBright,
                            )
                            Text(
                                text = stringResource(Res.string.settings_updates_checking),
                                color = colors.textSecondary,
                                fontSize = 13.sp,
                            )
                        }

                        is UpdatesState.UpToDate -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = colors.statusNominal,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(Res.string.settings_updates_up_to_date_desc),
                                color = colors.statusNominal,
                                fontSize = 13.sp,
                            )
                        }

                        is UpdatesState.UpdateAvailable -> {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = colors.primaryBright,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(Res.string.settings_updates_available_title),
                                color = colors.primaryBright,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }

                        is UpdatesState.Error -> {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = colors.statusWarning,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = getUpdateErrorMessage(updates.error),
                                color = colors.statusWarning,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            OpenAquaButton(
                text = stringResource(Res.string.settings_updates_check_button),
                onClick = onCheckForUpdates,
                isLoading = updates is UpdatesState.Checking,
                loadingText = stringResource(Res.string.settings_updates_checking),
                icon = painterResource(Res.drawable.ic_refresh),
            )
        }
    }
}

@Composable
private fun getUpdateErrorMessage(error: UpdateError): String =
    when (error) {
        UpdateError.Network -> stringResource(Res.string.settings_updates_error_network)
        UpdateError.RateLimited -> stringResource(Res.string.settings_updates_error_rate_limited)
        UpdateError.Generic -> stringResource(Res.string.settings_updates_error_generic)
    }

@Composable
private fun UpdateAvailableHighlightCard(
    release: GitHubRelease,
    onOpenReleaseUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.primary.copy(alpha = 0.12f))
                .border(1.dp, colors.primaryBright.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(metrics.spacingXl),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = metrics.spacingLg),
                verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
            ) {
                Text(
                    text = stringResource(Res.string.settings_updates_available_title),
                    color = colors.primaryBright,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(Res.string.settings_updates_available_desc, release.tagName),
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                )
            }

            OpenAquaButton(
                text = stringResource(Res.string.settings_updates_download_button),
                onClick = { onOpenReleaseUrl(release.htmlUrl) },
            )
        }
    }
}

@Composable
private fun UpdateErrorCard(
    error: UpdateError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.statusWarning.copy(alpha = 0.12f))
                .border(1.dp, colors.statusWarning.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(metrics.spacingLg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = metrics.spacingLg),
                horizontalArrangement = Arrangement.spacedBy(metrics.spacingMd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = colors.statusWarning,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = getUpdateErrorMessage(error),
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                )
            }

            OpenAquaOutlinedButton(
                text = stringResource(Res.string.settings_updates_retry_button),
                onClick = onRetry,
            )
        }
    }
}

@Composable
private fun ReleaseHistoryCard(
    release: GitHubRelease,
    isLatest: Boolean,
    onOpenReleaseUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceLow)
                .border(1.dp, colors.borderOutline, RoundedCornerShape(10.dp))
                .padding(metrics.spacingLg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(metrics.spacingMd)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(metrics.spacingMd),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = release.tagName,
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    if (release.displayName != release.tagName) {
                        Text(
                            text = release.displayName,
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                        )
                    }
                    if (isLatest) {
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.primaryBright.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "LATEST",
                                color = colors.primaryBright,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    release.publishedAt?.take(10)?.let { date ->
                        Text(
                            text = date,
                            color = colors.textMuted,
                            fontSize = 12.sp,
                        )
                    }
                }

                OpenAquaOutlinedButton(
                    text = stringResource(Res.string.settings_updates_view_on_github),
                    onClick = { onOpenReleaseUrl(release.htmlUrl) },
                )
            }

            if (!release.body.isNullOrBlank()) {
                HorizontalDivider(color = colors.borderOutline)
                Markdown(
                    content = release.body.trim(),
                    colors =
                        markdownColor(
                            text = colors.textSecondary,
                            codeBackground = colors.surfaceHigh,
                            inlineCodeBackground = colors.surfaceHigh,
                            dividerColor = colors.borderOutline,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
