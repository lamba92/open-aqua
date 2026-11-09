package io.github.lamba92.openaqua.app.feature.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.DeviceIllustration
import io.github.lamba92.openaqua.app.ui.components.OpenAquaVerticalScrollbar
import io.github.lamba92.openaqua.app.ui.components.ScreenHeaderSection
import io.github.lamba92.openaqua.app.ui.components.drawable
import io.github.lamba92.openaqua.app.ui.components.scrollFadingEdge
import io.github.lamba92.openaqua.model.device.DeviceCapability
import io.github.lamba92.openaqua.model.device.DeviceDescriptor
import io.github.lamba92.openaqua.model.device.DeviceProtocol
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import io.github.lamba92.openaqua.model.device.DeviceType
import io.github.lamba92.openaqua.model.device.DisplayKind
import io.github.lamba92.openaqua.model.device.findCapability
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.ic_computer
import open_aqua.app_compose.generated.resources.ic_device
import open_aqua.app_compose.generated.resources.ic_fan
import open_aqua.app_compose.generated.resources.ic_memory
import open_aqua.app_compose.generated.resources.ic_sensor
import open_aqua.app_compose.generated.resources.ic_sensors
import open_aqua.app_compose.generated.resources.ic_speed
import open_aqua.app_compose.generated.resources.ic_tune
import open_aqua.app_compose.generated.resources.ic_warning
import open_aqua.app_compose.generated.resources.supported_devices_filter_all
import open_aqua.app_compose.generated.resources.supported_devices_filter_fans
import open_aqua.app_compose.generated.resources.supported_devices_filter_lighting
import open_aqua.app_compose.generated.resources.supported_devices_filter_pumps
import open_aqua.app_compose.generated.resources.supported_devices_filter_sensors
import open_aqua.app_compose.generated.resources.supported_devices_no_results
import open_aqua.app_compose.generated.resources.supported_devices_screen_subtitle
import open_aqua.app_compose.generated.resources.supported_devices_screen_title
import open_aqua.app_compose.generated.resources.supported_devices_search_placeholder
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

enum class SupportedDeviceCategory {
    ALL,
    FANS,
    PUMPS,
    SENSORS,
    LIGHTING,
}

fun DeviceDescriptor.getCategory(): SupportedDeviceCategory =
    when (type) {
        DeviceType.OCTO,
        DeviceType.QUADRO,
        DeviceType.AQUAERO,
        DeviceType.POWERADJUST3,
        -> SupportedDeviceCategory.FANS

        DeviceType.D5_NEXT,
        DeviceType.AQUASTREAM_ULTIMATE,
        DeviceType.AQUASTREAM_XT,
        -> SupportedDeviceCategory.PUMPS

        DeviceType.HIGH_FLOW_NEXT,
        DeviceType.LEAKSHIELD,
        DeviceType.HIGH_FLOW_USB,
        -> SupportedDeviceCategory.SENSORS

        DeviceType.FARBWERK360,
        DeviceType.FARBWERK,
        -> SupportedDeviceCategory.LIGHTING
    }

fun DeviceDescriptor.getCategoryLabel(): String =
    when (getCategory()) {
        SupportedDeviceCategory.FANS -> "Fan & Pump Controller"
        SupportedDeviceCategory.PUMPS -> "Smart Pump"
        SupportedDeviceCategory.SENSORS -> "Sensor & System Protection"
        SupportedDeviceCategory.LIGHTING -> "Lighting Controller"
        SupportedDeviceCategory.ALL -> "Device"
    }

fun DeviceDescriptor.getDescription(): String =
    when (type) {
        DeviceType.OCTO -> {
            "Autonomous 8-channel PWM fan controller with calibrated flow sensor input, 4 physical NTC inputs, 16 virtual sensors, and 2 RGBpx ports."
        }

        DeviceType.QUADRO -> {
            "Autonomous 4-channel PWM fan controller with calibrated flow sensor input, 4 physical NTC inputs, 16 virtual sensors, and 1 RGBpx port."
        }

        DeviceType.D5_NEXT -> {
            "High-performance D5 water pump with integrated OLED graphic display, coolant temperature sensor, auxiliary PWM fan channel, RGBpx, and virtual sensor telemetry."
        }

        DeviceType.FARBWERK360 -> {
            "4-port addressable RGBpx lighting controller with 4 physical temperature sensor inputs, 16 virtual sensors, and support for up to 360 addressable LEDs."
        }

        DeviceType.FARBWERK -> {
            "Classic 4-channel 12V analog RGB lighting controller with 4 physical NTC temperature sensor inputs."
        }

        DeviceType.HIGH_FLOW_NEXT -> {
            "High-precision flow sensor with integrated water temperature monitoring, coolant conductivity/quality evaluation, OLED graphic display, and RGBpx optical alarm."
        }

        DeviceType.LEAKSHIELD -> {
            "Active leak prevention system with integrated vacuum pump, membrane pressure sensor, automatic deaeration, OLED graphic display, and alarm buzzer."
        }

        DeviceType.AQUASTREAM_ULTIMATE -> {
            "High-end water cooling pump with integrated OLED graphic display, PWM fan output, integrated coolant temperature sensor, and USB telemetry."
        }

        DeviceType.AQUAERO -> {
            "Flagship multi-channel system controller (Aquaero 5 & 6) with graphic display, multi-channel fan regulation, 8 temperature probe inputs, and dual flow inputs."
        }

        DeviceType.HIGH_FLOW_USB -> {
            "Classic USB flow sensor interface with calibrated rotor telemetry and physical water temperature input."
        }

        DeviceType.POWERADJUST3 -> {
            "Ultra high-power single-channel fan and pump controller (up to 30W) with temperature sensor input and flow pulse telemetry."
        }

        DeviceType.AQUASTREAM_XT -> {
            "Classic USB-controlled water cooling pump with integrated electronics, fan channel output, coolant temperature sensor, and flow input."
        }
    }

@Composable
fun SupportedDevicesContent(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    var selectedCategory by remember { mutableStateOf(SupportedDeviceCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val allDevices = remember { DeviceRegistry.ALL }

    val filteredDevices =
        remember(allDevices, selectedCategory, searchQuery) {
            allDevices.filter { descriptor ->
                val matchesCategory =
                    when (selectedCategory) {
                        SupportedDeviceCategory.ALL -> true
                        else -> descriptor.getCategory() == selectedCategory
                    }

                val query = searchQuery.trim().lowercase()
                val matchesSearch =
                    if (query.isEmpty()) {
                        true
                    } else {
                        val pidHex = descriptor.productId.toString(16).lowercase()
                        val pidFull = "0x$pidHex"
                        descriptor.name.lowercase().contains(query) ||
                            pidHex.contains(query) ||
                            pidFull.contains(query) ||
                            descriptor.getDescription().lowercase().contains(query) ||
                            descriptor.getCategoryLabel().lowercase().contains(query)
                    }

                matchesCategory && matchesSearch
            }
        }

    val fanCount = remember(allDevices) { allDevices.count { it.getCategory() == SupportedDeviceCategory.FANS } }
    val pumpCount = remember(allDevices) { allDevices.count { it.getCategory() == SupportedDeviceCategory.PUMPS } }
    val sensorCount = remember(allDevices) { allDevices.count { it.getCategory() == SupportedDeviceCategory.SENSORS } }
    val lightingCount = remember(allDevices) { allDevices.count { it.getCategory() == SupportedDeviceCategory.LIGHTING } }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            ScreenHeaderSection(
                title = stringResource(Res.string.supported_devices_screen_title),
                subtitle = stringResource(Res.string.supported_devices_screen_subtitle),
            )

            // Search & Category Filters
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = metrics.spacingXxl)
                        .padding(top = metrics.spacingMd, bottom = metrics.spacingMd),
                verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = stringResource(Res.string.supported_devices_search_placeholder),
                            color = colors.textMuted,
                            fontSize = 14.sp,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "Clear search",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(metrics.radiusMedium),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primaryBright,
                            unfocusedBorderColor = colors.borderOutline,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            cursorColor = colors.primaryBright,
                            focusedContainerColor = colors.surfaceLow,
                            unfocusedContainerColor = colors.surfaceLow,
                        ),
                )

                // Category filter chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
                    verticalArrangement = Arrangement.spacedBy(metrics.spacingSm),
                ) {
                    CategoryFilterChip(
                        label = stringResource(Res.string.supported_devices_filter_all, allDevices.size),
                        isSelected = selectedCategory == SupportedDeviceCategory.ALL,
                        onClick = { selectedCategory = SupportedDeviceCategory.ALL },
                    )
                    CategoryFilterChip(
                        label = stringResource(Res.string.supported_devices_filter_fans, fanCount),
                        isSelected = selectedCategory == SupportedDeviceCategory.FANS,
                        onClick = { selectedCategory = SupportedDeviceCategory.FANS },
                    )
                    CategoryFilterChip(
                        label = stringResource(Res.string.supported_devices_filter_pumps, pumpCount),
                        isSelected = selectedCategory == SupportedDeviceCategory.PUMPS,
                        onClick = { selectedCategory = SupportedDeviceCategory.PUMPS },
                    )
                    CategoryFilterChip(
                        label = stringResource(Res.string.supported_devices_filter_sensors, sensorCount),
                        isSelected = selectedCategory == SupportedDeviceCategory.SENSORS,
                        onClick = { selectedCategory = SupportedDeviceCategory.SENSORS },
                    )
                    CategoryFilterChip(
                        label = stringResource(Res.string.supported_devices_filter_lighting, lightingCount),
                        isSelected = selectedCategory == SupportedDeviceCategory.LIGHTING,
                        onClick = { selectedCategory = SupportedDeviceCategory.LIGHTING },
                    )
                }
            }

            // Device Cards List
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
                if (filteredDevices.isEmpty()) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(metrics.spacingXxl),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.supported_devices_no_results, searchQuery),
                            color = colors.textSecondary,
                            fontSize = 15.sp,
                        )
                    }
                } else {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(horizontal = metrics.spacingXxl)
                                .padding(top = metrics.spacingSm, bottom = metrics.spacingXxl),
                        verticalArrangement = Arrangement.spacedBy(metrics.spacingLg),
                    ) {
                        filteredDevices.forEach { descriptor ->
                            SupportedDeviceCard(descriptor = descriptor)
                        }
                    }
                }

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
private fun CategoryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(metrics.radiusMedium))
                .background(if (isSelected) colors.surfaceHigh else colors.surfaceLow)
                .border(
                    metrics.strokeThin,
                    if (isSelected) colors.primaryBright else colors.borderOutline,
                    RoundedCornerShape(metrics.radiusMedium),
                ).clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isSelected) colors.primaryBright else colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun SupportedDeviceCard(
    descriptor: DeviceDescriptor,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(metrics.radiusLarge))
                .background(colors.surfaceLow)
                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusLarge))
                .padding(metrics.spacingXl),
        verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(metrics.spacingLg),
            verticalAlignment = Alignment.Top,
        ) {
            // Device illustration box
            Box(
                modifier =
                    Modifier
                        .size(width = 170.dp, height = 120.dp)
                        .clip(RoundedCornerShape(metrics.radiusMedium))
                        .background(colors.surfaceContainer)
                        .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusMedium))
                        .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(descriptor.type.drawable),
                    contentDescription = descriptor.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }

            // Info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Header with name and PID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = descriptor.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = descriptor.getCategoryLabel(),
                            fontSize = 12.sp,
                            color = colors.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    // Product ID Chip
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(metrics.radiusSmall))
                                .background(colors.surfaceContainer)
                                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusSmall))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "0x0C70 : 0x${descriptor.productId.toString(16).uppercase()}",
                            fontSize = 12.sp,
                            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
                            color = colors.primaryBright,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                // Description
                Text(
                    text = descriptor.getDescription(),
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Capabilities badges
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(metrics.spacingSm),
                    verticalArrangement = Arrangement.spacedBy(metrics.spacingSm),
                ) {
                    descriptor.findCapability<DeviceCapability.Fans>()?.let { fans ->
                        val fanLabel =
                            when (descriptor.type) {
                                DeviceType.D5_NEXT -> "1x D5 Pump + 1x PWM Fan"
                                DeviceType.POWERADJUST3 -> "1x High-Power Channel (30W)"
                                else -> "${fans.channels.size} PWM Fan Channels"
                            }
                        CapabilityChip(
                            icon = painterResource(Res.drawable.ic_fan),
                            text = fanLabel,
                        )
                    }

                    descriptor.findCapability<DeviceCapability.TemperatureSensors>()?.let { temps ->
                        CapabilityChip(
                            icon = painterResource(Res.drawable.ic_sensor),
                            text = "${temps.sensors.size} Temp Inputs",
                        )
                    }

                    descriptor.findCapability<DeviceCapability.FlowSensor>()?.let {
                        val flowLabel =
                            if (descriptor.type == DeviceType.HIGH_FLOW_NEXT) {
                                "High-Precision Flow Sensor"
                            } else {
                                "Calibrated Flow Input"
                            }
                        CapabilityChip(
                            icon = painterResource(Res.drawable.ic_speed),
                            text = flowLabel,
                        )
                    }

                    descriptor.findCapability<DeviceCapability.VirtualSensors>()?.let { virtuals ->
                        CapabilityChip(
                            icon = painterResource(Res.drawable.ic_sensors),
                            text = "${virtuals.count} Virtual Sensors",
                        )
                    }

                    descriptor.findCapability<DeviceCapability.Lighting.Rgbpx>()?.let { rgbpx ->
                        val totalLeds = rgbpx.portCount * rgbpx.maxLedsPerPort
                        CapabilityChip(
                            icon = painterResource(Res.drawable.ic_tune),
                            text = "${rgbpx.portCount}x RGBpx ($totalLeds LEDs)",
                        )
                    }

                    descriptor.findCapability<DeviceCapability.Lighting.ClassicAnalogRgb>()?.let { analog ->
                        CapabilityChip(
                            icon = painterResource(Res.drawable.ic_tune),
                            text = "${analog.channelCount}x 12V RGB Channels",
                        )
                    }

                    descriptor.findCapability<DeviceCapability.Display>()?.let { display ->
                        val displayStr =
                            when (display.kind) {
                                DisplayKind.OLED -> "OLED Graphic Display"
                                DisplayKind.LCD -> "LCD Graphic Display"
                            }
                        CapabilityChip(
                            icon = painterResource(Res.drawable.ic_computer),
                            text = displayStr,
                        )
                    }

                    descriptor.findCapability<DeviceCapability.CoolantQuality>()?.let {
                        CapabilityChip(
                            icon = painterResource(Res.drawable.ic_sensor),
                            text = "Conductivity & Coolant Quality",
                        )
                    }

                    descriptor.findCapability<DeviceCapability.Pressure>()?.let {
                        CapabilityChip(
                            icon = painterResource(Res.drawable.ic_warning),
                            text = "Vacuum Pump & Deaeration",
                        )
                    }

                    val protocolLabel =
                        if (descriptor.protocol is DeviceProtocol.Flashable) {
                            "Flashable EEPROM"
                        } else {
                            "Legacy HID Protocol"
                        }
                    CapabilityChip(
                        icon = painterResource(Res.drawable.ic_memory),
                        text = protocolLabel,
                    )
                }
            }
        }
    }
}

@Composable
private fun CapabilityChip(
    icon: Painter,
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(metrics.radiusSmall))
                .background(colors.surfaceContainer)
                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusSmall))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = colors.primaryBright,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            color = colors.textPrimary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
