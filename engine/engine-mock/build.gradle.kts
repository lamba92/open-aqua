@file:OptIn(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi::class)

import io.github.lamba92.gradle.plugins.upx.UpxTask
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion

plugins {
    openaqua.engine
    id("io.github.lamba92.upx")
}

upx {
    bruteLevel = UpxTask.BruteLevel.ULTRA_BRUTE
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
