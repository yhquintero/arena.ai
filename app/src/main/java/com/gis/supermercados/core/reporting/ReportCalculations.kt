package com.gis.supermercados.core.reporting

import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.reporting.model.DateRangeModel
import com.gis.supermercados.core.reporting.model.SalesTotals
import com.gis.supermercados.domain.model.CashFlowEntry
import com.gis.supermercados.domain.model.DailyTotal
import com.gis.supermercados.domain.model.ProjectedPeriod

/**
 * Matematica de los informes: funciones PURAS (sin Android, sin base de datos).
 *
 * Todo lo sensible para el negocio (margenes, flujo de caja, proyecciones) vive
 * aqui y esta cubierto por pruebas unitarias.
 */
object ReportCalculations {

    /**
     * Completa la serie diaria rellenando con ceros los dias sin movimiento:
     * sin esto las graficas mostrarian saltos y los promedios harian trampa.
     */
    fun buildDailySeries(
        range: DateRangeModel,
        sales: List<DailyTotal>,
        expenses: List<DailyTotal> = emptyList(),
        returns: List<DailyTotal> = emptyList(),
    ): List<DailyTotal> {
        val salesByDay = sales.associateBy { AppDateTime.startOfDay(it.dayMillis) }
        val expensesByDay = expenses.associateBy { AppDateTime.startOfDay(it.dayMillis) }
        val returnsByDay = returns.associateBy { AppDateTime.startOfDay(it.dayMillis) }

        return ReportPeriods.daysInRange(range).map { day ->
            val sale = salesByDay[day]
            val expense = expensesByDay[day]
            val returnsOfDay = returnsByDay[day]
            DailyTotal(
                dayMillis = day,
                salesCents = sale?.salesCents ?: 0L,
                expensesCents = (expense?.expensesCents ?: 0L) + (returnsOfDay?.salesCents ?: 0L),
                tickets = sale?.tickets ?: 0,
                costCents = sale?.costCents ?: 0L
            )
        }
    }

    /** Flujo de caja acumulado dia a dia (entradas, salidas y saldo). */
    fun buildCashFlow(series: List<DailyTotal>): List<CashFlowEntry> {
        var balance = 0L
        return series.map { day ->
            val entry = CashFlowEntry(
                dateMillis = day.dayMillis,
                inflowCents = day.salesCents,
                outflowCents = day.expensesCents + day.costCents,
                openingBalanceCents = balance
            )
            balance = entry.closingBalanceCents
            entry
        }
    }

    /** Utilidad neta del periodo: ventas - costo - gastos - devoluciones. */
    fun netProfitCents(
        sales: SalesTotals,
        expensesCents: Long,
        returnsCents: Long,
    ): Long = sales.grossProfitCents - expensesCents - returnsCents

    /** Variacion porcentual entre dos periodos (manejo correcto del cero). */
    fun variationPercent(previous: Long, current: Long): Double =
        Money.variationPercent(previous, current)

    /**
     * Tendencia del periodo: compara el promedio diario de la segunda mitad
     * contra la primera. Devuelve un porcentaje (positivo = mejora).
     */
    fun trendPercent(series: List<DailyTotal>): Double {
        if (series.size < 4) return 0.0
        val middle = series.size / 2
        val firstHalf = series.take(middle)
        val secondHalf = series.drop(middle)
        val firstAverage = firstHalf.map { it.salesCents }.average()
        val secondAverage = secondHalf.map { it.salesCents }.average()
        if (firstAverage <= 0.0) return if (secondAverage > 0.0) 100.0 else 0.0
        return ((secondAverage - firstAverage) / firstAverage) * 100.0
    }

    fun dailyAverageCents(totalCents: Long, days: Long): Long =
        if (days <= 0) 0L else totalCents / days

    /**
     * Proyeccion simple y transparente: promedio diario del periodo, ajustado por
     * la tendencia observada, multiplicado por los dias de cada horizonte.
     * No es un modelo estadistico complejo: es una estimacion defendible y facil
     * de explicar al usuario.
     */
    fun project(
        baseTotalCents: Long,
        baseDays: Long,
        trendPercent: Double,
        horizonsDays: List<Int>,
    ): List<ProjectedPeriod> {
        val daily = dailyAverageCents(baseTotalCents, baseDays)
        val factor = 1.0 + (trendPercent.coerceIn(-MAX_TREND, MAX_TREND) / 100.0)
        return horizonsDays.map { days ->
            val projected = (daily * days * factor).toLong()
            ProjectedPeriod(label = "", days = days, projectedCents = projected.coerceAtLeast(0L))
        }
    }

    /** Participacion porcentual de cada elemento sobre el total. */
    fun shares(values: List<Long>): List<Double> {
        val total = values.sum()
        if (total == 0L) return values.map { 0.0 }
        return values.map { it.toDouble() * 100.0 / total.toDouble() }
    }

    /** Punto de equilibrio: ventas necesarias para cubrir los gastos fijos. */
    fun breakEvenSalesCents(fixedCostsCents: Long, grossMarginPercent: Double): Long {
        val margin = grossMarginPercent / 100.0
        if (margin <= 0.0) return 0L
        return (fixedCostsCents / margin).toLong()
    }

    /** Ticket promedio ponderado y unidades por ticket. */
    fun averageTicket(totalCents: Long, tickets: Int): Long =
        if (tickets <= 0) 0L else totalCents / tickets

    private const val MAX_TREND = 200.0
}
