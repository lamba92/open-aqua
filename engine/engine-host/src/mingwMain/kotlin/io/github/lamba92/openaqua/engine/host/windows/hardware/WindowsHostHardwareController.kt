package io.github.lamba92.openaqua.engine.host.windows.hardware

import io.github.lamba92.libhwmon.ControlMode
import io.github.lamba92.libhwmon.HardwareDescriptor
import io.github.lamba92.libhwmon.HardwareReading
import io.github.lamba92.libhwmon.HardwareType
import io.github.lamba92.libhwmon.HwmonClient
import io.github.lamba92.libhwmon.HwmonConfig
import io.github.lamba92.libhwmon.SensorId
import io.github.lamba92.libhwmon.SensorType
import io.github.lamba92.libhwmon.TelemetrySnapshot
import io.github.lamba92.openaqua.engine.host.hardware.HostHardwareController
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.round
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class WindowsHostHardwareController(
    private val client: HwmonClient =
        HwmonClient(
            HwmonConfig(
                enableCpu = true,
                enableGpu = true,
                enableMotherboard = true,
                enableStorage = true,
                enableController = true,
                enableMemory = false,
                enableNetwork = false,
                enableBattery = false,
            ),
        ),
) : HostHardwareController,
    AutoCloseable {
    override suspend fun getHostInfo(): HostInfo {
        val tree = client.getHardwareTree()
        val devices =
            buildList {
                for (hw in tree) {
                    collectHardware(hw)
                }
            }
        return HostInfo(devices)
    }

    override fun getTelemetryFlow(pollingInterval: Duration): Flow<HostTelemetry> =
        client.telemetryFlow(pollingInterval).map { it.toHostTelemetry() }

    override suspend fun streamFanControl(requests: Flow<HostFanControlRequest>) {
        try {
            requests.collect { request ->
                val channel = request.channel.value
                val sensorId =
                    if (channel.contains("/fan/")) {
                        SensorId.from(channel.replace("/fan/", "/control/"))
                    } else {
                        SensorId.from(channel)
                    }
                client.setControlSoftware(sensorId, request.pwmPercent.toFloat())
            }
        } finally {
            restoreFanDefaults()
        }
    }

    override suspend fun restoreFanDefaults() {
        client.restoreAllControlDefaults()
    }

    override fun close() {
        client.close()
    }
}

private fun MutableList<HostDevice>.collectHardware(hw: HardwareDescriptor) {
    if (isAquaComputerDevice(hw.name, hw.id.value)) return

    val tempSensors =
        hw.sensors
            .filter { it.type == SensorType.TEMPERATURE }
            .map { HostSensorDescriptor(HostSensorId(it.id.value), it.name) }

    val fanSensors = hw.sensors.filter { it.type == SensorType.FAN }
    val controlSensors = hw.sensors.filter { it.type == SensorType.CONTROL && it.hasControl }

    val fanDescriptors = mutableListOf<HostFanDescriptor>()
    val allIndices =
        (fanSensors.map { it.index } + controlSensors.map { it.index })
            .distinct()
            .sorted()

    for (index in allIndices) {
        val fanSensor = fanSensors.firstOrNull { it.index == index }
        val controlSensor = controlSensors.firstOrNull { it.index == index }

        if (controlSensor != null) {
            val idStr = fanSensor?.id?.value ?: controlSensor.id.value
            val fanId = HostFanId(idStr)
            val name = fanSensor?.name?.takeIf { it.isNotBlank() } ?: controlSensor.name
            fanDescriptors.add(HostFanDescriptor.Controllable(fanId, name))
        } else if (fanSensor != null) {
            val fanId = HostFanId(fanSensor.id.value)
            fanDescriptors.add(HostFanDescriptor.ReadOnly(fanId, fanSensor.name))
        }
    }

    if (tempSensors.isNotEmpty() || fanDescriptors.isNotEmpty()) {
        add(
            HostDevice(
                id = HostDeviceId(hw.id.value),
                name = hw.name,
                type = hw.type.toHostDeviceType(),
                sensors = tempSensors,
                fans = fanDescriptors,
            ),
        )
    }

    for (sub in hw.subHardware) {
        collectHardware(sub)
    }
}

private fun TelemetrySnapshot.toHostTelemetry(): HostTelemetry {
    val sensorTelemetry = mutableListOf<HostSensorTelemetry>()
    val fanTelemetry = mutableListOf<HostFanTelemetry>()

    for (hw in hardware) {
        collectTelemetry(hw, sensorTelemetry, fanTelemetry)
    }

    return HostTelemetry(
        sensors = sensorTelemetry,
        fans = fanTelemetry,
    )
}

private fun collectTelemetry(
    hw: HardwareReading,
    sensorTelemetry: MutableList<HostSensorTelemetry>,
    fanTelemetry: MutableList<HostFanTelemetry>,
) {
    if (isAquaComputerDevice(hw.name, hw.id.value)) return

    for (sensor in hw.sensors) {
        if (sensor.type != SensorType.TEMPERATURE) continue
        val id = HostSensorId(sensor.id.value)
        val v = sensor.value
        if (v != null && v in -40f..150f) {
            val rounded = round(v.toDouble() * 100.0) / 100.0
            sensorTelemetry.add(HostSensorTelemetry.Connected(id, rounded))
        } else {
            sensorTelemetry.add(HostSensorTelemetry.Disconnected(id))
        }
    }

    val fanSensors = hw.sensors.filter { it.type == SensorType.FAN }
    val controlSensors = hw.sensors.filter { it.type == SensorType.CONTROL }

    val allIndices =
        (fanSensors.map { it.id.value.substringAfterLast('/') } + controlSensors.map { it.id.value.substringAfterLast('/') })
            .distinct()
            .sorted()

    for (idx in allIndices) {
        val fanSensor = fanSensors.firstOrNull { it.id.value.substringAfterLast('/') == idx }
        val controlSensor = controlSensors.firstOrNull { it.id.value.substringAfterLast('/') == idx }

        val rpm = fanSensor?.value?.let { round(it).toInt() }

        if (controlSensor != null) {
            val idStr = fanSensor?.id?.value ?: controlSensor.id.value
            val fanId = HostFanId(idStr)
            val rawPwm = controlSensor.value ?: controlSensor.controlSoftwareValue ?: 0f
            val pwmPercent = round(rawPwm.toDouble().coerceIn(0.0, 100.0) * 10.0) / 10.0
            val isFollowingBiosCurve = controlSensor.controlMode != ControlMode.SOFTWARE

            val isDisconnected = rpm == null || (rpm == 0 && pwmPercent > 0.0)
            if (isDisconnected) {
                fanTelemetry.add(HostFanTelemetry.Disconnected(fanId))
            } else {
                checkNotNull(rpm)
                fanTelemetry.add(
                    HostFanTelemetry.Controllable(
                        id = fanId,
                        rpm = rpm,
                        pwmPercent = pwmPercent,
                        isFollowingBiosCurve = isFollowingBiosCurve,
                    ),
                )
            }
        } else if (fanSensor != null) {
            val fanId = HostFanId(fanSensor.id.value)
            if (rpm == null || rpm == 0) {
                fanTelemetry.add(HostFanTelemetry.Disconnected(fanId))
            } else {
                fanTelemetry.add(HostFanTelemetry.ReadOnly(fanId, rpm))
            }
        }
    }

    for (sub in hw.subHardware) {
        collectTelemetry(sub, sensorTelemetry, fanTelemetry)
    }
}

private fun HardwareType.toHostDeviceType(): HostDeviceType =
    when (this) {
        HardwareType.CPU -> HostDeviceType.CPU
        HardwareType.GPU_NVIDIA, HardwareType.GPU_AMD, HardwareType.GPU_INTEL -> HostDeviceType.GPU
        HardwareType.MOTHERBOARD, HardwareType.SUPER_IO, HardwareType.EMBEDDED_CONTROLLER -> HostDeviceType.MOTHERBOARD
        HardwareType.RAM -> HostDeviceType.RAM
        HardwareType.STORAGE -> HostDeviceType.STORAGE
        else -> HostDeviceType.OTHER
    }

private fun isAquaComputerDevice(
    name: String,
    id: String,
): Boolean {
    val lowerName = name.lowercase()
    val lowerId = id.lowercase()
    return lowerName.contains("aquacomputer") ||
        lowerName.contains("aqua computer") ||
        lowerName.contains("octo") ||
        lowerName.contains("quadro") ||
        lowerName.contains("d5next") ||
        lowerName.contains("d5 next") ||
        lowerName.contains("farbwerk") ||
        lowerName.contains("highflownext") ||
        lowerId.contains("0c70")
}
