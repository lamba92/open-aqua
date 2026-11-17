package io.github.lamba92.openaqua.app.permission

sealed interface DevicePermissionState {
    data object Initializing : DevicePermissionState

    data object Granted : DevicePermissionState

    sealed interface Denied : DevicePermissionState {
        data object LinuxUdevRulesMissing : Denied

        data object WindowsElevationRequired : Denied
    }
}
