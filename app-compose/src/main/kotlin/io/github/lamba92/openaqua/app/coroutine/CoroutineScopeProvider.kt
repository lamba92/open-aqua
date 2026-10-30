package io.github.lamba92.openaqua.app.coroutine

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.CoroutineContext

class CoroutineLogger(val logger: Logger) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<CoroutineLogger>

    override val key: CoroutineContext.Key<*> get() = Key
}

interface LoggingScope : CoroutineScope {
    val logger: Logger
}

private class LoggingScopeImpl(
    override val coroutineContext: CoroutineContext,
    override val logger: Logger,
) : LoggingScope

val CoroutineScope.logger: Logger
    get() = (this as? LoggingScope)?.logger
        ?: coroutineContext[CoroutineLogger]?.logger
        ?: error("No CoroutineLogger found in CoroutineScope")

suspend fun getLogger(): Logger =
    currentCoroutineContext()[CoroutineLogger]?.logger
        ?: error("No CoroutineLogger found in current coroutine context")

interface CoroutineScopeProvider {
    fun getScope(
        name: String,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        onException: ((CoroutineContext, Throwable) -> Unit)? = null,
    ): LoggingScope

    fun cancelAll(cause: CancellationException? = null)
}

context(context: T)
fun <T : Any> CoroutineScopeProvider.getScope(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    onException: ((CoroutineContext, Throwable) -> Unit)? = null,
): LoggingScope =
    getScope(name = context::class.qualifiedName!!, dispatcher = dispatcher, onException = onException)

class DefaultCoroutineScopeProvider(
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val baseLogger: Logger = Logger,
) : CoroutineScopeProvider {
    private val rootSupervisor = SupervisorJob()
    private val scopes = CopyOnWriteArrayList<CoroutineScope>()

    override fun getScope(
        name: String,
        dispatcher: CoroutineDispatcher,
        onException: ((CoroutineContext, Throwable) -> Unit)?,
    ): LoggingScope {
        val tag = name.substringAfterLast('.').ifEmpty { name }
        val logger = baseLogger.withTag(tag)
        val childJob = SupervisorJob(rootSupervisor)
        val effectiveDispatcher = if (dispatcher == Dispatchers.Default && defaultDispatcher != Dispatchers.Default) defaultDispatcher else dispatcher
        val coroutineLogger = CoroutineLogger(logger)
        val exceptionHandler = CoroutineExceptionHandler { ctx, throwable ->
            logger.e(throwable) { "Uncaught exception in $ctx: ${throwable.message}" }
            onException?.invoke(ctx, throwable)
        }
        val context = childJob + effectiveDispatcher + coroutineLogger + exceptionHandler + CoroutineName(name)
        val scope = LoggingScopeImpl(context, logger)
        scopes.add(scope)
        return scope
    }

    override fun cancelAll(cause: CancellationException?) {
        for (scope in scopes) {
            scope.coroutineContext[Job]?.cancel(cause)
        }
        scopes.clear()
        rootSupervisor.cancel(cause)
    }
}
