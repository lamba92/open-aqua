package io.github.lamba92.openaqua.app.feature.navigation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.theme.AppColors
import io.github.lamba92.openaqua.app.theme.LocalNavigationRailColors
import io.github.lamba92.openaqua.app.theme.LocalNavigationRailMetrics
import io.github.lamba92.openaqua.app.theme.NavigationRailColors
import io.github.lamba92.openaqua.app.theme.NavigationRailMetrics
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.OpenAquaVerticalScrollbar
import io.github.lamba92.openaqua.model.device.DeviceId
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.ic_computer
import open_aqua.app_compose.generated.resources.ic_device
import open_aqua.app_compose.generated.resources.ic_openaqua
import open_aqua.app_compose.generated.resources.ic_settings
import open_aqua.app_compose.generated.resources.nav_host
import open_aqua.app_compose.generated.resources.nav_settings
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

object NavigationRailDefaults {
    @Composable
    fun colors(): NavigationRailColors = LocalNavigationRailColors.current

    @Composable
    fun metrics(): NavigationRailMetrics = LocalNavigationRailMetrics.current
}

@Composable
fun DeviceNavigationRailScaffold(
    state: DeviceNavState,
    onEvent: (DeviceNavEvent) -> Unit,
    modifier: Modifier = Modifier,
    colors: NavigationRailColors = NavigationRailDefaults.colors(),
    metrics: NavigationRailMetrics = NavigationRailDefaults.metrics(),
    content: @Composable () -> Unit,
) {
    val railItemColors =
        NavigationRailItemDefaults.colors(
            selectedIconColor = colors.itemSelectedIcon,
            selectedTextColor = colors.itemSelectedText,
            indicatorColor = colors.itemIndicator,
            unselectedIconColor = colors.itemUnselectedIcon,
            unselectedTextColor = colors.itemUnselectedText,
        )

    Row(modifier = modifier.fillMaxSize()) {
        // LAYER 1: Material 3 NavigationRail (Host + Devices + Settings at bottom)
        NavigationRail(
            modifier = Modifier.width(metrics.width).fillMaxHeight(),
            containerColor = colors.container,
            contentColor = colors.itemSelectedText,
            header = {
                NavigationRailHeader(metrics = metrics)
            },
        ) {
            // First slot: Pinned Host Machine or Loading Indicator
            HostItem(
                state = state,
                metrics = metrics,
                onEvent = onEvent,
                railItemColors = railItemColors,
            )

            if (state.devices.isNotEmpty()) {
                HorizontalDivider(
                    color = OpenAquaTheme.colors.borderOutline,
                    modifier = Modifier.width(metrics.dividerWidth).padding(vertical = 4.dp),
                )
            }

            DeviceNavigationRailList(
                devices = state.devices,
                selectedItem = state.selectedItem,
                onSelectDevice = { onEvent(DeviceNavEvent.SelectDevice(it)) },
                railItemColors = railItemColors,
                containerColor = colors.container,
                metrics = metrics,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )

            // Pinned Footer (Settings)
            NavigationRailFooter(
                isSelected = state.selectedItem is DeviceNavState.SelectedItem.Settings,
                onClick = { onEvent(DeviceNavEvent.SelectSettings) },
                colors = railItemColors,
                metrics = metrics,
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            content()
        }
    }
}

@Composable
private fun HostItem(
    state: DeviceNavState,
    metrics: NavigationRailMetrics,
    onEvent: (DeviceNavEvent) -> Unit,
    railItemColors: NavigationRailItemColors,
    modifier: Modifier = Modifier,
) {
    when (val host = state.host) {
        is DeviceState.Host.Loading ->
            HostLoadingItem(metrics = metrics, modifier = modifier)

        is DeviceState.Host.Connecting,
        is DeviceState.Host.Error,
        is DeviceState.Host.Ready,
        ->
            HostNavigationRailItem(
                host = host,
                isSelected = state.selectedItem is DeviceNavState.SelectedItem.Host,
                onClick = { onEvent(DeviceNavEvent.SelectHost) },
                colors = railItemColors,
                metrics = metrics,
                modifier = modifier,
            )
    }
}

@Composable
private fun DeviceNavigationRailList(
    devices: List<DeviceState.Aqua>,
    selectedItem: DeviceNavState.SelectedItem?,
    onSelectDevice: (DeviceId) -> Unit,
    railItemColors: NavigationRailItemColors,
    containerColor: Color,
    metrics: NavigationRailMetrics,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        val scrollState = rememberScrollState()

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScrollFadingEdge(
                        color = containerColor,
                        scrollState = scrollState,
                        height = metrics.itemSpacing,
                    ).verticalScroll(scrollState)
                    .padding(vertical = metrics.itemSpacing),
            verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            devices.forEach { deviceState ->
                AquaDeviceNavigationRailItem(
                    deviceState = deviceState,
                    isSelected =
                        selectedItem is DeviceNavState.SelectedItem.Device &&
                            selectedItem.deviceId == deviceState.device.id,
                    onClick = { onSelectDevice(deviceState.device.id) },
                    colors = railItemColors,
                    metrics = metrics,
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

@Composable
private fun NavigationRailHeader(
    metrics: NavigationRailMetrics,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(top = 16.dp, bottom = 10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(metrics.headerLogoSize)
                    .clip(RoundedCornerShape(metrics.headerCornerRadius))
                    .background(OpenAquaTheme.colors.surfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_openaqua),
                contentDescription = null,
                tint = OpenAquaTheme.colors.primaryBright,
                modifier = Modifier.size(metrics.headerIconSize),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(
            color = OpenAquaTheme.colors.borderOutline,
            modifier = Modifier.width(metrics.dividerWidth),
        )
    }
}

@Composable
private fun NavigationRailFooter(
    isSelected: Boolean,
    onClick: () -> Unit,
    colors: NavigationRailItemColors,
    metrics: NavigationRailMetrics,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(bottom = 14.dp),
    ) {
        HorizontalDivider(
            color = OpenAquaTheme.colors.borderOutline,
            modifier = Modifier.width(metrics.dividerWidth),
        )
        Spacer(modifier = Modifier.height(10.dp))
        SettingsNavigationRailItem(
            isSelected = isSelected,
            onClick = onClick,
            colors = colors,
            metrics = metrics,
        )
    }
}

@Composable
private fun HostLoadingItem(
    metrics: NavigationRailMetrics,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(metrics.itemIconSize + 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(metrics.itemIconSize),
            strokeWidth = 2.dp,
            color = OpenAquaTheme.colors.primaryBright,
        )
    }
}

@Composable
private fun HostNavigationRailItem(
    host: DeviceState.Host,
    isSelected: Boolean,
    onClick: () -> Unit,
    colors: NavigationRailItemColors,
    metrics: NavigationRailMetrics,
    modifier: Modifier = Modifier,
) {
    NavigationRailItem(
        modifier = modifier.fillMaxWidth(),
        selected = isSelected,
        onClick = onClick,
        icon = {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    painter = painterResource(Res.drawable.ic_computer),
                    contentDescription = stringResource(Res.string.nav_host),
                    modifier = Modifier.size(metrics.itemIconSize),
                )
                Box(
                    modifier =
                        Modifier
                            .size(metrics.statusDotSize)
                            .clip(CircleShape)
                            .background(host.statusColor(OpenAquaTheme.colors)),
                )
            }
        },
        label = {
            Text(
                text = stringResource(Res.string.nav_host),
                fontSize = metrics.labelTextSize,
                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors = colors,
    )
}

@Composable
private fun AquaDeviceNavigationRailItem(
    deviceState: DeviceState.Aqua,
    isSelected: Boolean,
    onClick: () -> Unit,
    colors: NavigationRailItemColors,
    metrics: NavigationRailMetrics,
    modifier: Modifier = Modifier,
) {
    val device = deviceState.device
    NavigationRailItem(
        modifier = modifier.fillMaxWidth(),
        selected = isSelected,
        onClick = onClick,
        icon = {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    painter = painterResource(Res.drawable.ic_device),
                    contentDescription = device.name,
                    modifier = Modifier.size(metrics.itemIconSize),
                )
                Box(
                    modifier =
                        Modifier
                            .size(metrics.statusDotSize)
                            .clip(CircleShape)
                            .background(deviceState.statusColor(OpenAquaTheme.colors)),
                )
            }
        },
        label = {
            Text(
                text = device.type.name,
                fontSize = metrics.labelTextSize,
                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors = colors,
    )
}

@Composable
private fun SettingsNavigationRailItem(
    isSelected: Boolean,
    onClick: () -> Unit,
    colors: NavigationRailItemColors,
    metrics: NavigationRailMetrics,
    modifier: Modifier = Modifier,
) {
    val settingsLabel = stringResource(Res.string.nav_settings)
    NavigationRailItem(
        modifier = modifier.fillMaxWidth(),
        selected = isSelected,
        onClick = onClick,
        icon = {
            Icon(
                painter = painterResource(Res.drawable.ic_settings),
                contentDescription = settingsLabel,
                modifier = Modifier.size(metrics.itemIconSize),
            )
        },
        label = {
            Text(
                text = settingsLabel,
                fontSize = metrics.labelTextSize,
                fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors = colors,
    )
}

private fun Modifier.verticalScrollFadingEdge(
    color: Color,
    scrollState: ScrollState,
    height: Dp,
): Modifier =
    drawWithContent {
        drawContent()
        val fadeHeightPx = height.toPx()
        if (fadeHeightPx > 0f) {
            val topAlpha = (scrollState.value.toFloat() / (fadeHeightPx / 2f)).coerceIn(0f, 1f)
            val maxScroll = scrollState.maxValue.toFloat()
            val bottomAlpha =
                if (maxScroll > 0f) {
                    ((maxScroll - scrollState.value.toFloat()) / (fadeHeightPx / 2f)).coerceIn(0f, 1f)
                } else {
                    0f
                }

            if (topAlpha > 0f) {
                drawRect(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(color.copy(alpha = topAlpha), Color.Transparent),
                            startY = 0f,
                            endY = fadeHeightPx,
                        ),
                )
            }
            if (bottomAlpha > 0f) {
                drawRect(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, color.copy(alpha = bottomAlpha)),
                            startY = size.height - fadeHeightPx,
                            endY = size.height,
                        ),
                )
            }
        }
    }

private fun DeviceState.Host.statusColor(colors: AppColors): Color =
    when (this) {
        is DeviceState.Host.Connecting -> colors.primary
        is DeviceState.Host.Error -> colors.statusWarning
        is DeviceState.Host.Ready -> colors.statusNominal
        DeviceState.Host.Loading -> colors.primaryBright
    }

private fun DeviceState.Aqua.statusColor(colors: AppColors): Color =
    when (this) {
        is DeviceState.Aqua.Connecting -> colors.primary
        is DeviceState.Aqua.Error -> colors.statusWarning
        is DeviceState.Aqua.Ready -> colors.statusNominal
    }
