package io.github.lamba92.gradle.plugins.engine

import org.jetbrains.kotlin.konan.target.KonanTarget

fun KonanTarget.toCanonicalBinaryName(): String {
    val (os, arch) = toOsAndArch()
    return canonicalBinaryName(os, arch)
}
