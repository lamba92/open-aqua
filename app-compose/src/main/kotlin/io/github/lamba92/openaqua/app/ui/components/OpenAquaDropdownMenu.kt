package io.github.lamba92.openaqua.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import io.github.lamba92.openaqua.app.theme.DropdownMenuColors
import io.github.lamba92.openaqua.app.theme.DropdownMenuMetrics
import io.github.lamba92.openaqua.app.theme.LocalDropdownMenuColors
import io.github.lamba92.openaqua.app.theme.LocalDropdownMenuMetrics

object OpenAquaDropdownMenuDefaults {
    @Composable
    fun colors(): DropdownMenuColors = LocalDropdownMenuColors.current

    @Composable
    fun metrics(): DropdownMenuMetrics = LocalDropdownMenuMetrics.current
}

typealias DropdownMenuDefaults = OpenAquaDropdownMenuDefaults

@Composable
fun OpenAquaDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    colors: DropdownMenuColors = OpenAquaDropdownMenuDefaults.colors(),
    metrics: DropdownMenuMetrics = OpenAquaDropdownMenuDefaults.metrics(),
    shape: Shape = RoundedCornerShape(metrics.cornerRadius),
    containerColor: Color = colors.container,
    border: BorderStroke? = BorderStroke(metrics.borderWidth, colors.border),
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        shape = shape,
        containerColor = containerColor,
        border = border,
        content = content,
    )
}

@Composable
fun OpenAquaDropdownHeader(
    text: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    colors: DropdownMenuColors = OpenAquaDropdownMenuDefaults.colors(),
    metrics: DropdownMenuMetrics = OpenAquaDropdownMenuDefaults.metrics(),
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = metrics.headerPaddingHorizontal,
                        end = metrics.headerPaddingHorizontal,
                        top = metrics.headerPaddingTop,
                        bottom = metrics.headerPaddingBottom,
                    ),
        ) {
            Text(
                text = text,
                color = colors.headerText,
                fontSize = metrics.headerTextSize,
                lineHeight = metrics.headerLineHeight,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = colors.divider,
                modifier =
                    Modifier.padding(
                        horizontal = metrics.dividerPaddingHorizontal,
                        vertical = metrics.dividerPaddingVertical,
                    ),
            )
        }
    }
}

@Composable
fun OpenAquaDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    isSelected: Boolean = false,
    isHighlighted: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: DropdownMenuColors = OpenAquaDropdownMenuDefaults.colors(),
    metrics: DropdownMenuMetrics = OpenAquaDropdownMenuDefaults.metrics(),
    shape: Shape = RoundedCornerShape(metrics.itemCornerRadius),
) {
    val isHovered by interactionSource.collectIsHoveredAsState()
    val active = isSelected || isHighlighted || isHovered

    DropdownMenuItem(
        text = text,
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = metrics.itemPaddingHorizontal,
                    vertical = metrics.itemPaddingVertical,
                ).clip(shape)
                .background(if (active) colors.itemHoverContainer else Color.Transparent)
                .border(
                    metrics.borderWidth,
                    if (active) colors.itemHoverBorder else Color.Transparent,
                    shape,
                ),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        interactionSource = interactionSource,
    )
}

@Composable
fun OpenAquaDropdownSubmenuItem(
    text: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: DropdownMenuColors = OpenAquaDropdownMenuDefaults.colors(),
    metrics: DropdownMenuMetrics = OpenAquaDropdownMenuDefaults.metrics(),
    shape: Shape = RoundedCornerShape(metrics.itemCornerRadius),
    submenuContent: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isHighlighted = isHovered || expanded

    LaunchedEffect(isHovered) {
        if (isHovered && !expanded) {
            onExpandedChange(true)
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        DropdownMenuItem(
            text = {
                Text(
                    text = text,
                    fontSize = metrics.itemTextSize,
                    lineHeight = metrics.itemLineHeight,
                    color = if (isHighlighted) colors.itemTextHighlighted else colors.itemText,
                    fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (isHighlighted) colors.iconTintHighlighted else colors.iconTint,
                    modifier = Modifier.size(metrics.itemIconSize),
                )
            },
            leadingIcon = leadingIcon,
            onClick = { onExpandedChange(!expanded) },
            enabled = enabled,
            interactionSource = interactionSource,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = metrics.itemPaddingHorizontal,
                        vertical = metrics.itemPaddingVertical,
                    ).clip(shape)
                    .background(if (isHighlighted) colors.itemHoverContainer else Color.Transparent)
                    .border(
                        metrics.borderWidth,
                        if (isHighlighted) colors.itemHoverBorder else Color.Transparent,
                        shape,
                    ),
        )

        Box(
            modifier =
                Modifier
                    .size(0.dp)
                    .align(Alignment.TopEnd),
        ) {
            OpenAquaDropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismissRequest,
                offset = DpOffset(0.dp, 0.dp),
                shape = shape,
                colors = colors,
                metrics = metrics,
                content = submenuContent,
            )
        }
    }
}
