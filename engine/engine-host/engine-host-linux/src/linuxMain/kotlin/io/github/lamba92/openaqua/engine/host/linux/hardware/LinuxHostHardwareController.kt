package io.github.lamba92.openaqua.engine.host.linux.hardware

import io.github.lamba92.openaqua.engine.host.hardware.HostHardwareController
import io.github.lamba92.openaqua.model.every
import io.github.lamba92.openaqua.model.host.HostDevice
import io.github.lamba92.openaqua.model.host.HostDeviceId
import io.github.lamba92.openaqua.model.host.HostDeviceType
import io.github.lamba92.openaqua.model.host.HostFanControlRequest
import io.github.lamba92.openaqua.model.host.HostInfo
import io.github.lamba92.openaqua.model.host.HostSensorDescriptor
import io.github.lamba92.openaqua.model.host.HostSensorId
import io.github.lamba92.openaqua.model.host.HostSensorTelemetry
import io.github.lamba92.openaqua.model.host.HostTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.time.Duration

class LinuxHostHardwareController(
    private val nvidiaCapability: NvidiaCapability,
) : HostHardwareController {
    private val hwmonBasePath = Path("/sys/class/hwmon")

    private val cpuHwmonNames = setOf("k10temp", "zenpower", "coretemp", "cpu_thermal")
    private val gpuHwmonNames = setOf("amdgpu", "nouveau", "i915", "xe")

    override fun getHostInfo(): HostInfo {
        val devices = mutableListOf<HostDevice>()

        // 1. Scan sysfs hwmon
        for (entry in hwmonBasePath.listOrEmpty()) {
            val namePath = Path(entry, "name")
            val hwmonName = namePath.readTextOrNull() ?: continue
            if (isAquaComputerDevice(entry, hwmonName)) continue

            val deviceType =
                when {
                    hwmonName in cpuHwmonNames -> HostDeviceType.CPU
                    hwmonName in gpuHwmonNames -> HostDeviceType.GPU
                    hwmonName.startsWith("nvme") -> HostDeviceType.STORAGE
                    isMotherboardHwmon(hwmonName) -> HostDeviceType.MOTHERBOARD
                    hwmonName.startsWith("spd") || hwmonName.startsWith("ee1004") || hwmonName.startsWith("jc42") -> HostDeviceType.RAM
                    else -> HostDeviceType.OTHER
                }

            val deviceName = resolveDeviceName(entry, hwmonName, deviceType)
            val sensors = mutableListOf<HostSensorDescriptor>()

            for (file in entry.listOrEmpty()) {
                val fileName = file.name
                if (!fileName.startsWith("temp") || !fileName.endsWith("_input")) continue

                val sensorPrefix = fileName.removeSuffix("_input")
                val labelPath = Path(entry, "${sensorPrefix}_label")
                val sensorLabel =
                    labelPath.readTextOrNull()
                        ?: "${hwmonName.replaceFirstChar { it.uppercase() }} $sensorPrefix"

                val sensorId = HostSensorId("${entry.name}:$sensorPrefix")
                sensors.add(
                    HostSensorDescriptor(
                        id = sensorId,
                        name = sensorLabel,
                    ),
                )
            }

            if (sensors.isNotEmpty()) {
                devices.add(
                    HostDevice(
                        id = HostDeviceId(entry.name),
                        name = deviceName,
                        type = deviceType,
                        sensors = sensors,
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
                                id = HostDeviceId("nvidia$index"),
                                name = name,
                                type = HostDeviceType.GPU,
                                sensors =
                                    listOf(
                                        HostSensorDescriptor(
                                            id = HostSensorId("nvidia$index:temp"),
                                            name = "GPU Core",
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
        requests.collect { _ ->
            // Linux sysfs hwmon implementation in Task 3
        }
    }

    override fun restoreFanDefaults() {
        // Linux sysfs hwmon implementation in Task 3
    }
}

private fun HostInfo.getTelemetry(nvidiaCapability: NvidiaCapability): HostTelemetry {
    val sensorReadings =
        devices.flatMap { it.sensors }.map { sensor ->
            when (val temp = readSensor(sensor.id, nvidiaCapability)) {
                null -> HostSensorTelemetry.Disconnected(id = sensor.id)
                else -> HostSensorTelemetry.Connected(id = sensor.id, tempCelsius = temp)
            }
        }
    return HostTelemetry(sensors = sensorReadings)
}

private fun readSensor(
    id: HostSensorId,
    nvidiaCapability: NvidiaCapability,
): Double? =
    when {
        id.value.startsWith("hwmon") -> {
            val parts = id.value.split(":")
            when (parts.size) {
                2 -> readSysfsTemp(Path("/sys/class/hwmon/${parts[0]}/${parts[1]}_input"))
                else -> null
            }
        }

        id.value.startsWith("nvidia") && nvidiaCapability is NvidiaCapability.Available ->
            id
                .value
                .removePrefix("nvidia")
                .substringBefore(":")
                .toIntOrNull()
                ?.let { index -> nvidiaCapability.getGpuTemperature(index) }

        else -> null
    }

private fun readSysfsTemp(path: Path): Double? {
    val text = path.readTextOrNull() ?: return null
    val milliCelsius = text.toLongOrNull() ?: return null
    val celsius = milliCelsius / 1000.0
    // Motherboard thermistors report out-of-range when disconnected (e.g. -55°C)
    if (celsius < -40.0 || celsius > 150.0) return null
    return celsius
}

private val aquaComputerHwmonNames =
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

private fun isAquaComputerDevice(
    entry: Path,
    hwmonName: String,
): Boolean {
    if (hwmonName.lowercase() in aquaComputerHwmonNames) return true
    val uevent = Path(entry, "device/uevent").readTextOrNull() ?: return false
    return uevent.contains("aquacomputer", ignoreCase = true) ||
        uevent.contains("00000C70", ignoreCase = true)
}

private fun isMotherboardHwmon(name: String): Boolean =
    name.endsWith("_wmi") ||
        name.startsWith("nct") ||
        name.startsWith("it8") ||
        name == "acpitz" ||
        name.startsWith("f71")

private fun resolveDeviceName(
    entry: Path,
    hwmonName: String,
    type: HostDeviceType,
): String =
    when (type) {
        HostDeviceType.CPU ->
            Path("/proc/cpuinfo")
                .readTextOrNull()
                ?.lineSequence()
                ?.firstOrNull { it.startsWith("model name") }
                ?.substringAfter(":")
                ?.trim()
                ?: "Host CPU ($hwmonName)"

        HostDeviceType.MOTHERBOARD -> getMotherboardName(hwmonName)
        HostDeviceType.STORAGE -> Path(entry, "device/model").readTextOrNull() ?: "NVMe Storage (${entry.name})"
        HostDeviceType.GPU ->
            Path(entry, "device/product").readTextOrNull() ?: "GPU (${hwmonName.replaceFirstChar { it.uppercase() }})"

        HostDeviceType.RAM -> "System Memory (RAM)"
        HostDeviceType.OTHER -> hwmonName.replaceFirstChar { it.uppercase() }
    }

private fun getMotherboardName(hwmonName: String): String {
    val vendor = Path("/sys/class/dmi/id/sys_vendor").readTextOrNull()
    val board =
        Path("/sys/class/dmi/id/board_name").readTextOrNull()
            ?: Path("/sys/class/dmi/id/product_name").readTextOrNull()
    val combined = listOfNotNull(vendor, board).joinToString(" ").trim()
    return combined.ifBlank { "Motherboard ($hwmonName)" }
}

private fun Path.readTextOrNull(fileSystem: FileSystem = SystemFileSystem): String? =
    try {
        fileSystem
            .source(this)
            .buffered()
            .use { it.readString() }
            .trim()
    } catch (_: Throwable) {
        null
    }

private fun Path.listOrEmpty(fileSystem: FileSystem = SystemFileSystem): List<Path> =
    try {
        fileSystem.list(this).toList()
    } catch (_: Throwable) {
        emptyList()
    }
