package com.gis.supermercados.core.reporting.model

import com.gis.supermercados.core.common.Money
import com.gis.supermercados.domain.model.CategoryProfitability
import com.gis.supermercados.domain.model.DailyTotal
import com.gis.supermercados.domain.model.InventoryMovement
import com.gis.supermercados.domain.model.InventoryValue
import com.gis.supermercados.domain.model.PaymentMethod
import com.gis.supermercados.domain.model.NamedTotal
import com.gis.supermercados.domain.model.StockLevel
import com.gis.supermercados.domain.model.StoreSalesSummary
import com.gis.supermercados.domain.model.TopProduct

/** Totales de ventas del periodo (una unica fila agregada). */
data class SalesTotals(
    val subtotalCents: Long = 0L,
    val discountCents: Long = 0L,
    val taxCents: Long = 0L,
    val totalCents: Long = 0L,
    val costCents: Long = 0L,
    val tickets: Int = 0,
) {
    /** Base imponible: ventas brutas menos descuentos. */
    val netBaseCents: Long get() = subtotalCents - discountCents

    /** Utilidad bruta = base imponible - costo de lo vendido. */
    val grossProfitCents: Long get() = netBaseCents - costCents

    val averageTicketCents: Long get() = if (tickets == 0) 0L else totalCents / tickets

    val averageItemsCents: Long get() = if (tickets == 0) 0L else netBaseCents / tickets

    val grossMarginPercent: Double get() = Money.marginPercent(costCents, netBaseCents)
}

data class PaymentSplit(
    val method: PaymentMethod,
    val totalCents: Long = 0L,
    val tickets: Int = 0,
)

data class ExpenseTotals(val totalCents: Long = 0L, val count: Int = 0)

data class ReturnsTotals(val totalCents: Long = 0L, val count: Int = 0)

data class StoreExpense(
    val storeId: Long? = null,
    val storeName: String = "",
    val totalCents: Long = 0L,
    val count: Int = 0,
)

data class CustomerSpend(
    val customerId: Long = 0L,
    val name: String = "",
    val tickets: Int = 0,
    val totalCents: Long = 0L,
)

/** Datos del periodo actual y del anterior para el informe comparativo. */
data class PeriodComparisonData(
    val current: SalesTotals = SalesTotals(),
    val previous: SalesTotals = SalesTotals(),
    val currentExpensesCents: Long = 0L,
    val previousExpensesCents: Long = 0L,
    val currentReturnsCents: Long = 0L,
    val previousReturnsCents: Long = 0L,
    val currentInventoryCents: Long = 0L,
)

/**
 * Contenedor unico con toda la materia prima de los informes.
 * Cada tipo de informe rellena solo lo que necesita (el resto queda vacio).
 */
data class ReportData(
    val salesTotals: SalesTotals = SalesTotals(),
    val salesByStore: List<StoreSalesSummary> = emptyList(),
    val dailySales: List<DailyTotal> = emptyList(),
    val topProducts: List<TopProduct> = emptyList(),
    val categoryProfitability: List<CategoryProfitability> = emptyList(),
    val paymentSplits: List<PaymentSplit> = emptyList(),
    val expenseTotals: ExpenseTotals = ExpenseTotals(),
    val expensesByCategory: List<NamedTotal> = emptyList(),
    val expensesByStore: List<StoreExpense> = emptyList(),
    val dailyExpenses: List<DailyTotal> = emptyList(),
    val returnsTotals: ReturnsTotals = ReturnsTotals(),
    val dailyReturns: List<DailyTotal> = emptyList(),
    val inventoryValue: InventoryValue = InventoryValue(),
    val stockDetail: List<StockLevel> = emptyList(),
    val movements: List<InventoryMovement> = emptyList(),
    val topCustomers: List<CustomerSpend> = emptyList(),
    val comparison: PeriodComparisonData? = null,
) {
    /** Ingresos netos = ventas cobradas - devoluciones. */
    val netRevenueCents: Long get() = salesTotals.totalCents - returnsTotals.totalCents

    /** Utilidad operativa = utilidad bruta - gastos operacionales. */
    val operatingProfitCents: Long get() = salesTotals.grossProfitCents - expenseTotals.totalCents

    val operatingMarginPercent: Double
        get() = if (netRevenueCents == 0L) 0.0
        else operatingProfitCents.toDouble() * 100.0 / netRevenueCents.toDouble()
}
