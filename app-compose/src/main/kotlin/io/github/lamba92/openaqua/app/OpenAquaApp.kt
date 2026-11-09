package io.github.lamba92.openaqua.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.lamba92.openaqua.app.di.rememberInstance
import io.github.lamba92.openaqua.app.feature.about.AboutScreen
import io.github.lamba92.openaqua.app.feature.bugreport.BugReportScreen
import io.github.lamba92.openaqua.app.feature.device.DeviceScreen
import io.github.lamba92.openaqua.app.feature.device.SelectADeviceScreen
import io.github.lamba92.openaqua.app.feature.host.HostScreen
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavEvent
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavState
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavViewModel
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavigationRailScaffold
import io.github.lamba92.openaqua.app.feature.navigation.NavigationTargets
import io.github.lamba92.openaqua.app.feature.permission.PermissionLoadingScreen
import io.github.lamba92.openaqua.app.feature.permission.PermissionScreen
import io.github.lamba92.openaqua.app.feature.settings.SettingsScreen
import io.github.lamba92.openaqua.app.feature.settings.SettingsTab
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme

@Composable
fun OpenAquaApp(navViewModel: DeviceNavViewModel = rememberInstance()) {
    val navState by navViewModel.state.collectAsState()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(OpenAquaTheme.colors.background),
    ) {
        DeviceNavigationRailScaffold(
            state = navState,
            onEvent = navViewModel::onEvent,
        ) {
            when (val item = navState.selectedItem) {
                null -> {
                    when (navState.targets) {
                        NavigationTargets.Loading -> PermissionLoadingScreen()
                        else -> SelectADeviceScreen()
                    }
                }

                DeviceNavState.SelectedItem.BugReport -> {
                    BugReportScreen(
                        onNavigateToLogs = { navViewModel.onEvent(DeviceNavEvent.SelectSettingsTab(SettingsTab.LOGS)) },
                    )
                }

                is DeviceNavState.SelectedItem.About -> {
                    AboutScreen(
                        selectedTab = item.selectedTab,
                        onSelectTab = { navViewModel.onEvent(DeviceNavEvent.SelectAboutTab(it)) },
                    )
                }

                is DeviceNavState.SelectedItem.Settings -> {
                    SettingsScreen(
                        selectedTab = item.selectedTab,
                        onSelectTab = { navViewModel.onEvent(DeviceNavEvent.SelectSettingsTab(it)) },
                        viewModel = rememberInstance(),
                    )
                }

                DeviceNavState.SelectedItem.Host -> {
                    HostScreen(viewModel = rememberInstance())
                }

                is DeviceNavState.SelectedItem.Device -> {
                    DeviceScreen(
                        deviceId = item.deviceId,
                        selectedTab = item.selectedTab,
                        onSelectTab = { navViewModel.onEvent(DeviceNavEvent.SelectTab(it)) },
                    )
                }

                DeviceNavState.SelectedItem.PermissionError -> {
                    PermissionScreen()
                }
            }
        }
    }
}
