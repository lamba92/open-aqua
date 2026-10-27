package io.github.lamba92.openaqua.app.feature.device.fans

import io.github.lamba92.openaqua.app.feature.device.common.FanItem
import io.github.lamba92.openaqua.app.feature.device.common.SensorItem
import io.github.lamba92.openaqua.model.AutoCurveFanConfig
import io.github.lamba92.openaqua.model.CurveFanConfig
import io.github.lamba92.openaqua.model.FanChannelConfig
import io.github.lamba92.openaqua.model.FixedFanConfig
import io.github.lamba92.openaqua.model.TargetTempFanConfig

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

    data class Fixed(
        override val channel: Int,
        val powerPercent: Double = 50.0,
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
    ) : FanDraft {
        override val mode: CurveMode get() = CurveMode.AUTO_CURVE
    }

    data class TargetTemp(
        override val channel: Int,
        val sensorIndex: Int = 0,
        val targetTempCelsius: Double = 35.0,
    ) : FanDraft {
        override val mode: CurveMode get() = CurveMode.TARGET_TEMP
    }

    data class Copy(
        override val channel: Int,
        val sourceChannel: Int = 0,
    ) : FanDraft {
        override val mode: CurveMode get() = CurveMode.COPY_FAN
    }
}

fun FanDraft.toFanChannelConfig(
    allFans: List<FanItem>,
    allDrafts: Map<Int, FanDraft> = emptyMap(),
): FanChannelConfig =
    when (this) {
        is FanDraft.Fixed ->
            FixedFanConfig(
                channel = channel,
                powerPercent = powerPercent,
            )

        is FanDraft.AutoCurve ->
            AutoCurveFanConfig(
                channel = channel,
                sensorIndex = sensorIndex,
                tMin = tMin,
                tMax = tMax,
                pMin = pMin,
                pMax = pMax,
                shape = shape,
            )

        is FanDraft.TargetTemp ->
            TargetTempFanConfig(
                channel = channel,
                sensorIndex = sensorIndex,
                targetTempCelsius = targetTempCelsius,
            )

        is FanDraft.Copy -> {
            val sourceDraft = allDrafts[sourceChannel]
            val sourceConfig =
                if (sourceDraft != null && sourceDraft !is FanDraft.Copy) {
                    sourceDraft.toFanChannelConfig(allFans, emptyMap())
                } else {
                    allFans.firstOrNull { it.channel == sourceChannel }?.config
                }
            when (sourceConfig) {
                is FixedFanConfig -> sourceConfig.copy(channel = channel)
                is AutoCurveFanConfig -> sourceConfig.copy(channel = channel)
                is CurveFanConfig -> sourceConfig.copy(channel = channel)
                is TargetTempFanConfig -> sourceConfig.copy(channel = channel)
                else -> FixedFanConfig(channel = channel, powerPercent = 50.0)
            }
        }
    }

fun FanChannelConfig?.toInitialDraft(
    channel: Int,
    defaultSensorIndex: Int = 0,
): FanDraft =
    when (this) {
        is FixedFanConfig ->
            FanDraft.Fixed(
                channel = channel,
                powerPercent = powerPercent,
            )

        is AutoCurveFanConfig ->
            FanDraft.AutoCurve(
                channel = channel,
                sensorIndex = sensorIndex,
                tMin = tMin,
                tMax = tMax,
                pMin = pMin,
                pMax = pMax,
                shape = shape,
            )

        is CurveFanConfig ->
            FanDraft.AutoCurve(
                channel = channel,
                sensorIndex = sensorIndex,
                tMin = points.firstOrNull()?.temp ?: 30.0,
                tMax = points.lastOrNull()?.temp ?: 55.0,
                pMin = points.firstOrNull()?.pwm ?: 25.0,
                pMax = points.lastOrNull()?.pwm ?: 100.0,
                shape = 0.0,
            )

        is TargetTempFanConfig ->
            FanDraft.TargetTemp(
                channel = channel,
                sensorIndex = sensorIndex,
                targetTempCelsius = targetTempCelsius,
            )

        else ->
            FanDraft.AutoCurve(
                channel = channel,
                sensorIndex = defaultSensorIndex,
                tMin = 30.0,
                tMax = 55.0,
                pMin = 25.0,
                pMax = 100.0,
                shape = 0.0,
            )
    }

sealed interface FansUiState {
    data object Loading : FansUiState

    data class Ready(
        val fans: List<FanItem>,
        val selectedFanChannel: Int,
        val sensors: List<SensorItem>,
        val fanDrafts: Map<Int, FanDraft> = emptyMap(),
        val flashDialogState: FlashDialogState = FlashDialogState.NotShown,
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
    data class SelectFan(
        val channel: Int,
    ) : FansUiEvent

    data class RenameFan(
        val channel: Int,
        val name: String?,
    ) : FansUiEvent

    data object DiscardFanDraft : FansUiEvent

    data object RequestFlash : FansUiEvent

    data object DismissFlashDialog : FansUiEvent

    data object ConfirmFlash : FansUiEvent
}

sealed interface CurveStudioEvent : FansUiEvent {
    data class ChangeMode(
        val mode: CurveMode,
    ) : CurveStudioEvent

    data class ChangeFixedPwm(
        val pwm: Double,
    ) : CurveStudioEvent

    data class ChangeSensorIndex(
        val sensorIndex: Int,
    ) : CurveStudioEvent

    data class ChangeTMin(
        val tMin: Double,
    ) : CurveStudioEvent

    data class ChangeTMax(
        val tMax: Double,
    ) : CurveStudioEvent

    data class ChangePMin(
        val pMin: Double,
    ) : CurveStudioEvent

    data class ChangePMax(
        val pMax: Double,
    ) : CurveStudioEvent

    data class ChangeShape(
        val shape: Double,
    ) : CurveStudioEvent

    data class ChangeCopySource(
        val sourceChannel: Int,
    ) : CurveStudioEvent

    data class ChangeTargetTemp(
        val targetTemp: Double,
    ) : CurveStudioEvent
}

sealed interface FansUiEffect
