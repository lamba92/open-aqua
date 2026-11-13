package io.github.lamba92.openaqua.app.model

import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavState
import io.github.lamba92.openaqua.model.host.HostFanId
import io.github.lamba92.openaqua.model.host.HostSensorId
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AppStoredDataSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        }

    @Test
    fun testHostFanConfigSerializationRoundTrip() {
        val fixed = HostFanConfig.Fixed(75.0)
        val fixedJson = json.encodeToString<HostFanConfig>(fixed)
        assert(fixedJson.contains("\"type\": \"fixed\"") || fixedJson.contains("\"type\":\"fixed\""))
        assert(fixedJson.contains("75.0"))

        val decodedFixed = json.decodeFromString<HostFanConfig>(fixedJson)
        assertEquals(fixed, decodedFixed)

        val bios = HostFanConfig.FollowBios
        val biosJson = json.encodeToString<HostFanConfig>(bios)
        val decodedBios = json.decodeFromString<HostFanConfig>(biosJson)
        assertEquals(bios, decodedBios)

        val auto =
            HostFanConfig.AutoCurve(
                sensorSource = VirtualSensorFeedSource.Host(HostSensorId("mock-cpu:package")),
                tMin = 35.0,
                tMax = 70.0,
            )
        val autoJson = json.encodeToString<HostFanConfig>(auto)
        val decodedAuto = json.decodeFromString<HostFanConfig>(autoJson)
        assertEquals(auto, decodedAuto)
    }

    @Test
    fun testSelectedItemHostSerializationRoundTrip() {
        val navItem: DeviceNavState.SelectedItem = DeviceNavState.SelectedItem.Host(HostTab.FANS)
        val itemJson = json.encodeToString<DeviceNavState.SelectedItem>(navItem)
        assert(itemJson.contains("\"type\": \"host\"") || itemJson.contains("\"type\":\"host\""))

        val decodedItem = json.decodeFromString<DeviceNavState.SelectedItem>(itemJson)
        assertEquals(navItem, decodedItem)
    }

    @Test
    fun testVirtualSensorFeedSourceSerializationRoundTrip() {
        val source: VirtualSensorFeedSource = VirtualSensorFeedSource.Host(HostSensorId("mock-cpu:package"))
        val sourceJson = json.encodeToString<VirtualSensorFeedSource>(source)
        assert(sourceJson.contains("\"type\": \"host\"") || sourceJson.contains("\"type\":\"host\""))

        val decoded = json.decodeFromString<VirtualSensorFeedSource>(sourceJson)
        assertEquals(source, decoded)
    }
}
