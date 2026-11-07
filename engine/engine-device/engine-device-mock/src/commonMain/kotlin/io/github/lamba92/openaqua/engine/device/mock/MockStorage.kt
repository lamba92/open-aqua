package io.github.lamba92.openaqua.engine.device.mock

import io.github.lamba92.openaqua.engine.util.atomicWriteText
import io.github.lamba92.openaqua.engine.util.readText
import io.github.lamba92.openaqua.model.FeatureState
import io.github.lamba92.openaqua.model.device.AutoCurveFanConfig
import io.github.lamba92.openaqua.model.device.CurveFanConfig
import io.github.lamba92.openaqua.model.device.CurvePoint
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.device.DeviceAlias
import io.github.lamba92.openaqua.model.device.DeviceConfig
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import io.github.lamba92.openaqua.model.device.DeviceType
import io.github.lamba92.openaqua.model.device.DisplayConfig
import io.github.lamba92.openaqua.model.device.FanLimits
import io.github.lamba92.openaqua.model.device.FixedFanConfig
import io.github.lamba92.openaqua.model.device.RgbEffectMode
import io.github.lamba92.openaqua.model.device.RgbpxPortConfig
import io.github.lamba92.openaqua.model.device.SensorOffset
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

private val defaultMockLimits = FanLimits(minPowerPercent = 5.0, maxPowerPercent = 100.0)

fun defaultOctoConfig(): DeviceConfig =
    DeviceConfig(
        fans =
            listOf(
                CurveFanConfig(channel = 0, sensorIndex = 0, points = defaultCurvePoints(), limits = defaultMockLimits),
                FixedFanConfig(channel = 1, powerPercent = 40.0, limits = defaultMockLimits),
                FixedFanConfig(channel = 2, powerPercent = 50.0, limits = defaultMockLimits),
                FixedFanConfig(channel = 3, powerPercent = 50.0, limits = defaultMockLimits),
                FixedFanConfig(channel = 4, powerPercent = 0.0, limits = defaultMockLimits),
                FixedFanConfig(channel = 5, powerPercent = 0.0, limits = defaultMockLimits),
                FixedFanConfig(channel = 6, powerPercent = 0.0, limits = defaultMockLimits),
                FixedFanConfig(channel = 7, powerPercent = 0.0, limits = defaultMockLimits),
            ),
        sensorOffsets =
            listOf(
                SensorOffset(channel = 0, offsetCelsius = 0.0),
                SensorOffset(channel = 1, offsetCelsius = -0.2),
                SensorOffset(channel = 2, offsetCelsius = 0.0),
                SensorOffset(channel = 3, offsetCelsius = 0.5),
            ),
        flowPulsesPerLiter = FeatureState.Disconnected,
        display = FeatureState.Unsupported,
        rgbpx =
            listOf(
                RgbpxPortConfig(port = 0, enabled = true, ledCount = 30, brightnessPercent = 100, mode = RgbEffectMode.STATIC),
                RgbpxPortConfig(port = 1, enabled = true, ledCount = 30, brightnessPercent = 100, mode = RgbEffectMode.STATIC),
            ),
    )

fun defaultQuadroConfig(): DeviceConfig =
    DeviceConfig(
        fans =
            listOf(
                CurveFanConfig(channel = 0, sensorIndex = 0, points = defaultCurvePoints(), limits = defaultMockLimits),
                FixedFanConfig(channel = 1, powerPercent = 50.0, limits = defaultMockLimits),
                FixedFanConfig(channel = 2, powerPercent = 50.0, limits = defaultMockLimits),
                FixedFanConfig(channel = 3, powerPercent = 0.0, limits = defaultMockLimits),
            ),
        sensorOffsets =
            listOf(
                SensorOffset(channel = 0, offsetCelsius = 0.0),
                SensorOffset(channel = 1, offsetCelsius = 0.0),
                SensorOffset(channel = 2, offsetCelsius = 0.0),
                SensorOffset(channel = 3, offsetCelsius = 0.0),
            ),
        flowPulsesPerLiter = FeatureState.Active(169),
        display = FeatureState.Unsupported,
        rgbpx =
            listOf(
                RgbpxPortConfig(port = 0, enabled = true, ledCount = 30, brightnessPercent = 100, mode = RgbEffectMode.STATIC),
            ),
    )

fun defaultD5NextConfig(): DeviceConfig =
    DeviceConfig(
        fans =
            listOf(
                FixedFanConfig(channel = 0, powerPercent = 60.0, limits = defaultMockLimits),
                CurveFanConfig(channel = 1, sensorIndex = 0, points = defaultCurvePoints(), limits = defaultMockLimits),
            ),
        sensorOffsets =
            listOf(
                SensorOffset(channel = 0, offsetCelsius = 0.0),
            ),
        flowPulsesPerLiter = FeatureState.Unsupported,
        display = FeatureState.Active(DisplayConfig(brightnessPercent = 80, orientationDegrees = 0)),
        rgbpx =
            listOf(
                RgbpxPortConfig(port = 0, enabled = true, ledCount = 15, brightnessPercent = 100, mode = RgbEffectMode.STATIC),
            ),
    )

fun defaultFarbwerk360Config(): DeviceConfig =
    DeviceConfig(
        fans = emptyList(),
        sensorOffsets =
            listOf(
                SensorOffset(channel = 0, offsetCelsius = 0.0),
                SensorOffset(channel = 1, offsetCelsius = 0.0),
                SensorOffset(channel = 2, offsetCelsius = 0.0),
                SensorOffset(channel = 3, offsetCelsius = 0.0),
            ),
        flowPulsesPerLiter = FeatureState.Disconnected,
        display = FeatureState.Unsupported,
        rgbpx =
            listOf(
                RgbpxPortConfig(port = 0, enabled = true, ledCount = 30, brightnessPercent = 100, mode = RgbEffectMode.STATIC),
                RgbpxPortConfig(port = 1, enabled = true, ledCount = 30, brightnessPercent = 100, mode = RgbEffectMode.STATIC),
                RgbpxPortConfig(port = 2, enabled = true, ledCount = 30, brightnessPercent = 100, mode = RgbEffectMode.STATIC),
                RgbpxPortConfig(port = 3, enabled = true, ledCount = 30, brightnessPercent = 100, mode = RgbEffectMode.STATIC),
            ),
    )

fun defaultFarbwerkConfig(): DeviceConfig =
    DeviceConfig(
        fans = emptyList(),
        sensorOffsets =
            listOf(
                SensorOffset(channel = 0, offsetCelsius = 0.0),
                SensorOffset(channel = 1, offsetCelsius = 0.0),
                SensorOffset(channel = 2, offsetCelsius = 0.0),
                SensorOffset(channel = 3, offsetCelsius = 0.0),
            ),
        flowPulsesPerLiter = FeatureState.Unsupported,
        display = FeatureState.Unsupported,
        rgbpx = emptyList(),
    )

fun defaultHighFlowNextConfig(): DeviceConfig =
    DeviceConfig(
        fans = emptyList(),
        sensorOffsets =
            listOf(
                SensorOffset(channel = 0, offsetCelsius = 0.0),
            ),
        flowPulsesPerLiter = FeatureState.Active(169),
        display = FeatureState.Active(DisplayConfig(brightnessPercent = 90, orientationDegrees = 0)),
        rgbpx =
            listOf(
                RgbpxPortConfig(port = 0, enabled = true, ledCount = 10, brightnessPercent = 100, mode = RgbEffectMode.STATIC),
            ),
    )

fun defaultLeakshieldConfig(): DeviceConfig =
    DeviceConfig(
        fans = emptyList(),
        sensorOffsets = emptyList(),
        flowPulsesPerLiter = FeatureState.Unsupported,
        display = FeatureState.Active(DisplayConfig(brightnessPercent = 85, orientationDegrees = 0)),
        rgbpx =
            listOf(
                RgbpxPortConfig(port = 0, enabled = true, ledCount = 6, brightnessPercent = 100, mode = RgbEffectMode.STATIC),
            ),
    )

fun defaultAquastreamUltimateConfig(): DeviceConfig =
    DeviceConfig(
        fans =
            listOf(
                FixedFanConfig(channel = 0, powerPercent = 70.0, limits = defaultMockLimits),
                FixedFanConfig(channel = 1, powerPercent = 50.0, limits = defaultMockLimits),
            ),
        sensorOffsets =
            listOf(
                SensorOffset(channel = 0, offsetCelsius = 0.0),
                SensorOffset(channel = 1, offsetCelsius = 0.0),
            ),
        flowPulsesPerLiter = FeatureState.Unsupported,
        display = FeatureState.Active(DisplayConfig(brightnessPercent = 80, orientationDegrees = 0)),
        rgbpx =
            listOf(
                RgbpxPortConfig(port = 0, enabled = true, ledCount = 15, brightnessPercent = 100, mode = RgbEffectMode.STATIC),
            ),
    )

fun defaultAquaeroConfig(): DeviceConfig =
    DeviceConfig(
        fans =
            listOf(
                CurveFanConfig(channel = 0, sensorIndex = 0, points = defaultCurvePoints(), limits = defaultMockLimits),
                FixedFanConfig(channel = 1, powerPercent = 50.0, limits = defaultMockLimits),
                FixedFanConfig(channel = 2, powerPercent = 50.0, limits = defaultMockLimits),
                FixedFanConfig(channel = 3, powerPercent = 50.0, limits = defaultMockLimits),
            ),
        sensorOffsets =
            (0..7).map { SensorOffset(channel = it, offsetCelsius = 0.0) },
        flowPulsesPerLiter = FeatureState.Active(169),
        display = FeatureState.Active(DisplayConfig(brightnessPercent = 100, orientationDegrees = 0)),
        rgbpx = emptyList(),
    )

fun defaultHighFlowUsbConfig(): DeviceConfig =
    DeviceConfig(
        fans = emptyList(),
        sensorOffsets =
            listOf(
                SensorOffset(channel = 0, offsetCelsius = 0.0),
                SensorOffset(channel = 1, offsetCelsius = 0.0),
            ),
        flowPulsesPerLiter = FeatureState.Active(169),
        display = FeatureState.Unsupported,
        rgbpx = emptyList(),
    )

fun defaultPoweradjust3Config(): DeviceConfig =
    DeviceConfig(
        fans =
            listOf(
                FixedFanConfig(channel = 0, powerPercent = 75.0, limits = defaultMockLimits),
            ),
        sensorOffsets =
            listOf(
                SensorOffset(channel = 0, offsetCelsius = 0.0),
            ),
        flowPulsesPerLiter = FeatureState.Disconnected,
        display = FeatureState.Unsupported,
        rgbpx = emptyList(),
    )

fun defaultAquastreamXtConfig(): DeviceConfig =
    DeviceConfig(
        fans =
            listOf(
                FixedFanConfig(channel = 0, powerPercent = 65.0, limits = defaultMockLimits),
                FixedFanConfig(channel = 1, powerPercent = 45.0, limits = defaultMockLimits),
            ),
        sensorOffsets =
            listOf(
                SensorOffset(channel = 0, offsetCelsius = 0.0),
                SensorOffset(channel = 1, offsetCelsius = 0.0),
            ),
        flowPulsesPerLiter = FeatureState.Disconnected,
        display = FeatureState.Unsupported,
        rgbpx = emptyList(),
    )

fun defaultMockStorage(): MockConfigStorage =
    MockConfigStorage(
        devices =
            listOf(
                DetectedHardwareDevice(
                    id = DeviceId.from("mock_octo_0"),
                    alias = DeviceAlias.from("octo-1"),
                    type = DeviceType.OCTO,
                    name = DeviceRegistry.OCTO.name,
                    serial = "13235-54067-MOCK",
                    firmwareVersion = 1020,
                ),
                DetectedHardwareDevice(
                    id = DeviceId.from("mock_quadro_0"),
                    alias = DeviceAlias.from("quadro-1"),
                    type = DeviceType.QUADRO,
                    name = DeviceRegistry.QUADRO.name,
                    serial = "13236-54068-MOCK",
                    firmwareVersion = 1018,
                ),
                DetectedHardwareDevice(
                    id = DeviceId.from("mock_d5next_0"),
                    alias = DeviceAlias.from("d5next-1"),
                    type = DeviceType.D5_NEXT,
                    name = DeviceRegistry.D5_NEXT.name,
                    serial = "13237-54069-MOCK",
                    firmwareVersion = 1015,
                ),
                DetectedHardwareDevice(
                    id = DeviceId.from("mock_farbwerk360_0"),
                    alias = DeviceAlias.from("farbwerk360-1"),
                    type = DeviceType.FARBWERK360,
                    name = DeviceRegistry.FARBWERK360.name,
                    serial = "13238-54070-MOCK",
                    firmwareVersion = 1024,
                ),
                DetectedHardwareDevice(
                    id = DeviceId.from("mock_farbwerk_0"),
                    alias = DeviceAlias.from("farbwerk-1"),
                    type = DeviceType.FARBWERK,
                    name = DeviceRegistry.FARBWERK.name,
                    serial = "13239-54071-MOCK",
                    firmwareVersion = 1011,
                ),
                DetectedHardwareDevice(
                    id = DeviceId.from("mock_highflownext_0"),
                    alias = DeviceAlias.from("highflownext-1"),
                    type = DeviceType.HIGH_FLOW_NEXT,
                    name = DeviceRegistry.HIGH_FLOW_NEXT.name,
                    serial = "13240-54072-MOCK",
                    firmwareVersion = 1010,
                ),
                DetectedHardwareDevice(
                    id = DeviceId.from("mock_leakshield_0"),
                    alias = DeviceAlias.from("leakshield-1"),
                    type = DeviceType.LEAKSHIELD,
                    name = DeviceRegistry.LEAKSHIELD.name,
                    serial = "13241-54073-MOCK",
                    firmwareVersion = 1008,
                ),
                DetectedHardwareDevice(
                    id = DeviceId.from("mock_aquastream_ultimate_0"),
                    alias = DeviceAlias.from("aquastream-ultimate-1"),
                    type = DeviceType.AQUASTREAM_ULTIMATE,
                    name = DeviceRegistry.AQUASTREAM_ULTIMATE.name,
                    serial = "13242-54074-MOCK",
                    firmwareVersion = 1012,
                ),
                DetectedHardwareDevice(
                    id = DeviceId.from("mock_aquaero_0"),
                    alias = DeviceAlias.from("aquaero-1"),
                    type = DeviceType.AQUAERO,
                    name = DeviceRegistry.AQUAERO.name,
                    serial = "13243-54075-MOCK",
                    firmwareVersion = 2100,
                ),
                DetectedHardwareDevice(
                    id = DeviceId.from("mock_highflowusb_0"),
                    alias = DeviceAlias.from("highflowusb-1"),
                    type = DeviceType.HIGH_FLOW_USB,
                    name = DeviceRegistry.HIGH_FLOW_USB.name,
                    serial = "13244-54076-MOCK",
                    firmwareVersion = 1005,
                ),
                DetectedHardwareDevice(
                    id = DeviceId.from("mock_poweradjust3_0"),
                    alias = DeviceAlias.from("poweradjust3-1"),
                    type = DeviceType.POWERADJUST3,
                    name = DeviceRegistry.POWERADJUST3.name,
                    serial = "13245-54077-MOCK",
                    firmwareVersion = 1007,
                ),
                DetectedHardwareDevice(
                    id = DeviceId.from("mock_aquastream_xt_0"),
                    alias = DeviceAlias.from("aquastream-xt-1"),
                    type = DeviceType.AQUASTREAM_XT,
                    name = DeviceRegistry.AQUASTREAM_XT.name,
                    serial = "13246-54078-MOCK",
                    firmwareVersion = 1017,
                ),
                DetectedHardwareDevice(
                    id = DeviceId.from("mock_broken_octo_0"),
                    alias = DeviceAlias.from("broken-octo-1"),
                    type = DeviceType.OCTO,
                    name = "Broken OCTO",
                    serial = "13235-54067-BROKEN",
                    firmwareVersion = 1020,
                ),
            ),
        configs =
            mapOf(
                "octo-1" to defaultOctoConfig(),
                "quadro-1" to defaultQuadroConfig(),
                "d5next-1" to defaultD5NextConfig(),
                "farbwerk360-1" to defaultFarbwerk360Config(),
                "farbwerk-1" to defaultFarbwerkConfig(),
                "highflownext-1" to defaultHighFlowNextConfig(),
                "leakshield-1" to defaultLeakshieldConfig(),
                "aquastream-ultimate-1" to defaultAquastreamUltimateConfig(),
                "aquaero-1" to defaultAquaeroConfig(),
                "highflowusb-1" to defaultHighFlowUsbConfig(),
                "poweradjust3-1" to defaultPoweradjust3Config(),
                "aquastream-xt-1" to defaultAquastreamXtConfig(),
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
        "farbwerk360-1" to (4..19).associateWith { if (it == 4) 29.5 else null },
        "aquaero-1" to (8..15).associateWith { if (it == 8) 31.0 else null },
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
    val text = mockJson.encodeToString(storage)
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
