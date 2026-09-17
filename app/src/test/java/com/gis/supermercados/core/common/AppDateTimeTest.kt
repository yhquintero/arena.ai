package com.gis.supermercados.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pruebas de los limites de periodo. Los informes agrupan ventas y gastos por dia usando
 * estos limites: si fallan, los totales diarios quedan desplazados y ningun reporte cuadra.
 */
class AppDateTimeTest {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        ZonedDateTime.of(year, month, day, hour, 30, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun startOfDayYEndOfDayEnmarcanElMismoDia() {
        val reference = at(2026, 3, 15, 18)
        val start = AppDateTime.startOfDay(reference)
        val end = AppDateTime.endOfDay(reference)
        assertTrue(start <= reference)
        assertTrue(reference <= end)
        assertEquals(AppDateTime.formatDate(start), AppDateTime.formatDate(end))
    }

    @Test
    fun startOfWeekEmpiezaEnLunesYEndOfWeekTerminaEnDomingo() {
        val wednesday = at(2026, 3, 18)
        val start = AppDateTime.startOfWeek(wednesday)
        val end = AppDateTime.endOfWeek(wednesday)
        assertEquals(7, AppDateTime.daysBetween(start, end))
        assertTrue(start <= wednesday)
        assertTrue(wednesday <= end)
    }

    @Test
    fun startOfMonthYEndOfMonthCubrenTodoElMes() {
        val reference = at(2026, 2, 14)
        assertEquals(
            28,
            AppDateTime.daysBetween(AppDateTime.startOfMonth(reference), AppDateTime.endOfMonth(reference))
        )
        val leap = at(2024, 2, 14)
        assertEquals(
            29,
            AppDateTime.daysBetween(AppDateTime.startOfMonth(leap), AppDateTime.endOfMonth(leap))
        )
    }

    @Test
    fun startOfYearYEndOfYearCubrenElAnoCompleto() {
        val reference = at(2026, 7, 4)
        assertEquals(
            365,
            AppDateTime.daysBetween(AppDateTime.startOfYear(reference), AppDateTime.endOfYear(reference))
        )
    }

    @Test
    fun daysBetweenEsInclusivo() {
        assertEquals(1, AppDateTime.daysBetween(at(2026, 5, 1), at(2026, 5, 1, 23)))
        assertEquals(2, AppDateTime.daysBetween(at(2026, 5, 1), at(2026, 5, 2)))
    }

    @Test
    fun plusDaysYMinusDaysDesplazanLaFecha() {
        val reference = at(2026, 12, 31)
        assertEquals(
            AppDateTime.formatDate(at(2027, 1, 1)),
            AppDateTime.formatDate(AppDateTime.plusDays(reference, 1))
        )
        assertEquals(
            AppDateTime.formatDate(at(2026, 12, 30)),
            AppDateTime.formatDate(AppDateTime.minusDays(reference, 1))
        )
    }

    @Test
    fun formatDateUsaPatronDiaMesAno() {
        assertEquals("15/03/2026", AppDateTime.formatDate(at(2026, 3, 15)))
    }

    @Test
    fun formatForFileNoIncluyeCaracteresInvalidosParaNombresDeArchivo() {
        val stamp = AppDateTime.formatForFile(at(2026, 3, 15, 9))
        assertTrue(stamp.matches(Regex("[0-9_]+")))
        assertEquals(15, stamp.length)
    }

    @Test
    fun isTodayReconoceElDiaEnCurso() {
        assertTrue(AppDateTime.isToday(System.currentTimeMillis()))
        assertTrue(!AppDateTime.isToday(AppDateTime.plusDays(System.currentTimeMillis(), -3)))
    }
}
