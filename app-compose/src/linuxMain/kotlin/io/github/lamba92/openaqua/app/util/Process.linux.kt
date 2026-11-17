@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.lamba92.openaqua.app.util

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import platform.posix.SIGKILL
import platform.posix.STDERR_FILENO
import platform.posix.STDIN_FILENO
import platform.posix.STDOUT_FILENO
import platform.posix.WNOHANG
import platform.posix.close
import platform.posix.dup2
import platform.posix.execvp
import platform.posix.exit
import platform.posix.fork
import platform.posix.kill
import platform.posix.pipe
import platform.posix.read
import platform.posix.readlink
import platform.posix.waitpid
import platform.posix.write

internal actual fun getExecutableDirectory(): Path? =
    memScoped {
        val buffer = allocArray<ByteVar>(1024)
        val len = readlink("/proc/self/exe", buffer, 1024u)
        if (len > 0) {
            Path(buffer.toKString()).parent
        } else {
            null
        }
    }

private class LinuxPlatformProcess(
    private val childPid: Int,
    private val stdoutFd: Int,
    private val stderrFd: Int,
    private val stdinFd: Int,
) : PlatformProcess {
    private val stdoutBuffer = StringBuilder()
    private var stdinClosed = false
    private var cachedExitCode: Int? = null

    override val pid: Long get() = childPid.toLong()

    override val isAlive: Boolean
        get() {
            if (cachedExitCode != null) return false
            return memScoped {
                val status = alloc<IntVar>()
                val res = waitpid(childPid, status.ptr, WNOHANG)
                if (res == childPid) {
                    val raw = status.value
                    cachedExitCode = (raw shr 8) and 0xFF
                    false
                } else {
                    res == 0
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
                while (true) {
                    val newlineIdx = stdoutBuffer.indexOf('\n')
                    if (newlineIdx >= 0) {
                        val line = stdoutBuffer.substring(0, newlineIdx).trimEnd('\r')
                        stdoutBuffer.deleteRange(0, newlineIdx + 1)
                        return@withContext line
                    }
                    val bytesRead = read(stdoutFd, buf, 512u)
                    if (bytesRead <= 0) {
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
                            for (i in 0 until bytesRead.toInt()) {
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
                val sb = StringBuilder()
                while (true) {
                    val bytesRead = read(stderrFd, buf, 512u)
                    if (bytesRead <= 0) break
                    for (i in 0 until bytesRead.toInt()) {
                        sb.append(buf[i].toInt().toChar())
                    }
                }
                sb.toString()
            }
        }

    override suspend fun writeLine(line: String) =
        withContext(ioDispatcher) {
            val text = "$line\n"
            val bytes = text.encodeToByteArray()
            bytes.usePinned { pinned ->
                write(stdinFd, pinned.addressOf(0), bytes.size.toULong())
            }
            Unit
        }

    override suspend fun flushInput() {
        // Direct file descriptor writes are unbuffered
    }

    override suspend fun closeInput() {
        if (!stdinClosed) {
            close(stdinFd)
            stdinClosed = true
        }
    }

    override suspend fun waitFor(): Int =
        withContext(ioDispatcher) {
            if (cachedExitCode != null) return@withContext cachedExitCode!!
            memScoped {
                val status = alloc<IntVar>()
                waitpid(childPid, status.ptr, 0)
                val code = (status.value shr 8) and 0xFF
                cachedExitCode = code
                code
            }
        }

    override fun destroy() {
        if (isAlive) {
            kill(childPid, SIGKILL)
        }
        close(stdoutFd)
        close(stderrFd)
        closeInputSync()
    }

    private fun closeInputSync() {
        if (!stdinClosed) {
            close(stdinFd)
            stdinClosed = true
        }
    }
}

internal actual fun startNativeProcess(command: List<String>): PlatformProcess =
    memScoped {
        val stdoutPipe = allocArray<IntVar>(2)
        val stderrPipe = allocArray<IntVar>(2)
        val stdinPipe = allocArray<IntVar>(2)

        if (pipe(stdoutPipe) != 0 || pipe(stderrPipe) != 0 || pipe(stdinPipe) != 0) {
            error("Failed to create pipes for process execution")
        }

        val pid = fork()
        if (pid < 0) {
            close(stdoutPipe[0])
            close(stdoutPipe[1])
            close(stderrPipe[0])
            close(stderrPipe[1])
            close(stdinPipe[0])
            close(stdinPipe[1])
            error("Failed to fork process")
        }

        if (pid == 0) {
            dup2(stdoutPipe[1], STDOUT_FILENO)
            dup2(stderrPipe[1], STDERR_FILENO)
            dup2(stdinPipe[0], STDIN_FILENO)

            close(stdoutPipe[0])
            close(stdoutPipe[1])
            close(stderrPipe[0])
            close(stderrPipe[1])
            close(stdinPipe[0])
            close(stdinPipe[1])

            val argv = allocArray<CPointerVar<ByteVar>>(command.size + 1)
            command.forEachIndexed { index, arg ->
                argv[index] = arg.cstr.ptr
            }
            argv[command.size] = null

            execvp(command.first(), argv)
            exit(127)
        }

        close(stdoutPipe[1])
        close(stderrPipe[1])
        close(stdinPipe[0])

        LinuxPlatformProcess(
            childPid = pid,
            stdoutFd = stdoutPipe[0],
            stderrFd = stderrPipe[0],
            stdinFd = stdinPipe[1],
        )
    }
