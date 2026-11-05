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
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.FeederStatus
import io.github.lamba92.openaqua.app.ui.components.FeederStatusBadge
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice
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
    feederStatus: FeederStatus = FeederStatus.NONE,
    content: @Composable () -> Unit,
) {
    val colors = OpenAquaTheme.colors
    val drawerMetrics = OpenAquaTheme.metrics.navigationDrawer
    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(
                modifier =
                    Modifier
                        .width(drawerMetrics.width)
                        .fillMaxHeight(),
                drawerContainerColor = colors.surfaceLow,
                drawerContentColor = colors.textPrimary,
                drawerShape = RoundedCornerShape(0.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .padding(
                                horizontal = drawerMetrics.headerPaddingHorizontal,
                                vertical = drawerMetrics.headerPaddingVertical,
                            ),
                    verticalArrangement = Arrangement.spacedBy(drawerMetrics.itemSpacing),
                ) {
                    val displayName = device.name.removePrefix("Aquacomputer").trim()
                    Text(
                        text = displayName,
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Text(
                        text = device.alias.value,
                        color = colors.primary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = colors.borderOutline)
                    Spacer(modifier = Modifier.height(8.dp))

                    val drawerColors =
                        NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = colors.surfaceHigh,
                            unselectedContainerColor = Color.Transparent,
                            selectedIconColor = colors.primaryBright,
                            unselectedIconColor = colors.textSecondary,
                            selectedTextColor = colors.textPrimary,
                            unselectedTextColor = colors.textSecondary,
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
                        badge =
                            if (feederStatus != FeederStatus.NONE) {
                                { FeederStatusBadge(status = feederStatus) }
                            } else {
                                null
                            },
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
    badge: (@Composable () -> Unit)? = null,
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                style = LocalTextStyle.current.copy(lineHeightStyle = DrawerLineHeightStyle),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        },
        icon = {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        },
        badge = badge,
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
            color = OpenAquaTheme.colors.primaryBright,
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
                tint = OpenAquaTheme.colors.statusWarning,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = OpenAquaTheme.colors.textPrimary,
                fontSize = 18.sp,
            )
        }
    }
}
