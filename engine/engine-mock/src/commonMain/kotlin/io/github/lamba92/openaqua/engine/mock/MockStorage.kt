package io.github.lamba92.openaqua.engine.mock

import io.github.lamba92.openaqua.engine.util.atomicWriteText
import io.github.lamba92.openaqua.engine.util.readText
import io.github.lamba92.openaqua.model.AutoCurveFanConfig
import io.github.lamba92.openaqua.model.CurveFanConfig
import io.github.lamba92.openaqua.model.CurvePoint
import io.github.lamba92.openaqua.model.D5NextConfig
import io.github.lamba92.openaqua.model.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.DeviceConfig
import io.github.lamba92.openaqua.model.DeviceRegistry
import io.github.lamba92.openaqua.model.DeviceType
import io.github.lamba92.openaqua.model.DisplayConfig
import io.github.lamba92.openaqua.model.FixedFanConfig
import io.github.lamba92.openaqua.model.OctoConfig
import io.github.lamba92.openaqua.model.QuadroConfig
import io.github.lamba92.openaqua.model.RgbEffectMode
import io.github.lamba92.openaqua.model.RgbpxPortConfig
import io.github.lamba92.openaqua.model.SensorOffset
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val MOCK_CONFIG_PATH = Path("mock-config.json")
val MOCK_VIRTUAL_SENSORS_PATH = Path("mock-virtual-sensors.json")

private val mockJson =
    Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

@Serializable
data class MockConfigStorage(
    val devices: List<DetectedHardwareDevice>,
    val configs: Map<String, DeviceConfig>,
)

private fun defaultCurvePoints(): List<CurvePoint> =
    AutoCurveFanConfig(
        channel = 0,
        sensorIndex = 0,
        tMin = 20.0,
        tMax = 57.5,
        pMin = 30.0,
        pMax = 97.5,
        shape = 0.0,
    ).toCurveFanConfig().points

fun defaultOctoConfig(): OctoConfig =
    OctoConfig(
        fans =
            listOf(
                CurveFanConfig(channel = 0, sensorIndex = 0, points = defaultCurvePoints()),
                FixedFanConfig(channel = 1, powerPercent = 40.0),
                FixedFanConfig(channel = 2, powerPercent = 50.0),
                FixedFanConfig(channel = 3, powerPercent = 50.0),
                FixedFanConfig(channel = 4, powerPercent = 0.0),
                FixedFanConfig(channel = 5, powerPercent = 0.0),
                FixedFanConfig(channel = 6, powerPercent = 0.0),
                FixedFanConfig(channel = 7, powerPercent = 0.0),
            ),
        sensorOffsets =
            listOf(
                SensorOffset(channel = 0, offsetCelsius = 0.0),
                SensorOffset(channel = 1, offsetCelsius = -0.2),
                SensorOffset(channel = 2, offsetCelsius = 0.0),
                SensorOffset(channel = 3, offsetCelsius = 0.5),
            ),
        rgbpx =
            listOf(
                RgbpxPortConfig(port = 0, enabled = true, ledCount = 30, brightnessPercent = 100, mode = RgbEffectMode.STATIC),
                RgbpxPortConfig(port = 1, enabled = true, ledCount = 30, brightnessPercent = 100, mode = RgbEffectMode.STATIC),
            ),
    )

fun defaultQuadroConfig(): QuadroConfig =
    QuadroConfig(
        fans =
            listOf(
                CurveFanConfig(channel = 0, sensorIndex = 0, points = defaultCurvePoints()),
                FixedFanConfig(channel = 1, powerPercent = 50.0),
                FixedFanConfig(channel = 2, powerPercent = 50.0),
                FixedFanConfig(channel = 3, powerPercent = 0.0),
            ),
        sensorOffsets =
            listOf(
                SensorOffset(channel = 0, offsetCelsius = 0.0),
                SensorOffset(channel = 1, offsetCelsius = 0.0),
                SensorOffset(channel = 2, offsetCelsius = 0.0),
                SensorOffset(channel = 3, offsetCelsius = 0.0),
            ),
        flowPulsesPerLiter = 169,
        rgbpx =
            listOf(
                RgbpxPortConfig(port = 0, enabled = true, ledCount = 30, brightnessPercent = 100, mode = RgbEffectMode.STATIC),
            ),
    )

fun defaultD5NextConfig(): D5NextConfig =
    D5NextConfig(
        pump = FixedFanConfig(channel = 0, powerPercent = 60.0),
        fan = CurveFanConfig(channel = 1, sensorIndex = 0, points = defaultCurvePoints()),
        coolantOffset = SensorOffset(channel = 0, offsetCelsius = 0.0),
        display = DisplayConfig(brightnessPercent = 80, orientationDegrees = 0),
        rgbpx =
            listOf(
                RgbpxPortConfig(port = 0, enabled = true, ledCount = 15, brightnessPercent = 100, mode = RgbEffectMode.STATIC),
            ),
    )

fun defaultMockStorage(): MockConfigStorage =
    MockConfigStorage(
        devices =
            listOf(
                DetectedHardwareDevice(
                    id = "mock_octo_0",
                    alias = "octo-1",
                    type = DeviceType.OCTO,
                    name = DeviceRegistry.OCTO.name,
                    serial = "13235-54067-MOCK",
                    firmwareVersion = 1020,
                ),
                DetectedHardwareDevice(
                    id = "mock_quadro_0",
                    alias = "quadro-1",
                    type = DeviceType.QUADRO,
                    name = DeviceRegistry.QUADRO.name,
                    serial = "13236-54068-MOCK",
                    firmwareVersion = 1018,
                ),
                DetectedHardwareDevice(
                    id = "mock_d5next_0",
                    alias = "d5next-1",
                    type = DeviceType.D5_NEXT,
                    name = DeviceRegistry.D5_NEXT.name,
                    serial = "13237-54069-MOCK",
                    firmwareVersion = 1015,
                ),
            ),
        configs =
            mapOf(
                "octo-1" to defaultOctoConfig(),
                "quadro-1" to defaultQuadroConfig(),
                "d5next-1" to defaultD5NextConfig(),
            ),
    )

fun defaultVirtualSensorsStorage(): Map<String, Map<Int, Double?>> =
    mapOf(
        "octo-1" to
            (4..19).associateWith {
                if (it == 4) {
                    38.2
                } else if (it == 5) {
                    55.0
                } else {
                    null
                }
            },
        "quadro-1" to (4..19).associateWith { if (it == 4) 35.0 else null },
        "d5next-1" to (1..8).associateWith { if (it == 1) 40.0 else null },
    )

fun initializeMockStorage(fileSystem: FileSystem = SystemFileSystem) {
    if (!fileSystem.exists(MOCK_CONFIG_PATH)) {
        saveMockStorage(defaultMockStorage(), fileSystem)
    }
    if (!fileSystem.exists(MOCK_VIRTUAL_SENSORS_PATH)) {
        saveMockVirtualSensorsStorage(defaultVirtualSensorsStorage(), fileSystem)
    }
}

fun loadMockStorage(fileSystem: FileSystem = SystemFileSystem): MockConfigStorage =
    mockJson.decodeFromString<MockConfigStorage>(MOCK_CONFIG_PATH.readText(fileSystem))

fun saveMockStorage(
    storage: MockConfigStorage,
    fileSystem: FileSystem = SystemFileSystem,
) {
    val text = mockJson.encodeToString(MockConfigStorage.serializer(), storage)
    MOCK_CONFIG_PATH.atomicWriteText(text, fileSystem)
}

fun loadMockVirtualSensorsStorage(fileSystem: FileSystem = SystemFileSystem): Map<String, Map<Int, Double?>> =
    mockJson.decodeFromString<Map<String, Map<Int, Double?>>>(MOCK_VIRTUAL_SENSORS_PATH.readText(fileSystem))

fun saveMockVirtualSensorsStorage(
    sensors: Map<String, Map<Int, Double?>>,
    fileSystem: FileSystem = SystemFileSystem,
) {
    val text = mockJson.encodeToString(sensors)
    MOCK_VIRTUAL_SENSORS_PATH.atomicWriteText(text, fileSystem)
}
