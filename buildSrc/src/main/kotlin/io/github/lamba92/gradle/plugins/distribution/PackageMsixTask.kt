package io.github.lamba92.gradle.plugins.distribution

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

abstract class PackageMsixTask
    @Inject
    constructor(
        private val execOperations: ExecOperations,
        private val fileSystemOperations: FileSystemOperations,
        private val archiveOperations: ArchiveOperations,
    ) : DefaultTask() {
        init {
            group = "distribution"
            description = "Packages the Windows release distribution into a .msix package using MakeAppx"
        }

        @get:InputFiles
        abstract val distributionPackages: ConfigurableFileCollection

        @get:InputDirectory
        abstract val packagingDir: DirectoryProperty

        @get:OutputFile
        abstract val outputMsix: RegularFileProperty

        @TaskAction
        fun packageMsix() {
            if (distributionPackages.isEmpty) {
                error("No distribution packages dependency was configured for MSIX creation.")
            }
            val packagesDir = distributionPackages.singleFile
            val zipFile =
                packagesDir.walk().firstOrNull { it.isFile && it.name.endsWith(".zip") }
                    ?: error("Could not find Windows distribution zip archive in $packagesDir")

            val targetStagingDir = temporaryDir.resolve("layout")
            targetStagingDir.deleteRecursively()
            targetStagingDir.mkdirs()

            // 1. Unpack the zip distributable
            fileSystemOperations.copy {
                from(archiveOperations.zipTree(zipFile))
                into(targetStagingDir)
                eachFile {
                    val normalized = path.replace('\\', '/')
                    if (normalized.startsWith("open-aqua/")) {
                        path = normalized.removePrefix("open-aqua/")
                    }
                }
                includeEmptyDirs = false
            }

            // 2. Copy AppxManifest.xml & Assets from generated packaging
            val sourcePackagingDir = packagingDir.get().asFile
            val manifestFile = File(sourcePackagingDir, "AppxManifest.xml")
            if (!manifestFile.exists()) {
                error("MSIX manifest not found at: ${manifestFile.absolutePath}")
            }
            manifestFile.copyTo(File(targetStagingDir, "AppxManifest.xml"), overwrite = true)

            val assetsDir = File(sourcePackagingDir, "Assets")
            if (assetsDir.exists()) {
                assetsDir.copyRecursively(File(targetStagingDir, "Assets"), overwrite = true)
            }

            // 3. Locate MakeAppx.exe
            val makeAppxExe =
                WindowsSdkUtils.findMakeAppxExecutable()
                    ?: error("MakeAppx.exe could not be found in Windows Kits or PATH. Ensure Windows SDK is installed.")

            logger.lifecycle("Found MakeAppx at: ${makeAppxExe.absolutePath}")

            // 4. Pack MSIX with /nv
            val finalMsix = outputMsix.get().asFile
            finalMsix.parentFile.mkdirs()
            finalMsix.delete()

            val stdoutStream = ByteArrayOutputStream()
            val stderrStream = ByteArrayOutputStream()
            val result =
                execOperations.exec {
                    commandLine(
                        makeAppxExe.absolutePath,
                        "pack",
                        "/d",
                        targetStagingDir.absolutePath,
                        "/p",
                        finalMsix.absolutePath,
                        "/nv",
                    )
                    standardOutput = stdoutStream
                    errorOutput = stderrStream
                    isIgnoreExitValue = true
                }

            val stdout = stdoutStream.toString()
            val stderr = stderrStream.toString()
            val logText =
                buildString {
                    if (stdout.isNotBlank()) append(stdout)
                    if (stderr.isNotBlank()) {
                        if (isNotEmpty()) appendLine()
                        append(stderr)
                    }
                }
            if (logText.isNotBlank()) {
                logger.lifecycle("MakeAppx output:\n$logText")
            }
            if (result.exitValue != 0) {
                error("MakeAppx pack failed with exit code ${result.exitValue}:\n$logText")
            }

            logger.lifecycle("MSIX package generated successfully at: ${finalMsix.absolutePath}")
        }
    }
