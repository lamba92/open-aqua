package io.github.lamba92.openaqua.app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import com.compose.sdl.res.preferredLocaleTagsProvider

private val originalLocaleTagsProvider = preferredLocaleTagsProvider

@Composable
actual fun AppLocaleProvider(
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
