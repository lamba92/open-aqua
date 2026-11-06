// Root build script - plugins and build logic are provided via buildSrc
import java.security.MessageDigest

tasks.register("generateBrandAssets") {
    group = "branding"
    description = "Generates all OpenAqua SVG, PNG, ICO, and ICNS brand assets and syncs them into the repository."
    dependsOn(":assets:generateBrandAssets")
}

fun File.sha256Hex(): String {
    val md = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(8192)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            md.update(buffer, 0, read)
        }
    }
    return md.digest().joinToString("") { "%02x".format(it) }
}

tasks.register("releaseLinux") {
    group = "distribution"
    description = "Prepares all Linux release artifacts and checksums in build/releases"
    dependsOn(
        ":engine:engine-device:engine-device-prod:linkReleaseExecutableLinuxX64",
        ":app-compose:app-compose-prod:packageReleaseAppImage",
        ":app-compose:app-compose-prod:packageReleaseDeb",
        ":app-compose:app-compose-prod:packageReleaseRpm",
        ":app-compose:app-compose-prod:packageReleaseTarGz",
    )

    val releasesDir = layout.buildDirectory.dir("releases")
    outputs.dir(releasesDir)

    doLast {
        val out = releasesDir.get().asFile
        out.deleteRecursively()
        out.mkdirs()

        // 1. Engine binary
        val engineKexe =
            project(":engine:engine-device:engine-device-prod").layout.buildDirectory
                .file("bin/linuxX64/releaseExecutable/open-aqua-device-engine.kexe").get().asFile
        if (engineKexe.exists()) {
            engineKexe.copyTo(File(out, "open-aqua-device-engine-linux-x64"), overwrite = true)
        }

        // 2. Compose packages
        val composeBinDir =
            project(":app-compose:app-compose-prod").layout.buildDirectory
                .dir("compose/binaries/main-release").get().asFile

        val allowedExtensions = listOf("AppImage", "deb", "rpm", "tar.gz")
        composeBinDir.walk().forEach { file ->
            if (file.isFile && allowedExtensions.any { ext -> file.name.endsWith(".$ext") }) {
                file.copyTo(File(out, file.name), overwrite = true)
            }
        }

        // 3. Generate SHA-256 checksums
        out.listFiles()?.filter { it.isFile && !it.name.endsWith(".sha256") }?.forEach { file ->
            val hash = file.sha256Hex()
            File(out, "${file.name}.sha256").writeText("$hash  ${file.name}\n")
        }

        logger.lifecycle("Linux release assets prepared at: ${out.absolutePath}")
        out.listFiles()?.forEach { logger.lifecycle("  - ${it.name} (${it.length() / 1024} KB)") }
    }
}

tasks.register("releaseWindows") {
    group = "distribution"
    description = "Prepares all Windows release artifacts and checksums in build/releases"
    dependsOn(
        ":engine:engine-device:engine-device-prod:linkReleaseExecutableMingwX64",
        ":app-compose:app-compose-prod:packageReleaseMsi",
        ":app-compose:app-compose-prod:packageReleaseZip",
    )

    val releasesDir = layout.buildDirectory.dir("releases")
    outputs.dir(releasesDir)

    doLast {
        val out = releasesDir.get().asFile
        out.deleteRecursively()
        out.mkdirs()

        // 1. Engine binary
        val engineExe =
            project(":engine:engine-device:engine-device-prod").layout.buildDirectory
                .file("bin/mingwX64/releaseExecutable/open-aqua-device-engine.exe").get().asFile
        if (engineExe.exists()) {
            engineExe.copyTo(File(out, "open-aqua-device-engine-windows-x64.exe"), overwrite = true)
        }

        // 2. Compose packages
        val composeBinDir =
            project(":app-compose:app-compose-prod").layout.buildDirectory
                .dir("compose/binaries/main-release").get().asFile

        val allowedExtensions = listOf("msi", "zip")
        composeBinDir.walk().forEach { file ->
            if (file.isFile && allowedExtensions.any { ext -> file.name.endsWith(".$ext") }) {
                file.copyTo(File(out, file.name), overwrite = true)
            }
        }

        // 3. Generate SHA-256 checksums
        out.listFiles()?.filter { it.isFile && !it.name.endsWith(".sha256") }?.forEach { file ->
            val hash = file.sha256Hex()
            File(out, "${file.name}.sha256").writeText("$hash  ${file.name}\n")
        }

        logger.lifecycle("Windows release assets prepared at: ${out.absolutePath}")
        out.listFiles()?.forEach { logger.lifecycle("  - ${it.name} (${it.length() / 1024} KB)") }
    }
}
