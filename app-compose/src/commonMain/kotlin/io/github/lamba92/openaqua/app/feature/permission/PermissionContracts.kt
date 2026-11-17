package io.github.lamba92.openaqua.app.feature.permission

import io.github.lamba92.openaqua.app.permission.DevicePermissionState

data class PermissionState(
    val permissionState: DevicePermissionState = DevicePermissionState.Initializing,
    val isRequesting: Boolean = false,
)

sealed interface PermissionEvent {
    data object RequestPermissions : PermissionEvent
}

sealed interface PermissionEffect
