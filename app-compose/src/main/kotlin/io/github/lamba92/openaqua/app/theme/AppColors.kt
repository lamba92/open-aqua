package io.github.lamba92.openaqua.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import io.github.lamba92.openaqua.theme.ThemePalette

data class ChannelSelectionTileColors(
    val selectedContainer: Color,
    val unselectedContainer: Color,
    val selectedBorder: Color,
    val unselectedBorder: Color,
    val selectedBadge: Color,
    val unselectedBadge: Color,
    val selectedValue: Color,
    val unselectedValue: Color,
    val selectedLabel: Color,
    val unselectedLabel: Color,
)

typealias SquareSelectionButtonColors = ChannelSelectionTileColors

data class MetricCardColors(
    val container: Color,
    val border: Color,
    val label: Color,
    val value: Color,
)

data class ScreenHeaderColors(
    val title: Color,
    val subtitle: Color,
)

data class SectionHeaderColors(
    val iconTint: Color,
    val title: Color,
)

data class ChannelDetailHeaderColors(
    val iconTint: Color,
    val title: Color,
    val subtitle: Color,
)

data class FeaturePlaceholderBoxColors(
    val container: Color,
    val border: Color,
    val iconTint: Color,
    val title: Color,
    val description: Color,
)

data class NavigationRailColors(
    val container: Color,
    val itemIndicator: Color,
    val itemSelectedIcon: Color,
    val itemUnselectedIcon: Color,
    val itemSelectedText: Color,
    val itemUnselectedText: Color,
)

data class DropdownMenuColors(
    val container: Color,
    val border: Color,
    val headerText: Color,
    val itemText: Color,
    val itemTextHighlighted: Color,
    val itemHoverContainer: Color,
    val itemHoverBorder: Color,
    val divider: Color,
    val iconTint: Color,
    val iconTintHighlighted: Color,
)

data class AppColors(
    val palette: ThemePalette,
    // Base Surfaces
    val background: Color,
    val surfaceLowest: Color,
    val surfaceLow: Color,
    val surfaceContainer: Color,
    val surfaceHigh: Color,
    val surfaceHighest: Color,
    val surfaceBright: Color,
    // Base Accents
    val primary: Color,
    val primaryBright: Color,
    val secondary: Color,
    val secondaryDark: Color,
    val accent: Color,
    // Base Typography & Borders
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val borderOutline: Color,
    // Base Status
    val statusNominal: Color,
    val statusWarning: Color,
    val statusCritical: Color,
    // Component Colors
    val channelSelectionTile: ChannelSelectionTileColors,
    val squareSelectionButton: ChannelSelectionTileColors = channelSelectionTile,
    val metricCard: MetricCardColors,
    val screenHeader: ScreenHeaderColors,
    val sectionHeader: SectionHeaderColors,
    val channelDetailHeader: ChannelDetailHeaderColors,
    val featurePlaceholderBox: FeaturePlaceholderBoxColors,
    val navigationRail: NavigationRailColors,
    val dropdownMenu: DropdownMenuColors,
) {
    fun toMaterialDarkColorScheme(): ColorScheme =
        darkColorScheme(
            primary = primaryBright,
            onPrimary = background,
            primaryContainer = primary,
            onPrimaryContainer = background,
            secondary = secondary,
            onSecondaryContainer = background,
            secondaryContainer = secondaryDark,
            tertiary = accent,
            background = background,
            onBackground = textPrimary,
            surface = background,
            onSurface = textPrimary,
            surfaceVariant = surfaceHigh,
            onSurfaceVariant = textSecondary,
            outline = borderOutline,
            outlineVariant = surfaceHighest,
        )

    companion object {
        fun fromPalette(palette: ThemePalette): AppColors {
            val bg = palette.background.toComposeColor()
            val surfLowest = palette.surfaceLowest.toComposeColor()
            val surfLow = palette.surfaceLow.toComposeColor()
            val surf = palette.surfaceContainer.toComposeColor()
            val surfHigh = palette.surfaceHigh.toComposeColor()
            val surfHighest = palette.surfaceHighest.toComposeColor()
            val surfBright = palette.surfaceBright.toComposeColor()

            val primary = palette.primary.toComposeColor()
            val primaryBright = palette.primaryBright.toComposeColor()
            val secondary = palette.secondary.toComposeColor()
            val secondaryDark = palette.secondaryDark.toComposeColor()
            val accent = palette.accent.toComposeColor()

            val textPrimary = palette.textPrimary.toComposeColor()
            val textSecondary = palette.textSecondary.toComposeColor()
            val textMuted = palette.textMuted.toComposeColor()
            val borderOutline = palette.borderOutline.toComposeColor()

            val nominal = palette.statusNominal.toComposeColor()
            val warning = palette.statusWarning.toComposeColor()
            val critical = palette.statusCritical.toComposeColor()

            val tileColors =
                ChannelSelectionTileColors(
                    selectedContainer = surfHigh,
                    unselectedContainer = surfLow,
                    selectedBorder = primaryBright,
                    unselectedBorder = borderOutline,
                    selectedBadge = primary,
                    unselectedBadge = textMuted,
                    selectedValue = textPrimary,
                    unselectedValue = textSecondary,
                    selectedLabel = textPrimary,
                    unselectedLabel = textSecondary,
                )

            return AppColors(
                palette = palette,
                background = bg,
                surfaceLowest = surfLowest,
                surfaceLow = surfLow,
                surfaceContainer = surf,
                surfaceHigh = surfHigh,
                surfaceHighest = surfHighest,
                surfaceBright = surfBright,
                primary = primary,
                primaryBright = primaryBright,
                secondary = secondary,
                secondaryDark = secondaryDark,
                accent = accent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                textMuted = textMuted,
                borderOutline = borderOutline,
                statusNominal = nominal,
                statusWarning = warning,
                statusCritical = critical,
                channelSelectionTile = tileColors,
                squareSelectionButton = tileColors,
                metricCard =
                    MetricCardColors(
                        container = surfHigh,
                        border = borderOutline,
                        label = textMuted,
                        value = textPrimary,
                    ),
                screenHeader =
                    ScreenHeaderColors(
                        title = textPrimary,
                        subtitle = textMuted,
                    ),
                sectionHeader =
                    SectionHeaderColors(
                        iconTint = primaryBright,
                        title = primaryBright,
                    ),
                channelDetailHeader =
                    ChannelDetailHeaderColors(
                        iconTint = primaryBright,
                        title = textPrimary,
                        subtitle = textMuted,
                    ),
                featurePlaceholderBox =
                    FeaturePlaceholderBoxColors(
                        container = surf,
                        border = borderOutline,
                        iconTint = primary.copy(alpha = 0.7f),
                        title = textPrimary,
                        description = textMuted,
                    ),
                navigationRail =
                    NavigationRailColors(
                        container = surfLow,
                        itemIndicator = primary.copy(alpha = 0.2f),
                        itemSelectedIcon = primaryBright,
                        itemUnselectedIcon = textMuted,
                        itemSelectedText = textPrimary,
                        itemUnselectedText = textMuted,
                    ),
                dropdownMenu =
                    DropdownMenuColors(
                        container = surf,
                        border = borderOutline,
                        headerText = textMuted,
                        itemText = textPrimary,
                        itemTextHighlighted = primaryBright,
                        itemHoverContainer = surfHighest,
                        itemHoverBorder = borderOutline,
                        divider = borderOutline,
                        iconTint = textSecondary,
                        iconTintHighlighted = primaryBright,
                    ),
            )
        }
    }
}
