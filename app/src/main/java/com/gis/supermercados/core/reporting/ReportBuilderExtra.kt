package com.gis.supermercados.core.reporting

import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.designsystem.Brand
import com.gis.supermercados.core.reporting.model.Cell
import com.gis.supermercados.core.reporting.model.CellAlign
import com.gis.supermercados.core.reporting.model.ChartKind
import com.gis.supermercados.core.reporting.model.ChartPoint
import com.gis.supermercados.core.reporting.model.ReportBlock
import com.gis.supermercados.core.reporting.model.ReportColumn
import com.gis.supermercados.core.reporting.model.ReportData
import com.gis.supermercados.core.reporting.model.ReportRequest

/*
 * Bloques restantes del constructor de informes.
 * Se declaran como funciones de extension internas de [ReportBuilder] para
 * mantener cada archivo en un tamano razonable sin romper la cohesion: todos
 * comparten el mismo acceso a recursos (context) y etiquetas (labels).
 */

// -------------------------------- Flujo de caja --------------------------------

internal fun ReportBuilder.cashFlow(request: ReportRequest, data: ReportData): List<ReportBlock> {
    val symbol = request.currencySymbol
    val blocks = mutableListOf<ReportBlock>()

    val series = ReportCalculations.buildDailySeries(
        range = request.range,
        sales = data.dailySales,
        expenses = data.dailyExpenses,
        returns = data.dailyReturns
    )
    val flow = ReportCalculations.buildCashFlow(series)
    val totalInflow = flow.sumOf { it.inflowCents }
    val totalOutflow = flow.sumOf { it.outflowCents }
    val netFlow = totalInflow - totalOutflow

    blocks += ReportBlock.KpiGrid(
        listOf(
            kpi(context.getString(R.string.kpi_cash_in), Money.format(totalInflow, symbol), highlight = true),
            kpi(context.getString(R.string.kpi_cash_out), Money.format(totalOutflow, symbol)),
            kpi(context.getString(R.string.kpi_net_cash_flow), Money.format(netFlow, symbol), highlight = true),
            kpi(
                context.getString(R.string.kpi_best_day),
                flow.maxByOrNull { it.inflowCents }?.let { Money.format(it.inflowCents, symbol) } ?: "-",
                detail = flow.maxByOrNull { it.inflowCents }?.let { AppDateTime.formatDate(it.dateMillis) }.orEmpty()
            )
        )
    )

    blocks += ReportBlock.SectionTitle(context.getString(R.string.report_section_daily_cash_flow))
    blocks += ReportBlock.Table(
        title = context.getString(R.string.report_table_daily_cash_flow),
        columns = listOf(
            ReportColumn(context.getString(R.string.column_date), CellAlign.CENTER, 1.1f),
            ReportColumn(context.getString(R.string.column_inflow), CellAlign.END, 1.2f),
            ReportColumn(context.getString(R.string.column_outflow), CellAlign.END, 1.2f),
            ReportColumn(context.getString(R.string.column_net), CellAlign.END, 1.2f),
            ReportColumn(context.getString(R.string.column_cumulative_balance), CellAlign.END, 1.3f),
            ReportColumn(context.getString(R.string.column_tickets), CellAlign.END, 0.8f)
        ),
        rows = flow.take(MAX_TABLE_ROWS).mapIndexed { index, entry ->
            listOf(
                Cell.DateCell(entry.dateMillis),
                Cell.Money(entry.inflowCents),
                Cell.Money(entry.outflowCents),
                Cell.Money(entry.netCents),
                Cell.Money(entry.closingBalanceCents),
                Cell.Count(series.getOrNull(index)?.tickets?.toLong() ?: 0L)
            )
        },
        totalRow = listOf(
            Cell.Text(context.getString(R.string.row_total)),
            Cell.Money(totalInflow),
            Cell.Money(totalOutflow),
            Cell.Money(netFlow),
            Cell.Money(flow.lastOrNull()?.closingBalanceCents ?: netFlow),
            Cell.Count(series.sumOf { it.tickets.toLong() })
        ),
        note = context.getString(R.string.report_note_cash_flow)
    )

    if (data.paymentSplits.isNotEmpty()) {
        blocks += ReportBlock.SectionTitle(context.getString(R.string.report_section_payment_methods))
        blocks += paymentTable(request, data.paymentSplits, data.salesTotals.totalCents)
    }

    if (request.includeCharts && flow.isNotEmpty()) {
        blocks += ReportBlock.Chart(
            title = context.getString(R.string.chart_net_cash_flow),
            kind = ChartKind.BARS,
            points = flow.map {
                ChartPoint(AppDateTime.formatShortDate(it.dateMillis), it.netCents)
            }
        )
    }
    return blocks
}

// -------------------------- Rentabilidad por categoria -------------------------

internal fun ReportBuilder.categoryProfitability(
    request: ReportRequest,
    data: ReportData,
): List<ReportBlock> {
    val symbol = request.currencySymbol
    val blocks = mutableListOf<ReportBlock>()
    val categories = data.categoryProfitability
    val totalSales = categories.sumOf { it.salesCents }
    val totalCost = categories.sumOf { it.costCents }
    val totalProfit = totalSales - totalCost
    val best = categories.maxByOrNull { it.profitCents }
    val worst = categories.minByOrNull { it.marginPercent }

    blocks += ReportBlock.KpiGrid(
        listOf(
            kpi(context.getString(R.string.kpi_categories), categories.size.toString(), highlight = true),
            kpi(context.getString(R.string.kpi_category_sales), Money.format(totalSales, symbol)),
            kpi(context.getString(R.string.kpi_category_profit), Money.format(totalProfit, symbol)),
            kpi(
                context.getString(R.string.kpi_average_margin),
                formatPercent(Money.marginPercent(totalCost, totalSales))
            )
        )
    )

    blocks += ReportBlock.SectionTitle(context.getString(R.string.report_section_category_profitability))
    blocks += ReportBlock.Table(
        title = context.getString(R.string.report_table_category_profitability),
        columns = listOf(
            ReportColumn(context.getString(R.string.column_category), weight = 2.1f),
            ReportColumn(context.getString(R.string.column_units), CellAlign.END, 0.8f),
            ReportColumn(context.getString(R.string.column_revenue), CellAlign.END, 1.2f),
            ReportColumn(context.getString(R.string.column_cost), CellAlign.END, 1.1f),
            ReportColumn(context.getString(R.string.column_profit), CellAlign.END, 1.1f),
            ReportColumn(context.getString(R.string.column_margin), CellAlign.END, 0.9f),
            ReportColumn(context.getString(R.string.column_share), CellAlign.END, 0.9f)
        ),
        rows = categories.map { category ->
            listOf(
                Cell.Text(category.categoryName),
                Cell.Count(category.units.toLong()),
                Cell.Money(category.salesCents),
                Cell.Money(category.costCents),
                Cell.Money(category.profitCents),
                Cell.Decimal(category.marginPercent, 1, "%"),
                Cell.Decimal(sharePercent(category.salesCents, totalSales), 1, "%")
            )
        },
        totalRow = listOf(
            Cell.Text(context.getString(R.string.row_total)),
            Cell.Count(categories.sumOf { it.units.toLong() }),
            Cell.Money(totalSales),
            Cell.Money(totalCost),
            Cell.Money(totalProfit),
            Cell.Decimal(Money.marginPercent(totalCost, totalSales), 1, "%"),
            Cell.Decimal(if (totalSales == 0L) 0.0 else 100.0, 1, "%")
        )
    )

    if (best != null) {
        blocks += ReportBlock.Paragraph(
            context.getString(
                R.string.report_insight_best_category,
                best.categoryName,
                Money.format(best.profitCents, symbol),
                formatPercent(best.marginPercent)
            )
        )
    }
    if (worst != null && worst.marginPercent < LOW_MARGIN_THRESHOLD) {
        blocks += ReportBlock.Paragraph(
            context.getString(
                R.string.report_insight_low_margin_category,
                worst.categoryName,
                formatPercent(worst.marginPercent)
            ),
            bold = true
        )
    }

    if (request.includeCharts && categories.isNotEmpty()) {
        blocks += ReportBlock.Chart(
            title = context.getString(R.string.chart_category_composition),
            kind = ChartKind.DONUT,
            points = categories.take(MAX_CHART_POINTS).mapIndexed { index, category ->
                ChartPoint(
                    label = category.categoryName,
                    value = category.salesCents,
                    colorArgb = if (category.color != 0L) category.color else Brand.chartColor(index)
                )
            }
        )
        blocks += ReportBlock.Chart(
            title = context.getString(R.string.chart_profit_by_category),
            kind = ChartKind.HORIZONTAL_BARS,
            points = categories.take(MAX_CHART_POINTS).mapIndexed { index, category ->
                ChartPoint(category.categoryName, category.profitCents, colorArgb = Brand.chartColor(index))
            }
        )
    }
    return blocks
}

// ----------------------------- Analisis de gastos ------------------------------

internal fun ReportBuilder.expenseAnalysis(request: ReportRequest, data: ReportData): List<ReportBlock> {
    val symbol = request.currencySymbol
    val blocks = mutableListOf<ReportBlock>()
    val totalExpenses = data.expenseTotals.totalCents
    val records = data.expenseTotals.count
    val averagePerRecord = if (records == 0) 0L else totalExpenses / records
    val expenseOverRevenue = sharePercent(totalExpenses, data.salesTotals.netBaseCents)

    blocks += ReportBlock.KpiGrid(
        listOf(
            kpi(context.getString(R.string.kpi_total_expenses), Money.format(totalExpenses, symbol), highlight = true),
            kpi(context.getString(R.string.kpi_expense_records), records.toString()),
            kpi(context.getString(R.string.kpi_average_expense), Money.format(averagePerRecord, symbol)),
            kpi(context.getString(R.string.kpi_expenses_over_sales), formatPercent(expenseOverRevenue))
        )
    )

    blocks += ReportBlock.SectionTitle(context.getString(R.string.report_section_expense_breakdown))
    blocks += expenseCategoryTable(request, data.expensesByCategory, totalExpenses)

    if (data.expensesByStore.isNotEmpty()) {
        blocks += ReportBlock.SectionTitle(context.getString(R.string.report_section_expenses_by_store))
        blocks += ReportBlock.Table(
            title = context.getString(R.string.report_table_expenses_by_store),
            columns = listOf(
                ReportColumn(context.getString(R.string.column_store), weight = 2.2f),
                ReportColumn(context.getString(R.string.column_records), CellAlign.END, 0.9f),
                ReportColumn(context.getString(R.string.column_amount), CellAlign.END, 1.3f),
                ReportColumn(context.getString(R.string.column_share), CellAlign.END, 0.9f)
            ),
            rows = data.expensesByStore.map { store ->
                listOf(
                    Cell.Text(
                        store.storeName.ifBlank {
                            if (store.storeId == null) labels.corporateLabel() else "-"
                        }
                    ),
                    Cell.Count(store.count.toLong()),
                    Cell.Money(store.totalCents),
                    Cell.Decimal(sharePercent(store.totalCents, totalExpenses), 1, "%")
                )
            },
            totalRow = listOf(
                Cell.Text(context.getString(R.string.row_total)),
                Cell.Count(data.expensesByStore.sumOf { it.count.toLong() }),
                Cell.Money(data.expensesByStore.sumOf { it.totalCents }),
                Cell.Decimal(if (totalExpenses == 0L) 0.0 else 100.0, 1, "%")
            )
        )
    }

    if (request.includeCharts && data.expensesByCategory.isNotEmpty()) {
        blocks += ReportBlock.Chart(
            title = context.getString(R.string.chart_expenses_by_category),
            kind = ChartKind.HORIZONTAL_BARS,
            points = data.expensesByCategory.take(MAX_CHART_POINTS).mapIndexed { index, category ->
                ChartPoint(category.name, category.totalCents, colorArgb = Brand.chartColor(index))
            }
        )
    }

    if (request.includeCharts && data.dailyExpenses.isNotEmpty()) {
        blocks += ReportBlock.Chart(
            title = context.getString(R.string.chart_daily_expenses),
            kind = ChartKind.BARS,
            points = ReportCalculations.buildDailySeries(request.range, data.dailySales, data.dailyExpenses)
                .map { ChartPoint(AppDateTime.formatShortDate(it.dayMillis), it.expensesCents) }
        )
    }
    return blocks
}

// --------------------------- Comparativa de periodos ---------------------------

internal fun ReportBuilder.periodComparison(request: ReportRequest, data: ReportData): List<ReportBlock> {
    val symbol = request.currencySymbol
    val blocks = mutableListOf<ReportBlock>()
    val comparison = data.comparison ?: return listOf(
        ReportBlock.Paragraph(context.getString(R.string.report_no_comparison_data))
    )

    val current = comparison.current
    val previous = comparison.previous

    fun row(concept: String, currentValue: Long, previousValue: Long): List<Cell> {
        val delta = currentValue - previousValue
        val variation = ReportCalculations.variationPercent(previousValue, currentValue)
        return listOf(
            Cell.Text(concept),
            Cell.Money(currentValue),
            Cell.Money(previousValue),
            Cell.Money(delta),
            Cell.Decimal(variation, 1, "%")
        )
    }

    blocks += ReportBlock.KpiGrid(
        listOf(
            kpi(
                label = context.getString(R.string.kpi_sales_variation),
                value = formatPercent(ReportCalculations.variationPercent(previous.totalCents, current.totalCents)),
                detail = Money.format(current.totalCents, symbol),
                variation = ReportCalculations.variationPercent(previous.totalCents, current.totalCents),
                highlight = true
            ),
            kpi(
                label = context.getString(R.string.kpi_tickets_variation),
                value = formatPercent(
                    ReportCalculations.variationPercent(previous.tickets.toLong(), current.tickets.toLong())
                ),
                detail = current.tickets.toString(),
                variation = ReportCalculations.variationPercent(previous.tickets.toLong(), current.tickets.toLong())
            ),
            kpi(
                label = context.getString(R.string.kpi_expenses_variation),
                value = formatPercent(
                    ReportCalculations.variationPercent(
                        comparison.previousExpensesCents,
                        comparison.currentExpensesCents
                    )
                ),
                detail = Money.format(comparison.currentExpensesCents, symbol),
                variation = ReportCalculations.variationPercent(
                    comparison.previousExpensesCents,
                    comparison.currentExpensesCents
                )
            ),
            kpi(
                label = context.getString(R.string.kpi_profit_variation),
                value = formatPercent(
                    ReportCalculations.variationPercent(previous.grossProfitCents, current.grossProfitCents)
                ),
                detail = Money.format(current.grossProfitCents, symbol),
                variation = ReportCalculations.variationPercent(previous.grossProfitCents, current.grossProfitCents)
            )
        )
    )

    blocks += ReportBlock.SectionTitle(context.getString(R.string.report_section_comparison))
    blocks += ReportBlock.Table(
        title = context.getString(R.string.report_table_comparison),
        columns = listOf(
            ReportColumn(context.getString(R.string.column_concept), weight = 2.2f),
            ReportColumn(context.getString(R.string.column_current_period), CellAlign.END, 1.2f),
            ReportColumn(context.getString(R.string.column_previous_period), CellAlign.END, 1.2f),
            ReportColumn(context.getString(R.string.column_variation_amount), CellAlign.END, 1.1f),
            ReportColumn(context.getString(R.string.column_variation_percent), CellAlign.END, 0.9f)
        ),
        rows = listOf(
            row(context.getString(R.string.row_gross_sales), current.subtotalCents, previous.subtotalCents),
            row(context.getString(R.string.row_net_sales), current.netBaseCents, previous.netBaseCents),
            row(context.getString(R.string.row_taxes), current.taxCents, previous.taxCents),
            row(context.getString(R.string.row_cost_of_goods), current.costCents, previous.costCents),
            row(context.getString(R.string.row_gross_profit), current.grossProfitCents, previous.grossProfitCents),
            row(
                context.getString(R.string.row_expenses),
                comparison.currentExpensesCents,
                comparison.previousExpensesCents
            ),
            row(
                context.getString(R.string.row_returns),
                comparison.currentReturnsCents,
                comparison.previousReturnsCents
            ),
            row(
                context.getString(R.string.row_operating_result),
                ReportCalculations.netProfitCents(current, comparison.currentExpensesCents, comparison.currentReturnsCents),
                ReportCalculations.netProfitCents(previous, comparison.previousExpensesCents, comparison.previousReturnsCents)
            )
        ),
        note = context.getString(R.string.report_note_comparison)
    )

    if (request.includeCharts) {
        blocks += ReportBlock.Chart(
            title = context.getString(R.string.chart_period_comparison),
            kind = ChartKind.HORIZONTAL_BARS,
            points = listOf(
                ChartPoint(
                    context.getString(R.string.chart_current_sales),
                    current.totalCents,
                    colorArgb = Brand.PRIMARY
                ),
                ChartPoint(
                    context.getString(R.string.chart_previous_sales),
                    previous.totalCents,
                    colorArgb = Brand.SECONDARY
                ),
                ChartPoint(
                    context.getString(R.string.chart_current_expenses),
                    comparison.currentExpensesCents,
                    colorArgb = Brand.ACCENT
                ),
                ChartPoint(
                    context.getString(R.string.chart_previous_expenses),
                    comparison.previousExpensesCents,
                    colorArgb = Brand.TEXT_TERTIARY
                )
            )
        )
    }
    return blocks
}

// -------------------------------- Proyecciones ---------------------------------

internal fun ReportBuilder.projections(request: ReportRequest, data: ReportData): List<ReportBlock> {
    val symbol = request.currencySymbol
    val blocks = mutableListOf<ReportBlock>()

    val series = ReportCalculations.buildDailySeries(request.range, data.dailySales)
    val days = request.range.days.coerceAtLeast(1L)
    val dailyAverage = ReportCalculations.dailyAverageCents(data.salesTotals.totalCents, days)
    val trend = ReportCalculations.trendPercent(series)
    val horizons = listOf(30, 90, 180, 365)
    val projected = ReportCalculations.project(
        baseTotalCents = data.salesTotals.totalCents,
        baseDays = days,
        trendPercent = trend,
        horizonsDays = horizons
    )
    val margin = data.salesTotals.grossMarginPercent / 100.0

    blocks += ReportBlock.Paragraph(context.getString(R.string.report_projection_methodology))

    blocks += ReportBlock.KpiGrid(
        listOf(
            kpi(context.getString(R.string.kpi_daily_average), Money.format(dailyAverage, symbol), highlight = true),
            kpi(context.getString(R.string.kpi_period_days), days.toString()),
            kpi(context.getString(R.string.kpi_trend), formatPercent(trend), variation = trend),
            kpi(
                context.getString(R.string.kpi_projection_30_days),
                Money.format(projected.firstOrNull()?.projectedCents ?: 0L, symbol)
            )
        )
    )

    blocks += ReportBlock.SectionTitle(context.getString(R.string.report_section_projections))
    blocks += ReportBlock.Table(
        title = context.getString(R.string.report_table_projections),
        columns = listOf(
            ReportColumn(context.getString(R.string.column_horizon), weight = 1.6f),
            ReportColumn(context.getString(R.string.column_days), CellAlign.END, 0.8f),
            ReportColumn(context.getString(R.string.column_projected_sales), CellAlign.END, 1.3f),
            ReportColumn(context.getString(R.string.column_projected_profit), CellAlign.END, 1.3f)
        ),
        rows = projected.map { period ->
            listOf(
                Cell.Text(context.getString(R.string.report_horizon_next_days, period.days)),
                Cell.Count(period.days.toLong()),
                Cell.Money(period.projectedCents),
                Cell.Money((period.projectedCents * margin).toLong())
            )
        },
        note = context.getString(R.string.report_note_projections)
    )

    if (request.includeCharts) {
        blocks += ReportBlock.Chart(
            title = context.getString(R.string.chart_projections),
            kind = ChartKind.BARS,
            points = projected.mapIndexed { index, period ->
                ChartPoint(
                    label = context.getString(R.string.chart_horizon_days, period.days),
                    value = period.projectedCents,
                    colorArgb = Brand.chartColor(index)
                )
            }
        )
        if (series.isNotEmpty()) {
            blocks += ReportBlock.Chart(
                title = context.getString(R.string.chart_daily_sales),
                kind = ChartKind.LINE,
                points = series.map { ChartPoint(AppDateTime.formatShortDate(it.dayMillis), it.salesCents) }
            )
        }
    }
    return blocks
}

// ---------------------------- Inventario valorado ------------------------------

internal fun ReportBuilder.valuedInventory(request: ReportRequest, data: ReportData): List<ReportBlock> {
    val symbol = request.currencySymbol
    val blocks = mutableListOf<ReportBlock>()
    val value = data.inventoryValue

    blocks += ReportBlock.KpiGrid(
        listOf(
            kpi(context.getString(R.string.kpi_inventory_units), value.totalQuantity.toString(), highlight = true),
            kpi(context.getString(R.string.kpi_inventory_cost_value), Money.format(value.costValueCents, symbol)),
            kpi(context.getString(R.string.kpi_inventory_retail_value), Money.format(value.retailValueCents, symbol)),
            kpi(
                context.getString(R.string.kpi_inventory_potential_profit),
                Money.format(value.potentialProfitCents, symbol)
            ),
            kpi(
                context.getString(R.string.kpi_low_stock_items),
                value.lowStockCount.toString(),
                detail = context.getString(R.string.kpi_out_of_stock_items, value.outOfStockCount)
            )
        )
    )

    val byStore = data.stockDetail
        .groupBy { it.storeName }
        .map { (storeName, items) ->
            Triple(
                storeName,
                items.sumOf { it.quantity.toLong() },
                items.sumOf { it.stockValueCents }
            )
        }
        .sortedByDescending { it.third }

    if (byStore.isNotEmpty()) {
        blocks += ReportBlock.SectionTitle(context.getString(R.string.report_section_inventory_by_store))
        blocks += ReportBlock.Table(
            title = context.getString(R.string.report_table_inventory_by_store),
            columns = listOf(
                ReportColumn(context.getString(R.string.column_store), weight = 2.2f),
                ReportColumn(context.getString(R.string.column_skus), CellAlign.END, 1f),
                ReportColumn(context.getString(R.string.column_units), CellAlign.END, 1f),
                ReportColumn(context.getString(R.string.column_cost_value), CellAlign.END, 1.3f),
                ReportColumn(context.getString(R.string.column_retail_value), CellAlign.END, 1.3f),
                ReportColumn(context.getString(R.string.column_potential_profit), CellAlign.END, 1.3f)
            ),
            rows = byStore.map { (storeName, units, costValue) ->
                val items = data.stockDetail.filter { it.storeName == storeName }
                val retailValue = items.sumOf { it.retailValueCents }
                listOf(
                    Cell.Text(storeName),
                    Cell.Count(items.size.toLong()),
                    Cell.Count(units),
                    Cell.Money(costValue),
                    Cell.Money(retailValue),
                    Cell.Money(retailValue - costValue)
                )
            },
            totalRow = listOf(
                Cell.Text(context.getString(R.string.row_total)),
                Cell.Count(data.stockDetail.size.toLong()),
                Cell.Count(value.totalQuantity),
                Cell.Money(value.costValueCents),
                Cell.Money(value.retailValueCents),
                Cell.Money(value.potentialProfitCents)
            )
        )
    }

    val lowStock = data.stockDetail.filter { it.isLowStock }.sortedBy { it.quantity }.take(MAX_TABLE_ROWS)
    if (lowStock.isNotEmpty()) {
        blocks += ReportBlock.SectionTitle(context.getString(R.string.report_section_low_stock))
        blocks += ReportBlock.Table(
            title = context.getString(R.string.report_table_low_stock),
            columns = listOf(
                ReportColumn(context.getString(R.string.column_product), weight = 2.2f),
                ReportColumn(context.getString(R.string.column_sku), CellAlign.CENTER, 1f),
                ReportColumn(context.getString(R.string.column_store), weight = 1.6f),
                ReportColumn(context.getString(R.string.column_stock), CellAlign.END, 0.9f),
                ReportColumn(context.getString(R.string.column_minimum), CellAlign.END, 0.9f),
                ReportColumn(context.getString(R.string.column_reorder_suggestion), CellAlign.END, 1.1f)
            ),
            rows = lowStock.map { stock ->
                val suggested = (stock.maxStock.takeIf { it > 0 } ?: stock.minStock * 2) - stock.quantity
                listOf(
                    Cell.Text(stock.productName),
                    Cell.Text(stock.sku),
                    Cell.Text(stock.storeName),
                    Cell.Count(stock.quantity.toLong()),
                    Cell.Count(stock.minStock.toLong()),
                    Cell.Count(suggested.coerceAtLeast(0).toLong())
                )
            },
            note = context.getString(R.string.report_note_low_stock)
        )
    } else {
        blocks += ReportBlock.Paragraph(context.getString(R.string.report_no_low_stock), bold = true)
    }

    if (request.includeCharts && data.stockDetail.isNotEmpty()) {
        val topValue = data.stockDetail
            .sortedByDescending { it.stockValueCents }
            .take(MAX_CHART_POINTS)
        blocks += ReportBlock.Chart(
            title = context.getString(R.string.chart_inventory_value),
            kind = ChartKind.HORIZONTAL_BARS,
            points = topValue.mapIndexed { index, stock ->
                ChartPoint(stock.productName, stock.stockValueCents, colorArgb = Brand.chartColor(index))
            }
        )
    }
    return blocks
}

/** Umbral bajo el cual se destaca una categoria con margen debil. */
internal const val LOW_MARGIN_THRESHOLD = 12.0
