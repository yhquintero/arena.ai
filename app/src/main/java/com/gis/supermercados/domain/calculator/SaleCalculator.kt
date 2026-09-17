package com.gis.supermercados.domain.calculator

import com.gis.supermercados.core.common.Money
import com.gis.supermercados.domain.model.CartLine
import com.gis.supermercados.domain.model.Product
import com.gis.supermercados.domain.model.ProductUnit
import kotlin.math.floor

/**
 * Calculo exacto de una venta en el momento del cobro.
 *
 * Convenciones (las mismas que [com.gis.supermercados.domain.model.CartTotals],
 * para que la vista previa y lo guardado cuadren al centavo):
 *  1. `unitPriceCents` es precio SIN impuesto; `taxRate` es fraccion (0.16 = 16%).
 *  2. Primero se aplica el descuento POR LINEA y despues el GLOBAL.
 *  3. El descuento global se reparte con el metodo del resto mayor
 *     (largest remainder): la suma del reparto es exactamente el descuento y
 *     ningun centavo se pierde ni se duplica.
 *  4. El impuesto se calcula sobre la base realmente cobrada de cada linea.
 *  5. `subtotal` = bruto; `total` = subtotal - descuentos + impuestos.
 *
 * Objeto PURO (sin dependencias de Android), cubierto por tests unitarios.
 */
object SaleCalculator {

    /**
     * @param lines lineas del carrito.
     * @param globalDiscountCents descuento global en centavos (tiene prioridad).
     * @param globalDiscountPercent descuento global en porcentaje (0..100),
     *   usado solo si [globalDiscountCents] es 0.
     */
    fun calculate(
        lines: List<CartLine>,
        globalDiscountCents: Long = 0L,
        globalDiscountPercent: Double = 0.0,
    ): SaleCalculation {
        if (lines.isEmpty()) return SaleCalculation.EMPTY

        val gross = lines.sumOf { it.grossCents }
        val lineDiscount = lines.sumOf { it.discountCents }
        val cost = lines.sumOf { it.costCents }
        val afterLineDiscount = (gross - lineDiscount).coerceAtLeast(0L)

        val globalDiscount = when {
            globalDiscountCents > 0L -> globalDiscountCents
            globalDiscountPercent > 0.0 ->
                Money.percentOf(afterLineDiscount, globalDiscountPercent)
            else -> 0L
        }.coerceIn(0L, afterLineDiscount)

        val shares = distribute(globalDiscount, lines.map { it.netCents })

        val detailed = lines.mapIndexed { index, line ->
            val share = shares.getOrElse(index) { 0L }
            val netBase = (line.netCents - share).coerceAtLeast(0L)
            val tax = Money.tax(netBase, line.taxRate)
            CalculatedLine(
                line = line,
                globalDiscountCents = share,
                netCents = netBase,
                taxCents = tax,
                totalCents = netBase + tax,
                costCents = line.costCents,
                profitCents = netBase - line.costCents
            )
        }

        val netBase = detailed.sumOf { it.netCents }
        val tax = detailed.sumOf { it.taxCents }

        return SaleCalculation(
            lines = detailed,
            subtotalCents = gross,
            lineDiscountCents = lineDiscount,
            globalDiscountCents = globalDiscount,
            discountCents = lineDiscount + globalDiscount,
            netBaseCents = netBase,
            taxCents = tax,
            totalCents = netBase + tax,
            costCents = cost,
            profitCents = netBase - cost
        )
    }

    /** Reparte [amount] entre [weights] sin perder ni un centavo (resto mayor). */
    fun distribute(amount: Long, weights: List<Long>): List<Long> {
        if (amount <= 0L || weights.isEmpty()) return List(weights.size) { 0L }
        val totalWeight = weights.sum()
        if (totalWeight <= 0L) return List(weights.size) { 0L }

        val shares = weights.map { weight ->
            val exact = amount.toDouble() * weight.toDouble() / totalWeight.toDouble()
            val base = floor(exact).toLong()
            Share(floor = base, remainder = exact - base)
        }

        val result = shares.map { it.floor }.toMutableList()
        var pending = amount - shares.sumOf { it.floor }
        val order = shares.indices.sortedByDescending { shares[it].remainder }
        var index = 0
        while (pending > 0L && order.isNotEmpty() && index < MAX_DISTRIBUTION_STEPS) {
            result[order[index % order.size]] += 1L
            pending -= 1L
            index++
        }
        return result
    }

    /** Cambio a devolver tras cobrar [receivedCents]. */
    fun change(totalCents: Long, receivedCents: Long): Long = receivedCents - totalCents

    /** Precio de estanteria (con impuesto incluido). */
    fun priceWithTax(product: Product): Long = Money.withTax(product.priceCents, product.taxRate)

    /** Puntos de fidelidad: 1 punto por cada 100 centavos ($1) gastados. */
    fun loyaltyPointsFor(totalCents: Long): Int =
        (totalCents / LOYALTY_DIVISOR).toInt().coerceAtLeast(0)

    private data class Share(val floor: Long, val remainder: Double)

    private const val MAX_DISTRIBUTION_STEPS = 10_000
    private const val LOYALTY_DIVISOR = 100L
}

/** Linea del carrito ya calculada (base, impuesto y margen). */
data class CalculatedLine(
    val line: CartLine,
    val globalDiscountCents: Long,
    val netCents: Long,
    val taxCents: Long,
    val totalCents: Long,
    val costCents: Long,
    val profitCents: Long,
) {
    val productId: Long get() = line.productId
    val productName: String get() = line.productName
    val sku: String get() = line.sku
    val quantity: Int get() = line.quantity
    val unit: ProductUnit get() = line.unit
    val unitPriceCents: Long get() = line.unitPriceCents
    val unitCostCents: Long get() = line.unitCostCents
    val taxRate: Double get() = line.taxRate
    val discountPercent: Int get() = line.discountPercent
    val isService: Boolean get() = line.isService
    val requiresStock: Boolean get() = line.requiresStock
}

/** Resultado completo del calculo de una venta. */
data class SaleCalculation(
    val lines: List<CalculatedLine>,
    val subtotalCents: Long,
    val lineDiscountCents: Long,
    val globalDiscountCents: Long,
    val discountCents: Long,
    val netBaseCents: Long,
    val taxCents: Long,
    val totalCents: Long,
    val costCents: Long,
    val profitCents: Long,
) {
    val itemCount: Int get() = lines.sumOf { it.quantity }
    val lineCount: Int get() = lines.size
    val isEmpty: Boolean get() = lines.isEmpty()
    val grossMarginPercent: Double
        get() = Money.marginPercent(costCents, netBaseCents)
    val averageLineCents: Long
        get() = if (lineCount == 0) 0L else netBaseCents / lineCount

    companion object {
        val EMPTY = SaleCalculation(
            lines = emptyList(),
            subtotalCents = 0L,
            lineDiscountCents = 0L,
            globalDiscountCents = 0L,
            discountCents = 0L,
            netBaseCents = 0L,
            taxCents = 0L,
            totalCents = 0L,
            costCents = 0L,
            profitCents = 0L
        )
    }
}
