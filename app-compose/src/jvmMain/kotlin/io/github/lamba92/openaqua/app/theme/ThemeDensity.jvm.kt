package io.github.lamba92.openaqua.app.theme

import androidx.compose.ui.unit.Density

internal actual fun createScaledDensity(
    currentDensity: Density,
    fontScaleFactor: Float,
): Density =
    Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * fontScaleFactor,
    )
