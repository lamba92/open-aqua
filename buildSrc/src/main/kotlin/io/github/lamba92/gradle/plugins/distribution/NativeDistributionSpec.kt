package io.github.lamba92.gradle.plugins.distribution

import io.github.lamba92.gradle.plugins.engine.Arch
import io.github.lamba92.gradle.plugins.engine.Os
import org.gradle.api.Named
import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import javax.inject.Inject

abstract class NativeDistributionSpec
    @Inject
    constructor(
        private val name: String,
    ) : Named {
        override fun getName(): String = name

        abstract val os: Property<Os>
        abstract val arch: Property<Arch>
        abstract val format: Property<PackageFormat>
        abstract val files: ConfigurableFileCollection
        abstract val task: Property<TaskProvider<out Task>>

        fun from(task: TaskProvider<out Task>) {
            this.task.set(task)
            files.from(task)
        }

        fun from(vararg paths: Any) {
            files.from(*paths)
        }
    }
