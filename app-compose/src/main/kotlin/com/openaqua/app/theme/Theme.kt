package com.openaqua.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyanBright,
    onPrimary = ObsidianBg,
    primaryContainer = ElectricCyan,
    onPrimaryContainer = ObsidianBg,
    secondary = CoolantTeal,
    onSecondary = ObsidianBg,
    secondaryContainer = TealDark,
    onSecondaryContainer = ObsidianBg,
    tertiary = SkyBlue,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianBg,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = TextSecondary,
    outline = BorderOutline,
    outlineVariant = SurfaceContainerHighest
)

@Composable
fun OpenAquaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
