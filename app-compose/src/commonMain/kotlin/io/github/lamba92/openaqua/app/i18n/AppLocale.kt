package io.github.lamba92.openaqua.app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import com.compose.sdl.res.preferredLocaleTagsProvider
import kotlin.jvm.JvmInline

val LocalAppLocale = staticCompositionLocalOf<Locale> { error("Locale not provided") }

sealed interface Locale {
    object System : Locale

    @JvmInline
    value class Tag(
        val value: String,
    ) : Locale
}

private var originalLocaleTagsProvider = preferredLocaleTagsProvider

@Composable
fun AppLocaleProvider(
    locale: Locale,
    content: @Composable () -> Unit,
) {
    DisposableEffect(locale) {
        preferredLocaleTagsProvider = {
            when (locale) {
                Locale.System -> originalLocaleTagsProvider?.invoke() ?: listOf("en-US")
                is Locale.Tag -> listOf(locale.value)
            }
        }
        onDispose {
            preferredLocaleTagsProvider = originalLocaleTagsProvider
        }
    }

    CompositionLocalProvider(
        LocalAppLocale provides locale,
    ) {
        content()
    }
}
