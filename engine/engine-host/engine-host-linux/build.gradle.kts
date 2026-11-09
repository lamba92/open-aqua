@file:OptIn(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi::class)

import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion

plugins {
    openaqua.convention.multiplatform
    openaqua.engine.producer
}

kotlin {
    linuxX64 {
        binaries {
            executable {
                baseName = "open-aqua-host-engine"
                entryPoint = "io.github.lamba92.openaqua.engine.host.linux.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
                linkerOpts("-Wl,--as-needed")
            }
        }
    }
    linuxArm64 {
        binaries {
            executable {
                baseName = "open-aqua-host-engine"
                entryPoint = "io.github.lamba92.openaqua.engine.host.linux.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
                linkerOpts("-Wl,--as-needed")
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine.engineHost)
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
        fromExecutable(kotlin.linuxX64().binaries.getExecutable("release"))
    }
    register("linuxArm64") {
        binaryPrefix = "open-aqua-host-engine"
        fromExecutable(kotlin.linuxArm64().binaries.getExecutable("release"))
    }
}
