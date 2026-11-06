package io.github.lamba92.openaqua.app.feature.bugreport

import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.device.DeviceAlias
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.DeviceType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BugReportViewModelTest {
    @Test
    fun testFormatConnectedDevicesEmpty() {
        val formatted = BugReportViewModel.formatConnectedDevices(emptyList())
        assertTrue(formatted.isEmpty())
    }

    @Test
    fun testFormatConnectedDevicesSkipsSerialIdAndAlias() {
        val devices =
            listOf(
                DetectedHardwareDevice(
                    id = DeviceId.from("13235-54067-SECRET"),
                    alias = DeviceAlias.from("octo-private-alias"),
                    type = DeviceType.OCTO,
                    name = "Aquacomputer OCTO",
                    serial = "13235-54067-SECRET-SERIAL",
                    firmwareVersion = 1020,
                ),
            )

        val formatted = BugReportViewModel.formatConnectedDevices(devices)
        assertEquals(1, formatted.size)
        val text = formatted.first()
        assertEquals("Aquacomputer OCTO (FW: 1020)", text)
        assertFalse(text.contains("13235-54067"))
        assertFalse(text.contains("SECRET"))
        assertFalse(text.contains("private-alias"))
    }

    @Test
    fun testFormatConnectedDevicesMultiple() {
        val devices =
            listOf(
                DetectedHardwareDevice(
                    id = DeviceId.from("dev-1"),
                    alias = DeviceAlias.from("octo-1"),
                    type = DeviceType.OCTO,
                    name = "Aquacomputer OCTO",
                    firmwareVersion = 1020,
                ),
                DetectedHardwareDevice(
                    id = DeviceId.from("dev-2"),
                    alias = DeviceAlias.from("quadro-1"),
                    type = DeviceType.QUADRO,
                    name = "Aquacomputer QUADRO",
                    firmwareVersion = null,
                ),
            )

        val formatted = BugReportViewModel.formatConnectedDevices(devices)
        assertEquals(listOf("Aquacomputer OCTO (FW: 1020)", "Aquacomputer QUADRO"), formatted)
    }

    @Test
    fun testCreateIssueUrl() {
        val buildInfo =
            listOf(
                "OpenAqua Version" to "1.0.0",
                "Build Date" to "2026-11-06",
                "OS" to "Linux",
                "JVM" to "21",
                "Kotlin" to "2.4.0",
            )
        val connectedDevices = listOf("Aquacomputer OCTO (FW: 1020)")

        val url = BugReportViewModel.createIssueUrl(buildInfo, connectedDevices)
        assertTrue(url.startsWith("https://github.com/lamba92/open-aqua/issues/new?body="))
        assertTrue(url.contains("Aquacomputer"))
        assertTrue(url.contains("1020"))
        assertTrue(url.contains("Connected%20Devices"))
    }
}
