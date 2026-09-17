package com.gis.supermercados.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Utilidades de fecha/hora. Se almacena SIEMPRE epoch-millis (UTC) y se
 * presenta en la zona horaria del dispositivo. Requiere minSdk 26 (java.time).
 */
object AppDateTime {

    val zone: ZoneId get() = ZoneId.systemDefault()

    private val dateFormatter: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale())
    private val dateTimeFormatter: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", locale())
    private val timeFormatter: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern("HH:mm", locale())
    private val shortDateFormatter: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern("dd MMM", locale())
    private val monthFormatter: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern("MMMM yyyy", locale())
    private val isoFormatter: DateTimeFormatter
        get() = DateTimeFormatter.ISO_LOCAL_DATE
    private val fileStampFormatter: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.ROOT)

    private fun locale(): Locale = Locale.getDefault()

    fun now(): Long = System.currentTimeMillis()

    fun toLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun toLocalDateTime(millis: Long): LocalDateTime =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()

    fun toMillis(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun toMillis(dateTime: LocalDateTime): Long = dateTime.atZone(zone).toInstant().toEpochMilli()

    fun startOfDay(millis: Long): Long = toMillis(toLocalDate(millis))

    fun endOfDay(millis: Long): Long =
        toLocalDate(millis).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L

    fun startOfWeek(millis: Long): Long =
        toMillis(toLocalDate(millis).with(java.time.DayOfWeek.MONDAY))

    fun endOfWeek(millis: Long): Long =
        endOfDay(toMillis(toLocalDate(millis).with(java.time.DayOfWeek.SUNDAY)))

    fun startOfMonth(millis: Long): Long =
        toMillis(toLocalDate(millis).withDayOfMonth(1))

    fun endOfMonth(millis: Long): Long =
        endOfDay(toMillis(YearMonth.from(toLocalDate(millis)).atEndOfMonth()))

    fun startOfYear(millis: Long): Long = toMillis(toLocalDate(millis).withDayOfYear(1))

    fun endOfYear(millis: Long): Long {
        val lastDay = toLocalDate(millis).withDayOfYear(1).plusYears(1).minusDays(1)
        return endOfDay(toMillis(lastDay))
    }

    fun plusDays(millis: Long, days: Long): Long =
        toMillis(toLocalDate(millis).plusDays(days))

    fun minusDays(millis: Long, days: Long): Long = plusDays(millis, -days)

    fun daysBetween(startMillis: Long, endMillis: Long): Long =
        ChronoUnit.DAYS.between(toLocalDate(startMillis), toLocalDate(endMillis)) + 1

    // ---- Formateo para UI y reportes ----

    fun formatDate(millis: Long): String = dateFormatter.format(toLocalDateTime(millis))
    fun formatDateTime(millis: Long): String = dateTimeFormatter.format(toLocalDateTime(millis))
    fun formatTime(millis: Long): String = timeFormatter.format(toLocalDateTime(millis))
    fun formatShortDate(millis: Long): String = shortDateFormatter.format(toLocalDateTime(millis))
    fun formatMonthYear(millis: Long): String = monthFormatter.format(toLocalDateTime(millis))
    fun formatIso(millis: Long): String = isoFormatter.format(toLocalDateTime(millis))
    fun formatForFile(millis: Long): String = fileStampFormatter.format(toLocalDateTime(millis))

    fun formatWithPattern(millis: Long, pattern: String): String =
        DateTimeFormatter.ofPattern(pattern, locale()).format(toLocalDateTime(millis))

    fun dayOfWeekName(millis: Long): String =
        toLocalDate(millis).dayOfWeek.getDisplayName(TextStyle.FULL, locale())
            .replaceFirstChar { it.uppercase(locale()) }

    fun monthName(month: Int): String {
        val safe = month.coerceIn(1, 12)
        return java.time.Month.of(safe).getDisplayName(TextStyle.FULL, locale())
            .replaceFirstChar { it.uppercase(locale()) }
    }

    fun monthNameShort(month: Int): String {
        val safe = month.coerceIn(1, 12)
        return java.time.Month.of(safe).getDisplayName(TextStyle.SHORT, locale())
            .replaceFirstChar { it.uppercase(locale()) }
    }

    /** "Del 01/03/2026 al 31/03/2026" - etiqueta legible para reportes. */
    fun formatRange(startMillis: Long, endMillis: Long): String =
        "${formatDate(startMillis)} - ${formatDate(endMillis)}"

    fun isSameDay(a: Long, b: Long): Boolean = toLocalDate(a) == toLocalDate(b)

    fun isToday(millis: Long): Boolean = isSameDay(millis, now())
}
