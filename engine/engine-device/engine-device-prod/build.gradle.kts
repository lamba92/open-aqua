@file:OptIn(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi::class)

import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

plugins {
    openaqua.convention.multiplatform
    openaqua.engine.producer
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
                baseName = "open-aqua-device-engine"
                entryPoint = "io.github.lamba92.openaqua.engine.device.prod.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
                linkerOpts("-Wl,--as-needed")
            }
            getTest("debug").disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
        }
    }
    linuxArm64 {
        binaries {
            executable {
                baseName = "open-aqua-device-engine"
                entryPoint = "io.github.lamba92.openaqua.engine.device.prod.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
                linkerOpts("-Wl,--as-needed")
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
                baseName = "open-aqua-device-engine"
                entryPoint = "io.github.lamba92.openaqua.engine.device.prod.main"
                disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
            }
            getTest("debug").disableNativeCache(version = DisableCacheInKotlinVersion.`2_4_0`, reason = "Clikt duplicate symbol workaround")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine.engineDevice)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.core)
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
    register("mingwX64") {
        binaryPrefix = "open-aqua-device-engine"
        fromExecutable(kotlin.mingwX64().binaries.getExecutable("release"))
    }
}

val releaseVersion =
    project.version
        .toString()
        .removePrefix("v")
        .substringBefore("-")

tasks.register("releaseLinux") {
    group = "distribution"
    description = "Publishes Linux device engine binary to root build/releases"
    dependsOn("prepareEngineBinaryLinuxX64")
    val binaryFile = layout.buildDirectory.file("engine-binaries/linuxX64/open-aqua-device-engine-linux-x64")
    inputs.file(binaryFile)
    outputs.dir(rootProject.layout.buildDirectory.dir("releases"))

    doLast {
        publishReleaseArtifact(
            file = binaryFile.get().asFile,
            destinationName = "open-aqua-device-engine-$releaseVersion-linux-x64",
        )
    }
}

tasks.register("releaseWindows") {
    group = "distribution"
    description = "Publishes Windows device engine binary to root build/releases"
    dependsOn("prepareEngineBinaryMingwX64")
    val binaryFile = layout.buildDirectory.file("engine-binaries/mingwX64/open-aqua-device-engine-windows-x64.exe")
    inputs.file(binaryFile)
    outputs.dir(rootProject.layout.buildDirectory.dir("releases"))

    doLast {
        publishReleaseArtifact(
            file = binaryFile.get().asFile,
            destinationName = "open-aqua-device-engine-$releaseVersion-windows-x64.exe",
        )
    }
}

tasks.register("release") {
    group = "distribution"
    description = "Publishes device engine release binary to root build/releases for the current OS"
    val os =
        org.gradle.internal.os.OperatingSystem
            .current()
    when {
        os.isLinux -> dependsOn("releaseLinux")
        os.isWindows -> dependsOn("releaseWindows")
        else -> Unit
    }
}
