package io.github.lamba92.gradle.plugins.engine

import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget

val TARGET_PLATFORM_ATTRIBUTE: Attribute<String> =
    Attribute.of("io.github.lamba92.openaqua.targetPlatform", String::class.java)

fun KonanTarget.toCanonicalBinaryName(): String {
    val isWindows = family == Family.MINGW
    val ext = if (isWindows) ".exe" else ""
    val targetSuffix = when (this) {
        KonanTarget.LINUX_X64 -> "linux-x64"
        KonanTarget.LINUX_ARM64 -> "linux-arm64"
        KonanTarget.MINGW_X64 -> "windows-x64"
        KonanTarget.MACOS_ARM64 -> "macos-arm64"
        KonanTarget.MACOS_X64 -> "macos-x64"
        else -> name
    }
    return "open-aqua-engine-$targetSuffix$ext"
}
