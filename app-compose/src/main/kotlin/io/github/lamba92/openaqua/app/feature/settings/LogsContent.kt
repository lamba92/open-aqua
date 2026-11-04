package io.github.lamba92.openaqua.app.feature.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.OpenAquaOutlinedButton
import io.github.lamba92.openaqua.app.ui.components.ScreenHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.settings_logs_clear
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

@Composable
fun LogsContent(
    logs: List<String>,
    onClearLogs: () -> Unit,
    onOpenLogsFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    val coroutineScope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Header
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = metrics.spacingXxl)
                        .padding(top = metrics.spacingXxl, bottom = metrics.spacingLg),
                verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
            ) {
                ScreenHeader(
                    title = stringResource(Res.string.settings_logs_title),
                    subtitle = stringResource(Res.string.settings_logs_subtitle),
                )
            }

            HorizontalDivider(
                color = colors.borderOutline,
                modifier = Modifier.padding(horizontal = metrics.spacingXxl),
            )
        }

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
                    text = stringResource(Res.string.settings_logs_clear),
                    onClick = onClearLogs,
                    icon = rememberVectorPainter(Icons.Outlined.DeleteOutline),
                )

                OpenAquaOutlinedButton(
                    text = stringResource(Res.string.settings_logs_open_folder),
                    onClick = onOpenLogsFolder,
                    icon = rememberVectorPainter(Icons.Outlined.FolderOpen),
                )

                OpenAquaOutlinedButton(
                    text =
                        if (isCopied) {
                            stringResource(Res.string.settings_logs_copied)
                        } else {
                            stringResource(Res.string.settings_logs_copy)
                        },
                    onClick = {
                        if (logs.isNotEmpty()) {
                            val text = logs.joinToString("\n")
                            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
                            isCopied = true
                            coroutineScope.launch {
                                delay(2000)
                                isCopied = false
                            }
                        }
                    },
                    enabled = logs.isNotEmpty(),
                    icon =
                        rememberVectorPainter(
                            if (isCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
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
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(logs) { line ->
                                val textColor =
                                    when {
                                        line.contains("[ERROR") -> colors.statusCritical
                                        line.contains("[WARN") -> colors.statusWarning
                                        line.contains("[INFO") -> colors.textPrimary
                                        line.contains("[DEBUG") -> colors.textSecondary.copy(alpha = 0.85f)
                                        else -> colors.textSecondary
                                    }

                                Text(
                                    text = line,
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
