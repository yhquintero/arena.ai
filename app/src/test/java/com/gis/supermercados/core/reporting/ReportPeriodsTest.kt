package com.gis.supermercados.core.reporting

import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.core.reporting.model.PeriodType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pruebas de la resolucion de periodos de los informes (diario, semanal, mensual, trimestral,
 * semestral, anual y personalizado) y del periodo anterior equivalente usado en las comparativas.
 */
class ReportPeriodsTest {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        ZonedDateTime.of(year, month, day, hour, 30, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun periodoDiarioCubreUnSoloDia() {
        val reference = at(2026, 3, 15, 18)
        val range = ReportPeriods.rangeFor(PeriodType.DIARIO, reference)

        assertEquals(1L, range.days)
        assertEquals(AppDateTime.startOfDay(reference), range.startMillis)
        assertEquals(AppDateTime.endOfDay(reference), range.endMillis)
    }

    @Test
    fun periodoSemanalVaDeLunesADomingo() {
        val wednesday = at(2026, 3, 18)
        val range = ReportPeriods.rangeFor(PeriodType.SEMANAL, wednesday)

        assertEquals(7L, range.days)
        assertEquals("16/03/2026", AppDateTime.formatDate(range.startMillis))
        assertEquals("22/03/2026", AppDateTime.formatDate(range.endMillis))
    }

    @Test
    fun periodoMensualCubreTodoElMes() {
        val range = ReportPeriods.rangeFor(PeriodType.MENSUAL, at(2026, 3, 15))

        assertEquals(31L, range.days)
        assertEquals("01/03/2026", AppDateTime.formatDate(range.startMillis))
        assertEquals("31/03/2026", AppDateTime.formatDate(range.endMillis))
    }

    @Test
    fun periodoTrimestralUsaElTrimestreNatural() {
        val range = ReportPeriods.rangeFor(PeriodType.TRIMESTRAL, at(2026, 5, 10))

        assertEquals("01/04/2026", AppDateTime.formatDate(range.startMillis))
        assertEquals("30/06/2026", AppDateTime.formatDate(range.endMillis))
        assertEquals(91L, range.days)
    }

    @Test
    fun periodoSemestralUsaElSemestreNatural() {
        val range = ReportPeriods.rangeFor(PeriodType.SEMESTRAL, at(2026, 8, 10))

        assertEquals("01/07/2026", AppDateTime.formatDate(range.startMillis))
        assertEquals("31/12/2026", AppDateTime.formatDate(range.endMillis))
        assertEquals(184L, range.days)
    }

    @Test
    fun periodoAnualCubreElAnoCompleto() {
        val range = ReportPeriods.rangeFor(PeriodType.ANUAL, at(2026, 6, 1))

        assertEquals("01/01/2026", AppDateTime.formatDate(range.startMillis))
        assertEquals("31/12/2026", AppDateTime.formatDate(range.endMillis))
        assertEquals(365L, range.days)
    }

    @Test
    fun periodoPersonalizadoRespetaLasFechasElegidas() {
        val start = at(2026, 1, 5)
        val end = at(2026, 1, 20)
        val range = ReportPeriods.rangeFor(PeriodType.PERSONALIZADO, at(2026, 3, 1), start, end)

        assertEquals(start, range.startMillis)
        assertEquals(AppDateTime.endOfDay(end), range.endMillis)
        assertEquals(16L, range.days)
    }

    @Test
    fun periodoPersonalizadoSinFechasUsaElDiaEnCurso() {
        val range = ReportPeriods.rangeFor(PeriodType.PERSONALIZADO, at(2026, 3, 15))

        assertEquals(1L, range.days)
    }

    @Test
    fun periodoAnteriorTieneLaMismaLongitudYNoSeSolapa() {
        val current = ReportPeriods.rangeFor(PeriodType.MENSUAL, at(2026, 3, 15))
        val previous = ReportPeriods.previousOf(current)

        assertEquals(current.days, previous.days)
        assertTrue(previous.endMillis < current.startMillis)
        // El periodo anterior se calcula por longitud (no por calendario): mismo numero de dias.
        assertEquals(
            AppDateTime.daysBetween(previous.startMillis, previous.endMillis),
            AppDateTime.daysBetween(current.startMillis, current.endMillis)
        )
    }

    @Test
    fun daysInRangeRellenaTodosLosDiasDelPeriodo() {
        val range = ReportPeriods.rangeFor(PeriodType.SEMANAL, at(2026, 3, 18))
        val days = ReportPeriods.daysInRange(range)

        assertEquals(7, days.size)
        assertEquals(range.startMillis, days.first())
        assertTrue(days == days.sorted())
    }
}
