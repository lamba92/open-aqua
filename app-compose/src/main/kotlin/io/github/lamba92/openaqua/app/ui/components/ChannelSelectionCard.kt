package io.github.lamba92.openaqua.app.ui.components
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.lamba92.openaqua.app.model.SensorTargetType
import io.github.lamba92.openaqua.app.model.color
import io.github.lamba92.openaqua.app.theme.ChannelSelectionTileColors
import io.github.lamba92.openaqua.app.theme.ChannelSelectionTileMetrics
import io.github.lamba92.openaqua.app.theme.LocalChannelSelectionTileColors
import io.github.lamba92.openaqua.app.theme.LocalChannelSelectionTileMetrics
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import java.awt.Toolkit

object ChannelSelectionTileDefaults {
    @Composable
    fun colors(): ChannelSelectionTileColors = LocalChannelSelectionTileColors.current

    @Composable
    fun metrics(): ChannelSelectionTileMetrics = LocalChannelSelectionTileMetrics.current
}

/**
 * Linearly interpolates between two colors in sRGB color space.
 */
fun lerpColor(
    start: Color,
    stop: Color,
    fraction: Float,
): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (stop.red - start.red) * f,
        green = start.green + (stop.green - start.green) * f,
        blue = start.blue + (stop.blue - start.blue) * f,
        alpha = start.alpha + (stop.alpha - start.alpha) * f,
    )
}

/**
 * Maps a temperature reading (assumed 20°C min to 60°C max) smoothly across:
 * Cool Blue (20°C) -> Hydro Teal (30°C) -> Emerald Green (40°C) -> Amber Alert (50°C) -> Coral Red (60°C).
 */
fun sensorTemperatureColor(
    tempCelsius: Double?,
    targetType: SensorTargetType = SensorTargetType.DIE,
): Color = targetType.color(tempCelsius)

@Composable
fun ChannelSelectionCard(
    name: String,
    valueText: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    secondaryValueText: String? = null,
    hasPendingChanges: Boolean = false,
    onDoubleClick: (() -> Unit)? = null,
    fillFraction: Float = 0f,
    fillColor: Color = OpenAquaTheme.colors.primary,
    colors: ChannelSelectionTileColors = ChannelSelectionTileDefaults.colors(),
    metrics: ChannelSelectionTileMetrics = ChannelSelectionTileDefaults.metrics(),
    icon: @Composable () -> Unit,
) {
    val doubleClickInterval =
        remember {
            try {
                (Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval") as? Number)?.toLong() ?: 500L
            } catch (_: Throwable) {
                500L
            }
        }
    var lastClickTime by remember { mutableStateOf(0L) }
    val dotSize = 16.dp
    val dotRadius = dotSize / 2

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopStart,
    ) {
        Box(
            modifier =
                Modifier
                    .width(metrics.width)
                    .height(metrics.height)
                    .clip(RoundedCornerShape(metrics.cornerRadius))
                    .background(if (selected) colors.selectedContainer else colors.unselectedContainer)
                    .drawBehind {
                        val clampedFraction = fillFraction.coerceIn(0f, 1f)
                        if (clampedFraction > 0f) {
                            val fillWidth = size.width * clampedFraction
                            // Soft, diffused ("soffused") gauge fill
                            drawRect(
                                brush =
                                    Brush.horizontalGradient(
                                        colors =
                                            listOf(
                                                fillColor.copy(alpha = 0.22f),
                                                fillColor.copy(alpha = 0.12f),
                                            ),
                                        startX = 0f,
                                        endX = fillWidth,
                                    ),
                                topLeft = Offset.Zero,
                                size = Size(fillWidth, size.height),
                            )
                            // Leading edge highlight line
                            drawLine(
                                color = fillColor.copy(alpha = 0.55f),
                                start = Offset(fillWidth, 0f),
                                end = Offset(fillWidth, size.height),
                                strokeWidth = 2.dp.toPx(),
                            )
                        }
                    }.border(
                        border =
                            if (selected) {
                                BorderStroke(metrics.selectedBorderWidth, colors.selectedBorder)
                            } else {
                                BorderStroke(metrics.unselectedBorderWidth, colors.unselectedBorder)
                            },
                        shape = RoundedCornerShape(metrics.cornerRadius),
                    ).clickable {
                        onClick()
                        if (onDoubleClick != null) {
                            val now = System.currentTimeMillis()
                            if (now - lastClickTime <= doubleClickInterval) {
                                lastClickTime = 0L
                                onDoubleClick()
                            } else {
                                lastClickTime = now
                            }
                        }
                    }.padding(horizontal = metrics.padding, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Icon on the left
                Box(
                    modifier = Modifier.size(metrics.iconSize),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }

                // Title on top, Value below it
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = name,
                            color = if (selected) colors.selectedLabel else colors.unselectedLabel,
                            fontSize = metrics.titleTextSize,
                            lineHeight = metrics.titleLineHeight,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (badgeText != null) {
                            Text(
                                text = badgeText,
                                color = if (selected) colors.selectedBadge else colors.unselectedBadge,
                                fontSize = metrics.badgeTextSize,
                                lineHeight = metrics.badgeLineHeight,
                                fontWeight = FontWeight.Medium,
                                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                                maxLines = 1,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = valueText,
                            color = if (selected) colors.selectedValue else colors.unselectedValue,
                            fontSize = metrics.valueTextSize,
                            lineHeight = metrics.valueLineHeight,
                            fontWeight = FontWeight.Bold,
                            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (secondaryValueText != null) {
                            Text(
                                text = secondaryValueText,
                                color = if (selected) colors.selectedBadge else colors.unselectedBadge,
                                fontSize = metrics.badgeTextSize,
                                lineHeight = metrics.badgeLineHeight,
                                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        // Red dot centered on the top-left vertex/corner of the card rectangle
        if (hasPendingChanges) {
            Box(
                modifier =
                    Modifier
                        .offset(x = -dotRadius, y = -dotRadius)
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(OpenAquaTheme.colors.statusCritical),
            )
        }
    }
}
