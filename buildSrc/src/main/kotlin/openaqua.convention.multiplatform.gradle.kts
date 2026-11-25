import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    id("openaqua.base")
}

kotlin {
    jvmToolchain(21)
    targets
        .withType<KotlinNativeTarget>()
        .all {
            binaries.all {
                linkerOpts("--threads=8")
            }
        }
}

ktlint {
    version = "1.8.0"
    filter {
        exclude {
            it.file.path.contains("/build/") ||
                it.file.path.contains("/generated/")
        }
    }
}
