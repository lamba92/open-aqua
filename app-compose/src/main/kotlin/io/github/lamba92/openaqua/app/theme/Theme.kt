package io.github.lamba92.openaqua.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val darkColorScheme =
    darkColorScheme(
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
        outlineVariant = SurfaceContainerHighest,
    )

@Composable
@Suppress("ktlint:standard:function-naming")
fun OpenAquaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content,
    )
}
