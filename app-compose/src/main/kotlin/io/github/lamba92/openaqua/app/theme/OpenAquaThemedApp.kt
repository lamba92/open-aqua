package io.github.lamba92.openaqua.app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.lamba92.openaqua.app.OpenAquaApp
import io.github.lamba92.openaqua.app.model.AppStoredData
import io.github.lamba92.openaqua.theme.ThemePalette

@Composable
fun OpenAquaThemedApp(storedData: AppStoredData) {
    val palette =
        remember(storedData.colorScheme) {
            ThemePalette.fromColorScheme(storedData.colorScheme)
        }
    val metrics =
        remember(storedData.metricsScale) {
            AppMetrics.fromScale(storedData.metricsScale)
        }
    val fontFamily = storedData.fontFamily
    val fontSp = storedData.fontSp

    OpenAquaTheme(
        palette = palette,
        metrics = metrics,
        fontFamilyPreference = fontFamily,
        fontSp = fontSp,
        preferences = storedData,
    ) {
        OpenAquaApp()
    }
}
