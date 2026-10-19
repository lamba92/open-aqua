package io.github.lamba92.openaqua.app.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.lamba92.openaqua.model.AutoCurveParams
import io.github.lamba92.openaqua.model.CurveMath
import io.github.lamba92.openaqua.model.CurvePoint
import io.github.lamba92.openaqua.model.SensorTelemetry

enum class NavDestination(
    val label: String,
    val iconName: String,
) {
    CURVE_STUDIO("Curve Studio", "show_chart"),
    DEVICES_CHANNELS("Devices & Ch", "developer_board"),
    PROFILES("Profiles", "tune"),
    SETTINGS("Settings", "settings"),
}

enum class ChannelMode(
    val label: String,
) {
    FIXED("Fixed"),
    AUTO_CURVE("Auto Curve"),
    EXPERT_16_POINT("16-Point Expert"),
    FOLLOW("Follow / Mirror"),
}

data class FanChannelState(
    val id: String,
    val pinLabel: String,
    var name: String,
    val iconEmoji: String,
    val roleBadge: String,
    var mode: ChannelMode,
    var sourceSensor: String,
    var liveRpm: Int,
    var livePwm: Double,
    var isDirty: Boolean = false,
    var autoCurve: AutoCurveParams =
        AutoCurveParams(
            tMin = 25.0,
            tMax = 45.0,
            pMin = 20.0,
            pMax = 100.0,
            shape = 0.4,
        ),
    var activeHardwareCurve: List<CurvePoint> = CurveMath.generateCurve(25.0, 45.0, 20.0, 100.0, 0.4),
)

data class NtcSensorState(
    val id: String,
    val pinLabel: String,
    val name: String,
    var currentTemp: Double,
    var calibrationOffset: Double = 0.0,
)

class AppState {
    var currentDestination by mutableStateOf(NavDestination.CURVE_STUDIO)
    var selectedDevice by mutableStateOf("OCTO 8-CH (USB Connected)")
    var selectedProfile by mutableStateOf("Daily Quiet")

    // Loop Vitals
    var coolantTemp by mutableDoubleStateOf(28.5)
    var deltaTemp by mutableDoubleStateOf(2.7)
    var flowRateLh by mutableIntStateOf(142)

    // Hardware Channels
    val channels =
        mutableStateListOf(
            FanChannelState(
                id = "fan1",
                pinLabel = "PWM CH 1",
                name = "Top 360 Rad Push Fans",
                iconEmoji = "💧",
                roleBadge = "Radiator",
                mode = ChannelMode.AUTO_CURVE,
                sourceSensor = "Sensor 1: Coolant Temp (28.5°C)",
                liveRpm = 850,
                livePwm = 38.0,
                isDirty = true,
            ),
            FanChannelState(
                id = "fan2",
                pinLabel = "PWM CH 2",
                name = "Front Intake 140s",
                iconEmoji = "🌪️",
                roleBadge = "Intake",
                mode = ChannelMode.FOLLOW,
                sourceSensor = "Follow Ch 1 (-5% offset)",
                liveRpm = 720,
                livePwm = 30.0,
                isDirty = false,
            ),
            FanChannelState(
                id = "fan3",
                pinLabel = "PWM CH 3",
                name = "D5 Next Main Pump",
                iconEmoji = "⚙️",
                roleBadge = "Pump",
                mode = ChannelMode.FIXED,
                sourceSensor = "Fixed 50% Speed",
                liveRpm = 3200,
                livePwm = 50.0,
                isDirty = true,
            ),
            FanChannelState(
                id = "fan4",
                pinLabel = "PWM CH 4",
                name = "Rear Exhaust 120",
                iconEmoji = "💨",
                roleBadge = "Exhaust",
                mode = ChannelMode.AUTO_CURVE,
                sourceSensor = "Sensor 3: Ambient (23.1°C)",
                liveRpm = 600,
                livePwm = 25.0,
                isDirty = false,
            ),
        )

    var selectedChannelIndex by mutableIntStateOf(0)

    val selectedChannel: FanChannelState
        get() = channels[selectedChannelIndex]

    // NTC Probes
    val ntcSensors =
        listOf(
            NtcSensorState("temp1", "T1", "Water In (Reservoir)", 28.5, 0.0),
            NtcSensorState("temp2", "T2", "Water Out (Radiator)", 31.2, -0.2),
            NtcSensorState("temp3", "T3", "Ambient Chassis Air", 23.1, 0.0),
            NtcSensorState("temp4", "T4", "RAM Airflow Temp", 34.0, 0.5),
        )

    val pendingDirtyCount: Int
        get() = channels.count { it.isDirty }

    fun updateSensorsFromTelemetry(sensorTelemetries: List<SensorTelemetry>) {
        sensorTelemetries.forEach { telemetry ->
            ntcSensors.getOrNull(telemetry.channel)?.let { sensor ->
                telemetry.tempCelsius?.let { sensor.currentTemp = it }
            }
        }
    }
}
