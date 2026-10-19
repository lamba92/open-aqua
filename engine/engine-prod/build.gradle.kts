@file:OptIn(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi::class)

import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

plugins {
    openaqua.engine
}

val enableHardwareTests =
    (project.findProperty("test.hardware") as? String)?.toBoolean()
        ?: System.getenv("TEST_HARDWARE")?.toBoolean()
        ?: false

tasks.withType<KotlinTest>().configureEach {
    if (!enableHardwareTests) {
        (excludePatterns as MutableSet<String>).add("*Hardware*")
    }
}

kotlin {
    linuxX64 {
        binaries {
            executable {
                baseName = "open-aqua-engine"
                entryPoint = "com.openaqua.engine.prod.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
            }
            getTest("debug").disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
        }
    }
    linuxArm64 {
        binaries {
            executable {
                baseName = "open-aqua-engine"
                entryPoint = "com.openaqua.engine.prod.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
            }
            getTest("debug").disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
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
            getTest("debug").disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
