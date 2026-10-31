package io.github.lamba92.openaqua.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.theme.AppColors
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme

enum class FeederStatus {
    OPERATIONAL,
    ERROR,
    NONE,
}

@Immutable
data class FeederStatusBadgeStyle(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color,
    val icon: ImageVector? = null,
) {
    companion object {
        @Composable
        fun forStatus(
            status: FeederStatus,
            colors: AppColors = OpenAquaTheme.colors,
        ): FeederStatusBadgeStyle =
            when (status) {
                FeederStatus.NONE -> none(colors)
                FeederStatus.OPERATIONAL -> operational(colors)
                FeederStatus.ERROR -> error(colors)
            }

        @Composable
        fun none(colors: AppColors = OpenAquaTheme.colors): FeederStatusBadgeStyle =
            FeederStatusBadgeStyle(
                containerColor = colors.surfaceLowest,
                contentColor = colors.textMuted,
                borderColor = colors.borderOutline,
                icon = null,
            )

        @Composable
        fun operational(colors: AppColors = OpenAquaTheme.colors): FeederStatusBadgeStyle =
            FeederStatusBadgeStyle(
                containerColor = colors.statusNominal.copy(alpha = 0.15f),
                contentColor = colors.statusNominal,
                borderColor = colors.statusNominal.copy(alpha = 0.5f),
                icon = null,
            )

        @Composable
        fun error(colors: AppColors = OpenAquaTheme.colors): FeederStatusBadgeStyle =
            FeederStatusBadgeStyle(
                containerColor = colors.statusCritical.copy(alpha = 0.15f),
                contentColor = colors.statusCritical,
                borderColor = colors.statusCritical.copy(alpha = 0.6f),
                icon = Icons.Outlined.ErrorOutline,
            )
    }
}

@Composable
fun FeederStatusBadge(
    status: FeederStatus,
    text: String,
    modifier: Modifier = Modifier,
    style: FeederStatusBadgeStyle = FeederStatusBadgeStyle.forStatus(status),
) {
    val metrics = OpenAquaTheme.metrics
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(metrics.radiusSmall))
                .background(style.containerColor)
                .border(metrics.strokeThin, style.borderColor, RoundedCornerShape(metrics.radiusSmall))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (style.icon != null) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.contentColor,
                modifier = Modifier.size(13.dp),
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(style.contentColor),
            )
        }
        Text(
            text = text,
            color = style.contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
