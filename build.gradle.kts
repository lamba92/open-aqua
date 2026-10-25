// Root build script - plugins and build logic are provided via buildSrc

tasks.register("generateBrandAssets") {
    group = "branding"
    description = "Generates all OpenAqua SVG, PNG, ICO, and ICNS brand assets and syncs them into the repository."
    dependsOn(":brand-assets:generateBrandAssets")
}
