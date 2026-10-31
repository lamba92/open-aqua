package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.coroutine.logger
import io.github.lamba92.openaqua.app.model.VirtualSensorFeedSource
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.FeedVirtualSensorsRequest
import io.github.lamba92.openaqua.model.device.tempCelsiusOrNull
import io.github.lamba92.openaqua.model.host.HostSensorTelemetry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class ChannelFeedStatus(
    val isStreaming: Boolean = false,
    val lastFedTempCelsius: Double? = null,
    val lastFedTimestamp: Long? = null,
)

class VirtualSensorFeederService(
    private val openAquaDeviceEngineApi: OpenAquaDeviceEngineApi,
    private val deviceDataCacheService: DeviceDataCacheService,
    private val appDataRepository: AppDataRepository,
    coroutineScopeProvider: CoroutineScopeProvider,
    private val feedInterval: Duration = 1.seconds,
) {
    private val coroutineScope = coroutineScopeProvider.getScope(Dispatchers.Default)
    private val logger = coroutineScope.logger

    private val _streamingStatus = MutableStateFlow<Map<DeviceId, Map<Int, ChannelFeedStatus>>>(emptyMap())
    val streamingStatus: StateFlow<Map<DeviceId, Map<Int, ChannelFeedStatus>>> = _streamingStatus.asStateFlow()

    private val activeFeederJobs = mutableMapOf<DeviceId, Job>()

    init {
        coroutineScope.launch {
            appDataRepository.state
                .filterIsInstance<AppDataRepository.State.Ready>()
                .map { ready ->
                    ready.data.devices.mapNotNull { (deviceId, deviceData) ->
                        val virtualFeeds =
                            deviceData.sensors.mapNotNull { (channel, sensorData) ->
                                sensorData.virtualFeedSource?.let { channel to it }
                            }.toMap()
                        if (virtualFeeds.isNotEmpty()) deviceId to virtualFeeds else null
                    }.toMap()
                }.distinctUntilChanged()
                .collect { configuredFeedsByDevice ->
                    syncFeederJobs(configuredFeedsByDevice)
                }
        }
    }

    private fun syncFeederJobs(configuredFeedsByDevice: Map<DeviceId, Map<Int, VirtualSensorFeedSource>>) {
        val iterator = activeFeederJobs.iterator()
        while (iterator.hasNext()) {
            val (deviceId, job) = iterator.next()
            if (deviceId !in configuredFeedsByDevice) {
                logger.i { "Cancelling virtual sensor feeder for device '$deviceId' (no configured feeds)" }
                job.cancel()
                iterator.remove()
                updateDeviceStatus(deviceId, emptyMap())
            }
        }

        for (deviceId in configuredFeedsByDevice.keys) {
            val existingJob = activeFeederJobs[deviceId]
            if (existingJob == null || !existingJob.isActive) {
                activeFeederJobs[deviceId] =
                    coroutineScope.launch {
                        runDeviceFeederLoop(deviceId)
                    }
            }
        }
    }

    private suspend fun runDeviceFeederLoop(deviceId: DeviceId) {
        logger.i { "Starting virtual sensor feeder supervisor for device '$deviceId'" }
        while (currentCoroutineContext().isActive) {
            val currentFeeds = getCurrentFeeds(deviceId)
            if (currentFeeds.isEmpty()) {
                logger.i { "No active virtual feeds for device '$deviceId', terminating feeder loop" }
                break
            }

            val deviceReady =
                deviceDataCacheService.state.value
                    .filterIsInstance<DeviceState.Aqua.Ready>()
                    .firstOrNull { it.device.id == deviceId }

            if (deviceReady == null) {
                updateDeviceStatus(deviceId, currentFeeds.keys.associateWith { ChannelFeedStatus(isStreaming = false) })
                delay(1.seconds)
                continue
            }

            if (deviceReady.descriptor.virtualSensorsCount <= 0) {
                logger.w { "Device '$deviceId' (${deviceReady.device.name}) does not support virtual sensors, stopping feeder" }
                updateDeviceStatus(deviceId, currentFeeds.keys.associateWith { ChannelFeedStatus(isStreaming = false) })
                break
            }

            try {
                logger.i { "Launching virtual sensor feeder stream for device '$deviceId'..." }
                val feedStream = createFeedFlow(deviceId)
                openAquaDeviceEngineApi.feedVirtualSensors(deviceId, feedStream)
            } catch (e: CancellationException) {
                updateDeviceStatus(deviceId, currentFeeds.keys.associateWith { ChannelFeedStatus(isStreaming = false) })
                throw e
            } catch (e: Exception) {
                logger.w(e) { "Virtual sensor feeder stream for device '$deviceId' failed: ${e.message}. Retrying in 2s..." }
                updateDeviceStatus(deviceId, currentFeeds.keys.associateWith { ChannelFeedStatus(isStreaming = false) })
                delay(2.seconds)
            }
        }
    }

    private fun createFeedFlow(deviceId: DeviceId): Flow<FeedVirtualSensorsRequest> =
        flow {
            while (currentCoroutineContext().isActive) {
                val feeds = getCurrentFeeds(deviceId)
                if (feeds.isEmpty()) break

                val stateSnapshot = deviceDataCacheService.state.value
                val hostReady = stateSnapshot.filterIsInstance<DeviceState.Host.Ready>().firstOrNull()
                val aquaReadyMap = stateSnapshot.filterIsInstance<DeviceState.Aqua.Ready>().associateBy { it.device.id }

                val requestSensors = mutableMapOf<Int, Double>()
                val channelStatuses = mutableMapOf<Int, ChannelFeedStatus>()
                val now = System.currentTimeMillis()

                for ((channel, source) in feeds) {
                    val temp =
                        when (source) {
                            is VirtualSensorFeedSource.Host -> {
                                hostReady?.telemetry?.sensors
                                    ?.filterIsInstance<HostSensorTelemetry.Connected>()
                                    ?.firstOrNull { it.id == source.sensorId }
                                    ?.tempCelsius
                            }

                            is VirtualSensorFeedSource.Device -> {
                                aquaReadyMap[source.deviceId]?.telemetry?.sensors
                                    ?.getOrNull(source.channel)
                                    ?.tempCelsiusOrNull
                            }
                        }

                    if (temp != null) {
                        requestSensors[channel] = temp
                        channelStatuses[channel] =
                            ChannelFeedStatus(
                                isStreaming = true,
                                lastFedTempCelsius = temp,
                                lastFedTimestamp = now,
                            )
                    } else {
                        channelStatuses[channel] =
                            ChannelFeedStatus(
                                isStreaming = false,
                                lastFedTempCelsius = null,
                                lastFedTimestamp = null,
                            )
                    }
                }

                updateDeviceStatus(deviceId, channelStatuses)

                if (requestSensors.isNotEmpty()) {
                    emit(FeedVirtualSensorsRequest(sensors = requestSensors))
                }

                delay(feedInterval)
            }
        }

    private fun getCurrentFeeds(deviceId: DeviceId): Map<Int, VirtualSensorFeedSource> {
        val readyState = appDataRepository.state.value as? AppDataRepository.State.Ready ?: return emptyMap()
        val deviceData = readyState.data.devices[deviceId] ?: return emptyMap()
        return deviceData.sensors.mapNotNull { (channel, sensor) ->
            sensor.virtualFeedSource?.let { channel to it }
        }.toMap()
    }

    private fun updateDeviceStatus(
        deviceId: DeviceId,
        channels: Map<Int, ChannelFeedStatus>,
    ) {
        _streamingStatus.update { current ->
            if (channels.isEmpty()) current - deviceId else current + (deviceId to channels)
        }
    }
}
