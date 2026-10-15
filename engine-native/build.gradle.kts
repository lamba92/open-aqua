plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

kotlin {
    linuxX64 {
        binaries {
            executable {
                baseName = "open-aqua-engine"
                entryPoint = "com.openaqua.engine.main"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.coreModel)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.io.core)
            implementation(libs.clikt)
        }
    }
}

tasks.register("optimizeEngineExecutable") {
    group = "build"
    description = "Strips and UPX-compresses the native engine executable"
    val linkRelease = tasks.named("linkReleaseExecutableLinuxX64")
    dependsOn(linkRelease)

    val inputExe = layout.buildDirectory.file("bin/linuxX64/releaseExecutable/open-aqua-engine.kexe")
    val outputDir = layout.buildDirectory.dir("optimizedBin")
    val outputExe = outputDir.map { it.file("open-aqua-engine") }

    inputs.file(inputExe)
    outputs.file(outputExe)

    doLast {
        val src = inputExe.get().asFile
        val destDir = outputDir.get().asFile
        destDir.mkdirs()
        val dest = outputExe.get().asFile
        src.copyTo(dest, overwrite = true)
        dest.setExecutable(true)

        val stripCode = ProcessBuilder("strip", dest.absolutePath).inheritIO().start().waitFor()
        if (stripCode != 0) error("strip failed with code $stripCode")

        try {
            val upxCode = ProcessBuilder("upx", "-q", "-9", dest.absolutePath).inheritIO().start().waitFor()
            if (upxCode == 0) {
                println("[UPX] Compressed ${dest.name} to ${dest.length()} bytes")
            }
        } catch (_: Exception) {
            println("[UPX] Not available, skipping UPX compression")
        }
    }
}
