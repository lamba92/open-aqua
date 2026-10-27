package io.github.lamba92.openaqua.app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale as JavaLocale

val LocalAppLocale = staticCompositionLocalOf<Locale> { error("Locale not provided") }

sealed interface Locale {
    object System : Locale

    @JvmInline
    value class Tag(
        val value: String,
    ) : Locale
}

private val systemDefaultLocale: JavaLocale = JavaLocale.getDefault()

@Composable
fun AppLocaleProvider(
    locale: Locale,
    content: @Composable () -> Unit,
) {
    val targetLocale =
        when (locale) {
            Locale.System -> systemDefaultLocale
            is Locale.Tag -> JavaLocale.forLanguageTag(locale.value)
        }
    JavaLocale.setDefault(targetLocale)

    CompositionLocalProvider(
        LocalAppLocale provides locale,
    ) {
        content()
    }
}
