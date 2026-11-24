@file:OptIn(ExperimentalNativeApi::class)

package io.github.lamba92.openaqua.app.platform

import kotlin.experimental.ExperimentalNativeApi

object PlatformInfo {
    val osName: String
        get() = Platform.osFamily.name

    val osArch: String
        get() = Platform.cpuArchitecture.name
}
