package io.github.lamba92.openaqua.packaging

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.writeText

@Serializable
data class SnapcraftManifest(
    val name: String,
    val base: String,
    val version: String,
    val summary: String,
    val description: String,
    val grade: String,
    val confinement: String,
    val apps: Map<String, SnapcraftApp>,
    val parts: Map<String, SnapcraftPart>,
)

@Serializable
data class SnapcraftApp(
    val command: String,
    val extensions: List<String> = emptyList(),
    val plugs: List<String> = emptyList(),
)

@Serializable
data class SnapcraftPart(
    val plugin: String,
    val source: String,
    @SerialName("stage-packages")
    val stagePackages: List<String> = emptyList(),
)

fun generateSnapFiles(
    metadata: AppMetadata,
    outputDir: Path,
    version: String,
) {
    val snapDir = (outputDir / "snap").createDirectories()
    val snapFile = snapDir / "snapcraft.yaml"
    snapFile.writeText(generateSnapcraftYaml(metadata, version))
}

fun generateSnapcraftYaml(
    metadata: AppMetadata,
    version: String,
): String {
    val manifest =
        SnapcraftManifest(
            name = metadata.executable,
            base = "core24",
            version = version,
            summary = metadata.summary,
            description = metadata.appstream.descriptionParagraphs.joinToString("\n"),
            grade = "stable",
            confinement = "strict",
            apps =
                mapOf(
                    metadata.executable to
                        SnapcraftApp(
                            command = "bin/${metadata.executable}",
                            extensions = listOf("gnome"),
                            plugs = listOf("raw-usb", "hardware-observe", "system-observe", "network"),
                        ),
                ),
            parts =
                mapOf(
                    metadata.executable to
                        SnapcraftPart(
                            plugin = "dump",
                            source = ".",
                            stagePackages = listOf("libasound2t64"),
                        ),
                ),
        )
    return yamlSerializer.encodeToString(SnapcraftManifest.serializer(), manifest)
}
