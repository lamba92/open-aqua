@file:OptIn(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi::class)

import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion

plugins {
    openaqua.engine
}

kotlin {
    linuxX64 {
        binaries {
            executable {
                baseName = "open-aqua-engine"
                entryPoint = "com.openaqua.engine.prod.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
            }
        }
    }
    linuxArm64 {
        binaries {
            executable {
                baseName = "open-aqua-engine"
                entryPoint = "com.openaqua.engine.prod.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
            }
        }
    }
    mingwX64 {
        compilations.getByName("main") {
            cinterops {
                val winhid by creating {
                    defFile(file("src/mingwMain/cinterop/winhid.def"))
                }
            }
        }
        binaries {
            executable {
                baseName = "open-aqua-engine"
                entryPoint = "com.openaqua.engine.prod.main"
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
