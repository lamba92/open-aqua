package io.github.lamba92.openaqua.screenshots

import io.github.lamba92.openaqua.app.model.AppStoredData
import io.github.lamba92.openaqua.screenshots.mock.mockOctoVirtualSensorFeedingUiState
import io.github.lamba92.openaqua.screenshots.model.Scene
import io.github.lamba92.openaqua.screenshots.model.ScreenshotState
import io.github.lamba92.openaqua.theme.AppColorScheme
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

fun main(args: Array<String>) {
    val outputDir = if (args.isNotEmpty()) Path(args[0]) else Path("screenshots")
    outputDir.createDirectories()

    println("Generating screenshots in: $outputDir")

    val scenarios =
        listOf(
            ScreenshotState(
                name = "fans_screen",
                scene = Scene.DeviceScreen.Fans(),
                storedData = AppStoredData.DEFAULT.copy(colorScheme = AppColorScheme.HYDRO_DARK),
            ),
            ScreenshotState(
                name = "sensors_screen",
                scene = Scene.DeviceScreen.Sensors(),
                storedData = AppStoredData.DEFAULT.copy(colorScheme = AppColorScheme.MIDNIGHT_NAVY),
            ),
            ScreenshotState(
                name = "virtual_sensor_feeding",
                scene =
                    Scene.DeviceScreen.Sensors(
                        sensorsState = mockOctoVirtualSensorFeedingUiState,
                    ),
                storedData = AppStoredData.DEFAULT.copy(colorScheme = AppColorScheme.SOLAR_AMBER),
            ),
            ScreenshotState(
                name = "device_info_screen",
                scene = Scene.DeviceScreen.DeviceInfo(),
                storedData = AppStoredData.DEFAULT.copy(colorScheme = AppColorScheme.HIGH_CONTRAST_DARK),
            ),
        )

    renderScreenshots(scenarios, outputDir)

    println("Screenshots generation completed!")
}
