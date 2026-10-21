@file:OptIn(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi::class)

import io.github.lamba92.gradle.plugins.upx.UpxTask
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

plugins {
    openaqua.engine
    id("io.github.lamba92.upx")
    id("openaqua.engine.producer")
}

upx {
    bruteLevel = UpxTask.BruteLevel.ULTRA_BRUTE
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
                entryPoint = "io.github.lamba92.openaqua.engine.prod.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
            }
            getTest("debug").disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
        }
    }
    linuxArm64 {
        binaries {
            executable {
                baseName = "open-aqua-engine"
                entryPoint = "io.github.lamba92.openaqua.engine.prod.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
            }
            getTest("debug").disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
        }
    }
    mingwX64 {
        compilations.getByName("main") {
            cinterops {
                create("winhid") {
                    defFile(file("src/mingwMain/cinterop/winhid.def"))
                }
            }
        }
        binaries {
            executable {
                baseName = "open-aqua-engine"
                entryPoint = "io.github.lamba92.openaqua.engine.prod.main"
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

engineBinaries {
    register("linuxX64") {
        fromUpx(
            kotlin.linuxX64().binaries.getExecutable("release"),
            tasks.named<UpxTask>("compressReleaseExecutableLinuxX64"),
        )
    }
    register("linuxArm64") {
        fromUpx(
            kotlin.linuxArm64().binaries.getExecutable("release"),
            tasks.named<UpxTask>("compressReleaseExecutableLinuxArm64"),
        )
    }
    register("mingwX64") {
        fromUpx(
            kotlin.mingwX64().binaries.getExecutable("release"),
            tasks.named<UpxTask>("compressReleaseExecutableMingwX64"),
        )
    }
}
