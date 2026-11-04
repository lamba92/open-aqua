package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getLogger
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.coroutine.logger
import io.github.lamba92.openaqua.app.feature.device.common.ChannelName
import io.github.lamba92.openaqua.app.model.AppStoredData
import io.github.lamba92.openaqua.app.model.BaseAppDataDir
import io.github.lamba92.openaqua.app.model.DeviceStoredData
import io.github.lamba92.openaqua.app.model.FanStoredData
import io.github.lamba92.openaqua.app.model.NavigationSelectionStoredData
import io.github.lamba92.openaqua.app.model.SensorStoredData
import io.github.lamba92.openaqua.app.model.VirtualSensorFeedSource
import io.github.lamba92.openaqua.app.model.WindowStateStoredData
import io.github.lamba92.openaqua.app.model.withVirtualSensorFeed
import io.github.lamba92.openaqua.app.util.atomicMove
import io.github.lamba92.openaqua.app.util.deleteIfExists
import io.github.lamba92.openaqua.app.util.openInDesktopEnvironment
import io.github.lamba92.openaqua.app.util.readText
import io.github.lamba92.openaqua.app.util.resolveSibling
import io.github.lamba92.openaqua.app.util.writeText
import io.github.lamba92.openaqua.model.device.DeviceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class AppDataRepository(
    coroutineScopeProvider: CoroutineScopeProvider,
    val baseAppDataDir: BaseAppDataDir,
) {
    sealed interface State {
        data object Loading : State

        @JvmInline
        value class Ready(
            val data: AppStoredData,
        ) : State
    }

    fun setNavigationSelection(selection: NavigationSelectionStoredData?) {
        update { it.copy(navigationSelection = selection) }
    }

    fun saveWindowStateSync(windowState: WindowStateStoredData) {
        update { it.copy(windowState = windowState) }
        val current = (state.value as? State.Ready)?.data ?: return
        saveToDisk(current)
    }

    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private val coroutineScope = coroutineScopeProvider.getScope(Dispatchers.IO)

    val state: StateFlow<State>
        field = MutableStateFlow<State>(State.Loading)

    init {
        coroutineScope.launch {
            val initial = loadFromDisk()
            logger.i { "Loaded configuration from ${baseAppDataDir.configFile.path}" }
            state.value = State.Ready(initial)
            state
                .filterIsInstance<State.Ready>()
                .map { it.data }
                .distinctUntilChanged()
                .flowOn(Dispatchers.Default)
                .onEach {
                    saveToDisk(it)
                    getLogger().d { "Saved updated configuration to ${baseAppDataDir.configFile.path}" }
                }.collect()
        }
    }

    fun update(transform: (AppStoredData) -> AppStoredData) {
        state.update { current ->
            when (current) {
                State.Loading -> current
                is State.Ready -> State.Ready(transform(current.data))
            }
        }
    }

    fun resetToDefaults() {
        update { AppStoredData.DEFAULT }
    }

    fun setHideDisconnectedFans(hide: Boolean) {
        update { it.copy(hideDisconnectedFans = hide) }
    }

    fun setHideDisconnectedSensorsInFans(hide: Boolean) {
        update { it.copy(hideDisconnectedSensorsInFans = hide) }
    }

    fun setHideDisconnectedSensors(hide: Boolean) {
        update { it.copy(hideDisconnectedSensors = hide) }
    }

    fun setHideUnconfiguredVirtualSensors(hide: Boolean) {
        update { it.copy(hideUnconfiguredVirtualSensors = hide) }
    }

    fun openDataFolder() {
        coroutineScope.launch {
            baseAppDataDir.path.openInDesktopEnvironment()
        }
    }

    fun updateDevice(
        deviceId: DeviceId,
        transform: (DeviceStoredData) -> DeviceStoredData,
    ) {
        update { current ->
            val currentDevice = current.devices[deviceId] ?: DeviceStoredData.DEFAULT
            val updatedDevices =
                when (val updatedDevice = transform(currentDevice)) {
                    DeviceStoredData.DEFAULT -> current.devices - deviceId
                    else -> current.devices + (deviceId to updatedDevice)
                }
            current.copy(devices = updatedDevices)
        }
    }

    fun setDeviceName(
        deviceId: DeviceId,
        name: ChannelName,
    ) {
        updateDevice(deviceId) { device ->
            device.copy(name = name)
        }
    }

    fun setFanName(
        deviceId: DeviceId,
        channel: Int,
        name: ChannelName,
    ) {
        updateDevice(deviceId) { device ->
            val currentFan = device.fans[channel] ?: FanStoredData.DEFAULT
            val updatedFans =
                when (val updatedFan = currentFan.copy(name = name)) {
                    FanStoredData.DEFAULT -> device.fans - channel
                    else -> device.fans + (channel to updatedFan)
                }
            device.copy(fans = updatedFans)
        }
    }

    fun setSensorName(
        deviceId: DeviceId,
        channel: Int,
        name: ChannelName,
    ) {
        updateDevice(deviceId) { device ->
            val currentSensor = device.sensors[channel] ?: SensorStoredData.DEFAULT
            val updatedSensors =
                when (val updatedSensor = currentSensor.copy(name = name)) {
                    SensorStoredData.DEFAULT -> device.sensors - channel
                    else -> device.sensors + (channel to updatedSensor)
                }
            device.copy(sensors = updatedSensors)
        }
    }

    fun setSensorVirtualFeed(
        deviceId: DeviceId,
        channel: Int,
        feedSource: VirtualSensorFeedSource?,
    ) {
        update { it.withVirtualSensorFeed(deviceId, channel, feedSource) }
    }

    private fun loadFromDisk(): AppStoredData =
        try {
            json.decodeFromString<AppStoredData>(baseAppDataDir.configFile.path.readText())
        } catch (_: Exception) {
            baseAppDataDir.configFile.path.deleteIfExists()
            AppStoredData()
        }

    private fun saveToDisk(storedData: AppStoredData) {
        val configFile = baseAppDataDir.configFile.path
        val tmpPath = configFile.resolveSibling("${configFile.name}.tmp")
        val encoded = json.encodeToString(storedData)
        tmpPath.writeText(encoded)
        tmpPath.atomicMove(configFile)
    }
}
