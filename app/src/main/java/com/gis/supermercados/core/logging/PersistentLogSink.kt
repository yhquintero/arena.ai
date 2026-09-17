package com.gis.supermercados.core.logging

import android.util.Log
import com.gis.supermercados.data.local.GisDatabase
import com.gis.supermercados.data.local.entity.AuditLogEntity
import com.gis.supermercados.domain.model.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sumidero que escribe en Logcat y persiste en la base de datos por lotes.
 *
 * Detalles de rendimiento importantes:
 * - Nunca bloquea al llamador: acumula en memoria y vuelca en segundo plano.
 * - Vuelca cada [FLUSH_INTERVAL_MS] o al alcanzar [BUFFER_LIMIT] entradas.
 * - Purga automatica: conserva como maximo [MAX_ENTRIES] filas (la tabla no crece
 *   sin limite, evitando degradar la base de datos con el tiempo).
 */
@Singleton
class PersistentLogSink @Inject constructor(
    private val database: GisDatabase,
    private val scope: CoroutineScope,
) : LogSink {

    private val buffer = ArrayDeque<AuditLogEntity>()
    private var flushJob: Job? = null

    override fun write(
        level: LogLevel,
        tag: String,
        message: String,
        screen: String,
        throwable: Throwable?,
    ) {
        // 1) Logcat inmediato (util durante el desarrollo con Android Studio).
        val logcatTag = "$LOGCAT_PREFIX$tag"
        when (level) {
            LogLevel.DEBUG -> Log.d(logcatTag, message, throwable)
            LogLevel.INFO -> Log.i(logcatTag, message, throwable)
            LogLevel.WARN -> Log.w(logcatTag, message, throwable)
            LogLevel.ERROR -> Log.e(logcatTag, message, throwable)
        }

        // 2) Persistencia diferida por lotes.
        val truncated = if (message.length > MAX_MESSAGE_LENGTH) {
            message.take(MAX_MESSAGE_LENGTH) + "…"
        } else {
            message
        }
        synchronized(buffer) {
            buffer.addLast(
                AuditLogEntity(
                    level = level,
                    tag = tag.take(48),
                    message = truncated,
                    screen = screen.take(48),
                    createdAt = System.currentTimeMillis()
                )
            )
            if (buffer.size >= BUFFER_LIMIT) {
                flushNow()
            } else {
                scheduleFlush()
            }
        }
    }

    /** Vuelca el buffer de inmediato (por ejemplo al pasar la app a segundo plano). */
    fun flushNow() {
        val pending = synchronized(buffer) {
            if (buffer.isEmpty()) return
            val copy = buffer.toList()
            buffer.clear()
            copy
        }
        scope.launch {
            runCatching {
                pending.forEach { database.systemLogDao().insertLog(it) }
                database.systemLogDao().trimLogs(MAX_ENTRIES)
            }.onFailure { Log.w(LOGCAT_PREFIX + "Sink", "No se pudo persistir el log", it) }
        }
    }

    private fun scheduleFlush() {
        if (flushJob?.isActive == true) return
        flushJob = scope.launch {
            delay(FLUSH_INTERVAL_MS)
            if (isActive) flushNow()
        }
    }

    companion object {
        private const val LOGCAT_PREFIX = "GIS/"
        private const val BUFFER_LIMIT = 20
        private const val FLUSH_INTERVAL_MS = 3_000L
        private const val MAX_ENTRIES = 5_000
        private const val MAX_MESSAGE_LENGTH = 1_200
    }
}
