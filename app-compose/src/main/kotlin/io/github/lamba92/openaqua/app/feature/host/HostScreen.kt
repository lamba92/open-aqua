package io.github.lamba92.openaqua.app.feature.host

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.OpenAquaVerticalScrollbar
import io.github.lamba92.openaqua.app.ui.components.ScreenHeader
import io.github.lamba92.openaqua.app.ui.components.scrollFadingEdge
import io.github.lamba92.openaqua.app.ui.components.sensorTemperatureColor
import io.github.lamba92.openaqua.model.host.HostDevice
import io.github.lamba92.openaqua.model.host.HostDeviceType
import io.github.lamba92.openaqua.model.host.HostSensorDescriptor
import io.github.lamba92.openaqua.model.host.HostSensorId
import io.github.lamba92.openaqua.model.host.HostSensorTelemetry
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.host_component_cpu
import open_aqua.app_compose.generated.resources.host_component_gpu
import open_aqua.app_compose.generated.resources.host_component_motherboard
import open_aqua.app_compose.generated.resources.host_component_other
import open_aqua.app_compose.generated.resources.host_component_storage
import open_aqua.app_compose.generated.resources.host_no_sensors
import open_aqua.app_compose.generated.resources.host_status_connected
import open_aqua.app_compose.generated.resources.host_status_connecting
import open_aqua.app_compose.generated.resources.host_status_error
import open_aqua.app_compose.generated.resources.host_telemetry_subtitle
import open_aqua.app_compose.generated.resources.host_telemetry_title
import open_aqua.app_compose.generated.resources.sensor_temp_disconnected
import open_aqua.app_compose.generated.resources.sensor_temp_short_value
import org.jetbrains.compose.resources.stringResource

@Composable
fun HostScreen(
    hostState: DeviceState.Host,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Fixed Top Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = metrics.spacingXxl)
                        .padding(top = metrics.spacingXxl, bottom = metrics.spacingLg),
            ) {
                ScreenHeader(
                    title = stringResource(Res.string.host_telemetry_title),
                    subtitle = stringResource(Res.string.host_telemetry_subtitle),
                    trailingContent = {
                        HostStatusBadge(hostState)
                    },
                )
            }
            HorizontalDivider(
                color = colors.borderOutline,
                modifier = Modifier.padding(horizontal = metrics.spacingXxl),
            )
        }

        // Scrollable Body
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .scrollFadingEdge(
                        color = colors.background,
                        scrollState = scrollState,
                        height = 36.dp,
                    ),
        ) {
            when (hostState) {
                DeviceState.Host.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = colors.primaryBright,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
                is DeviceState.Host.Error -> {
                    HostErrorContent(hostState)
                }
                is DeviceState.Host.Connecting -> {
                    HostContent(
                        devices = hostState.info.devices,
                        telemetryMap = emptyMap(),
                        scrollState = scrollState,
                    )
                }
                is DeviceState.Host.Ready -> {
                    val telemetryMap =
                        remember(hostState.telemetry) {
                            hostState.telemetry.sensors
                                .filterIsInstance<HostSensorTelemetry.Connected>()
                                .associate { it.id to it.tempCelsius }
                        }
                    HostContent(
                        devices = hostState.info.devices,
                        telemetryMap = telemetryMap,
                        scrollState = scrollState,
                    )
                }
            }

            if (scrollState.maxValue > 0) {
                OpenAquaVerticalScrollbar(
                    scrollState = scrollState,
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun HostStatusBadge(hostState: DeviceState.Host) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    val (badgeText, statusColor) =
        when (hostState) {
            DeviceState.Host.Loading, is DeviceState.Host.Connecting ->
                stringResource(Res.string.host_status_connecting) to colors.primary
            is DeviceState.Host.Error ->
                stringResource(Res.string.host_status_error) to colors.statusWarning
            is DeviceState.Host.Ready ->
                stringResource(Res.string.host_status_connected) to colors.statusNominal
        }

    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surfaceHigh)
                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor),
        )
        Text(
            text = badgeText,
            color = colors.textPrimary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun HostErrorContent(error: DeviceState.Host.Error) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Box(
        modifier = Modifier.fillMaxSize().padding(metrics.spacingXxl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(metrics.radiusLarge))
                    .background(colors.surfaceLow)
                    .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusLarge))
                    .padding(metrics.spacingXxl),
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = colors.statusWarning,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error.message ?: stringResource(Res.string.host_status_error),
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HostContent(
    devices: List<HostDevice>,
    telemetryMap: Map<HostSensorId, Double>,
    scrollState: ScrollState,
) {
    val metrics = OpenAquaTheme.metrics

    val groupedDevices =
        remember(devices) {
            devices.groupBy { it.type }
        }

    val displayOrder =
        listOf(
            HostDeviceType.CPU to Res.string.host_component_cpu,
            HostDeviceType.GPU to Res.string.host_component_gpu,
            HostDeviceType.MOTHERBOARD to Res.string.host_component_motherboard,
            HostDeviceType.STORAGE to Res.string.host_component_storage,
            HostDeviceType.OTHER to Res.string.host_component_other,
        )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = metrics.spacingXxl)
                .padding(top = metrics.spacingLg, bottom = metrics.spacingXxl),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingXl),
    ) {
        displayOrder.forEach { (type, titleRes) ->
            val devList = groupedDevices[type] ?: emptyList()
            if (devList.isNotEmpty()) {
                HostDeviceTypeSection(
                    sectionTitle = stringResource(titleRes),
                    type = type,
                    devices = devList,
                    telemetryMap = telemetryMap,
                )
            }
        }
    }
}

@Composable
private fun HostDeviceTypeSection(
    sectionTitle: String,
    type: HostDeviceType,
    devices: List<HostDevice>,
    telemetryMap: Map<HostSensorId, Double>,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    val icon: ImageVector =
        when (type) {
            HostDeviceType.CPU -> Icons.Outlined.Memory
            HostDeviceType.GPU -> Icons.Outlined.Tv
            HostDeviceType.MOTHERBOARD -> Icons.Outlined.DeveloperBoard
            HostDeviceType.STORAGE -> Icons.Outlined.Storage
            HostDeviceType.OTHER -> Icons.Outlined.Devices
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(metrics.radiusLarge))
                .background(colors.surfaceLow)
                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusLarge))
                .padding(metrics.spacingXl),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primaryBright,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = sectionTitle,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        devices.forEach { device ->
            HostDeviceCard(
                device = device,
                telemetryMap = telemetryMap,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HostDeviceCard(
    device: HostDevice,
    telemetryMap: Map<HostSensorId, Double>,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(metrics.radiusMedium))
                .background(colors.surfaceContainer)
                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusMedium))
                .padding(metrics.spacingLg),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Device sub-header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = device.name,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                text = device.id.value,
                color = colors.textMuted,
                fontSize = 11.sp,
                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
            )
        }

        if (device.sensors.isEmpty()) {
            Text(
                text = stringResource(Res.string.host_no_sensors),
                color = colors.textMuted,
                fontSize = 12.sp,
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(metrics.spacingMd),
                verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
            ) {
                device.sensors.forEach { sensor ->
                    val temp = telemetryMap[sensor.id]
                    HostSensorTile(
                        sensor = sensor,
                        tempCelsius = temp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HostSensorTile(
    sensor: HostSensorDescriptor,
    tempCelsius: Double?,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    val tempColor = sensorTemperatureColor(tempCelsius)
    val tempText =
        tempCelsius?.let {
            stringResource(Res.string.sensor_temp_short_value, "%.1f".format(it))
        } ?: stringResource(Res.string.sensor_temp_disconnected)

    val fillFraction =
        when (tempCelsius) {
            null -> 0f
            else -> ((tempCelsius - 20.0) / 40.0).toFloat().coerceIn(0f, 1f)
        }

    Column(
        modifier =
            modifier
                .width(180.dp)
                .clip(RoundedCornerShape(metrics.radiusSmall))
                .background(colors.surfaceHigh)
                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusSmall))
                .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = sensor.name,
            color = colors.textSecondary,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = tempText,
            color = if (tempCelsius != null) tempColor else colors.textMuted,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
        )

        Text(
            text = sensor.id.value.substringAfter(":"),
            color = colors.textMuted,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        // Mini temperature bar
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(colors.surfaceContainer),
        ) {
            if (fillFraction > 0f) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(fillFraction)
                            .fillMaxHeight()
                            .background(tempColor),
                )
            }
        }
    }
}
