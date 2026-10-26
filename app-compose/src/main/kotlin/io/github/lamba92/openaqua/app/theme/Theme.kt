package io.github.lamba92.openaqua.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import io.github.lamba92.openaqua.app.model.AppStoredData
import io.github.lamba92.openaqua.theme.AppColorScheme
import io.github.lamba92.openaqua.theme.ThemePalette
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.color_scheme_high_contrast_dark
import open_aqua.app_compose.generated.resources.color_scheme_hydro_dark
import open_aqua.app_compose.generated.resources.color_scheme_midnight_navy
import open_aqua.app_compose.generated.resources.color_scheme_solar_amber
import open_aqua.app_compose.generated.resources.metrics_scale_comfortable
import open_aqua.app_compose.generated.resources.metrics_scale_compact
import open_aqua.app_compose.generated.resources.metrics_scale_standard
import org.jetbrains.compose.resources.StringResource

val LocalAppPreferences = staticCompositionLocalOf { AppStoredData() }
val LocalAppMetrics = staticCompositionLocalOf { AppMetrics.Standard }
val LocalAppColors = staticCompositionLocalOf { AppColors.fromPalette(ThemePalette.HydroDark) }
val LocalAppTypography = staticCompositionLocalOf { AppTypography() }

val LocalChannelSelectionTileMetrics = staticCompositionLocalOf { AppMetrics.Standard.channelSelectionTile }
val LocalChannelSelectionTileColors = staticCompositionLocalOf { AppColors.fromPalette(ThemePalette.HydroDark).channelSelectionTile }
val LocalSquareSelectionButtonMetrics = LocalChannelSelectionTileMetrics
val LocalSquareSelectionButtonColors = LocalChannelSelectionTileColors

val LocalMetricCardMetrics = staticCompositionLocalOf { AppMetrics.Standard.metricCard }
val LocalMetricCardColors = staticCompositionLocalOf { AppColors.fromPalette(ThemePalette.HydroDark).metricCard }

val LocalScreenHeaderMetrics = staticCompositionLocalOf { AppMetrics.Standard.screenHeader }
val LocalScreenHeaderColors = staticCompositionLocalOf { AppColors.fromPalette(ThemePalette.HydroDark).screenHeader }

val LocalSectionHeaderMetrics = staticCompositionLocalOf { AppMetrics.Standard.sectionHeader }
val LocalSectionHeaderColors = staticCompositionLocalOf { AppColors.fromPalette(ThemePalette.HydroDark).sectionHeader }

val LocalChannelDetailHeaderMetrics = staticCompositionLocalOf { AppMetrics.Standard.channelDetailHeader }
val LocalChannelDetailHeaderColors = staticCompositionLocalOf { AppColors.fromPalette(ThemePalette.HydroDark).channelDetailHeader }

val LocalFeaturePlaceholderBoxMetrics = staticCompositionLocalOf { AppMetrics.Standard.featurePlaceholderBox }
val LocalFeaturePlaceholderBoxColors = staticCompositionLocalOf { AppColors.fromPalette(ThemePalette.HydroDark).featurePlaceholderBox }

val LocalNavigationRailMetrics = staticCompositionLocalOf { AppMetrics.Standard.navigationRail }
val LocalNavigationRailColors = staticCompositionLocalOf { AppColors.fromPalette(ThemePalette.HydroDark).navigationRail }

val AppColorScheme.stringResource: StringResource
    get() =
        when (this) {
            AppColorScheme.HYDRO_DARK -> Res.string.color_scheme_hydro_dark
            AppColorScheme.HIGH_CONTRAST_DARK -> Res.string.color_scheme_high_contrast_dark
            AppColorScheme.MIDNIGHT_NAVY -> Res.string.color_scheme_midnight_navy
            AppColorScheme.SOLAR_AMBER -> Res.string.color_scheme_solar_amber
        }

val AppMetricsScale.stringResource: StringResource
    get() =
        when (this) {
            AppMetricsScale.COMPACT -> Res.string.metrics_scale_compact
            AppMetricsScale.STANDARD -> Res.string.metrics_scale_standard
            AppMetricsScale.COMFORTABLE -> Res.string.metrics_scale_comfortable
        }

object OpenAquaTheme {
    val preferences: AppStoredData
        @Composable
        @ReadOnlyComposable
        get() = LocalAppPreferences.current

    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val metrics: AppMetrics
        @Composable
        @ReadOnlyComposable
        get() = LocalAppMetrics.current

    val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current
}

@Composable
@Suppress("ktlint:standard:function-naming")
fun OpenAquaTheme(
    palette: ThemePalette = ThemePalette.HydroDark,
    metrics: AppMetrics = AppMetrics.Standard,
    fontFamilyPreference: AppFontFamily = AppFontFamily.JETBRAINS_MONO,
    fontSp: Int = FontScaleConstants.DEFAULT_SP,
    preferences: AppStoredData = AppStoredData(),
    content: @Composable () -> Unit,
) {
    val jetBrainsMono = rememberJetBrainsMonoFontFamily()
    val fontScaleFactor = remember(fontSp) { calculateFontScaleFactor(fontSp) }
    val appTypography =
        remember(fontFamilyPreference, fontSp, fontScaleFactor, jetBrainsMono) {
            when (fontFamilyPreference) {
                AppFontFamily.JETBRAINS_MONO ->
                    AppTypography(
                        fontPreference = fontFamilyPreference,
                        fontSp = fontSp,
                        fontScaleFactor = fontScaleFactor,
                        defaultFontFamily = jetBrainsMono,
                        monospaceFontFamily = jetBrainsMono,
                    )
                AppFontFamily.SYSTEM_DEFAULT ->
                    AppTypography(
                        fontPreference = fontFamilyPreference,
                        fontSp = fontSp,
                        fontScaleFactor = fontScaleFactor,
                        defaultFontFamily = FontFamily.Default,
                        monospaceFontFamily = FontFamily.Monospace,
                    )
            }
        }

    val appColors = remember(palette) { AppColors.fromPalette(palette) }
    val materialColorScheme = remember(appColors) { appColors.toMaterialDarkColorScheme() }

    val baseTypography = remember { Typography() }
    val materialTypography =
        remember(baseTypography, appTypography.defaultFontFamily) {
            val family = appTypography.defaultFontFamily
            Typography(
                displayLarge = baseTypography.displayLarge.copy(fontFamily = family),
                displayMedium = baseTypography.displayMedium.copy(fontFamily = family),
                displaySmall = baseTypography.displaySmall.copy(fontFamily = family),
                headlineLarge = baseTypography.headlineLarge.copy(fontFamily = family),
                headlineMedium = baseTypography.headlineMedium.copy(fontFamily = family),
                headlineSmall = baseTypography.headlineSmall.copy(fontFamily = family),
                titleLarge = baseTypography.titleLarge.copy(fontFamily = family),
                titleMedium = baseTypography.titleMedium.copy(fontFamily = family),
                titleSmall = baseTypography.titleSmall.copy(fontFamily = family),
                bodyLarge = baseTypography.bodyLarge.copy(fontFamily = family),
                bodyMedium = baseTypography.bodyMedium.copy(fontFamily = family),
                bodySmall = baseTypography.bodySmall.copy(fontFamily = family),
                labelLarge = baseTypography.labelLarge.copy(fontFamily = family),
                labelMedium = baseTypography.labelMedium.copy(fontFamily = family),
                labelSmall = baseTypography.labelSmall.copy(fontFamily = family),
            )
        }

    val currentDensity = LocalDensity.current
    val scaledDensity =
        remember(currentDensity, fontScaleFactor) {
            Density(
                density = currentDensity.density,
                fontScale = currentDensity.fontScale * fontScaleFactor,
            )
        }

    CompositionLocalProvider(
        LocalAppPreferences provides preferences,
        LocalDensity provides scaledDensity,
        LocalAppMetrics provides metrics,
        LocalAppColors provides appColors,
        LocalAppTypography provides appTypography,
        LocalChannelSelectionTileMetrics provides metrics.channelSelectionTile,
        LocalChannelSelectionTileColors provides appColors.channelSelectionTile,
        LocalSquareSelectionButtonMetrics provides metrics.squareSelectionButton,
        LocalSquareSelectionButtonColors provides appColors.squareSelectionButton,
        LocalMetricCardMetrics provides metrics.metricCard,
        LocalMetricCardColors provides appColors.metricCard,
        LocalScreenHeaderMetrics provides metrics.screenHeader,
        LocalScreenHeaderColors provides appColors.screenHeader,
        LocalSectionHeaderMetrics provides metrics.sectionHeader,
        LocalSectionHeaderColors provides appColors.sectionHeader,
        LocalChannelDetailHeaderMetrics provides metrics.channelDetailHeader,
        LocalChannelDetailHeaderColors provides appColors.channelDetailHeader,
        LocalFeaturePlaceholderBoxMetrics provides metrics.featurePlaceholderBox,
        LocalFeaturePlaceholderBoxColors provides appColors.featurePlaceholderBox,
        LocalNavigationRailMetrics provides metrics.navigationRail,
        LocalNavigationRailColors provides appColors.navigationRail,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = materialTypography,
            content = content,
        )
    }
}
