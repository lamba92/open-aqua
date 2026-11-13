package io.github.lamba92.openaqua.app.feature.host

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.di.rememberInstance
import io.github.lamba92.openaqua.app.feature.host.fans.HostFansTabContent
import io.github.lamba92.openaqua.app.model.HostTab
import io.github.lamba92.openaqua.app.model.SensorTargetType
import io.github.lamba92.openaqua.app.model.color
import io.github.lamba92.openaqua.app.model.progressFraction
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.CenteredScreenCard
import io.github.lamba92.openaqua.app.ui.components.OpenAquaVerticalScrollbar
import io.github.lamba92.openaqua.app.ui.components.ScreenHeaderSection
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
import open_aqua.app_compose.generated.resources.host_component_ram
import open_aqua.app_compose.generated.resources.host_component_storage
import open_aqua.app_compose.generated.resources.host_no_sensors
import open_aqua.app_compose.generated.resources.host_status_connected
import open_aqua.app_compose.generated.resources.host_status_connecting
import open_aqua.app_compose.generated.resources.host_status_error
import open_aqua.app_compose.generated.resources.host_telemetry_subtitle
import open_aqua.app_compose.generated.resources.host_telemetry_title
import open_aqua.app_compose.generated.resources.ic_fan
import open_aqua.app_compose.generated.resources.ic_sensor
import open_aqua.app_compose.generated.resources.nav_fans
import open_aqua.app_compose.generated.resources.nav_host
import open_aqua.app_compose.generated.resources.nav_sensors
import open_aqua.app_compose.generated.resources.sensor_temp_disconnected
import open_aqua.app_compose.generated.resources.sensor_temp_short_value
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val HostDrawerLineHeightStyle =
    LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    )

@Composable
fun HostScreen(
    selectedTab: HostTab = HostTab.SENSORS,
    onSelectTab: (HostTab) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HostViewModel = rememberInstance(),
) {
    val state by viewModel.state.collectAsState()
    val saveableStateHolder = rememberSaveableStateHolder()
    val sensorsScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val fansScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }

    HostDrawerScaffold(
        selectedTab = selectedTab,
        onSelectTab = onSelectTab,
        modifier = modifier,
    ) {
        saveableStateHolder.SaveableStateProvider(selectedTab) {
            when (selectedTab) {
                HostTab.SENSORS -> {
                    HostSensorsContent(
                        state = state,
                        onEvent = viewModel::onEvent,
                        scrollState = sensorsScrollState,
                    )
                }

                HostTab.FANS -> {
                    HostFansTabContent(
                        scrollState = fansScrollState,
                    )
                }
            }
        }
    }
}

@Composable
fun HostSensorsContent(
    state: HostState,
    onEvent: (HostEvent) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val colors = OpenAquaTheme.colors

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Fixed Top Header
        ScreenHeaderSection(
            title = stringResource(Res.string.host_telemetry_title),
            subtitle = stringResource(Res.string.host_telemetry_subtitle),
            trailingContent = {
                HostStatusBadge(state.hostState)
            },
        )

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

    CenteredScreenCard {
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
            HostDeviceType.RAM to Res.string.host_component_ram,
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
            HostDeviceType.RAM -> Icons.Outlined.Memory
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

@Composable
fun HostDrawerScaffold(
    selectedTab: HostTab,
    onSelectTab: (HostTab) -> Unit,
    modifier: Modifier = Modifier,
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
                    Text(
                        text = stringResource(Res.string.nav_host),
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    Text(
                        text = "Local System",
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

                    HostDrawerTabItem(
                        label = stringResource(Res.string.nav_fans),
                        icon = painterResource(Res.drawable.ic_fan),
                        selected = selectedTab == HostTab.FANS,
                        onClick = { onSelectTab(HostTab.FANS) },
                        colors = drawerColors,
                    )

                    HostDrawerTabItem(
                        label = stringResource(Res.string.nav_sensors),
                        icon = painterResource(Res.drawable.ic_sensor),
                        selected = selectedTab == HostTab.SENSORS,
                        onClick = { onSelectTab(HostTab.SENSORS) },
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
private fun HostDrawerTabItem(
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
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                style = LocalTextStyle.current.copy(lineHeightStyle = HostDrawerLineHeightStyle),
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
        selected = selected,
        onClick = onClick,
        colors = colors,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
    )
}
