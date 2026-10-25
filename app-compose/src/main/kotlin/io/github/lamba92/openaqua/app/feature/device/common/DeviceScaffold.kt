package io.github.lamba92.openaqua.app.feature.device.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.repository.DeviceErrorType
import io.github.lamba92.openaqua.app.theme.BorderOutline
import io.github.lamba92.openaqua.app.theme.CyanBright
import io.github.lamba92.openaqua.app.theme.ElectricCyan
import io.github.lamba92.openaqua.app.theme.StatusWarning
import io.github.lamba92.openaqua.app.theme.SurfaceContainerHigh
import io.github.lamba92.openaqua.app.theme.SurfaceContainerLow
import io.github.lamba92.openaqua.app.theme.TextPrimary
import io.github.lamba92.openaqua.app.theme.TextSecondary
import io.github.lamba92.openaqua.model.DetectedHardwareDevice
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.device_error_generic
import open_aqua.app_compose.generated.resources.device_error_io
import open_aqua.app_compose.generated.resources.device_error_protocol
import open_aqua.app_compose.generated.resources.device_error_timeout
import open_aqua.app_compose.generated.resources.ic_fan
import open_aqua.app_compose.generated.resources.ic_info
import open_aqua.app_compose.generated.resources.ic_sensor
import open_aqua.app_compose.generated.resources.nav_device_info
import open_aqua.app_compose.generated.resources.nav_fans
import open_aqua.app_compose.generated.resources.nav_sensors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val DrawerLineHeightStyle =
    LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    )

@Composable
fun DeviceDrawerScaffold(
    device: DetectedHardwareDevice,
    selectedTab: CommonDeviceTab,
    onSelectTab: (CommonDeviceTab) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
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
                    Text(
                        text = device.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Text(
                        text = device.alias.value,
                        color = ElectricCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = BorderOutline)
                    Spacer(modifier = Modifier.height(8.dp))

                    val drawerColors =
                        NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = SurfaceContainerHigh,
                            unselectedContainerColor = Color.Transparent,
                            selectedIconColor = CyanBright,
                            unselectedIconColor = TextSecondary,
                            selectedTextColor = TextPrimary,
                            unselectedTextColor = TextSecondary,
                        )

                    DeviceDrawerTabItem(
                        label = stringResource(Res.string.nav_device_info),
                        icon = painterResource(Res.drawable.ic_info),
                        selected = selectedTab == CommonDeviceTab.DEVICE_INFO,
                        onClick = { onSelectTab(CommonDeviceTab.DEVICE_INFO) },
                        colors = drawerColors,
                    )

                    DeviceDrawerTabItem(
                        label = stringResource(Res.string.nav_fans),
                        icon = painterResource(Res.drawable.ic_fan),
                        selected = selectedTab == CommonDeviceTab.FANS,
                        onClick = { onSelectTab(CommonDeviceTab.FANS) },
                        colors = drawerColors,
                    )

                    DeviceDrawerTabItem(
                        label = stringResource(Res.string.nav_sensors),
                        icon = painterResource(Res.drawable.ic_sensor),
                        selected = selectedTab == CommonDeviceTab.SENSORS,
                        onClick = { onSelectTab(CommonDeviceTab.SENSORS) },
                        colors = drawerColors,
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        content = content,
    )
}

@Composable
private fun DeviceDrawerTabItem(
    label: String,
    icon: Painter,
    selected: Boolean,
    onClick: () -> Unit,
    colors: NavigationDrawerItemColors,
    modifier: Modifier = Modifier,
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                style = LocalTextStyle.current.copy(lineHeightStyle = DrawerLineHeightStyle),
            )
        },
        icon = {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        },
        selected = selected,
        onClick = onClick,
        colors = colors,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
    )
}

@Composable
fun DeviceLoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = CyanBright,
            modifier = Modifier.size(48.dp),
        )
    }
}

@Composable
fun DeviceErrorContent(
    errorType: DeviceErrorType,
    modifier: Modifier = Modifier,
) {
    val message =
        when (errorType) {
            DeviceErrorType.CommunicationTimeout -> stringResource(Res.string.device_error_timeout)
            DeviceErrorType.IoFailure -> stringResource(Res.string.device_error_io)
            DeviceErrorType.ProtocolMismatch -> stringResource(Res.string.device_error_protocol)
            DeviceErrorType.Unknown -> stringResource(Res.string.device_error_generic)
        }

    Box(
        modifier = modifier.fillMaxSize(),
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
                text = message,
                color = TextPrimary,
                fontSize = 16.sp,
            )
        }
    }
}
