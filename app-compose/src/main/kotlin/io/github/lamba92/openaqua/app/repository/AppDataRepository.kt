package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.model.AppStoredData
import io.github.lamba92.openaqua.app.model.DeviceStoredData
import io.github.lamba92.openaqua.app.model.FanStoredData
import io.github.lamba92.openaqua.app.model.SensorStoredData
import io.github.lamba92.openaqua.model.DeviceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class AppDataRepository(
    coroutineScopeProvider: CoroutineScopeProvider,
    private val storagePath: Path = defaultAppDataPath(),
) {
    sealed interface State {
        data object Loading : State

        @JvmInline
        value class Ready(
            val data: AppStoredData,
        ) : State
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
            state.value = State.Ready(loadFromDisk())
            state
                .filterIsInstance<State.Ready>()
                .map { it.data }
                .distinctUntilChanged()
                .flowOn(Dispatchers.Default)
                .onEach { saveToDisk(it) }
                .collect()
        }
    }

    fun getDeviceDataFlow(deviceId: DeviceId): Flow<DeviceStoredData> =
        state
            .filterIsInstance<State.Ready>()
            .map { it.data.devices[deviceId] ?: DeviceStoredData() }
            .distinctUntilChanged()

    fun update(transform: (AppStoredData) -> AppStoredData) {
        state.update { current ->
            when (current) {
                State.Loading -> current
                is State.Ready -> State.Ready(transform(current.data))
            }
        }
    }

    fun updateDevice(
        deviceId: DeviceId,
        transform: (DeviceStoredData) -> DeviceStoredData,
    ) {
        update { current ->
            val currentDevice = current.devices[deviceId] ?: DeviceStoredData()
            val updatedDevice = transform(currentDevice)
            val updatedDevices =
                if (updatedDevice == DeviceStoredData()) {
                    current.devices - deviceId
                } else {
                    current.devices + (deviceId to updatedDevice)
                }
            current.copy(devices = updatedDevices)
        }
    }

    fun setDeviceName(
        deviceId: DeviceId,
        name: String?,
    ) {
        val normalized = name?.trim()?.takeIf { it.isNotEmpty() }
        updateDevice(deviceId) { device ->
            device.copy(customName = normalized)
        }
    }

    fun setFanName(
        deviceId: DeviceId,
        channel: Int,
        name: String?,
    ) {
        val normalized = name?.trim()?.takeIf { it.isNotEmpty() }
        updateDevice(deviceId) { device ->
            val currentFan = device.fans[channel] ?: FanStoredData()
            val updatedFan = currentFan.copy(customName = normalized)
            val updatedFans =
                if (updatedFan == FanStoredData()) {
                    device.fans - channel
                } else {
                    device.fans + (channel to updatedFan)
                }
            device.copy(fans = updatedFans)
        }
    }

    fun setSensorName(
        deviceId: DeviceId,
        channel: Int,
        name: String?,
    ) {
        val normalized = name?.trim()?.takeIf { it.isNotEmpty() }
        updateDevice(deviceId) { device ->
            val currentSensor = device.sensors[channel] ?: SensorStoredData()
            val updatedSensor = currentSensor.copy(customName = normalized)
            val updatedSensors =
                if (updatedSensor == SensorStoredData()) {
                    device.sensors - channel
                } else {
                    device.sensors + (channel to updatedSensor)
                }
            device.copy(sensors = updatedSensors)
        }
    }

    private fun loadFromDisk(): AppStoredData =
        if (!storagePath.exists()) {
            AppStoredData()
        } else {
            json.decodeFromString<AppStoredData>(storagePath.readText())
        }

    private fun saveToDisk(storedData: AppStoredData) {
        storagePath.parent?.createDirectories()
        val tmpPath = storagePath.resolveSibling("${storagePath.fileName}.tmp")
        val encoded = json.encodeToString(AppStoredData.serializer(), storedData)
        tmpPath.writeText(encoded)
        try {
            Files.move(
                tmpPath,
                storagePath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(tmpPath, storagePath, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        private const val APP_DIR_NAME = "open-aqua"
        private const val APP_DATA_FILE_NAME = "app-data.json"

        fun defaultAppDataPath(): Path {
            val os = System.getProperty("os.name").lowercase()
            val userHome = System.getProperty("user.home")
            val baseDir =
                when {
                    os.contains("win") -> {
                        val appData = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
                        if (appData != null) Path.of(appData) else Path.of(userHome, "AppData", "Roaming")
                    }
                    os.contains("mac") -> {
                        Path.of(userHome, "Library", "Application Support")
                    }
                    else -> {
                        val xdgConfig = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
                        if (xdgConfig != null) Path.of(xdgConfig) else Path.of(userHome, ".config")
                    }
                }
            return baseDir.resolve(APP_DIR_NAME).resolve(APP_DATA_FILE_NAME)
        }
    }
}
