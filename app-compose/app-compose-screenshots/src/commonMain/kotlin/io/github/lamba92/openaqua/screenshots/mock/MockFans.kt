package io.github.lamba92.openaqua.screenshots.mock

import io.github.lamba92.openaqua.app.feature.device.common.ChannelName
import io.github.lamba92.openaqua.app.feature.device.common.FanItem
import io.github.lamba92.openaqua.app.feature.device.fans.FanDraft
import io.github.lamba92.openaqua.app.feature.device.fans.FansUiState
import io.github.lamba92.openaqua.model.device.AutoCurveFanConfig
import io.github.lamba92.openaqua.model.device.CoolingChannelKind
import io.github.lamba92.openaqua.model.device.CoolingChannelTelemetry
import io.github.lamba92.openaqua.model.device.FanLimits
import io.github.lamba92.openaqua.model.device.FixedFanConfig
import io.github.lamba92.openaqua.model.device.TargetTempFanConfig

val mockOctoFans: List<FanItem> =
    listOf(
        FanItem(
            channel = 0,
            name = ChannelName.Custom("Rad Intake Front"),
            telemetry =
                CoolingChannelTelemetry.Connected(
                    channel = 0,
                    kind = CoolingChannelKind.FAN,
                    rpm = 1240,
                    powerWatts = 3.12,
                    voltageVolts = 12.05,
                    currentAmps = 0.26,
                    pwmPercent = 54.0,
                ),
            config =
                AutoCurveFanConfig(
                    channel = 0,
                    sensorIndex = 0,
                    tMin = 28.0,
                    tMax = 42.0,
                    pMin = 30.0,
                    pMax = 100.0,
                    shape = 0.25,
                ),
        ),
        FanItem(
            channel = 1,
            name = ChannelName.Custom("Rad Top Exhaust"),
            telemetry =
                CoolingChannelTelemetry.Connected(
                    channel = 1,
                    kind = CoolingChannelKind.FAN,
                    rpm = 1080,
                    powerWatts = 2.65,
                    voltageVolts = 12.02,
                    currentAmps = 0.22,
                    pwmPercent = 46.0,
                ),
            config =
                AutoCurveFanConfig(
                    channel = 1,
                    sensorIndex = 0,
                    tMin = 28.0,
                    tMax = 44.0,
                    pMin = 25.0,
                    pMax = 90.0,
                    shape = 0.0,
                ),
        ),
        FanItem(
            channel = 2,
            name = ChannelName.Custom("Rear Exhaust"),
            telemetry =
                CoolingChannelTelemetry.Connected(
                    channel = 2,
                    kind = CoolingChannelKind.FAN,
                    rpm = 920,
                    powerWatts = 1.85,
                    voltageVolts = 12.00,
                    currentAmps = 0.15,
                    pwmPercent = 38.0,
                ),
            config =
                FixedFanConfig(
                    channel = 2,
                    powerPercent = 40.0,
                ),
        ),
        FanItem(
            channel = 3,
            name = ChannelName.Custom("D5 Next Pump"),
            telemetry =
                CoolingChannelTelemetry.Connected(
                    channel = 3,
                    kind = CoolingChannelKind.PUMP,
                    rpm = 2850,
                    powerWatts = 17.80,
                    voltageVolts = 12.10,
                    currentAmps = 1.47,
                    pwmPercent = 65.0,
                ),
            config =
                TargetTempFanConfig(
                    channel = 3,
                    sensorIndex = 0,
                    targetTempCelsius = 33.0,
                ),
        ),
        FanItem(
            channel = 4,
            name = ChannelName.Custom("VRM Spot Fan"),
            telemetry =
                CoolingChannelTelemetry.Connected(
                    channel = 4,
                    kind = CoolingChannelKind.FAN,
                    rpm = 750,
                    powerWatts = 0.95,
                    voltageVolts = 11.98,
                    currentAmps = 0.08,
                    pwmPercent = 30.0,
                ),
            config =
                FixedFanConfig(
                    channel = 4,
                    powerPercent = 30.0,
                ),
        ),
        FanItem(
            channel = 5,
            name = ChannelName.Default,
            telemetry = CoolingChannelTelemetry.Disconnected(channel = 5),
            config = FixedFanConfig(channel = 5, powerPercent = 50.0),
        ),
        FanItem(
            channel = 6,
            name = ChannelName.Default,
            telemetry = CoolingChannelTelemetry.Disconnected(channel = 6),
            config = FixedFanConfig(channel = 6, powerPercent = 50.0),
        ),
        FanItem(
            channel = 7,
            name = ChannelName.Default,
            telemetry = CoolingChannelTelemetry.Disconnected(channel = 7),
            config = FixedFanConfig(channel = 7, powerPercent = 50.0),
        ),
    )

val mockOctoFanDrafts: Map<Int, FanDraft> =
    mapOf(
        0 to
            FanDraft.AutoCurve(
                channel = 0,
                sensorIndex = 0,
                tMin = 28.0,
                tMax = 42.0,
                pMin = 30.0,
                pMax = 100.0,
                shape = 0.25,
                limits = FanLimits.Standard,
            ),
    )

val mockOctoFansUiState: FansUiState.Ready =
    FansUiState.Ready(
        fans = mockOctoFans,
        selectedFanChannel = 0,
        sensors = mockOctoSensors,
        fanDrafts = mockOctoFanDrafts,
        hideDisconnectedFans = false,
    )
