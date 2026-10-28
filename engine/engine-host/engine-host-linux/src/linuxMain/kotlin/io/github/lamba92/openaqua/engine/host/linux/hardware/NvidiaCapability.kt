package io.github.lamba92.openaqua.engine.host.linux.hardware

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.posix.RTLD_LAZY
import platform.posix.dlclose
import platform.posix.dlopen
import platform.posix.dlsym

sealed interface NvidiaCapability {
    object NotAvailable : NvidiaCapability

    interface Available : NvidiaCapability {
        fun getGpus(): List<NvidiaGpu>

        fun getGpuTemperature(gpuIndex: Int): Double?
    }

    companion object {
        fun get() = Nvml.loadOrNull() ?: NotAvailable
    }
}

data class NvidiaGpu(
    val index: Int,
    val name: String,
)

/**
 * NVIDIA Management Library (NVML) wrapper for querying NVIDIA GPU devices and sensors.
 */
private class Nvml private constructor(
    private val libraryHandle: COpaquePointer,
    private val nvmlShutdown: CPointer<CFunction<() -> Int>>,
    private val nvmlDeviceGetCount: CPointer<CFunction<(CPointer<UIntVar>) -> Int>>,
    private val nvmlDeviceGetHandleByIndex: CPointer<CFunction<(UInt, CPointer<COpaquePointerVar>) -> Int>>,
    private val nvmlDeviceGetName: CPointer<CFunction<(COpaquePointer, CPointer<ByteVar>, UInt) -> Int>>,
    private val nvmlDeviceGetTemperature: CPointer<CFunction<(COpaquePointer, UInt, CPointer<UIntVar>) -> Int>>,
) : NvidiaCapability.Available {
    override fun getGpus(): List<NvidiaGpu> =
        memScoped {
            val countVar = alloc<UIntVar>()
            if (nvmlDeviceGetCount(countVar.ptr) != 0) return emptyList()

            val count = countVar.value.toInt()
            val gpus = mutableListOf<NvidiaGpu>()

            for (i in 0 until count) {
                val devHandle = alloc<COpaquePointerVar>()
                if (nvmlDeviceGetHandleByIndex(i.toUInt(), devHandle.ptr) != 0) continue

                val dev = devHandle.value ?: continue
                val nameBuf = allocArray<ByteVar>(64)
                val gpuName =
                    when (nvmlDeviceGetName(dev, nameBuf, 64u)) {
                        0 -> nameBuf.toKString()
                        else -> "NVIDIA GPU $i"
                    }

                gpus.add(
                    NvidiaGpu(
                        index = i,
                        name = gpuName,
                    ),
                )
            }
            gpus
        }

    override fun getGpuTemperature(gpuIndex: Int): Double? =
        memScoped {
            val devHandle = alloc<COpaquePointerVar>()
            if (nvmlDeviceGetHandleByIndex(gpuIndex.toUInt(), devHandle.ptr) != 0) return null

            val dev = devHandle.value ?: return null
            val tempVar = alloc<UIntVar>()
            when (nvmlDeviceGetTemperature(dev, NVML_TEMPERATURE_GPU, tempVar.ptr)) {
                0 -> tempVar.value.toDouble()
                else -> null
            }
        }

    fun close() {
        nvmlShutdown()
        dlclose(libraryHandle)
    }

    companion object {
        private const val NVML_TEMPERATURE_GPU: UInt = 0u

        internal fun loadOrNull(): Nvml? {
            val libraryNames = listOf("libnvidia-ml.so.1", "libnvidia-ml.so")
            var handle: COpaquePointer? = null
            for (lib in libraryNames) {
                handle = dlopen(lib, RTLD_LAZY)
                if (handle != null) break
            }
            if (handle == null) return null

            val nvmlInit =
                dlsym(handle, "nvmlInit_v2")?.reinterpret<CFunction<() -> Int>>() ?: run {
                    dlclose(handle)
                    return null
                }
            val nvmlShutdown =
                dlsym(handle, "nvmlShutdown")?.reinterpret<CFunction<() -> Int>>() ?: run {
                    dlclose(handle)
                    return null
                }
            val nvmlDeviceGetCount =
                dlsym(handle, "nvmlDeviceGetCount_v2")?.reinterpret<CFunction<(CPointer<UIntVar>) -> Int>>() ?: run {
                    dlclose(handle)
                    return null
                }
            val nvmlDeviceGetHandleByIndex =
                dlsym(
                    handle,
                    "nvmlDeviceGetHandleByIndex_v2",
                )?.reinterpret<CFunction<(UInt, CPointer<COpaquePointerVar>) -> Int>>()
                    ?: run {
                        dlclose(handle)
                        return null
                    }
            val nvmlDeviceGetName =
                dlsym(
                    handle,
                    "nvmlDeviceGetName",
                )?.reinterpret<CFunction<(COpaquePointer, CPointer<ByteVar>, UInt) -> Int>>() ?: run {
                    dlclose(handle)
                    return null
                }
            val nvmlDeviceGetTemperature =
                dlsym(
                    handle,
                    "nvmlDeviceGetTemperature",
                )?.reinterpret<CFunction<(COpaquePointer, UInt, CPointer<UIntVar>) -> Int>>()
                    ?: run {
                        dlclose(handle)
                        return null
                    }

            if (nvmlInit() != 0) {
                dlclose(handle)
                return null
            }

            return Nvml(
                libraryHandle = handle,
                nvmlShutdown = nvmlShutdown,
                nvmlDeviceGetCount = nvmlDeviceGetCount,
                nvmlDeviceGetHandleByIndex = nvmlDeviceGetHandleByIndex,
                nvmlDeviceGetName = nvmlDeviceGetName,
                nvmlDeviceGetTemperature = nvmlDeviceGetTemperature,
            )
        }
    }
}
