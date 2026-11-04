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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.di.rememberInstance
import io.github.lamba92.openaqua.app.model.SensorTargetType
import io.github.lamba92.openaqua.app.model.color
import io.github.lamba92.openaqua.app.model.progressFraction
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.OpenAquaVerticalScrollbar
import io.github.lamba92.openaqua.app.ui.components.ScreenHeader
import io.github.lamba92.openaqua.app.ui.components.SensorTargetTypeMenuButton
import io.github.lamba92.openaqua.app.ui.components.gaugeProgress
import io.github.lamba92.openaqua.app.ui.components.scrollFadingEdge
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
    viewModel: HostViewModel = rememberInstance(),
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val state by viewModel.state.collectAsState()
    HostScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        scrollState = scrollState,
    )
}

@Composable
fun HostScreen(
    state: HostState,
    onEvent: (HostEvent) -> Unit,
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
                        HostStatusBadge(state.hostState)
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
            when (val host = state.hostState) {
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
                    HostErrorContent(host)
                }

                is DeviceState.Host.Connecting -> {
                    HostContent(
                        devices = host.info.devices,
                        telemetryMap = emptyMap(),
                        state = state,
                        onEvent = onEvent,
                        scrollState = scrollState,
                    )
                }

                is DeviceState.Host.Ready -> {
                    val telemetryMap =
                        remember(host.telemetry) {
                            host.telemetry.sensors
                                .filterIsInstance<HostSensorTelemetry.Connected>()
                                .associate { it.id to it.tempCelsius }
                        }
                    HostContent(
                        devices = host.info.devices,
                        telemetryMap = telemetryMap,
                        state = state,
                        onEvent = onEvent,
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
            DeviceState.Host.Loading, is DeviceState.Host.Connecting -> {
                stringResource(Res.string.host_status_connecting) to colors.primary
            }

            is DeviceState.Host.Error -> {
                stringResource(Res.string.host_status_error) to colors.statusWarning
            }

            is DeviceState.Host.Ready -> {
                stringResource(Res.string.host_status_connected) to colors.statusNominal
            }
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
    state: HostState,
    onEvent: (HostEvent) -> Unit,
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
                    state = state,
                    onEvent = onEvent,
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
    state: HostState,
    onEvent: (HostEvent) -> Unit,
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
                state = state,
                onEvent = onEvent,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HostDeviceCard(
    device: HostDevice,
    telemetryMap: Map<HostSensorId, Double>,
    state: HostState,
    onEvent: (HostEvent) -> Unit,
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
                fontSize = 13.sp,
                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
            )
        }

        if (device.sensors.isEmpty()) {
            Text(
                text = stringResource(Res.string.host_no_sensors),
                color = colors.textMuted,
                fontSize = 14.sp,
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(metrics.spacingMd),
                verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
            ) {
                device.sensors.forEach { sensor ->
                    val temp = telemetryMap[sensor.id]
                    val targetType = state.targetTypeFor(sensor.id)
                    HostSensorTile(
                        sensor = sensor,
                        tempCelsius = temp,
                        targetType = targetType,
                        onSetTargetType = { newType ->
                            onEvent(HostEvent.SetSensorTargetType(sensor.id, newType))
                        },
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
    targetType: SensorTargetType,
    onSetTargetType: (SensorTargetType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    val tempColor = targetType.color(tempCelsius)
    val isDisconnected = tempCelsius == null
    val fillFraction = targetType.progressFraction(tempCelsius)
    val tempText =
        tempCelsius?.let {
            stringResource(Res.string.sensor_temp_short_value, "%.1f".format(it))
        } ?: stringResource(Res.string.sensor_temp_disconnected)

    Box(
        modifier =
            modifier
                .width(280.dp)
                .height(100.dp)
                .clip(RoundedCornerShape(metrics.radiusMedium))
                .background(colors.surfaceHigh)
                .gaugeProgress(
                    fraction = fillFraction,
                    color = tempColor,
                    leadingEdgeAlpha = 0.65f,
                ).border(
                    metrics.strokeThin,
                    colors.borderOutline,
                    RoundedCornerShape(metrics.radiusMedium),
                ).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top row: Sensor Name (flexible, up to 2 lines) + 3-dots kebab menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = sensor.name,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                )

                // 3-dots button (kebab menu)
                SensorTargetTypeMenuButton(
                    targetType = targetType,
                    onSetTargetType = onSetTargetType,
                )
            }

            // Bottom row: Temperature Reading + Sensor ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = tempText,
                    color = if (!isDisconnected) tempColor else colors.textMuted,
                    fontSize = 19.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = sensor.id.value.substringAfter(":"),
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
