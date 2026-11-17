package io.github.lamba92.openaqua.app.platform

actual object PlatformInfo {
    actual val jvmVersion: String
        get() = System.getProperty("java.version") ?: "unknown"

    actual val jvmVendor: String
        get() = System.getProperty("java.vendor") ?: "unknown"

    actual val osName: String
        get() = System.getProperty("os.name") ?: "unknown"

    actual val osArch: String
        get() = System.getProperty("os.arch") ?: "unknown"
}
