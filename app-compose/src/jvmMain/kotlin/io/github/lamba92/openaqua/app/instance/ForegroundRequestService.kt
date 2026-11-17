package io.github.lamba92.openaqua.app.instance

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.coroutine.logger
import io.github.lamba92.openaqua.app.model.AppPortFile
import io.github.lamba92.openaqua.app.model.BaseAppDataDir
import io.github.lamba92.openaqua.app.util.deleteIfExists
import io.github.lamba92.openaqua.app.util.exists
import io.github.lamba92.openaqua.app.util.readText
import io.github.lamba92.openaqua.app.util.toJavaPath
import io.github.lamba92.openaqua.app.util.writeText
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption
import kotlin.system.exitProcess
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.server.cio.CIO as ServerCIO

sealed interface AcquisitionResult {
    data object Success : AcquisitionResult

    @JvmInline
    value class AppAlreadyOpen(
        val port: Int,
    ) : AcquisitionResult

    data object Failure : AcquisitionResult
}

private var appInstanceChannel: FileChannel? = null
private var appInstanceLock: FileLock? = null

fun acquireFileState(
    lockFilePath: Path,
    portFilePath: Path,
): AcquisitionResult {
    val channel =
        try {
            FileChannel.open(
                lockFilePath.toJavaPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
            )
        } catch (_: IOException) {
            return AcquisitionResult.Failure
        }

    val lock =
        try {
            channel.tryLock()
        } catch (_: IOException) {
            null
        }

    if (lock != null) {
        appInstanceChannel = channel
        appInstanceLock = lock
        Runtime.getRuntime().addShutdownHook(
            Thread {
                runCatching { lock.release() }
                runCatching { channel.close() }
            },
        )
        return AcquisitionResult.Success
    }

    runCatching { channel.close() }

    // Another instance holds the lock. Read port from port file.
    repeat(20) {
        if (portFilePath.exists()) {
            val port = runCatching { portFilePath.readText().trim().toIntOrNull() }.getOrNull()
            if (port != null) {
                return AcquisitionResult.AppAlreadyOpen(port)
            }
        }
        Thread.sleep(100)
    }

    return AcquisitionResult.Failure
}

fun acquireOrNotify() {
    val defaultAppDir = BaseAppDataDir.default()
    when (val result = acquireFileState(defaultAppDir.lockFile.path, defaultAppDir.portFile.path)) {
        is AcquisitionResult.AppAlreadyOpen -> notifyExistingInstance(result.port)
        is AcquisitionResult.Failure -> exitProcess(1)
        AcquisitionResult.Success -> return
    }
}

fun notifyExistingInstance(port: Int) {
    println("Notified existing instance. Exiting...")
    runBlocking {
        HttpClient(ClientCIO).use { client ->
            client.post("http://127.0.0.1:$port")
        }
    }
    exitProcess(0)
}

class ForegroundRequestService(
    private val portFile: AppPortFile,
    scopeProvider: CoroutineScopeProvider,
) : AutoCloseable {
    private val scope = scopeProvider.getScope()

    val focusRequests: SharedFlow<Unit>
        field =
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    init {
        startServer()
    }

    private fun startServer() {
        val embedded =
            scope.embeddedServer(
                factory = ServerCIO,
                port = 0,
                host = "127.0.0.1",
            ) {
                routing {
                    post {
                        logger.i { "Received foreground request from secondary instance" }
                        focusRequests.tryEmit(Unit)
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }

        embedded.start(wait = false)

        val port =
            runBlocking {
                embedded.engine
                    .resolvedConnectors()
                    .first()
                    .port
            }

        scope.logger.i { "Foreground request server listening on port $port" }
        portFile.path.writeText("$port\n")

        Runtime.getRuntime().addShutdownHook(Thread { close() })
    }

    override fun close() {
        runCatching { portFile.path.deleteIfExists() }
    }
}
