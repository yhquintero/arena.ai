package com.gis.supermercados.data.repository

import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.common.Labels
import com.gis.supermercados.core.common.runCatchingApp
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.core.reporting.ReportBuilder
import com.gis.supermercados.core.reporting.ReportExporter
import com.gis.supermercados.core.reporting.model.DateRangeModel
import com.gis.supermercados.core.reporting.model.ExportFormat
import com.gis.supermercados.core.reporting.model.PeriodComparisonData
import com.gis.supermercados.core.reporting.model.ReportData
import com.gis.supermercados.core.reporting.model.ReportDocument
import com.gis.supermercados.core.reporting.model.ReportRequest
import com.gis.supermercados.core.reporting.model.ReportType
import com.gis.supermercados.core.reporting.model.SalesTotals
import com.gis.supermercados.data.local.GisDatabase
import com.gis.supermercados.data.local.entity.ReportLogEntity
import com.gis.supermercados.data.mapper.toDailyExpense
import com.gis.supermercados.data.mapper.toDailyReturns
import com.gis.supermercados.data.mapper.toDailyTotal
import com.gis.supermercados.data.mapper.toDomain
import com.gis.supermercados.data.mapper.toExpenseTotals
import com.gis.supermercados.data.mapper.toNamedTotals
import com.gis.supermercados.data.mapper.toReturnsTotals
import com.gis.supermercados.data.mapper.toSalesTotals
import com.gis.supermercados.di.IoDispatcher
import com.gis.supermercados.domain.model.DailyTotal
import com.gis.supermercados.domain.model.DashboardStats
import com.gis.supermercados.domain.model.ExportedFile
import com.gis.supermercados.domain.repository.ReportRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacion del motor de informes.
 *
 * Responsabilidades:
 *  1. Consultar los agregados necesarios segun el tipo de informe (solo los que
 *     se necesitan: ni una consulta de mas).
 *  2. Pedir al [ReportBuilder] el documento intermedio.
 *  3. Exportarlo con [ReportExporter] y dejar constancia en el historial.
 *  4. Alimentar las metricas del panel de inicio.
 */
@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val database: GisDatabase,
    private val builder: ReportBuilder,
    private val exporter: ReportExporter,
    private val labels: Labels,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ReportRepository {

    private val queries get() = database.reportQueriesDao()

    // ------------------------------ Panel de inicio ------------------------------

    override suspend fun dashboardStats(referenceMillis: Long): AppResult<DashboardStats> =
        runCatchingApp(TAG, UiText.of(R.string.dashboard_error_load)) {
            withContext(ioDispatcher) {
                val todayStart = AppDateTime.startOfDay(referenceMillis)
                val todayEnd = AppDateTime.endOfDay(referenceMillis)
                val yesterdayStart = AppDateTime.startOfDay(AppDateTime.minusDays(referenceMillis, 1))
                val yesterdayEnd = AppDateTime.endOfDay(AppDateTime.minusDays(referenceMillis, 1))
                val weekStart = AppDateTime.startOfWeek(referenceMillis)
                val weekEnd = AppDateTime.endOfWeek(referenceMillis)
                val monthStart = AppDateTime.startOfMonth(referenceMillis)
                val monthEnd = AppDateTime.endOfMonth(referenceMillis)

                val today = queries.salesTotals(todayStart, todayEnd, null)
                val yesterday = queries.salesTotals(yesterdayStart, yesterdayEnd, null)
                val week = queries.salesTotals(weekStart, weekEnd, null)
                val month = queries.salesTotals(monthStart, monthEnd, null)
                val monthExpenses = queries.expenseTotals(monthStart, monthEnd, null)
                val monthReturns = queries.returnsTotals(monthStart, monthEnd, null)

                val seriesStart = AppDateTime.startOfDay(AppDateTime.minusDays(referenceMillis, DASHBOARD_SERIES_DAYS - 1))
                val daily = queries.dailySales(seriesStart, todayEnd, null, timeZoneOffset()).map { it.toDailyTotal() }
                val dailyExpenses = queries.dailyExpenses(seriesStart, todayEnd, null, timeZoneOffset()).map { it.toDailyExpense() }

                val inventory = database.stockDao().inventoryValue(null)

                DashboardStats(
                    todayTotalCents = today.totalCents,
                    todayTickets = today.tickets,
                    todayProfitCents = today.toSalesTotals().grossProfitCents,
                    yesterdayTotalCents = yesterday.totalCents,
                    weekTotalCents = week.totalCents,
                    monthTotalCents = month.totalCents,
                    monthTickets = month.tickets,
                    monthExpensesCents = monthExpenses.totalCents,
                    monthProfitCents = month.toSalesTotals().grossProfitCents - monthExpenses.totalCents,
                    monthReturnsCents = monthReturns.totalCents,
                    lowStockCount = database.stockDao().countLowStock(),
                    outOfStockCount = database.stockDao().countOutOfStock(),
                    activeStores = database.storeDao().countActive(),
                    activeProducts = database.productDao().countActive(),
                    activeCustomers = database.customerDao().countActive(),
                    inventoryValueCents = inventory.costValueCents,
                    inventoryRetailCents = inventory.retailValueCents,
                    dailySeries = mergeSeries(seriesStart, todayEnd, daily, dailyExpenses),
                    salesByStore = queries.salesByStore(monthStart, monthEnd, null).map { it.toDomain() },
                    topProducts = queries.topProducts(monthStart, monthEnd, null, DASHBOARD_TOP_PRODUCTS).map { it.toDomain() },
                    expensesByCategory = queries.expensesByCategory(monthStart, monthEnd, null)
                        .toNamedTotals { labels.of(it.category) }
                )
            }
        }

    // --------------------------------- Informes ---------------------------------

    override suspend fun buildReport(request: ReportRequest): AppResult<ReportDocument> =
        runCatchingApp(TAG, UiText.of(R.string.report_error_build)) {
            withContext(ioDispatcher) {
                AppLogger.i(TAG, "Generando informe ${request.type} (${request.range.label})")
                val data = collect(request)
                builder.build(request, data)
            }
        }

    override suspend fun exportReport(
        document: ReportDocument,
        request: ReportRequest,
        format: ExportFormat,
    ): AppResult<ExportedFile> = withContext(ioDispatcher) {
        val result = exporter.export(document, request, format)
        if (result is AppResult.Success) {
            runCatching {
                database.systemLogDao().insertReportLog(
                    ReportLogEntity(
                        reportType = request.type.name,
                        periodType = request.periodType.name,
                        startMillis = request.range.startMillis,
                        endMillis = request.range.endMillis,
                        format = format.name,
                        fileName = result.data.fileName,
                        filePath = result.data.filePath,
                        sizeBytes = result.data.sizeBytes,
                        storeId = request.storeId,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }.onFailure { AppLogger.w(TAG, "No se pudo registrar el informe en el historial: ${it.message}") }
        }
        result
    }

    override suspend fun copyToUri(file: ExportedFile, destinationUri: String): AppResult<ExportedFile> =
        exporter.copyToUri(file, destinationUri)

    // --------------------------- Recoleccion de datos ---------------------------

    private suspend fun collect(request: ReportRequest): ReportData {
        val range = request.range
        val start = range.startMillis
        val end = range.endMillis
        val storeId = request.storeId
        val tz = timeZoneOffset()

        return when (request.type) {
            ReportType.RESUMEN_EJECUTIVO -> ReportData(
                salesTotals = salesTotals(start, end, storeId),
                salesByStore = queries.salesByStore(start, end, storeId).map { it.toDomain() },
                topProducts = queries.topProducts(start, end, storeId, SUMMARY_TOP).map { it.toDomain() },
                dailySales = queries.dailySales(start, end, storeId, tz).map { it.toDailyTotal() },
                expenseTotals = queries.expenseTotals(start, end, storeId).toExpenseTotals(),
                expensesByCategory = queries.expensesByCategory(start, end, storeId)
                    .toNamedTotals { labels.of(it.category) },
                returnsTotals = queries.returnsTotals(start, end, storeId).toReturnsTotals()
            )

            ReportType.BALANCE_GENERAL -> ReportData(
                salesTotals = salesTotals(start, end, storeId),
                expenseTotals = queries.expenseTotals(start, end, storeId).toExpenseTotals(),
                expensesByCategory = queries.expensesByCategory(start, end, storeId)
                    .toNamedTotals { labels.of(it.category) },
                returnsTotals = queries.returnsTotals(start, end, storeId).toReturnsTotals(),
                dailySales = queries.dailySales(start, end, storeId, tz).map { it.toDailyTotal() },
                dailyExpenses = queries.dailyExpenses(start, end, storeId, tz).map { it.toDailyExpense() },
                dailyReturns = queries.dailyReturns(start, end, storeId, tz).map { it.toDailyReturns() }
            )

            ReportType.FLUJO_CAJA -> ReportData(
                salesTotals = salesTotals(start, end, storeId),
                dailySales = queries.dailySales(start, end, storeId, tz).map { it.toDailyTotal() },
                dailyExpenses = queries.dailyExpenses(start, end, storeId, tz).map { it.toDailyExpense() },
                dailyReturns = queries.dailyReturns(start, end, storeId, tz).map { it.toDailyReturns() },
                paymentSplits = queries.salesByPaymentMethod(start, end, storeId).map { it.toDomain() }
            )

            ReportType.VENTAS_POR_TIENDA -> ReportData(
                salesTotals = salesTotals(start, end, storeId),
                salesByStore = queries.salesByStore(start, end, storeId).map { it.toDomain() },
                paymentSplits = queries.salesByPaymentMethod(start, end, storeId).map { it.toDomain() },
                dailySales = queries.dailySales(start, end, storeId, tz).map { it.toDailyTotal() }
            )

            ReportType.PRODUCTOS_MAS_VENDIDOS -> ReportData(
                salesTotals = salesTotals(start, end, storeId),
                topProducts = queries.topProducts(start, end, storeId, request.topLimit).map { it.toDomain() }
            )

            ReportType.RENTABILIDAD_CATEGORIA -> ReportData(
                salesTotals = salesTotals(start, end, storeId),
                categoryProfitability = queries.categoryProfitability(start, end, storeId).map { it.toDomain() }
            )

            ReportType.ANALISIS_GASTOS -> ReportData(
                salesTotals = salesTotals(start, end, storeId),
                expenseTotals = queries.expenseTotals(start, end, storeId).toExpenseTotals(),
                expensesByCategory = queries.expensesByCategory(start, end, storeId)
                    .toNamedTotals { labels.of(it.category) },
                expensesByStore = queries.expensesByStore(start, end).map { it.toDomain() },
                dailyExpenses = queries.dailyExpenses(start, end, storeId, tz).map { it.toDailyExpense() },
                dailySales = queries.dailySales(start, end, storeId, tz).map { it.toDailyTotal() }
            )

            ReportType.COMPARATIVA_PERIODOS -> {
                val previous = range.previous()
                ReportData(
                    salesTotals = salesTotals(start, end, storeId),
                    expenseTotals = queries.expenseTotals(start, end, storeId).toExpenseTotals(),
                    returnsTotals = queries.returnsTotals(start, end, storeId).toReturnsTotals(),
                    comparison = PeriodComparisonData(
                        current = salesTotals(start, end, storeId),
                        previous = salesTotals(previous.startMillis, previous.endMillis, storeId),
                        currentExpensesCents = queries.expenseTotals(start, end, storeId).totalCents,
                        previousExpensesCents = queries.expenseTotals(
                            previous.startMillis, previous.endMillis, storeId
                        ).totalCents,
                        currentReturnsCents = queries.returnsTotals(start, end, storeId).totalCents,
                        previousReturnsCents = queries.returnsTotals(
                            previous.startMillis, previous.endMillis, storeId
                        ).totalCents
                    ),
                    dailySales = queries.dailySales(start, end, storeId, tz).map { it.toDailyTotal() }
                )
            }

            ReportType.PROYECCIONES -> ReportData(
                salesTotals = salesTotals(start, end, storeId),
                dailySales = queries.dailySales(start, end, storeId, tz).map { it.toDailyTotal() },
                expenseTotals = queries.expenseTotals(start, end, storeId).toExpenseTotals()
            )

            ReportType.INVENTARIO_VALORADO -> {
                val stock = database.stockDao().getStockForReport(storeId)
                ReportData(
                    inventoryValue = database.stockDao().inventoryValue(storeId).toDomain(),
                    stockDetail = stock.map { it.toDomain() },
                    movements = database.inventoryMovementDao()
                        .getMovementsBetween(start, end, storeId)
                        .map { it.toDomain() }
                        .take(MAX_MOVEMENTS)
                )
            }
        }
    }

    private suspend fun salesTotals(start: Long, end: Long, storeId: Long?): SalesTotals =
        queries.salesTotals(start, end, storeId).toSalesTotals()

    /**
     * Fusiona ventas y gastos en una serie diaria COMPLETA (sin huecos):
     * los dias sin movimiento aparecen en cero para que la grafica sea honesta.
     */
    private fun mergeSeries(
        startMillis: Long,
        endMillis: Long,
        sales: List<DailyTotal>,
        expenses: List<DailyTotal>,
    ): List<DailyTotal> {
        val salesByDay = sales.associateBy { it.dayMillis }
        val expensesByDay = expenses.associateBy { it.dayMillis }
        val days = mutableListOf<DailyTotal>()
        var cursor = AppDateTime.startOfDay(startMillis)
        val last = AppDateTime.startOfDay(endMillis)
        var guard = 0
        while (cursor <= last && guard < MAX_SERIES_DAYS) {
            days += DailyTotal(
                dayMillis = cursor,
                salesCents = salesByDay[cursor]?.salesCents ?: 0L,
                expensesCents = expensesByDay[cursor]?.expensesCents ?: 0L,
                tickets = salesByDay[cursor]?.tickets ?: 0,
                costCents = salesByDay[cursor]?.costCents ?: 0L
            )
            cursor = AppDateTime.plusDays(cursor, 1)
            guard++
        }
        return days
    }

    private fun timeZoneOffset(): Long =
        ZoneId.systemDefault().rules.getOffset(Instant.now()).totalSeconds * MILLIS_PER_SECOND

    private companion object {
        const val TAG = "ReportRepository"
        const val DASHBOARD_TOP_PRODUCTS = 5
        const val DASHBOARD_SERIES_DAYS = 14
        const val MAX_SERIES_DAYS = 400
        const val SUMMARY_TOP = 8
        const val MAX_MOVEMENTS = 300
        const val MILLIS_PER_SECOND = 1_000L
    }
}
