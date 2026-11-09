import io.github.lamba92.gradle.plugins.distribution.DISTRIBUTION_TYPE_ATTRIBUTE
import io.github.lamba92.gradle.plugins.distribution.DistributionType
import io.github.lamba92.gradle.plugins.distribution.NativeDistributionSpec
import io.github.lamba92.gradle.plugins.distribution.PACKAGE_FORMAT_ATTRIBUTE
import io.github.lamba92.gradle.plugins.distribution.PackageFormat
import io.github.lamba92.gradle.plugins.platform.PlatformExtension
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.named

plugins {
    id("openaqua.platform")
}

val platform = extensions.getByType<PlatformExtension>()
val targetOs = platform.os
val targetArch = platform.arch
val archClassifier = platform.classifier
val releasePackageVersion =
    provider {
        project.version
            .toString()
            .removePrefix("v")
            .substringBefore("-")
    }

val nativeDistributions = container<NativeDistributionSpec>()
extensions.add("nativeDistributions", nativeDistributions)

val appDistributableElements =
    configurations.create("appDistributableElements") {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("app-distribution"))
            attribute(DISTRIBUTION_TYPE_ATTRIBUTE, DistributionType.APP_DIR)
            attribute(PACKAGE_FORMAT_ATTRIBUTE, PackageFormat.APP_DIR)
        }
    }

val distributionPackagesElements =
    configurations.create("distributionPackagesElements") {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("app-distribution"))
            attribute(DISTRIBUTION_TYPE_ATTRIBUTE, DistributionType.PACKAGES)
        }
    }

val appDistributableDir = layout.buildDirectory.dir("compose/binaries/main-release/app/open-aqua")

val prepareAppDistributable =
    tasks.register<Sync>("prepareAppDistributable") {
        group = "distribution"
        description = "Syncs unpacked release application directory for distribution consumption"
        into(layout.buildDirectory.dir("distributions/app-distributable"))
        from(appDistributableDir)
    }
appDistributableElements.outgoing.artifact(prepareAppDistributable)

val packageReleaseTarGz =
    tasks.register<Tar>("packageReleaseTarGz") {
        group = "distribution"
        description = "Packages the release standalone distributable into a tar.gz archive"
        archiveBaseName = "open-aqua"
        archiveVersion = releasePackageVersion
        archiveClassifier = archClassifier
        archiveExtension = "tar.gz"
        compression = Compression.GZIP
        destinationDirectory = layout.buildDirectory.dir("compose/binaries/main-release/tar")
        into("open-aqua") {
            from(appDistributableDir)
        }
    }

val packageReleaseZip =
    tasks.register<Zip>("packageReleaseZip") {
        group = "distribution"
        description = "Packages the release standalone distributable into a zip archive"
        archiveBaseName = "open-aqua"
        archiveVersion = releasePackageVersion
        archiveClassifier = archClassifier
        destinationDirectory = layout.buildDirectory.dir("compose/binaries/main-release/zip")
        into("open-aqua") {
            from(appDistributableDir)
        }
    }

val prepareLinuxDistributions =
    tasks.register<Sync>("prepareLinuxDistributions") {
        group = "distribution"
        description = "Collects all built Linux distribution packages into distributions/linux"
        dependsOn(packageReleaseTarGz)
        into(layout.buildDirectory.dir("distributions/linux"))
        from(packageReleaseTarGz.flatMap { it.archiveFile })
    }

val prepareWindowsDistributions =
    tasks.register<Sync>("prepareWindowsDistributions") {
        group = "distribution"
        description = "Collects all built Windows distribution packages into distributions/windows"
        dependsOn(packageReleaseZip)
        into(layout.buildDirectory.dir("distributions/windows"))
        from(packageReleaseZip.flatMap { it.archiveFile })
    }

nativeDistributions.all {
    val spec = this
    when (spec.format.orNull ?: if (spec.name == "appDir") PackageFormat.APP_DIR else null) {
        PackageFormat.APP_DIR -> {
            prepareAppDistributable.configure { dependsOn(spec.files) }
            packageReleaseTarGz.configure { dependsOn(spec.files) }
            packageReleaseZip.configure { dependsOn(spec.files) }
        }

        PackageFormat.DEB -> {
            prepareLinuxDistributions.configure {
                dependsOn(spec.files)
                from(layout.buildDirectory.dir("compose/binaries/main-release/deb")) { include("*.deb") }
            }
            if (distributionPackagesElements.outgoing.artifacts.isEmpty()) {
                distributionPackagesElements.outgoing.artifact(prepareLinuxDistributions)
            }
        }

        PackageFormat.RPM -> {
            prepareLinuxDistributions.configure {
                dependsOn(spec.files)
                from(layout.buildDirectory.dir("compose/binaries/main-release/rpm")) { include("*.rpm") }
            }
            if (distributionPackagesElements.outgoing.artifacts.isEmpty()) {
                distributionPackagesElements.outgoing.artifact(prepareLinuxDistributions)
            }
        }

        PackageFormat.MSI -> {
            prepareWindowsDistributions.configure {
                dependsOn(spec.files)
                from(layout.buildDirectory.dir("compose/binaries/main-release/msi")) { include("*.msi") }
            }
            if (distributionPackagesElements.outgoing.artifacts.isEmpty()) {
                distributionPackagesElements.outgoing.artifact(prepareWindowsDistributions)
            }
        }

        else -> {
            prepareLinuxDistributions.configure {
                dependsOn(spec.files)
                from(spec.files)
            }
            prepareWindowsDistributions.configure {
                dependsOn(spec.files)
                from(spec.files)
            }
        }
    }
}
