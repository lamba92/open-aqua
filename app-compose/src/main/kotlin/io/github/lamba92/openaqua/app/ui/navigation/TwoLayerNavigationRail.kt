package io.github.lamba92.openaqua.app.ui.navigation

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavComponent
import io.github.lamba92.openaqua.app.theme.BorderOutline
import io.github.lamba92.openaqua.app.theme.CyanBright
import io.github.lamba92.openaqua.app.theme.ElectricCyan
import io.github.lamba92.openaqua.app.theme.ObsidianBg
import io.github.lamba92.openaqua.app.theme.StatusNominal
import io.github.lamba92.openaqua.app.theme.SurfaceContainerHigh
import io.github.lamba92.openaqua.app.theme.SurfaceContainerLow
import io.github.lamba92.openaqua.app.theme.SurfaceContainerLowest
import io.github.lamba92.openaqua.app.theme.TextMuted
import io.github.lamba92.openaqua.app.theme.TextPrimary
import io.github.lamba92.openaqua.app.theme.TextSecondary
import io.github.lamba92.openaqua.model.DetectedHardwareDevice
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.ic_device
import open_aqua.app_compose.generated.resources.ic_fan
import open_aqua.app_compose.generated.resources.ic_info
import open_aqua.app_compose.generated.resources.ic_openaqua
import open_aqua.app_compose.generated.resources.ic_refresh
import open_aqua.app_compose.generated.resources.ic_sensor
import open_aqua.app_compose.generated.resources.nav_device_info
import open_aqua.app_compose.generated.resources.nav_fans
import open_aqua.app_compose.generated.resources.nav_sensors
import open_aqua.app_compose.generated.resources.refresh_devices
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TwoLayerNavigationScaffold(
    devices: List<DetectedHardwareDevice>,
    selectedDevice: DetectedHardwareDevice,
    selectedComponent: DeviceNavComponent,
    onSelectDevice: (DetectedHardwareDevice) -> Unit,
    onSelectComponent: (DeviceNavComponent) -> Unit,
    onRefreshDevices: () -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(modifier = modifier.fillMaxSize()) {
        // LAYER 1: Material 3 NavigationRail (Devices)
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
                devices.forEach { device ->
                    val isSelected = device.id == selectedDevice.id
                    NavigationRailItem(
                        selected = isSelected,
                        onClick = { onSelectDevice(device) },
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
                                            .background(StatusNominal),
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
                        colors =
                            NavigationRailItemDefaults.colors(
                                selectedIconColor = ObsidianBg,
                                selectedTextColor = CyanBright,
                                indicatorColor = CyanBright,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextMuted,
                            ),
                    )
                }
            }

            // Bottom refresh button
            Box(
                modifier = Modifier.padding(bottom = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = CyanBright,
                        strokeWidth = 2.dp,
                    )
                } else {
                    IconButton(onClick = onRefreshDevices) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_refresh),
                            contentDescription = stringResource(Res.string.refresh_devices),
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        // LAYER 2: Material 3 PermanentNavigationDrawer (Device Specific Components) wrapping Content
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(
                    modifier =
                        Modifier
                            .width(220.dp)
                            .fillMaxHeight(),
                    drawerContainerColor = SurfaceContainerLow,
                    drawerContentColor = TextPrimary,
                    drawerShape = RoundedCornerShape(0.dp),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 12.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Selected Device Header
                        Text(
                            text = selectedDevice.name,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        Text(
                            text = selectedDevice.alias.ifBlank { selectedDevice.id },
                            color = ElectricCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = BorderOutline)
                        Spacer(modifier = Modifier.height(8.dp))

                        // NavigationDrawerItems for Device Components
                        val drawerColors =
                            NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = SurfaceContainerHigh,
                                unselectedContainerColor = Color.Transparent,
                                selectedIconColor = CyanBright,
                                unselectedIconColor = TextSecondary,
                                selectedTextColor = TextPrimary,
                                unselectedTextColor = TextSecondary,
                            )

                        NavigationDrawerItem(
                            label = { Text(stringResource(Res.string.nav_device_info), fontSize = 13.sp) },
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_info),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            selected = selectedComponent == DeviceNavComponent.DEVICE_INFO,
                            onClick = { onSelectComponent(DeviceNavComponent.DEVICE_INFO) },
                            colors = drawerColors,
                            shape = RoundedCornerShape(8.dp),
                        )

                        NavigationDrawerItem(
                            label = { Text(stringResource(Res.string.nav_fans), fontSize = 13.sp) },
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_fan),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            selected = selectedComponent == DeviceNavComponent.FANS,
                            onClick = { onSelectComponent(DeviceNavComponent.FANS) },
                            colors = drawerColors,
                            shape = RoundedCornerShape(8.dp),
                        )

                        NavigationDrawerItem(
                            label = { Text(stringResource(Res.string.nav_sensors), fontSize = 13.sp) },
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_sensor),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            selected = selectedComponent == DeviceNavComponent.SENSORS,
                            onClick = { onSelectComponent(DeviceNavComponent.SENSORS) },
                            colors = drawerColors,
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }
            },
            modifier = Modifier.weight(1f).fillMaxHeight(),
            content = content,
        )
    }
}
