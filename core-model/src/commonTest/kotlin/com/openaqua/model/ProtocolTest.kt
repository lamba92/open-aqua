package com.openaqua.model

import kotlinx.serialization.SerializationException
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
        val report: DeviceConfig = OctoConfig(fans, offsets)

        val serialized = json.encodeToString(DeviceConfig.serializer(), report)
        assertTrue(serialized.contains(""""type":"octo""""))

        val deserialized = json.decodeFromString(DeviceConfig.serializer(), serialized)
        assertTrue(deserialized is OctoConfig)
        val octo = deserialized as OctoConfig
        assertEquals(8, octo.fans.size)
        assertEquals(4, octo.sensorOffsets.size)
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
        val quadro = deserialized as QuadroTelemetry
        assertEquals(200, quadro.flowRateDlH)
        assertEquals(1, quadro.fans.size)
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
        val d5 = deserialized as D5NextTelemetry
        assertEquals(28.4, d5.coolantTempCelsius)
        assertEquals(2, d5.virtualSensors.size)
        assertTrue(d5.virtualSensors[0].isVirtual)
        assertEquals(35.5, d5.virtualSensors[0].tempCelsius)
        assertEquals(null, d5.virtualSensors[1].tempCelsius)
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
        val octo = deserialized as OctoTelemetry
        assertEquals(3, octo.sensors.size)
        assertEquals(42.0, octo.sensors[1].tempCelsius)
        assertTrue(octo.sensors[1].isVirtual)
    }
}
