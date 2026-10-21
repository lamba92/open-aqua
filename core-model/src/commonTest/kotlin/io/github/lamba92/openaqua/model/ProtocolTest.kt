package io.github.lamba92.openaqua.model

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProtocolTest {
    private val strictJson =
        Json {
            ignoreUnknownKeys = false
        }

    @Test
    fun testFlashOctoConfigValidation() {
        // Channel 7 is valid
        val validReq =
            FlashOctoConfig(
                fans = mapOf(7 to FixedFanConfig(channel = 7, powerPercent = 100.0)),
            )
        assertNotNull(validReq.fans)

        // Channel 8 is invalid on Octo (must be 0..7)
        assertFailsWith<IllegalArgumentException> {
            FlashOctoConfig(
                fans = mapOf(8 to FixedFanConfig(channel = 8, powerPercent = 100.0)),
            )
        }

        // Sensor 4 is invalid on Octo (must be 0..3)
        assertFailsWith<IllegalArgumentException> {
            FlashOctoConfig(
                sensorOffsets = mapOf(4 to 1.5),
            )
        }
    }

    @Test
    fun testAutoCurveFanConfigValidationAndSerialization() {
        // Valid auto-curve config
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

        // Invalid shape (out of range -10..10)
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

        // Invalid temperature range (tMin >= tMax)
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

        // Invalid PWM range
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

        // Serialization in FlashOctoConfig
        val json = Json { encodeDefaults = true }
        val flashReq =
            FlashOctoConfig(
                fans = mapOf(1 to valid),
            )
        val serialized = json.encodeToString(FlashConfigRequest.serializer(), flashReq)
        assertTrue(serialized.contains(""""type":"auto-curve""""))
        assertTrue(serialized.contains(""""shape":7.0"""))

        val deserialized = json.decodeFromString(FlashConfigRequest.serializer(), serialized)
        assertTrue(deserialized is FlashOctoConfig)
        val fan1 = deserialized.fans?.get(1)
        assertTrue(fan1 is AutoCurveFanConfig)
        assertEquals(7.0, fan1.shape)
        assertEquals(30.0, fan1.tMin)
        assertEquals(45.0, fan1.tMax)
    }

    @Test
    fun testFlashQuadroConfigValidation() {
        // Channel 3 is valid on Quadro
        val validReq =
            FlashQuadroConfig(
                fans = mapOf(3 to FixedFanConfig(channel = 3, powerPercent = 100.0)),
            )
        assertNotNull(validReq.fans)

        // Channel 4 is invalid on Quadro (must be 0..3)
        assertFailsWith<IllegalArgumentException> {
            FlashQuadroConfig(
                fans = mapOf(4 to FixedFanConfig(channel = 4, powerPercent = 100.0)),
            )
        }
    }

    @Test
    fun testFlashRgbpxValidation() {
        // Valid OCTO RGBpx ports 0 and 1
        val validOctoRgb =
            FlashOctoConfig(
                rgbpx =
                    mapOf(
                        0 to RgbpxPortConfig(port = 0, ledCount = 30, brightnessPercent = 80, mode = RgbEffectMode.STATIC),
                        1 to RgbpxPortConfig(port = 1, ledCount = 60, brightnessPercent = 100, mode = RgbEffectMode.RAINBOW),
                    ),
            )
        assertNotNull(validOctoRgb.rgbpx)
        assertEquals(2, validOctoRgb.rgbpx.size)

        // Port 2 is invalid on OCTO (must be 0..1)
        assertFailsWith<IllegalArgumentException> {
            FlashOctoConfig(
                rgbpx = mapOf(2 to RgbpxPortConfig(port = 2, ledCount = 30, brightnessPercent = 80)),
            )
        }

        // Valid QUADRO RGBpx port 0
        val validQuadroRgb =
            FlashQuadroConfig(
                rgbpx = mapOf(0 to RgbpxPortConfig(port = 0, ledCount = 45, brightnessPercent = 50)),
            )
        assertNotNull(validQuadroRgb.rgbpx)

        // Port 1 is invalid on QUADRO (must be 0)
        assertFailsWith<IllegalArgumentException> {
            FlashQuadroConfig(
                rgbpx = mapOf(1 to RgbpxPortConfig(port = 1, ledCount = 30, brightnessPercent = 80)),
            )
        }

        // Valid D5 NEXT RGBpx port 0
        val validD5Rgb =
            FlashD5NextConfig(
                rgbpx = mapOf(0 to RgbpxPortConfig(port = 0, ledCount = 64, brightnessPercent = 90)),
            )
        assertNotNull(validD5Rgb.rgbpx)

        // Port 1 is invalid on D5 NEXT (must be 0)
        assertFailsWith<IllegalArgumentException> {
            FlashD5NextConfig(
                rgbpx = mapOf(1 to RgbpxPortConfig(port = 1, ledCount = 30, brightnessPercent = 80)),
            )
        }

        // LED count > 90 is invalid
        assertFailsWith<IllegalArgumentException> {
            RgbpxPortConfig(port = 0, ledCount = 91, brightnessPercent = 50)
        }

        // Brightness > 100% is invalid
        assertFailsWith<IllegalArgumentException> {
            RgbpxPortConfig(port = 0, ledCount = 30, brightnessPercent = 101)
        }

        // Color component > 255 is invalid
        assertFailsWith<IllegalArgumentException> {
            RgbColor(r = 256, g = 0, b = 0)
        }
    }

    @Test
    fun testStrictJsonRejectsInvalidDeviceFields() {
        // Attempting to pass 'display' to FlashOctoConfig should fail during strict deserialization
        val jsonWithDisplay = """{"display":{"brightnessPercent":80}}"""
        assertFailsWith<SerializationException> {
            strictJson.decodeFromString(FlashOctoConfig.serializer(), jsonWithDisplay)
        }

        // Attempting to pass 'flowPulsesPerLiter' to FlashOctoConfig should fail
        val jsonWithFlow = """{"flowPulsesPerLiter":150}"""
        assertFailsWith<SerializationException> {
            strictJson.decodeFromString(FlashOctoConfig.serializer(), jsonWithFlow)
        }

        // But flowPulsesPerLiter succeeds on FlashQuadroConfig
        val quadroParsed = strictJson.decodeFromString(FlashQuadroConfig.serializer(), jsonWithFlow)
        assertEquals(150, quadroParsed.flowPulsesPerLiter)
    }

    @Test
    fun testConfigPolymorphicSerialization() {
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
        val report: DeviceConfig = OctoConfig(fans, offsets, rgbpx)

        val serialized = json.encodeToString(DeviceConfig.serializer(), report)
        assertTrue(serialized.contains(""""type":"octo""""))
        assertTrue(serialized.contains(""""rgbpx""""))
        assertTrue(serialized.contains(""""primaryColor":{"r":255,"g":128,"b":0}"""))

        val deserialized = json.decodeFromString(DeviceConfig.serializer(), serialized)
        assertTrue(deserialized is OctoConfig)
        assertEquals(8, deserialized.fans.size)
        assertEquals(4, deserialized.sensorOffsets.size)
        assertEquals(1, deserialized.rgbpx.size)
        assertEquals(RgbEffectMode.STATIC, deserialized.rgbpx[0].mode)
        assertEquals(RgbColor(255, 128, 0), deserialized.rgbpx[0].primaryColor)
    }

    @Test
    fun testTelemetryPolymorphicSerialization() {
        val json = Json { encodeDefaults = true }
        val fans = listOf(FanTelemetry(0, 2000, 5.0, 12.0, 0.4, 50.0))
        val sensors = listOf(SensorTelemetry(0, 25.5))
        val telemetry: DeviceTelemetry = QuadroTelemetry(fans, sensors, flowRateDlH = 200)

        val serialized = json.encodeToString(DeviceTelemetry.serializer(), telemetry)
        assertTrue(serialized.contains(""""type":"quadro""""))
        assertTrue(serialized.contains(""""flowRateDlH":200"""))

        val deserialized = json.decodeFromString(DeviceTelemetry.serializer(), serialized)
        assertTrue(deserialized is QuadroTelemetry)
        assertEquals(200, deserialized.flowRateDlH)
        assertEquals(1, deserialized.fans.size)
    }

    @Test
    fun testD5NextTelemetrySerializationWithVirtualSensors() {
        val json = Json { encodeDefaults = true }
        val pump = FanTelemetry(0, 3000, 10.0, 12.0, 0.8, 60.0)
        val fan = FanTelemetry(1, 1000, 2.0, 12.0, 0.16, 40.0)
        val virtualSensors =
            listOf(
                SensorTelemetry(channel = 1, tempCelsius = 35.5, isVirtual = true),
                SensorTelemetry(channel = 2, tempCelsius = null, isVirtual = true),
            )
        val telemetry: DeviceTelemetry =
            D5NextTelemetry(
                pump = pump,
                fan = fan,
                coolantTempCelsius = 28.4,
                virtualSensors = virtualSensors,
            )

        val serialized = json.encodeToString(DeviceTelemetry.serializer(), telemetry)
        assertTrue(serialized.contains(""""type":"d5next""""))
        assertTrue(serialized.contains(""""virtualSensors""""))

        val deserialized = json.decodeFromString(DeviceTelemetry.serializer(), serialized)
        assertTrue(deserialized is D5NextTelemetry)
        assertEquals(28.4, deserialized.coolantTempCelsius)
        assertEquals(2, deserialized.virtualSensors.size)
        assertTrue(deserialized.virtualSensors[0].isVirtual)
        assertEquals(35.5, deserialized.virtualSensors[0].tempCelsius)
        assertEquals(null, deserialized.virtualSensors[1].tempCelsius)
    }

    @Test
    fun testOctoTelemetrySerializationWithVirtualSensors() {
        val json = Json { encodeDefaults = true }
        val sensors =
            listOf(
                SensorTelemetry(0, 30.0, isVirtual = false),
                SensorTelemetry(4, 42.0, isVirtual = true),
                SensorTelemetry(5, null, isVirtual = true),
            )
        val telemetry: DeviceTelemetry =
            OctoTelemetry(
                fans = emptyList(),
                sensors = sensors,
            )

        val serialized = json.encodeToString(DeviceTelemetry.serializer(), telemetry)
        assertTrue(serialized.contains(""""type":"octo""""))

        val deserialized = json.decodeFromString(DeviceTelemetry.serializer(), serialized)
        assertTrue(deserialized is OctoTelemetry)
        assertEquals(3, deserialized.sensors.size)
        assertEquals(42.0, deserialized.sensors[1].tempCelsius)
        assertTrue(deserialized.sensors[1].isVirtual)
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
                    id = "octo-1",
                    alias = "octo-1",
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
            "octo-1",
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
