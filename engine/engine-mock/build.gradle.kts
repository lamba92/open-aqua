@file:OptIn(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi::class)

import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion

plugins {
    openaqua.engine
    id("openaqua.engine.producer")
}

kotlin {
    linuxX64 {
        binaries {
            executable {
                baseName = "open-aqua-engine-mock"
                entryPoint = "io.github.lamba92.openaqua.engine.mock.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
            }
        }
    }
    linuxArm64 {
        binaries {
            executable {
                baseName = "open-aqua-engine-mock"
                entryPoint = "io.github.lamba92.openaqua.engine.mock.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
            }
        }
    }
    macosArm64 {
        binaries {
            executable {
                baseName = "open-aqua-engine-mock"
                entryPoint = "io.github.lamba92.openaqua.engine.mock.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
            }
        }
    }
    mingwX64 {
        binaries {
            executable {
                baseName = "open-aqua-engine-mock"
                entryPoint = "io.github.lamba92.openaqua.engine.mock.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine)
        }
    }
}

engineBinaries {
    register("linuxX64") {
        fromExecutable(kotlin.linuxX64().binaries.getExecutable("release"))
    }
    register("linuxArm64") {
        fromExecutable(kotlin.linuxArm64().binaries.getExecutable("release"))
    }
    register("macosArm64") {
        fromExecutable(kotlin.macosArm64().binaries.getExecutable("release"))
    }
    register("mingwX64") {
        fromExecutable(kotlin.mingwX64().binaries.getExecutable("release"))
    }
}
