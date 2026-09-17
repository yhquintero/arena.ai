package com.gis.supermercados.domain.calculator

import com.gis.supermercados.domain.model.CartLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del calculo exacto de una venta: descuentos por linea, descuento global repartido
 * sin perder centavos, impuesto sumado sobre la base neta, utilidad y puntos de fidelidad.
 * Es la logica mas critica del negocio (dinero real del cliente).
 */
class SaleCalculatorTest {

    private fun line(
        price: Long,
        quantity: Int = 1,
        taxRate: Double = 0.16,
        cost: Long = 0L,
        discountPercent: Int = 0,
        id: Long = 1L,
    ) = CartLine(
        productId = id,
        productName = "Producto $id",
        sku = "SKU-$id",
        unitPriceCents = price,
        unitCostCents = cost,
        taxRate = taxRate,
        quantity = quantity,
        discountPercent = discountPercent
    )

    @Test
    fun ventaSimpleSumaImpuestoSobreLaBase() {
        val result = SaleCalculator.calculate(listOf(line(price = 1_000, quantity = 2, cost = 600)))

        assertEquals(2_000L, result.subtotalCents)
        assertEquals(2_000L, result.netBaseCents)
        assertEquals(320L, result.taxCents)
        assertEquals(2_320L, result.totalCents)
        assertEquals(1_200L, result.costCents)
        assertEquals(800L, result.profitCents)
    }

    @Test
    fun descuentoPorLineaReduceBaseEImpuesto() {
        val result = SaleCalculator.calculate(
            listOf(line(price = 1_000, quantity = 2, cost = 600, discountPercent = 10))
        )

        assertEquals(2_000L, result.subtotalCents)
        assertEquals(200L, result.lineDiscountCents)
        assertEquals(1_800L, result.netBaseCents)
        assertEquals(288L, result.taxCents)
        assertEquals(2_088L, result.totalCents)
        assertEquals(600L, result.profitCents)
    }

    @Test
    fun descuentoGlobalSeReparteEntreLineasSinPerderCentavos() {
        val lines = listOf(
            line(price = 1_000, taxRate = 0.16, cost = 500, id = 1),
            line(price = 2_000, taxRate = 0.0, cost = 1_200, id = 2)
        )

        val result = SaleCalculator.calculate(lines, globalDiscountPercent = 5.0)

        assertEquals(3_000L, result.subtotalCents)
        assertEquals(150L, result.globalDiscountCents)
        assertEquals(2_850L, result.netBaseCents)
        // 950 * 0.16 = 152; la segunda linea no lleva impuesto
        assertEquals(152L, result.taxCents)
        assertEquals(3_002L, result.totalCents)
        assertEquals(2_850L - 1_700L, result.profitCents)
    }

    @Test
    fun descuentoGlobalEnCentavosTienePrioridadSobreElPorcentaje() {
        val lines = listOf(line(price = 5_000, taxRate = 0.0))

        val result = SaleCalculator.calculate(lines, globalDiscountCents = 500L, globalDiscountPercent = 50.0)

        assertEquals(500L, result.globalDiscountCents)
        assertEquals(4_500L, result.totalCents)
    }

    @Test
    fun descuentoGlobalNuncaSuperaElImporteNeto() {
        val result = SaleCalculator.calculate(listOf(line(price = 1_000, taxRate = 0.0)), globalDiscountCents = 99_999L)

        assertEquals(1_000L, result.globalDiscountCents)
        assertEquals(0L, result.netBaseCents)
        assertEquals(0L, result.totalCents)
    }

    @Test
    fun carritoVacioDevuelveCalculoVacio() {
        val result = SaleCalculator.calculate(emptyList())

        assertEquals(0L, result.totalCents)
        assertEquals(0L, result.subtotalCents)
        assertTrue(result.lines.isEmpty())
    }

    @Test
    fun distributeReparteExactamenteElImporte() {
        val shares = SaleCalculator.distribute(100L, listOf(1L, 1L, 1L))

        assertEquals(3, shares.size)
        assertEquals(100L, shares.sum())
        assertTrue(shares.all { it in 33L..34L })
    }

    @Test
    fun distributeSinPesosDevuelveCeros() {
        assertEquals(listOf(0L, 0L), SaleCalculator.distribute(100L, listOf(0L, 0L)))
        assertTrue(SaleCalculator.distribute(0L, listOf(10L)).all { it == 0L })
    }

    @Test
    fun changeCalculaElVuelto() {
        assertEquals(2_680L, SaleCalculator.change(totalCents = 2_320L, receivedCents = 5_000L))
        assertEquals(0L, SaleCalculator.change(totalCents = 2_320L, receivedCents = 2_320L))
    }

    @Test
    fun loyaltyPointsSeAcumulanPorCadaUnidadMonetaria() {
        assertEquals(23, SaleCalculator.loyaltyPointsFor(2_320L))
        assertEquals(0, SaleCalculator.loyaltyPointsFor(99L))
    }

    @Test
    fun losServiciosNoAportanCostoDeMercancia() {
        val result = SaleCalculator.calculate(
            listOf(line(price = 10_000, taxRate = 0.16, cost = 0L).copy(isService = true))
        )

        assertEquals(0L, result.costCents)
        assertEquals(10_000L, result.profitCents)
    }
}
