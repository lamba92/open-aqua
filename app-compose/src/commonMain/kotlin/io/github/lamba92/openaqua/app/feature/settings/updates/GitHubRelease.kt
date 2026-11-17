package io.github.lamba92.openaqua.app.feature.settings.updates

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String,
    val name: String? = null,
    @SerialName("html_url")
    val htmlUrl: String,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @SerialName("published_at")
    val publishedAt: String? = null,
)

val GitHubRelease.displayName: String
    get() = name?.takeIf { it.isNotBlank() } ?: tagName

val GitHubRelease.semVer: SemVer?
    get() = SemVer.parseOrNull(tagName)
