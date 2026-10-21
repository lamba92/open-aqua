import io.github.lamba92.gradle.plugins.engine.Arch
import io.github.lamba92.gradle.plugins.engine.Os

enum class SupportedTargets(val os: Os, val arch: Arch) {
    LinuxX64(Os.LINUX, Arch.X64),
    LinuxArm64(Os.LINUX, Arch.ARM64),
    WindowsX64(Os.WINDOWS, Arch.X64),
    MacOSX64(Os.MACOS, Arch.X64),
    MacOSArm64(Os.MACOS, Arch.ARM64),
    ;

    companion object {
        fun getCurrent(): SupportedTargets {
            val os = Os.current()
            val arch = Arch.current()
            return values().firstOrNull { it.os == os && it.arch == arch }
                ?: error("Unsupported current target: OS=$os, Arch=$arch")
        }
    }
}
