plugins {
    openaqua.convention.jvm
    id(libs.plugins.kotlin.compose)
    id(libs.plugins.compose.multiplatform)
}

dependencies {
    implementation(projects.appCompose)
}

compose.desktop {
    application {
        mainClass = "io.github.lamba92.openaqua.screenshots.MainKt"
    }
}

tasks.register<JavaExec>("generateScreenshots") {
    group = "documentation"
    description = "Generates application screenshots using headless ImageComposeScene"
    mainClass.set("io.github.lamba92.openaqua.screenshots.MainKt")
    classpath = sourceSets["main"].runtimeClasspath
    val screenshotsDir = project.file("screenshots")
    outputs.dir(screenshotsDir)
    args(screenshotsDir.absolutePath)
}
