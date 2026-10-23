package io.github.lamba92.openaqua.app.coroutine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.CoroutineContext

private val DEFAULT_EXCEPTION_HANDLER_CALLBACK = { ctx: CoroutineContext, throwable: Throwable ->
    System.err.println("[CoroutineScopeProvider] Uncaught exception in $ctx: ${throwable.message}")
    throwable.printStackTrace()
}

interface CoroutineScopeProvider {
    fun getScope(
        name: String,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        onException: (CoroutineContext, Throwable) -> Unit = DEFAULT_EXCEPTION_HANDLER_CALLBACK
    ): CoroutineScope

    fun cancelAll(cause: CancellationException? = null)
}

context(context: T)
fun <T : Any> CoroutineScopeProvider.getScope(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    onException: (CoroutineContext, Throwable) -> Unit = DEFAULT_EXCEPTION_HANDLER_CALLBACK
) =
    getScope(name = context::class.qualifiedName!!, dispatcher = dispatcher, onException = onException)

class DefaultCoroutineScopeProvider(
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : CoroutineScopeProvider {
    private val rootSupervisor = SupervisorJob()
    private val scopes = CopyOnWriteArrayList<CoroutineScope>()

    override fun getScope(
        name: String,
        dispatcher: CoroutineDispatcher,
        onException: (CoroutineContext, Throwable) -> Unit
    ): CoroutineScope {
        val childJob = SupervisorJob(rootSupervisor)
        val effectiveDispatcher = if (dispatcher == Dispatchers.Default && defaultDispatcher != Dispatchers.Default) defaultDispatcher else dispatcher
        val context = childJob + effectiveDispatcher + CoroutineExceptionHandler(onException) + CoroutineName(name)
        val scope = CoroutineScope(context)
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
