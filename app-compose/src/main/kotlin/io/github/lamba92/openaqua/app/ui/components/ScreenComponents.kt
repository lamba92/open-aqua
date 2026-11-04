package io.github.lamba92.openaqua.app.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.model.SensorTargetType
import io.github.lamba92.openaqua.app.theme.ChannelDetailHeaderColors
import io.github.lamba92.openaqua.app.theme.ChannelDetailHeaderMetrics
import io.github.lamba92.openaqua.app.theme.FeaturePlaceholderBoxColors
import io.github.lamba92.openaqua.app.theme.FeaturePlaceholderBoxMetrics
import io.github.lamba92.openaqua.app.theme.LocalChannelDetailHeaderColors
import io.github.lamba92.openaqua.app.theme.LocalChannelDetailHeaderMetrics
import io.github.lamba92.openaqua.app.theme.LocalFeaturePlaceholderBoxColors
import io.github.lamba92.openaqua.app.theme.LocalFeaturePlaceholderBoxMetrics
import io.github.lamba92.openaqua.app.theme.LocalMetricCardColors
import io.github.lamba92.openaqua.app.theme.LocalMetricCardMetrics
import io.github.lamba92.openaqua.app.theme.LocalScreenHeaderColors
import io.github.lamba92.openaqua.app.theme.LocalScreenHeaderMetrics
import io.github.lamba92.openaqua.app.theme.LocalSectionHeaderColors
import io.github.lamba92.openaqua.app.theme.LocalSectionHeaderMetrics
import io.github.lamba92.openaqua.app.theme.MetricCardColors
import io.github.lamba92.openaqua.app.theme.MetricCardMetrics
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.theme.ScreenHeaderColors
import io.github.lamba92.openaqua.app.theme.ScreenHeaderMetrics
import io.github.lamba92.openaqua.app.theme.SectionHeaderColors
import io.github.lamba92.openaqua.app.theme.SectionHeaderMetrics
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.ic_info
import open_aqua.app_compose.generated.resources.ic_tune
import open_aqua.app_compose.generated.resources.sensor_options
import open_aqua.app_compose.generated.resources.sensor_type_die
import open_aqua.app_compose.generated.resources.sensor_type_set
import open_aqua.app_compose.generated.resources.sensor_type_water
import open_aqua.app_compose.generated.resources.visibility_filter_hidden_count
import open_aqua.app_compose.generated.resources.visibility_filter_hide_inactive
import open_aqua.app_compose.generated.resources.visibility_show_all_action
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun Modifier.scrollFadingEdge(
    color: Color,
    scrollState: ScrollState,
    height: Dp = 32.dp,
): Modifier =
    this.drawWithContent {
        drawContent()
        val scrollPx = scrollState.value.toFloat()
        val fadeHeightPx = height.toPx()
        if (fadeHeightPx > 0f) {
            val alpha = (scrollPx / (fadeHeightPx / 2f)).coerceIn(0f, 1f)
            if (alpha > 0f) {
                drawRect(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                            startY = 0f,
                            endY = fadeHeightPx,
                        ),
                )
            }
        }
    }

object ScreenHeaderDefaults {
    @Composable
    fun colors(): ScreenHeaderColors = LocalScreenHeaderColors.current

    @Composable
    fun metrics(): ScreenHeaderMetrics = LocalScreenHeaderMetrics.current
}

object SectionHeaderDefaults {
    @Composable
    fun colors(): SectionHeaderColors = LocalSectionHeaderColors.current

    @Composable
    fun metrics(): SectionHeaderMetrics = LocalSectionHeaderMetrics.current
}

object ChannelDetailHeaderDefaults {
    @Composable
    fun colors(): ChannelDetailHeaderColors = LocalChannelDetailHeaderColors.current

    @Composable
    fun metrics(): ChannelDetailHeaderMetrics = LocalChannelDetailHeaderMetrics.current
}

object FeaturePlaceholderBoxDefaults {
    @Composable
    fun colors(): FeaturePlaceholderBoxColors = LocalFeaturePlaceholderBoxColors.current

    @Composable
    fun metrics(): FeaturePlaceholderBoxMetrics = LocalFeaturePlaceholderBoxMetrics.current
}

object MetricCardDefaults {
    @Composable
    fun colors(): MetricCardColors = LocalMetricCardColors.current

    @Composable
    fun metrics(): MetricCardMetrics = LocalMetricCardMetrics.current
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    colors: ScreenHeaderColors = ScreenHeaderDefaults.colors(),
    metrics: ScreenHeaderMetrics = ScreenHeaderDefaults.metrics(),
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(metrics.verticalSpacing),
        ) {
            Text(
                text = title,
                color = colors.title,
                fontSize = metrics.titleTextSize,
                lineHeight = metrics.titleLineHeight,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = colors.subtitle,
                fontSize = metrics.subtitleTextSize,
                lineHeight = metrics.subtitleLineHeight,
            )
        }
        trailingContent?.invoke(this)
    }
}

@Composable
fun ChannelPageScaffold(
    title: String,
    subtitle: String,
    selectorHeader: String,
    detailContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    selectorHeaderTrailing: (@Composable RowScope.() -> Unit)? = null,
    selectorContent: @Composable () -> Unit,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
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
                    title = title,
                    subtitle = subtitle,
                )
            }

            HorizontalDivider(
                color = colors.borderOutline,
                modifier = Modifier.padding(horizontal = metrics.spacingXxl),
            )
        }

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
                        .padding(top = metrics.spacingLg, bottom = metrics.spacingXxl),
                verticalArrangement = Arrangement.spacedBy(metrics.spacingXl),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(metrics.spacingSm),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = metrics.spacingXxl),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = selectorHeader,
                            color = colors.primary,
                            fontSize = metrics.channelSelectionTile.badgeTextSize,
                            lineHeight = metrics.channelSelectionTile.badgeLineHeight,
                            fontWeight = FontWeight.Bold,
                            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                        )
                        selectorHeaderTrailing?.invoke(this)
                    }

                    FlowRow(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = metrics.spacingXxl),
                        horizontalArrangement = Arrangement.spacedBy(metrics.spacingMd),
                        verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
                        content = { selectorContent() },
                    )
                }

                HorizontalDivider(
                    color = colors.borderOutline,
                    modifier = Modifier.padding(horizontal = metrics.spacingXxl),
                )

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = metrics.spacingXxl),
                ) {
                    detailContent()
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

@Composable
fun StandardScreenScaffold(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) },
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = metrics.spacingXxl)
                            .padding(top = metrics.spacingXxl, bottom = metrics.spacingLg),
                    verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
                ) {
                    ScreenHeader(
                        title = title,
                        subtitle = subtitle,
                    )
                }

                HorizontalDivider(
                    color = colors.borderOutline,
                    modifier = Modifier.padding(horizontal = metrics.spacingXxl),
                )
            }

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
                    content = content,
                )

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
fun ScreenCard(
    modifier: Modifier = Modifier,
    padding: Dp = OpenAquaTheme.metrics.spacingXl,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    val shape = RoundedCornerShape(metrics.radiusLarge)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surfaceLow)
                .border(metrics.strokeThin, colors.borderOutline, shape)
                .padding(padding),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
        content = content,
    )
}

@Composable
fun VisibilityFilterChip(
    hiddenCount: Int,
    showingAll: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    val shape = RoundedCornerShape(metrics.radiusSmall)
    val text =
        if (showingAll) {
            stringResource(Res.string.visibility_filter_hide_inactive)
        } else {
            stringResource(Res.string.visibility_filter_hidden_count, hiddenCount)
        }
    val borderColor = if (showingAll) colors.primaryBright else colors.borderOutline
    val containerColor = if (showingAll) colors.surfaceHigh else colors.surfaceLowest
    val textColor = if (showingAll) colors.primaryBright else colors.textSecondary

    Row(
        modifier =
            modifier
                .clip(shape)
                .background(containerColor)
                .border(metrics.strokeThin, borderColor, shape)
                .clickable { onClick() }
                .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (showingAll) colors.statusWarning else colors.primary),
        )
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
        )
    }
}

@Composable
fun VisibilityEmptyState(
    message: String,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(metrics.radiusLarge))
                .background(colors.surfaceLow)
                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusLarge))
                .padding(metrics.spacingXxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_info),
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = message,
            color = colors.textSecondary,
            fontSize = 16.sp,
        )
        OpenAquaOutlinedButton(
            text = stringResource(Res.string.visibility_show_all_action),
            icon = painterResource(Res.drawable.ic_tune),
            onClick = onShowAll,
        )
    }
}

@Composable
fun SectionHeader(
    icon: Painter,
    title: String,
    modifier: Modifier = Modifier,
    colors: SectionHeaderColors = SectionHeaderDefaults.colors(),
    metrics: SectionHeaderMetrics = SectionHeaderDefaults.metrics(),
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(metrics.horizontalSpacing),
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = colors.iconTint,
            modifier = Modifier.size(metrics.iconSize),
        )
        Text(
            text = title,
            color = colors.title,
            fontSize = metrics.titleTextSize,
            lineHeight = metrics.titleLineHeight,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ChannelDetailHeader(
    icon: Painter,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    colors: ChannelDetailHeaderColors = ChannelDetailHeaderDefaults.colors(),
    metrics: ChannelDetailHeaderMetrics = ChannelDetailHeaderDefaults.metrics(),
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(metrics.horizontalSpacing),
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = colors.iconTint,
                modifier = Modifier.size(metrics.iconSize),
            )
            Column(verticalArrangement = Arrangement.spacedBy(metrics.verticalSpacing)) {
                Text(
                    text = title,
                    color = colors.title,
                    fontSize = metrics.titleTextSize,
                    lineHeight = metrics.titleLineHeight,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    color = colors.subtitle,
                    fontSize = metrics.subtitleTextSize,
                    lineHeight = metrics.subtitleLineHeight,
                )
            }
        }
        trailingContent?.invoke(this)
    }
}

@Composable
fun FeaturePlaceholderBox(
    icon: Painter,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    colors: FeaturePlaceholderBoxColors = FeaturePlaceholderBoxDefaults.colors(),
    metrics: FeaturePlaceholderBoxMetrics = FeaturePlaceholderBoxDefaults.metrics(),
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(metrics.cornerRadius))
                .background(colors.container)
                .border(metrics.borderWidth, colors.border, RoundedCornerShape(metrics.cornerRadius))
                .padding(metrics.padding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(metrics.verticalSpacing),
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = colors.iconTint,
                modifier = Modifier.size(metrics.iconSize),
            )
            Text(
                text = title,
                color = colors.title,
                fontSize = metrics.titleTextSize,
                lineHeight = metrics.titleLineHeight,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                color = colors.description,
                fontSize = metrics.descriptionTextSize,
                lineHeight = metrics.descriptionLineHeight,
                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
            )
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    width: Dp = MetricCardDefaults.metrics().width,
    colors: MetricCardColors = MetricCardDefaults.colors(),
    metrics: MetricCardMetrics = MetricCardDefaults.metrics(),
) {
    Column(
        modifier =
            modifier
                .width(width)
                .clip(RoundedCornerShape(metrics.cornerRadius))
                .background(colors.container)
                .border(metrics.borderWidth, colors.border, RoundedCornerShape(metrics.cornerRadius))
                .padding(metrics.padding),
        verticalArrangement = Arrangement.spacedBy(metrics.verticalSpacing),
    ) {
        Text(
            text = label,
            color = colors.label,
            fontSize = metrics.labelTextSize,
            lineHeight = metrics.labelLineHeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = colors.value,
            fontSize = metrics.valueTextSize,
            lineHeight = metrics.valueLineHeight,
            fontWeight = FontWeight.Bold,
            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SensorTargetTypeMenuButton(
    targetType: SensorTargetType,
    onSetTargetType: (SensorTargetType) -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 24.dp,
    iconSize: Dp = 20.dp,
) {
    val colors = OpenAquaTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .size(buttonSize)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (menuExpanded) colors.surfaceContainer else Color.Transparent)
                    .clickable { menuExpanded = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(Res.string.sensor_options),
                tint = if (menuExpanded) colors.primaryBright else colors.textMuted,
                modifier = Modifier.size(iconSize),
            )
        }

        OpenAquaDropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            OpenAquaDropdownHeader(
                text = stringResource(Res.string.sensor_type_set),
            )
            SensorTargetType.entries.forEach { type ->
                val isSelected = type == targetType
                OpenAquaDropdownMenuItem(
                    text = {
                        Text(
                            text =
                                when (type) {
                                    SensorTargetType.DIE -> stringResource(Res.string.sensor_type_die)
                                    SensorTargetType.WATER -> stringResource(Res.string.sensor_type_water)
                                },
                            fontSize = 15.sp,
                            color = if (isSelected) colors.primaryBright else colors.textPrimary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = colors.primaryBright,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            Spacer(modifier = Modifier.size(16.dp))
                        }
                    },
                    onClick = {
                        onSetTargetType(type)
                        menuExpanded = false
                    },
                    isSelected = isSelected,
                )
            }
        }
    }
}
