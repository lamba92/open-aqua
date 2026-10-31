package io.github.lamba92.openaqua.engine.device.prod

import io.github.lamba92.openaqua.engine.device.hardware.HardwareController

actual fun getPlatformHardwareController(): HardwareController = WindowsHardwareController
