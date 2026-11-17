package io.github.lamba92.openaqua.app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.jvm.JvmInline

val LocalAppLocale = staticCompositionLocalOf<Locale> { error("Locale not provided") }

sealed interface Locale {
    object System : Locale

    @JvmInline
    value class Tag(
        val value: String,
    ) : Locale
}

@Composable
expect fun AppLocaleProvider(
    locale: Locale,
    content: @Composable () -> Unit,
)
