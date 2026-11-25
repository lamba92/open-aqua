package io.github.lamba92.openaqua.screenshots

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import com.compose.sdl.ApplicationScope
import com.compose.sdl.GpuMode
import com.compose.sdl.Window
import com.compose.sdl.disableInfiniteAnimations
import com.compose.sdl.nativeComposeApp
import com.compose.sdl.useVirtualFrameTime
import com.compose.sdl.windowHasInvalidations
import io.github.lamba92.openaqua.app.feature.about.AboutScreen
import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.feature.device.common.DeviceDrawerScaffold
import io.github.lamba92.openaqua.app.feature.device.fans.FansContent
import io.github.lamba92.openaqua.app.feature.device.info.DeviceInfoContent
import io.github.lamba92.openaqua.app.feature.device.sensors.SensorsContent
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavigationRailScaffold
import io.github.lamba92.openaqua.app.theme.AppMetrics
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.kotlinx.io.createDirectories
import io.github.lamba92.openaqua.kotlinx.io.resolve
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import io.github.lamba92.openaqua.screenshots.model.Scene
import io.github.lamba92.openaqua.screenshots.model.ScreenshotState
import io.github.lamba92.openaqua.theme.ThemePalette
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

@OptIn(ExperimentalForeignApi::class)
fun renderScreenshots(
    states: List<ScreenshotState>,
    outputDir: Path,
    quality: Int = 92,
) {
    if (states.isEmpty()) return

    outputDir.createDirectories()
    disableInfiniteAnimations = true
    useVirtualFrameTime = true

    nativeComposeApp {
        ScreenshotRunner(
            states = states,
            outputDir = outputDir,
            quality = quality,
        )
    }
}

@Composable
private fun ApplicationScope.ScreenshotRunner(
    states: List<ScreenshotState>,
    outputDir: Path,
    quality: Int,
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var quietFrames by remember { mutableIntStateOf(0) }

    if (currentIndex >= states.size) {
        exitApplication()
        return
    }

    val firstState = remember { states.first() }
    val windowState =
        remember {
            WindowState(size = DpSize(firstState.logicalWidth.dp, firstState.logicalHeight.dp))
        }

    val currentState = states[currentIndex]

    LaunchedEffect(currentIndex) {
        windowState.size = DpSize(currentState.logicalWidth.dp, currentState.logicalHeight.dp)
    }

    Window(
        onCloseRequest = { exitApplication() },
        state = windowState,
        title = "OpenAqua Screenshot - ${currentState.name}",
        undecorated = true,
        gpu = GpuMode.Auto,
        onFrame = { backend, _ ->
            if (windowHasInvalidations()) {
                quietFrames = 0
                true
            } else {
                quietFrames++
                if (quietFrames >= 3) {
                    val currentIdx = currentIndex
                    if (currentIdx < states.size) {
                        val stateToSave = states[currentIdx]
                        val snapshot = backend.snapshotBgra()
                        if (snapshot != null) {
                            val (width, height, bytes) = snapshot
                            val imageInfo =
                                ImageInfo(
                                    width = width,
                                    height = height,
                                    colorType = ColorType.BGRA_8888,
                                    alphaType = ColorAlphaType.PREMUL,
                                )
                            val skiaImage = Image.makeRaster(imageInfo, bytes, width * 4)
                            val data = skiaImage.encodeToData(EncodedImageFormat.JPEG, quality)
                            if (data != null) {
                                val targetFile = outputDir.resolve(stateToSave.filename)
                                SystemFileSystem.sink(targetFile).buffered().use { sink ->
                                    sink.write(data.bytes)
                                }
                                println("Saved screenshot: ${stateToSave.filename}")
                            } else {
                                println("Failed to encode JPEG for: ${stateToSave.name}")
                            }
                        } else {
                            println("Warning: snapshotBgra returned null for: ${stateToSave.name}")
                        }
                    }
                    quietFrames = 0
                    currentIndex++
                    if (currentIndex >= states.size) {
                        false
                    } else {
                        true
                    }
                } else {
                    true
                }
            }
        },
    ) {
        val palette = ThemePalette.fromColorScheme(currentState.storedData.colorScheme)
        val metrics = AppMetrics.fromScale(currentState.storedData.metricsScale)

        OpenAquaTheme(
            palette = palette,
            metrics = metrics,
            fontFamilyPreference = currentState.storedData.fontFamily,
            fontSp = currentState.storedData.fontSp,
            preferences = currentState.storedData,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = OpenAquaTheme.colors.background,
            ) {
                when (val currentScene = currentState.scene) {
                    is Scene.DeviceScreen.Fans -> {
                        DeviceNavigationRailScaffold(state = currentScene.navState, onEvent = {}) {
                            DeviceDrawerScaffold(
                                device = currentScene.device,
                                descriptor =
                                    DeviceRegistry.findByType(
                                        currentScene.device.type,
                                        currentScene.device.firmwareVersion,
                                    ),
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
                                descriptor =
                                    DeviceRegistry.findByType(
                                        currentScene.device.type,
                                        currentScene.device.firmwareVersion,
                                    ),
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
                                descriptor =
                                    DeviceRegistry.findByType(
                                        currentScene.device.type,
                                        currentScene.device.firmwareVersion,
                                    ),
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
}
