package io.github.lamba92.openaqua.app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import org.jetbrains.compose.resources.rememberResourceEnvironment
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
    rememberResourceEnvironment()
    val targetLocale =
        when (locale) {
            Locale.System -> systemDefaultLocale
            is Locale.Tag -> JavaLocale.forLanguageTag(locale.value)
        }
    DisposableEffect(targetLocale) {
        JavaLocale.setDefault(targetLocale)
        onDispose {
            JavaLocale.setDefault(systemDefaultLocale)
        }
    }

    CompositionLocalProvider(
        LocalAppLocale provides locale,
    ) {
        content()
    }
}
