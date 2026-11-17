package io.github.lamba92.openaqua.app.theme

import androidx.compose.ui.graphics.Color
import io.github.lamba92.openaqua.theme.OpenAquaPalette
import io.github.lamba92.openaqua.theme.ThemeColor

fun ThemeColor.toComposeColor(): Color = Color(argb)

// Brand Identity Tokens
val BrandDeepNavy = OpenAquaPalette.DeepNavy.toComposeColor()
val BrandAquaBlue = OpenAquaPalette.AquaBlue.toComposeColor()
val BrandPureWhite = OpenAquaPalette.PureWhite.toComposeColor()
val BrandElectricCyan = OpenAquaPalette.BrandElectricCyan.toComposeColor()

// Hydro-Telemetry Design System Palette
val ObsidianBg = OpenAquaPalette.ObsidianBg.toComposeColor()
val SurfaceContainerLowest = OpenAquaPalette.SurfaceContainerLowest.toComposeColor()
val SurfaceContainerLow = OpenAquaPalette.SurfaceContainerLow.toComposeColor()
val SurfaceContainer = OpenAquaPalette.SurfaceContainer.toComposeColor()
val SurfaceContainerHigh = OpenAquaPalette.SurfaceContainerHigh.toComposeColor()
val SurfaceContainerHighest = OpenAquaPalette.SurfaceContainerHighest.toComposeColor()
val SurfaceBright = OpenAquaPalette.SurfaceBright.toComposeColor()

val ElectricCyan = OpenAquaPalette.ElectricCyan.toComposeColor()
val CyanBright = OpenAquaPalette.CyanBright.toComposeColor()
val CoolantTeal = OpenAquaPalette.CoolantTeal.toComposeColor()
val TealDark = OpenAquaPalette.TealDark.toComposeColor()
val SkyBlue = OpenAquaPalette.SkyBlue.toComposeColor()

val TextPrimary = OpenAquaPalette.TextPrimary.toComposeColor()
val TextSecondary = OpenAquaPalette.TextSecondary.toComposeColor()
val TextMuted = OpenAquaPalette.TextMuted.toComposeColor()
val BorderOutline = OpenAquaPalette.BorderOutline.toComposeColor()

val StatusNominal = OpenAquaPalette.StatusNominal.toComposeColor()
val StatusWarning = OpenAquaPalette.StatusWarning.toComposeColor()
val StatusCritical = OpenAquaPalette.StatusCritical.toComposeColor()
