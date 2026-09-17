package com.gis.supermercados.data.mapper

import com.gis.supermercados.core.reporting.model.CustomerSpend
import com.gis.supermercados.core.reporting.model.ExpenseTotals
import com.gis.supermercados.core.reporting.model.PaymentSplit
import com.gis.supermercados.core.reporting.model.ReturnsTotals
import com.gis.supermercados.core.reporting.model.SalesTotals
import com.gis.supermercados.core.reporting.model.StoreExpense
import com.gis.supermercados.data.local.entity.CategoryProfitRow
import com.gis.supermercados.data.local.entity.CustomerSpendRow
import com.gis.supermercados.data.local.entity.DayExpenseRow
import com.gis.supermercados.data.local.entity.DayTotalRow
import com.gis.supermercados.data.local.entity.ExpenseCategoryRow
import com.gis.supermercados.data.local.entity.ExpenseStoreRow
import com.gis.supermercados.data.local.entity.InventoryValueRow
import com.gis.supermercados.data.local.entity.MoneyCountRow
import com.gis.supermercados.data.local.entity.PaymentMethodRow
import com.gis.supermercados.data.local.entity.ReturnsRow
import com.gis.supermercados.data.local.entity.StoreSalesRow
import com.gis.supermercados.data.local.entity.TopProductRow
import com.gis.supermercados.domain.model.CategoryProfitability
import com.gis.supermercados.domain.model.DailyTotal
import com.gis.supermercados.domain.model.InventoryValue
import com.gis.supermercados.domain.model.StoreSalesSummary
import com.gis.supermercados.domain.model.TopProduct

/** Mapeo de proyecciones SQL (agregados) a modelos de dominio/informe. */

fun StoreSalesRow.toDomain(): StoreSalesSummary = StoreSalesSummary(
    storeId = storeId,
    storeName = storeName,
    tickets = tickets,
    subtotalCents = subtotalCents,
    discountCents = discountCents,
    taxCents = taxCents,
    totalCents = totalCents,
    costCents = costCents
)

fun StoreSalesRow.toSalesTotals(): SalesTotals = SalesTotals(
    subtotalCents = subtotalCents,
    discountCents = discountCents,
    taxCents = taxCents,
    totalCents = totalCents,
    costCents = costCents,
    tickets = tickets
)

fun DayTotalRow.toDailyTotal(): DailyTotal = DailyTotal(
    dayMillis = dayBucket,
    salesCents = totalCents,
    tickets = tickets,
    costCents = costCents
)

fun DayExpenseRow.toDailyExpense(): DailyTotal = DailyTotal(
    dayMillis = dayBucket,
    expensesCents = totalCents
)

fun DayExpenseRow.toDailyReturns(): DailyTotal = DailyTotal(
    dayMillis = dayBucket,
    salesCents = totalCents
)

fun TopProductRow.toDomain(): TopProduct = TopProduct(
    productId = productId,
    name = productName,
    sku = sku,
    categoryName = categoryName,
    units = units,
    totalCents = totalCents,
    costCents = costCents
)

fun CategoryProfitRow.toDomain(): CategoryProfitability = CategoryProfitability(
    categoryId = categoryId,
    categoryName = categoryName,
    units = units,
    salesCents = salesCents,
    costCents = costCents,
    color = color
)

fun PaymentMethodRow.toDomain(): PaymentSplit = PaymentSplit(
    method = paymentMethod,
    totalCents = totalCents,
    tickets = tickets
)

fun MoneyCountRow.toExpenseTotals(): ExpenseTotals =
    ExpenseTotals(totalCents = totalCents, count = count)

fun MoneyCountRow.toReturnsTotals(): ReturnsTotals =
    ReturnsTotals(totalCents = totalCents, count = count)

fun ExpenseStoreRow.toDomain(): StoreExpense = StoreExpense(
    storeId = storeId,
    storeName = storeName.orEmpty(),
    totalCents = totalCents,
    count = count
)

fun ReturnsRow.toDomain(): StoreExpense = StoreExpense(
    storeId = storeId,
    storeName = storeName,
    totalCents = totalCents,
    count = count
)

fun CustomerSpendRow.toDomain(): CustomerSpend = CustomerSpend(
    customerId = customerId,
    name = customerName,
    tickets = tickets,
    totalCents = totalCents
)

fun InventoryValueRow.toDomain(): InventoryValue = InventoryValue(
    totalQuantity = totalQuantity,
    costValueCents = costValueCents,
    retailValueCents = retailValueCents,
    lowStockCount = lowStockCount,
    outOfStockCount = outOfStockCount
)

/** Agrupa las categorias de gasto con su etiqueta localizada. */
fun List<ExpenseCategoryRow>.toNamedTotals(labelResolver: (ExpenseCategoryRow) -> String) =
    map { row ->
        com.gis.supermercados.domain.model.NamedTotal(
            id = 0L,
            name = labelResolver(row),
            totalCents = row.totalCents,
            quantity = row.count
        )
    }
