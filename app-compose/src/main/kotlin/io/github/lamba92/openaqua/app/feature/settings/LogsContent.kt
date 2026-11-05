package io.github.lamba92.openaqua.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.touchlab.kermit.Severity
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.OpenAquaOutlinedButton
import io.github.lamba92.openaqua.app.ui.components.ScreenHeaderSection
import io.github.lamba92.openaqua.theme.AppColorScheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.settings_logs_copied
import open_aqua.app_compose.generated.resources.settings_logs_copy
import open_aqua.app_compose.generated.resources.settings_logs_empty
import open_aqua.app_compose.generated.resources.settings_logs_lines_count
import open_aqua.app_compose.generated.resources.settings_logs_open_folder
import open_aqua.app_compose.generated.resources.settings_logs_subtitle
import open_aqua.app_compose.generated.resources.settings_logs_title
import org.jetbrains.compose.resources.stringResource
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlin.time.Duration.Companion.seconds

@Composable
fun LogsContent(
    logs: List<LogLine>,
    onOpenLogsFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    val coroutineScope = rememberCoroutineScope()
    var hasJustCupiedMomentAgo by remember { mutableStateOf(false) }

    val debugColor =
        if (colors.palette.colorScheme == AppColorScheme.SOLAR_AMBER) {
            colors.secondary
        } else {
            colors.accent
        }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        ScreenHeaderSection(
            title = stringResource(Res.string.settings_logs_title),
            subtitle = stringResource(Res.string.settings_logs_subtitle),
        )

        // Action bar and log viewer container
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = metrics.spacingXxl, vertical = metrics.spacingLg),
            verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
        ) {
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(metrics.spacingMd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OpenAquaOutlinedButton(
                    text = stringResource(Res.string.settings_logs_open_folder),
                    onClick = onOpenLogsFolder,
                    icon = rememberVectorPainter(Icons.Outlined.FolderOpen),
                )

                OpenAquaOutlinedButton(
                    text =
                        if (hasJustCupiedMomentAgo) {
                            stringResource(Res.string.settings_logs_copied)
                        } else {
                            stringResource(Res.string.settings_logs_copy)
                        },
                    onClick = {
                        if (logs.isNotEmpty()) {
                            val text = logs.joinToString("\n") { it.text }
                            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
                            hasJustCupiedMomentAgo = true
                            coroutineScope.launch {
                                delay(2.seconds)
                                hasJustCupiedMomentAgo = false
                            }
                        }
                    },
                    enabled = logs.isNotEmpty() && !hasJustCupiedMomentAgo,
                    icon =
                        rememberVectorPainter(
                            if (hasJustCupiedMomentAgo) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        ),
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(Res.string.settings_logs_lines_count, logs.size),
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Log Console Box
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, colors.borderOutline, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                color = colors.surfaceLow,
            ) {
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_logs_empty),
                            color = colors.textSecondary,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                } else {
                    val lazyListState = rememberLazyListState()

                    LaunchedEffect(logs.size) {
                        if (logs.isNotEmpty()) {
                            lazyListState.scrollToItem(logs.size - 1)
                        }
                    }

                    SelectionContainer(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp),
                        ) {
                            itemsIndexed(logs) { index, logLine ->
                                val rowBackground =
                                    when (logLine.severity) {
                                        Severity.Error,
                                        Severity.Assert,
                                        -> {
                                            colors.statusCritical.copy(alpha = if (index % 2 == 1) 0.20f else 0.14f)
                                        }

                                        Severity.Warn -> {
                                            colors.statusWarning.copy(alpha = if (index % 2 == 1) 0.20f else 0.14f)
                                        }

                                        Severity.Debug -> {
                                            debugColor.copy(alpha = if (index % 2 == 1) 0.20f else 0.14f)
                                        }

                                        Severity.Info,
                                        Severity.Verbose,
                                        null,
                                        -> {
                                            if (index % 2 == 1) colors.surfaceLowest else Color.Transparent
                                        }
                                    }

                                val textColor =
                                    when (logLine.severity) {
                                        Severity.Error,
                                        Severity.Assert,
                                        -> colors.statusCritical

                                        Severity.Warn -> colors.statusWarning

                                        Severity.Debug -> debugColor

                                        Severity.Info -> colors.textPrimary

                                        Severity.Verbose,
                                        null,
                                        -> colors.textSecondary
                                    }

                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .background(rowBackground)
                                            .padding(horizontal = 14.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = logLine.text,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp,
                                        color = textColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
