package io.github.lamba92.openaqua.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.svg.SvgDecoder
import io.github.lamba92.openaqua.app.coroutine.DefaultCoroutineScopeProvider
import io.github.lamba92.openaqua.app.di.LocalDI
import io.github.lamba92.openaqua.app.di.createOpenAquaDI
import io.github.lamba92.openaqua.app.di.rememberInstance
import io.github.lamba92.openaqua.app.feature.fans.FansViewModel
import io.github.lamba92.openaqua.app.feature.info.DeviceInfoViewModel
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavComponent
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavEvent
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavState
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavViewModel
import io.github.lamba92.openaqua.app.feature.sensors.SensorsViewModel
import io.github.lamba92.openaqua.app.theme.CyanBright
import io.github.lamba92.openaqua.app.theme.ObsidianBg
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.theme.StatusWarning
import io.github.lamba92.openaqua.app.theme.TextPrimary
import io.github.lamba92.openaqua.app.theme.TextSecondary
import io.github.lamba92.openaqua.app.ui.navigation.TwoLayerNavigationScaffold
import io.github.lamba92.openaqua.app.ui.screens.DeviceInfoScreen
import io.github.lamba92.openaqua.app.ui.screens.FansScreen
import io.github.lamba92.openaqua.app.ui.screens.SensorsScreen
import io.github.lamba92.openaqua.app.util.EngineExtractor
import kotlinx.coroutines.runBlocking
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.retry_scan
import open_aqua.app_compose.generated.resources.scanning_hardware
import open_aqua.app_compose.generated.resources.window_title
import org.jetbrains.compose.resources.stringResource

fun main() {
    val enginePath = runBlocking { EngineExtractor.extract() }
    val scopeProvider = DefaultCoroutineScopeProvider()
    val di = createOpenAquaDI(enginePath, scopeProvider)

    application {
        val windowState =
            rememberWindowState(
                size = DpSize(1380.dp, 880.dp),
            )

        setSingletonImageLoaderFactory { context ->
            ImageLoader
                .Builder(context)
                .components {
                    add(SvgDecoder.Factory())
                }.build()
        }

        Window(
            onCloseRequest = {
                scopeProvider.cancelAll()
                exitApplication()
            },
            state = windowState,
            title = stringResource(Res.string.window_title),
        ) {
            CompositionLocalProvider(LocalDI provides di) {
                OpenAquaTheme {
                    OpenAquaApp()
                }
            }
        }
    }
}

@Composable
fun OpenAquaApp(
    navViewModel: DeviceNavViewModel = rememberInstance(),
    infoViewModel: DeviceInfoViewModel = rememberInstance(),
    fansViewModel: FansViewModel = rememberInstance(),
    sensorsViewModel: SensorsViewModel = rememberInstance(),
) {
    val navState by navViewModel.state.collectAsState()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(ObsidianBg),
    ) {
        when (val state = navState) {
            DeviceNavState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp),
                    ) {
                        CircularProgressIndicator(
                            color = CyanBright,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(Res.string.scanning_hardware),
                            color = TextSecondary,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            is DeviceNavState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = StatusWarning,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            color = TextPrimary,
                            fontSize = 16.sp,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navViewModel.onEvent(DeviceNavEvent.RefreshDevices) }) {
                            Text(stringResource(Res.string.retry_scan))
                        }
                    }
                }
            }

            is DeviceNavState.Ready -> {
                TwoLayerNavigationScaffold(
                    devices = state.devices,
                    selectedDevice = state.selectedDevice,
                    selectedComponent = state.selectedComponent,
                    onSelectDevice = { navViewModel.onEvent(DeviceNavEvent.SelectDevice(it)) },
                    onSelectComponent = { navViewModel.onEvent(DeviceNavEvent.SelectComponent(it)) },
                    onRefreshDevices = { navViewModel.onEvent(DeviceNavEvent.RefreshDevices) },
                    isRefreshing = state.isRefreshing,
                ) {
                    when (state.selectedComponent) {
                        DeviceNavComponent.DEVICE_INFO -> {
                            DeviceInfoScreen(
                                device = state.selectedDevice,
                                viewModel = infoViewModel,
                            )
                        }

                        DeviceNavComponent.FANS -> {
                            FansScreen(
                                device = state.selectedDevice,
                                viewModel = fansViewModel,
                            )
                        }

                        DeviceNavComponent.SENSORS -> {
                            SensorsScreen(
                                device = state.selectedDevice,
                                viewModel = sensorsViewModel,
                            )
                        }
                    }
                }
            }
        }
    }
}
