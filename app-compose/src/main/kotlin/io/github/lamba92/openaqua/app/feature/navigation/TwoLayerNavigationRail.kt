package io.github.lamba92.openaqua.app.feature.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.theme.LocalNavigationRailColors
import io.github.lamba92.openaqua.app.theme.LocalNavigationRailMetrics
import io.github.lamba92.openaqua.app.theme.NavigationRailColors
import io.github.lamba92.openaqua.app.theme.NavigationRailMetrics
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.FeederStatus
import io.github.lamba92.openaqua.app.ui.components.FeederStatusBadge
import io.github.lamba92.openaqua.app.ui.components.OpenAquaVerticalScrollbar
import io.github.lamba92.openaqua.model.device.DeviceId
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.ic_bug_report
import open_aqua.app_compose.generated.resources.ic_computer
import open_aqua.app_compose.generated.resources.ic_device
import open_aqua.app_compose.generated.resources.ic_info
import open_aqua.app_compose.generated.resources.ic_menu
import open_aqua.app_compose.generated.resources.ic_menu_open
import open_aqua.app_compose.generated.resources.ic_openaqua
import open_aqua.app_compose.generated.resources.ic_settings
import open_aqua.app_compose.generated.resources.ic_warning
import open_aqua.app_compose.generated.resources.nav_about
import open_aqua.app_compose.generated.resources.nav_bug_report
import open_aqua.app_compose.generated.resources.nav_collapse_menu
import open_aqua.app_compose.generated.resources.nav_expand_menu
import open_aqua.app_compose.generated.resources.nav_host
import open_aqua.app_compose.generated.resources.nav_settings
import open_aqua.app_compose.generated.resources.permission_loading_desc
import open_aqua.app_compose.generated.resources.permission_warning_tooltip
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

    val drawerItemColors =
        NavigationDrawerItemDefaults.colors(
            selectedContainerColor = colors.itemIndicator,
            unselectedContainerColor = Color.Transparent,
            selectedIconColor = colors.itemSelectedIcon,
            unselectedIconColor = colors.itemUnselectedIcon,
            selectedTextColor = colors.itemSelectedText,
            unselectedTextColor = colors.itemUnselectedText,
        )

    val railWidth by animateDpAsState(
        targetValue = if (state.isExpanded) metrics.expandedWidth else metrics.collapsedWidth,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "NavigationRailWidth",
    )

    Row(modifier = modifier.fillMaxSize()) {
        // LAYER 1: NavigationRail (Host + Devices + Settings/BugReport/About at bottom)
        NavigationRail(
            modifier = Modifier.width(railWidth).fillMaxHeight().clipToBounds(),
            containerColor = colors.container,
            contentColor = colors.itemSelectedText,
            header = {
                NavigationRailHeader(
                    isExpanded = state.isExpanded,
                    onToggleExpand = { onEvent(DeviceNavEvent.ToggleNavigationRailExpanded) },
                    metrics = metrics,
                )
            },
        ) {
            when (val targets = state.targets) {
                NavigationTargets.Loading -> {
                    DeviceNavigationLoadingItem(
                        metrics = metrics,
                        isExpanded = state.isExpanded,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }

                is NavigationTargets.PermissionError -> {
                    DeviceNavigationWarningItem(
                        isSelected = state.selectedItem is DeviceNavState.SelectedItem.PermissionError,
                        onClick = { onEvent(DeviceNavEvent.SelectPermissionError) },
                        metrics = metrics,
                        isExpanded = state.isExpanded,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }

                is NavigationTargets.Ready -> {
                    HostItem(
                        host = targets.host,
                        isSelected = state.selectedItem is DeviceNavState.SelectedItem.Host,
                        metrics = metrics,
                        onEvent = onEvent,
                        railItemColors = railItemColors,
                        drawerItemColors = drawerItemColors,
                        isExpanded = state.isExpanded,
                    )

                    if (targets.devices.isNotEmpty()) {
                        HorizontalDivider(
                            color = OpenAquaTheme.colors.borderOutline,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }

                    DeviceNavigationRailList(
                        devices = targets.devices,
                        devicesWithFeedErrors = state.devicesWithFeedErrors,
                        selectedItem = state.selectedItem,
                        onSelectDevice = { onEvent(DeviceNavEvent.SelectDevice(it)) },
                        railItemColors = railItemColors,
                        drawerItemColors = drawerItemColors,
                        containerColor = colors.container,
                        metrics = metrics,
                        isExpanded = state.isExpanded,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
            }

            // Pinned Footer (Bug Report + About + Settings)
            NavigationRailFooter(
                isBugReportSelected = state.selectedItem is DeviceNavState.SelectedItem.BugReport,
                onBugReportClick = { onEvent(DeviceNavEvent.SelectBugReport) },
                isAboutSelected = state.selectedItem is DeviceNavState.SelectedItem.About,
                onAboutClick = { onEvent(DeviceNavEvent.SelectAbout) },
                isSettingsSelected = state.selectedItem is DeviceNavState.SelectedItem.Settings,
                onSettingsClick = { onEvent(DeviceNavEvent.SelectSettings) },
                railColors = railItemColors,
                drawerColors = drawerItemColors,
                metrics = metrics,
                isExpanded = state.isExpanded,
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            content()
        }
    }
}

@Composable
private fun HostItem(
    host: DeviceState.Host,
    isSelected: Boolean,
    metrics: NavigationRailMetrics,
    onEvent: (DeviceNavEvent) -> Unit,
    railItemColors: NavigationRailItemColors,
    drawerItemColors: NavigationDrawerItemColors,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    when (host) {
        is DeviceState.Host.Loading -> {
            HostLoadingItem(
                metrics = metrics,
                isExpanded = isExpanded,
                modifier = modifier,
            )
        }

        is DeviceState.Host.Connecting,
        is DeviceState.Host.Error,
        is DeviceState.Host.Ready,
        -> {
            HostNavigationRailItem(
                host = host,
                isSelected = isSelected,
                onClick = { onEvent(DeviceNavEvent.SelectHost) },
                railColors = railItemColors,
                drawerColors = drawerItemColors,
                metrics = metrics,
                isExpanded = isExpanded,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun DeviceNavigationRailList(
    devices: List<DeviceState.Aqua>,
    devicesWithFeedErrors: Set<DeviceId>,
    selectedItem: DeviceNavState.SelectedItem?,
    onSelectDevice: (DeviceId) -> Unit,
    railItemColors: NavigationRailItemColors,
    drawerItemColors: NavigationDrawerItemColors,
    containerColor: Color,
    metrics: NavigationRailMetrics,
    isExpanded: Boolean,
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
                    hasError = deviceState.device.id in devicesWithFeedErrors,
                    isSelected =
                        selectedItem is DeviceNavState.SelectedItem.Device &&
                            selectedItem.deviceId == deviceState.device.id,
                    onClick = { onSelectDevice(deviceState.device.id) },
                    railColors = railItemColors,
                    drawerColors = drawerItemColors,
                    metrics = metrics,
                    isExpanded = isExpanded,
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
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    metrics: NavigationRailMetrics,
    modifier: Modifier = Modifier,
) {
    val fixedSlotWidth = metrics.collapsedWidth - 16.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().padding(top = 14.dp, bottom = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.width(fixedSlotWidth),
                contentAlignment = Alignment.Center,
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
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(durationMillis = 200, delayMillis = 50)),
                exit = fadeOut(animationSpec = tween(durationMillis = 150)),
            ) {
                Row(
                    modifier = Modifier.padding(start = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "OpenAqua",
                        color = OpenAquaTheme.colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = onToggleExpand,
                modifier =
                    Modifier
                        .width(fixedSlotWidth)
                        .height(metrics.itemIconSize + 16.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (isExpanded) Res.drawable.ic_menu_open else Res.drawable.ic_menu),
                        contentDescription =
                            stringResource(
                                if (isExpanded) Res.string.nav_collapse_menu else Res.string.nav_expand_menu,
                            ),
                        tint = OpenAquaTheme.colors.textSecondary,
                        modifier = Modifier.size(metrics.itemIconSize),
                    )
                }
            }
        }
    }
}

private data class FooterItemData(
    val icon: Painter,
    val label: String,
    val isSelected: Boolean,
    val onClick: () -> Unit,
)

@Composable
private fun NavigationRailFooter(
    isBugReportSelected: Boolean,
    onBugReportClick: () -> Unit,
    isAboutSelected: Boolean,
    onAboutClick: () -> Unit,
    isSettingsSelected: Boolean,
    onSettingsClick: () -> Unit,
    railColors: NavigationRailItemColors,
    drawerColors: NavigationDrawerItemColors,
    metrics: NavigationRailMetrics,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val items =
        listOf(
            FooterItemData(
                icon = painterResource(Res.drawable.ic_bug_report),
                label = stringResource(Res.string.nav_bug_report),
                isSelected = isBugReportSelected,
                onClick = onBugReportClick,
            ),
            FooterItemData(
                icon = painterResource(Res.drawable.ic_info),
                label = stringResource(Res.string.nav_about),
                isSelected = isAboutSelected,
                onClick = onAboutClick,
            ),
            FooterItemData(
                icon = painterResource(Res.drawable.ic_settings),
                label = stringResource(Res.string.nav_settings),
                isSelected = isSettingsSelected,
                onClick = onSettingsClick,
            ),
        )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HorizontalDivider(
            color = OpenAquaTheme.colors.borderOutline,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        items.forEach { item ->
            SimpleNavigationRailItem(
                icon = item.icon,
                label = item.label,
                isSelected = item.isSelected,
                onClick = item.onClick,
                railColors = railColors,
                drawerColors = drawerColors,
                metrics = metrics,
                isExpanded = isExpanded,
            )
        }
    }
}

@Composable
private fun HostLoadingItem(
    metrics: NavigationRailMetrics,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val fixedSlotWidth = metrics.collapsedWidth - 16.dp

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(metrics.itemIconSize + 16.dp)
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(fixedSlotWidth),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(metrics.itemIconSize),
                strokeWidth = 2.dp,
                color = OpenAquaTheme.colors.primaryBright,
            )
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = tween(durationMillis = 200, delayMillis = 50)),
            exit = fadeOut(animationSpec = tween(durationMillis = 150)),
        ) {
            Row(
                modifier = Modifier.padding(start = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.nav_host),
                    fontSize = metrics.labelTextSize,
                    fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                    color = OpenAquaTheme.colors.textMuted,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DeviceNavigationLoadingItem(
    metrics: NavigationRailMetrics,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val fixedSlotWidth = metrics.collapsedWidth - 16.dp

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(metrics.itemIconSize + 16.dp)
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(fixedSlotWidth),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(metrics.itemIconSize),
                strokeWidth = 2.dp,
                color = OpenAquaTheme.colors.primaryBright,
            )
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = tween(durationMillis = 200, delayMillis = 50)),
            exit = fadeOut(animationSpec = tween(durationMillis = 150)),
        ) {
            Row(
                modifier = Modifier.padding(start = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.permission_loading_desc),
                    fontSize = metrics.labelTextSize,
                    fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                    color = OpenAquaTheme.colors.textMuted,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DeviceNavigationWarningItem(
    isSelected: Boolean,
    onClick: () -> Unit,
    metrics: NavigationRailMetrics,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val fixedSlotWidth = metrics.collapsedWidth - 16.dp
    val warningColor = OpenAquaTheme.colors.statusWarning
    val containerColor =
        if (isSelected) {
            NavigationRailDefaults.colors().itemIndicator
        } else {
            Color.Transparent
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .semantics {
                    role = Role.Tab
                    selected = isSelected
                },
    ) {
        val shape = RoundedCornerShape(10.dp)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(metrics.itemIconSize + 16.dp)
                    .clip(shape)
                    .background(containerColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = warningColor),
                        onClick = onClick,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.width(fixedSlotWidth),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_warning),
                    contentDescription = stringResource(Res.string.permission_warning_tooltip),
                    tint = warningColor,
                    modifier = Modifier.size(metrics.itemIconSize),
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(durationMillis = 200, delayMillis = 50)),
                exit = fadeOut(animationSpec = tween(durationMillis = 150)),
            ) {
                Row(
                    modifier = Modifier.padding(start = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.permission_warning_tooltip),
                        color = warningColor,
                        fontSize = metrics.labelTextSize,
                        fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun BaseNavigationRailItem(
    icon: @Composable () -> Unit,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    railColors: NavigationRailItemColors,
    drawerColors: NavigationDrawerItemColors,
    metrics: NavigationRailMetrics,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val fixedSlotWidth = metrics.collapsedWidth - 16.dp
    val containerColor =
        if (isSelected) {
            NavigationRailDefaults.colors().itemIndicator
        } else {
            Color.Transparent
        }

    val iconColor =
        if (isSelected) {
            NavigationRailDefaults.colors().itemSelectedIcon
        } else {
            NavigationRailDefaults.colors().itemUnselectedIcon
        }

    val textColor =
        if (isSelected) {
            NavigationRailDefaults.colors().itemSelectedText
        } else {
            NavigationRailDefaults.colors().itemUnselectedText
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .semantics {
                    role = Role.Tab
                    selected = isSelected
                },
    ) {
        val shape = RoundedCornerShape(10.dp)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(metrics.itemIconSize + 16.dp)
                    .clip(shape)
                    .background(containerColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = iconColor),
                        onClick = onClick,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.width(fixedSlotWidth),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(durationMillis = 200, delayMillis = 50)),
                exit = fadeOut(animationSpec = tween(durationMillis = 150)),
            ) {
                Row(
                    modifier = Modifier.padding(start = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = metrics.labelTextSize,
                        fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleNavigationRailItem(
    icon: Painter,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    railColors: NavigationRailItemColors,
    drawerColors: NavigationDrawerItemColors,
    metrics: NavigationRailMetrics,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    BaseNavigationRailItem(
        icon = {
            Icon(
                painter = icon,
                contentDescription = label,
                tint = if (isSelected) railColors.selectedIconColor else railColors.unselectedIconColor,
                modifier = Modifier.size(metrics.itemIconSize),
            )
        },
        label = label,
        isSelected = isSelected,
        onClick = onClick,
        railColors = railColors,
        drawerColors = drawerColors,
        metrics = metrics,
        isExpanded = isExpanded,
        modifier = modifier,
    )
}

@Composable
private fun StatusNavigationRailItem(
    icon: Painter,
    label: String,
    status: FeederStatus,
    isSelected: Boolean,
    onClick: () -> Unit,
    railColors: NavigationRailItemColors,
    drawerColors: NavigationDrawerItemColors,
    metrics: NavigationRailMetrics,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    BaseNavigationRailItem(
        icon = {
            Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                    painter = icon,
                    contentDescription = label,
                    tint = if (isSelected) railColors.selectedIconColor else railColors.unselectedIconColor,
                    modifier = Modifier.size(metrics.itemIconSize),
                )
                FeederStatusBadge(
                    status = status,
                    size = metrics.statusDotSize,
                )
            }
        },
        label = label,
        isSelected = isSelected,
        onClick = onClick,
        railColors = railColors,
        drawerColors = drawerColors,
        metrics = metrics,
        isExpanded = isExpanded,
        modifier = modifier,
    )
}

@Composable
private fun HostNavigationRailItem(
    host: DeviceState.Host,
    isSelected: Boolean,
    onClick: () -> Unit,
    railColors: NavigationRailItemColors,
    drawerColors: NavigationDrawerItemColors,
    metrics: NavigationRailMetrics,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val status =
        when (host) {
            is DeviceState.Host.Error -> FeederStatus.ERROR

            is DeviceState.Host.Ready -> FeederStatus.OPERATIONAL

            is DeviceState.Host.Connecting,
            DeviceState.Host.Loading,
            -> FeederStatus.NONE
        }

    StatusNavigationRailItem(
        icon = painterResource(Res.drawable.ic_computer),
        label = stringResource(Res.string.nav_host),
        status = status,
        isSelected = isSelected,
        onClick = onClick,
        railColors = railColors,
        drawerColors = drawerColors,
        metrics = metrics,
        isExpanded = isExpanded,
        modifier = modifier,
    )
}

@Composable
private fun AquaDeviceNavigationRailItem(
    deviceState: DeviceState.Aqua,
    isSelected: Boolean,
    onClick: () -> Unit,
    railColors: NavigationRailItemColors,
    drawerColors: NavigationDrawerItemColors,
    metrics: NavigationRailMetrics,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    hasError: Boolean = false,
) {
    val device = deviceState.device
    val status =
        when {
            hasError || deviceState is DeviceState.Aqua.Error -> FeederStatus.ERROR
            deviceState is DeviceState.Aqua.Ready -> FeederStatus.OPERATIONAL
            else -> FeederStatus.NONE
        }

    StatusNavigationRailItem(
        icon = painterResource(Res.drawable.ic_device),
        label = device.type.name,
        status = status,
        isSelected = isSelected,
        onClick = onClick,
        railColors = railColors,
        drawerColors = drawerColors,
        metrics = metrics,
        isExpanded = isExpanded,
        modifier = modifier,
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
