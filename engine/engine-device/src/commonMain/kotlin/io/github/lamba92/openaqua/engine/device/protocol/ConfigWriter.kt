package io.github.lamba92.openaqua.engine.device.protocol

import io.github.lamba92.openaqua.model.FeatureState
import io.github.lamba92.openaqua.model.device.AutoCurveFanConfig
import io.github.lamba92.openaqua.model.device.CommitStrategy
import io.github.lamba92.openaqua.model.device.CopyFanConfig
import io.github.lamba92.openaqua.model.device.CurveFanConfig
import io.github.lamba92.openaqua.model.device.CurvePoint
import io.github.lamba92.openaqua.model.device.DeviceCapability
import io.github.lamba92.openaqua.model.device.DeviceConfig
import io.github.lamba92.openaqua.model.device.DeviceDescriptor
import io.github.lamba92.openaqua.model.device.DeviceProtocol
import io.github.lamba92.openaqua.model.device.FanCapability
import io.github.lamba92.openaqua.model.device.FanChannelConfig
import io.github.lamba92.openaqua.model.device.FanChannelDescriptor
import io.github.lamba92.openaqua.model.device.FanLimits
import io.github.lamba92.openaqua.model.device.FanLimitsLayout
import io.github.lamba92.openaqua.model.device.FixedFanConfig
import io.github.lamba92.openaqua.model.device.FlashConfigRequest
import io.github.lamba92.openaqua.model.device.LimitsOnlyFanConfig
import io.github.lamba92.openaqua.model.device.TargetTempFanConfig
import io.github.lamba92.openaqua.model.device.TwoPointFanConfig
import io.github.lamba92.openaqua.model.device.UnconfiguredFanConfig
import io.github.lamba92.openaqua.model.device.findCapability
import io.github.lamba92.openaqua.model.device.findFanCapability
import io.github.lamba92.openaqua.model.device.flashableProtocol
import io.github.lamba92.openaqua.model.device.hasCapability
import io.github.lamba92.openaqua.model.device.logicalToHardwareSensorIndex
import io.github.lamba92.openaqua.model.device.requireCapability
import io.github.lamba92.openaqua.model.device.validateRequestMatchesDevice
import kotlin.math.abs
import kotlin.math.roundToInt

object ConfigWriter {
    /**
     * Secondary HID Feature report sent after writing Report 0x03 to commit
     * changes to hardware memory (same report sent by official Aquasuite and Linux kernel driver).
     */
    val SECONDARY_COMMIT_REPORT: UByteArray
        get() = CommitStrategy.SecondaryReport.defaultCommitReport.toUByteArray()

    /**
     * Prepares the sequence of raw HID reports required to flash and persist configuration to the device.
     *
     * Applies the requested changes to [report], finalizes its checksum using the device's [DeviceProtocol.Flashable.reportSigner],
     * and appends any secondary commit report required by the device's [CommitStrategy].
     *
     * @param report Pre-allocated buffer containing the device's current control report (Report ID + payload).
     * @param descriptor The target device descriptor.
     * @param request The flash configuration request containing desired settings.
     * @return Ordered list of raw byte buffers to transmit sequentially to the device.
     */
    fun prepareFlashReports(
        report: UByteArray,
        descriptor: DeviceDescriptor,
        request: FlashConfigRequest,
    ): List<UByteArray> {
        val flashProtocol = descriptor.flashableProtocol

        applyFlashConfig(report, descriptor, request)
        flashProtocol.reportSigner.sign(report)

        return when (val strategy = flashProtocol.commitStrategy) {
            is CommitStrategy.SecondaryReport -> listOf(report, strategy.report.toUByteArray())
            is CommitStrategy.Direct -> listOf(report)
        }
    }

    fun encodeFanChannelConfig(
        report: UByteArray,
        baseOffset: Int,
        config: FanChannelConfig,
        descriptor: DeviceDescriptor,
    ) {
        when (config) {
            is FixedFanConfig -> {
                val cap =
                    descriptor.findFanCapability<FanCapability.Fixed>()
                        ?: error("Device ${descriptor.name} does not support Fixed fan mode")
                report[baseOffset + cap.modeOffset] = cap.modeByte.toUByte()
                val rawPwm = (config.powerPercent * 100.0).roundToInt().coerceIn(0, 10000).toUShort()
                report[baseOffset + cap.pwmOffset] = ((rawPwm.toInt() ushr 8) and 0xFF).toUByte()
                report[baseOffset + cap.pwmOffset + 1] = (rawPwm.toInt() and 0xFF).toUByte()
            }
            is CurveFanConfig -> {
                val cap =
                    descriptor.findFanCapability<FanCapability.Curve>()
                        ?: error("Device ${descriptor.name} does not support Curve fan mode")
                report[baseOffset + cap.modeOffset] = cap.modeByte.toUByte()
                val hwSensor = descriptor.logicalToHardwareSensorIndex(config.sensorIndex)
                report[baseOffset + cap.sensorIndexOffset] = ((hwSensor ushr 8) and 0xFF).toUByte()
                report[baseOffset + cap.sensorIndexOffset + 1] = (hwSensor and 0xFF).toUByte()
                encodeCurvePoints(report, baseOffset, config.points, cap)
            }
            is AutoCurveFanConfig -> {
                encodeFanChannelConfig(report, baseOffset, config.toCurveFanConfig(), descriptor)
            }
            is TargetTempFanConfig -> {
                val cap =
                    descriptor.findFanCapability<FanCapability.TargetTemp>()
                        ?: error("Device ${descriptor.name} does not support TargetTemp fan mode")
                report[baseOffset + cap.modeOffset] = cap.modeByte.toUByte()
                val hwSensor = descriptor.logicalToHardwareSensorIndex(config.sensorIndex)
                report[baseOffset + cap.sensorIndexOffset] = ((hwSensor ushr 8) and 0xFF).toUByte()
                report[baseOffset + cap.sensorIndexOffset + 1] = (hwSensor and 0xFF).toUByte()
                val rawTemp = (config.targetTempCelsius * 100.0).roundToInt().coerceIn(-5000, 15000).toShort()
                report[baseOffset + cap.targetTempOffset] = ((rawTemp.toInt() ushr 8) and 0xFF).toUByte()
                report[baseOffset + cap.targetTempOffset + 1] = (rawTemp.toInt() and 0xFF).toUByte()
            }
            is TwoPointFanConfig -> {
                val cap =
                    descriptor.findFanCapability<FanCapability.TwoPoint>()
                        ?: error("Device ${descriptor.name} does not support TwoPoint fan mode")
                report[baseOffset + cap.modeOffset] = cap.modeByte.toUByte()
                val hwSensor = descriptor.logicalToHardwareSensorIndex(config.sensorIndex)
                report[baseOffset + cap.sensorIndexOffset] = ((hwSensor ushr 8) and 0xFF).toUByte()
                report[baseOffset + cap.sensorIndexOffset + 1] = (hwSensor and 0xFF).toUByte()
            }
            is CopyFanConfig -> {
                val follow =
                    descriptor.findFanCapability<FanCapability.Follow>()
                        ?: error("Device ${descriptor.name} does not support follow fan mode")
                val mode = follow.startModeByteInclusive + config.sourceChannel
                check(mode <= follow.endModeByteInclusive) {
                    "Source fan channel ${config.sourceChannel} exceeds follow mode range ($mode > ${follow.endModeByteInclusive}) on ${descriptor.name}"
                }
                report[baseOffset + follow.modeOffset] = mode.toUByte()
                report[baseOffset + follow.sensorIndexOffset] = ((follow.unassignedSensorSentinel ushr 8) and 0xFF).toUByte()
                report[baseOffset + follow.sensorIndexOffset + 1] = (follow.unassignedSensorSentinel and 0xFF).toUByte()
            }
            is UnconfiguredFanConfig -> {
                val follow = descriptor.findFanCapability<FanCapability.Follow>()
                val sensorIndexOffset = follow?.sensorIndexOffset ?: 3
                val unassignedSentinel = follow?.unassignedSensorSentinel ?: 0xFFFF
                report[baseOffset + 0] = config.rawMode.toUByte()
                report[baseOffset + sensorIndexOffset] = ((unassignedSentinel ushr 8) and 0xFF).toUByte()
                report[baseOffset + sensorIndexOffset + 1] = (unassignedSentinel and 0xFF).toUByte()
            }
            is LimitsOnlyFanConfig -> {
                // No control register modification for limits-only config
            }
        }
    }

    fun encodeCurvePoints(
        report: UByteArray,
        baseOffset: Int,
        points: List<CurvePoint>,
        curveCap: FanCapability.Curve = FanCapability.Curve(),
    ) {
        require(points.size == curveCap.pointsCount) {
            "Expected exactly ${curveCap.pointsCount} curve points, got ${points.size}"
        }

        for (i in 0 until curveCap.pointsCount) {
            val pt = points[i]
            val tRaw = (pt.temp * 100.0).roundToInt().coerceIn(-5000, 15000).toShort()
            val pRaw = (pt.pwm * 100.0).roundToInt().coerceIn(0, 10000).toUShort()

            val tOffset = baseOffset + curveCap.tempsOffset + (i * 2)
            val pOffset = baseOffset + curveCap.pwmsOffset + (i * 2)

            report[tOffset] = ((tRaw.toInt() ushr 8) and 0xFF).toUByte()
            report[tOffset + 1] = (tRaw.toInt() and 0xFF).toUByte()

            report[pOffset] = ((pRaw.toInt() ushr 8) and 0xFF).toUByte()
            report[pOffset + 1] = (pRaw.toInt() and 0xFF).toUByte()
        }
    }

    fun encodeSensorOffset(
        report: UByteArray,
        baseOffset: Int,
        channel: Int,
        offsetCelsius: Double,
    ) {
        val offsetPos = baseOffset + (channel * 2)
        val centiCelsius = (offsetCelsius * 100.0).roundToInt().coerceIn(-1500, 1500).toShort()

        report[offsetPos] = ((centiCelsius.toInt() ushr 8) and 0xFF).toUByte()
        report[offsetPos + 1] = (centiCelsius.toInt() and 0xFF).toUByte()
    }

    fun encodeFanLimits(
        report: UByteArray,
        setupOffset: Int,
        limits: FanLimits,
        limitsLayout: FanLimitsLayout = FanLimitsLayout.Standard,
    ) {
        var flags = 0
        if (limits.holdMinPower) flags = flags or limitsLayout.holdMinPowerMask
        if (limits.startBoost) flags = flags or limitsLayout.startBoostMask
        report[setupOffset + limitsLayout.flagsOffset] = flags.toUByte()

        val rawMin = (limits.minPowerPercent * 100.0).roundToInt().coerceIn(0, 10000)
        report[setupOffset + limitsLayout.minPowerOffset] = ((rawMin ushr 8) and 0xFF).toUByte()
        report[setupOffset + limitsLayout.minPowerOffset + 1] = (rawMin and 0xFF).toUByte()

        val rawMax = (limits.maxPowerPercent * 100.0).roundToInt().coerceIn(0, 10000)
        report[setupOffset + limitsLayout.maxPowerOffset] = ((rawMax ushr 8) and 0xFF).toUByte()
        report[setupOffset + limitsLayout.maxPowerOffset + 1] = (rawMax and 0xFF).toUByte()

        val rawFallback = (limits.fallbackPowerPercent * 100.0).roundToInt().coerceIn(0, 10000)
        report[setupOffset + limitsLayout.fallbackPowerOffset] = ((rawFallback ushr 8) and 0xFF).toUByte()
        report[setupOffset + limitsLayout.fallbackPowerOffset + 1] = (rawFallback and 0xFF).toUByte()
    }

    fun encodeFanLimits(
        report: UByteArray,
        fan: FanChannelDescriptor.WithLimits,
        limits: FanLimits,
        limitsLayout: FanLimitsLayout = FanLimitsLayout.Standard,
    ) = encodeFanLimits(report, fan.setupOffset, limits, limitsLayout)

    fun applyFlashConfig(
        report: UByteArray,
        descriptor: DeviceDescriptor,
        request: FlashConfigRequest,
    ) {
        descriptor.validateRequestMatchesDevice(request)
        val fansCap = descriptor.findCapability<DeviceCapability.Fans>()
        val limitsLayout = fansCap?.limitsLayout ?: FanLimitsLayout.Standard
        request.fans?.forEach { (channel, config) ->
            val fan =
                fansCap?.channels?.firstOrNull { it.channel == channel }
                    ?: error("Unknown fan channel $channel for ${descriptor.name}")
            when (config) {
                is LimitsOnlyFanConfig -> Unit
                else -> encodeFanChannelConfig(report, fan.ctrlOffset, config, descriptor)
            }
            when (fan) {
                is FanChannelDescriptor.WithLimits -> encodeFanLimits(report, fan.setupOffset, config.limits, limitsLayout)
                is FanChannelDescriptor.Basic -> Unit
            }
        }
        request.sensorOffsets?.let { offsets ->
            val tempCap = descriptor.requireCapability<DeviceCapability.TemperatureSensors.Calibrated>()
            offsets.forEach { (channel, offset) ->
                encodeSensorOffset(report, tempCap.ctrlOffset, channel, offset)
            }
        }
        request.flowPulsesPerLiter?.let { reqPulses ->
            val flowCap = descriptor.requireCapability<DeviceCapability.FlowSensor.Calibrated>()
            report[flowCap.pulsesCtrlOffset] = ((reqPulses ushr 8) and 0xFF).toUByte()
            report[flowCap.pulsesCtrlOffset + 1] = (reqPulses and 0xFF).toUByte()
        }
        request.display?.let {
            require(descriptor.hasCapability<DeviceCapability.Display>()) {
                "${descriptor.name} does not have a display"
            }
        }
    }

    fun verifyFlashConfig(
        expected: FlashConfigRequest,
        actual: DeviceConfig,
    ) {
        expected.fans?.forEach { (channel, expConfig) ->
            val actConfig =
                actual.fans.firstOrNull { it.channel == channel }
                    ?: error("Readback missing fan channel $channel")
            if (expConfig !is LimitsOnlyFanConfig) {
                verifyFanChannelConfig(expected = expConfig, actual = actConfig)
            }
            val expLimits = expConfig.limits
            val actLimits = actConfig.limits
            check(abs(actLimits.minPowerPercent - expLimits.minPowerPercent) <= 0.2) {
                "Verification failed on fan $channel minPower: expected ${expLimits.minPowerPercent}%, read ${actLimits.minPowerPercent}%"
            }
            check(abs(actLimits.maxPowerPercent - expLimits.maxPowerPercent) <= 0.2) {
                "Verification failed on fan $channel maxPower: expected ${expLimits.maxPowerPercent}%, read ${actLimits.maxPowerPercent}%"
            }
            check(abs(actLimits.fallbackPowerPercent - expLimits.fallbackPowerPercent) <= 0.2) {
                "Verification failed on fan $channel fallbackPower: expected ${expLimits.fallbackPowerPercent}%, read ${actLimits.fallbackPowerPercent}%"
            }
            check(actLimits.startBoost == expLimits.startBoost) {
                "Verification failed on fan $channel startBoost: expected ${expLimits.startBoost}, read ${actLimits.startBoost}"
            }
            check(actLimits.holdMinPower == expLimits.holdMinPower) {
                "Verification failed on fan $channel holdMinPower: expected ${expLimits.holdMinPower}, read ${actLimits.holdMinPower}"
            }
        }
        expected.sensorOffsets?.forEach { (channel, expOffset) ->
            val actOffset =
                actual.sensorOffsets.firstOrNull { it.channel == channel }?.offsetCelsius
                    ?: error("Readback missing sensor channel $channel")
            check(abs(actOffset - expOffset) <= 0.1) {
                "Verification failed on sensor $channel: expected offset $expOffset°C, read $actOffset°C"
            }
        }
        val expPulses = expected.flowPulsesPerLiter
        if (expPulses != null) {
            val actPulses = actual.flowPulsesPerLiter
            check(actPulses is FeatureState.Active && actPulses.value == expPulses) {
                "Verification failed on flowPulsesPerLiter: expected $expPulses, read ${actual.flowPulsesPerLiter}"
            }
        }
        val expDisplay = expected.display
        if (expDisplay != null) {
            val actDisplay = actual.display
            check(actDisplay is FeatureState.Active && actDisplay.value == expDisplay) {
                "Verification failed on display: expected $expDisplay, read ${actual.display}"
            }
        }
    }

    fun verifyFanChannelConfig(
        expected: FanChannelConfig,
        actual: FanChannelConfig,
    ) {
        when (expected) {
            is FixedFanConfig -> {
                check(actual is FixedFanConfig) {
                    "Verification failed: expected FixedFanConfig, got ${actual::class.simpleName}"
                }
                check(abs(actual.powerPercent - expected.powerPercent) <= 0.2) {
                    "Verification failed: expected power ${expected.powerPercent}%, read ${actual.powerPercent}%"
                }
            }
            is CurveFanConfig -> {
                check(actual is CurveFanConfig) {
                    "Verification failed: expected CurveFanConfig, got ${actual::class.simpleName}"
                }
                check(actual.sensorIndex == expected.sensorIndex) {
                    "Verification failed: expected sensorIndex ${expected.sensorIndex}, read ${actual.sensorIndex}"
                }
                for (i in expected.points.indices) {
                    val expPt = expected.points[i]
                    val actPt = actual.points[i]
                    check(abs(actPt.temp - expPt.temp) <= 0.2 && abs(actPt.pwm - expPt.pwm) <= 0.2) {
                        "Verification failed at point $i: expected (${expPt.temp}°C, ${expPt.pwm}%), read (${actPt.temp}°C, ${actPt.pwm}%)"
                    }
                }
            }
            is AutoCurveFanConfig -> {
                verifyFanChannelConfig(expected = expected.toCurveFanConfig(), actual = actual)
            }
            is TargetTempFanConfig -> {
                check(actual is TargetTempFanConfig) {
                    "Verification failed: expected TargetTempFanConfig, got ${actual::class.simpleName}"
                }
                check(actual.sensorIndex == expected.sensorIndex) {
                    "Verification failed: expected sensorIndex ${expected.sensorIndex}, read ${actual.sensorIndex}"
                }
                check(abs(actual.targetTempCelsius - expected.targetTempCelsius) <= 0.2) {
                    "Verification failed: expected targetTemp ${expected.targetTempCelsius}°C, read ${actual.targetTempCelsius}°C"
                }
            }
            is TwoPointFanConfig -> {
                check(actual is TwoPointFanConfig) {
                    "Verification failed: expected TwoPointFanConfig, got ${actual::class.simpleName}"
                }
                check(actual.sensorIndex == expected.sensorIndex) {
                    "Verification failed: expected sensorIndex ${expected.sensorIndex}, read ${actual.sensorIndex}"
                }
            }
            is CopyFanConfig -> {
                check(actual is CopyFanConfig) {
                    "Verification failed: expected CopyFanConfig, got ${actual::class.simpleName}"
                }
                check(actual.sourceChannel == expected.sourceChannel) {
                    "Verification failed: expected sourceChannel ${expected.sourceChannel}, read ${actual.sourceChannel}"
                }
            }
            is UnconfiguredFanConfig -> Unit
            is LimitsOnlyFanConfig -> Unit
        }
    }
}
