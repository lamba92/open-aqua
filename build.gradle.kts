// Root build script - plugins and build logic are provided via buildSrc

tasks.register("generateBrandAssets") {
    group = "branding"
    description = "Generates all OpenAqua SVG, PNG, ICO, and ICNS brand assets and syncs them into the repository."
    dependsOn(":assets:generateBrandAssets")
}

tasks.register("generatePackagingFiles") {
    group = "distribution"
    description = "Generates all Linux store packaging files (Flatpak, Snap, AUR, AppStream XML, desktop entries, udev rules) from AppMetadata."
    dependsOn(":packaging:generatePackagingFiles")
}

tasks.register("release") {
    group = "distribution"
    description = "Publishes all release artifacts to root build/releases for the current OS"
    dependsOn(":packaging:release", ":engine:engine-device:engine-device-prod:release")
}
