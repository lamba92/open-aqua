package io.github.lamba92.openaqua.app.permission

import kotlinx.coroutines.flow.StateFlow

interface DevicePermissionService {
    val state: StateFlow<DevicePermissionState>

    fun requestPermissions()

    fun removePermissions() {}
}

val LINUX_UDEV_RULE_INSTALL_SCRIPT =
    $$"""mkdir -p /etc/udev/rules.d && cat > /etc/udev/rules.d/60-open-aqua.rules << 'EOF'
KERNEL=="hidraw*", ATTRS{idVendor}=="0c70", MODE="0666", TAG+="uaccess"
SUBSYSTEM=="usb", ATTRS{idVendor}=="0c70", MODE="0666", TAG+="uaccess"
EOF
rm -f /etc/udev/rules.d/70-open-aqua.rules
udevadm control --reload-rules
udevadm trigger --action=change --subsystem-match=hidraw || true
udevadm trigger --action=add --subsystem-match=hidraw || true
for dev in /sys/class/hidraw/hidraw*; do
    if [ -f "$dev/device/uevent" ] && grep -qi "0c70" "$dev/device/uevent"; then
        chmod 666 "/dev/$(basename "$dev")" 2>/dev/null || true
    fi
done"""

val LINUX_UDEV_RULE_UNINSTALL_SCRIPT =
    "rm -f /etc/udev/rules.d/60-open-aqua.rules /etc/udev/rules.d/70-open-aqua.rules && udevadm control --reload-rules && udevadm trigger --action=change --subsystem-match=hidraw || true"
