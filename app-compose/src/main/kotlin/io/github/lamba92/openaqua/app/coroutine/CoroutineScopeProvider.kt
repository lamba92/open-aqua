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

interface CoroutineScopeProvider {
    fun getScope(
        name: String,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): CoroutineScope

    fun cancelAll(cause: CancellationException? = null)
}

context(context: T)
fun <T : Any> CoroutineScopeProvider.getScope(dispatcher: CoroutineDispatcher = Dispatchers.Default) =
    getScope(name = context::class.qualifiedName!!, dispatcher = dispatcher)

class DefaultCoroutineScopeProvider(
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val onException: (CoroutineContext, Throwable) -> Unit = { ctx, throwable ->
        System.err.println("[CoroutineScopeProvider] Uncaught exception in $ctx: ${throwable.message}")
        throwable.printStackTrace()
    },
) : CoroutineScopeProvider {
    private val rootSupervisor = SupervisorJob()
    private val scopes = CopyOnWriteArrayList<CoroutineScope>()

    private val exceptionHandler =
        CoroutineExceptionHandler { ctx, throwable ->
            if (throwable !is CancellationException) {
                onException(ctx, throwable)
            }
        }

    override fun getScope(
        name: String,
        dispatcher: CoroutineDispatcher,
    ): CoroutineScope {
        val childJob = SupervisorJob(rootSupervisor)
        val effectiveDispatcher = if (dispatcher == Dispatchers.Default && defaultDispatcher != Dispatchers.Default) defaultDispatcher else dispatcher
        val context = childJob + effectiveDispatcher + exceptionHandler + CoroutineName(name)
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
