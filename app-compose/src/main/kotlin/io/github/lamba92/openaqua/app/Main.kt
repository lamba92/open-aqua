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
import io.github.lamba92.openaqua.app.feature.device.d5next.D5NextScreen
import io.github.lamba92.openaqua.app.feature.device.octo.OctoScreen
import io.github.lamba92.openaqua.app.feature.device.quadro.QuadroScreen
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavState
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavViewModel
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavigationRailScaffold
import io.github.lamba92.openaqua.app.theme.CyanBright
import io.github.lamba92.openaqua.app.theme.ObsidianBg
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.theme.StatusWarning
import io.github.lamba92.openaqua.app.theme.TextPrimary
import io.github.lamba92.openaqua.app.theme.TextSecondary
import io.github.lamba92.openaqua.app.util.EngineExtractor
import io.github.lamba92.openaqua.model.DeviceType
import kotlinx.coroutines.runBlocking
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.no_devices_found
import open_aqua.app_compose.generated.resources.scanning_hardware
import open_aqua.app_compose.generated.resources.unsupported_device
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
fun OpenAquaApp(navViewModel: DeviceNavViewModel = rememberInstance()) {
    val navState by navViewModel.state.collectAsState()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(ObsidianBg),
    ) {
        when (val state = navState) {
            DeviceNavState.Loading -> LoadingIndicator()
            DeviceNavState.NoDevices -> NoDevicesDetected()
            is DeviceNavState.Ready -> DeviceReady(state, navViewModel)
        }
    }
}

@Composable
private fun DeviceReady(
    state: DeviceNavState.Ready,
    navViewModel: DeviceNavViewModel,
) {
    val selectedDevice = state.selectedDeviceState.device

    DeviceNavigationRailScaffold(
        state = state,
        onEvent = navViewModel::onEvent,
    ) {
        when (selectedDevice.type) {
            DeviceType.OCTO -> OctoScreen(viewModel = rememberInstance(arg = selectedDevice.id))
            DeviceType.QUADRO -> QuadroScreen(viewModel = rememberInstance(arg = selectedDevice.id))
            DeviceType.D5_NEXT -> D5NextScreen(viewModel = rememberInstance(arg = selectedDevice.id))
            else -> UnsupportedDevice(selectedDevice.type)
        }
    }
}

@Composable
private fun UnsupportedDevice(type: DeviceType) {
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
                text = stringResource(Res.string.unsupported_device, type.name),
                color = TextPrimary,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun NoDevicesDetected() {
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
                text = stringResource(Res.string.no_devices_found),
                color = TextPrimary,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun LoadingIndicator() {
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
