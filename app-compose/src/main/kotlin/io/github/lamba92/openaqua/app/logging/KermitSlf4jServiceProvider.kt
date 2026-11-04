package io.github.lamba92.openaqua.app.logging

import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.helpers.MessageFormatter
import org.slf4j.helpers.NOPMDCAdapter
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider
import java.util.concurrent.ConcurrentHashMap
import co.touchlab.kermit.Logger as KermitLogger

class KermitSlf4jServiceProvider : SLF4JServiceProvider {
    private val loggerFactory = KermitLoggerFactory()
    private val markerFactory = BasicMarkerFactory()
    private val mdcAdapter = NOPMDCAdapter()

    override fun getLoggerFactory(): ILoggerFactory = loggerFactory

    override fun getMarkerFactory(): IMarkerFactory = markerFactory

    override fun getMDCAdapter(): MDCAdapter = mdcAdapter

    override fun getRequestedApiVersion(): String = "2.0.99"

    override fun initialize() {
        // Nothing to initialize
    }
}

class KermitLoggerFactory : ILoggerFactory {
    private val loggers = ConcurrentHashMap<String, Logger>()

    override fun getLogger(name: String): Logger = loggers.computeIfAbsent(name) { KermitSlf4jLogger(it) }
}

class KermitSlf4jLogger(
    name: String,
) : org.slf4j.helpers.AbstractLogger() {
    init {
        this.name = name
    }

    private val kermit = KermitLogger.withTag(name.substringAfterLast('.'))

    override fun isTraceEnabled(): Boolean = false

    override fun isTraceEnabled(marker: Marker?): Boolean = false

    override fun isDebugEnabled(): Boolean = true

    override fun isDebugEnabled(marker: Marker?): Boolean = true

    override fun isInfoEnabled(): Boolean = true

    override fun isInfoEnabled(marker: Marker?): Boolean = true

    override fun isWarnEnabled(): Boolean = true

    override fun isWarnEnabled(marker: Marker?): Boolean = true

    override fun isErrorEnabled(): Boolean = true

    override fun isErrorEnabled(marker: Marker?): Boolean = true

    override fun getFullyQualifiedCallerName(): String? = null

    override fun handleNormalizedLoggingCall(
        level: Level,
        marker: Marker?,
        msgPattern: String?,
        arguments: Array<out Any>?,
        throwable: Throwable?,
    ) {
        val formattedMessage =
            if (arguments != null && msgPattern != null) {
                MessageFormatter.arrayFormat(msgPattern, arguments).message
            } else {
                msgPattern ?: ""
            }

        when (level) {
            Level.TRACE -> kermit.v(throwable) { formattedMessage }
            Level.DEBUG -> kermit.d(throwable) { formattedMessage }
            Level.INFO -> kermit.i(throwable) { formattedMessage }
            Level.WARN -> kermit.w(throwable) { formattedMessage }
            Level.ERROR -> kermit.e(throwable) { formattedMessage }
        }
    }
}
