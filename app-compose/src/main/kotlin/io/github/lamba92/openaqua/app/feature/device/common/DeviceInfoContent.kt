package io.github.lamba92.openaqua.app.feature.device.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.theme.BorderOutline
import io.github.lamba92.openaqua.app.theme.ElectricCyan
import io.github.lamba92.openaqua.app.theme.StatusNominal
import io.github.lamba92.openaqua.app.theme.SurfaceContainer
import io.github.lamba92.openaqua.app.theme.SurfaceContainerHigh
import io.github.lamba92.openaqua.app.theme.SurfaceContainerLow
import io.github.lamba92.openaqua.app.theme.TextMuted
import io.github.lamba92.openaqua.app.theme.TextPrimary
import io.github.lamba92.openaqua.app.theme.TextSecondary
import io.github.lamba92.openaqua.app.ui.components.DeviceIllustration
import io.github.lamba92.openaqua.app.ui.components.ScreenHeader
import io.github.lamba92.openaqua.app.ui.components.SectionHeader
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
import open_aqua.app_compose.generated.resources.spec_fan_channels
import open_aqua.app_compose.generated.resources.spec_fan_channels_subtitle
import open_aqua.app_compose.generated.resources.spec_fan_channels_value
import open_aqua.app_compose.generated.resources.spec_flow_sensor
import open_aqua.app_compose.generated.resources.spec_flow_sensor_subtitle
import open_aqua.app_compose.generated.resources.spec_flow_sensor_supported
import open_aqua.app_compose.generated.resources.spec_flow_sensor_unsupported
import open_aqua.app_compose.generated.resources.spec_rgbpx_ports
import open_aqua.app_compose.generated.resources.spec_rgbpx_ports_none
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceInfoContent(
    deviceState: DeviceState.Ready,
    modifier: Modifier = Modifier,
) {
    val device = deviceState.device
    val descriptor = deviceState.descriptor
    val notAvailable = stringResource(Res.string.info_not_available)

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Title Header
            ScreenHeader(
                title = device.name,
                subtitle = stringResource(Res.string.device_telemetry_identity_subtitle),
                trailingContent = {
                    // Status Badge
                    Row(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(SurfaceContainerHigh)
                                .border(1.dp, BorderOutline, RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(StatusNominal),
                        )
                        Text(
                            text = stringResource(Res.string.usb_hid_connected),
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                },
            )

            HorizontalDivider(color = BorderOutline)

            // Image / Visual Representation and High-Level Specs
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
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
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, BorderOutline, RoundedCornerShape(16.dp))
                            .padding(20.dp),
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
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerLow)
                        .border(1.dp, BorderOutline, RoundedCornerShape(16.dp))
                        .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SectionHeader(
                    icon = painterResource(Res.drawable.ic_usb),
                    title = stringResource(Res.string.hardware_descriptors_title),
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SpecCard(
                        title = stringResource(Res.string.spec_fan_channels),
                        value = stringResource(Res.string.spec_fan_channels_value, descriptor.fanChannels.size),
                        subtitle = stringResource(Res.string.spec_fan_channels_subtitle),
                    )
                    SpecCard(
                        title = stringResource(Res.string.spec_temp_probes),
                        value = stringResource(Res.string.spec_temp_probes_value, descriptor.tempSensors.size),
                        subtitle = stringResource(Res.string.spec_temp_probes_subtitle),
                    )
                    SpecCard(
                        title = stringResource(Res.string.spec_virtual_sensors),
                        value = stringResource(Res.string.spec_virtual_sensors_value, descriptor.virtualSensorsCount),
                        subtitle = stringResource(Res.string.spec_virtual_sensors_subtitle),
                    )
                    SpecCard(
                        title = stringResource(Res.string.spec_rgbpx_ports),
                        value =
                            if (descriptor.hasRgbpx) {
                                stringResource(Res.string.spec_rgbpx_ports_value, descriptor.rgbpxPortCount)
                            } else {
                                stringResource(Res.string.spec_rgbpx_ports_none)
                            },
                        subtitle =
                            if (descriptor.hasRgbpx) {
                                stringResource(Res.string.spec_rgbpx_ports_subtitle)
                            } else {
                                notAvailable
                            },
                    )
                    SpecCard(
                        title = stringResource(Res.string.spec_flow_sensor),
                        value =
                            if (descriptor.hasFlowSensor) {
                                stringResource(Res.string.spec_flow_sensor_supported)
                            } else {
                                stringResource(Res.string.spec_flow_sensor_unsupported)
                            },
                        subtitle =
                            if (descriptor.hasFlowSensor) {
                                stringResource(Res.string.spec_flow_sensor_subtitle)
                            } else {
                                notAvailable
                            },
                    )
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
                                descriptor.statusReportLength,
                                descriptor.ctrlReportLength,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
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
    Column(
        modifier =
            modifier
                .width(196.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceContainer)
                .border(1.dp, BorderOutline, RoundedCornerShape(10.dp))
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            color = ElectricCyan,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
