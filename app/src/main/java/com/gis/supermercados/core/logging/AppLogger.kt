package com.gis.supermercados.core.logging

import com.gis.supermercados.domain.model.LogLevel

/**
 * Logger interno de la aplicacion.
 *
 * Como la app funciona sin servidor, el diagnostico tiene que poder consultarse
 * en el propio dispositivo: cada mensaje va a Logcat Y a un sumidero persistido
 * (base de datos), visible en Ajustes > Registro interno y exportable a archivo.
 *
 * Se usa como `object` para poder registrar desde cualquier capa sin inyectar
 * dependencias; el sumidero se instala al arrancar la aplicacion.
 */
object AppLogger {

    /** Pantalla/origen actual, para filtrar logs por contexto. */
    var currentScreen: String = ""

    private var sink: LogSink? = null

    /** Instala el sumidero persistente (se llama desde GisApplication). */
    fun install(logSink: LogSink) {
        sink = logSink
    }

    fun d(tag: String, message: String) = write(LogLevel.DEBUG, tag, message, null)
    fun i(tag: String, message: String) = write(LogLevel.INFO, tag, message, null)
    fun w(tag: String, message: String, throwable: Throwable? = null) =
        write(LogLevel.WARN, tag, message, throwable)

    fun e(tag: String, message: String, throwable: Throwable? = null) =
        write(LogLevel.ERROR, tag, message, throwable)

    private fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val text = if (throwable == null) {
            message
        } else {
            "$message | ${throwable.javaClass.simpleName}: ${throwable.message}"
        }
        runCatching { sink?.write(level, tag, text, currentScreen, throwable) }
    }
}

/** Destino de los mensajes de log. */
interface LogSink {
    fun write(level: LogLevel, tag: String, message: String, screen: String, throwable: Throwable?)
}
