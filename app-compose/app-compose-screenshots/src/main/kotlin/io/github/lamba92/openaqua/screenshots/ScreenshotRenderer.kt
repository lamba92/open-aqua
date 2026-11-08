package io.github.lamba92.openaqua.screenshots

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import io.github.lamba92.openaqua.app.feature.about.AboutScreen
import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.feature.device.common.DeviceDrawerScaffold
import io.github.lamba92.openaqua.app.feature.device.fans.FansContent
import io.github.lamba92.openaqua.app.feature.device.info.DeviceInfoContent
import io.github.lamba92.openaqua.app.feature.device.sensors.SensorsContent
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavigationRailScaffold
import io.github.lamba92.openaqua.app.theme.AppMetrics
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import io.github.lamba92.openaqua.screenshots.model.Scene
import io.github.lamba92.openaqua.screenshots.model.ScreenshotState
import io.github.lamba92.openaqua.theme.ThemePalette
import org.jetbrains.skia.EncodedImageFormat
import java.nio.file.Path
import kotlin.io.path.writeBytes

fun renderScreenshot(
    state: ScreenshotState,
    outputDir: Path,
    quality: Int = 92,
) {
    val width = (state.logicalWidth * state.scale).toInt()
    val height = (state.logicalHeight * state.scale).toInt()

    val scene =
        ImageComposeScene(
            width = width,
            height = height,
            density = Density(state.scale),
        ) {
            val palette = ThemePalette.fromColorScheme(state.storedData.colorScheme)
            val metrics = AppMetrics.fromScale(state.storedData.metricsScale)

            OpenAquaTheme(
                palette = palette,
                metrics = metrics,
                fontFamilyPreference = state.storedData.fontFamily,
                fontSp = state.storedData.fontSp,
                preferences = state.storedData,
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = OpenAquaTheme.colors.background,
                ) {
                    when (val currentScene = state.scene) {
                        is Scene.DeviceScreen.Fans -> {
                            DeviceNavigationRailScaffold(state = currentScene.navState, onEvent = {}) {
                                DeviceDrawerScaffold(
                                    device = currentScene.device,
                                    descriptor = DeviceRegistry.findByType(currentScene.device.type, currentScene.device.firmwareVersion),
                                    selectedTab = CommonDeviceTab.FANS,
                                    onSelectTab = {},
                                    feederStatus = currentScene.feederStatus,
                                ) {
                                    FansContent(state = currentScene.fansState, onEvent = {})
                                }
                            }
                        }

                        is Scene.DeviceScreen.Sensors -> {
                            DeviceNavigationRailScaffold(state = currentScene.navState, onEvent = {}) {
                                DeviceDrawerScaffold(
                                    device = currentScene.device,
                                    descriptor = DeviceRegistry.findByType(currentScene.device.type, currentScene.device.firmwareVersion),
                                    selectedTab = CommonDeviceTab.SENSORS,
                                    onSelectTab = {},
                                    feederStatus = currentScene.feederStatus,
                                ) {
                                    SensorsContent(state = currentScene.sensorsState, onEvent = {})
                                }
                            }
                        }

                        is Scene.DeviceScreen.DeviceInfo -> {
                            DeviceNavigationRailScaffold(state = currentScene.navState, onEvent = {}) {
                                DeviceDrawerScaffold(
                                    device = currentScene.device,
                                    descriptor = DeviceRegistry.findByType(currentScene.device.type, currentScene.device.firmwareVersion),
                                    selectedTab = CommonDeviceTab.DEVICE_INFO,
                                    onSelectTab = {},
                                    feederStatus = currentScene.feederStatus,
                                ) {
                                    DeviceInfoContent(deviceState = currentScene.deviceState)
                                }
                            }
                        }

                        is Scene.About -> {
                            DeviceNavigationRailScaffold(state = currentScene.navState, onEvent = {}) {
                                AboutScreen()
                            }
                        }
                    }
                }
            }
        }

    try {
        val outputFile = outputDir.resolve(state.filename)
        val image = scene.render()
        val data =
            image.encodeToData(EncodedImageFormat.JPEG, quality)
                ?: error("Failed to encode Skia image to JPEG")
        outputFile.writeBytes(data.bytes)
    } finally {
        scene.close()
    }
}

fun renderScreenshots(
    states: List<ScreenshotState>,
    outputDir: Path,
    quality: Int = 92,
) {
    for (state in states) {
        renderScreenshot(state, outputDir, quality)
    }
}
