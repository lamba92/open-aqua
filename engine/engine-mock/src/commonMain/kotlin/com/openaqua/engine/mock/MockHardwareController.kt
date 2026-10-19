package com.openaqua.engine.mock

import com.openaqua.engine.hardware.HardwareController
import com.openaqua.model.CurveFanConfig
import com.openaqua.model.CurvePoint
import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.DeviceConfig
import com.openaqua.model.DeviceRegistry
import com.openaqua.model.DeviceTelemetry
import com.openaqua.model.DeviceType
import com.openaqua.model.FanTelemetry
import com.openaqua.model.FixedFanConfig
import com.openaqua.model.FlashConfigRequest
import com.openaqua.model.FlashOctoConfig
import com.openaqua.model.OctoConfig
import com.openaqua.model.OctoTelemetry
import com.openaqua.model.RgbEffectMode
import com.openaqua.model.RgbpxPortConfig
import com.openaqua.model.SensorOffset
import com.openaqua.model.SensorTelemetry
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.seconds

class MockHardwareController : HardwareController {
    private val mockFans =
        mutableListOf(
            CurveFanConfig(
                channel = 0,
                sensorIndex = 0,
                points =
                    listOf(
                        CurvePoint(20.0, 30.0),
                        CurvePoint(22.5, 34.5),
                        CurvePoint(25.0, 39.0),
                        CurvePoint(27.5, 43.5),
                        CurvePoint(30.0, 48.0),
                        CurvePoint(32.5, 52.5),
                        CurvePoint(35.0, 57.0),
                        CurvePoint(37.5, 61.5),
                        CurvePoint(40.0, 66.0),
                        CurvePoint(42.5, 70.5),
                        CurvePoint(45.0, 75.0),
                        CurvePoint(47.5, 79.5),
                        CurvePoint(50.0, 84.0),
                        CurvePoint(52.5, 88.5),
                        CurvePoint(55.0, 93.0),
                        CurvePoint(57.5, 97.5),
                    ),
            ),
            FixedFanConfig(channel = 1, powerPercent = 40.0),
            FixedFanConfig(channel = 2, powerPercent = 50.0),
            FixedFanConfig(channel = 3, powerPercent = 50.0),
            FixedFanConfig(channel = 4, powerPercent = 0.0),
            FixedFanConfig(channel = 5, powerPercent = 0.0),
            FixedFanConfig(channel = 6, powerPercent = 0.0),
            FixedFanConfig(channel = 7, powerPercent = 0.0),
        )

    private val mockRgbpx =
        mutableMapOf(
            0 to RgbpxPortConfig(port = 0, enabled = true, ledCount = 30, brightnessPercent = 100, mode = RgbEffectMode.STATIC),
            1 to RgbpxPortConfig(port = 1, enabled = true, ledCount = 30, brightnessPercent = 100, mode = RgbEffectMode.STATIC),
        )

    private val mockOffsets =
        mutableListOf(
            SensorOffset(channel = 0, offsetCelsius = 0.0),
            SensorOffset(channel = 1, offsetCelsius = -0.2),
            SensorOffset(channel = 2, offsetCelsius = 0.0),
            SensorOffset(channel = 3, offsetCelsius = 0.5),
        )

    private val mockVirtualSensors: MutableMap<Int, Double?> =
        (4..19)
            .associateWith { channel ->
                when (channel) {
                    4 -> 38.2
                    5 -> 55.0
                    else -> null
                }
            }.toMutableMap()

    override fun scanDevices(): List<DetectedHardwareDevice> =
        listOf(
            DetectedHardwareDevice(
                id = "mock_octo_0",
                alias = "octo-1",
                type = DeviceType.OCTO,
                name = DeviceRegistry.OCTO.name,
                serial = "13235-54067-MOCK",
                firmwareVersion = 1020,
            ),
        )

    override fun streamTelemetry(device: DetectedHardwareDevice): Flow<DeviceTelemetry> =
        flow {
            while (currentCoroutineContext().isActive) {
                emit(
                    OctoTelemetry(
                        sensors =
                            listOf(
                                SensorTelemetry(0, 32.5),
                                SensorTelemetry(1, 22.8),
                                SensorTelemetry(2, 27.4),
                                SensorTelemetry(3, 43.1),
                            ) +
                                mockVirtualSensors.map { (ch, temp) ->
                                    SensorTelemetry(ch, temp, isVirtual = true)
                                },
                        fans =
                            listOf(
                                FanTelemetry(
                                    channel = 0,
                                    rpm = 2450,
                                    powerWatts = 8.4,
                                    voltageVolts = 12.0,
                                    currentAmps = 0.7,
                                    pwmPercent = 45.0,
                                ),
                                FanTelemetry(
                                    channel = 1,
                                    rpm = 850,
                                    powerWatts = 1.2,
                                    voltageVolts = 12.0,
                                    currentAmps = 0.1,
                                    pwmPercent = 30.0,
                                ),
                                FanTelemetry(
                                    channel = 2,
                                    rpm = 920,
                                    powerWatts = 1.6,
                                    voltageVolts = 12.0,
                                    currentAmps = 0.13,
                                    pwmPercent = 35.0,
                                ),
                                FanTelemetry(
                                    channel = 3,
                                    rpm = 650,
                                    powerWatts = 0.8,
                                    voltageVolts = 12.0,
                                    currentAmps = 0.07,
                                    pwmPercent = 25.0,
                                ),
                                FanTelemetry(
                                    channel = 4,
                                    rpm = 0,
                                    powerWatts = 0.0,
                                    voltageVolts = 0.0,
                                    currentAmps = 0.0,
                                    pwmPercent = 0.0,
                                ),
                                FanTelemetry(
                                    channel = 5,
                                    rpm = 0,
                                    powerWatts = 0.0,
                                    voltageVolts = 0.0,
                                    currentAmps = 0.0,
                                    pwmPercent = 0.0,
                                ),
                                FanTelemetry(
                                    channel = 6,
                                    rpm = 0,
                                    powerWatts = 0.0,
                                    voltageVolts = 0.0,
                                    currentAmps = 0.0,
                                    pwmPercent = 0.0,
                                ),
                                FanTelemetry(
                                    channel = 7,
                                    rpm = 0,
                                    powerWatts = 0.0,
                                    voltageVolts = 0.0,
                                    currentAmps = 0.0,
                                    pwmPercent = 0.0,
                                ),
                            ),
                    ),
                )
                delay(1.seconds)
            }
        }

    override fun readConfig(device: DetectedHardwareDevice): DeviceConfig =
        OctoConfig(
            fans = mockFans.toList(),
            sensorOffsets = mockOffsets.toList(),
            rgbpx = mockRgbpx.values.toList(),
        )

    override fun flashConfig(
        device: DetectedHardwareDevice,
        request: FlashConfigRequest,
    ): DeviceConfig {
        check(request is FlashOctoConfig) {
            "Expected FlashOctoConfig for device type OCTO, got ${request::class.simpleName}"
        }

        request.fans?.forEach { (channel, config) ->
            val idx = mockFans.indexOfFirst { it.channel == channel }
            if (idx >= 0) {
                mockFans[idx] = config
            }
        }

        request.sensorOffsets?.forEach { (channel, offset) ->
            val idx = mockOffsets.indexOfFirst { it.channel == channel }
            if (idx >= 0) {
                mockOffsets[idx] = SensorOffset(channel, offset)
            }
        }

        request.rgbpx?.forEach { (port, config) ->
            mockRgbpx[port] = config
        }

        return readConfig(device)
    }

    override fun feedVirtualSensors(
        device: DetectedHardwareDevice,
        sensors: Map<Int, Double>,
    ) {
        sensors.forEach { (channel, temp) ->
            if (channel in 4..19) {
                mockVirtualSensors[channel] = temp
            }
        }
    }
}
