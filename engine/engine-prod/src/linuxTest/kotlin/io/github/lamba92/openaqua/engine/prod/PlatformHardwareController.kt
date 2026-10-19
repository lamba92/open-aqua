package io.github.lamba92.openaqua.engine.prod

import io.github.lamba92.openaqua.engine.hardware.HardwareController

actual fun getPlatformHardwareController(): HardwareController = LinuxHardwareController()
