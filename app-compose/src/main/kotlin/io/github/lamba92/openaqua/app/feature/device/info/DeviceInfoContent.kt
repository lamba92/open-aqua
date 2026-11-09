package io.github.lamba92.openaqua.app.feature.device.info

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.theme.OpenAquaTheme
import io.github.lamba92.openaqua.app.ui.components.DeviceIllustration
import io.github.lamba92.openaqua.app.ui.components.OpenAquaVerticalScrollbar
import io.github.lamba92.openaqua.app.ui.components.ScreenHeaderSection
import io.github.lamba92.openaqua.app.ui.components.SectionHeader
import io.github.lamba92.openaqua.app.ui.components.scrollFadingEdge
import io.github.lamba92.openaqua.model.device.DeviceCapability
import io.github.lamba92.openaqua.model.device.DeviceProtocol
import io.github.lamba92.openaqua.model.device.DisplayKind
import io.github.lamba92.openaqua.model.device.findCapability
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.device_identification_title
import open_aqua.app_compose.generated.resources.device_telemetry_identity_subtitle
import open_aqua.app_compose.generated.resources.hardware_descriptors_title
import open_aqua.app_compose.generated.resources.ic_memory
import open_aqua.app_compose.generated.resources.ic_usb
import open_aqua.app_compose.generated.resources.info_device_architecture
import open_aqua.app_compose.generated.resources.info_device_name
import open_aqua.app_compose.generated.resources.info_firmware_version
import open_aqua.app_compose.generated.resources.info_firmware_version_value
import open_aqua.app_compose.generated.resources.info_hardware_alias
import open_aqua.app_compose.generated.resources.info_not_available
import open_aqua.app_compose.generated.resources.info_serial_number
import open_aqua.app_compose.generated.resources.info_unique_id
import open_aqua.app_compose.generated.resources.info_unknown
import open_aqua.app_compose.generated.resources.spec_analog_rgb
import open_aqua.app_compose.generated.resources.spec_analog_rgb_subtitle
import open_aqua.app_compose.generated.resources.spec_analog_rgb_value
import open_aqua.app_compose.generated.resources.spec_coolant_quality
import open_aqua.app_compose.generated.resources.spec_coolant_quality_subtitle
import open_aqua.app_compose.generated.resources.spec_coolant_quality_supported
import open_aqua.app_compose.generated.resources.spec_display
import open_aqua.app_compose.generated.resources.spec_display_lcd
import open_aqua.app_compose.generated.resources.spec_display_oled
import open_aqua.app_compose.generated.resources.spec_display_subtitle
import open_aqua.app_compose.generated.resources.spec_fan_channels
import open_aqua.app_compose.generated.resources.spec_fan_channels_subtitle
import open_aqua.app_compose.generated.resources.spec_fan_channels_value
import open_aqua.app_compose.generated.resources.spec_flow_sensor
import open_aqua.app_compose.generated.resources.spec_flow_sensor_subtitle
import open_aqua.app_compose.generated.resources.spec_flow_sensor_supported
import open_aqua.app_compose.generated.resources.spec_pressure
import open_aqua.app_compose.generated.resources.spec_pressure_subtitle
import open_aqua.app_compose.generated.resources.spec_pressure_supported
import open_aqua.app_compose.generated.resources.spec_rgbpx_ports
import open_aqua.app_compose.generated.resources.spec_rgbpx_ports_subtitle
import open_aqua.app_compose.generated.resources.spec_rgbpx_ports_value
import open_aqua.app_compose.generated.resources.spec_temp_probes
import open_aqua.app_compose.generated.resources.spec_temp_probes_subtitle
import open_aqua.app_compose.generated.resources.spec_temp_probes_value
import open_aqua.app_compose.generated.resources.spec_usb_protocol
import open_aqua.app_compose.generated.resources.spec_usb_protocol_subtitle
import open_aqua.app_compose.generated.resources.spec_usb_protocol_value
import open_aqua.app_compose.generated.resources.spec_virtual_sensors
import open_aqua.app_compose.generated.resources.spec_virtual_sensors_subtitle
import open_aqua.app_compose.generated.resources.spec_virtual_sensors_value
import open_aqua.app_compose.generated.resources.usb_hid_connected
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeviceInfoContent(
    deviceState: DeviceState.Aqua,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val device = deviceState.device
    val descriptor = deviceState.descriptor
    val notAvailable = stringResource(Res.string.info_not_available)
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Top-level fixed header
        ScreenHeaderSection(
            title = device.name,
            subtitle = stringResource(Res.string.device_telemetry_identity_subtitle),
            trailingContent = {
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
                                .background(colors.statusNominal),
                    )
                    Text(
                        text = stringResource(Res.string.usb_hid_connected),
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            },
        )

        // Scrolling content with internal padding and top fading edge
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
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = metrics.spacingXxl)
                        .padding(top = metrics.spacingLg, bottom = metrics.spacingXxl),
                verticalArrangement = Arrangement.spacedBy(metrics.spacingXl),
            ) {
                // Image / Visual Representation and High-Level Specs
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(metrics.spacingXl),
                ) {
                    DeviceIllustration(
                        type = device.type,
                        modifier =
                            Modifier
                                .weight(0.42f)
                                .fillMaxHeight(),
                    )

                    Column(
                        modifier =
                            Modifier
                                .weight(0.58f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(metrics.radiusLarge))
                                .background(colors.surfaceLow)
                                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusLarge))
                                .padding(metrics.spacingXl),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        SectionHeader(
                            icon = painterResource(Res.drawable.ic_memory),
                            title = stringResource(Res.string.device_identification_title),
                        )

                        InfoRow(label = stringResource(Res.string.info_device_name), value = device.name)
                        InfoRow(
                            label = stringResource(Res.string.info_hardware_alias),
                            value = device.alias.value,
                        )
                        InfoRow(
                            label = stringResource(Res.string.info_serial_number),
                            value = device.serial ?: notAvailable,
                        )
                        InfoRow(label = stringResource(Res.string.info_unique_id), value = device.id.value)
                        InfoRow(
                            label = stringResource(Res.string.info_firmware_version),
                            value =
                                device.firmwareVersion?.let {
                                    stringResource(Res.string.info_firmware_version_value, it)
                                } ?: stringResource(Res.string.info_unknown),
                        )
                        InfoRow(label = stringResource(Res.string.info_device_architecture), value = device.type.name)
                    }
                }

                // Hardware Capabilities & Descriptors Section
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(metrics.radiusLarge))
                            .background(colors.surfaceLow)
                            .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusLarge))
                            .padding(metrics.spacingXl),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SectionHeader(
                        icon = painterResource(Res.drawable.ic_usb),
                        title = stringResource(Res.string.hardware_descriptors_title),
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.spacingMd),
                        verticalArrangement = Arrangement.spacedBy(metrics.spacingMd),
                    ) {
                        descriptor.findCapability<DeviceCapability.Fans>()?.let { fans ->
                            SpecCard(
                                title = stringResource(Res.string.spec_fan_channels),
                                value = stringResource(Res.string.spec_fan_channels_value, fans.channels.size),
                                subtitle = stringResource(Res.string.spec_fan_channels_subtitle),
                            )
                        }
                        descriptor.findCapability<DeviceCapability.TemperatureSensors>()?.let { temps ->
                            SpecCard(
                                title = stringResource(Res.string.spec_temp_probes),
                                value = stringResource(Res.string.spec_temp_probes_value, temps.sensors.size),
                                subtitle = stringResource(Res.string.spec_temp_probes_subtitle),
                            )
                        }
                        descriptor.findCapability<DeviceCapability.VirtualSensors>()?.let { virtuals ->
                            SpecCard(
                                title = stringResource(Res.string.spec_virtual_sensors),
                                value = stringResource(Res.string.spec_virtual_sensors_value, virtuals.count),
                                subtitle = stringResource(Res.string.spec_virtual_sensors_subtitle),
                            )
                        }
                        descriptor.findCapability<DeviceCapability.Lighting.Rgbpx>()?.let { rgbpx ->
                            SpecCard(
                                title = stringResource(Res.string.spec_rgbpx_ports),
                                value = stringResource(Res.string.spec_rgbpx_ports_value, rgbpx.portCount),
                                subtitle = stringResource(Res.string.spec_rgbpx_ports_subtitle),
                            )
                        }
                        descriptor.findCapability<DeviceCapability.Lighting.ClassicAnalogRgb>()?.let { analog ->
                            SpecCard(
                                title = stringResource(Res.string.spec_analog_rgb),
                                value = stringResource(Res.string.spec_analog_rgb_value, analog.channelCount),
                                subtitle = stringResource(Res.string.spec_analog_rgb_subtitle),
                            )
                        }
                        descriptor.findCapability<DeviceCapability.FlowSensor>()?.let {
                            SpecCard(
                                title = stringResource(Res.string.spec_flow_sensor),
                                value = stringResource(Res.string.spec_flow_sensor_supported),
                                subtitle = stringResource(Res.string.spec_flow_sensor_subtitle),
                            )
                        }
                        descriptor.findCapability<DeviceCapability.Display>()?.let { display ->
                            val displayKindStr =
                                when (display.kind) {
                                    DisplayKind.OLED -> stringResource(Res.string.spec_display_oled)
                                    DisplayKind.LCD -> stringResource(Res.string.spec_display_lcd)
                                }
                            SpecCard(
                                title = stringResource(Res.string.spec_display),
                                value = displayKindStr,
                                subtitle = stringResource(Res.string.spec_display_subtitle),
                            )
                        }
                        descriptor.findCapability<DeviceCapability.CoolantQuality>()?.let {
                            SpecCard(
                                title = stringResource(Res.string.spec_coolant_quality),
                                value = stringResource(Res.string.spec_coolant_quality_supported),
                                subtitle = stringResource(Res.string.spec_coolant_quality_subtitle),
                            )
                        }
                        descriptor.findCapability<DeviceCapability.Pressure>()?.let {
                            SpecCard(
                                title = stringResource(Res.string.spec_pressure),
                                value = stringResource(Res.string.spec_pressure_supported),
                                subtitle = stringResource(Res.string.spec_pressure_subtitle),
                            )
                        }
                        val configLength = (descriptor.protocol as? DeviceProtocol.Flashable)?.configReportLength ?: 0
                        SpecCard(
                            title = stringResource(Res.string.spec_usb_protocol),
                            value =
                                stringResource(
                                    Res.string.spec_usb_protocol_value,
                                    descriptor.productId.toString(16).uppercase(),
                                ),
                            subtitle =
                                stringResource(
                                    Res.string.spec_usb_protocol_subtitle,
                                    descriptor.protocol.statusReportLength,
                                    configLength,
                                ),
                        )
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

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    val colors = OpenAquaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
        Text(
            text = value,
            color = colors.textPrimary,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
        )
    }
}

@Composable
private fun SpecCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val colors = OpenAquaTheme.colors
    val metrics = OpenAquaTheme.metrics
    Column(
        modifier =
            modifier
                .width(240.dp)
                .clip(RoundedCornerShape(metrics.radiusSmall))
                .background(colors.surfaceContainer)
                .border(metrics.strokeThin, colors.borderOutline, RoundedCornerShape(metrics.radiusSmall))
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            color = colors.textMuted,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = colors.textPrimary,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            color = colors.primaryBright,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontFamily = OpenAquaTheme.typography.monospaceFontFamily,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
