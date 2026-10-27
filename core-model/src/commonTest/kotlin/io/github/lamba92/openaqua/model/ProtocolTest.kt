package io.github.lamba92.openaqua.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProtocolTest {
    @Test
    fun testFeatureState() {
        val active: FeatureState<Int> = FeatureState.Active(42)
        assertTrue(active is FeatureState.Active)
        assertEquals(42, active.value)
        assertEquals(42, active.valueOrNull)

        val disconnected: FeatureState<Int> = FeatureState.Disconnected
        assertTrue(disconnected is FeatureState.Disconnected)
        assertEquals(null, disconnected.valueOrNull)

        val unsupported: FeatureState<Int> = FeatureState.Unsupported
        assertTrue(unsupported is FeatureState.Unsupported)
        assertEquals(null, unsupported.valueOrNull)
    }

    @Test
    fun testAutoCurveFanConfigValidationAndSerialization() {
        val valid =
            AutoCurveFanConfig(
                channel = 1,
                sensorIndex = 0,
                tMin = 30.0,
                tMax = 45.0,
                pMin = 25.0,
                pMax = 100.0,
                shape = 7.0,
            )
        assertEquals(1, valid.channel)
        val curveConfig = valid.toCurveFanConfig()
        assertEquals(16, curveConfig.points.size)
        assertEquals(30.0, curveConfig.points.first().temp)
        assertEquals(25.0, curveConfig.points.first().pwm)
        assertEquals(45.0, curveConfig.points.last().temp)
        assertEquals(100.0, curveConfig.points.last().pwm)

        assertFailsWith<IllegalArgumentException> {
            AutoCurveFanConfig(
                channel = 0,
                sensorIndex = 0,
                tMin = 30.0,
                tMax = 50.0,
                pMin = 20.0,
                pMax = 50.0,
                shape = 11.0,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            AutoCurveFanConfig(
                channel = 0,
                sensorIndex = 0,
                tMin = 50.0,
                tMax = 30.0,
                pMin = 20.0,
                pMax = 50.0,
            )
        }

        assertFailsWith<IllegalArgumentException> {
            AutoCurveFanConfig(
                channel = 0,
                sensorIndex = 0,
                tMin = 30.0,
                tMax = 50.0,
                pMin = -5.0,
                pMax = 50.0,
            )
        }

        val json = Json { encodeDefaults = true }
        val flashReq =
            FlashConfigRequest(
                fans = mapOf(1 to valid),
                flowPulsesPerLiter = 450,
                display = DisplayConfig(brightnessPercent = 80),
            )
        val serialized = json.encodeToString(FlashConfigRequest.serializer(), flashReq)
        assertTrue(serialized.contains(""""type":"auto-curve""""))
        assertTrue(serialized.contains(""""shape":7.0"""))
        assertTrue(serialized.contains(""""flowPulsesPerLiter":450"""))
        assertTrue(serialized.contains(""""display":{"brightnessPercent":80,"orientationDegrees":0}"""))

        val deserialized = json.decodeFromString(FlashConfigRequest.serializer(), serialized)
        val fan1 = deserialized.fans?.get(1)
        assertTrue(fan1 is AutoCurveFanConfig)
        assertEquals(7.0, fan1.shape)
        assertEquals(30.0, fan1.tMin)
        assertEquals(45.0, fan1.tMax)
        assertEquals(450, deserialized.flowPulsesPerLiter)
        assertEquals(80, deserialized.display?.brightnessPercent)
    }

    @Test
    fun testRgbpxValidation() {
        val validRgb =
            FlashConfigRequest(
                rgbpx =
                    mapOf(
                        0 to RgbpxPortConfig(port = 0, ledCount = 30, brightnessPercent = 80, mode = RgbEffectMode.STATIC),
                        1 to RgbpxPortConfig(port = 1, ledCount = 60, brightnessPercent = 100, mode = RgbEffectMode.RAINBOW),
                    ),
            )
        assertNotNull(validRgb.rgbpx)
        assertEquals(2, validRgb.rgbpx.size)

        assertFailsWith<IllegalArgumentException> {
            RgbpxPortConfig(port = 0, ledCount = 91, brightnessPercent = 50)
        }

        assertFailsWith<IllegalArgumentException> {
            RgbpxPortConfig(port = 0, ledCount = 30, brightnessPercent = 101)
        }

        assertFailsWith<IllegalArgumentException> {
            RgbColor(r = 256, g = 0, b = 0)
        }
    }

    @Test
    fun testDeviceConfigSerialization() {
        val json = Json { encodeDefaults = true }
        val fans = List(8) { FixedFanConfig(it, 50.0) }
        val offsets = List(4) { SensorOffset(it, 0.0) }
        val rgbpx =
            listOf(
                RgbpxPortConfig(
                    port = 0,
                    enabled = true,
                    ledCount = 30,
                    brightnessPercent = 80,
                    mode = RgbEffectMode.STATIC,
                    primaryColor = RgbColor(255, 128, 0),
                ),
            )
        val config =
            DeviceConfig(
                fans = fans,
                sensorOffsets = offsets,
                rgbpx = rgbpx,
                flowPulsesPerLiter = FeatureState.Active(169),
                display = FeatureState.Unsupported,
            )

        val serialized = json.encodeToString(DeviceConfig.serializer(), config)
        assertTrue(serialized.contains(""""rgbpx""""))
        assertTrue(serialized.contains(""""primaryColor":{"r":255,"g":128,"b":0}"""))
        assertTrue(serialized.contains(""""flowPulsesPerLiter":{"type":"active","value":169}"""))
        assertTrue(serialized.contains(""""display":{"type":"unsupported"}"""))

        val deserialized = json.decodeFromString(DeviceConfig.serializer(), serialized)
        assertEquals(8, deserialized.fans.size)
        assertEquals(4, deserialized.sensorOffsets.size)
        assertEquals(1, deserialized.rgbpx.size)
        assertEquals(FeatureState.Active(169), deserialized.flowPulsesPerLiter)
        assertEquals(FeatureState.Unsupported, deserialized.display)
        assertEquals(RgbEffectMode.STATIC, deserialized.rgbpx[0].mode)
        assertEquals(RgbColor(255, 128, 0), deserialized.rgbpx[0].primaryColor)
    }

    @Test
    fun testDeviceTelemetrySerialization() {
        val json = Json { encodeDefaults = true }
        val fans =
            listOf(
                CoolingChannelTelemetry.Connected(
                    channel = 0,
                    rpm = 2000,
                    powerWatts = 5.0,
                    voltageVolts = 12.0,
                    currentAmps = 0.4,
                    pwmPercent = 50.0,
                    kind = CoolingChannelKind.PUMP,
                ),
                CoolingChannelTelemetry.Disconnected(
                    channel = 1,
                    kind = CoolingChannelKind.FAN,
                ),
            )
        val sensors =
            listOf(
                SensorTelemetry.Connected(
                    channel = 0,
                    tempCelsius = 25.5,
                    kind = SensorChannelKind.INTEGRATED_COOLANT,
                ),
                SensorTelemetry.Disconnected(
                    channel = 1,
                    kind = SensorChannelKind.VIRTUAL,
                ),
            )
        val telemetry =
            DeviceTelemetry(
                fans = fans,
                sensors = sensors,
                flowRateDlH = FeatureState.Active(200),
                display = FeatureState.Unsupported,
            )

        val serialized = json.encodeToString(DeviceTelemetry.serializer(), telemetry)
        assertTrue(serialized.contains(""""flowRateDlH":{"type":"active","value":200}"""))
        assertTrue(serialized.contains(""""kind":"PUMP""""))
        assertTrue(serialized.contains(""""kind":"INTEGRATED_COOLANT""""))

        val deserialized = json.decodeFromString(DeviceTelemetry.serializer(), serialized)
        assertEquals(FeatureState.Active(200), deserialized.flowRateDlH)
        assertEquals(2, deserialized.fans.size)
        val fan0 = deserialized.fans[0]
        assertTrue(fan0 is CoolingChannelTelemetry.Connected)
        assertEquals(2000, fan0.rpm)
        assertEquals(CoolingChannelKind.PUMP, fan0.kind)
        val fan1 = deserialized.fans[1]
        assertTrue(fan1 is CoolingChannelTelemetry.Disconnected)
        assertEquals(CoolingChannelKind.FAN, fan1.kind)

        val sensor0 = deserialized.sensors[0]
        assertTrue(sensor0 is SensorTelemetry.Connected)
        assertEquals(25.5, sensor0.tempCelsius)
        assertEquals(SensorChannelKind.INTEGRATED_COOLANT, sensor0.kind)
        val sensor1 = deserialized.sensors[1]
        assertTrue(sensor1 is SensorTelemetry.Disconnected)
        assertEquals(SensorChannelKind.VIRTUAL, sensor1.kind)
    }

    @Test
    fun testFeedVirtualSensorsRequestSerialization() {
        val json = Json { encodeDefaults = true }
        val request = FeedVirtualSensorsRequest(sensors = mapOf(4 to 36.5, 5 to 48.0))

        val serialized = json.encodeToString(FeedVirtualSensorsRequest.serializer(), request)
        assertTrue(serialized.contains(""""4":36.5"""))
        assertTrue(serialized.contains(""""5":48.0"""))

        val deserialized = json.decodeFromString(FeedVirtualSensorsRequest.serializer(), serialized)
        assertEquals(2, deserialized.sensors.size)
        assertEquals(36.5, deserialized.sensors[4])
        assertEquals(48.0, deserialized.sensors[5])
    }

    @Test
    fun testEngineResponseSerialization() {
        val json = Json { encodeDefaults = true }

        val devices =
            listOf(
                DetectedHardwareDevice(
                    id = DeviceId.from("octo-1"),
                    alias = DeviceAlias.from("octo-1"),
                    type = DeviceType.OCTO,
                    name = "Aqua Computer OCTO",
                ),
            )
        val successResponse: EngineResponse<DevicesResponse> =
            EngineResponse.Success(DevicesResponse(devices))
        val serializedSuccess = json.encodeToString(successResponse)
        assertTrue(serializedSuccess.contains(""""type":"success""""))
        assertTrue(serializedSuccess.contains(""""id":"octo-1""""))

        val deserializedSuccess = json.decodeFromString<EngineResponse<DevicesResponse>>(serializedSuccess)
        assertTrue(deserializedSuccess is EngineResponse.Success)
        assertEquals(1, deserializedSuccess.value.devices.size)
        assertEquals(
            DeviceId.from("octo-1"),
            deserializedSuccess.value.devices
                .first()
                .id,
        )

        val errorResponse: EngineResponse<Nothing> = EngineResponse.Error("Device not found")
        val serializedError = json.encodeToString(errorResponse)
        assertTrue(serializedError.contains(""""type":"error""""))
        assertTrue(serializedError.contains(""""message":"Device not found""""))

        val deserializedError = json.decodeFromString<EngineResponse<DevicesResponse>>(serializedError)
        assertTrue(deserializedError is EngineResponse.Error)
        assertEquals("Device not found", deserializedError.message)
    }
}
