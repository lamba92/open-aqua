package io.github.lamba92.openaqua.app.feature.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.theme.BorderOutline
import io.github.lamba92.openaqua.app.theme.CyanBright
import io.github.lamba92.openaqua.app.theme.ElectricCyan
import io.github.lamba92.openaqua.app.theme.ObsidianBg
import io.github.lamba92.openaqua.app.theme.StatusNominal
import io.github.lamba92.openaqua.app.theme.StatusWarning
import io.github.lamba92.openaqua.app.theme.SurfaceContainerHigh
import io.github.lamba92.openaqua.app.theme.SurfaceContainerLowest
import io.github.lamba92.openaqua.app.theme.TextMuted
import io.github.lamba92.openaqua.app.theme.TextPrimary
import io.github.lamba92.openaqua.app.theme.TextSecondary
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.ic_device
import open_aqua.app_compose.generated.resources.ic_openaqua
import open_aqua.app_compose.generated.resources.ic_settings
import open_aqua.app_compose.generated.resources.nav_settings
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeviceNavigationRailScaffold(
    state: DeviceNavState.Ready,
    onEvent: (DeviceNavEvent) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val selectedDevice = state.selectedDeviceState.device
    val railItemColors =
        NavigationRailItemDefaults.colors(
            selectedIconColor = ObsidianBg,
            selectedTextColor = CyanBright,
            indicatorColor = CyanBright,
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextMuted,
        )

    Row(modifier = modifier.fillMaxSize()) {
        // LAYER 1: Material 3 NavigationRail (Devices + Settings at bottom)
        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = SurfaceContainerLowest,
            contentColor = TextPrimary,
            header = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_openaqua),
                            contentDescription = null,
                            tint = CyanBright,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        color = BorderOutline,
                        modifier = Modifier.width(48.dp),
                    )
                }
            },
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                state.devices.forEach { deviceState ->
                    val device = deviceState.device
                    val isSelected = !state.isSettingsSelected && device.id == selectedDevice.id
                    val statusColor =
                        when (deviceState) {
                            is DeviceState.Connecting -> ElectricCyan
                            is DeviceState.Error -> StatusWarning
                            is DeviceState.Ready -> StatusNominal
                        }
                    NavigationRailItem(
                        selected = isSelected,
                        onClick = { onEvent(DeviceNavEvent.SelectDevice(device.id)) },
                        icon = {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_device),
                                    contentDescription = device.name,
                                    modifier = Modifier.size(24.dp),
                                )
                                Box(
                                    modifier =
                                        Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(statusColor),
                                )
                            }
                        },
                        label = {
                            Text(
                                text = device.type.name,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        colors = railItemColors,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                HorizontalDivider(
                    color = BorderOutline,
                    modifier = Modifier.width(48.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                val settingsLabel = stringResource(Res.string.nav_settings)
                NavigationRailItem(
                    selected = state.isSettingsSelected,
                    onClick = { onEvent(DeviceNavEvent.SelectSettings) },
                    icon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_settings),
                            contentDescription = settingsLabel,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = {
                        Text(
                            text = settingsLabel,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = railItemColors,
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            content()
        }
    }
}
