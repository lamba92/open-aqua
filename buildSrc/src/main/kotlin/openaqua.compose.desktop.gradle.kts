import io.github.lamba92.gradle.plugins.distribution.NativeDistributionSpec
import io.github.lamba92.gradle.plugins.platform.PlatformExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("openaqua.convention.jvm")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    id("openaqua.platform")
    id("openaqua.engine.consumer")
    id("openaqua.distribution.producer")
}

val platform = extensions.getByType<PlatformExtension>()
val nativeDistributions = extensions.getByType<NamedDomainObjectContainer<NativeDistributionSpec>>()

nativeDistributions.all {
    os.convention(platform.os)
    arch.convention(platform.arch)
}

val releasePackageVersion =
    project.version
        .toString()
        .removePrefix("v")
        .substringBefore("-")

compose.desktop {
    application {
        mainClass = "io.github.lamba92.openaqua.app.MainKt"

        buildTypes.release.proguard {
            configurationFiles.from(rootProject.file("app-compose/proguard-rules.pro"))
        }

        nativeDistributions {
            packageName = "open-aqua"
            packageVersion = releasePackageVersion

            linux {
                iconFile = rootProject.file("assets/generated/icon-512.png")
                rpmPackageVersion = releasePackageVersion
            }
            windows {
                iconFile = rootProject.file("assets/generated/favicon.ico")
                msiPackageVersion = releasePackageVersion
                perUserInstall = false
            }
            macOS {
                iconFile = rootProject.file("assets/generated/icon.icns")
            }
        }
    }
}
