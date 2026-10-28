@file:OptIn(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi::class)

import io.github.lamba92.gradle.plugins.upx.UpxTask
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion

plugins {
    openaqua.engine
    id("io.github.lamba92.upx")
    id("openaqua.engine.producer")
}

upx {
    bruteLevel = UpxTask.BruteLevel.ULTRA_BRUTE
}

kotlin {
    linuxX64 {
        binaries {
            executable {
                baseName = "open-aqua-host-engine"
                entryPoint = "io.github.lamba92.openaqua.engine.host.linux.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
            }
        }
    }
    linuxArm64 {
        binaries {
            executable {
                baseName = "open-aqua-host-engine"
                entryPoint = "io.github.lamba92.openaqua.engine.host.linux.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.coreModel.coreModelHost)
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.io.core)
            api(libs.clikt)
            api(libs.kodein.di)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

engineBinaries {
    register("linuxX64") {
        binaryPrefix = "open-aqua-host-engine"
        fromUpx(
            kotlin.linuxX64().binaries.getExecutable("release"),
            tasks.named<UpxTask>("compressReleaseExecutableLinuxX64"),
        )
    }
    register("linuxArm64") {
        binaryPrefix = "open-aqua-host-engine"
        fromUpx(
            kotlin.linuxArm64().binaries.getExecutable("release"),
            tasks.named<UpxTask>("compressReleaseExecutableLinuxArm64"),
        )
    }
}
