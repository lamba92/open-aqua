plugins {
    openaqua.convention.native.app
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.appCompose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.desktop {
    native {
        entryPoint = "io.github.lamba92.openaqua.screenshots.main"
    }
}

val generateScreenshots =
    tasks.register<Exec>("generateScreenshots") {
        group = "documentation"
        description = "Generates application screenshots using native Compose SDL3 window capture"
        val linkTask = tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink>("linkDebugExecutableLinuxX64")
        dependsOn(linkTask)
        val executable = linkTask.flatMap { it.outputFile }
        val screenshotsDir = rootProject.file("docs/screenshots")
        outputs.dir(screenshotsDir)
        val xvfb = file("/usr/bin/xvfb-run")
        if (xvfb.exists()) {
            executable(xvfb.absolutePath)
            argumentProviders.add(
                CommandLineArgumentProvider {
                    listOf("-a", "-s", "-screen 0 3840x2160x24", executable.get().absolutePath, screenshotsDir.absolutePath)
                },
            )
        } else {
            executable(executable.get().absolutePath)
            args(screenshotsDir.absolutePath)
        }
    }
