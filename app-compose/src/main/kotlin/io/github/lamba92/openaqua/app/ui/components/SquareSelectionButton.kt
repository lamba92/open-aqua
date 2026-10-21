package io.github.lamba92.openaqua.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.theme.BorderOutline
import io.github.lamba92.openaqua.app.theme.CyanBright
import io.github.lamba92.openaqua.app.theme.ElectricCyan
import io.github.lamba92.openaqua.app.theme.SurfaceContainerHigh
import io.github.lamba92.openaqua.app.theme.SurfaceContainerLow
import io.github.lamba92.openaqua.app.theme.TextMuted
import io.github.lamba92.openaqua.app.theme.TextPrimary
import io.github.lamba92.openaqua.app.theme.TextSecondary

@Composable
fun SquareSelectionButton(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 82.dp,
    badgeText: String? = null,
    valueText: String? = null,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.width(size + 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // The Square Button
        Box(
            modifier =
                Modifier
                    .size(size)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) SurfaceContainerHigh else SurfaceContainerLow)
                    .border(
                        border =
                            if (selected) {
                                BorderStroke(2.dp, CyanBright)
                            } else {
                                BorderStroke(1.dp, BorderOutline)
                            },
                        shape = RoundedCornerShape(12.dp),
                    ).clickable(onClick = onClick)
                    .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top badge (e.g. "CH 1", "T1")
                if (badgeText != null) {
                    Text(
                        text = badgeText,
                        color = if (selected) ElectricCyan else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                } else {
                    Box(modifier = Modifier.size(1.dp))
                }

                // Center Icon
                Box(
                    modifier = Modifier.size(26.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }

                // Bottom value text (e.g. "2184 RPM", "34.8°C")
                if (valueText != null) {
                    Text(
                        text = valueText,
                        color = if (selected) TextPrimary else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Box(modifier = Modifier.size(1.dp))
                }
            }
        }

        // Name below the square button
        Text(
            text = name,
            color = if (selected) TextPrimary else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp,
        )
    }
}
