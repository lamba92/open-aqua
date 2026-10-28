package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.util.combineByKey
import io.github.lamba92.openaqua.app.util.throttleLatest
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.device.DeviceConfig
import io.github.lamba92.openaqua.model.device.DeviceDescriptor
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import io.github.lamba92.openaqua.model.device.DeviceTelemetry
import io.github.lamba92.openaqua.model.every
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.SerializationException
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DeviceDataCacheService(
    private val openAquaEngineApi: OpenAquaEngineApi,
    coroutineScopeProvider: CoroutineScopeProvider,
    pollingIntervals: PollingIntervals = PollingIntervals.DEFAULT,
) : OpenAquaDeviceFlasher by openAquaEngineApi {
    data class PollingIntervals(
        val deviceConfig: Duration = 2.seconds,
        val telemetry: Duration = 1.seconds,
    ) {
        companion object {
            val DEFAULT = PollingIntervals()
        }
    }

    sealed interface State {
        data object Loading : State

        @JvmInline
        value class Ready(
            val deviceStates: List<DeviceState>,
        ) : State

        data object NoDevices : State
    }

    private val coroutineScope = coroutineScopeProvider.getScope()

    val state: StateFlow<State> =
        every(pollingIntervals.deviceConfig) { openAquaEngineApi.listDevices().devices }
            .combineByKey(
                keySelector = { it.id },
                itemFlow = { device -> device.getStateFlow(pollingIntervals) },
            ).map { if (it.isEmpty()) State.NoDevices else State.Ready(it) }
            .throttleLatest(pollingIntervals.telemetry)
            .stateIn(
                scope = coroutineScope,
                started = SharingStarted.Eagerly,
                initialValue = State.Loading,
            )

    fun getDeviceStateFlow(deviceId: DeviceId): Flow<DeviceState> =
        state
            .filterIsInstance<State.Ready>()
            .mapNotNull { ready -> ready.deviceStates.firstOrNull { it.device.id == deviceId } }

    fun getReadyDeviceStateFlow(deviceId: DeviceId): Flow<DeviceState.Ready> =
        getDeviceStateFlow(deviceId)
            .filterIsInstance<DeviceState.Ready>()

    private fun DetectedHardwareDevice.getStateFlow(pollingIntervals: PollingIntervals): Flow<DeviceState> {
        val descriptor = DeviceRegistry.findByType(type)
        return combine<DeviceConfig, DeviceTelemetry, DeviceState>(
            every(pollingIntervals.deviceConfig) { openAquaEngineApi.getDeviceConfig(id) },
            openAquaEngineApi.getTelemetryStream(id),
        ) { config, telemetry ->
            DeviceState.Ready(
                device = this@getStateFlow,
                descriptor = descriptor,
                telemetry = telemetry,
                config = config,
            )
        }.onStart { emit(DeviceState.Connecting(this@getStateFlow, descriptor)) }
            .catch { cause ->
                System.err.println("[DeviceDataCacheService] Error reading device '$id' ($name): ${cause.message}")
                cause.printStackTrace()
                emit(DeviceState.Error(this@getStateFlow, descriptor, cause.toDeviceErrorType()))
            }
    }
}

private fun Throwable.toDeviceErrorType(): DeviceErrorType =
    when (this) {
        is TimeoutCancellationException -> DeviceErrorType.CommunicationTimeout
        is SerializationException -> DeviceErrorType.ProtocolMismatch
        is IOException -> DeviceErrorType.IoFailure
        else -> DeviceErrorType.Unknown
    }

sealed interface DeviceErrorType {
    data object CommunicationTimeout : DeviceErrorType

    data object IoFailure : DeviceErrorType

    data object ProtocolMismatch : DeviceErrorType

    data object Unknown : DeviceErrorType
}

sealed interface DeviceState {
    val device: DetectedHardwareDevice
    val descriptor: DeviceDescriptor

    data class Connecting(
        override val device: DetectedHardwareDevice,
        override val descriptor: DeviceDescriptor,
    ) : DeviceState

    data class Error(
        override val device: DetectedHardwareDevice,
        override val descriptor: DeviceDescriptor,
        val errorType: DeviceErrorType,
    ) : DeviceState

    data class Ready(
        override val device: DetectedHardwareDevice,
        override val descriptor: DeviceDescriptor,
        val telemetry: DeviceTelemetry,
        val config: DeviceConfig,
    ) : DeviceState
}
