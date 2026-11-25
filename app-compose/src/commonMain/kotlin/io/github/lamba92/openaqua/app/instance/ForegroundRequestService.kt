package io.github.lamba92.openaqua.app.instance

import co.touchlab.kermit.Logger
import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.coroutine.logger
import io.github.lamba92.openaqua.app.model.AppPortFile
import io.github.lamba92.openaqua.app.model.BaseAppDataDir
import io.github.lamba92.openaqua.app.model.MemoryDumpsDirectory
import io.github.lamba92.openaqua.app.util.deleteIfExists
import io.github.lamba92.openaqua.app.util.exists
import io.github.lamba92.openaqua.app.util.readText
import io.github.lamba92.openaqua.app.util.resolve
import io.github.lamba92.openaqua.app.util.writeText
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import platform.posix.O_CREAT
import platform.posix.O_TRUNC
import platform.posix.O_WRONLY
import platform.posix.S_IRUSR
import platform.posix.S_IWUSR
import platform.posix.close
import platform.posix.open
import kotlin.jvm.JvmInline
import kotlin.native.runtime.Debugging
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi
import kotlin.system.exitProcess
import kotlin.time.Clock
import io.ktor.server.cio.CIO as ServerCIO

sealed interface AcquisitionResult {
    data object Success : AcquisitionResult

    @JvmInline
    value class AppAlreadyOpen(
        val port: Int,
    ) : AcquisitionResult

    data object Failure : AcquisitionResult
}

private var appInstanceLock: AppInstanceLock? = null
private val startTimeAttributeKey = AttributeKey<Long>("CallStartTime")

fun acquireFileState(
    lockFilePath: Path,
    portFilePath: Path,
): AcquisitionResult {
    val lock = AppInstanceLock.tryAcquire(lockFilePath)
    if (lock != null) {
        appInstanceLock = lock
        return AcquisitionResult.Success
    }

    repeat(20) {
        if (portFilePath.exists()) {
            val port = runCatching { portFilePath.readText().trim().toIntOrNull() }.getOrNull()
            if (port != null) {
                return AcquisitionResult.AppAlreadyOpen(port)
            }
        }
        platformSleep(100)
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
        runCatching {
            HttpClient().use { client ->
                client.post("http://127.0.0.1:$port/focus")
            }
        }
    }
    exitProcess(0)
}

class ForegroundRequestService(
    private val portFile: AppPortFile,
    private val memoryDumpsDirectory: MemoryDumpsDirectory,
    scopeProvider: CoroutineScopeProvider,
) : AutoCloseable {
    private val scope = scopeProvider.getScope()

    val focusRequests: SharedFlow<Unit>
        field = MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        startServer()
        scope.coroutineContext.job.invokeOnCompletion { close() }
    }

    private fun startServer() {
        val embedded =
            scope.embeddedServer(
                factory = ServerCIO,
                port = 0,
                host = "127.0.0.1",
            ) {
                install(createCallLoggingPlugin(scope.logger))
                install(ContentNegotiation) {
                    json()
                }
                routing {
                    post {
                        focusRequests.tryEmit(Unit)
                        call.respond(ForegroundRequestResponse(status = "ok"))
                    }
                    post("/focus") {
                        focusRequests.tryEmit(Unit)
                        call.respond(ForegroundRequestResponse(status = "ok"))
                    }
                    post("/dump-memory") {
                        val timestamp = Clock.System.now()
                        val fileName = "$timestamp.raw"
                        val filePath = memoryDumpsDirectory.value.resolve(fileName)
                        val success = dumpMemorySnapshot(filePath.toString())
                        if (success) {
                            call.respond(
                                HttpStatusCode.OK,
                                MemoryDumpResponse.Success(
                                    file = filePath.toString(),
                                    hint = "Run 'kdumputil $filePath dump.hprof' to inspect in IntelliJ",
                                ),
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                MemoryDumpResponse.Error(
                                    message = "Failed to create memory dump",
                                ),
                            )
                        }
                    }
                }
            }

        embedded.start(wait = false)

        scope.launch {
            val port =
                embedded.engine
                    .resolvedConnectors()
                    .first()
                    .port
            scope.logger.i { "Foreground request server listening on port $port" }
            portFile.path.writeText("$port\n")
        }
    }

    override fun close() {
        runCatching { portFile.path.deleteIfExists() }
        appInstanceLock?.release()
        appInstanceLock = null
    }
}

private fun createCallLoggingPlugin(logger: Logger) =
    createApplicationPlugin("KermitCallLogging") {
        onCall { call ->
            call.attributes.put(startTimeAttributeKey, Clock.System.now().toEpochMilliseconds())
        }

        onCallRespond { call ->
            val startTime = call.attributes.getOrNull(startTimeAttributeKey) ?: Clock.System.now().toEpochMilliseconds()
            val duration = Clock.System.now().toEpochMilliseconds() - startTime
            val method = call.request.httpMethod.value
            val uri = call.request.uri
            val status = call.response.status()?.value ?: 200
            logger.i { "$method $uri -> $status in ${duration}ms" }
        }
    }

@OptIn(NativeRuntimeApi::class)
private fun dumpMemorySnapshot(path: String): Boolean {
    val fd = open(path, O_CREAT or O_WRONLY or O_TRUNC, S_IRUSR or S_IWUSR)
    if (fd < 0) return false
    return try {
        GC.collect()
        Debugging.dumpMemory(fd.toLong())
    } finally {
        close(fd)
    }
}
