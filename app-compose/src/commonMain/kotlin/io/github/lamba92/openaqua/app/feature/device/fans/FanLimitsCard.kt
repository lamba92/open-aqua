package io.github.lamba92.openaqua.app.feature.device.fans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lamba92.openaqua.app.feature.device.common.FanItem
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.OpenAquaOutlinedButton
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.fan_limits_desc
import open_aqua.app_compose.generated.resources.fan_limits_fallback_power
import open_aqua.app_compose.generated.resources.fan_limits_fallback_power_desc
import open_aqua.app_compose.generated.resources.fan_limits_hold_min
import open_aqua.app_compose.generated.resources.fan_limits_hold_min_desc
import open_aqua.app_compose.generated.resources.fan_limits_max_power
import open_aqua.app_compose.generated.resources.fan_limits_max_power_desc
import open_aqua.app_compose.generated.resources.fan_limits_min_power
import open_aqua.app_compose.generated.resources.fan_limits_min_power_desc
import open_aqua.app_compose.generated.resources.fan_limits_reset_standard
import open_aqua.app_compose.generated.resources.fan_limits_start_boost
import open_aqua.app_compose.generated.resources.fan_limits_start_boost_desc
import open_aqua.app_compose.generated.resources.fan_limits_tip_1to1
import open_aqua.app_compose.generated.resources.fan_limits_title
import open_aqua.app_compose.generated.resources.ic_info
import open_aqua.app_compose.generated.resources.ic_tune
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun FanLimitsCard(
    fan: FanItem,
    draft: FanDraft,
    onLimitsEvent: (FanLimitsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    val currentLimits = draft.limits

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(metrics.radiusLarge))
                .background(colors.surfaceContainer)
                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusLarge))
                .padding(metrics.spacingXl),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = metrics.spacingLg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_tune),
                    contentDescription = null,
                    tint = colors.primaryBright,
                    modifier = Modifier.size(metrics.iconMd),
                )
                Column(verticalArrangement = Arrangement.spacedBy(metrics.spacingXs)) {
                    Text(
                        text = stringResource(Res.string.fan_limits_title),
                        color = colors.textPrimary,
                        fontSize = metrics.fontSizeLg,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(Res.string.fan_limits_desc),
                        color = colors.textMuted,
                        fontSize = metrics.fontSizeSm,
                    )
                }
            }

            OpenAquaOutlinedButton(
                text = stringResource(Res.string.fan_limits_reset_standard),
                onClick = { onLimitsEvent(FanLimitsEvent.ResetToStandardLimits) },
                enabled = currentLimits.minPowerPercent != 0.0 || currentLimits.maxPowerPercent != 100.0,
            )
        }

        HorizontalDivider(color = colors.borderOutline.copy(alpha = 0.5f))

        // Tip Box for 1:1 Curve Tracking
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(metrics.radiusSmall))
                    .background(colors.surfaceLowest)
                    .border(metrics.strokeThin, colors.borderOutline.copy(alpha = 0.5f), RoundedCornerShape(metrics.radiusSmall))
                    .padding(horizontal = metrics.spacingMd, vertical = metrics.spacingSm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_info),
                contentDescription = null,
                tint = colors.primaryBright,
                modifier = Modifier.size(metrics.iconSm),
            )
            Text(
                text = stringResource(Res.string.fan_limits_tip_1to1),
                color = colors.textSecondary,
                fontSize = metrics.fontSizeSm,
            )
        }

        val sliderHeaderHeights = remember { mutableStateMapOf<Int, Dp>() }
        val maxSliderHeaderHeight = sliderHeaderHeights.values.maxOrNull() ?: Dp.Unspecified

        // Sliders Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(metrics.spacingLg),
        ) {
            // Min Power Slider
            Box(modifier = Modifier.weight(1f)) {
                LimitSlider(
                    label = stringResource(Res.string.fan_limits_min_power),
                    description = stringResource(Res.string.fan_limits_min_power_desc),
                    value = currentLimits.minPowerPercent.toFloat(),
                    onValueChange = { onLimitsEvent(FanLimitsEvent.ChangeMinPower(it.toDouble())) },
                    minHeaderHeight = maxSliderHeaderHeight,
                    onHeaderHeightMeasured = { if (sliderHeaderHeights[0] != it) sliderHeaderHeights[0] = it },
                )
            }

            // Max Power Slider
            Box(modifier = Modifier.weight(1f)) {
                LimitSlider(
                    label = stringResource(Res.string.fan_limits_max_power),
                    description = stringResource(Res.string.fan_limits_max_power_desc),
                    value = currentLimits.maxPowerPercent.toFloat(),
                    onValueChange = { onLimitsEvent(FanLimitsEvent.ChangeMaxPower(it.toDouble())) },
                    minHeaderHeight = maxSliderHeaderHeight,
                    onHeaderHeightMeasured = { if (sliderHeaderHeights[1] != it) sliderHeaderHeights[1] = it },
                )
            }

            // Fallback Power Slider
            Box(modifier = Modifier.weight(1f)) {
                LimitSlider(
                    label = stringResource(Res.string.fan_limits_fallback_power),
                    description = stringResource(Res.string.fan_limits_fallback_power_desc),
                    value = currentLimits.fallbackPowerPercent.toFloat(),
                    onValueChange = { onLimitsEvent(FanLimitsEvent.ChangeFallbackPower(it.toDouble())) },
                    minHeaderHeight = maxSliderHeaderHeight,
                    onHeaderHeightMeasured = { if (sliderHeaderHeights[2] != it) sliderHeaderHeights[2] = it },
                )
            }
        }

        HorizontalDivider(color = colors.borderOutline.copy(alpha = 0.3f))

        // Boolean Toggle Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(metrics.spacingXl),
        ) {
            // Start Boost Switch
            Box(modifier = Modifier.weight(1f)) {
                LimitToggleRow(
                    title = stringResource(Res.string.fan_limits_start_boost),
                    description = stringResource(Res.string.fan_limits_start_boost_desc),
                    checked = currentLimits.startBoost,
                    onCheckedChange = { onLimitsEvent(FanLimitsEvent.ChangeStartBoost(it)) },
                )
            }

            // Hold Minimum Power Switch
            Box(modifier = Modifier.weight(1f)) {
                LimitToggleRow(
                    title = stringResource(Res.string.fan_limits_hold_min),
                    description = stringResource(Res.string.fan_limits_hold_min_desc),
                    checked = currentLimits.holdMinPower,
                    onCheckedChange = { onLimitsEvent(FanLimitsEvent.ChangeHoldMinPower(it)) },
                )
            }
        }
    }
}

@Composable
private fun LimitSlider(
    label: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    minHeaderHeight: Dp = Dp.Unspecified,
    onHeaderHeightMeasured: ((Dp) -> Unit)? = null,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    val density = LocalDensity.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (minHeaderHeight != Dp.Unspecified && minHeaderHeight > 0.dp) {
                            Modifier.defaultMinSize(minHeight = minHeaderHeight)
                        } else {
                            Modifier
                        },
                    ),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            onHeaderHeightMeasured?.invoke(with(density) { size.height.toDp() })
                        },
                verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        color = colors.textPrimary,
                        fontSize = metrics.fontSizeSm,
                        lineHeight = metrics.lineHeightSm,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(modifier = Modifier.width(metrics.spacingSm))
                    Text(
                        text = "${value.roundToInt()}%",
                        color = colors.primaryBright,
                        fontSize = metrics.fontSizeMd,
                        lineHeight = metrics.lineHeightMd,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                    )
                }

                Text(
                    text = description,
                    color = colors.textMuted,
                    fontSize = metrics.fontSizeXs,
                    lineHeight = metrics.lineHeightXs,
                    softWrap = true,
                )
            }
        }

        Slider(
            value = value,
            onValueChange = { onValueChange(it.roundToInt().toFloat()) },
            valueRange = 0f..100f,
            steps = 99,
            colors =
                SliderDefaults.colors(
                    thumbColor = colors.primaryBright,
                    activeTrackColor = colors.primary,
                    inactiveTrackColor = colors.surfaceLowest,
                ),
        )
    }
}

@Composable
private fun LimitToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = metrics.spacingMd),
            verticalArrangement = Arrangement.spacedBy(metrics.spacingXs),
        ) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = metrics.fontSizeSm,
                lineHeight = metrics.lineHeightSm,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                color = colors.textMuted,
                fontSize = metrics.fontSizeXs,
                lineHeight = metrics.lineHeightXs,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = colors.surfaceLowest,
                    checkedTrackColor = colors.primary,
                    checkedBorderColor = colors.primary,
                    uncheckedThumbColor = colors.textMuted,
                    uncheckedTrackColor = colors.surfaceHigh,
                    uncheckedBorderColor = colors.borderOutline,
                ),
        )
    }
}
