package io.github.lamba92.gradle.plugins.distribution

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest

abstract class PublishReleaseArtifactsTask : DefaultTask() {
    init {
        group = "distribution"
    }

    @get:InputFiles
    abstract val releaseFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val destinationDir: DirectoryProperty

    @TaskAction
    fun publish() {
        val targetDir = destinationDir.get().asFile
        targetDir.mkdirs()

        val allFiles =
            releaseFiles.files.flatMap { file ->
                if (file.isDirectory) {
                    file.walk().filter { it.isFile }.toList()
                } else if (file.isFile) {
                    listOf(file)
                } else {
                    emptyList()
                }
            }

        allFiles.forEach { file ->
            val dest = File(targetDir, file.name)
            file.copyTo(dest, overwrite = true)
            val hash = dest.sha256Hex()
            File(targetDir, "${dest.name}.sha256").writeText("$hash  ${dest.name}\n")
            logger.lifecycle("Published release asset: ${dest.name} (${dest.length() / 1024} KB)")
        }
    }

    private fun File.sha256Hex(): String {
        val md = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
