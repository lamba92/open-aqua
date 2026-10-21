import io.github.lamba92.gradle.plugins.engine.TARGET_PLATFORM_ATTRIBUTE
import org.gradle.api.attributes.Usage
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

open class EngineBinariesConsumerExtension(
    val targetPlatform: Property<String>,
) {
    fun all() {
        targetPlatform.set("all")
    }
}

val engineBinariesExtension =
    extensions.create<EngineBinariesConsumerExtension>(
        "engineBinaries",
        objects.property<String>().convention(SupportedTargets.getCurrent().toTargetPlatformName()),
    )

val engineBinaries =
    configurations.create("engineBinaries") {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, "compressed-engine-binaries"))
        }
    }

afterEvaluate {
    engineBinaries.attributes.attribute(TARGET_PLATFORM_ATTRIBUTE, engineBinariesExtension.targetPlatform.get())
}

val syncEngineBinaries =
    tasks.register<Sync>("syncEngineBinaries") {
        group = "engine"
        description = "Syncs compressed engine binaries into the generated resources directory"
        into(layout.buildDirectory.dir("generated/engineResources"))
        from(engineBinaries) {
            into("files/bin")
        }
        from(engineBinaries) {
            into("composeResources/files/bin")
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
