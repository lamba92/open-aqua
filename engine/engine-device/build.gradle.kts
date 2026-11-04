@file:OptIn(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi::class)

import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    openaqua.convention.multiplatform
}

kotlin {
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries
            .getTest("debug")
            .disableNativeCache(
                version = DisableCacheInKotlinVersion.`2_4_0`,
                reason = "Clikt duplicate symbol workaround",
            )
    }

    linuxX64()
    linuxArm64()
    macosArm64()
    mingwX64()

    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
            api(projects.coreModel.coreModelDevice)
            api(libs.kodein.di)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
