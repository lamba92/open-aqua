package io.github.lamba92.openaqua.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
            )

        @Composable
        fun operational(colors: AppColors = OpenAquaTheme.colors): FeederStatusBadgeStyle =
            FeederStatusBadgeStyle(
                containerColor = colors.statusNominal.copy(alpha = 0.15f),
                contentColor = colors.statusNominal,
                borderColor = colors.statusNominal.copy(alpha = 0.5f),
            )

        @Composable
        fun error(colors: AppColors = OpenAquaTheme.colors): FeederStatusBadgeStyle =
            FeederStatusBadgeStyle(
                containerColor = colors.statusCritical.copy(alpha = 0.15f),
                contentColor = colors.statusCritical,
                borderColor = colors.statusCritical.copy(alpha = 0.6f),
            )
    }
}

@Composable
fun FeederStatusBadge(
    status: FeederStatus,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
    style: FeederStatusBadgeStyle = FeederStatusBadgeStyle.forStatus(status),
) {
    when (status) {
        FeederStatus.NONE -> Unit
        FeederStatus.OPERATIONAL,
        FeederStatus.ERROR,
        -> {
            Box(
                modifier =
                    modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(style.contentColor),
            )
        }
    }
}
