package com.gis.supermercados.domain.model

import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.core.common.Money

/** Rango de fechas (inclusivo) usado por consultas y reportes. */
data class DateRange(
    val startMillis: Long,
    val endMillis: Long,
    val label: String = "",
) {
    val days: Long get() = AppDateTime.daysBetween(startMillis, endMillis)
    val formattedLabel: String
        get() = label.ifBlank { AppDateTime.formatRange(startMillis, endMillis) }

    /** Rango inmediatamente anterior de la misma longitud (para comparativas). */
    fun previous(): DateRange {
        val length = endMillis - startMillis + 1L
        return DateRange(
            startMillis = startMillis - length,
            endMillis = startMillis - 1L,
            label = ""
        )
    }
}

/** Total agregado generico (reutilizado en multiples group-by). */
data class NamedTotal(
    val id: Long = 0L,
    val name: String = "",
    val totalCents: Long = 0L,
    val quantity: Int = 0,
    val costCents: Long = 0L,
    val color: Long = 0L,
)

/** Total de un dia natural (serie temporal de graficos). */
data class DailyTotal(
    val dayMillis: Long,
    val salesCents: Long = 0L,
    val expensesCents: Long = 0L,
    val tickets: Int = 0,
    val costCents: Long = 0L,
) {
    val profitCents: Long get() = salesCents - expensesCents - costCents
    val label: String get() = AppDateTime.formatShortDate(dayMillis)
}

/** Resumen de ventas por sucursal. */
data class StoreSalesSummary(
    val storeId: Long,
    val storeName: String = "",
    val tickets: Int = 0,
    val subtotalCents: Long = 0L,
    val discountCents: Long = 0L,
    val taxCents: Long = 0L,
    val totalCents: Long = 0L,
    val costCents: Long = 0L,
    val returnsCents: Long = 0L,
) {
    val profitCents: Long get() = subtotalCents - discountCents - costCents
    val netCents: Long get() = totalCents - returnsCents
    val averageTicketCents: Long get() = if (tickets == 0) 0L else totalCents / tickets
}

/** Producto mas vendido dentro del periodo. */
data class TopProduct(
    val productId: Long,
    val name: String,
    val sku: String = "",
    val categoryName: String = "",
    val units: Int = 0,
    val totalCents: Long = 0L,
    val costCents: Long = 0L,
) {
    val profitCents: Long get() = totalCents - costCents
    val marginPercent: Double get() = Money.marginPercent(costCents, totalCents)
}

/** Rentabilidad por categoria. */
data class CategoryProfitability(
    val categoryId: Long,
    val categoryName: String,
    val units: Int = 0,
    val salesCents: Long = 0L,
    val costCents: Long = 0L,
    val color: Long = Category.DEFAULT_COLOR,
) {
    val profitCents: Long get() = salesCents - costCents
    val marginPercent: Double get() = Money.marginPercent(costCents, salesCents)
}

/** Movimiento de caja: entradas (ventas cobradas) y salidas (gastos/devoluciones). */
data class CashFlowEntry(
    val dateMillis: Long,
    val inflowCents: Long = 0L,
    val outflowCents: Long = 0L,
    val openingBalanceCents: Long = 0L,
) {
    val netCents: Long get() = inflowCents - outflowCents
    val closingBalanceCents: Long get() = openingBalanceCents + netCents
    val label: String get() = AppDateTime.formatDate(dateMillis)
}

/** Resultado de la proyeccion simple (promedio diario x dias futuros). */
data class ProjectionResult(
    val baseStartMillis: Long,
    val baseEndMillis: Long,
    val baseTotalCents: Long,
    val baseDays: Long,
    val dailyAverageCents: Long,
    val growthPercent: Double,
    val projections: List<ProjectedPeriod>,
)

data class ProjectedPeriod(
    val label: String,
    val days: Int,
    val projectedCents: Long,
)

/** Metricas de la pantalla de inicio. */
data class DashboardStats(
    val todayTotalCents: Long = 0L,
    val todayTickets: Int = 0,
    val todayProfitCents: Long = 0L,
    val yesterdayTotalCents: Long = 0L,
    val weekTotalCents: Long = 0L,
    val monthTotalCents: Long = 0L,
    val monthTickets: Int = 0,
    val monthExpensesCents: Long = 0L,
    val monthProfitCents: Long = 0L,
    val monthReturnsCents: Long = 0L,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val activeStores: Int = 0,
    val activeProducts: Int = 0,
    val activeCustomers: Int = 0,
    val inventoryValueCents: Long = 0L,
    val inventoryRetailCents: Long = 0L,
    val dailySeries: List<DailyTotal> = emptyList(),
    val salesByStore: List<StoreSalesSummary> = emptyList(),
    val topProducts: List<TopProduct> = emptyList(),
    val expensesByCategory: List<NamedTotal> = emptyList(),
) {
    val dayVariationPercent: Double
        get() = Money.variationPercent(yesterdayTotalCents, todayTotalCents)
}
