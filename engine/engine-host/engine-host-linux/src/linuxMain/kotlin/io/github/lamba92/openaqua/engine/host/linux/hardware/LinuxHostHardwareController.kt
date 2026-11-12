@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.lamba92.openaqua.engine.host.linux.hardware

import io.github.lamba92.openaqua.engine.host.hardware.HostHardwareController
import io.github.lamba92.openaqua.engine.util.exists
import io.github.lamba92.openaqua.engine.util.listDirectory
import io.github.lamba92.openaqua.engine.util.readTextOrNull
import io.github.lamba92.openaqua.engine.util.writeText
import io.github.lamba92.openaqua.model.every
import io.github.lamba92.openaqua.model.host.HostDevice
import io.github.lamba92.openaqua.model.host.HostDeviceId
import io.github.lamba92.openaqua.model.host.HostDeviceType
import io.github.lamba92.openaqua.model.host.HostFanControlRequest
import io.github.lamba92.openaqua.model.host.HostFanDescriptor
import io.github.lamba92.openaqua.model.host.HostFanId
import io.github.lamba92.openaqua.model.host.HostFanTelemetry
import io.github.lamba92.openaqua.model.host.HostInfo
import io.github.lamba92.openaqua.model.host.HostSensorDescriptor
import io.github.lamba92.openaqua.model.host.HostSensorId
import io.github.lamba92.openaqua.model.host.HostSensorTelemetry
import io.github.lamba92.openaqua.model.host.HostTelemetry
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.flow.Flow
import kotlinx.io.files.Path
import platform.posix.SIGHUP
import platform.posix.SIGINT
import platform.posix.SIGTERM
import platform.posix.SIG_DFL
import platform.posix.atexit
import platform.posix.exit
import platform.posix.signal
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.time.Duration

private const val HWMON_BASE_PATH_STRING = "/sys/class/hwmon"
private val HWMON_BASE_PATH = Path(HWMON_BASE_PATH_STRING)
private val PROC_CPUINFO_PATH = Path("/proc/cpuinfo")
private val DMI_SYS_VENDOR_PATH = Path("/sys/class/dmi/id/sys_vendor")
private val DMI_BOARD_NAME_PATH = Path("/sys/class/dmi/id/board_name")
private val DMI_PRODUCT_NAME_PATH = Path("/sys/class/dmi/id/product_name")

private const val SYSFS_NAME = "name"
private const val SYSFS_UEVENT = "device/uevent"
private const val SYSFS_MODEL = "device/model"
private const val SYSFS_PRODUCT = "device/product"

private const val TEMP_PREFIX = "temp"
private const val FAN_PREFIX = "fan"
private const val PWM_PREFIX = "pwm"
private const val INPUT_SUFFIX = "_input"
private const val LABEL_SUFFIX = "_label"
private const val ENABLE_SUFFIX = "_enable"

private const val PWM_MANUAL_MODE_INT = 1
private const val PWM_MANUAL_MODE_STRING = "1"
private const val PWM_AUTO_MODE_DEFAULT = "2"
private const val PWM_AUTO_MODE_NCT = "5"
private const val MAX_RAW_PWM = 255.0
private const val MAX_RAW_PWM_INT = 255
private const val PERCENT_SCALE = 100.0
private const val ROUNDING_DECIMAL_FACTOR = 1000.0
private const val ROUNDING_DECIMAL_DIVISOR = 10.0
private const val EXIT_CODE_SIGNAL_BASE = 128

private const val MILLICELSIUS_TO_CELSIUS_FACTOR = 1000.0
private const val MIN_VALID_TEMP_CELSIUS = -40.0
private const val MAX_VALID_TEMP_CELSIUS = 150.0

private const val HWMON_PREFIX = "hwmon"
private const val NCT_CHIP_PREFIX = "nct"
private const val NVME_PREFIX = "nvme"
private const val NVIDIA_PREFIX = "nvidia"
private const val NVIDIA_TEMP_SENSOR_SUFFIX = ":temp"
private const val GPU_CORE_SENSOR_NAME = "GPU Core"
private const val RAM_DEVICE_NAME = "System Memory (RAM)"
private const val CPUINFO_MODEL_NAME_KEY = "model name"

private val CPU_HWMON_NAMES = setOf("k10temp", "zenpower", "coretemp", "cpu_thermal")
private val GPU_HWMON_NAMES = setOf("amdgpu", "nouveau", "i915", "xe")
private val RAM_HWMON_PREFIXES = listOf("spd", "ee1004", "jc42")
private val MOTHERBOARD_HWMON_PREFIXES = listOf("nct", "it8", "f71")
private val MOTHERBOARD_HWMON_SUFFIXES = listOf("_wmi")
private const val MOTHERBOARD_ACPITZ_NAME = "acpitz"

private val AQUA_COMPUTER_HWMON_NAMES =
    setOf(
        "octo",
        "quadro",
        "d5next",
        "farbwerk",
        "farbwerk360",
        "highflownext",
        "aquastreamxt",
        "poweradjust3",
        "leakshield",
    )

private val AQUA_COMPUTER_UEVENT_IDENTIFIERS = listOf("aquacomputer", "00000C70")

class LinuxHostHardwareController(
    private val nvidiaCapability: NvidiaCapability,
) : HostHardwareController {
    override fun getHostInfo(): HostInfo {
        val devices = mutableListOf<HostDevice>()

        // 1. Scan sysfs hwmon
        for (entry in HWMON_BASE_PATH.listDirectory()) {
            val namePath = Path(entry, SYSFS_NAME)
            val hwmonName = namePath.readTextOrNull()?.trim() ?: continue
            if (isAquaComputerDevice(entry, hwmonName)) continue

            val deviceType =
                when {
                    hwmonName in CPU_HWMON_NAMES -> HostDeviceType.CPU
                    hwmonName in GPU_HWMON_NAMES -> HostDeviceType.GPU
                    hwmonName.startsWith(NVME_PREFIX) -> HostDeviceType.STORAGE
                    isMotherboardHwmon(hwmonName) -> HostDeviceType.MOTHERBOARD
                    RAM_HWMON_PREFIXES.any { hwmonName.startsWith(it) } -> HostDeviceType.RAM
                    else -> HostDeviceType.OTHER
                }

            val deviceName = resolveDeviceName(entry, hwmonName, deviceType)
            val sensors = mutableListOf<HostSensorDescriptor>()
            val fans = mutableListOf<HostFanDescriptor>()

            for (file in entry.listDirectory()) {
                val fileName = file.name
                if (fileName.startsWith(TEMP_PREFIX) && fileName.endsWith(INPUT_SUFFIX)) {
                    val sensorPrefix = fileName.removeSuffix(INPUT_SUFFIX)
                    val labelPath = Path(entry, "$sensorPrefix$LABEL_SUFFIX")
                    val sensorLabel =
                        labelPath.readTextOrNull()?.trim()
                            ?: "${hwmonName.replaceFirstChar { it.uppercase() }} $sensorPrefix"

                    val sensorId = HostSensorId("${entry.name}:$sensorPrefix")
                    sensors.add(
                        HostSensorDescriptor(
                            id = sensorId,
                            name = sensorLabel,
                        ),
                    )
                }

                if (fileName.startsWith(FAN_PREFIX) && fileName.endsWith(INPUT_SUFFIX)) {
                    val fanPrefix = fileName.removeSuffix(INPUT_SUFFIX)
                    val fanIdx = fanPrefix.removePrefix(FAN_PREFIX)
                    val labelPath = Path(entry, "$fanPrefix$LABEL_SUFFIX")
                    val fanLabel =
                        labelPath.readTextOrNull()?.trim()
                            ?: "${hwmonName.replaceFirstChar { it.uppercase() }} Fan $fanIdx"

                    val fanId = HostFanId("${entry.name}:$fanPrefix")
                    val pwmFile = Path(entry, "$PWM_PREFIX$fanIdx")

                    if (pwmFile.exists()) {
                        fans.add(HostFanDescriptor.Controllable(id = fanId, name = fanLabel))
                    } else {
                        fans.add(HostFanDescriptor.ReadOnly(id = fanId, name = fanLabel))
                    }
                }
            }

            // Also check for standalone pwmX without fanX_input
            for (file in entry.listDirectory()) {
                val fileName = file.name
                if (fileName.startsWith(PWM_PREFIX) && !fileName.contains("_")) {
                    val pwmIdx = fileName.removePrefix(PWM_PREFIX)
                    val fanId = HostFanId("${entry.name}:$FAN_PREFIX$pwmIdx")
                    if (fans.none { it.id == fanId }) {
                        fans.add(
                            HostFanDescriptor.Controllable(
                                id = fanId,
                                name = "${hwmonName.replaceFirstChar { it.uppercase() }} PWM $pwmIdx",
                            ),
                        )
                    }
                }
            }

            if (sensors.isNotEmpty() || fans.isNotEmpty()) {
                devices.add(
                    HostDevice(
                        id = HostDeviceId(entry.name),
                        name = deviceName,
                        type = deviceType,
                        sensors = sensors,
                        fans = fans,
                    ),
                )
            }
        }

        val gpus =
            when (nvidiaCapability) {
                is NvidiaCapability.Available ->
                    nvidiaCapability
                        .getGpus()
                        .map { (index, name) ->
                            HostDevice(
                                id = HostDeviceId("$NVIDIA_PREFIX$index"),
                                name = name,
                                type = HostDeviceType.GPU,
                                sensors =
                                    listOf(
                                        HostSensorDescriptor(
                                            id = HostSensorId("$NVIDIA_PREFIX$index$NVIDIA_TEMP_SENSOR_SUFFIX"),
                                            name = GPU_CORE_SENSOR_NAME,
                                        ),
                                    ),
                            )
                        }

                NvidiaCapability.NotAvailable -> emptyList()
            }
        devices.addAll(gpus)
        return HostInfo(devices = devices.toList())
    }

    override fun getTelemetryFlow(pollingInterval: Duration): Flow<HostTelemetry> {
        val info = getHostInfo()
        return every(pollingInterval) {
            info.getTelemetry(nvidiaCapability)
        }
    }

    override suspend fun streamFanControl(requests: Flow<HostFanControlRequest>) {
        val sigHandler =
            staticCFunction<Int, Unit> { sig ->
                restoreHostFanDefaults()
                exit(EXIT_CODE_SIGNAL_BASE + sig)
            }
        signal(SIGINT, sigHandler)
        signal(SIGTERM, sigHandler)
        signal(SIGHUP, sigHandler)
        atexit(staticCFunction<Unit> { restoreHostFanDefaults() })

        try {
            requests.collect { request ->
                setFanSpeed(request.channel, request.pwmPercent)
            }
        } finally {
            restoreHostFanDefaults()
            signal(SIGINT, SIG_DFL)
            signal(SIGTERM, SIG_DFL)
            signal(SIGHUP, SIG_DFL)
        }
    }

    private fun setFanSpeed(
        channel: HostFanId,
        pwmPercent: Double,
    ) {
        val parts = channel.value.split(":")
        if (parts.size != 2) return
        val hwmonName = parts[0]
        val fanPrefix = parts[1]
        val pwmIdx = fanPrefix.removePrefix(FAN_PREFIX).removePrefix(PWM_PREFIX)
        val entry = Path(HWMON_BASE_PATH, hwmonName)
        val pwmPath = Path(entry, "$PWM_PREFIX$pwmIdx")
        val pwmEnablePath = Path(entry, "$PWM_PREFIX$pwmIdx$ENABLE_SUFFIX")

        val currentEnable = pwmEnablePath.readTextOrNull()?.trim()?.toIntOrNull()
        if (currentEnable != null && currentEnable != PWM_MANUAL_MODE_INT) {
            runCatching { pwmEnablePath.writeText("$PWM_MANUAL_MODE_STRING\n") }
        }

        val rawPwm = (pwmPercent / PERCENT_SCALE * MAX_RAW_PWM).roundToInt().coerceIn(0, MAX_RAW_PWM_INT)
        runCatching { pwmPath.writeText("$rawPwm\n") }
    }

    override fun restoreFanDefaults() {
        restoreHostFanDefaults()
    }
}

fun restoreHostFanDefaults() {
    for (entry in HWMON_BASE_PATH.listDirectory()) {
        val hwmonName = Path(entry, SYSFS_NAME).readTextOrNull()?.trim() ?: continue
        if (isAquaComputerDevice(entry, hwmonName)) continue

        val defaultAutoMode =
            if (hwmonName.startsWith(NCT_CHIP_PREFIX)) PWM_AUTO_MODE_NCT else PWM_AUTO_MODE_DEFAULT

        for (file in entry.listDirectory()) {
            val fileName = file.name
            if (fileName.startsWith(PWM_PREFIX) && fileName.endsWith(ENABLE_SUFFIX)) {
                val currentMode = file.readTextOrNull()?.trim()
                if (currentMode == PWM_MANUAL_MODE_STRING) {
                    val result = runCatching { file.writeText("$defaultAutoMode\n") }
                    if (result.isFailure && defaultAutoMode == PWM_AUTO_MODE_NCT) {
                        runCatching { file.writeText("$PWM_AUTO_MODE_DEFAULT\n") }
                    }
                }
            }
        }
    }
}

private fun HostInfo.getTelemetry(nvidiaCapability: NvidiaCapability): HostTelemetry {
    val sensorReadings =
        devices.flatMap { it.sensors }.map { sensor ->
            val value = readSensor(sensor.id, nvidiaCapability)
            when {
                value != null -> HostSensorTelemetry.Connected(id = sensor.id, tempCelsius = value)
                else -> HostSensorTelemetry.Disconnected(id = sensor.id)
            }
        }

    val fanReadings =
        devices.flatMap { it.fans }.map { fan ->
            readFanTelemetry(fan)
        }

    return HostTelemetry(
        sensors = sensorReadings,
        fans = fanReadings,
    )
}

private fun readFanTelemetry(fan: HostFanDescriptor): HostFanTelemetry {
    val parts = fan.id.value.split(":")
    if (parts.size != 2) return HostFanTelemetry.Disconnected(fan.id)

    val entry = Path(HWMON_BASE_PATH, parts[0])
    val fanPrefix = parts[1]
    val pwmIdx = fanPrefix.removePrefix(FAN_PREFIX).removePrefix(PWM_PREFIX)

    val fanInputPath = Path(entry, "$fanPrefix$INPUT_SUFFIX")
    val rpm = fanInputPath.readTextOrNull()?.trim()?.toIntOrNull()

    return when (fan) {
        is HostFanDescriptor.Controllable -> {
            val pwmPath = Path(entry, "$PWM_PREFIX$pwmIdx")
            val pwmEnablePath = Path(entry, "$PWM_PREFIX$pwmIdx$ENABLE_SUFFIX")

            val rawPwm = pwmPath.readTextOrNull()?.trim()?.toDoubleOrNull() ?: 0.0
            val pwmPercent = round((rawPwm / MAX_RAW_PWM) * ROUNDING_DECIMAL_FACTOR) / ROUNDING_DECIMAL_DIVISOR
            val enableMode = pwmEnablePath.readTextOrNull()?.trim()?.toIntOrNull()
            val isFollowingBiosCurve = enableMode == null || enableMode != PWM_MANUAL_MODE_INT

            val isDisconnected = rpm == null || (rpm == 0 && pwmPercent > 0.0)
            if (isDisconnected) {
                HostFanTelemetry.Disconnected(id = fan.id)
            } else {
                HostFanTelemetry.Controllable(
                    id = fan.id,
                    rpm = rpm,
                    pwmPercent = pwmPercent,
                    isFollowingBiosCurve = isFollowingBiosCurve,
                )
            }
        }

        is HostFanDescriptor.ReadOnly -> {
            if (rpm == null || rpm == 0) {
                HostFanTelemetry.Disconnected(id = fan.id)
            } else {
                HostFanTelemetry.ReadOnly(
                    id = fan.id,
                    rpm = rpm,
                )
            }
        }
    }
}

private fun readSensor(
    id: HostSensorId,
    nvidiaCapability: NvidiaCapability,
): Double? =
    when {
        id.value.startsWith(HWMON_PREFIX) -> {
            val parts = id.value.split(":")
            when (parts.size) {
                2 -> readSysfsTemp(Path("$HWMON_BASE_PATH_STRING/${parts[0]}/${parts[1]}$INPUT_SUFFIX"))
                else -> null
            }
        }

        id.value.startsWith(NVIDIA_PREFIX) && nvidiaCapability is NvidiaCapability.Available ->
            id
                .value
                .removePrefix(NVIDIA_PREFIX)
                .substringBefore(":")
                .toIntOrNull()
                ?.let { index -> nvidiaCapability.getGpuTemperature(index) }

        else -> null
    }

private fun readSysfsTemp(path: Path): Double? {
    val text = path.readTextOrNull()?.trim() ?: return null
    val milliCelsius = text.toLongOrNull() ?: return null
    val celsius = milliCelsius / MILLICELSIUS_TO_CELSIUS_FACTOR
    // Motherboard thermistors report out-of-range when disconnected (e.g. -55°C)
    if (celsius !in MIN_VALID_TEMP_CELSIUS..MAX_VALID_TEMP_CELSIUS) return null
    return celsius
}

private fun isAquaComputerDevice(
    entry: Path,
    hwmonName: String,
): Boolean {
    if (hwmonName.lowercase() in AQUA_COMPUTER_HWMON_NAMES) return true
    val uevent = Path(entry, SYSFS_UEVENT).readTextOrNull() ?: return false
    return AQUA_COMPUTER_UEVENT_IDENTIFIERS.any { uevent.contains(it, ignoreCase = true) }
}

private fun isMotherboardHwmon(name: String): Boolean =
    MOTHERBOARD_HWMON_SUFFIXES.any { name.endsWith(it) } ||
        MOTHERBOARD_HWMON_PREFIXES.any { name.startsWith(it) } ||
        name == MOTHERBOARD_ACPITZ_NAME

private fun resolveDeviceName(
    entry: Path,
    hwmonName: String,
    type: HostDeviceType,
): String =
    when (type) {
        HostDeviceType.CPU ->
            PROC_CPUINFO_PATH
                .readTextOrNull()
                ?.lineSequence()
                ?.firstOrNull { it.startsWith(CPUINFO_MODEL_NAME_KEY) }
                ?.substringAfter(":")
                ?.trim()
                ?: "Host CPU ($hwmonName)"

        HostDeviceType.MOTHERBOARD -> getMotherboardName(hwmonName)
        HostDeviceType.STORAGE -> Path(entry, SYSFS_MODEL).readTextOrNull()?.trim() ?: "NVMe Storage (${entry.name})"
        HostDeviceType.GPU ->
            Path(entry, SYSFS_PRODUCT).readTextOrNull()?.trim() ?: "GPU (${hwmonName.replaceFirstChar { it.uppercase() }})"

        HostDeviceType.RAM -> RAM_DEVICE_NAME
        HostDeviceType.OTHER -> hwmonName.replaceFirstChar { it.uppercase() }
    }

private fun getMotherboardName(hwmonName: String): String {
    val vendor = DMI_SYS_VENDOR_PATH.readTextOrNull()?.trim()
    val board =
        DMI_BOARD_NAME_PATH.readTextOrNull()?.trim()
            ?: DMI_PRODUCT_NAME_PATH.readTextOrNull()?.trim()
    val combined = listOfNotNull(vendor, board).joinToString(" ").trim()
    return combined.ifBlank { "Motherboard ($hwmonName)" }
}
