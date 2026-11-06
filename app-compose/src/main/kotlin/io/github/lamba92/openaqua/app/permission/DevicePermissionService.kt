package io.github.lamba92.openaqua.app.permission

import kotlinx.coroutines.flow.StateFlow

interface DevicePermissionService {
    val state: StateFlow<DevicePermissionState>

    fun requestPermissions()

    fun removePermissions() {}
}
