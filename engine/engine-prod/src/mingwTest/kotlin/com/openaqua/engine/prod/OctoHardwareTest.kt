package com.openaqua.engine.prod

import com.openaqua.model.CurveFanConfig
import com.openaqua.model.DeviceType
import com.openaqua.model.FanChannelConfig
import com.openaqua.model.FixedFanConfig
import com.openaqua.model.FlashOctoConfig
import com.openaqua.model.OctoConfig
import com.openaqua.model.OctoTelemetry
import com.openaqua.model.TargetTempFanConfig
import com.openaqua.model.TwoPointFanConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end hardware tests tailored specifically for lamba92's workstation environment
 * featuring a physical Aquacomputer OCTO controller with:
 *  - Fans 1-4 actively connected/occupied (channels 0..3)
 *  - Sensor 1 actively connected/occupied (channel 0)
 *  - Fan 5 unoccupied (channel 4), used as safe flash verification target
 *
 * NOTE: These tests interact with real physical USB hardware and are excluded by default
 * from `./gradlew check` and `./gradlew test`. To run them explicitly, pass `-Ptest.hardware=true`
 * (e.g. `./gradlew.bat :engine:engine-prod:mingwX64Test -Ptest.hardware=true`).
 */
class OctoHardwareTest {
    private val controller = WindowsHardwareController()

    @Test
    fun testOctoDetection() {
        val devices = controller.scanDevices()
        val octo = devices.firstOrNull { it.type == DeviceType.OCTO }
        assertNotNull(octo, "Aquacomputer OCTO must be detected on Windows")
        println("Detected OCTO: id=${octo.id}, serial=${octo.serial}, fw=${octo.firmwareVersion}")

        assertNotNull(octo.serial, "OCTO serial number string should be readable via HidD_GetSerialNumberString")
        val fwVersion = octo.firmwareVersion
        assertNotNull(fwVersion, "OCTO firmware version should be readable via status report")
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

            // User specified: Fans 1-4 are occupied (indices 0..3)
            for (channel in 0..3) {
                val fan = telemetry.fans.firstOrNull { it.channel == channel }
                assertNotNull(fan, "Fan ${channel + 1} (channel $channel) must be present in telemetry")
                println(
                    "Fan ${channel + 1}: rpm=${fan.rpm}, voltage=${fan.voltageVolts}V, " +
                        "current=${fan.currentAmps}A, power=${fan.powerWatts}W",
                )
                assertTrue(fan.voltageVolts > 0.0, "Fan ${channel + 1} voltage should be > 0V")
            }

            // User specified: Sensor 1 is occupied (channel 0)
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
    fun testConfigReadAndSafeFlashUnoccupiedFan() {
        val octo =
            controller.scanDevices().firstOrNull { it.type == DeviceType.OCTO }
                ?: error("OCTO not found")

        val initialConfig = controller.readConfig(octo)
        assertTrue(initialConfig is OctoConfig, "Config must be OctoConfig")
        assertEquals(8, initialConfig.fans.size, "OCTO has 8 fan channels")

        // Fans 1-4 (channels 0..3) and Sensor 1 (channel 0) MUST NOT be altered!
        // We test flashing on Fan 5 (channel 4, unoccupied)
        val testFanChannel = 4
        val originalFan5Config = initialConfig.fans[testFanChannel]

        try {
            // Flash a safe test fixed duty cycle to Fan 5 only
            val testTargetPower = 35.0
            val testFixedConfig = FixedFanConfig(channel = testFanChannel, powerPercent = testTargetPower)
            val testRequest =
                FlashOctoConfig(
                    fans = mapOf(testFanChannel to testFixedConfig),
                )

            println("Flashing test configuration to Fan 5 (channel $testFanChannel) with $testTargetPower% power...")
            val readback = controller.flashConfig(octo, testRequest)
            assertTrue(readback is OctoConfig)

            val updatedFan5 = readback.fans[testFanChannel]
            assertTrue(updatedFan5 is FixedFanConfig, "Fan 5 should now be FixedFanConfig")
            assertEquals(testTargetPower, updatedFan5.powerPercent, 0.5, "Fan 5 power should match flashed value")
            println("Verified Fan 5 updated successfully to ${updatedFan5.powerPercent}%")
        } finally {
            println("Restoring original Fan 5 configuration...")
            // Restore only Fan 5 to its original config
            val restoreRequest =
                FlashOctoConfig(
                    fans =
                        mapOf(
                            testFanChannel to originalFan5Config.toSafeRestorableConfig(testFanChannel),
                        ),
                )
            controller.flashConfig(octo, restoreRequest)
            val restoredConfig = controller.readConfig(octo) as OctoConfig
            println("Restored Fan 5: ${restoredConfig.fans[testFanChannel]}")
        }
    }

    @Test
    fun testFeedVirtualSensors() {
        val octo =
            controller.scanDevices().firstOrNull { it.type == DeviceType.OCTO }
                ?: error("OCTO not found")

        // OCTO virtual sensors are channels 4..19. Sensor 1 (channel 0) is occupied physical.
        // We feed virtual sensor slot 2 (channel 5) with 26.5°C
        val testChannel = 5
        val testTemp = 26.5
        println("Feeding virtual sensor channel $testChannel with $testTemp°C...")
        controller.feedVirtualSensors(octo, mapOf(testChannel to testTemp))
        println("Virtual sensor packet sent successfully")
    }

    private fun FanChannelConfig.toSafeRestorableConfig(channel: Int): FanChannelConfig =
        when (this) {
            is FixedFanConfig -> this
            is CurveFanConfig -> this
            is TargetTempFanConfig -> this
            is TwoPointFanConfig -> this
            else -> FixedFanConfig(channel = channel, powerPercent = 50.0)
        }
}
