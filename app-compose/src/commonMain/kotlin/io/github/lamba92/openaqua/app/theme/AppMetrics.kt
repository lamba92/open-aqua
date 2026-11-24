package io.github.lamba92.openaqua.app.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable

@Serializable
enum class AppMetricsScale {
    COMPACT,
    STANDARD,
    COMFORTABLE,
}

data class AppMetricsTokens(
    val scale: AppMetricsScale,
    // Spacing scale
    val spacingXs: Dp,
    val spacingSm: Dp,
    val spacingMd: Dp,
    val spacingLg: Dp,
    val spacingXl: Dp,
    val spacingXxl: Dp,
    // Corner radius scale
    val radiusXs: Dp,
    val radiusSm: Dp,
    val radiusMd: Dp,
    val radiusLg: Dp,
    val radiusXl: Dp,
    // Stroke scale
    val strokeThin: Dp = 1.dp,
    val strokeFocused: Dp = 2.dp,
    // Icon size scale
    val iconXs: Dp,
    val iconSm: Dp,
    val iconMd: Dp,
    val iconLg: Dp,
    val iconXl: Dp,
    // Base Type Scale (Font sizes & Line heights)
    val fontXs: TextUnit,
    val lineHeightXs: TextUnit,
    val fontSm: TextUnit,
    val lineHeightSm: TextUnit,
    val fontMd: TextUnit,
    val lineHeightMd: TextUnit,
    val fontLg: TextUnit,
    val lineHeightLg: TextUnit,
    val fontXl: TextUnit,
    val lineHeightXl: TextUnit,
    val fontXxl: TextUnit,
    val lineHeightXxl: TextUnit,
) {
    companion object {
        val Compact =
            AppMetricsTokens(
                scale = AppMetricsScale.COMPACT,
                spacingXs = 2.dp,
                spacingSm = 4.dp,
                spacingMd = 8.dp,
                spacingLg = 12.dp,
                spacingXl = 16.dp,
                spacingXxl = 20.dp,
                radiusXs = 3.dp,
                radiusSm = 6.dp,
                radiusMd = 8.dp,
                radiusLg = 10.dp,
                radiusXl = 12.dp,
                strokeThin = 1.dp,
                strokeFocused = 2.dp,
                iconXs = 8.dp,
                iconSm = 12.dp,
                iconMd = 18.dp,
                iconLg = 20.dp,
                iconXl = 28.dp,
                fontXs = 10.sp,
                lineHeightXs = 13.sp,
                fontSm = 11.sp,
                lineHeightSm = 14.sp,
                fontMd = 13.sp,
                lineHeightMd = 18.sp,
                fontLg = 14.sp,
                lineHeightLg = 20.sp,
                fontXl = 15.sp,
                lineHeightXl = 18.sp,
                fontXxl = 20.sp,
                lineHeightXxl = 24.sp,
            )

        val Standard =
            AppMetricsTokens(
                scale = AppMetricsScale.STANDARD,
                spacingXs = 4.dp,
                spacingSm = 8.dp,
                spacingMd = 12.dp,
                spacingLg = 16.dp,
                spacingXl = 20.dp,
                spacingXxl = 24.dp,
                radiusXs = 4.dp,
                radiusSm = 6.dp,
                radiusMd = 10.dp,
                radiusLg = 12.dp,
                radiusXl = 16.dp,
                strokeThin = 1.dp,
                strokeFocused = 2.dp,
                iconXs = 10.dp,
                iconSm = 14.dp,
                iconMd = 20.dp,
                iconLg = 24.dp,
                iconXl = 32.dp,
                fontXs = 11.sp,
                lineHeightXs = 14.sp,
                fontSm = 12.sp,
                lineHeightSm = 16.sp,
                fontMd = 14.sp,
                lineHeightMd = 20.sp,
                fontLg = 16.sp,
                lineHeightLg = 22.sp,
                fontXl = 18.sp,
                lineHeightXl = 22.sp,
                fontXxl = 22.sp,
                lineHeightXxl = 28.sp,
            )

        val Comfortable =
            AppMetricsTokens(
                scale = AppMetricsScale.COMFORTABLE,
                spacingXs = 6.dp,
                spacingSm = 10.dp,
                spacingMd = 14.dp,
                spacingLg = 18.dp,
                spacingXl = 24.dp,
                spacingXxl = 32.dp,
                radiusXs = 6.dp,
                radiusSm = 8.dp,
                radiusMd = 12.dp,
                radiusLg = 14.dp,
                radiusXl = 18.dp,
                strokeThin = 1.dp,
                strokeFocused = 2.dp,
                iconXs = 12.dp,
                iconSm = 16.dp,
                iconMd = 22.dp,
                iconLg = 28.dp,
                iconXl = 36.dp,
                fontXs = 12.sp,
                lineHeightXs = 15.sp,
                fontSm = 13.sp,
                lineHeightSm = 18.sp,
                fontMd = 15.sp,
                lineHeightMd = 22.sp,
                fontLg = 17.sp,
                lineHeightLg = 24.sp,
                fontXl = 20.sp,
                lineHeightXl = 24.sp,
                fontXxl = 24.sp,
                lineHeightXxl = 30.sp,
            )
    }
}

data class ChannelSelectionTileMetrics(
    val width: Dp = 220.dp,
    val height: Dp = 64.dp,
    val cornerRadius: Dp = 12.dp,
    val padding: Dp = 12.dp,
    val paddingVertical: Dp = 8.dp,
    val iconSize: Dp = 24.dp,
    val titleTextSize: TextUnit = 16.sp,
    val titleLineHeight: TextUnit = 22.sp,
    val badgeTextSize: TextUnit = 12.sp,
    val badgeLineHeight: TextUnit = 16.sp,
    val valueTextSize: TextUnit = 18.sp,
    val valueLineHeight: TextUnit = 22.sp,
    val selectedBorderWidth: Dp = 2.dp,
    val unselectedBorderWidth: Dp = 1.dp,
) {
    companion object {
        fun fromTokens(tokens: AppMetricsTokens): ChannelSelectionTileMetrics =
            ChannelSelectionTileMetrics(
                width =
                    when (tokens.scale) {
                        AppMetricsScale.COMPACT -> 190.dp
                        AppMetricsScale.STANDARD -> 220.dp
                        AppMetricsScale.COMFORTABLE -> 240.dp
                    },
                height =
                    when (tokens.scale) {
                        AppMetricsScale.COMPACT -> 60.dp
                        AppMetricsScale.STANDARD -> 64.dp
                        AppMetricsScale.COMFORTABLE -> 72.dp
                    },
                cornerRadius = tokens.radiusLg,
                padding = tokens.spacingMd,
                paddingVertical =
                    when (tokens.scale) {
                        AppMetricsScale.COMPACT -> 6.dp
                        AppMetricsScale.STANDARD -> 8.dp
                        AppMetricsScale.COMFORTABLE -> 10.dp
                    },
                iconSize = tokens.iconLg,
                titleTextSize = tokens.fontLg,
                titleLineHeight = tokens.lineHeightLg,
                badgeTextSize = tokens.fontSm,
                badgeLineHeight = tokens.lineHeightSm,
                valueTextSize = tokens.fontXl,
                valueLineHeight = tokens.lineHeightXl,
                selectedBorderWidth = tokens.strokeFocused,
                unselectedBorderWidth = tokens.strokeThin,
            )
    }
}

typealias SquareSelectionButtonMetrics = ChannelSelectionTileMetrics

data class MetricCardMetrics(
    val width: Dp = 180.dp,
    val height: Dp = 68.dp,
    val cornerRadius: Dp = 10.dp,
    val padding: Dp = 10.dp,
    val verticalSpacing: Dp = 4.dp,
    val labelTextSize: TextUnit = 12.sp,
    val labelLineHeight: TextUnit = 16.sp,
    val valueTextSize: TextUnit = 16.sp,
    val valueLineHeight: TextUnit = 22.sp,
    val borderWidth: Dp = 1.dp,
) {
    companion object {
        fun fromTokens(tokens: AppMetricsTokens): MetricCardMetrics =
            MetricCardMetrics(
                width =
                    when (tokens.scale) {
                        AppMetricsScale.COMPACT -> 150.dp
                        AppMetricsScale.STANDARD -> 180.dp
                        AppMetricsScale.COMFORTABLE -> 200.dp
                    },
                height =
                    when (tokens.scale) {
                        AppMetricsScale.COMPACT -> 60.dp
                        AppMetricsScale.STANDARD -> 68.dp
                        AppMetricsScale.COMFORTABLE -> 76.dp
                    },
                cornerRadius = tokens.radiusMd,
                padding =
                    when (tokens.scale) {
                        AppMetricsScale.COMPACT -> tokens.spacingMd
                        AppMetricsScale.STANDARD -> 10.dp
                        AppMetricsScale.COMFORTABLE -> tokens.spacingMd
                    },
                verticalSpacing = tokens.spacingXs,
                labelTextSize = tokens.fontSm,
                labelLineHeight = tokens.lineHeightSm,
                valueTextSize = tokens.fontLg,
                valueLineHeight = tokens.lineHeightLg,
                borderWidth = tokens.strokeThin,
            )
    }
}

data class ScreenHeaderMetrics(
    val titleTextSize: TextUnit = 22.sp,
    val titleLineHeight: TextUnit = 28.sp,
    val subtitleTextSize: TextUnit = 14.sp,
    val subtitleLineHeight: TextUnit = 20.sp,
    val verticalSpacing: Dp = 4.dp,
) {
    companion object {
        fun fromTokens(tokens: AppMetricsTokens): ScreenHeaderMetrics =
            ScreenHeaderMetrics(
                titleTextSize = tokens.fontXxl,
                titleLineHeight = tokens.lineHeightXxl,
                subtitleTextSize = tokens.fontMd,
                subtitleLineHeight = tokens.lineHeightMd,
                verticalSpacing = tokens.spacingXs,
            )
    }
}

data class SectionHeaderMetrics(
    val iconSize: Dp = 20.dp,
    val titleTextSize: TextUnit = 16.sp,
    val titleLineHeight: TextUnit = 22.sp,
    val horizontalSpacing: Dp = 8.dp,
) {
    companion object {
        fun fromTokens(tokens: AppMetricsTokens): SectionHeaderMetrics =
            SectionHeaderMetrics(
                iconSize = tokens.iconMd,
                titleTextSize = tokens.fontLg,
                titleLineHeight = tokens.lineHeightLg,
                horizontalSpacing = tokens.spacingSm,
            )
    }
}

data class ChannelDetailHeaderMetrics(
    val iconSize: Dp = 20.dp,
    val titleTextSize: TextUnit = 16.sp,
    val titleLineHeight: TextUnit = 22.sp,
    val subtitleTextSize: TextUnit = 12.sp,
    val subtitleLineHeight: TextUnit = 16.sp,
    val horizontalSpacing: Dp = 8.dp,
    val verticalSpacing: Dp = 4.dp,
) {
    companion object {
        fun fromTokens(tokens: AppMetricsTokens): ChannelDetailHeaderMetrics =
            ChannelDetailHeaderMetrics(
                iconSize = tokens.iconMd,
                titleTextSize = tokens.fontLg,
                titleLineHeight = tokens.lineHeightLg,
                subtitleTextSize = tokens.fontSm,
                subtitleLineHeight = tokens.lineHeightSm,
                horizontalSpacing = tokens.spacingSm,
                verticalSpacing = tokens.spacingXs,
            )
    }
}

data class FeaturePlaceholderBoxMetrics(
    val iconSize: Dp = 32.dp,
    val cornerRadius: Dp = 10.dp,
    val padding: Dp = 20.dp,
    val verticalSpacing: Dp = 8.dp,
    val titleTextSize: TextUnit = 16.sp,
    val titleLineHeight: TextUnit = 22.sp,
    val descriptionTextSize: TextUnit = 12.sp,
    val descriptionLineHeight: TextUnit = 16.sp,
    val borderWidth: Dp = 1.dp,
) {
    companion object {
        fun fromTokens(tokens: AppMetricsTokens): FeaturePlaceholderBoxMetrics =
            FeaturePlaceholderBoxMetrics(
                iconSize = tokens.iconXl,
                cornerRadius = tokens.radiusMd,
                padding = tokens.spacingXl,
                verticalSpacing = tokens.spacingSm,
                titleTextSize = tokens.fontLg,
                titleLineHeight = tokens.lineHeightLg,
                descriptionTextSize = tokens.fontSm,
                descriptionLineHeight = tokens.lineHeightSm,
                borderWidth = tokens.strokeThin,
            )
    }
}

data class NavigationRailMetrics(
    val collapsedWidth: Dp = 72.dp,
    val expandedWidth: Dp = 240.dp,
    val width: Dp = collapsedWidth,
    val itemIconSize: Dp = 24.dp,
    val itemHeight: Dp = 40.dp,
    val itemCornerRadius: Dp = 10.dp,
    val labelTextSize: TextUnit = 12.sp,
    val itemSpacing: Dp = 8.dp,
    val headerLogoSize: Dp = 32.dp,
    val headerIconSize: Dp = 24.dp,
    val headerCornerRadius: Dp = 10.dp,
    val headerTitleTextSize: TextUnit = 16.sp,
    val statusDotSize: Dp = 7.dp,
    val dividerWidth: Dp = 48.dp,
) {
    companion object {
        fun fromTokens(tokens: AppMetricsTokens): NavigationRailMetrics =
            NavigationRailMetrics(
                collapsedWidth =
                    when (tokens.scale) {
                        AppMetricsScale.COMPACT -> 64.dp
                        AppMetricsScale.STANDARD -> 72.dp
                        AppMetricsScale.COMFORTABLE -> 80.dp
                    },
                expandedWidth =
                    when (tokens.scale) {
                        AppMetricsScale.COMPACT -> 200.dp
                        AppMetricsScale.STANDARD -> 240.dp
                        AppMetricsScale.COMFORTABLE -> 260.dp
                    },
                width =
                    when (tokens.scale) {
                        AppMetricsScale.COMPACT -> 64.dp
                        AppMetricsScale.STANDARD -> 72.dp
                        AppMetricsScale.COMFORTABLE -> 80.dp
                    },
                itemIconSize = tokens.iconLg,
                itemHeight = tokens.iconLg + 16.dp,
                itemCornerRadius = tokens.radiusMd,
                labelTextSize = tokens.fontSm,
                itemSpacing = tokens.spacingSm,
                headerLogoSize = tokens.iconXl,
                headerIconSize = tokens.iconLg,
                headerCornerRadius = tokens.radiusMd,
                headerTitleTextSize = tokens.fontLg,
                statusDotSize =
                    when (tokens.scale) {
                        AppMetricsScale.COMPACT -> 6.dp
                        AppMetricsScale.STANDARD -> 7.dp
                        AppMetricsScale.COMFORTABLE -> 8.dp
                    },
                dividerWidth =
                    when (tokens.scale) {
                        AppMetricsScale.COMPACT -> 40.dp
                        AppMetricsScale.STANDARD -> 48.dp
                        AppMetricsScale.COMFORTABLE -> 56.dp
                    },
            )
    }
}

data class NavigationDrawerMetrics(
    val width: Dp = 240.dp,
    val itemSpacing: Dp = 8.dp,
    val itemHeight: Dp = 40.dp,
    val itemCornerRadius: Dp = 10.dp,
    val itemPaddingHorizontal: Dp = 12.dp,
    val itemHorizontalMargin: Dp = 8.dp,
    val headerPaddingHorizontal: Dp = 16.dp,
    val headerPaddingVertical: Dp = 12.dp,
    val headerTitleTextSize: TextUnit = 16.sp,
    val headerTitleLineHeight: TextUnit = 22.sp,
    val headerSubtitleTextSize: TextUnit = 12.sp,
    val headerSubtitleLineHeight: TextUnit = 16.sp,
    val itemTextSize: TextUnit = 14.sp,
    val itemLineHeight: TextUnit = 20.sp,
    val itemIconSize: Dp = 20.dp,
) {
    companion object {
        fun fromTokens(tokens: AppMetricsTokens): NavigationDrawerMetrics =
            NavigationDrawerMetrics(
                width =
                    when (tokens.scale) {
                        AppMetricsScale.COMPACT -> 200.dp
                        AppMetricsScale.STANDARD -> 240.dp
                        AppMetricsScale.COMFORTABLE -> 260.dp
                    },
                itemSpacing = tokens.spacingSm,
                itemHeight = tokens.iconLg + 16.dp,
                itemCornerRadius = tokens.radiusMd,
                itemPaddingHorizontal = tokens.spacingMd,
                itemHorizontalMargin = tokens.spacingSm,
                headerPaddingHorizontal = tokens.spacingLg,
                headerPaddingVertical = tokens.spacingMd,
                headerTitleTextSize = tokens.fontLg,
                headerTitleLineHeight = tokens.lineHeightLg,
                headerSubtitleTextSize = tokens.fontSm,
                headerSubtitleLineHeight = tokens.lineHeightSm,
                itemTextSize = tokens.fontMd,
                itemLineHeight = tokens.lineHeightMd,
                itemIconSize = tokens.iconMd,
            )
    }
}

data class ChipMetrics(
    val paddingHorizontal: Dp = 12.dp,
    val paddingVertical: Dp = 6.dp,
    val textSize: TextUnit = 14.sp,
    val iconSize: Dp = 10.dp,
    val cornerRadius: Dp = 6.dp,
) {
    companion object {
        fun fromTokens(tokens: AppMetricsTokens): ChipMetrics =
            ChipMetrics(
                paddingHorizontal = tokens.spacingMd,
                paddingVertical =
                    when (tokens.scale) {
                        AppMetricsScale.COMPACT -> 4.dp
                        AppMetricsScale.STANDARD -> 6.dp
                        AppMetricsScale.COMFORTABLE -> 8.dp
                    },
                textSize = tokens.fontMd,
                iconSize = tokens.iconXs,
                cornerRadius = tokens.radiusSm,
            )
    }
}

data class SettingRowMetrics(
    val titleTextSize: TextUnit = 16.sp,
    val titleLineHeight: TextUnit = 22.sp,
    val descriptionTextSize: TextUnit = 12.sp,
    val descriptionLineHeight: TextUnit = 16.sp,
    val verticalSpacing: Dp = 4.dp,
) {
    companion object {
        fun fromTokens(tokens: AppMetricsTokens): SettingRowMetrics =
            SettingRowMetrics(
                titleTextSize = tokens.fontLg,
                titleLineHeight = tokens.lineHeightLg,
                descriptionTextSize = tokens.fontSm,
                descriptionLineHeight = tokens.lineHeightSm,
                verticalSpacing = tokens.spacingXs,
            )
    }
}

data class DropdownMenuMetrics(
    val cornerRadius: Dp = 6.dp,
    val borderWidth: Dp = 1.dp,
    val headerPaddingHorizontal: Dp = 12.dp,
    val headerPaddingTop: Dp = 6.dp,
    val headerPaddingBottom: Dp = 3.dp,
    val headerTextSize: TextUnit = 12.sp,
    val headerLineHeight: TextUnit = 16.sp,
    val itemPaddingHorizontal: Dp = 6.dp,
    val itemPaddingVertical: Dp = 2.dp,
    val itemCornerRadius: Dp = 6.dp,
    val itemTextSize: TextUnit = 14.sp,
    val itemLineHeight: TextUnit = 20.sp,
    val itemIconSize: Dp = 14.dp,
    val dividerPaddingHorizontal: Dp = 6.dp,
    val dividerPaddingVertical: Dp = 4.dp,
) {
    companion object {
        fun fromTokens(tokens: AppMetricsTokens): DropdownMenuMetrics =
            DropdownMenuMetrics(
                cornerRadius = tokens.radiusSm,
                borderWidth = tokens.strokeThin,
                headerPaddingHorizontal = tokens.spacingMd,
                headerPaddingTop =
                    when (tokens.scale) {
                        AppMetricsScale.COMPACT -> 4.dp
                        AppMetricsScale.STANDARD -> 6.dp
                        AppMetricsScale.COMFORTABLE -> 8.dp
                    },
                headerPaddingBottom =
                    when (tokens.scale) {
                        AppMetricsScale.COMPACT -> 2.dp
                        AppMetricsScale.STANDARD -> 3.dp
                        AppMetricsScale.COMFORTABLE -> 4.dp
                    },
                headerTextSize = tokens.fontSm,
                headerLineHeight = tokens.lineHeightSm,
                itemPaddingHorizontal = tokens.spacingSm,
                itemPaddingVertical = 2.dp,
                itemCornerRadius = tokens.radiusSm,
                itemTextSize = tokens.fontMd,
                itemLineHeight = tokens.lineHeightMd,
                itemIconSize = tokens.iconSm,
                dividerPaddingHorizontal = tokens.spacingSm,
                dividerPaddingVertical = tokens.spacingXs,
            )
    }
}

data class ButtonMetrics(
    val cornerRadius: Dp = 10.dp,
    val iconSize: Dp = 20.dp,
    val textSize: TextUnit = 14.sp,
    val spacing: Dp = 8.dp,
    val strokeWidth: Dp = 1.dp,
) {
    companion object {
        fun fromTokens(tokens: AppMetricsTokens): ButtonMetrics =
            ButtonMetrics(
                cornerRadius = tokens.radiusMd,
                iconSize = tokens.iconMd,
                textSize = tokens.fontMd,
                spacing = tokens.spacingSm,
                strokeWidth = tokens.strokeThin,
            )
    }
}

data class AppMetrics(
    val scale: AppMetricsScale = AppMetricsScale.STANDARD,
    val tokens: AppMetricsTokens = AppMetricsTokens.Standard,
    // Base Spacings
    val spacingXs: Dp = tokens.spacingXs,
    val spacingSm: Dp = tokens.spacingSm,
    val spacingMd: Dp = tokens.spacingMd,
    val spacingLg: Dp = tokens.spacingLg,
    val spacingXl: Dp = tokens.spacingXl,
    val spacingXxl: Dp = tokens.spacingXxl,
    // Base Radiuses
    val radiusXs: Dp = tokens.radiusXs,
    val radiusSm: Dp = tokens.radiusSm,
    val radiusMd: Dp = tokens.radiusMd,
    val radiusLg: Dp = tokens.radiusLg,
    val radiusXl: Dp = tokens.radiusXl,
    val radiusSmall: Dp = tokens.radiusSm,
    val radiusMedium: Dp = tokens.radiusMd,
    val radiusLarge: Dp = tokens.radiusLg,
    // Base Borders
    val strokeThin: Dp = tokens.strokeThin,
    val strokeFocused: Dp = tokens.strokeFocused,
    // Base Icons
    val iconXs: Dp = tokens.iconXs,
    val iconSm: Dp = tokens.iconSm,
    val iconMd: Dp = tokens.iconMd,
    val iconLg: Dp = tokens.iconLg,
    val iconXl: Dp = tokens.iconXl,
    // Base Font Sizes & Line Heights
    val fontSizeXs: TextUnit = tokens.fontXs,
    val lineHeightXs: TextUnit = tokens.lineHeightXs,
    val fontSizeSm: TextUnit = tokens.fontSm,
    val lineHeightSm: TextUnit = tokens.lineHeightSm,
    val fontSizeMd: TextUnit = tokens.fontMd,
    val lineHeightMd: TextUnit = tokens.lineHeightMd,
    val fontSizeLg: TextUnit = tokens.fontLg,
    val lineHeightLg: TextUnit = tokens.lineHeightLg,
    val fontSizeXl: TextUnit = tokens.fontXl,
    val lineHeightXl: TextUnit = tokens.lineHeightXl,
    val fontSizeXxl: TextUnit = tokens.fontXxl,
    val lineHeightXxl: TextUnit = tokens.lineHeightXxl,
    // Component Metrics
    val channelSelectionTile: ChannelSelectionTileMetrics = ChannelSelectionTileMetrics.fromTokens(tokens),
    val squareSelectionButton: ChannelSelectionTileMetrics = channelSelectionTile,
    val metricCard: MetricCardMetrics = MetricCardMetrics.fromTokens(tokens),
    val screenHeader: ScreenHeaderMetrics = ScreenHeaderMetrics.fromTokens(tokens),
    val sectionHeader: SectionHeaderMetrics = SectionHeaderMetrics.fromTokens(tokens),
    val channelDetailHeader: ChannelDetailHeaderMetrics = ChannelDetailHeaderMetrics.fromTokens(tokens),
    val featurePlaceholderBox: FeaturePlaceholderBoxMetrics = FeaturePlaceholderBoxMetrics.fromTokens(tokens),
    val navigationRail: NavigationRailMetrics = NavigationRailMetrics.fromTokens(tokens),
    val navigationDrawer: NavigationDrawerMetrics = NavigationDrawerMetrics.fromTokens(tokens),
    val dropdownMenu: DropdownMenuMetrics = DropdownMenuMetrics.fromTokens(tokens),
    val chip: ChipMetrics = ChipMetrics.fromTokens(tokens),
    val settingRow: SettingRowMetrics = SettingRowMetrics.fromTokens(tokens),
    val button: ButtonMetrics = ButtonMetrics.fromTokens(tokens),
) {
    companion object {
        fun fromTokens(tokens: AppMetricsTokens): AppMetrics =
            AppMetrics(
                scale = tokens.scale,
                tokens = tokens,
                spacingXs = tokens.spacingXs,
                spacingSm = tokens.spacingSm,
                spacingMd = tokens.spacingMd,
                spacingLg = tokens.spacingLg,
                spacingXl = tokens.spacingXl,
                spacingXxl = tokens.spacingXxl,
                radiusXs = tokens.radiusXs,
                radiusSm = tokens.radiusSm,
                radiusMd = tokens.radiusMd,
                radiusLg = tokens.radiusLg,
                radiusXl = tokens.radiusXl,
                radiusSmall = tokens.radiusSm,
                radiusMedium = tokens.radiusMd,
                radiusLarge = tokens.radiusLg,
                strokeThin = tokens.strokeThin,
                strokeFocused = tokens.strokeFocused,
                iconXs = tokens.iconXs,
                iconSm = tokens.iconSm,
                iconMd = tokens.iconMd,
                iconLg = tokens.iconLg,
                iconXl = tokens.iconXl,
                fontSizeXs = tokens.fontXs,
                lineHeightXs = tokens.lineHeightXs,
                fontSizeSm = tokens.fontSm,
                lineHeightSm = tokens.lineHeightSm,
                fontSizeMd = tokens.fontMd,
                lineHeightMd = tokens.lineHeightMd,
                fontSizeLg = tokens.fontLg,
                lineHeightLg = tokens.lineHeightLg,
                fontSizeXl = tokens.fontXl,
                lineHeightXl = tokens.lineHeightXl,
                fontSizeXxl = tokens.fontXxl,
                lineHeightXxl = tokens.lineHeightXxl,
                channelSelectionTile = ChannelSelectionTileMetrics.fromTokens(tokens),
                metricCard = MetricCardMetrics.fromTokens(tokens),
                screenHeader = ScreenHeaderMetrics.fromTokens(tokens),
                sectionHeader = SectionHeaderMetrics.fromTokens(tokens),
                channelDetailHeader = ChannelDetailHeaderMetrics.fromTokens(tokens),
                featurePlaceholderBox = FeaturePlaceholderBoxMetrics.fromTokens(tokens),
                navigationRail = NavigationRailMetrics.fromTokens(tokens),
                navigationDrawer = NavigationDrawerMetrics.fromTokens(tokens),
                dropdownMenu = DropdownMenuMetrics.fromTokens(tokens),
                chip = ChipMetrics.fromTokens(tokens),
                settingRow = SettingRowMetrics.fromTokens(tokens),
                button = ButtonMetrics.fromTokens(tokens),
            )

        val Compact: AppMetrics = fromTokens(AppMetricsTokens.Compact)
        val Standard: AppMetrics = fromTokens(AppMetricsTokens.Standard)
        val Comfortable: AppMetrics = fromTokens(AppMetricsTokens.Comfortable)

        val all: List<AppMetrics> = listOf(Compact, Standard, Comfortable)

        fun fromScale(scale: AppMetricsScale): AppMetrics =
            when (scale) {
                AppMetricsScale.COMPACT -> Compact
                AppMetricsScale.STANDARD -> Standard
                AppMetricsScale.COMFORTABLE -> Comfortable
            }
    }
}
