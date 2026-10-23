package io.github.lamba92.gradle.plugins.engine

import SupportedTargets
import io.github.lamba92.gradle.plugins.upx.UpxTask
import org.gradle.api.Named
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import javax.inject.Inject

abstract class EngineBinarySpec @Inject constructor(
    private val name: String,
    private val layout: ProjectLayout,
) : Named {
    override fun getName(): String = name

    abstract val os: Property<Os>
    abstract val arch: Property<Arch>
    abstract val binaryFile: RegularFileProperty
    abstract val canonicalName: Property<String>

    init {
        val matched = SupportedTargets.values().firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (matched != null) {
            os.convention(matched.os)
            arch.convention(matched.arch)
        }
        canonicalName.convention(
            os.flatMap { o ->
                arch.map { a ->
                    canonicalBinaryName(o, a)
                }
            },
        )
    }

    fun fromExecutable(executable: Executable) {
        val (detectedOs, detectedArch) = executable.target.konanTarget.toOsAndArch()
        os.convention(detectedOs)
        arch.convention(detectedArch)
        binaryFile.set(layout.file(executable.linkTaskProvider.map { it.outputFile.get() }))
    }

    fun fromUpx(
        executable: Executable,
        upxTask: TaskProvider<UpxTask>,
    ) {
        fromExecutable(executable)
        binaryFile.set(upxTask.flatMap { it.outputExecutable })
    }
}
