package com.gis.supermercados.domain.model

/**
 * Gasto operacional.
 * [storeId] = null indica un gasto CORPORATIVO (no imputable a una sucursal).
 */
data class Expense(
    val id: Long = 0L,
    val category: ExpenseCategory,
    val storeId: Long? = null,
    val storeName: String = "",
    val concept: String,
    val provider: String = "",
    /** Importe base sin impuestos. */
    val amountCents: Long = 0L,
    val taxCents: Long = 0L,
    /** Total pagado = amount + tax. */
    val totalCents: Long = 0L,
    val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    val reference: String = "",
    /** Ruta/URI del justificante adjunto (factura, recibo...). */
    val receiptPath: String = "",
    val hasReceipt: Boolean = false,
    val userId: Long = 0L,
    val userName: String = "",
    /** Fecha contable del gasto (puede diferir de la fecha de registro). */
    val expenseDate: Long = 0L,
    val createdAt: Long = 0L,
) {
    val isCorporate: Boolean get() = storeId == null
}

/** Datos necesarios para crear/editar un gasto desde la UI. */
data class ExpenseDraft(
    val id: Long = 0L,
    val category: ExpenseCategory = ExpenseCategory.OTROS,
    val storeId: Long? = null,
    val concept: String = "",
    val provider: String = "",
    val amountCents: Long = 0L,
    val taxRate: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    val reference: String = "",
    val receiptPath: String = "",
    val expenseDate: Long = 0L,
)
