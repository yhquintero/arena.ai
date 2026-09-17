package com.gis.supermercados.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del nucleo monetario: TODO el dinero se maneja en centavos (Long) y se formatea
 * con separadores en espanol ($1.234,56). Un error aqui se propaga a ventas, inventario,
 * gastos e informes, por eso es la logica con mayor cobertura.
 */
class MoneyTest {

    @Test
    fun parseAceptaFormatoEuropeoConPuntoDeMiles() {
        assertEquals(123_456L, Money.parse("1.234,56"))
    }

    @Test
    fun parseAceptaFormatoConPuntoDecimal() {
        assertEquals(123_456L, Money.parse("1234.56"))
        assertEquals(123_456L, Money.parse("1,234.56"))
    }

    @Test
    fun parseIgnoraSimboloDeMonedaYEspacios() {
        assertEquals(123_456L, Money.parse("  $ 1.234,56 "))
    }

    @Test
    fun parseInterpretaEnterosComoUnidadesMonetarias() {
        assertEquals(1_200L, Money.parse("12"))
    }

    @Test
    fun parseAdmiteNegativosContablesEntreParentesis() {
        assertEquals(-5_000L, Money.parse("(50,00)"))
        assertEquals(-5_000L, Money.parse("-50"))
    }

    @Test
    fun parseDevuelveNullConEntradasInvalidas() {
        assertNull(Money.parse(null))
        assertNull(Money.parse(""))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("$"))
    }

    @Test
    fun formatUsaSeparadoresEnEspanolYSimboloPorDefecto() {
        assertEquals("$1.234,56", Money.format(123_456L))
        assertEquals("$0,00", Money.format(0L))
        assertEquals("-$1.234,56", Money.format(-123_456L))
    }

    @Test
    fun formatNumberOmiteElSimbolo() {
        assertEquals("1.234,56", Money.formatNumber(123_456L))
    }

    @Test
    fun formatCompactAbreviaMilesYMillones() {
        assertTrue(Money.formatCompact(1_500_000L).endsWith("K"))
        assertTrue(Money.formatCompact(250_000_000L).endsWith("M"))
        assertEquals("$999,99", Money.formatCompact(99_999L))
    }

    @Test
    fun taxRedondeaAMedioCentavoHaciaArriba() {
        assertEquals(1_600L, Money.tax(10_000L, 0.16))
        assertEquals(0L, Money.tax(10_000L, 0.0))
        assertEquals(1L, Money.tax(10L, 0.05))
    }

    @Test
    fun withTaxSumaElImpuestoSobreLaBase() {
        assertEquals(11_600L, Money.withTax(10_000L, 0.16))
    }

    @Test
    fun percentOfYAfterDiscountAplicanPorcentajesExactos() {
        assertEquals(1_250L, Money.percentOf(10_000L, 12.5))
        assertEquals(9_000L, Money.afterDiscount(10_000L, 10))
        assertEquals(10_000L, Money.afterDiscount(10_000L, 0))
    }

    @Test
    fun fromDecimalYToBigDecimalSonInversos() {
        assertEquals(12_345L, Money.fromDecimal(Money.toDouble(12_345L)))
        assertEquals(12_345.0, Money.toDouble(1_234_500L), 0.0001)
        assertEquals(1_234_500L, Money.fromBigDecimal(Money.toBigDecimal(1_234_500L)))
    }

    @Test
    fun marginPercentCalculaSobreElPrecioDeVenta() {
        assertEquals(25.0, Money.marginPercent(750L, 1_000L), 0.01)
        assertEquals(0.0, Money.marginPercent(1_000L, 0L), 0.01)
    }
}
