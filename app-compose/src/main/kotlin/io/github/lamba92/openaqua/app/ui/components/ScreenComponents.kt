package io.github.lamba92.openaqua.app.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

fun Modifier.horizontalScrollFadingEdge(
    color: Color,
    scrollState: ScrollState,
    width: Dp = 24.dp,
): Modifier =
    this.drawWithContent {
        drawContent()
        val fadeWidthPx = width.toPx()
        if (fadeWidthPx > 0f) {
            val leftAlpha = (scrollState.value.toFloat() / (fadeWidthPx / 2f)).coerceIn(0f, 1f)
            val maxScroll = scrollState.maxValue.toFloat()
            val rightAlpha =
                if (maxScroll > 0f) {
                    ((maxScroll - scrollState.value.toFloat()) / (fadeWidthPx / 2f)).coerceIn(0f, 1f)
                } else {
                    0f
                }

            if (leftAlpha > 0f) {
                drawRect(
                    brush =
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = leftAlpha), Color.Transparent),
                            startX = 0f,
                            endX = fadeWidthPx,
                        ),
                )
            }
            if (rightAlpha > 0f) {
                drawRect(
                    brush =
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, color.copy(alpha = rightAlpha)),
                            startX = size.width - fadeWidthPx,
                            endX = size.width,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChannelPageScaffold(
    title: String,
    subtitle: String,
    selectorHeader: String,
    detailContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selectorContent: @Composable () -> Unit,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    val verticalScrollState = rememberScrollState()

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
                        scrollState = verticalScrollState,
                        height = 36.dp,
                    ),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                        .padding(top = metrics.spacingLg, bottom = metrics.spacingXxl),
                verticalArrangement = Arrangement.spacedBy(metrics.spacingXl),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(metrics.spacingSm),
                ) {
                    Text(
                        text = selectorHeader,
                        color = colors.primary,
                        fontSize = metrics.channelSelectionTile.badgeTextSize,
                        lineHeight = metrics.channelSelectionTile.badgeLineHeight,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                        modifier = Modifier.padding(horizontal = metrics.spacingXxl),
                    )

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
        }
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
        )
        Text(
            text = value,
            color = colors.value,
            fontSize = metrics.valueTextSize,
            lineHeight = metrics.valueLineHeight,
            fontWeight = FontWeight.Bold,
            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
        )
    }
}
