@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.lamba92.openaqua.app.util

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKStringFromUtf16
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import platform.windows.CREATE_NO_WINDOW
import platform.windows.CloseHandle
import platform.windows.CreatePipe
import platform.windows.CreateProcessW
import platform.windows.DWORDVar
import platform.windows.GetExitCodeProcess
import platform.windows.GetModuleFileNameW
import platform.windows.HANDLE
import platform.windows.HANDLE_FLAG_INHERIT
import platform.windows.INFINITE
import platform.windows.PROCESS_INFORMATION
import platform.windows.ReadFile
import platform.windows.SECURITY_ATTRIBUTES
import platform.windows.STARTF_USESTDHANDLES
import platform.windows.STARTUPINFOW
import platform.windows.STILL_ACTIVE
import platform.windows.SetHandleInformation
import platform.windows.TRUE
import platform.windows.TerminateProcess
import platform.windows.WAIT_OBJECT_0
import platform.windows.WaitForSingleObject
import platform.windows.WriteFile

internal actual fun getExecutableDirectory(): Path? =
    memScoped {
        val buffer = allocArray<UShortVar>(1024)
        val len = GetModuleFileNameW(null, buffer, 1024u)
        if (len > 0u) {
            Path(buffer.toKStringFromUtf16()).parent
        } else {
            null
        }
    }

private class MingwPlatformProcess(
    private val hProcess: HANDLE?,
    private val dwProcessId: Long,
    private val hStdoutRead: HANDLE?,
    private val hStderrRead: HANDLE?,
    private var hStdinWrite: HANDLE?,
) : PlatformProcess {
    private val stdoutBuffer = StringBuilder()
    private var cachedExitCode: Int? = null

    override val pid: Long get() = dwProcessId

    override val isAlive: Boolean
        get() {
            if (cachedExitCode != null) return false
            return memScoped {
                val exitCode = alloc<DWORDVar>()
                if (GetExitCodeProcess(hProcess, exitCode.ptr) != 0) {
                    if (exitCode.value == STILL_ACTIVE) {
                        WaitForSingleObject(hProcess, 0u) != WAIT_OBJECT_0
                    } else {
                        cachedExitCode = exitCode.value.toInt()
                        false
                    }
                } else {
                    false
                }
            }
        }

    override val exitCode: Int?
        get() {
            if (cachedExitCode != null) return cachedExitCode
            if (!isAlive) return cachedExitCode
            return null
        }

    override suspend fun readLine(): String? =
        withContext(ioDispatcher) {
            memScoped {
                val buf = allocArray<ByteVar>(512)
                val bytesRead = alloc<DWORDVar>()
                while (true) {
                    val newlineIdx = stdoutBuffer.indexOf('\n')
                    if (newlineIdx >= 0) {
                        val line = stdoutBuffer.substring(0, newlineIdx).trimEnd('\r')
                        stdoutBuffer.deleteRange(0, newlineIdx + 1)
                        return@withContext line
                    }
                    val ok = ReadFile(hStdoutRead, buf, 512u, bytesRead.ptr, null)
                    if (ok == 0 || bytesRead.value == 0u) {
                        return@withContext if (stdoutBuffer.isNotEmpty()) {
                            val line = stdoutBuffer.toString().trimEnd('\r', '\n')
                            stdoutBuffer.clear()
                            line
                        } else {
                            null
                        }
                    }
                    val chunk =
                        buildString {
                            for (i in 0 until bytesRead.value.toInt()) {
                                append(buf[i].toInt().toChar())
                            }
                        }
                    stdoutBuffer.append(chunk)
                }
                null
            }
        }

    override suspend fun readStderr(): String =
        withContext(ioDispatcher) {
            memScoped {
                val buf = allocArray<ByteVar>(512)
                val bytesRead = alloc<DWORDVar>()
                val sb = StringBuilder()
                while (true) {
                    val ok = ReadFile(hStderrRead, buf, 512u, bytesRead.ptr, null)
                    if (ok == 0 || bytesRead.value == 0u) break
                    for (i in 0 until bytesRead.value.toInt()) {
                        sb.append(buf[i].toInt().toChar())
                    }
                }
                sb.toString()
            }
        }

    override suspend fun writeLine(line: String) =
        withContext(ioDispatcher) {
            val handle = hStdinWrite ?: error("Stdin is closed")
            val text = "$line\n"
            val bytes = text.encodeToByteArray()
            memScoped {
                val bytesWritten = alloc<DWORDVar>()
                bytes.usePinned { pinned ->
                    WriteFile(handle, pinned.addressOf(0), bytes.size.toUInt(), bytesWritten.ptr, null)
                }
            }
            Unit
        }

    override suspend fun flushInput() {
        // Direct pipe writes are flushed immediately
    }

    override suspend fun closeInput() {
        val handle = hStdinWrite
        if (handle != null) {
            CloseHandle(handle)
            hStdinWrite = null
        }
    }

    override suspend fun waitFor(): Int =
        withContext(ioDispatcher) {
            if (cachedExitCode != null) return@withContext cachedExitCode!!
            memScoped {
                WaitForSingleObject(hProcess, INFINITE)
                val exitCode = alloc<DWORDVar>()
                GetExitCodeProcess(hProcess, exitCode.ptr)
                val code = exitCode.value.toInt()
                cachedExitCode = code
                code
            }
        }

    override fun destroy() {
        if (isAlive) {
            TerminateProcess(hProcess, 1u)
        }
        closeInputSync()
        CloseHandle(hStdoutRead)
        CloseHandle(hStderrRead)
        CloseHandle(hProcess)
    }

    private fun closeInputSync() {
        val handle = hStdinWrite
        if (handle != null) {
            CloseHandle(handle)
            hStdinWrite = null
        }
    }
}

internal actual fun startNativeProcess(command: List<String>): PlatformProcess =
    memScoped {
        val sa =
            alloc<SECURITY_ATTRIBUTES>().apply {
                nLength = sizeOf<SECURITY_ATTRIBUTES>().toUInt()
                bInheritHandle = TRUE
                lpSecurityDescriptor = null
            }

        val hStdoutRead = alloc<COpaquePointerVar>()
        val hStdoutWrite = alloc<COpaquePointerVar>()
        if (CreatePipe(hStdoutRead.ptr, hStdoutWrite.ptr, sa.ptr, 0u) == 0) {
            error("Failed to create stdout pipe")
        }
        SetHandleInformation(hStdoutRead.value, HANDLE_FLAG_INHERIT.toUInt(), 0u)

        val hStderrRead = alloc<COpaquePointerVar>()
        val hStderrWrite = alloc<COpaquePointerVar>()
        if (CreatePipe(hStderrRead.ptr, hStderrWrite.ptr, sa.ptr, 0u) == 0) {
            error("Failed to create stderr pipe")
        }
        SetHandleInformation(hStderrRead.value, HANDLE_FLAG_INHERIT.toUInt(), 0u)

        val hStdinRead = alloc<COpaquePointerVar>()
        val hStdinWrite = alloc<COpaquePointerVar>()
        if (CreatePipe(hStdinRead.ptr, hStdinWrite.ptr, sa.ptr, 0u) == 0) {
            error("Failed to create stdin pipe")
        }
        SetHandleInformation(hStdinWrite.value, HANDLE_FLAG_INHERIT.toUInt(), 0u)

        val si =
            alloc<STARTUPINFOW>().apply {
                cb = sizeOf<STARTUPINFOW>().toUInt()
                hStdInput = hStdinRead.value
                hStdOutput = hStdoutWrite.value
                hStdError = hStderrWrite.value
                dwFlags = STARTF_USESTDHANDLES.toUInt()
            }
        val pi = alloc<PROCESS_INFORMATION>()

        val cmdLine = command.toWindowsCommandLine()
        val cmdLineWchar = allocArray<UShortVar>(cmdLine.length + 1)
        cmdLine.forEachIndexed { i, c ->
            cmdLineWchar[i] = c.code.toUShort()
        }
        cmdLineWchar[cmdLine.length] = 0u

        val ok =
            CreateProcessW(
                null,
                cmdLineWchar,
                null,
                null,
                TRUE,
                CREATE_NO_WINDOW.toUInt(),
                null,
                null,
                si.ptr,
                pi.ptr,
            )

        CloseHandle(hStdoutWrite.value)
        CloseHandle(hStderrWrite.value)
        CloseHandle(hStdinRead.value)

        if (ok == 0) {
            CloseHandle(hStdoutRead.value)
            CloseHandle(hStderrRead.value)
            CloseHandle(hStdinWrite.value)
            error("Failed to create process for: $cmdLine")
        }

        CloseHandle(pi.hThread)

        MingwPlatformProcess(
            hProcess = pi.hProcess,
            dwProcessId = pi.dwProcessId.toLong(),
            hStdoutRead = hStdoutRead.value,
            hStderrRead = hStderrRead.value,
            hStdinWrite = hStdinWrite.value,
        )
    }

private fun List<String>.toWindowsCommandLine(): String =
    joinToString(" ") { arg ->
        if (arg.contains(' ') || arg.contains('"') || arg.isEmpty()) {
            "\"" + arg.replace("\"", "\\\"") + "\""
        } else {
            arg
        }
    }
