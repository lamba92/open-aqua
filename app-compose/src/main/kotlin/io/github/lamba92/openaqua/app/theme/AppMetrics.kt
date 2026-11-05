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
    val width: Dp = 260.dp,
    val height: Dp = 86.dp,
    val cornerRadius: Dp = 14.dp,
    val padding: Dp = 14.dp,
    val iconSize: Dp = 38.dp,
    val titleTextSize: TextUnit = 18.sp,
    val titleLineHeight: TextUnit = 22.sp,
    val badgeTextSize: TextUnit = 14.sp,
    val badgeLineHeight: TextUnit = 18.sp,
    val valueTextSize: TextUnit = 22.sp,
    val valueLineHeight: TextUnit = 26.sp,
    val selectedBorderWidth: Dp = 2.dp,
    val unselectedBorderWidth: Dp = 1.dp,
)

typealias SquareSelectionButtonMetrics = ChannelSelectionTileMetrics

data class MetricCardMetrics(
    val width: Dp = 220.dp,
    val cornerRadius: Dp = 10.dp,
    val padding: Dp = 12.dp,
    val verticalSpacing: Dp = 4.dp,
    val labelTextSize: TextUnit = 13.sp,
    val labelLineHeight: TextUnit = 16.sp,
    val valueTextSize: TextUnit = 16.sp,
    val valueLineHeight: TextUnit = 20.sp,
    val borderWidth: Dp = 1.dp,
)

data class ScreenHeaderMetrics(
    val titleTextSize: TextUnit = 26.sp,
    val titleLineHeight: TextUnit = 30.sp,
    val subtitleTextSize: TextUnit = 15.sp,
    val subtitleLineHeight: TextUnit = 20.sp,
    val verticalSpacing: Dp = 4.dp,
)

data class SectionHeaderMetrics(
    val iconSize: Dp = 20.dp,
    val titleTextSize: TextUnit = 16.sp,
    val titleLineHeight: TextUnit = 20.sp,
    val horizontalSpacing: Dp = 8.dp,
)

data class ChannelDetailHeaderMetrics(
    val iconSize: Dp = 20.dp,
    val titleTextSize: TextUnit = 18.sp,
    val titleLineHeight: TextUnit = 22.sp,
    val subtitleTextSize: TextUnit = 14.sp,
    val subtitleLineHeight: TextUnit = 18.sp,
    val horizontalSpacing: Dp = 10.dp,
    val verticalSpacing: Dp = 2.dp,
)

data class FeaturePlaceholderBoxMetrics(
    val iconSize: Dp = 32.dp,
    val cornerRadius: Dp = 12.dp,
    val padding: Dp = 24.dp,
    val verticalSpacing: Dp = 8.dp,
    val titleTextSize: TextUnit = 16.sp,
    val titleLineHeight: TextUnit = 20.sp,
    val descriptionTextSize: TextUnit = 14.sp,
    val descriptionLineHeight: TextUnit = 18.sp,
    val borderWidth: Dp = 1.dp,
)

data class NavigationRailMetrics(
    val collapsedWidth: Dp = 80.dp,
    val expandedWidth: Dp = 250.dp,
    val width: Dp = collapsedWidth,
    val itemIconSize: Dp = 32.dp,
    val labelTextSize: TextUnit = 14.sp,
    val itemSpacing: Dp = 8.dp,
    val headerLogoSize: Dp = 48.dp,
    val headerIconSize: Dp = 28.dp,
    val headerCornerRadius: Dp = 12.dp,
    val statusDotSize: Dp = 8.dp,
    val dividerWidth: Dp = 56.dp,
)

data class NavigationDrawerMetrics(
    val width: Dp = 250.dp,
    val itemSpacing: Dp = 8.dp,
    val headerPaddingHorizontal: Dp = 12.dp,
    val headerPaddingVertical: Dp = 16.dp,
)

data class DropdownMenuMetrics(
    val cornerRadius: Dp = 8.dp,
    val borderWidth: Dp = 1.dp,
    val headerPaddingHorizontal: Dp = 14.dp,
    val headerPaddingTop: Dp = 8.dp,
    val headerPaddingBottom: Dp = 4.dp,
    val headerTextSize: TextUnit = 13.sp,
    val headerLineHeight: TextUnit = 16.sp,
    val itemPaddingHorizontal: Dp = 6.dp,
    val itemPaddingVertical: Dp = 2.dp,
    val itemCornerRadius: Dp = 8.dp,
    val itemTextSize: TextUnit = 15.sp,
    val itemLineHeight: TextUnit = 18.sp,
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
    val navigationDrawer: NavigationDrawerMetrics = NavigationDrawerMetrics(),
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
                        width = 230.dp,
                        height = 76.dp,
                        cornerRadius = 12.dp,
                        padding = 12.dp,
                        iconSize = 34.dp,
                        titleTextSize = 16.sp,
                        titleLineHeight = 20.sp,
                        badgeTextSize = 13.sp,
                        badgeLineHeight = 17.sp,
                        valueTextSize = 19.sp,
                        valueLineHeight = 23.sp,
                    ),
                metricCard =
                    MetricCardMetrics(
                        width = 180.dp,
                        padding = 10.dp,
                        labelTextSize = 13.sp,
                        labelLineHeight = 16.sp,
                        valueTextSize = 16.sp,
                        valueLineHeight = 20.sp,
                    ),
                navigationRail =
                    NavigationRailMetrics(
                        collapsedWidth = 72.dp,
                        expandedWidth = 220.dp,
                        width = 72.dp,
                        itemIconSize = 28.dp,
                        labelTextSize = 13.sp,
                        itemSpacing = 6.dp,
                        headerLogoSize = 40.dp,
                        headerIconSize = 24.dp,
                        headerCornerRadius = 10.dp,
                        statusDotSize = 7.dp,
                        dividerWidth = 48.dp,
                    ),
                navigationDrawer =
                    NavigationDrawerMetrics(
                        width = 220.dp,
                        itemSpacing = 6.dp,
                        headerPaddingHorizontal = 10.dp,
                        headerPaddingVertical = 14.dp,
                    ),
                dropdownMenu =
                    DropdownMenuMetrics(
                        cornerRadius = 6.dp,
                        headerPaddingHorizontal = 10.dp,
                        headerPaddingTop = 6.dp,
                        headerPaddingBottom = 3.dp,
                        headerTextSize = 12.sp,
                        headerLineHeight = 14.sp,
                        itemPaddingHorizontal = 4.dp,
                        itemPaddingVertical = 2.dp,
                        itemCornerRadius = 6.dp,
                        itemTextSize = 14.sp,
                        itemLineHeight = 17.sp,
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
                navigationDrawer = NavigationDrawerMetrics(),
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
                        width = 290.dp,
                        height = 96.dp,
                        cornerRadius = 16.dp,
                        padding = 16.dp,
                        iconSize = 44.dp,
                        titleTextSize = 20.sp,
                        titleLineHeight = 24.sp,
                        badgeTextSize = 16.sp,
                        badgeLineHeight = 20.sp,
                        valueTextSize = 25.sp,
                        valueLineHeight = 30.sp,
                    ),
                metricCard =
                    MetricCardMetrics(
                        width = 250.dp,
                        padding = 14.dp,
                        verticalSpacing = 6.dp,
                        labelTextSize = 15.sp,
                        labelLineHeight = 18.sp,
                        valueTextSize = 18.sp,
                        valueLineHeight = 22.sp,
                    ),
                sectionHeader =
                    SectionHeaderMetrics(
                        iconSize = 22.dp,
                        horizontalSpacing = 10.dp,
                        titleTextSize = 18.sp,
                        titleLineHeight = 22.sp,
                    ),
                channelDetailHeader =
                    ChannelDetailHeaderMetrics(
                        iconSize = 22.dp,
                        horizontalSpacing = 12.dp,
                        verticalSpacing = 4.dp,
                        titleTextSize = 18.sp,
                        titleLineHeight = 22.sp,
                        subtitleTextSize = 14.sp,
                        subtitleLineHeight = 18.sp,
                    ),
                featurePlaceholderBox =
                    FeaturePlaceholderBoxMetrics(
                        iconSize = 36.dp,
                        padding = 28.dp,
                        verticalSpacing = 10.dp,
                        titleTextSize = 16.sp,
                        titleLineHeight = 20.sp,
                        descriptionTextSize = 14.sp,
                        descriptionLineHeight = 18.sp,
                    ),
                navigationRail =
                    NavigationRailMetrics(
                        collapsedWidth = 92.dp,
                        expandedWidth = 280.dp,
                        width = 92.dp,
                        itemIconSize = 36.dp,
                        labelTextSize = 16.sp,
                        itemSpacing = 12.dp,
                        headerLogoSize = 56.dp,
                        headerIconSize = 34.dp,
                        headerCornerRadius = 14.dp,
                        statusDotSize = 10.dp,
                        dividerWidth = 68.dp,
                    ),
                navigationDrawer =
                    NavigationDrawerMetrics(
                        width = 280.dp,
                        itemSpacing = 10.dp,
                        headerPaddingHorizontal = 14.dp,
                        headerPaddingVertical = 18.dp,
                    ),
                dropdownMenu =
                    DropdownMenuMetrics(
                        cornerRadius = 10.dp,
                        headerPaddingHorizontal = 16.dp,
                        headerPaddingTop = 10.dp,
                        headerPaddingBottom = 6.dp,
                        headerTextSize = 14.sp,
                        headerLineHeight = 18.sp,
                        itemPaddingHorizontal = 8.dp,
                        itemPaddingVertical = 3.dp,
                        itemCornerRadius = 10.dp,
                        itemTextSize = 16.sp,
                        itemLineHeight = 20.sp,
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
