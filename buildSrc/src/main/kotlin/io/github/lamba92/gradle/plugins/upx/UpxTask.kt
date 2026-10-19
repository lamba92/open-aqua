package io.github.lamba92.gradle.plugins.upx

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecOperations
import java.io.File
import java.io.Serializable
import javax.inject.Inject

abstract class UpxTask @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {

    init {
        group = "upx"
        command.convention(Command.COMPRESS)
        logLevel.convention(LogLevel.NORMAL)
        exact.convention(false)
        overlay.convention(Overlay.COPY)
        compressionLevel.convention(CompressionLevel.Best)
        strip.convention(true)
        lzma.convention(true)
        stripExecutable.convention("strip")
        additionalOptions.convention(emptyList())
        outputExecutable.convention(
            inputExecutable.flatMap { input ->
                val file = input.asFile
                val name =
                    buildString {
                        append(file.nameWithoutExtension)
                        append("-compressed")
                        if (OperatingSystem.current().isWindows) append(".exe")
                    }
                project.layout.file(project.provider { file.parentFile.resolve(name) })
            },
        )
    }

    enum class Command(
        val command: String? = null,
    ) {
        COMPRESS,
        DECOMPRESS("-d"),
        TEST("-t"),
        LIST("-l"),
    }

    enum class LogLevel(
        val command: String? = null,
    ) {
        NORMAL,
        NO_WARNINGS("-q"),
        NO_ERRORS("-qq"),
        OFF("-qqq"),
    }

    enum class Overlay {
        COPY,
        STRIP,
        SKIP,
    }

    sealed class CompressionLevel : Serializable {
        abstract val command: String

        data class Number(
            val amount: Int,
        ) : CompressionLevel() {
            override val command = "-$amount"
        }

        object Best : CompressionLevel() {
            @Suppress("unused")
            private fun readResolve(): Any = Best

            override val command = "--best"
        }
    }

    enum class BruteLevel(
        val command: String,
    ) {
        BRUTE("--brute"),
        ULTRA_BRUTE("--ultra-brute"),
    }

    @get:InputFile
    abstract val inputExecutable: RegularFileProperty

    @get:OutputFile
    @get:Optional
    abstract val outputExecutable: RegularFileProperty

    @get:Input
    abstract val command: Property<Command>

    @get:Input
    abstract val logLevel: Property<LogLevel>

    @get:Input
    abstract val exact: Property<Boolean>

    @get:Input
    abstract val overlay: Property<Overlay>

    @get:Input
    abstract val compressionLevel: Property<CompressionLevel>

    @get:Input
    @get:Optional
    abstract val bruteLevel: Property<BruteLevel>

    @get:Input
    abstract val strip: Property<Boolean>

    @get:Input
    abstract val lzma: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val stripExecutable: Property<String>

    @get:Input
    abstract val additionalOptions: ListProperty<String>

    @get:InputFile
    abstract val upxExecutableFile: RegularFileProperty

    @TaskAction
    fun execute() {
        val outputFile = outputExecutable.get().asFile
        if (outputFile.exists()) {
            outputFile.delete()
        }
        outputFile.parentFile?.mkdirs()

        val inputFile = inputExecutable.get().asFile
        val fileToCompress: File = if (strip.get()) {
            val strippedFile = temporaryDir.resolve("stripped-${inputFile.name}")
            if (strippedFile.exists()) {
                strippedFile.delete()
            }
            val os = OperatingSystem.current()
            val stripTool = stripExecutable.get()
            val stripResult = runCatching {
                if (os.isMacOsX) {
                    inputFile.copyTo(strippedFile, overwrite = true)
                    execOperations.exec {
                        executable = stripTool
                        args(strippedFile.absolutePath)
                    }
                } else {
                    execOperations.exec {
                        executable = stripTool
                        args("-s", inputFile.absolutePath, "-o", strippedFile.absolutePath)
                    }
                }
            }
            if (stripResult.isSuccess && strippedFile.exists()) {
                strippedFile
            } else {
                logger.warn("Stripping executable before UPX compression failed: ${stripResult.exceptionOrNull()?.message}. Using original binary.")
                inputFile
            }
        } else {
            inputFile
        }

        try {
            execOperations.exec {
                executable = upxExecutableFile.get().asFile.absolutePath
                args(
                    buildList {
                        command.get().command?.let { add(it) }
                        add("-o")
                        add(outputFile.absolutePath)
                        logLevel.get().command?.let { add(it) }
                        if (exact.get()) add("--exact")
                        if (lzma.get()) add("--lzma")
                        add("--overlay=${overlay.get().name.lowercase()}")
                        add(compressionLevel.get().command)
                        bruteLevel.orNull?.let { add(it.command) }
                        addAll(additionalOptions.get())
                        add(fileToCompress.absolutePath)
                    },
                )
            }
        } finally {
            if (fileToCompress != inputFile && fileToCompress.exists()) {
                fileToCompress.delete()
            }
        }
    }
}
