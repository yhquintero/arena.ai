package com.gis.supermercados.domain.model

import com.gis.supermercados.core.common.Money

/**
 * Linea del carrito del punto de venta.
 * Los precios de venta son SIN impuestos; el impuesto se calcula por linea
 * (cada producto puede tener su propia tasa).
 */
data class CartLine(
    val productId: Long,
    val productName: String,
    val sku: String = "",
    val unitPriceCents: Long,
    val unitCostCents: Long = 0L,
    val taxRate: Double = 0.0,
    val quantity: Int = 1,
    val discountPercent: Int = 0,
    val availableStock: Int = 0,
    val unit: ProductUnit = ProductUnit.UNIDAD,
    val isService: Boolean = false,
) {
    val grossCents: Long get() = unitPriceCents * quantity
    val discountCents: Long get() = Money.percentOf(grossCents, discountPercent.toDouble())
    val netCents: Long get() = grossCents - discountCents
    val taxCents: Long get() = Money.tax(netCents, taxRate)
    val totalCents: Long get() = netCents + taxCents
    val costCents: Long get() = unitCostCents * quantity
    val profitCents: Long get() = netCents - costCents

    /** Un servicio no descuenta inventario fisico. */
    val requiresStock: Boolean get() = !isService
}

/** Totales agregados del carrito (se recalculan en cada cambio). */
data class CartTotals(
    val subtotalCents: Long = 0L,
    val discountCents: Long = 0L,
    val taxCents: Long = 0L,
    val totalCents: Long = 0L,
    val costCents: Long = 0L,
    val itemCount: Int = 0,
    val lineCount: Int = 0,
) {
    val profitCents: Long get() = subtotalCents - discountCents - costCents
    val isEmpty: Boolean get() = lineCount == 0

    companion object {
        fun from(lines: List<CartLine>, globalDiscountPercent: Int = 0): CartTotals {
            val subtotal = lines.sumOf { it.grossCents }
            val lineDiscounts = lines.sumOf { it.discountCents }
            val afterLineDiscount = subtotal - lineDiscounts
            val globalDiscount = Money.percentOf(afterLineDiscount, globalDiscountPercent.toDouble())
            val netBase = afterLineDiscount - globalDiscount

            // El impuesto se recalcula sobre la base realmente cobrada, repartiendo
            // el descuento global proporcionalmente (sin descuadres de centavos).
            val tax = if (subtotal <= 0L) {
                0L
            } else {
                val factor = Money.safeDivide(netBase.toDouble(), afterLineDiscount.toDouble())
                    .coerceIn(0.0, 1.0)
                lines.sumOf { Money.tax((it.netCents * factor).toLong(), it.taxRate) }
            }

            return CartTotals(
                subtotalCents = subtotal,
                discountCents = lineDiscounts + globalDiscount,
                taxCents = tax,
                totalCents = netBase + tax,
                costCents = lines.sumOf { it.costCents },
                itemCount = lines.sumOf { it.quantity },
                lineCount = lines.size,
            )
        }
    }
}

/** Peticion de cobro enviada al caso de uso [com.gis.supermercados.domain.usecase.CheckoutSale]. */
data class CheckoutRequest(
    val storeId: Long,
    val userId: Long,
    val customerId: Long? = null,
    val lines: List<CartLine>,
    val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    val paymentReference: String = "",
    val cashReceivedCents: Long = 0L,
    val globalDiscountPercent: Int = 0,
    val notes: String = "",
)

/** Venta registrada (cabecera). */
data class Sale(
    val id: Long = 0L,
    val ticketNumber: String,
    val storeId: Long,
    val storeName: String = "",
    val customerId: Long? = null,
    val customerName: String = "",
    val userId: Long = 0L,
    val userName: String = "",
    val subtotalCents: Long = 0L,
    val discountCents: Long = 0L,
    val taxCents: Long = 0L,
    val totalCents: Long = 0L,
    val costCents: Long = 0L,
    val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    val paymentReference: String = "",
    val cashReceivedCents: Long = 0L,
    val changeCents: Long = 0L,
    val status: SaleStatus = SaleStatus.COMPLETADA,
    val itemCount: Int = 0,
    val notes: String = "",
    val createdAt: Long = 0L,
) {
    val profitCents: Long get() = subtotalCents - discountCents - costCents
    val averageTicketCents: Long get() = totalCents
    val isReturned: Boolean get() = status == SaleStatus.DEVUELTA ||
        status == SaleStatus.PARCIALMENTE_DEVUELTA
}

/** Detalle de una venta (producto, cantidad, precio y costo historico). */
data class SaleItem(
    val id: Long = 0L,
    val saleId: Long = 0L,
    val productId: Long,
    val productName: String,
    val sku: String = "",
    val quantity: Int,
    val returnedQuantity: Int = 0,
    val unitPriceCents: Long,
    val unitCostCents: Long = 0L,
    val discountPercent: Int = 0,
    val taxRate: Double = 0.0,
    val lineTotalCents: Long = 0L,
) {
    val netCents: Long get() = Money.afterDiscount(unitPriceCents * quantity, discountPercent)
    val costCents: Long get() = unitCostCents * quantity
    val profitCents: Long get() = netCents - costCents
    val returnableQuantity: Int get() = (quantity - returnedQuantity).coerceAtLeast(0)
}

/** Linea a devolver dentro de una nota de credito. */
data class ReturnLine(
    val saleItemId: Long,
    val productName: String,
    val sku: String = "",
    val maxQuantity: Int,
    val quantity: Int,
    val unitPriceCents: Long,
    val taxRate: Double = 0.0,
) {
    val grossCents: Long get() = unitPriceCents * quantity
    val taxCents: Long get() = Money.tax(grossCents, taxRate)
    val totalCents: Long get() = grossCents + taxCents
}

/** Nota de credito (devolucion total o parcial de una venta). */
data class CreditNote(
    val id: Long = 0L,
    val creditNoteNumber: String,
    val saleId: Long,
    val ticketNumber: String = "",
    val storeId: Long,
    val storeName: String = "",
    val customerId: Long? = null,
    val customerName: String = "",
    val userId: Long = 0L,
    val userName: String = "",
    val reason: String = "",
    val subtotalCents: Long = 0L,
    val taxCents: Long = 0L,
    val totalCents: Long = 0L,
    /** Si es true, los productos devueltos vuelven al stock de la tienda. */
    val restock: Boolean = true,
    val itemCount: Int = 0,
    val createdAt: Long = 0L,
    val items: List<CreditNoteItem> = emptyList(),
)

data class CreditNoteItem(
    val id: Long = 0L,
    val creditNoteId: Long = 0L,
    val saleItemId: Long,
    val productId: Long = 0L,
    val productName: String = "",
    val sku: String = "",
    val quantity: Int = 0,
    val unitPriceCents: Long = 0L,
    val taxRate: Double = 0.0,
    val lineTotalCents: Long = 0L,
)
