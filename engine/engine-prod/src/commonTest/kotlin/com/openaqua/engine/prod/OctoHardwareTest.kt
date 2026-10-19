package com.openaqua.engine.prod

import com.openaqua.engine.hardware.HardwareController
import com.openaqua.model.AutoCurveParams
import com.openaqua.model.CurveFanConfig
import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.DeviceType
import com.openaqua.model.FanChannelConfig
import com.openaqua.model.FixedFanConfig
import com.openaqua.model.FlashOctoConfig
import com.openaqua.model.OctoAllowedSpecs
import com.openaqua.model.OctoConfig
import com.openaqua.model.OctoTelemetry
import com.openaqua.model.TargetTempFanConfig
import com.openaqua.model.TwoPointFanConfig
import com.openaqua.model.isFanAllowed
import com.openaqua.model.isSensorOffsetAllowed
import com.openaqua.model.isVirtualSensorAllowed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end hardware tests tailored specifically for lamba92's workstation environment
 * featuring a physical Aquacomputer OCTO controller with:
 *  - Fans 1-4 actively connected/occupied (channels 0..3) -> Protected from writing
 *  - Sensor 1 actively connected/occupied (channel 0) -> Protected from offset modification
 *  - Fans 5-8 unoccupied (channels 4..7) -> Allowed for flash tests
 *  - Virtual Sensors 5-19 -> Allowed for feeding tests
 *  - RGBpx: Flashing is not yet tested here as report byte offsets and encoding for RGBpx
 *    hardware controller memory are pending implementation in ConfigWriter.
 *
 * NOTE: These tests interact with real physical USB hardware and are excluded by default
 * from `./gradlew check` and `./gradlew test`. To run them explicitly, pass `-Ptest.hardware=true`
 * (e.g. `./gradlew.bat :engine:engine-prod:mingwX64Test -Ptest.hardware=true` or `./gradlew :engine:engine-prod:linuxX64Test -Ptest.hardware=true`).
 */
class OctoHardwareTest {
    private val controller = getPlatformHardwareController()

    /**
     * Declares which components on lamba92's OCTO are allowed to be modified during tests.
     * Fans 1-4 (channels 0..3) and Sensor 1 (channel 0) are omitted/false to protect active hardware.
     */
    private val lamba92sOctoAllowedSpec =
        OctoAllowedSpecs(
            fans =
                mapOf(
                    0 to false,
                    1 to false,
                    2 to false,
                    3 to false,
                    4 to true, // Fan 5 (unoccupied)
                    5 to true, // Fan 6 (unoccupied)
                    6 to true, // Fan 7 (unoccupied)
                    7 to true, // Fan 8 (unoccupied)
                ),
            sensorOffsets =
                mapOf(
                    0 to false,
                    1 to true, // Sensor 2 (unoccupied)
                    2 to true, // Sensor 3 (unoccupied)
                    3 to true, // Sensor 4 (unoccupied)
                ),
            virtualSensors = (0..19).associateWith { true },
            // RGBpx ports 0..1 will be enabled here once RGBpx encoding is added to ConfigWriter
            rgbpx = emptyMap(),
        )

    private class OctoTestScope(
        val controller: HardwareController,
        val octo: DetectedHardwareDevice,
        val allowedSpecs: OctoAllowedSpecs,
        val initialConfig: OctoConfig,
    ) {
        fun flashAndVerify(request: FlashOctoConfig): OctoConfig {
            request.fans?.keys?.forEach { channel ->
                require(allowedSpecs.isFanAllowed(channel)) {
                    "Safety check failed: Fan channel $channel is not allowed in AllowedSpecs"
                }
            }
            request.sensorOffsets?.keys?.forEach { channel ->
                require(allowedSpecs.isSensorOffsetAllowed(channel)) {
                    "Safety check failed: Sensor channel $channel is not allowed in AllowedSpecs"
                }
            }
            return controller.flashConfig(octo, request) as OctoConfig
        }

        fun feedVirtualSensors(sensors: Map<Int, Double>) {
            sensors.keys.forEach { channel ->
                require(allowedSpecs.isVirtualSensorAllowed(channel)) {
                    "Safety check failed: Virtual sensor channel $channel is not allowed in AllowedSpecs"
                }
            }
            controller.feedVirtualSensors(octo, sensors)
        }
    }

    private fun withOctoSafeScope(block: OctoTestScope.() -> Unit) {
        val octo =
            controller.scanDevices().firstOrNull { it.type == DeviceType.OCTO }
                ?: error("OCTO not found")
        val initialConfig = controller.readConfig(octo) as OctoConfig
        val scope = OctoTestScope(controller, octo, lamba92sOctoAllowedSpec, initialConfig)

        try {
            scope.block()
        } finally {
            println("Safety Scope: Restoring initial OCTO configuration for allowed test channels...")
            val restoreRequest =
                FlashOctoConfig(
                    fans =
                        initialConfig.fans
                            .filter { lamba92sOctoAllowedSpec.isFanAllowed(it.channel) }
                            .associate { it.channel to it.toSafeRestorableConfig() },
                    sensorOffsets =
                        initialConfig.sensorOffsets
                            .filter { lamba92sOctoAllowedSpec.isSensorOffsetAllowed(it.channel) }
                            .associate { it.channel to it.offsetCelsius },
                )
            controller.flashConfig(octo, restoreRequest)
            println("Safety Scope: Successfully restored original OCTO configuration")
        }
    }

    @Test
    fun testOctoDetection() {
        val devices = controller.scanDevices()
        val octo = devices.firstOrNull { it.type == DeviceType.OCTO }
        assertNotNull(octo, "Aquacomputer OCTO must be detected")
        println("Detected OCTO: id=${octo.id}, serial=${octo.serial}, fw=${octo.firmwareVersion}")

        assertNotNull(octo.serial, "OCTO serial number string should be readable")
        val fwVersion = octo.firmwareVersion
        assertNotNull(fwVersion, "OCTO firmware version should be readable")
        assertTrue(fwVersion > 0, "OCTO firmware version should be a positive integer")
    }

    @Test
    fun testTelemetryReadsOccupiedFansAndSensors() =
        runBlocking {
            val octo =
                controller.scanDevices().firstOrNull { it.type == DeviceType.OCTO }
                    ?: error("OCTO not found")

            val telemetry = controller.streamTelemetry(octo).first()
            assertTrue(telemetry is OctoTelemetry, "Telemetry must be OctoTelemetry")

            println("Telemetry received with ${telemetry.fans.size} fans and ${telemetry.sensors.size} sensors")

            // Fans 1-4 are occupied (indices 0..3)
            for (channel in 0..3) {
                val fan = telemetry.fans.firstOrNull { it.channel == channel }
                assertNotNull(fan, "Fan ${channel + 1} (channel $channel) must be present in telemetry")
                println(
                    "Fan ${channel + 1}: rpm=${fan.rpm}, voltage=${fan.voltageVolts}V, " +
                        "current=${fan.currentAmps}A, power=${fan.powerWatts}W",
                )
                assertTrue(fan.voltageVolts > 0.0, "Fan ${channel + 1} voltage should be > 0V")
            }

            // Sensor 1 is occupied (channel 0)
            val sensor1 = telemetry.sensors.firstOrNull { it.channel == 0 }
            assertNotNull(sensor1, "Sensor 1 (channel 0) must be present in telemetry")
            val temp = sensor1.tempCelsius
            assertNotNull(temp, "Sensor 1 temperature must not be null")
            println("Sensor 1 temp: $temp°C")
            assertTrue(
                temp in 5.0..80.0,
                "Sensor 1 temperature ($temp°C) should be in realistic range",
            )
        }

    @Test
    fun testFlashFixedPowerOnAllowedFans() =
        withOctoSafeScope {
            val testChannel = 4 // Fan 5
            val testTargetPower = 35.0
            val testFixedConfig = FixedFanConfig(channel = testChannel, powerPercent = testTargetPower)
            val testRequest = FlashOctoConfig(fans = mapOf(testChannel to testFixedConfig))

            println("Testing Fixed Power on allowed Fan ${testChannel + 1} ($testTargetPower%)...")
            val readback = flashAndVerify(testRequest)
            val updated = readback.fans[testChannel]
            assertTrue(updated is FixedFanConfig)
            assertEquals(testTargetPower, updated.powerPercent, 0.5)
            println("Verified Fixed Power flashed and read back successfully")
        }

    @Test
    fun testFlashCurveOnAllowedFan() =
        withOctoSafeScope {
            val testChannel = 5 // Fan 6
            val curvePoints =
                AutoCurveParams(
                    tMin = 20.0,
                    tMax = 50.0,
                    pMin = 30.0,
                    pMax = 90.0,
                    shape = 0.5,
                ).toPoints()

            val testCurveConfig =
                CurveFanConfig(
                    channel = testChannel,
                    sensorIndex = 0, // Bound to Sensor 1 for reference
                    points = curvePoints,
                )
            val testRequest = FlashOctoConfig(fans = mapOf(testChannel to testCurveConfig))

            println("Testing 16-point Curve on allowed Fan ${testChannel + 1}...")
            val readback = flashAndVerify(testRequest)
            val updated = readback.fans[testChannel]
            assertTrue(updated is CurveFanConfig)
            assertEquals(0, updated.sensorIndex)
            assertEquals(16, updated.points.size)
            assertEquals(30.0, updated.points.first().pwm, 0.5)
            assertEquals(90.0, updated.points.last().pwm, 0.5)
            println("Verified 16-point Curve flashed and read back successfully")
        }

    @Test
    fun testFlashSensorOffsetOnAllowedSensor() =
        withOctoSafeScope {
            val testSensorChannel = 1 // Sensor 2 (unoccupied)
            val testOffset = 2.5
            println("Testing Sensor Offset on allowed Sensor ${testSensorChannel + 1} ($testOffset°C)...")
            val testRequest = FlashOctoConfig(sensorOffsets = mapOf(testSensorChannel to testOffset))
            val readback = flashAndVerify(testRequest)
            val updated = readback.sensorOffsets.first { it.channel == testSensorChannel }
            assertEquals(testOffset, updated.offsetCelsius, 0.2)
            println("Verified Sensor Offset flashed and read back successfully")
        }

    /**
     * NOTE: Virtual sensor streaming writes Report ID 0x04, which on OCTO is transmitted over
     * USB Bulk OUT Endpoint 0x02 on Interface 0 rather than the HIDraw Interface 1.
     * Pending implementation of USB Bulk OUT routing.
     */
    @Ignore
    @Test
    fun testFeedVirtualSensorsOnAllowedChannels() =
        withOctoSafeScope {
            val testChannel = 5 // Virtual sensor slot 2
            val testTemp = 28.5
            println("Feeding virtual sensor channel $testChannel with $testTemp°C...")
            feedVirtualSensors(mapOf(testChannel to testTemp))
            println("Virtual sensor packet sent successfully")
        }

    companion object {
        private fun FanChannelConfig.toSafeRestorableConfig(): FanChannelConfig =
            when (this) {
                is FixedFanConfig -> this
                is CurveFanConfig -> this
                is TargetTempFanConfig -> this
                is TwoPointFanConfig -> this
                else -> FixedFanConfig(channel = channel, powerPercent = 50.0)
            }
    }
}
