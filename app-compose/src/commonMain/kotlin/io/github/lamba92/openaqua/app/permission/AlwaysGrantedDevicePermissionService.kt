package io.github.lamba92.openaqua.app.permission

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AlwaysGrantedDevicePermissionService : DevicePermissionService {
    override val state: StateFlow<DevicePermissionState> =
        MutableStateFlow(DevicePermissionState.Granted)

    override fun requestPermissions() {}
}
