package io.github.lamba92.openaqua.app.feature.device.fans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.feature.device.common.FanItem
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.OpenAquaOutlinedButton
import io.github.lamba92.openaqua.model.device.FanLimits
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FanLimitsCard(
    fan: FanItem,
    draft: FanDraft,
    onLimitsEvent: (FanLimitsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    val currentLimits = draft.limits ?: fan.config.limits ?: FanLimits(minPowerPercent = 0.0, maxPowerPercent = 100.0)

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
                    modifier = Modifier.size(20.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(Res.string.fan_limits_title),
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(Res.string.fan_limits_desc),
                        color = colors.textMuted,
                        fontSize = 12.sp,
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
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(Res.string.fan_limits_tip_1to1),
                color = colors.textSecondary,
                fontSize = 12.sp,
            )
        }

        // Sliders Grid
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(metrics.spacingLg),
            verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
        ) {
            // Min Power Slider
            Box(modifier = Modifier.weight(1f)) {
                LimitSlider(
                    label = stringResource(Res.string.fan_limits_min_power),
                    description = stringResource(Res.string.fan_limits_min_power_desc),
                    value = currentLimits.minPowerPercent.toFloat(),
                    onValueChange = { onLimitsEvent(FanLimitsEvent.ChangeMinPower(it.toDouble())) },
                )
            }

            // Max Power Slider
            Box(modifier = Modifier.weight(1f)) {
                LimitSlider(
                    label = stringResource(Res.string.fan_limits_max_power),
                    description = stringResource(Res.string.fan_limits_max_power_desc),
                    value = currentLimits.maxPowerPercent.toFloat(),
                    onValueChange = { onLimitsEvent(FanLimitsEvent.ChangeMaxPower(it.toDouble())) },
                )
            }

            // Fallback Power Slider
            Box(modifier = Modifier.weight(1f)) {
                LimitSlider(
                    label = stringResource(Res.string.fan_limits_fallback_power),
                    description = stringResource(Res.string.fan_limits_fallback_power_desc),
                    value = currentLimits.fallbackPowerPercent.toFloat(),
                    onValueChange = { onLimitsEvent(FanLimitsEvent.ChangeFallbackPower(it.toDouble())) },
                )
            }
        }

        HorizontalDivider(color = colors.borderOutline.copy(alpha = 0.3f))

        // Boolean Toggle Controls
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(metrics.spacingXl),
            verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
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
) {
    val colors = OpenAquaTheme.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${value.roundToInt()}%",
                color = colors.primaryBright,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
            )
        }

        Text(
            text = description,
            color = colors.textMuted,
            fontSize = 11.sp,
            lineHeight = 14.sp,
        )

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
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                color = colors.textMuted,
                fontSize = 11.sp,
                lineHeight = 14.sp,
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
