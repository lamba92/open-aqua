plugins {
    openaqua.convention.jvm
    id(libs.plugins.kotlin.serialization)
    application
    openaqua.distribution.consumer
}

application {
    mainClass = "io.github.lamba92.openaqua.packaging.PackagingGeneratorKt"
}

dependencies {
    implementation(libs.kaml)
    implementation(libs.xmlutil.serialization)
    implementation(libs.kotlinx.serialization.json)

    appDistributable(projects.appCompose.appComposeProd)
    linuxDistributionPackages(projects.appCompose.appComposeProd)
    windowsDistributionPackages(projects.appCompose.appComposeProd)
}

val releasePackageVersion =
    project.version
        .toString()
        .removePrefix("v")
        .substringBefore("-")

val generatePackagingFiles by tasks.registering(JavaExec::class) {
    group = "distribution"
    description =
        "Generates all Linux & Windows store packaging files (Flatpak, Snap, AUR, MSIX, AppStream XML, desktop entries, udev rules) " +
        "from metadata.json."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    val metadataFile = project.file("metadata.json")
    val outputDir = layout.buildDirectory.dir("generated/packaging")
    inputs.file(metadataFile)
    outputs.dir(outputDir)
    environment("OPENAQUA_METADATA_FILE", metadataFile.absolutePath)
    environment("OPENAQUA_OUTPUT_DIR", outputDir.get().asFile.absolutePath)
    environment("OPENAQUA_VERSION", releasePackageVersion)
    environment("OPENAQUA_ROOT_DIR", rootProject.projectDir.absolutePath)
}
