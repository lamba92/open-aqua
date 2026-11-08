package io.github.lamba92.openaqua.packaging

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.writeText

@Serializable
data class FlatpakManifest(
    @SerialName("app-id")
    val appId: String,
    val runtime: String,
    @SerialName("runtime-version")
    val runtimeVersion: String,
    val sdk: String,
    val command: String,
    @SerialName("finish-args")
    val finishArgs: List<String>,
    val modules: List<FlatpakModule>,
)

@Serializable
data class FlatpakModule(
    val name: String,
    val buildsystem: String,
    @SerialName("build-commands")
    val buildCommands: List<String>,
    val sources: List<FlatpakSource>,
)

@Serializable
data class FlatpakSource(
    val type: String,
    val url: String? = null,
    val sha256: String? = null,
    val path: String? = null,
)

fun generateFlatpakFiles(
    metadata: AppMetadata,
    outputDir: Path,
    version: String,
) {
    val flatpakDir = (outputDir / "flatpak").createDirectories()
    val flatpakFile = flatpakDir / "${metadata.id}.yaml"
    flatpakFile.writeText(generateFlatpakYaml(metadata, version))
}

fun generateFlatpakYaml(
    metadata: AppMetadata,
    version: String,
): String {
    val manifest =
        FlatpakManifest(
            appId = metadata.id,
            runtime = "org.freedesktop.Platform",
            runtimeVersion = metadata.flatpak.runtimeVersion,
            sdk = "org.freedesktop.Sdk",
            command = metadata.executable,
            finishArgs =
                listOf(
                    "--share=ipc",
                    "--socket=x11",
                    "--socket=wayland",
                    "--device=all",
                    "--filesystem=/sys:ro",
                ),
            modules =
                listOf(
                    FlatpakModule(
                        name = metadata.executable,
                        buildsystem = "simple",
                        buildCommands =
                            listOf(
                                "install -d /app/lib/${metadata.executable} /app/bin /app/share/applications " +
                                    "/app/share/icons/hicolor/512x512/apps /app/share/metainfo",
                                "cp -r ${metadata.executable}/* /app/lib/${metadata.executable}/",
                                "chmod +x /app/lib/${metadata.executable}/bin/${metadata.executable}",
                                "ln -s /app/lib/${metadata.executable}/bin/${metadata.executable} /app/bin/${metadata.executable}",
                                "install -Dm644 ${metadata.id}.desktop /app/share/applications/${metadata.id}.desktop",
                                "install -Dm644 ${metadata.iconName}.png " +
                                    "/app/share/icons/hicolor/512x512/apps/${metadata.iconName}.png",
                                "install -Dm644 ${metadata.id}.metainfo.xml /app/share/metainfo/${metadata.id}.metainfo.xml",
                            ),
                        sources =
                            listOf(
                                FlatpakSource(
                                    type = "archive",
                                    url =
                                        "https://github.com/${metadata.project.githubRepo}/releases/download" +
                                            "/v$version/${metadata.executable}-$version-linux-x64.tar.gz",
                                    sha256 = "0000000000000000000000000000000000000000000000000000000000000000",
                                ),
                                FlatpakSource(type = "file", path = "../metadata/${metadata.id}.desktop"),
                                FlatpakSource(type = "file", path = "../metadata/${metadata.iconName}.png"),
                                FlatpakSource(type = "file", path = "../metadata/${metadata.id}.metainfo.xml"),
                            ),
                    ),
                ),
        )
    return yamlSerializer.encodeToString(FlatpakManifest.serializer(), manifest)
}
