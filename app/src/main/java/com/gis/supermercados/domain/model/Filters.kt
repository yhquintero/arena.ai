package com.gis.supermercados.domain.model

/** Filtros combinables del historial de ventas. */
data class SaleFilter(
    val storeId: Long? = null,
    val from: Long? = null,
    val to: Long? = null,
    val status: SaleStatus? = null,
    val query: String? = null,
    val limit: Int = 500,
)

/** Filtros combinables del listado de gastos. */
data class ExpenseFilter(
    val storeId: Long? = null,
    val category: ExpenseCategory? = null,
    val from: Long? = null,
    val to: Long? = null,
    val query: String? = null,
    val limit: Int = 500,
)

/** Venta completa con su detalle y las notas de credito asociadas. */
data class SaleDetail(
    val sale: Sale,
    val items: List<SaleItem> = emptyList(),
    val creditNotes: List<CreditNote> = emptyList(),
) {
    val totalReturnedCents: Long get() = creditNotes.sumOf { it.totalCents }
    val netTotalCents: Long get() = sale.totalCents - totalReturnedCents
    val canBeReturned: Boolean
        get() = sale.status != SaleStatus.ANULADA &&
            sale.status != SaleStatus.DEVUELTA &&
            items.any { it.returnableQuantity > 0 }
}

/** Solicitud de devolucion (nota de credito). */
data class ReturnRequest(
    val saleId: Long,
    val userId: Long,
    val lines: List<ReturnLine>,
    val reason: String,
    val restock: Boolean = true,
)
