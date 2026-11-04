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
                baseName = "open-aqua-device-engine-mock"
                entryPoint = "io.github.lamba92.openaqua.engine.device.mock.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
            }
        }
    }
    linuxArm64 {
        binaries {
            executable {
                baseName = "open-aqua-device-engine-mock"
                entryPoint = "io.github.lamba92.openaqua.engine.device.mock.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
            }
        }
    }
    macosArm64 {
        binaries {
            executable {
                baseName = "open-aqua-device-engine-mock"
                entryPoint = "io.github.lamba92.openaqua.engine.device.mock.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
            }
        }
    }
    mingwX64 {
        binaries {
            executable {
                baseName = "open-aqua-device-engine-mock"
                entryPoint = "io.github.lamba92.openaqua.engine.device.mock.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine.engineDevice)
        }
    }
}

engineBinaries {
    register("linuxX64") {
        binaryPrefix = "open-aqua-device-engine"
        fromExecutable(kotlin.linuxX64().binaries.getExecutable("release"))
    }
    register("linuxArm64") {
        binaryPrefix = "open-aqua-device-engine"
        fromExecutable(kotlin.linuxArm64().binaries.getExecutable("release"))
    }
    register("macosArm64") {
        binaryPrefix = "open-aqua-device-engine"
        fromExecutable(kotlin.macosArm64().binaries.getExecutable("release"))
    }
    register("mingwX64") {
        binaryPrefix = "open-aqua-device-engine"
        fromExecutable(kotlin.mingwX64().binaries.getExecutable("release"))
    }
}
