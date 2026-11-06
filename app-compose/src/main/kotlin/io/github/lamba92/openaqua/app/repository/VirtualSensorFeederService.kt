package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.model.AppStoredData
import io.github.lamba92.openaqua.app.model.DeviceStoredData
import io.github.lamba92.openaqua.app.model.VirtualSensorFeedError
import io.github.lamba92.openaqua.app.model.VirtualSensorFeedSource
import io.github.lamba92.openaqua.app.util.throttleLatest
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.FeedVirtualSensorsRequest
import io.github.lamba92.openaqua.model.device.SensorTelemetry
import io.github.lamba92.openaqua.model.host.HostSensorTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class VirtualSensorFeederService(
    private val openAquaDeviceEngineApi: OpenAquaDeviceEngineApi,
    private val deviceDataCacheService: DeviceDataCacheService,
    appDataRepository: AppDataRepository,
    coroutineScopeProvider: CoroutineScopeProvider,
    private val feedInterval: Duration = 1.seconds,
) {
    private val coroutineScope = coroutineScopeProvider.getScope(Dispatchers.Default)
    private val logger = coroutineScope.logger

    private val deviceFeedSources = mutableMapOf<DeviceId, MutableStateFlow<Map<Int, VirtualSensorFeedSource>>>()
    private val deviceFeederJobs = mutableMapOf<DeviceId, Job>()

    val state: StateFlow<Map<DeviceId, Map<Int, VirtualSensorFeedError>>> =
        combine(
            appDataRepository.state.filterIsInstance<AppDataRepository.State.Ready>(),
            deviceDataCacheService.state,
        ) { readyState, availableDevices ->
            computeAllErrors(readyState.data, availableDevices)
        }.stateIn(coroutineScope, SharingStarted.Eagerly, emptyMap())

    init {
        logSensorFeedErrors()
        startFeedingVirtualSensors(appDataRepository)
    }

    private fun startFeedingVirtualSensors(appDataRepository: AppDataRepository) {
        coroutineScope.launch {
            appDataRepository
                .state
                .filterIsInstance<AppDataRepository.State.Ready>()
                .map { readyState ->
                    readyState
                        .data
                        .devices
                        .mapValues { (_, deviceData) ->
                            deviceData.sensors.mapNotNullValues { it.value.virtualFeedSource }
                        }
                }.distinctUntilChanged()
                .collect { allConfiguredFeeds ->
                    syncDeviceFeeders(allConfiguredFeeds)
                }
        }
    }

    private fun syncDeviceFeeders(allConfiguredFeeds: Map<DeviceId, Map<Int, VirtualSensorFeedSource>>) {
        allConfiguredFeeds.forEach { (deviceId, feeds) ->
            if (feeds.isEmpty()) {
                deviceFeederJobs.remove(deviceId)?.cancel()
                deviceFeedSources.remove(deviceId)
            } else {
                val existingFlow = deviceFeedSources[deviceId]
                if (existingFlow != null) {
                    existingFlow.value = feeds
                } else {
                    val sourcesFlow = MutableStateFlow(feeds)
                    deviceFeedSources[deviceId] = sourcesFlow
                    deviceFeederJobs[deviceId] =
                        coroutineScope.launch {
                            try {
                                openAquaDeviceEngineApi.feedVirtualSensors(
                                    deviceId = deviceId,
                                    source = buildDeviceRequestFlow(sourcesFlow),
                                )
                            } finally {
                                deviceFeedSources.remove(deviceId)
                                deviceFeederJobs.remove(deviceId)
                            }
                        }
                }
            }
        }

        val removedDevices = deviceFeedSources.keys - allConfiguredFeeds.keys
        removedDevices.forEach { deviceId ->
            deviceFeederJobs.remove(deviceId)?.cancel()
            deviceFeedSources.remove(deviceId)
        }
    }

    private fun buildDeviceRequestFlow(sourcesFlow: StateFlow<Map<Int, VirtualSensorFeedSource>>): Flow<FeedVirtualSensorsRequest> =
        flow {
            combine(
                sourcesFlow,
                deviceDataCacheService.state,
            ) { sources, availableDevices ->
                sources to availableDevices
            }.throttleLatest(feedInterval)
                .collect { (sources, availableDevices) ->
                    if (sources.isEmpty()) {
                        return@collect
                    }
                    val request = buildFeedVirtualSensorsRequest(sources, availableDevices)
                    emit(request)
                }
        }

    private fun logSensorFeedErrors() {
        state
            .onEach { allErrors ->
                when {
                    allErrors.isEmpty() -> {
                        logger.d { "All virtual sensor feeds are operational" }
                    }

                    else -> {
                        allErrors.forEach { (deviceId, deviceErrors) ->
                            deviceErrors.forEach { (channel, error) ->
                                logger.e { "Virtual feed error on device '$deviceId' channel $channel: $error" }
                            }
                        }
                    }
                }
            }.launchIn(coroutineScope)
    }

    private fun computeAllErrors(
        appData: AppStoredData,
        availableDevices: List<DeviceState>,
    ): Map<DeviceId, Map<Int, VirtualSensorFeedError>> {
        val readyAquaDevices =
            availableDevices
                .filterIsInstance<DeviceState.Aqua.Ready>()
                .associateBy { it.device.id }
        val readyHost =
            availableDevices
                .filterIsInstance<DeviceState.Host.Ready>()
                .firstOrNull()

        return appData
            .devices
            .mapNotNull { (destinationDeviceId, deviceData) ->
                val errors = computeDeviceErrors(destinationDeviceId, deviceData, readyAquaDevices, readyHost)
                if (errors.isNotEmpty()) destinationDeviceId to errors else null
            }.toMap()
    }

    private fun computeDeviceErrors(
        destinationDeviceId: DeviceId,
        deviceData: DeviceStoredData,
        readyAquaDevices: Map<DeviceId, DeviceState.Aqua.Ready>,
        readyHost: DeviceState.Host.Ready?,
    ): Map<Int, VirtualSensorFeedError> =
        deviceData
            .sensors
            .mapNotNull { (channel, sensor) ->
                val source = sensor.virtualFeedSource ?: return@mapNotNull null
                val error = detectFeedError(destinationDeviceId, channel, source, readyAquaDevices, readyHost)
                error?.let { channel to it }
            }.toMap()

    private fun detectFeedError(
        destinationDeviceId: DeviceId,
        destinationChannel: Int,
        source: VirtualSensorFeedSource,
        readyAquaDevices: Map<DeviceId, DeviceState.Aqua.Ready>,
        readyHost: DeviceState.Host.Ready?,
    ): VirtualSensorFeedError? =
        when (destinationDeviceId) {
            !in readyAquaDevices -> {
                VirtualSensorFeedError.DestinationDeviceDisconnected(destinationDeviceId, destinationChannel)
            }

            else -> {
                when (source) {
                    is VirtualSensorFeedSource.Device -> {
                        when (val sourceDevice = readyAquaDevices[source.deviceId]) {
                            null -> {
                                VirtualSensorFeedError.SourceDeviceUnavailable(
                                    destinationDeviceId = destinationDeviceId,
                                    destinationChannel = destinationChannel,
                                    source = source,
                                )
                            }

                            else -> {
                                val sensor = sourceDevice.telemetry.sensors.firstOrNull { it.channel == source.channel }
                                when (sensor) {
                                    null, is SensorTelemetry.Disconnected -> {
                                        VirtualSensorFeedError.SourceSensorDisconnected(
                                            destinationDeviceId = destinationDeviceId,
                                            destinationChannel = destinationChannel,
                                            source = source,
                                        )
                                    }

                                    is SensorTelemetry.Connected -> {
                                        null
                                    }
                                }
                            }
                        }
                    }

                    is VirtualSensorFeedSource.Host -> {
                        when (readyHost) {
                            null -> {
                                VirtualSensorFeedError.SourceDeviceUnavailable(
                                    destinationDeviceId = destinationDeviceId,
                                    destinationChannel = destinationChannel,
                                    source = source,
                                )
                            }

                            else -> {
                                val sensor = readyHost.telemetry.sensors.firstOrNull { it.id == source.sensorId }
                                when (sensor) {
                                    null, is HostSensorTelemetry.Disconnected -> {
                                        VirtualSensorFeedError.SourceSensorDisconnected(
                                            destinationDeviceId = destinationDeviceId,
                                            destinationChannel = destinationChannel,
                                            source = source,
                                        )
                                    }

                                    is HostSensorTelemetry.Connected -> {
                                        null
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    private fun buildFeedVirtualSensorsRequest(
        destinationChannelIdToSourceSensorMap: Map<Int, VirtualSensorFeedSource>,
        availableDevices: List<DeviceState>,
    ): FeedVirtualSensorsRequest =
        destinationChannelIdToSourceSensorMap
            .mapNotNullValues { (_, source) ->
                resolveFeedSourceValue(source, availableDevices)
            }.let { FeedVirtualSensorsRequest(it) }
}

fun resolveFeedSourceValue(
    source: VirtualSensorFeedSource?,
    availableDevices: List<DeviceState>,
): Double? {
    if (source == null) return null
    return when (source) {
        is VirtualSensorFeedSource.Device -> {
            val sensor =
                availableDevices
                    .filterIsInstance<DeviceState.Aqua.Ready>()
                    .firstOrNull { it.device.id == source.deviceId }
                    ?.telemetry
                    ?.sensors
                    ?.firstOrNull { it.channel == source.channel }
            when (sensor) {
                is SensorTelemetry.Connected -> sensor.tempCelsius
                is SensorTelemetry.Disconnected, null -> null
            }
        }

        is VirtualSensorFeedSource.Host -> {
            val sensor =
                availableDevices
                    .filterIsInstance<DeviceState.Host.Ready>()
                    .firstOrNull()
                    ?.telemetry
                    ?.sensors
                    ?.firstOrNull { it.id == source.sensorId }
            when (sensor) {
                is HostSensorTelemetry.Connected -> sensor.tempCelsius
                is HostSensorTelemetry.Disconnected, null -> null
            }
        }
    }
}

private fun <K, V1, V2> Map<K, V1>.mapNotNullValues(function: (Map.Entry<K, V1>) -> V2?): Map<K, V2> =
    buildMap {
        this@mapNotNullValues.forEach { entry ->
            function(entry)?.let { put(entry.key, it) }
        }
    }
