package io.github.lamba92.gradle.plugins.upx

import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family
import java.io.File

@Suppress("unused")
class UpxPlugin : Plugin<Project> {
    open class Extension(
        private val name: String,
        val version: Property<String>,
        val localUpxPath: RegularFileProperty,
        val upxExecutableProvider: Provider<RegularFile>,
        val strip: Property<Boolean>,
        val lzma: Property<Boolean>,
        val bruteLevel: Property<UpxTask.BruteLevel>,
        val compressionLevel: Property<UpxTask.CompressionLevel>,
        val stripExecutable: Property<String>,
        val additionalOptions: ListProperty<String>,
    ) : Named {
        override fun getName(): String = name

        fun ultraBrute() {
            bruteLevel.set(UpxTask.BruteLevel.ULTRA_BRUTE)
        }

        fun brute() {
            bruteLevel.set(UpxTask.BruteLevel.BRUTE)
        }
    }

    override fun apply(target: Project): Unit =
        with(target) {
            val versionProperty = objects.property<String>().convention("5.2.1")
            val localUpxPath = objects.fileProperty()
            val stripProperty = objects.property<Boolean>().convention(true)
            val lzmaProperty = objects.property<Boolean>().convention(true)
            val bruteLevelProperty = objects.property<UpxTask.BruteLevel>()
            val compressionLevelProperty =
                objects.property<UpxTask.CompressionLevel>().convention(UpxTask.CompressionLevel.Best)
            val stripExecutableProperty = objects.property<String>().convention("strip")
            val additionalOptionsProperty = objects.listProperty<String>().convention(emptyList())

            val os = OperatingSystem.current()
            if (os.isMacOsX || os.isLinux) {
                val upxFromSystem =
                    providers.exec {
                        commandLine("whereis", "upx")
                        isIgnoreExitValue = true
                    }.standardOutput.asText.flatMap { output ->
                        val file =
                            output.lines()
                                .firstOrNull { it.startsWith("upx: ") }
                                ?.removePrefix("upx: ")
                                ?.split(" ")
                                ?.firstOrNull { it.isNotBlank() }
                                ?.let { File(it) }
                                ?.takeIf { it.exists() }
                        if (file != null) {
                            layout.file(provider { file })
                        } else {
                            providers.provider { null }
                        }
                    }
                localUpxPath.convention(upxFromSystem)
            }

            val upxDownloadTask =
                rootProject.tasks.maybeRegister("downloadUpx", DownloadTask::class.java) {
                    group = "upx"
                    outputFile.set(
                        rootProject.layout.buildDirectory.file(
                            "upx/downloads/" + if (os.isWindows) "upx.zip" else "upx.tar.xz",
                        ),
                    )
                    url.set(
                        versionProperty.map { ver ->
                            val platform =
                                UpxSupportedOperatingSystems.current()
                                    ?: error("Current OS \"$os\" is not supported for UPX download.")
                            UpxSupportedOperatingSystems.buildUpxUrl(ver, platform)
                        },
                    )
                }

            val unzipUpx =
                rootProject.tasks.maybeRegister("unzipUpx", Sync::class.java) {
                    group = "upx"
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    from(
                        upxDownloadTask.map {
                            if (os.isWindows) zipTree(it.outputFile) else tarTree(resources.xz(it.outputFile))
                        },
                    )
                    include { it.name == "upx" || it.name == "upx.exe" }
                    eachFile { relativePath = RelativePath(true, name) }
                    into(rootProject.layout.buildDirectory.dir("upx/exec"))
                }

            val defaultUpxExecutable: Provider<RegularFile> =
                rootProject.layout.buildDirectory.file(
                    if (os.isWindows) "upx/exec/upx.exe" else "upx/exec/upx",
                )

            val upxExecutable: Provider<RegularFile> = localUpxPath.orElse(defaultUpxExecutable)

            val extension =
                extensions.create<Extension>(
                    "upx",
                    "upx",
                    versionProperty,
                    localUpxPath,
                    upxExecutable,
                    stripProperty,
                    lzmaProperty,
                    bruteLevelProperty,
                    compressionLevelProperty,
                    stripExecutableProperty,
                    additionalOptionsProperty,
                )

            val compress =
                tasks.maybeRegister("compress") {
                    group = "upx"
                    description = "Compress all native executables with UPX"
                }

            val compressRelease =
                tasks.maybeRegister("compressRelease") {
                    group = "upx"
                    description = "Compress all release native executables with UPX"
                }

            plugins.withId("org.jetbrains.kotlin.multiplatform") {
                val kotlinExtension = extensions.getByType<KotlinMultiplatformExtension>()
                kotlinExtension.targets.withType<KotlinNativeTarget>().configureEach {
                    val nativeTarget = this
                    binaries.withType<Executable>().configureEach {
                        val linkTaskProvider = linkTaskProvider
                        val targetName = nativeTarget.name
                        val buildTypeName = buildType.name.lowercase()
                        val taskSuffix =
                            linkTaskName.removePrefix("link").replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase() else it.toString()
                            }

                        val compressTask =
                            tasks.register<UpxTask>("compress$taskSuffix") {
                                dependsOn(linkTaskProvider)
                                dependsOn(
                                    provider {
                                        if (localUpxPath.isPresent) emptyList<TaskProvider<*>>() else listOf(unzipUpx)
                                    },
                                )
                                inputExecutable.fileProvider(linkTaskProvider.flatMap { it.outputFile })
                                upxExecutableFile.set(upxExecutable)

                                strip.convention(extension.strip)
                                lzma.convention(extension.lzma)
                                bruteLevel.convention(extension.bruteLevel)
                                compressionLevel.convention(extension.compressionLevel)
                                stripExecutable.convention(extension.stripExecutable)
                                additionalOptions.convention(extension.additionalOptions)

                                val isWindows = konanTarget.family == Family.MINGW
                                val binaryExtension = if (isWindows) ".exe" else ""
                                val outputFileName = "${project.name}$binaryExtension"
                                outputExecutable.set(
                                    layout.buildDirectory.file(
                                        "bin/upx/$targetName/$buildTypeName/$outputFileName",
                                    ),
                                )
                            }

                        compress.configure {
                            dependsOn(compressTask)
                        }

                        if (buildTypeName == "release") {
                            compressRelease.configure {
                                dependsOn(compressTask)
                            }
                        }

                        tasks.register<Exec>("runCompressed$taskSuffix") {
                            dependsOn(compressTask)
                            group = "run"
                            val output = compressTask.flatMap { it.outputExecutable }
                            commandLine(output.map { it.asFile.absolutePath })

                            onlyIf {
                                when (konanTarget.family) {
                                    Family.OSX -> os.isMacOsX
                                    Family.LINUX -> os.isLinux
                                    Family.MINGW -> os.isWindows
                                    else -> false
                                }
                            }
                        }
                    }
                }
            }
        }
}

private fun <T : Task> TaskContainer.maybeRegister(
    name: String,
    type: Class<T>,
    configuration: T.() -> Unit,
): TaskProvider<T> =
    if (name in names) {
        named(name, type).apply { configure(configuration) }
    } else {
        register(name, type, configuration)
    }

private fun TaskContainer.maybeRegister(
    name: String,
    configuration: Task.() -> Unit,
): TaskProvider<Task> =
    if (name in names) {
        named(name).apply { configure(configuration) }
    } else {
        register(name, configuration)
    }
