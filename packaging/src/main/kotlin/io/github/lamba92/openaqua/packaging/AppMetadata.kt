package io.github.lamba92.openaqua.packaging

import kotlinx.serialization.Serializable

@Serializable
data class AppMetadata(
    val id: String,
    val name: String,
    val summary: String,
    val comment: String,
    val executable: String,
    val mainClass: String,
    val iconName: String,
    val udevRule: String = "",
    val project: Project = Project(),
    val developer: Developer = Developer(),
    val appstream: AppStream = AppStream(),
    val flatpak: Flatpak = Flatpak(),
    val msix: Msix = Msix(),
) {
    @Serializable
    data class Project(
        val githubRepo: String = "",
        val homepage: String = "",
        val bugtracker: String = "",
        val license: String = "Apache-2.0",
        val metadataLicense: String = "CC0-1.0",
    )

    @Serializable
    data class Developer(
        val id: String = "",
        val name: String = "",
        val email: String = "",
    )

    @Serializable
    data class AppStream(
        val categories: List<String> = emptyList(),
        val descriptionParagraphs: List<String> = emptyList(),
        val features: List<String> = emptyList(),
        val screenshots: List<Screenshot> = emptyList(),
    )

    @Serializable
    data class Flatpak(
        val runtimeVersion: String = "24.08",
    )

    @Serializable
    data class Msix(
        val identityName: String = "",
        val publisher: String = "",
        val publisherDisplayName: String = "",
        val storeId: String = "",
    )

    @Serializable
    data class Screenshot(
        val caption: String,
        val url: String,
        val isDefault: Boolean = false,
    )
}
