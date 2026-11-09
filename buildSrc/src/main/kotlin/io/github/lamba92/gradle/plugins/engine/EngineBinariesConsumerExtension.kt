package io.github.lamba92.gradle.plugins.engine

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

open class EngineBinariesConsumerExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        val os: Property<Os> = objects.property<Os>().convention(Os.current())
        val arch: Property<Arch> = objects.property<Arch>().convention(Arch.current())
        val bundleAll: Property<Boolean> = objects.property<Boolean>().convention(false)

        fun all() {
            bundleAll.set(true)
        }
    }
