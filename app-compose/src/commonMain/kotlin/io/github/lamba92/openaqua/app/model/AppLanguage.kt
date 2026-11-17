package io.github.lamba92.openaqua.app.model

import io.github.lamba92.openaqua.app.i18n.Locale
import kotlinx.serialization.Serializable
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.language_english
import open_aqua.app_compose.generated.resources.language_french
import open_aqua.app_compose.generated.resources.language_german
import open_aqua.app_compose.generated.resources.language_italian
import open_aqua.app_compose.generated.resources.language_spanish
import open_aqua.app_compose.generated.resources.language_system_default
import org.jetbrains.compose.resources.StringResource

@Serializable
enum class AppLanguage(
    val isoCode: String?,
) {
    SYSTEM(null),
    ENGLISH("en"),
    GERMAN("de"),
    FRENCH("fr"),
    ITALIAN("it"),
    SPANISH("es"),
}

val AppLanguage.locale: Locale
    get() =
        when (isoCode) {
            null -> Locale.System
            else -> Locale.Tag(isoCode)
        }

val AppLanguage.stringResource: StringResource
    get() =
        when (this) {
            AppLanguage.SYSTEM -> Res.string.language_system_default
            AppLanguage.ENGLISH -> Res.string.language_english
            AppLanguage.GERMAN -> Res.string.language_german
            AppLanguage.FRENCH -> Res.string.language_french
            AppLanguage.ITALIAN -> Res.string.language_italian
            AppLanguage.SPANISH -> Res.string.language_spanish
        }
