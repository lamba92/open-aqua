plugins {
    id(libs.plugins.kotlin.jvm)
    id(libs.plugins.ktlint)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("io.github.lamba92.openaqua.brand.BrandAssetGeneratorKt")
}

ktlint {
    filter {
        exclude("**/build/**")
        exclude("**/generated/**")
    }
}

dependencies {
    implementation(projects.coreTheme)
}

val generateBrandAssets by tasks.registering(JavaExec::class) {
    group = "branding"
    description = "Generates all OpenAqua SVG, PNG, ICO, and ICNS brand assets and syncs them into the repository."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(application.mainClass)
    args(
        rootProject.file(".github/assets").absolutePath,
        rootProject.file("app-compose/src/main/composeResources").absolutePath,
    )
    jvmArgs("-Djava.awt.headless=true")
}
