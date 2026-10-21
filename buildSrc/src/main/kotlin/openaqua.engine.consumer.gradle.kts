import io.github.lamba92.gradle.plugins.engine.ARCH_ATTRIBUTE
import io.github.lamba92.gradle.plugins.engine.Arch
import io.github.lamba92.gradle.plugins.engine.OS_ATTRIBUTE
import io.github.lamba92.gradle.plugins.engine.Os
import org.gradle.api.attributes.Usage
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import javax.inject.Inject

open class EngineBinariesConsumerExtension @Inject constructor(
    objects: ObjectFactory,
) {
    val os: Property<Os> = objects.property(Os::class.java).convention(Os.current())
    val arch: Property<Arch> = objects.property(Arch::class.java).convention(Arch.current())
    val bundleAll: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

    fun all() {
        bundleAll.set(true)
    }
}

val engineBinariesExtension =
    extensions.create<EngineBinariesConsumerExtension>("engineBinaries")

val engineBinaries =
    configurations.create("engineBinaries") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }

afterEvaluate {
    engineBinaries.attributes {
        if (engineBinariesExtension.bundleAll.get()) {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, "all-engine-binaries"))
        } else {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, "engine-binaries"))
            attribute(OS_ATTRIBUTE, engineBinariesExtension.os.get())
            attribute(ARCH_ATTRIBUTE, engineBinariesExtension.arch.get())
        }
    }
}

val syncEngineBinaries =
    tasks.register<Sync>("syncEngineBinaries") {
        group = "engine"
        description = "Syncs engine binaries into the generated resources directory"
        into(layout.buildDirectory.dir("generated/engineResources"))
        from(engineBinaries) {
            into("files/bin")
        }
    }

plugins.withId("org.jetbrains.kotlin.jvm") {
    val kotlin = extensions.getByType<KotlinJvmProjectExtension>()
    kotlin.sourceSets.named("main").configure {
        resources.srcDir(layout.buildDirectory.dir("generated/engineResources"))
    }
    tasks.named("processResources").configure {
        dependsOn(syncEngineBinaries)
    }
}

tasks.matching { it.name.startsWith("prepareComposeResourcesTask") }.configureEach {
    dependsOn(syncEngineBinaries)
}
