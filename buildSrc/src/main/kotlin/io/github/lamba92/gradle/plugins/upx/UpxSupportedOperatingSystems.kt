package io.github.lamba92.gradle.plugins.upx

import org.gradle.internal.os.OperatingSystem

@Suppress("EnumEntryName")
enum class UpxSupportedOperatingSystems(val fileSuffix: String, val extension: String) {
    WINDOWS_x86("win32", "zip"),
    WINDOWS_x64("win64", "zip"),
    LINUX_x64("amd64_linux", "tar.xz"),
    LINUX_ARM64("arm64_linux", "tar.xz"),
    LINUX_ARM("arm_linux", "tar.xz"),
    LINUX_x86("i386_linux", "tar.xz");

    companion object {
        fun current(): UpxSupportedOperatingSystems? {
            val os = OperatingSystem.current()
            val arch = System.getProperty("os.arch").lowercase()
            val is64 = "64" in arch
            val isArm = "arm" in arch || "aarch" in arch
            return when {
                os.isWindows -> if (is64) WINDOWS_x64 else WINDOWS_x86
                os.isLinux ->
                    when {
                        is64 && isArm -> LINUX_ARM64
                        !is64 && isArm -> LINUX_ARM
                        is64 -> LINUX_x64
                        else -> LINUX_x86
                    }
                else -> null
            }
        }

        fun buildUpxFileName(
            version: String,
            platform: UpxSupportedOperatingSystems,
        ): String = "upx-$version-${platform.fileSuffix}.${platform.extension}"

        fun buildUpxUrl(
            version: String,
            platform: UpxSupportedOperatingSystems,
        ): String = "https://github.com/upx/upx/releases/download/v$version/${buildUpxFileName(version, platform)}"
    }
}
