package io.github.lamba92.openaqua.packaging

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.serialization.XmlValue
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.writeText

@Serializable
@XmlSerialName("component", "", "")
data class AppStreamComponent(
    val type: String = "desktop-application",
    @XmlElement(true)
    val id: String,
    @XmlElement(true)
    @XmlSerialName("metadata_license", "", "")
    val metadataLicense: String,
    @XmlElement(true)
    @XmlSerialName("project_license", "", "")
    val projectLicense: String,
    @XmlElement(true)
    val name: String,
    @XmlElement(true)
    val summary: String,
    @XmlElement(true)
    val description: AppStreamDescription,
    @XmlElement(true)
    val launchable: AppStreamLaunchable,
    val url: List<AppStreamUrl>,
    @XmlElement(true)
    val developer: AppStreamDeveloper,
    @XmlElement(true)
    val screenshots: AppStreamScreenshots,
    @XmlElement(true)
    val categories: AppStreamCategories,
    @XmlElement(true)
    @XmlSerialName("content_rating", "", "")
    val contentRating: AppStreamContentRating,
    @XmlElement(true)
    val releases: AppStreamReleases,
)

@Serializable
@XmlSerialName("description", "", "")
data class AppStreamDescription(
    @XmlElement(true)
    val p: List<String>,
    @XmlElement(true)
    val ul: AppStreamUl,
)

@Serializable
@XmlSerialName("ul", "", "")
data class AppStreamUl(
    @XmlElement(true)
    val li: List<String>,
)

@Serializable
@XmlSerialName("launchable", "", "")
data class AppStreamLaunchable(
    val type: String = "desktop-id",
    @XmlValue
    val value: String,
)

@Serializable
@XmlSerialName("url", "", "")
data class AppStreamUrl(
    val type: String,
    @XmlValue
    val value: String,
)

@Serializable
@XmlSerialName("developer", "", "")
data class AppStreamDeveloper(
    val id: String,
    @XmlElement(true)
    val name: String,
)

@Serializable
@XmlSerialName("screenshots", "", "")
data class AppStreamScreenshots(
    @XmlElement(true)
    val screenshot: List<AppStreamScreenshot>?,
)

@Serializable
@XmlSerialName("screenshot", "", "")
data class AppStreamScreenshot(
    val type: String? = null,
    @XmlElement(true)
    val caption: String,
    @XmlElement(true)
    val image: String,
)

@Serializable
@XmlSerialName("categories", "", "")
data class AppStreamCategories(
    @XmlElement(true)
    val category: List<String>,
)

@Serializable
@XmlSerialName("content_rating", "", "")
data class AppStreamContentRating(
    val type: String = "oars-1.1",
)

@Serializable
@XmlSerialName("releases", "", "")
data class AppStreamReleases(
    @XmlElement(true)
    val release: List<AppStreamRelease>,
)

@Serializable
@XmlSerialName("release", "", "")
data class AppStreamRelease(
    val version: String,
    val date: String,
    @XmlElement(true)
    val description: AppStreamReleaseDescription,
)

@Serializable
@XmlSerialName("description", "", "")
data class AppStreamReleaseDescription(
    @XmlElement(true)
    val p: String,
)

fun generateMetadataFiles(
    metadata: AppMetadata,
    outputDir: Path,
    rootDir: Path,
    version: String,
) {
    val metadataDir = (outputDir / "metadata").createDirectories()
    val appImageDir = (outputDir / "appimage").createDirectories()

    // 1. AppStream metainfo.xml
    val metainfoFile = metadataDir / "${metadata.id}.metainfo.xml"
    metainfoFile.writeText(generateAppStreamXml(metadata, version))

    // 2. Desktop files
    val desktopFile = metadataDir / "${metadata.id}.desktop"
    desktopFile.writeText(generateDesktopFile(metadata, icon = metadata.iconName))

    val appImageDesktopFile = appImageDir / "${metadata.executable}.desktop"
    appImageDesktopFile.writeText(generateDesktopFile(metadata, icon = metadata.executable))

    // 3. AppRun script for AppImage
    val appRunFile = appImageDir / "AppRun"
    appRunFile.writeText(generateAppRunScript())
    appRunFile.setExecutable()

    // 4. udev rule
    val udevFile = metadataDir / "99-aquacomputer.rules"
    udevFile.writeText(metadata.udevRule.trimIndent() + "\n")

    // 5. Copy 512px icon if present
    val iconSrc = rootDir / "assets/generated/icon-512.png"
    if (iconSrc.exists()) {
        iconSrc.copyTo(metadataDir / "${metadata.iconName}.png", overwrite = true)
    }
}

private fun Path.setExecutable() {
    runCatching {
        Files.setPosixFilePermissions(this, PosixFilePermissions.fromString("rwxr-xr-x"))
    }.onFailure {
        toFile().setExecutable(true, false)
    }
}

fun generateAppStreamXml(
    metadata: AppMetadata,
    version: String,
): String {
    val component =
        AppStreamComponent(
            id = metadata.id,
            metadataLicense = metadata.project.metadataLicense,
            projectLicense = metadata.project.license,
            name = metadata.name,
            summary = metadata.summary,
            description =
                AppStreamDescription(
                    p = metadata.appstream.descriptionParagraphs + listOf("Features include:"),
                    ul = AppStreamUl(li = metadata.appstream.features),
                ),
            launchable = AppStreamLaunchable(value = "${metadata.id}.desktop"),
            url =
                listOf(
                    AppStreamUrl("homepage", metadata.project.homepage),
                    AppStreamUrl("bugtracker", metadata.project.bugtracker),
                    AppStreamUrl("vcs-browser", metadata.project.homepage),
                ),
            developer =
                AppStreamDeveloper(
                    id = metadata.developer.id,
                    name = metadata.developer.name,
                ),
            screenshots =
                AppStreamScreenshots(
                    screenshot =
                        metadata.appstream.screenshots.map {
                            AppStreamScreenshot(
                                type = if (it.isDefault) "default" else null,
                                caption = it.caption,
                                image = it.url,
                            )
                        },
                ),
            categories = AppStreamCategories(category = metadata.appstream.categories),
            contentRating = AppStreamContentRating(),
            releases =
                AppStreamReleases(
                    release =
                        listOf(
                            AppStreamRelease(
                                version = version,
                                date = "2026-11-08",
                                description = AppStreamReleaseDescription("Release $version of ${metadata.name}."),
                            ),
                        ),
                ),
        )
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        xmlSerializer.encodeToString(AppStreamComponent.serializer(), component) + "\n"
}

fun generateDesktopFile(
    metadata: AppMetadata,
    icon: String,
): String =
    """
    |[Desktop Entry]
    |Name=${metadata.name}
    |Comment=${metadata.comment}
    |Exec=${metadata.executable} %U
    |Icon=$icon
    |Terminal=false
    |Type=Application
    |Categories=${metadata.appstream.categories.joinToString(";")};
    |StartupWMClass=${metadata.mainClass}
    """.trimMargin() + "\n"

fun generateAppRunScript(): String =
    """
    |#!/bin/sh
    |set -e
    |
    |HERE="${'$'}(dirname "${'$'}(readlink -f "${'$'}0")")"
    |export APPDIR="${'$'}HERE"
    |export PATH="${'$'}HERE/usr/bin:${'$'}PATH"
    |export LD_LIBRARY_PATH="${'$'}HERE/usr/lib:${'$'}LD_LIBRARY_PATH"
    |export XDG_DATA_DIRS="${'$'}HERE/usr/share:${'$'}{XDG_DATA_DIRS:-/usr/local/share:/usr/share}"
    |
    |exec "${'$'}HERE/usr/bin/open-aqua" "${'$'}@"
    """.trimMargin() + "\n"
