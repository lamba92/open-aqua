// Root build script - plugins and build logic are provided via buildSrc

tasks.register("generateBrandAssets") {
    group = "branding"
    description = "Generates all OpenAqua SVG, PNG, ICO, and ICNS brand assets and syncs them into the repository."
    dependsOn(":assets:generateBrandAssets")
}

tasks.register("releaseLinux") {
    group = "distribution"
    description = "Packages and publishes all Linux release artifacts into build/releases"
    dependsOn(
        ":engine:engine-device:engine-device-prod:releaseLinux",
        ":app-compose:app-compose-prod:releaseLinux",
    )
}

tasks.register("releaseWindows") {
    group = "distribution"
    description = "Packages and publishes all Windows release artifacts into build/releases"
    dependsOn(
        ":engine:engine-device:engine-device-prod:releaseWindows",
        ":app-compose:app-compose-prod:releaseWindows",
    )
}
