package io.github.lamba92.openaqua.app.feature.device.fans

import io.github.lamba92.openaqua.app.feature.device.common.ChannelName
import io.github.lamba92.openaqua.app.feature.device.common.FanItem
import io.github.lamba92.openaqua.app.feature.device.common.SensorItem
import io.github.lamba92.openaqua.model.device.AutoCurveFanConfig
import io.github.lamba92.openaqua.model.device.CopyFanConfig
import io.github.lamba92.openaqua.model.device.CurveFanConfig
import io.github.lamba92.openaqua.model.device.CurveMath
import io.github.lamba92.openaqua.model.device.FanChannelConfig
import io.github.lamba92.openaqua.model.device.FanLimits
import io.github.lamba92.openaqua.model.device.FixedFanConfig
import io.github.lamba92.openaqua.model.device.TargetTempFanConfig

enum class CurveMode {
    FIXED,
    AUTO_CURVE,
    TARGET_TEMP,
    COPY_FAN,
}

sealed interface FlashDialogState {
    data object NotShown : FlashDialogState

    data object Shown : FlashDialogState

    data object Flashing : FlashDialogState
}

sealed interface FanDraft {
    val channel: Int
    val mode: CurveMode
    val limits: FanLimits

    data class Fixed(
        override val channel: Int,
        val powerPercent: Double = 50.0,
        override val limits: FanLimits = FanLimits.Standard,
    ) : FanDraft {
        override val mode: CurveMode get() = CurveMode.FIXED
    }

    data class AutoCurve(
        override val channel: Int,
        val sensorIndex: Int = 0,
        val tMin: Double = 30.0,
        val tMax: Double = 55.0,
        val pMin: Double = 25.0,
        val pMax: Double = 100.0,
        val shape: Double = 0.0,
        override val limits: FanLimits = FanLimits.Standard,
    ) : FanDraft {
        override val mode: CurveMode get() = CurveMode.AUTO_CURVE
    }

    data class TargetTemp(
        override val channel: Int,
        val sensorIndex: Int = 0,
        val targetTempCelsius: Double = 35.0,
        override val limits: FanLimits = FanLimits.Standard,
    ) : FanDraft {
        override val mode: CurveMode get() = CurveMode.TARGET_TEMP
    }

    data class Copy(
        override val channel: Int,
        val sourceChannel: Int = 0,
        override val limits: FanLimits = FanLimits.Standard,
    ) : FanDraft {
        override val mode: CurveMode get() = CurveMode.COPY_FAN
    }
}

fun FanDraft.withLimits(limits: FanLimits): FanDraft =
    when (this) {
        is FanDraft.Fixed -> copy(limits = limits)
        is FanDraft.AutoCurve -> copy(limits = limits)
        is FanDraft.TargetTemp -> copy(limits = limits)
        is FanDraft.Copy -> copy(limits = limits)
    }

fun FanDraft.toFanChannelConfig(
    allFans: List<FanItem>,
    allDrafts: Map<Int, FanDraft> = emptyMap(),
): FanChannelConfig {
    val targetLimits = limits
    return when (this) {
        is FanDraft.Fixed -> {
            FixedFanConfig(
                channel = channel,
                powerPercent = powerPercent,
                limits = targetLimits,
            )
        }

        is FanDraft.AutoCurve -> {
            AutoCurveFanConfig(
                channel = channel,
                sensorIndex = sensorIndex,
                tMin = tMin,
                tMax = tMax,
                pMin = pMin,
                pMax = pMax,
                shape = shape,
                limits = targetLimits,
            )
        }

        is FanDraft.TargetTemp -> {
            TargetTempFanConfig(
                channel = channel,
                sensorIndex = sensorIndex,
                targetTempCelsius = targetTempCelsius,
                limits = targetLimits,
            )
        }

        is FanDraft.Copy -> {
            CopyFanConfig(
                channel = channel,
                sourceChannel = sourceChannel,
                limits = targetLimits,
            )
        }
    }
}

fun FanChannelConfig?.toInitialDraft(
    channel: Int,
    defaultSensorIndex: Int = 0,
): FanDraft =
    when (this) {
        is FixedFanConfig -> {
            FanDraft.Fixed(
                channel = channel,
                powerPercent = powerPercent,
                limits = limits,
            )
        }

        is AutoCurveFanConfig -> {
            FanDraft.AutoCurve(
                channel = channel,
                sensorIndex = sensorIndex,
                tMin = tMin,
                tMax = tMax,
                pMin = pMin,
                pMax = pMax,
                shape = shape,
                limits = limits,
            )
        }

        is CurveFanConfig -> {
            FanDraft.AutoCurve(
                channel = channel,
                sensorIndex = sensorIndex,
                tMin = points.firstOrNull()?.temp ?: 30.0,
                tMax = points.lastOrNull()?.temp ?: 55.0,
                pMin = points.firstOrNull()?.pwm ?: 25.0,
                pMax = points.lastOrNull()?.pwm ?: 100.0,
                shape = CurveMath.inferShape(points),
                limits = limits,
            )
        }

        is TargetTempFanConfig -> {
            FanDraft.TargetTemp(
                channel = channel,
                sensorIndex = sensorIndex,
                targetTempCelsius = targetTempCelsius,
                limits = limits,
            )
        }

        is CopyFanConfig -> {
            FanDraft.Copy(
                channel = channel,
                sourceChannel = sourceChannel,
                limits = limits,
            )
        }

        else -> {
            FanDraft.AutoCurve(
                channel = channel,
                sensorIndex = defaultSensorIndex,
                tMin = 30.0,
                tMax = 55.0,
                pMin = 25.0,
                pMax = 100.0,
                shape = 0.0,
                limits = this?.limits ?: FanLimits.Standard,
            )
        }
    }

sealed interface FansUiState {
    data object Loading : FansUiState

    data class Ready(
        val fans: List<FanItem>,
        val selectedFanChannel: Int,
        val sensors: List<SensorItem>,
        val fanDrafts: Map<Int, FanDraft> = emptyMap(),
        val flashDialogState: FlashDialogState = FlashDialogState.NotShown,
        val hideDisconnectedFans: Boolean = false,
        val hideDisconnectedSensors: Boolean = false,
    ) : FansUiState {
        fun isFanModified(fan: FanItem): Boolean {
            val draft = fanDrafts[fan.channel] ?: return false
            return draft.toFanChannelConfig(fans, fanDrafts) != fan.config
        }

        val pendingFans: List<Pair<FanItem, FanChannelConfig>>
            get() =
                fans.mapNotNull { fan ->
                    val draft = fanDrafts[fan.channel] ?: return@mapNotNull null
                    val target = draft.toFanChannelConfig(fans, fanDrafts)
                    if (target != fan.config) fan to target else null
                }
    }
}

sealed interface FansUiEvent {
    @JvmInline
    value class SelectFan(
        val channel: Int,
    ) : FansUiEvent

    data class RenameFan(
        val channel: Int,
        val name: ChannelName,
    ) : FansUiEvent

    data object DiscardFanDraft : FansUiEvent

    data object RequestFlash : FansUiEvent

    data object DismissFlashDialog : FansUiEvent

    data object ConfirmFlash : FansUiEvent

    data object ToggleHideDisconnectedFans : FansUiEvent

    data object ToggleHideDisconnectedSensors : FansUiEvent
}

sealed interface CurveStudioEvent : FansUiEvent {
    data object ToggleHideDisconnectedSensors : CurveStudioEvent

    @JvmInline
    value class ChangeMode(
        val mode: CurveMode,
    ) : CurveStudioEvent

    @JvmInline
    value class ChangeFixedPwm(
        val pwm: Double,
    ) : CurveStudioEvent

    @JvmInline
    value class ChangeSensorIndex(
        val sensorIndex: Int,
    ) : CurveStudioEvent

    @JvmInline
    value class ChangeTMin(
        val tMin: Double,
    ) : CurveStudioEvent

    @JvmInline
    value class ChangeTMax(
        val tMax: Double,
    ) : CurveStudioEvent

    @JvmInline
    value class ChangePMin(
        val pMin: Double,
    ) : CurveStudioEvent

    @JvmInline
    value class ChangePMax(
        val pMax: Double,
    ) : CurveStudioEvent

    @JvmInline
    value class ChangeShape(
        val shape: Double,
    ) : CurveStudioEvent

    @JvmInline
    value class ChangeCopySource(
        val sourceChannel: Int,
    ) : CurveStudioEvent

    @JvmInline
    value class ChangeTargetTemp(
        val targetTemp: Double,
    ) : CurveStudioEvent
}

sealed interface FanLimitsEvent : FansUiEvent {
    @JvmInline
    value class ChangeMinPower(
        val minPowerPercent: Double,
    ) : FanLimitsEvent

    @JvmInline
    value class ChangeMaxPower(
        val maxPowerPercent: Double,
    ) : FanLimitsEvent

    @JvmInline
    value class ChangeFallbackPower(
        val fallbackPowerPercent: Double,
    ) : FanLimitsEvent

    @JvmInline
    value class ChangeStartBoost(
        val startBoost: Boolean,
    ) : FanLimitsEvent

    @JvmInline
    value class ChangeHoldMinPower(
        val holdMinPower: Boolean,
    ) : FanLimitsEvent

    data object ResetToStandardLimits : FanLimitsEvent
}

sealed interface FansUiEffect
