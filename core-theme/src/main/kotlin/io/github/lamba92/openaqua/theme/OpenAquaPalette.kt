package io.github.lamba92.openaqua.theme

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
    val ElectricCyan = ThemeColor(0xFF00F2FE)
    val CyanBright = ThemeColor(0xFF6FF6FF)
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
