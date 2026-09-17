package com.gis.supermercados.core.reporting

import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.core.reporting.model.DateRangeModel
import com.gis.supermercados.core.reporting.model.PeriodType
import java.time.DayOfWeek
import java.time.YearMonth

/**
 * Resolucion de periodos de reporte.
 *
 * Es logica PURA (sin Context ni Android) para poder probarla con tests unitarios:
 * cada periodicidad devuelve el rango exacto de epoch-millis que se usara en las
 * consultas SQL.
 */
object ReportPeriods {

    private const val MILLIS_PER_DAY = 86_400_000L

    /** Devuelve el rango [start, end] inclusivo para el periodo indicado. */
    fun rangeFor(
        type: PeriodType,
        referenceMillis: Long = AppDateTime.now(),
        customStartMillis: Long? = null,
        customEndMillis: Long? = null,
    ): DateRangeModel {
        val date = AppDateTime.toLocalDate(referenceMillis)
        val (start, end) = when (type) {
            PeriodType.DIARIO -> AppDateTime.startOfDay(referenceMillis) to AppDateTime.endOfDay(referenceMillis)

            PeriodType.SEMANAL -> {
                val monday = date.with(DayOfWeek.MONDAY)
                val sunday = date.with(DayOfWeek.SUNDAY)
                AppDateTime.toMillis(monday) to AppDateTime.endOfDay(AppDateTime.toMillis(sunday))
            }

            PeriodType.MENSUAL -> {
                val month = YearMonth.from(date)
                AppDateTime.toMillis(month.atDay(1)) to AppDateTime.endOfDay(AppDateTime.toMillis(month.atEndOfMonth()))
            }

            PeriodType.TRIMESTRAL -> {
                val quarterIndex = (date.monthValue - 1) / 3          // 0..3
                val firstMonth = quarterIndex * 3 + 1
                val startMonth = YearMonth.of(date.year, firstMonth)
                val endMonth = YearMonth.of(date.year, firstMonth + 2)
                AppDateTime.toMillis(startMonth.atDay(1)) to AppDateTime.endOfDay(AppDateTime.toMillis(endMonth.atEndOfMonth()))
            }

            PeriodType.SEMESTRAL -> {
                val firstMonth = if (date.monthValue <= 6) 1 else 7
                val startMonth = YearMonth.of(date.year, firstMonth)
                val endMonth = YearMonth.of(date.year, firstMonth + 5)
                AppDateTime.toMillis(startMonth.atDay(1)) to AppDateTime.endOfDay(AppDateTime.toMillis(endMonth.atEndOfMonth()))
            }

            PeriodType.ANUAL -> {
                val startMonth = YearMonth.of(date.year, 1)
                val endMonth = YearMonth.of(date.year, 12)
                AppDateTime.toMillis(startMonth.atDay(1)) to AppDateTime.endOfDay(AppDateTime.toMillis(endMonth.atEndOfMonth()))
            }

            PeriodType.PERSONALIZADO -> {
                val start = customStartMillis ?: AppDateTime.startOfDay(referenceMillis)
                val rawEnd = customEndMillis ?: AppDateTime.endOfDay(referenceMillis)
                start to AppDateTime.endOfDay(rawEnd)
            }
        }
        return DateRangeModel(startMillis = start, endMillis = end, label = "")
    }

    /** Rango anterior de igual longitud (informes comparativos). */
    fun previousOf(range: DateRangeModel): DateRangeModel = range.previous()

    /**
     * Serie de dias "naturales" del rango (rellena huecos sin ventas con ceros).
     * Imprescindible para que las graficas no muestren saltos.
     */
    fun daysInRange(range: DateRangeModel): List<Long> {
        val days = mutableListOf<Long>()
        var cursor = AppDateTime.startOfDay(range.startMillis)
        val last = AppDateTime.startOfDay(range.endMillis)
        var guard = 0
        while (cursor <= last && guard < MAX_DAYS) {
            days.add(cursor)
            cursor += MILLIS_PER_DAY
            guard++
        }
        return days
    }

    /** Divide el rango en bloques mensuales (para proyecciones y tendencias). */
    fun monthsInRange(range: DateRangeModel): List<Pair<Long, Long>> {
        val result = mutableListOf<Pair<Long, Long>>()
        var month = YearMonth.from(AppDateTime.toLocalDate(range.startMillis))
        val lastMonth = YearMonth.from(AppDateTime.toLocalDate(range.endMillis))
        var guard = 0
        while (!month.isAfter(lastMonth) && guard < MAX_MONTHS) {
            val start = AppDateTime.toMillis(month.atDay(1))
                .coerceAtLeast(range.startMillis)
            val end = AppDateTime.endOfDay(AppDateTime.toMillis(month.atEndOfMonth()))
                .coerceAtMost(range.endMillis)
            result.add(start to end)
            month = month.plusMonths(1)
            guard++
        }
        return result
    }

    private const val MAX_DAYS = 3_700
    private const val MAX_MONTHS = 120
}
