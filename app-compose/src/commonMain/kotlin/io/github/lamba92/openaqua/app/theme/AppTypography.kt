package io.github.lamba92.openaqua.app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.serialization.Serializable
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.font_family_jetbrains_mono
import open_aqua.app_compose.generated.resources.font_family_system_default
import open_aqua.app_compose.generated.resources.jetbrains_mono_bold
import open_aqua.app_compose.generated.resources.jetbrains_mono_bold_italic
import open_aqua.app_compose.generated.resources.jetbrains_mono_italic
import open_aqua.app_compose.generated.resources.jetbrains_mono_medium
import open_aqua.app_compose.generated.resources.jetbrains_mono_regular
import open_aqua.app_compose.generated.resources.jetbrains_mono_semibold
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.StringResource

object FontScaleConstants {
    const val MIN_SP = 12
    const val MAX_SP = 22
    const val DEFAULT_SP = 15

    val PRESETS = listOf(13, 15, 17, 20)
}

fun calculateFontScaleFactor(sp: Int): Float = sp.toFloat() / FontScaleConstants.DEFAULT_SP.toFloat()

@Serializable
enum class AppFontFamily {
    JETBRAINS_MONO,
    SYSTEM_DEFAULT,
}

val AppFontFamily.stringResource: StringResource
    get() =
        when (this) {
            AppFontFamily.JETBRAINS_MONO -> Res.string.font_family_jetbrains_mono
            AppFontFamily.SYSTEM_DEFAULT -> Res.string.font_family_system_default
        }

data class AppTypography(
    val fontPreference: AppFontFamily = AppFontFamily.JETBRAINS_MONO,
    val fontSp: Int = FontScaleConstants.DEFAULT_SP,
    val fontScaleFactor: Float = 1.0f,
    val defaultFontFamily: FontFamily = FontFamily.Default,
    val monospaceFontFamily: FontFamily = FontFamily.Monospace,
)

@Composable
fun rememberJetBrainsMonoFontFamily(): FontFamily {
    val regular = Font(Res.font.jetbrains_mono_regular, FontWeight.Normal, FontStyle.Normal)
    val medium = Font(Res.font.jetbrains_mono_medium, FontWeight.Medium, FontStyle.Normal)
    val semiBold = Font(Res.font.jetbrains_mono_semibold, FontWeight.SemiBold, FontStyle.Normal)
    val bold = Font(Res.font.jetbrains_mono_bold, FontWeight.Bold, FontStyle.Normal)
    val italic = Font(Res.font.jetbrains_mono_italic, FontWeight.Normal, FontStyle.Italic)
    val boldItalic = Font(Res.font.jetbrains_mono_bold_italic, FontWeight.Bold, FontStyle.Italic)
    return remember {
        FontFamily(regular, medium, semiBold, bold, italic, boldItalic)
    }
}
