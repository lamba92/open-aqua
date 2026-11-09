package io.github.lamba92.gradle.plugins.platform

import io.github.lamba92.gradle.plugins.engine.Arch
import io.github.lamba92.gradle.plugins.engine.Os
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

open class PlatformExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        val os: Property<Os> = objects.property<Os>().convention(Os.current())
        val arch: Property<Arch> = objects.property<Arch>().convention(Arch.current())

        val classifier: Provider<String> =
            os.zip(arch) { targetOs, targetArch ->
                "${targetOs.name.lowercase()}-${targetArch.name.lowercase()}"
            }
    }
