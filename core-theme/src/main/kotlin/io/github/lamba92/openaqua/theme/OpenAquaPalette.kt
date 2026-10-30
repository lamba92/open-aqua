package io.github.lamba92.openaqua.theme

import kotlinx.serialization.Serializable
import java.awt.Color as AwtColor

@JvmInline
value class ThemeColor(
    val argb: Long,
) {
    val alpha: Int get() = ((argb ushr 24) and 0xFF).toInt()
    val red: Int get() = ((argb ushr 16) and 0xFF).toInt()
    val green: Int get() = ((argb ushr 8) and 0xFF).toInt()
    val blue: Int get() = (argb and 0xFF).toInt()

    val hex: String
        get() = "#%02X%02X%02X".format(red, green, blue)
}

fun ThemeColor.toAwtColor(): AwtColor = AwtColor(red, green, blue, alpha)

@Serializable
enum class AppColorScheme {
    HYDRO_DARK,
    HIGH_CONTRAST_DARK,
    MIDNIGHT_NAVY,
    SOLAR_AMBER,
}

data class ThemePalette(
    val colorScheme: AppColorScheme,
    // Surfaces
    val background: ThemeColor,
    val surfaceLowest: ThemeColor,
    val surfaceLow: ThemeColor,
    val surfaceContainer: ThemeColor,
    val surfaceHigh: ThemeColor,
    val surfaceHighest: ThemeColor,
    val surfaceBright: ThemeColor,
    // Primary / Accents
    val primary: ThemeColor,
    val primaryBright: ThemeColor,
    val secondary: ThemeColor,
    val secondaryDark: ThemeColor,
    val accent: ThemeColor,
    // Typography & Borders
    val textPrimary: ThemeColor,
    val textSecondary: ThemeColor,
    val textMuted: ThemeColor,
    val borderOutline: ThemeColor,
    // Semantic Status
    val statusNominal: ThemeColor,
    val statusWarning: ThemeColor,
    val statusCritical: ThemeColor,
) {
    companion object {
        val HydroDark =
            ThemePalette(
                colorScheme = AppColorScheme.HYDRO_DARK,
                background = OpenAquaPalette.ObsidianBg,
                surfaceLowest = OpenAquaPalette.SurfaceContainerLowest,
                surfaceLow = OpenAquaPalette.SurfaceContainerLow,
                surfaceContainer = OpenAquaPalette.SurfaceContainer,
                surfaceHigh = OpenAquaPalette.SurfaceContainerHigh,
                surfaceHighest = OpenAquaPalette.SurfaceContainerHighest,
                surfaceBright = OpenAquaPalette.SurfaceBright,
                primary = OpenAquaPalette.ElectricCyan,
                primaryBright = OpenAquaPalette.CyanBright,
                secondary = OpenAquaPalette.CoolantTeal,
                secondaryDark = OpenAquaPalette.TealDark,
                accent = OpenAquaPalette.SkyBlue,
                textPrimary = OpenAquaPalette.TextPrimary,
                textSecondary = OpenAquaPalette.TextSecondary,
                textMuted = OpenAquaPalette.TextMuted,
                borderOutline = OpenAquaPalette.BorderOutline,
                statusNominal = OpenAquaPalette.StatusNominal,
                statusWarning = OpenAquaPalette.StatusWarning,
                statusCritical = OpenAquaPalette.StatusCritical,
            )

        val HighContrastDark =
            ThemePalette(
                colorScheme = AppColorScheme.HIGH_CONTRAST_DARK,
                background = ThemeColor(0xFF04060A),
                surfaceLowest = ThemeColor(0xFF000000),
                surfaceLow = ThemeColor(0xFF111622),
                surfaceContainer = ThemeColor(0xFF181F30),
                surfaceHigh = ThemeColor(0xFF222B42),
                surfaceHighest = ThemeColor(0xFF2E3B59),
                surfaceBright = ThemeColor(0xFF3B4A70),
                primary = OpenAquaPalette.ElectricCyan,
                primaryBright = OpenAquaPalette.CyanBright,
                secondary = ThemeColor(0xFF22D3EE),
                secondaryDark = ThemeColor(0xFF0891B2),
                accent = ThemeColor(0xFF38BDF8),
                textPrimary = ThemeColor(0xFFFFFFFF),
                textSecondary = ThemeColor(0xFFD1D5DB),
                textMuted = ThemeColor(0xFF9CA3AF),
                borderOutline = ThemeColor(0xFF4B5563),
                statusNominal = ThemeColor(0xFF34D399),
                statusWarning = ThemeColor(0xFFFBBF24),
                statusCritical = ThemeColor(0xFFF87171),
            )

        val MidnightNavy =
            ThemePalette(
                colorScheme = AppColorScheme.MIDNIGHT_NAVY,
                background = ThemeColor(0xFF090D16),
                surfaceLowest = ThemeColor(0xFF05080E),
                surfaceLow = ThemeColor(0xFF0F172A),
                surfaceContainer = ThemeColor(0xFF1E293B),
                surfaceHigh = ThemeColor(0xFF334155),
                surfaceHighest = ThemeColor(0xFF475569),
                surfaceBright = ThemeColor(0xFF64748B),
                primary = ThemeColor(0xFF0284C7),
                primaryBright = ThemeColor(0xFF38BDF8),
                secondary = ThemeColor(0xFF6366F1),
                secondaryDark = ThemeColor(0xFF4338CA),
                accent = ThemeColor(0xFF0EA5E9),
                textPrimary = ThemeColor(0xFFF8FAFC),
                textSecondary = ThemeColor(0xFFCBD5E1),
                textMuted = ThemeColor(0xFF94A3B8),
                borderOutline = ThemeColor(0xFF475569),
                statusNominal = ThemeColor(0xFF10B981),
                statusWarning = ThemeColor(0xFFF59E0B),
                statusCritical = ThemeColor(0xFFEF4444),
            )

        val SolarAmber =
            ThemePalette(
                colorScheme = AppColorScheme.SOLAR_AMBER,
                background = ThemeColor(0xFF0C0E14),
                surfaceLowest = ThemeColor(0xFF07090D),
                surfaceLow = ThemeColor(0xFF161922),
                surfaceContainer = ThemeColor(0xFF202430),
                surfaceHigh = ThemeColor(0xFF2D3344),
                surfaceHighest = ThemeColor(0xFF3D445A),
                surfaceBright = ThemeColor(0xFF4E5772),
                primary = ThemeColor(0xFFD97706),
                primaryBright = ThemeColor(0xFFF59E0B),
                secondary = ThemeColor(0xFF14B8A6),
                secondaryDark = ThemeColor(0xFF0D9488),
                accent = ThemeColor(0xFFF97316),
                textPrimary = ThemeColor(0xFFF8FAFC),
                textSecondary = ThemeColor(0xFFE2E8F0),
                textMuted = ThemeColor(0xFF94A3B8),
                borderOutline = ThemeColor(0xFF4B5563),
                statusNominal = ThemeColor(0xFF10B981),
                statusWarning = ThemeColor(0xFFF59E0B),
                statusCritical = ThemeColor(0xFFEF4444),
            )

        val all: List<ThemePalette> = listOf(HydroDark, HighContrastDark, MidnightNavy, SolarAmber)

        fun fromColorScheme(scheme: AppColorScheme): ThemePalette =
            when (scheme) {
                AppColorScheme.HYDRO_DARK -> HydroDark
                AppColorScheme.HIGH_CONTRAST_DARK -> HighContrastDark
                AppColorScheme.MIDNIGHT_NAVY -> MidnightNavy
                AppColorScheme.SOLAR_AMBER -> SolarAmber
            }
    }
}

object OpenAquaPalette {
    // Master Brand Tokens
    val DeepNavy = ThemeColor(0xFF0B192C)
    val AquaBlue = ThemeColor(0xFF32C3F2)
    val PureWhite = ThemeColor(0xFFFFFFFF)
    val BrandElectricCyan = ThemeColor(0xFF5BE2FF)

    // Hydro-Telemetry UI Surface Tokens
    val ObsidianBg = ThemeColor(0xFF0E131E)
    val SurfaceContainerLowest = ThemeColor(0xFF090E19)
    val SurfaceContainerLow = ThemeColor(0xFF171C27)
    val SurfaceContainer = ThemeColor(0xFF1B202B)
    val SurfaceContainerHigh = ThemeColor(0xFF252A35)
    val SurfaceContainerHighest = ThemeColor(0xFF303541)
    val SurfaceBright = ThemeColor(0xFF343945)

    // Hydro-Telemetry UI Accent Tokens
    val ElectricCyan = ThemeColor(0xFF06B6D4)
    val CyanBright = ThemeColor(0xFF38C2DE)
    val CoolantTeal = ThemeColor(0xFF4FDBC8)
    val TealDark = ThemeColor(0xFF04B4A2)
    val SkyBlue = ThemeColor(0xFF38BDF8)

    // Typography & Border Tokens
    val TextPrimary = ThemeColor(0xFFDEE2F2)
    val TextSecondary = ThemeColor(0xFFB9CACB)
    val TextMuted = ThemeColor(0xFF849495)
    val BorderOutline = ThemeColor(0xFF3A494B)

    // Semantic Status Tokens
    val StatusNominal = ThemeColor(0xFF10B981)
    val StatusWarning = ThemeColor(0xFFF59E0B)
    val StatusCritical = ThemeColor(0xFFEF4444)
}
