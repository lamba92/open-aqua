import io.github.lamba92.gradle.plugins.distribution.DISTRIBUTION_TYPE_ATTRIBUTE
import io.github.lamba92.gradle.plugins.distribution.DistributionType
import io.github.lamba92.gradle.plugins.engine.OS_ATTRIBUTE
import io.github.lamba92.gradle.plugins.engine.Os
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.named

val appDistributableElements =
    configurations.create("appDistributableElements") {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("app-distribution"))
            attribute(OS_ATTRIBUTE, Os.LINUX)
            attribute(DISTRIBUTION_TYPE_ATTRIBUTE, DistributionType.APP_DIR)
        }
    }

val linuxDistributionElements =
    configurations.create("linuxDistributionElements") {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("app-distribution"))
            attribute(OS_ATTRIBUTE, Os.LINUX)
            attribute(DISTRIBUTION_TYPE_ATTRIBUTE, DistributionType.PACKAGES)
        }
    }

val windowsDistributionElements =
    configurations.create("windowsDistributionElements") {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("app-distribution"))
            attribute(OS_ATTRIBUTE, Os.WINDOWS)
            attribute(DISTRIBUTION_TYPE_ATTRIBUTE, DistributionType.PACKAGES)
        }
    }

plugins.withId("org.jetbrains.compose") {
    afterEvaluate {
        val createDistributableTask = tasks.findByName("createReleaseDistributable")
        if (createDistributableTask != null) {
            val releasePackageVersion =
                project.version
                    .toString()
                    .removePrefix("v")
                    .substringBefore("-")

            val appDistributableDir = layout.buildDirectory.dir("compose/binaries/main-release/app/open-aqua")

            val prepareAppDistributable =
                tasks.register<Sync>("prepareAppDistributable") {
                    group = "distribution"
                    description = "Syncs unpacked release application directory for distribution consumption"
                    dependsOn(createDistributableTask)
                    into(layout.buildDirectory.dir("distributions/app-distributable"))
                    from(appDistributableDir)
                }
            appDistributableElements.outgoing.artifact(prepareAppDistributable)

            val packageReleaseTarGz =
                tasks.register<Tar>("packageReleaseTarGz") {
                    group = "distribution"
                    description = "Packages the release standalone distributable into a tar.gz archive"
                    dependsOn(createDistributableTask)
                    archiveBaseName = "open-aqua"
                    archiveVersion = releasePackageVersion
                    archiveClassifier = "linux-x64"
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
                    dependsOn(createDistributableTask)
                    archiveBaseName = "open-aqua"
                    archiveVersion = releasePackageVersion
                    archiveClassifier = "windows-x64"
                    destinationDirectory = layout.buildDirectory.dir("compose/binaries/main-release/zip")
                    into("open-aqua") {
                        from(appDistributableDir)
                    }
                }

            val debTask = tasks.findByName("packageReleaseDeb")
            val rpmTask = tasks.findByName("packageReleaseRpm")
            val msiTask = tasks.findByName("packageReleaseMsi")

            val prepareLinuxDistributions =
                tasks.register<Sync>("prepareLinuxDistributions") {
                    group = "distribution"
                    description = "Collects all built Linux distribution packages into distributions/linux"
                    dependsOn(packageReleaseTarGz)
                    into(layout.buildDirectory.dir("distributions/linux"))
                    from(packageReleaseTarGz.flatMap { it.archiveFile })

                    if (debTask != null) {
                        dependsOn(debTask)
                        from(layout.buildDirectory.dir("compose/binaries/main-release/deb")) { include("*.deb") }
                    }
                    if (rpmTask != null) {
                        dependsOn(rpmTask)
                        from(layout.buildDirectory.dir("compose/binaries/main-release/rpm")) { include("*.rpm") }
                    }
                }
            linuxDistributionElements.outgoing.artifact(prepareLinuxDistributions)

            val prepareWindowsDistributions =
                tasks.register<Sync>("prepareWindowsDistributions") {
                    group = "distribution"
                    description = "Collects all built Windows distribution packages into distributions/windows"
                    dependsOn(packageReleaseZip)
                    into(layout.buildDirectory.dir("distributions/windows"))
                    from(packageReleaseZip.flatMap { it.archiveFile })

                    if (msiTask != null) {
                        dependsOn(msiTask)
                        from(layout.buildDirectory.dir("compose/binaries/main-release/msi")) { include("*.msi") }
                    }
                }
            windowsDistributionElements.outgoing.artifact(prepareWindowsDistributions)
        }
    }
}
