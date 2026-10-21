package io.github.lamba92.openaqua.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.feature.info.DeviceInfoEvent
import io.github.lamba92.openaqua.app.feature.info.DeviceInfoState
import io.github.lamba92.openaqua.app.feature.info.DeviceInfoViewModel
import io.github.lamba92.openaqua.app.theme.BorderOutline
import io.github.lamba92.openaqua.app.theme.CyanBright
import io.github.lamba92.openaqua.app.theme.ElectricCyan
import io.github.lamba92.openaqua.app.theme.StatusNominal
import io.github.lamba92.openaqua.app.theme.SurfaceContainer
import io.github.lamba92.openaqua.app.theme.SurfaceContainerHigh
import io.github.lamba92.openaqua.app.theme.SurfaceContainerLow
import io.github.lamba92.openaqua.app.theme.TextMuted
import io.github.lamba92.openaqua.app.theme.TextPrimary
import io.github.lamba92.openaqua.app.theme.TextSecondary
import io.github.lamba92.openaqua.app.ui.components.DeviceIllustration
import io.github.lamba92.openaqua.model.DetectedHardwareDevice
import open_aqua.app_compose.generated.resources.Res
import open_aqua.app_compose.generated.resources.device_identification_title
import open_aqua.app_compose.generated.resources.device_telemetry_identity_subtitle
import open_aqua.app_compose.generated.resources.hardware_descriptors_title
import open_aqua.app_compose.generated.resources.ic_memory
import open_aqua.app_compose.generated.resources.ic_usb
import open_aqua.app_compose.generated.resources.info_device_architecture
import open_aqua.app_compose.generated.resources.info_device_name
import open_aqua.app_compose.generated.resources.info_firmware_version
import open_aqua.app_compose.generated.resources.info_hardware_alias
import open_aqua.app_compose.generated.resources.info_serial_number
import open_aqua.app_compose.generated.resources.info_unique_id
import open_aqua.app_compose.generated.resources.spec_fan_channels
import open_aqua.app_compose.generated.resources.spec_flow_sensor
import open_aqua.app_compose.generated.resources.spec_rgbpx_ports
import open_aqua.app_compose.generated.resources.spec_temp_probes
import open_aqua.app_compose.generated.resources.spec_usb_protocol
import open_aqua.app_compose.generated.resources.spec_virtual_sensors
import open_aqua.app_compose.generated.resources.usb_hid_connected
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceInfoScreen(
    device: DetectedHardwareDevice,
    viewModel: DeviceInfoViewModel,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(device) {
        viewModel.onEvent(DeviceInfoEvent.SetDevice(device))
    }

    val state by viewModel.state.collectAsState()

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
    ) {
        when (val s = state) {
            DeviceInfoState.Idle -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = CyanBright,
                )
            }
            is DeviceInfoState.Error -> {
                Text(
                    text = s.message,
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is DeviceInfoState.Loaded -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // Title Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = s.device.name,
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(Res.string.device_telemetry_identity_subtitle),
                                color = TextMuted,
                                fontSize = 13.sp,
                            )
                        }

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
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    HorizontalDivider(color = BorderOutline)

                    // Image / Visual Representation and High-Level Specs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        // Device Illustration (Image)
                        DeviceIllustration(
                            type = s.device.type,
                            modifier =
                                Modifier
                                    .weight(0.42f)
                                    .height(260.dp),
                        )

                        // Info from `devices` command & identity specs
                        Column(
                            modifier =
                                Modifier
                                    .weight(0.58f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceContainerLow)
                                    .border(1.dp, BorderOutline, RoundedCornerShape(16.dp))
                                    .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_memory),
                                    contentDescription = null,
                                    tint = CyanBright,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = stringResource(Res.string.device_identification_title),
                                    color = CyanBright,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            InfoRow(label = stringResource(Res.string.info_device_name), value = s.device.name)
                            InfoRow(label = stringResource(Res.string.info_hardware_alias), value = s.device.alias.ifBlank { "N/A" })
                            InfoRow(label = stringResource(Res.string.info_serial_number), value = s.device.serial ?: "N/A")
                            InfoRow(label = stringResource(Res.string.info_unique_id), value = s.device.id)
                            InfoRow(
                                label = stringResource(Res.string.info_firmware_version),
                                value = s.device.firmwareVersion?.let { "v$it" } ?: "Unknown",
                            )
                            InfoRow(label = stringResource(Res.string.info_device_architecture), value = s.device.type.name)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_usb),
                                contentDescription = null,
                                tint = CyanBright,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = stringResource(Res.string.hardware_descriptors_title),
                                color = CyanBright,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SpecCard(
                                title = stringResource(Res.string.spec_fan_channels),
                                value = "${s.descriptor.fanChannels.size} PWM Channels",
                                subtitle = "4-pin 12V 2A / channel",
                            )
                            SpecCard(
                                title = stringResource(Res.string.spec_temp_probes),
                                value = "${s.descriptor.tempSensors.size} Physical NTC",
                                subtitle = "10k thermistor inputs",
                            )
                            SpecCard(
                                title = stringResource(Res.string.spec_virtual_sensors),
                                value = "${s.descriptor.virtualSensorsCount} Software Slots",
                                subtitle = "Host telemetry feed",
                            )
                            SpecCard(
                                title = stringResource(Res.string.spec_rgbpx_ports),
                                value = if (s.descriptor.hasRgbpx) "${s.descriptor.rgbpxPortCount} Addressable" else "None",
                                subtitle = if (s.descriptor.hasRgbpx) "Up to 90 LEDs/port" else "N/A",
                            )
                            SpecCard(
                                title = stringResource(Res.string.spec_flow_sensor),
                                value = if (s.descriptor.hasFlowSensor) "Supported" else "Not supported",
                                subtitle = if (s.descriptor.hasFlowSensor) "Pulse / Flow input" else "N/A",
                            )
                            SpecCard(
                                title = stringResource(Res.string.spec_usb_protocol),
                                value = "0x0C70 : 0x${s.descriptor.productId.toString(16).uppercase()}",
                                subtitle = "Reports: ${s.descriptor.statusReportLength}B / ${s.descriptor.ctrlReportLength}B",
                            )
                        }
                    }
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
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
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
                .width(180.dp)
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
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = subtitle,
            color = ElectricCyan,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}
