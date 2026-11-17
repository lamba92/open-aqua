package io.github.lamba92.openaqua.app.platform

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual object PlatformInfo {
    actual val jvmVersion: String
        get() = "Native"

    actual val jvmVendor: String
        get() = "Kotlin/Native"

    actual val osName: String
        get() = kotlin.native.Platform.osFamily.name

    actual val osArch: String
        get() = kotlin.native.Platform.cpuArchitecture.name
}
