package io.github.lamba92.openaqua.app.platform

expect object PlatformInfo {
    val jvmVersion: String
    val jvmVendor: String
    val osName: String
    val osArch: String
}
