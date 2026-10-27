package io.github.lamba92.openaqua.app.feature.navigation

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.theme.BorderOutline
import io.github.lamba92.openaqua.app.theme.CyanBright
import io.github.lamba92.openaqua.app.theme.ElectricCyan
import io.github.lamba92.openaqua.app.theme.LocalNavigationRailColors
import io.github.lamba92.openaqua.app.theme.LocalNavigationRailMetrics
import io.github.lamba92.openaqua.app.theme.NavigationRailColors
import io.github.lamba92.openaqua.app.theme.NavigationRailMetrics
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.theme.StatusNominal
import io.github.lamba92.openaqua.app.theme.StatusWarning
import io.github.lamba92.openaqua.app.theme.SurfaceContainerHigh
import io.github.lamba92.openaqua.app.theme.TextPrimary
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.ic_device
import open_aqua.app_compose.generated.resources.ic_openaqua
import open_aqua.app_compose.generated.resources.ic_settings
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
    state: DeviceNavState.Ready,
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
        // LAYER 1: Material 3 NavigationRail (Devices + Settings at bottom)
        NavigationRail(
            modifier = Modifier.width(metrics.width).fillMaxHeight(),
            containerColor = colors.container,
            contentColor = TextPrimary,
            header = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp, bottom = 10.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(metrics.headerLogoSize)
                                .clip(RoundedCornerShape(metrics.headerCornerRadius))
                                .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_openaqua),
                            contentDescription = null,
                            tint = CyanBright,
                            modifier = Modifier.size(metrics.headerIconSize),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        color = BorderOutline,
                        modifier = Modifier.width(metrics.dividerWidth),
                    )
                }
            },
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                state.devices.forEach { deviceState ->
                    val device = deviceState.device
                    val isSelected =
                        state.selectedItem is DeviceNavState.Ready.SelectedItem.Device &&
                            state.selectedItem.deviceId == device.id
                    val statusColor =
                        when (deviceState) {
                            is DeviceState.Connecting -> ElectricCyan
                            is DeviceState.Error -> StatusWarning
                            is DeviceState.Ready -> StatusNominal
                        }
                    NavigationRailItem(
                        modifier = Modifier.fillMaxWidth(),
                        selected = isSelected,
                        onClick = { onEvent(DeviceNavEvent.SelectDevice(device.id)) },
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
                                            .background(statusColor),
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
                        colors = railItemColors,
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 14.dp),
            ) {
                HorizontalDivider(
                    color = BorderOutline,
                    modifier = Modifier.width(metrics.dividerWidth),
                )
                Spacer(modifier = Modifier.height(10.dp))
                val settingsLabel = stringResource(Res.string.nav_settings)
                NavigationRailItem(
                    modifier = Modifier.fillMaxWidth(),
                    selected = state.selectedItem is DeviceNavState.Ready.SelectedItem.Settings,
                    onClick = { onEvent(DeviceNavEvent.SelectSettings) },
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
                    colors = railItemColors,
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            content()
        }
    }
}
