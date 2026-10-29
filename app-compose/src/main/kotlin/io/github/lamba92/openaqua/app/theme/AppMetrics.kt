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

data class ChannelSelectionTileMetrics(
    val width: Dp = 248.dp,
    val height: Dp = 82.dp,
    val cornerRadius: Dp = 14.dp,
    val padding: Dp = 14.dp,
    val iconSize: Dp = 38.dp,
    val titleTextSize: TextUnit = 16.sp,
    val titleLineHeight: TextUnit = 20.sp,
    val badgeTextSize: TextUnit = 12.sp,
    val badgeLineHeight: TextUnit = 16.sp,
    val valueTextSize: TextUnit = 20.sp,
    val valueLineHeight: TextUnit = 24.sp,
    val selectedBorderWidth: Dp = 2.dp,
    val unselectedBorderWidth: Dp = 1.dp,
)

typealias SquareSelectionButtonMetrics = ChannelSelectionTileMetrics

data class MetricCardMetrics(
    val width: Dp = 220.dp,
    val cornerRadius: Dp = 10.dp,
    val padding: Dp = 10.dp,
    val verticalSpacing: Dp = 4.dp,
    val labelTextSize: TextUnit = 11.sp,
    val labelLineHeight: TextUnit = 14.sp,
    val valueTextSize: TextUnit = 14.sp,
    val valueLineHeight: TextUnit = 18.sp,
    val borderWidth: Dp = 1.dp,
)

data class ScreenHeaderMetrics(
    val titleTextSize: TextUnit = 24.sp,
    val titleLineHeight: TextUnit = 28.sp,
    val subtitleTextSize: TextUnit = 13.sp,
    val subtitleLineHeight: TextUnit = 18.sp,
    val verticalSpacing: Dp = 4.dp,
)

data class SectionHeaderMetrics(
    val iconSize: Dp = 20.dp,
    val titleTextSize: TextUnit = 14.sp,
    val titleLineHeight: TextUnit = 18.sp,
    val horizontalSpacing: Dp = 8.dp,
)

data class ChannelDetailHeaderMetrics(
    val iconSize: Dp = 20.dp,
    val titleTextSize: TextUnit = 16.sp,
    val titleLineHeight: TextUnit = 20.sp,
    val subtitleTextSize: TextUnit = 12.sp,
    val subtitleLineHeight: TextUnit = 16.sp,
    val horizontalSpacing: Dp = 10.dp,
    val verticalSpacing: Dp = 2.dp,
)

data class FeaturePlaceholderBoxMetrics(
    val iconSize: Dp = 32.dp,
    val cornerRadius: Dp = 12.dp,
    val padding: Dp = 24.dp,
    val verticalSpacing: Dp = 8.dp,
    val titleTextSize: TextUnit = 14.sp,
    val titleLineHeight: TextUnit = 18.sp,
    val descriptionTextSize: TextUnit = 12.sp,
    val descriptionLineHeight: TextUnit = 16.sp,
    val borderWidth: Dp = 1.dp,
)

data class NavigationRailMetrics(
    val width: Dp = 108.dp,
    val itemIconSize: Dp = 36.dp,
    val labelTextSize: TextUnit = 13.sp,
    val itemSpacing: Dp = 12.dp,
    val headerLogoSize: Dp = 64.dp,
    val headerIconSize: Dp = 38.dp,
    val headerCornerRadius: Dp = 16.dp,
    val statusDotSize: Dp = 9.dp,
    val dividerWidth: Dp = 72.dp,
)

data class DropdownMenuMetrics(
    val cornerRadius: Dp = 8.dp,
    val borderWidth: Dp = 1.dp,
    val headerPaddingHorizontal: Dp = 14.dp,
    val headerPaddingTop: Dp = 8.dp,
    val headerPaddingBottom: Dp = 4.dp,
    val headerTextSize: TextUnit = 11.sp,
    val headerLineHeight: TextUnit = 14.sp,
    val itemPaddingHorizontal: Dp = 6.dp,
    val itemPaddingVertical: Dp = 2.dp,
    val itemCornerRadius: Dp = 8.dp,
    val itemTextSize: TextUnit = 13.sp,
    val itemLineHeight: TextUnit = 16.sp,
    val itemIconSize: Dp = 16.dp,
    val dividerPaddingHorizontal: Dp = 6.dp,
    val dividerPaddingVertical: Dp = 4.dp,
)

data class AppMetrics(
    val scale: AppMetricsScale = AppMetricsScale.STANDARD,
    // Base Spacings
    val spacingXs: Dp = 4.dp,
    val spacingSm: Dp = 8.dp,
    val spacingMd: Dp = 12.dp,
    val spacingLg: Dp = 16.dp,
    val spacingXl: Dp = 20.dp,
    val spacingXxl: Dp = 24.dp,
    // Base Radiuses
    val radiusSmall: Dp = 8.dp,
    val radiusMedium: Dp = 12.dp,
    val radiusLarge: Dp = 16.dp,
    // Base Borders
    val strokeThin: Dp = 1.dp,
    val strokeFocused: Dp = 2.dp,
    // Component Metrics
    val channelSelectionTile: ChannelSelectionTileMetrics = ChannelSelectionTileMetrics(),
    val squareSelectionButton: ChannelSelectionTileMetrics = channelSelectionTile,
    val metricCard: MetricCardMetrics = MetricCardMetrics(),
    val screenHeader: ScreenHeaderMetrics = ScreenHeaderMetrics(),
    val sectionHeader: SectionHeaderMetrics = SectionHeaderMetrics(),
    val channelDetailHeader: ChannelDetailHeaderMetrics = ChannelDetailHeaderMetrics(),
    val featurePlaceholderBox: FeaturePlaceholderBoxMetrics = FeaturePlaceholderBoxMetrics(),
    val navigationRail: NavigationRailMetrics = NavigationRailMetrics(),
    val dropdownMenu: DropdownMenuMetrics = DropdownMenuMetrics(),
) {
    companion object {
        val Compact =
            AppMetrics(
                scale = AppMetricsScale.COMPACT,
                spacingXs = 3.dp,
                spacingSm = 6.dp,
                spacingMd = 10.dp,
                spacingLg = 14.dp,
                spacingXl = 18.dp,
                spacingXxl = 20.dp,
                channelSelectionTile =
                    ChannelSelectionTileMetrics(
                        width = 216.dp,
                        height = 72.dp,
                        cornerRadius = 12.dp,
                        padding = 12.dp,
                        iconSize = 34.dp,
                        titleTextSize = 14.sp,
                        titleLineHeight = 18.sp,
                        badgeTextSize = 11.sp,
                        badgeLineHeight = 15.sp,
                        valueTextSize = 17.sp,
                        valueLineHeight = 21.sp,
                    ),
                metricCard =
                    MetricCardMetrics(
                        width = 180.dp,
                        padding = 10.dp,
                    ),
                navigationRail =
                    NavigationRailMetrics(
                        width = 92.dp,
                        itemIconSize = 30.dp,
                        labelTextSize = 11.sp,
                        itemSpacing = 8.dp,
                        headerLogoSize = 54.dp,
                        headerIconSize = 32.dp,
                        headerCornerRadius = 14.dp,
                        statusDotSize = 7.dp,
                        dividerWidth = 60.dp,
                    ),
                dropdownMenu =
                    DropdownMenuMetrics(
                        cornerRadius = 6.dp,
                        headerPaddingHorizontal = 10.dp,
                        headerPaddingTop = 6.dp,
                        headerPaddingBottom = 3.dp,
                        headerTextSize = 10.sp,
                        headerLineHeight = 12.sp,
                        itemPaddingHorizontal = 4.dp,
                        itemPaddingVertical = 2.dp,
                        itemCornerRadius = 6.dp,
                        itemTextSize = 12.sp,
                        itemLineHeight = 15.sp,
                        itemIconSize = 14.dp,
                    ),
            )

        val Standard =
            AppMetrics(
                scale = AppMetricsScale.STANDARD,
                channelSelectionTile = ChannelSelectionTileMetrics(),
                metricCard = MetricCardMetrics(),
                screenHeader = ScreenHeaderMetrics(),
                sectionHeader = SectionHeaderMetrics(),
                channelDetailHeader = ChannelDetailHeaderMetrics(),
                featurePlaceholderBox = FeaturePlaceholderBoxMetrics(),
                navigationRail = NavigationRailMetrics(),
                dropdownMenu = DropdownMenuMetrics(),
            )

        val Comfortable =
            AppMetrics(
                scale = AppMetricsScale.COMFORTABLE,
                spacingXs = 6.dp,
                spacingSm = 10.dp,
                spacingMd = 14.dp,
                spacingLg = 18.dp,
                spacingXl = 22.dp,
                spacingXxl = 28.dp,
                channelSelectionTile =
                    ChannelSelectionTileMetrics(
                        width = 280.dp,
                        height = 92.dp,
                        cornerRadius = 16.dp,
                        padding = 16.dp,
                        iconSize = 44.dp,
                        titleTextSize = 18.sp,
                        titleLineHeight = 22.sp,
                        badgeTextSize = 14.sp,
                        badgeLineHeight = 18.sp,
                        valueTextSize = 23.sp,
                        valueLineHeight = 28.sp,
                    ),
                metricCard =
                    MetricCardMetrics(
                        width = 250.dp,
                        padding = 14.dp,
                        verticalSpacing = 6.dp,
                    ),
                sectionHeader =
                    SectionHeaderMetrics(
                        iconSize = 22.dp,
                        horizontalSpacing = 10.dp,
                    ),
                channelDetailHeader =
                    ChannelDetailHeaderMetrics(
                        iconSize = 22.dp,
                        horizontalSpacing = 12.dp,
                        verticalSpacing = 4.dp,
                    ),
                featurePlaceholderBox =
                    FeaturePlaceholderBoxMetrics(
                        iconSize = 36.dp,
                        padding = 28.dp,
                        verticalSpacing = 10.dp,
                    ),
                navigationRail =
                    NavigationRailMetrics(
                        width = 124.dp,
                        itemIconSize = 42.dp,
                        labelTextSize = 14.sp,
                        itemSpacing = 16.dp,
                        headerLogoSize = 74.dp,
                        headerIconSize = 44.dp,
                        headerCornerRadius = 18.dp,
                        statusDotSize = 10.dp,
                        dividerWidth = 84.dp,
                    ),
                dropdownMenu =
                    DropdownMenuMetrics(
                        cornerRadius = 10.dp,
                        headerPaddingHorizontal = 16.dp,
                        headerPaddingTop = 10.dp,
                        headerPaddingBottom = 6.dp,
                        headerTextSize = 12.sp,
                        headerLineHeight = 16.sp,
                        itemPaddingHorizontal = 8.dp,
                        itemPaddingVertical = 3.dp,
                        itemCornerRadius = 10.dp,
                        itemTextSize = 14.sp,
                        itemLineHeight = 18.sp,
                        itemIconSize = 18.dp,
                    ),
            )

        val all: List<AppMetrics> = listOf(Compact, Standard, Comfortable)

        fun fromScale(scale: AppMetricsScale): AppMetrics =
            when (scale) {
                AppMetricsScale.COMPACT -> Compact
                AppMetricsScale.STANDARD -> Standard
                AppMetricsScale.COMFORTABLE -> Comfortable
            }
    }
}
