package com.openaqua.engine.prod

import com.openaqua.engine.hardware.HardwareController

actual fun getPlatformHardwareController(): HardwareController = LinuxHardwareController()
