package io.github.lamba92.gradle.plugins.packaging

import io.github.lamba92.gradle.plugins.platform.Os
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.security.MessageDigest
import kotlin.io.path.createDirectories

abstract class GeneratePackagingFilesTask : DefaultTask() {
    init {
        group = "distribution"
        description = "Generates packaging descriptor files for the target platform"
    }

    @get:InputFile
    abstract val metadataFile: RegularFileProperty

    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val targetOs: Property<Os>

    @get:InputDirectory
    abstract val rootProjectDir: DirectoryProperty

    @get:InputFile
    @get:Optional
    abstract val tarballArchive: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val tarballSha256: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val meta = jsonSerializer.decodeFromString<AppMetadata>(metadataFile.get().asFile.readText())
        val outPath =
            outputDir
                .get()
                .asFile
                .toPath()
                .createDirectories()
        val rootPath = rootProjectDir.get().asFile.toPath()
        val ver = version.get()
        val os = targetOs.get()

        val computedSha256 =
            tarballArchive.orNull?.asFile?.takeIf { it.exists() }?.let { tarFile ->
                tarFile.inputStream().use { input ->
                    val digest = MessageDigest.getInstance("SHA-256")
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                    digest.digest().joinToString("") { "%02x".format(it) }
                }
            } ?: tarballSha256.orNull

        when (os) {
            Os.LINUX -> {
                generateMetadataFiles(meta, outPath, rootPath, ver)
                generateFlatpakFiles(
                    metadata = meta,
                    outputDir = outPath,
                    version = ver,
                    tarballSha256 = computedSha256 ?: "0000000000000000000000000000000000000000000000000000000000000000",
                )
                generateSnapFiles(meta, outPath, ver)
                generateAurFiles(
                    metadata = meta,
                    outputDir = outPath,
                    version = ver,
                    tarballSha256 = computedSha256 ?: "SKIP",
                )
            }

            Os.WINDOWS -> {
                generateMsixFiles(meta, outPath, rootPath, ver)
            }

            else -> {}
        }
    }
}
