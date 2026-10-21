package io.github.lamba92.gradle.plugins.engine

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget

enum class Os(private val id: String) : Named {
    LINUX("linux"),
    MACOS("macos"),
    WINDOWS("windows"),
    ;

    override fun getName(): String = id

    val executableExtension: String
        get() = if (this == WINDOWS) ".exe" else ""

    companion object {
        fun current(): Os {
            val os = System.getProperty("os.name")
            return when {
                os.startsWith("Linux", ignoreCase = true) -> LINUX
                os.startsWith("Mac", ignoreCase = true) -> MACOS
                os.startsWith("Win", ignoreCase = true) -> WINDOWS
                else -> error("Unsupported OS: $os")
            }
        }
    }
}

enum class Arch(private val id: String) : Named {
    X64("x64"),
    ARM64("arm64"),
    ;

    override fun getName(): String = id

    companion object {
        fun current(): Arch {
            val arch = System.getProperty("os.arch")
            return when (arch) {
                "x86_64", "amd64", "x64" -> X64
                "aarch64", "arm64", "arm" -> ARM64
                else -> error("Unsupported Arch: $arch")
            }
        }
    }
}

val OS_ATTRIBUTE: Attribute<Os> = Attribute.of("io.github.lamba92.openaqua.os", Os::class.java)
val ARCH_ATTRIBUTE: Attribute<Arch> = Attribute.of("io.github.lamba92.openaqua.arch", Arch::class.java)

fun canonicalBinaryName(
    os: Os,
    arch: Arch,
): String = "open-aqua-engine-${os.name.lowercase()}-${arch.name.lowercase()}${os.executableExtension}"

fun KonanTarget.toOsAndArch(): Pair<Os, Arch> {
    val os =
        when (family) {
            Family.LINUX -> Os.LINUX
            Family.OSX -> Os.MACOS
            Family.MINGW -> Os.WINDOWS
            else -> error("Unsupported KonanTarget family: $family for target $this")
        }
    val arch =
        when (this) {
            KonanTarget.LINUX_X64, KonanTarget.MACOS_X64, KonanTarget.MINGW_X64 -> Arch.X64
            KonanTarget.LINUX_ARM64, KonanTarget.MACOS_ARM64 -> Arch.ARM64
            else -> error("Unsupported KonanTarget architecture for target $this")
        }
    return os to arch
}
