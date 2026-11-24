package io.github.lamba92.openaqua.app.feature.device.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.repository.DeviceErrorType
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.FeederStatus
import io.github.lamba92.openaqua.app.ui.components.OpenAquaNavItem
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.device.DeviceCapability
import io.github.lamba92.openaqua.model.device.DeviceDescriptor
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import io.github.lamba92.openaqua.model.device.hasCapability
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.device_error_generic
import open_aqua.app_compose.generated.resources.device_error_io
import open_aqua.app_compose.generated.resources.device_error_protocol
import open_aqua.app_compose.generated.resources.device_error_timeout
import open_aqua.app_compose.generated.resources.ic_fan
import open_aqua.app_compose.generated.resources.ic_info
import open_aqua.app_compose.generated.resources.ic_sensor
import open_aqua.app_compose.generated.resources.ic_warning
import open_aqua.app_compose.generated.resources.nav_device_info
import open_aqua.app_compose.generated.resources.nav_fans
import open_aqua.app_compose.generated.resources.nav_sensors
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeviceDrawerScaffold(
    device: DetectedHardwareDevice,
    descriptor: DeviceDescriptor,
    selectedTab: CommonDeviceTab,
    onSelectTab: (CommonDeviceTab) -> Unit,
    modifier: Modifier = Modifier,
    feederStatus: FeederStatus = FeederStatus.NONE,
    content: @Composable () -> Unit,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    val drawerMetrics = metrics.navigationDrawer
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
                            .padding(vertical = drawerMetrics.headerPaddingVertical),
                    verticalArrangement = Arrangement.spacedBy(drawerMetrics.itemSpacing),
                ) {
                    val displayName = device.name.removePrefix("Aquacomputer").trim()
                    Text(
                        text = displayName,
                        color = colors.textPrimary,
                        fontSize = drawerMetrics.headerTitleTextSize,
                        lineHeight = drawerMetrics.headerTitleLineHeight,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = drawerMetrics.headerPaddingHorizontal),
                    )
                    Text(
                        text = device.alias.value,
                        color = colors.primary,
                        fontSize = drawerMetrics.headerSubtitleTextSize,
                        lineHeight = drawerMetrics.headerSubtitleLineHeight,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = drawerMetrics.headerPaddingHorizontal),
                    )

                    Spacer(modifier = Modifier.height(metrics.spacingXs))
                    HorizontalDivider(
                        color = colors.borderOutline,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = drawerMetrics.headerPaddingHorizontal),
                    )
                    Spacer(modifier = Modifier.height(metrics.spacingXs))

                    OpenAquaNavItem(
                        icon = painterResource(Res.drawable.ic_info),
                        label = stringResource(Res.string.nav_device_info),
                        isSelected = selectedTab == CommonDeviceTab.DEVICE_INFO,
                        onClick = { onSelectTab(CommonDeviceTab.DEVICE_INFO) },
                    )

                    if (descriptor.hasCapability<DeviceCapability.Fans>()) {
                        OpenAquaNavItem(
                            icon = painterResource(Res.drawable.ic_fan),
                            label = stringResource(Res.string.nav_fans),
                            isSelected = selectedTab == CommonDeviceTab.FANS,
                            onClick = { onSelectTab(CommonDeviceTab.FANS) },
                        )
                    }

                    if (
                        descriptor.hasCapability<DeviceCapability.TemperatureSensors>() ||
                        descriptor.hasCapability<DeviceCapability.VirtualSensors>() ||
                        descriptor.hasCapability<DeviceCapability.FlowSensor>()
                    ) {
                        val sensorStatus =
                            when {
                                feederStatus != FeederStatus.NONE -> feederStatus
                                else -> FeederStatus.NONE
                            }
                        OpenAquaNavItem(
                            icon = painterResource(Res.drawable.ic_sensor),
                            label = stringResource(Res.string.nav_sensors),
                            isSelected = selectedTab == CommonDeviceTab.SENSORS,
                            onClick = { onSelectTab(CommonDeviceTab.SENSORS) },
                            status = sensorStatus,
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        content = content,
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
                painter = painterResource(Res.drawable.ic_warning),
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
