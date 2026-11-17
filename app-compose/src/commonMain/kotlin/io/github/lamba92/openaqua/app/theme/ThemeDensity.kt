package io.github.lamba92.openaqua.app.theme

import androidx.compose.ui.unit.Density

internal expect fun createScaledDensity(
    currentDensity: Density,
    fontScaleFactor: Float,
): Density
