enum class SupportedTargets {
    LinuxX64,
    LinuxArm64,
    WindowsX64,
    MacOSX64,
    MacOSArm64;

    companion object {
        fun getCurrent(): SupportedTargets {
            val arch = when (val archString = System.getProperty("os.arch")) {
                "x86_64", "amd64" -> "x64"
                "aarch64" -> "arm"
                else -> error("Unsupported OS arch: $archString")
            }
            val os = System.getProperty("os.name")
            return when {
                os.equals("Mac OS X", ignoreCase = true) && arch == "x64" -> MacOSX64
                os.equals("Mac OS X", ignoreCase = true) && arch == "arm" -> MacOSArm64
                os.startsWith("Win", ignoreCase = true) && arch == "x64" -> WindowsX64
                os.startsWith("Linux", ignoreCase = true) && arch == "x64" -> LinuxX64
                os.startsWith("Linux", ignoreCase = true) && arch == "arm" -> LinuxArm64
                else -> error("Unsupported Compose Desktop target: $os $arch")
            }
        }

    }
}