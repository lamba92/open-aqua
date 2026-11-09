import io.github.lamba92.gradle.plugins.engine.ARCH_ATTRIBUTE
import io.github.lamba92.gradle.plugins.engine.EngineBinariesConsumerExtension
import io.github.lamba92.gradle.plugins.engine.OS_ATTRIBUTE
import io.github.lamba92.gradle.plugins.platform.PlatformExtension
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    id("openaqua.platform")
}

val platform = extensions.getByType<PlatformExtension>()

val engineBinariesExtension =
    extensions.create<EngineBinariesConsumerExtension>("engineBinaries")

engineBinariesExtension.os.convention(platform.os)
engineBinariesExtension.arch.convention(platform.arch)

val engineBinaries =
    configurations.create("engineBinaries") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }

afterEvaluate {
    engineBinaries.attributes {
        if (engineBinariesExtension.bundleAll.get()) {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("all-engine-binaries"))
        } else {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("engine-binaries"))
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
