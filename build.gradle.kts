// Root build script - plugins and build logic are provided via buildSrc

tasks.register("generateBrandAssets") {
    group = "branding"
    description = "Generates all OpenAqua SVG, PNG, ICO, and ICNS brand assets and syncs them into the repository."
    dependsOn(":assets:generateBrandAssets")
}

tasks.register("generatePackagingFiles") {
    group = "distribution"
    description = "Generates all Linux store packaging files (Flatpak, Snap, AUR, AppStream XML, desktop entries, udev rules) from AppMetadata."
    dependsOn(":packaging:packaging-linux-x64:generatePackagingFiles")
}

tasks.register("release") {
    group = "distribution"
    description = "Publishes all release artifacts to root build/releases for the current OS"
    dependsOn(
        provider {
            val os = org.gradle.internal.os.OperatingSystem.current()
            when {
                os.isLinux -> listOf(":packaging:packaging-linux-x64:release")
                os.isWindows -> listOf(":packaging:packaging-windows-x64:release")
                else -> emptyList()
            }
        },
        ":engine:engine-device:engine-device-prod:release",
    )
}

