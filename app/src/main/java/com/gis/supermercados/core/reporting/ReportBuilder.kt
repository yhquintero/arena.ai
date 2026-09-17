package com.gis.supermercados.core.reporting

import android.content.Context
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.core.common.Labels
import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.designsystem.Brand
import com.gis.supermercados.core.reporting.model.Cell
import com.gis.supermercados.core.reporting.model.CellAlign
import com.gis.supermercados.core.reporting.model.ChartKind
import com.gis.supermercados.core.reporting.model.ChartPoint
import com.gis.supermercados.core.reporting.model.Kpi
import com.gis.supermercados.core.reporting.model.ReportBlock
import com.gis.supermercados.core.reporting.model.ReportColumn
import com.gis.supermercados.core.reporting.model.ReportData
import com.gis.supermercados.core.reporting.model.ReportDocument
import com.gis.supermercados.core.reporting.model.ReportRequest
import com.gis.supermercados.core.reporting.model.ReportType
import com.gis.supermercados.core.reporting.model.SalesTotals
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ensambla el [ReportDocument] a partir de los datos agregados.
 *
 * Un unico modelo de documento alimenta PDF, Excel, CSV y la vista previa en
 * pantalla: los datos nunca se calculan dos veces ni de forma distinta.
 */
@Singleton
class ReportBuilder @Inject constructor(
    @ApplicationContext internal val context: Context,
    internal val labels: Labels,
) {

    fun build(request: ReportRequest, data: ReportData): ReportDocument {
        val blocks = when (request.type) {
            ReportType.RESUMEN_EJECUTIVO -> executiveSummary(request, data)
            ReportType.BALANCE_GENERAL -> balanceSheet(request, data)
            ReportType.FLUJO_CAJA -> cashFlow(request, data)
            ReportType.VENTAS_POR_TIENDA -> salesByStore(request, data)
            ReportType.PRODUCTOS_MAS_VENDIDOS -> topProducts(request, data)
            ReportType.RENTABILIDAD_CATEGORIA -> categoryProfitability(request, data)
            ReportType.ANALISIS_GASTOS -> expenseAnalysis(request, data)
            ReportType.COMPARATIVA_PERIODOS -> periodComparison(request, data)
            ReportType.PROYECCIONES -> projections(request, data)
            ReportType.INVENTARIO_VALORADO -> valuedInventory(request, data)
        }

        return ReportDocument(
            title = labels.of(request.type),
            subtitle = labels.reportSubtitle(request.type),
            companyName = request.companyName.ifBlank { context.getString(R.string.app_name) },
            companyTaxId = request.companyTaxId,
            companyAddress = request.companyAddress,
            storeLabel = request.storeName.ifBlank { labels.allStoresLabel() },
            periodLabel = labels.periodLabel(
                request.range.startMillis,
                request.range.endMillis,
                request.periodType
            ),
            generatedAtLabel = AppDateTime.formatDateTime(request.generatedAt),
            generatedBy = request.generatedBy,
            currencySymbol = request.currencySymbol,
            blocks = blocks,
            footerNote = context.getString(R.string.report_footer_note),
            type = request.type
        )
    }

    // --------------------------- Informe ejecutivo ---------------------------

    internal fun executiveSummary(request: ReportRequest, data: ReportData): List<ReportBlock> {
        val blocks = mutableListOf<ReportBlock>()
        val symbol = request.currencySymbol

        blocks += ReportBlock.KpiGrid(
            listOf(
                kpi(
                    label = context.getString(R.string.kpi_net_revenue),
                    value = Money.format(data.netRevenueCents, symbol),
                    detail = labels.pluralTickets(data.salesTotals.tickets),
                    highlight = true
                ),
                kpi(
                    label = context.getString(R.string.kpi_gross_profit),
                    value = Money.format(data.salesTotals.grossProfitCents, symbol),
                    detail = context.getString(
                        R.string.kpi_margin_format,
                        formatPercent(data.salesTotals.grossMarginPercent)
                    )
                ),
                kpi(
                    label = context.getString(R.string.kpi_operating_expenses),
                    value = Money.format(data.expenseTotals.totalCents, symbol),
                    detail = context.getString(R.string.kpi_records_format, data.expenseTotals.count)
                ),
                kpi(
                    label = context.getString(R.string.kpi_operating_profit),
                    value = Money.format(data.operatingProfitCents, symbol),
                    detail = context.getString(
                        R.string.kpi_margin_format,
                        formatPercent(data.operatingMarginPercent)
                    )
                ),
            )
        )

        if (data.salesByStore.isNotEmpty()) {
            blocks += ReportBlock.SectionTitle(context.getString(R.string.report_section_sales_by_store))
            blocks += storeTable(request, data.salesByStore, data.salesTotals.totalCents)
        }

        if (data.topProducts.isNotEmpty()) {
            blocks += ReportBlock.SectionTitle(context.getString(R.string.report_section_top_products))
            blocks += topProductsTable(request, data.topProducts.take(SUMMARY_TOP_PRODUCTS))
        }

        if (data.dailySales.isNotEmpty() && request.includeCharts) {
            blocks += ReportBlock.Chart(
                title = context.getString(R.string.chart_daily_sales),
                kind = ChartKind.LINE,
                points = data.dailySales.map {
                    ChartPoint(AppDateTime.formatShortDate(it.dayMillis), it.salesCents)
                }
            )
        }

        if (data.expensesByCategory.isNotEmpty() && request.includeCharts) {
            blocks += ReportBlock.Chart(
                title = context.getString(R.string.chart_expenses_by_category),
                kind = ChartKind.HORIZONTAL_BARS,
                points = data.expensesByCategory.take(MAX_CHART_POINTS).mapIndexed { index, item ->
                    ChartPoint(item.name, item.totalCents, colorArgb = Brand.chartColor(index))
                }
            )
        }
        return blocks
    }

    // ----------------------------- Balance general ---------------------------

    internal fun balanceSheet(request: ReportRequest, data: ReportData): List<ReportBlock> {
        val symbol = request.currencySymbol
        val blocks = mutableListOf<ReportBlock>()
        val sales = data.salesTotals
        val netProfit = ReportCalculations.netProfitCents(
            sales = sales,
            expensesCents = data.expenseTotals.totalCents,
            returnsCents = data.returnsTotals.totalCents
        )

        blocks += ReportBlock.KpiGrid(
            listOf(
                kpi(context.getString(R.string.kpi_total_revenue), Money.format(sales.totalCents, symbol), highlight = true),
                kpi(context.getString(R.string.kpi_operating_expenses), Money.format(data.expenseTotals.totalCents, symbol)),
                kpi(context.getString(R.string.kpi_returns), Money.format(data.returnsTotals.totalCents, symbol)),
                kpi(context.getString(R.string.kpi_net_result), Money.format(netProfit, symbol), highlight = true)
            )
        )

        blocks += ReportBlock.SectionTitle(context.getString(R.string.report_section_income_statement))

        fun statementRow(concept: String, amount: Long): List<Cell> = listOf(
            Cell.Text(concept),
            Cell.Money(amount),
            Cell.Empty
        )

        val rows = listOf(
            statementRow(context.getString(R.string.statement_gross_sales), sales.subtotalCents),
            statementRow(context.getString(R.string.statement_discounts), -sales.discountCents),
            statementRow(context.getString(R.string.statement_net_sales), sales.netBaseCents),
            statementRow(context.getString(R.string.statement_taxes_collected), sales.taxCents),
            statementRow(context.getString(R.string.statement_cost_of_goods), -sales.costCents),
            statementRow(context.getString(R.string.statement_gross_profit), sales.grossProfitCents),
            statementRow(context.getString(R.string.statement_operating_expenses), -data.expenseTotals.totalCents),
            statementRow(context.getString(R.string.statement_returns), -data.returnsTotals.totalCents),
            statementRow(context.getString(R.string.statement_net_profit), netProfit),
        )

        blocks += ReportBlock.Table(
            title = context.getString(R.string.report_table_income_statement),
            columns = listOf(
                ReportColumn(context.getString(R.string.column_concept), weight = 2.6f),
                ReportColumn(context.getString(R.string.column_amount), CellAlign.END, 1.2f),
                ReportColumn(context.getString(R.string.column_percent), CellAlign.END, 0.8f)
            ),
            rows = rows.mapIndexed { index, row ->
                val base = sales.netBaseCents.coerceAtLeast(1L)
                val raw = when (index) {
                    0 -> sales.subtotalCents
                    1 -> sales.discountCents
                    2 -> sales.netBaseCents
                    3 -> sales.taxCents
                    4 -> sales.costCents
                    5 -> sales.grossProfitCents
                    6 -> data.expenseTotals.totalCents
                    7 -> data.returnsTotals.totalCents
                    else -> netProfit
                }
                row.take(2) + Cell.Decimal(raw * 100.0 / base, 1, "%")
            },
            note = context.getString(R.string.report_note_percentages_of_net_sales)
        )

        if (data.expensesByCategory.isNotEmpty()) {
            blocks += ReportBlock.SectionTitle(context.getString(R.string.report_section_expense_breakdown))
            blocks += expenseCategoryTable(request, data.expensesByCategory, data.expenseTotals.totalCents)
        }

        if (request.includeCharts && data.dailySales.isNotEmpty()) {
            val series = ReportCalculations.buildDailySeries(
                range = request.range,
                sales = data.dailySales,
                expenses = data.dailyExpenses,
                returns = data.dailyReturns
            )
            blocks += ReportBlock.Chart(
                title = context.getString(R.string.chart_revenue_vs_expenses),
                kind = ChartKind.BARS,
                points = series.map { day ->
                    ChartPoint(
                        label = AppDateTime.formatShortDate(day.dayMillis),
                        value = day.salesCents,
                        secondaryValue = day.expensesCents
                    )
                }
            )
        }
        return blocks
    }

    // --------------------------- Ventas por tienda ---------------------------

    internal fun salesByStore(request: ReportRequest, data: ReportData): List<ReportBlock> {
        val symbol = request.currencySymbol
        val blocks = mutableListOf<ReportBlock>()
        val totals = data.salesTotals

        blocks += ReportBlock.KpiGrid(
            listOf(
                kpi(context.getString(R.string.kpi_total_sales), Money.format(totals.totalCents, symbol), highlight = true),
                kpi(context.getString(R.string.kpi_tickets), totals.tickets.toString()),
                kpi(context.getString(R.string.kpi_average_ticket), Money.format(totals.averageTicketCents, symbol)),
                kpi(context.getString(R.string.kpi_gross_margin), formatPercent(totals.grossMarginPercent))
            )
        )

        blocks += ReportBlock.SectionTitle(context.getString(R.string.report_section_sales_by_store))
        blocks += storeTable(request, data.salesByStore, totals.totalCents)

        if (data.paymentSplits.isNotEmpty()) {
            blocks += ReportBlock.SectionTitle(context.getString(R.string.report_section_payment_methods))
            blocks += paymentTable(request, data.paymentSplits, totals.totalCents)
        }

        if (request.includeCharts && data.salesByStore.isNotEmpty()) {
            blocks += ReportBlock.Chart(
                title = context.getString(R.string.chart_sales_by_store),
                kind = ChartKind.HORIZONTAL_BARS,
                points = data.salesByStore.mapIndexed { index, store ->
                    ChartPoint(store.storeName, store.totalCents, colorArgb = Brand.chartColor(index))
                }
            )
        }

        if (request.includeCharts && data.dailySales.isNotEmpty()) {
            blocks += ReportBlock.Chart(
                title = context.getString(R.string.chart_daily_sales),
                kind = ChartKind.LINE,
                points = ReportCalculations.buildDailySeries(request.range, data.dailySales).map {
                    ChartPoint(AppDateTime.formatShortDate(it.dayMillis), it.salesCents, it.tickets.toLong())
                }
            )
        }
        return blocks
    }

    // ------------------------- Productos mas vendidos ------------------------

    internal fun topProducts(request: ReportRequest, data: ReportData): List<ReportBlock> {
        val symbol = request.currencySymbol
        val blocks = mutableListOf<ReportBlock>()
        val products = data.topProducts

        val totalUnits = products.sumOf { it.units.toLong() }
        val totalRevenue = products.sumOf { it.totalCents }

        blocks += ReportBlock.KpiGrid(
            listOf(
                kpi(context.getString(R.string.kpi_products_analyzed), products.size.toString(), highlight = true),
                kpi(context.getString(R.string.kpi_units_sold), totalUnits.toString()),
                kpi(context.getString(R.string.kpi_revenue_top), Money.format(totalRevenue, symbol)),
                kpi(
                    context.getString(R.string.kpi_average_margin),
                    formatPercent(Money.marginPercent(products.sumOf { it.costCents }, totalRevenue))
                )
            )
        )

        blocks += ReportBlock.SectionTitle(context.getString(R.string.report_section_top_products))
        blocks += topProductsTable(request, products)

        if (request.includeCharts && products.isNotEmpty()) {
            blocks += ReportBlock.Chart(
                title = context.getString(R.string.chart_units_by_product),
                kind = ChartKind.HORIZONTAL_BARS,
                points = products.take(MAX_CHART_POINTS).mapIndexed { index, product ->
                    ChartPoint(product.name, product.units.toLong(), colorArgb = Brand.chartColor(index))
                },
                note = context.getString(R.string.chart_note_units)
            )
        }
        return blocks
    }

    // ------------------------------- Utilidades ------------------------------

    internal fun kpi(
        label: String,
        value: String,
        detail: String = "",
        variation: Double? = null,
        highlight: Boolean = false,
    ): Kpi = Kpi(
        label = label,
        value = value,
        detail = detail,
        variationPercent = variation,
        highlight = highlight
    )

    /** Delega en el formateo comun para que pantalla e informe coincidan. */
    internal fun formatPercent(value: Double): String =
        com.gis.supermercados.core.common.formatPercent(value)

    internal fun sharePercent(part: Long, total: Long): Double =
        if (total == 0L) 0.0 else part * 100.0 / total.toDouble()

    internal fun storeTable(
        request: ReportRequest,
        stores: List<com.gis.supermercados.domain.model.StoreSalesSummary>,
        grandTotal: Long,
    ): ReportBlock.Table {
        val rows = stores.map { store ->
            listOf(
                Cell.Text(store.storeName),
                Cell.Count(store.tickets.toLong()),
                Cell.Money(store.subtotalCents),
                Cell.Money(store.discountCents),
                Cell.Money(store.taxCents),
                Cell.Money(store.totalCents),
                Cell.Money(store.costCents),
                Cell.Money(store.profitCents),
                Cell.Decimal(Money.marginPercent(store.costCents, store.subtotalCents - store.discountCents), 1, "%"),
                Cell.Decimal(sharePercent(store.totalCents, grandTotal), 1, "%")
            )
        }
        val totals = stores.fold(
            SalesTotals()
        ) { accumulator, store ->
            accumulator.copy(
                subtotalCents = accumulator.subtotalCents + store.subtotalCents,
                discountCents = accumulator.discountCents + store.discountCents,
                taxCents = accumulator.taxCents + store.taxCents,
                totalCents = accumulator.totalCents + store.totalCents,
                costCents = accumulator.costCents + store.costCents,
                tickets = accumulator.tickets + store.tickets
            )
        }
        return ReportBlock.Table(
            title = context.getString(R.string.report_table_sales_by_store),
            columns = listOf(
                ReportColumn(context.getString(R.string.column_store), weight = 1.9f),
                ReportColumn(context.getString(R.string.column_tickets), CellAlign.END, 0.8f),
                ReportColumn(context.getString(R.string.column_subtotal), CellAlign.END, 1.1f),
                ReportColumn(context.getString(R.string.column_discount), CellAlign.END, 0.9f),
                ReportColumn(context.getString(R.string.column_tax), CellAlign.END, 0.9f),
                ReportColumn(context.getString(R.string.column_total), CellAlign.END, 1.1f),
                ReportColumn(context.getString(R.string.column_cost), CellAlign.END, 1.0f),
                ReportColumn(context.getString(R.string.column_profit), CellAlign.END, 1.0f),
                ReportColumn(context.getString(R.string.column_margin), CellAlign.END, 0.8f),
                ReportColumn(context.getString(R.string.column_share), CellAlign.END, 0.8f)
            ),
            rows = rows,
            totalRow = listOf(
                Cell.Text(context.getString(R.string.row_total)),
                Cell.Count(totals.tickets.toLong()),
                Cell.Money(totals.subtotalCents),
                Cell.Money(totals.discountCents),
                Cell.Money(totals.taxCents),
                Cell.Money(totals.totalCents),
                Cell.Money(totals.costCents),
                Cell.Money(totals.grossProfitCents),
                Cell.Decimal(totals.grossMarginPercent, 1, "%"),
                Cell.Decimal(if (grandTotal == 0L) 0.0 else 100.0, 1, "%")
            ),
            note = context.getString(R.string.report_note_void_sales_excluded)
        )
    }

    internal fun topProductsTable(
        request: ReportRequest,
        products: List<com.gis.supermercados.domain.model.TopProduct>,
    ): ReportBlock.Table {
        val rows = products.mapIndexed { index, product ->
            listOf(
                Cell.Count(index + 1L),
                Cell.Text(product.name),
                Cell.Text(product.sku),
                Cell.Text(product.categoryName),
                Cell.Count(product.units.toLong()),
                Cell.Money(product.totalCents),
                Cell.Money(product.costCents),
                Cell.Money(product.profitCents),
                Cell.Decimal(product.marginPercent, 1, "%")
            )
        }
        val totalUnits = products.sumOf { it.units.toLong() }
        val totalRevenue = products.sumOf { it.totalCents }
        val totalCost = products.sumOf { it.costCents }
        return ReportBlock.Table(
            title = context.getString(R.string.report_table_top_products),
            columns = listOf(
                ReportColumn("#", CellAlign.CENTER, 0.4f),
                ReportColumn(context.getString(R.string.column_product), weight = 2.2f),
                ReportColumn(context.getString(R.string.column_sku), CellAlign.CENTER, 1.0f),
                ReportColumn(context.getString(R.string.column_category), weight = 1.4f),
                ReportColumn(context.getString(R.string.column_units), CellAlign.END, 0.7f),
                ReportColumn(context.getString(R.string.column_revenue), CellAlign.END, 1.1f),
                ReportColumn(context.getString(R.string.column_cost), CellAlign.END, 1.0f),
                ReportColumn(context.getString(R.string.column_profit), CellAlign.END, 1.0f),
                ReportColumn(context.getString(R.string.column_margin), CellAlign.END, 0.8f)
            ),
            rows = rows,
            totalRow = listOf(
                Cell.Empty,
                Cell.Text(context.getString(R.string.row_total)),
                Cell.Empty,
                Cell.Empty,
                Cell.Count(totalUnits),
                Cell.Money(totalRevenue),
                Cell.Money(totalCost),
                Cell.Money(totalRevenue - totalCost),
                Cell.Decimal(Money.marginPercent(totalCost, totalRevenue), 1, "%")
            )
        )
    }

    internal fun paymentTable(
        request: ReportRequest,
        splits: List<com.gis.supermercados.core.reporting.model.PaymentSplit>,
        grandTotal: Long,
    ): ReportBlock.Table {
        val rows = splits.map { split ->
            listOf(
                Cell.Text(labels.of(split.method)),
                Cell.Count(split.tickets.toLong()),
                Cell.Money(split.totalCents),
                Cell.Decimal(sharePercent(split.totalCents, grandTotal), 1, "%")
            )
        }
        return ReportBlock.Table(
            title = context.getString(R.string.report_table_payment_methods),
            columns = listOf(
                ReportColumn(context.getString(R.string.column_payment_method), weight = 2f),
                ReportColumn(context.getString(R.string.column_tickets), CellAlign.END, 0.9f),
                ReportColumn(context.getString(R.string.column_amount), CellAlign.END, 1.2f),
                ReportColumn(context.getString(R.string.column_share), CellAlign.END, 0.9f)
            ),
            rows = rows,
            totalRow = listOf(
                Cell.Text(context.getString(R.string.row_total)),
                Cell.Count(splits.sumOf { it.tickets.toLong() }),
                Cell.Money(splits.sumOf { it.totalCents }),
                Cell.Decimal(if (grandTotal == 0L) 0.0 else 100.0, 1, "%")
            )
        )
    }

    internal fun expenseCategoryTable(
        request: ReportRequest,
        categories: List<com.gis.supermercados.domain.model.NamedTotal>,
        grandTotal: Long,
    ): ReportBlock.Table {
        val rows = categories.map { category ->
            listOf(
                Cell.Text(category.name),
                Cell.Count(category.quantity.toLong()),
                Cell.Money(category.totalCents),
                Cell.Decimal(sharePercent(category.totalCents, grandTotal), 1, "%")
            )
        }
        return ReportBlock.Table(
            title = context.getString(R.string.report_table_expenses_by_category),
            columns = listOf(
                ReportColumn(context.getString(R.string.column_category), weight = 2.2f),
                ReportColumn(context.getString(R.string.column_records), CellAlign.END, 0.9f),
                ReportColumn(context.getString(R.string.column_amount), CellAlign.END, 1.3f),
                ReportColumn(context.getString(R.string.column_share), CellAlign.END, 0.9f)
            ),
            rows = rows,
            totalRow = listOf(
                Cell.Text(context.getString(R.string.row_total)),
                Cell.Count(categories.sumOf { it.quantity.toLong() }),
                Cell.Money(categories.sumOf { it.totalCents }),
                Cell.Decimal(if (grandTotal == 0L) 0.0 else 100.0, 1, "%")
            )
        )
    }

}

/*
 * Constantes de maquetacion de informes.
 * Se declaran a nivel de paquete (no dentro del companion) para que tambien las
 * funciones de extension de ReportBuilderExtra.kt puedan usarlas.
 */
internal const val MAX_CHART_POINTS = 14
internal const val SUMMARY_TOP_PRODUCTS = 8
internal const val MAX_TABLE_ROWS = 400
