package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getLogger
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.permission.DevicePermissionService
import io.github.lamba92.openaqua.app.permission.DevicePermissionState
import io.github.lamba92.openaqua.app.util.combineByKey
import io.github.lamba92.openaqua.app.util.throttleLatest
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.device.DeviceConfig
import io.github.lamba92.openaqua.model.device.DeviceDescriptor
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import io.github.lamba92.openaqua.model.device.DeviceTelemetry
import io.github.lamba92.openaqua.model.every
import io.github.lamba92.openaqua.model.host.HostInfo
import io.github.lamba92.openaqua.model.host.HostSensorId
import io.github.lamba92.openaqua.model.host.HostSensorTelemetry
import io.github.lamba92.openaqua.model.host.HostTelemetry
import io.github.lamba92.openaqua.model.retryAtLeast
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.SerializationException
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DeviceDataCacheService(
    private val openAquaDeviceEngineApi: OpenAquaDeviceEngineApi,
    private val openAquaHostEngineApi: OpenAquaHostEngineApi,
    private val permissionService: DevicePermissionService,
    coroutineScopeProvider: CoroutineScopeProvider,
    private val pollingIntervals: PollingIntervals = PollingIntervals.DEFAULT,
) : OpenAquaDeviceFlasher by openAquaDeviceEngineApi {
    data class PollingIntervals(
        val deviceConfig: Duration = 2.seconds,
        val telemetry: Duration = 1.seconds,
    ) {
        companion object {
            val DEFAULT = PollingIntervals()
        }
    }

    private val coroutineScope = coroutineScopeProvider.getScope()

    private fun getHostFlow(): Flow<DeviceState.Host> =
        flow {
            val info = openAquaHostEngineApi.getHostInfo()
            getLogger().i { "Host engine connected with ${info.devices.size} device(s): ${info.devices.joinToString { it.name }}" }
            emit(DeviceState.Host.Connecting(info))
            openAquaHostEngineApi
                .getTelemetryStream()
                .map { telemetry ->
                    DeviceState.Host.Ready(info = info, telemetry = telemetry)
                }.retryAtLeast(3)
                .catch { cause ->
                    getLogger().e(cause) { "Host engine telemetry stream error: ${cause.message}" }
                    emit(
                        DeviceState.Host.Error(
                            info = info,
                            errorType = cause.toDeviceErrorType(),
                            message = cause.message,
                        ),
                    )
                }.collect { emit(it) }
        }.retryAtLeast(3)
            .catch { cause ->
                getLogger().e(cause) { "Failed to read host info: ${cause.message}" }
                emit(
                    DeviceState.Host.Error(
                        info = null,
                        errorType = cause.toDeviceErrorType(),
                        message = cause.message,
                    ),
                )
            }

    val state: StateFlow<List<DeviceState>> =
        combine(
            getHostFlow(),
            getRestartingDevicesFlow(),
        ) { host, aquaDevices ->
            listOf(host) + aquaDevices
        }.throttleLatest(pollingIntervals.telemetry)
            .stateIn(
                scope = coroutineScope,
                started = SharingStarted.Eagerly,
                initialValue = listOf(DeviceState.Host.Loading),
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getRestartingDevicesFlow(): Flow<List<DeviceState.Aqua>> =
        permissionService
            .state
            .flatMapLatest { permState ->
                when (permState) {
                    is DevicePermissionState.Granted -> getDevicesFlow()
                    else -> flowOf(emptyList())
                }
            }

    private fun getDevicesFlow(): Flow<List<DeviceState.Aqua>> =
        every(pollingIntervals.deviceConfig) { openAquaDeviceEngineApi.listDevices().devices }
            .retryAtLeast(3)
            .distinctUntilChanged()
            .onEach { devices ->
                coroutineScope.logger.i {
                    when {
                        devices.isEmpty() -> "No Aqua devices detected"
                        else -> "Detected ${devices.size} Aqua device(s): ${devices.joinToString { "${it.name} (${it.id})" }}"
                    }
                }
            }.combineByKey(
                keySelector = { it.id },
                itemFlow = { device -> device.getStateFlow(pollingIntervals) },
            )

    /**
     * Real-time stream of all connected host sensor temperatures mapped by HostSensorId.
     * Centralized telemetry access for UI and virtual sensor feeds.
     */
    val hostTemperatures: Flow<Map<HostSensorId, Double>> =
        getHostStateFlow()
            .filterIsInstance<DeviceState.Host.Ready>()
            .map { ready ->
                ready.telemetry.sensors
                    .filterIsInstance<HostSensorTelemetry.Connected>()
                    .associate { it.id to it.tempCelsius }
            }

    fun getHostStateFlow(): Flow<DeviceState.Host> =
        state.mapNotNull { list ->
            list.filterIsInstance<DeviceState.Host>().firstOrNull()
        }

    fun getAquaDeviceStateFlow(deviceId: DeviceId): Flow<DeviceState.Aqua> =
        state.mapNotNull { list ->
            list
                .filterIsInstance<DeviceState.Aqua>()
                .firstOrNull { it.device.id == deviceId }
        }

    fun getReadyAquaDeviceStateFlow(deviceId: DeviceId): Flow<DeviceState.Aqua.Ready> =
        getAquaDeviceStateFlow(deviceId)
            .filterIsInstance<DeviceState.Aqua.Ready>()

    private fun DetectedHardwareDevice.getStateFlow(pollingIntervals: PollingIntervals): Flow<DeviceState.Aqua> {
        val descriptor = DeviceRegistry.findByType(type)
        var isFirstReady = true
        return combine<DeviceConfig, DeviceTelemetry, DeviceState.Aqua>(
            every(pollingIntervals.deviceConfig) { openAquaDeviceEngineApi.getDeviceConfig(id) }
                .retryAtLeast(3),
            openAquaDeviceEngineApi
                .getTelemetryStream(id)
                .retryAtLeast(3),
        ) { config, telemetry ->
            DeviceState.Aqua.Ready(
                device = this@getStateFlow,
                descriptor = descriptor,
                telemetry = telemetry,
                config = config,
            )
        }.onStart {
            getLogger().i { "Connecting to Aqua device '$name' (id=$id, type=$type)..." }
            emit(DeviceState.Aqua.Connecting(this@getStateFlow, descriptor))
        }.onEach { state ->
            if (state is DeviceState.Aqua.Ready && isFirstReady) {
                isFirstReady = false
                getLogger().i { "Aqua device '$name' (id=$id) is ready with ${state.config.fans.size} fan(s)" }
            }
        }.catch { cause ->
            getLogger().e(cause) { "Error reading device '$id' ($name): ${cause.message}" }
            emit(DeviceState.Aqua.Error(this@getStateFlow, descriptor, cause.toDeviceErrorType()))
        }
    }
}

private fun Throwable.toDeviceErrorType(): DeviceErrorType =
    when {
        this is TimeoutCancellationException -> DeviceErrorType.CommunicationTimeout

        this is SerializationException -> DeviceErrorType.ProtocolMismatch

        this is IOException -> DeviceErrorType.IoFailure

        message?.contains("open", ignoreCase = true) == true ||
            message?.contains("permission", ignoreCase = true) == true ||
            message?.contains("access", ignoreCase = true) == true ||
            message?.contains("hidraw", ignoreCase = true) == true ||
            message?.contains("device", ignoreCase = true) == true -> DeviceErrorType.IoFailure

        else -> DeviceErrorType.Unknown
    }

sealed interface DeviceErrorType {
    data object CommunicationTimeout : DeviceErrorType

    data object IoFailure : DeviceErrorType

    data object ProtocolMismatch : DeviceErrorType

    data object Unknown : DeviceErrorType
}

sealed interface DeviceState {
    sealed interface Host : DeviceState {
        data object Loading : Host

        data class Connecting(
            val info: HostInfo,
        ) : Host

        data class Ready(
            val info: HostInfo,
            val telemetry: HostTelemetry,
        ) : Host

        data class Error(
            val info: HostInfo? = null,
            val errorType: DeviceErrorType,
            val message: String? = null,
        ) : Host
    }

    sealed interface Aqua : DeviceState {
        val device: DetectedHardwareDevice
        val descriptor: DeviceDescriptor

        data class Connecting(
            override val device: DetectedHardwareDevice,
            override val descriptor: DeviceDescriptor,
        ) : Aqua

        data class Error(
            override val device: DetectedHardwareDevice,
            override val descriptor: DeviceDescriptor,
            val errorType: DeviceErrorType,
        ) : Aqua

        data class Ready(
            override val device: DetectedHardwareDevice,
            override val descriptor: DeviceDescriptor,
            val telemetry: DeviceTelemetry,
            val config: DeviceConfig,
        ) : Aqua
    }
}
