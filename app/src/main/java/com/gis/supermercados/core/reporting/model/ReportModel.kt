package com.gis.supermercados.core.reporting.model

/**
 * Modelo intermedio de reporte.
 *
 * El motor de reportes construye UN [ReportDocument] y cuatro renderizadores lo
 * consumen: PDF, Excel (XLSX), CSV y la vista previa en pantalla. Asi se garantiza
 * que el documento impreso y lo que ve el usuario son exactamente los mismos datos.
 */

/** Tipos de informe economico disponibles. */
enum class ReportType {
    RESUMEN_EJECUTIVO,
    BALANCE_GENERAL,
    FLUJO_CAJA,
    VENTAS_POR_TIENDA,
    PRODUCTOS_MAS_VENDIDOS,
    RENTABILIDAD_CATEGORIA,
    ANALISIS_GASTOS,
    COMPARATIVA_PERIODOS,
    PROYECCIONES,
    INVENTARIO_VALORADO,
}

/** Periodicidad del informe. */
enum class PeriodType {
    DIARIO,
    SEMANAL,
    MENSUAL,
    TRIMESTRAL,
    SEMESTRAL,
    ANUAL,
    PERSONALIZADO,
}

/** Formatos de exportacion soportados. */
enum class ExportFormat(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    CSV("csv", "text/csv"),
}

/** Tipo de grafica para un bloque [ReportBlock.Chart]. */
enum class ChartKind { BARS, HORIZONTAL_BARS, LINE, DONUT }

/** Alineacion de una columna de tabla. */
enum class CellAlign { START, CENTER, END }

/** Parametros con los que se genera un informe. */
data class ReportRequest(
    val type: ReportType,
    val periodType: PeriodType,
    val range: DateRangeModel,
    val storeId: Long? = null,
    val storeName: String = "",
    val topLimit: Int = 20,
    val includeCharts: Boolean = true,
    val companyName: String = "",
    val companyTaxId: String = "",
    val companyAddress: String = "",
    val currencySymbol: String = "$",
    val generatedBy: String = "",
    val generatedAt: Long = System.currentTimeMillis(),
)

/** Rango de fechas del informe (etiqueta ya localizada). */
data class DateRangeModel(
    val startMillis: Long,
    val endMillis: Long,
    val label: String,
) {
    val days: Long get() = ((endMillis - startMillis) / MILLIS_PER_DAY) + 1

    /** Rango anterior equivalente, para informes comparativos. */
    fun previous(): DateRangeModel {
        val length = endMillis - startMillis + 1L
        return DateRangeModel(
            startMillis = startMillis - length,
            endMillis = startMillis - 1L,
            label = ""
        )
    }

    companion object {
        private const val MILLIS_PER_DAY = 86_400_000L
    }
}

/** Valor de una celda: tipado para que Excel reciba numeros reales, no texto. */
sealed interface Cell {

    data class Text(val value: String, val align: CellAlign = CellAlign.START) : Cell

    data class Money(val cents: Long, val align: CellAlign = CellAlign.END) : Cell

    data class Count(val value: Long, val align: CellAlign = CellAlign.END) : Cell

    data class Decimal(
        val value: Double,
        val decimals: Int = 1,
        val suffix: String = "",
        val align: CellAlign = CellAlign.END,
    ) : Cell

    data class DateCell(val millis: Long, val align: CellAlign = CellAlign.CENTER) : Cell

    data object Empty : Cell

    /** Representacion textual (usada por PDF, CSV y vista previa). */
    fun display(currencySymbol: String): String = when (this) {
        is Text -> value
        is Money -> com.gis.supermercados.core.common.Money.format(cents, currencySymbol)
        is Count -> INT_FORMAT.get().format(value)
        is Decimal -> (DECIMAL_FORMATS.get()?.get(decimals.coerceIn(0, 4))?.format(value)
            ?: value.toString()) + suffix
        is DateCell -> com.gis.supermercados.core.common.AppDateTime.formatDate(millis)
        Empty -> ""
    }

    /** Valor numerico para Excel (null cuando la celda es texto). */
    fun numericValue(): Double? = when (this) {
        is Money -> cents / 100.0
        is Count -> value.toDouble()
        is Decimal -> value
        else -> null
    }

    companion object {
        /**
         * DecimalFormat NO es thread-safe: se usa ThreadLocal para poder generar
         * informes en paralelo sin corromper los formatos.
         */
        private val INT_FORMAT = ThreadLocal.withInitial {
            java.text.DecimalFormat("#,##0", symbols())
        }
        private val DECIMAL_FORMATS = ThreadLocal.withInitial {
            (0..4).associateWith { decimals ->
                java.text.DecimalFormat("#,##0" + ".".takeIf { decimals > 0 }.orEmpty() + "0".repeat(decimals), symbols())
            }
        }

        private fun symbols() = java.text.DecimalFormatSymbols(java.util.Locale("es")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
    }
}

/** Alineacion efectiva de una celda (fuera de la jerarquia para evitar recursividad). */
fun Cell.alignment(): CellAlign = when (this) {
    is Cell.Text -> align
    is Cell.Money -> align
    is Cell.Count -> align
    is Cell.Decimal -> align
    is Cell.DateCell -> align
    Cell.Empty -> CellAlign.START
}

/** Columna de una tabla del informe. */
data class ReportColumn(
    val header: String,
    val align: CellAlign = CellAlign.START,
    /** Peso relativo para el ancho en PDF (1.0 = normal). */
    val weight: Float = 1f,
)

/** Bloques de contenido que componen el informe. */
sealed interface ReportBlock {

    data class SectionTitle(val text: String) : ReportBlock

    data class Paragraph(val text: String, val bold: Boolean = false) : ReportBlock

    /** Tarjetas de indicadores clave (KPI). */
    data class KpiGrid(val items: List<Kpi>) : ReportBlock

    data class Table(
        val title: String,
        val columns: List<ReportColumn>,
        val rows: List<List<Cell>>,
        val totalRow: List<Cell>? = null,
        val note: String? = null,
    ) : ReportBlock

    data class Chart(
        val title: String,
        val kind: ChartKind,
        val points: List<ChartPoint>,
        val note: String? = null,
    ) : ReportBlock

    data object PageBreak : ReportBlock
}

/** Indicador clave con variacion opcional respecto al periodo anterior. */
data class Kpi(
    val label: String,
    val value: String,
    val detail: String = "",
    val variationPercent: Double? = null,
    val highlight: Boolean = false,
) {
    val isPositiveVariation: Boolean get() = (variationPercent ?: 0.0) >= 0.0
}

/** Punto de una grafica (etiqueta + valor en centavos o unidades). */
data class ChartPoint(
    val label: String,
    val value: Long,
    val secondaryValue: Long = 0L,
    val colorArgb: Long? = null,
)

/** Documento completo listo para renderizar o previsualizar. */
data class ReportDocument(
    val title: String,
    val subtitle: String,
    val companyName: String,
    val companyTaxId: String = "",
    val companyAddress: String = "",
    val storeLabel: String,
    val periodLabel: String,
    val generatedAtLabel: String,
    val generatedBy: String,
    val currencySymbol: String,
    val blocks: List<ReportBlock>,
    val footerNote: String = "",
    val type: ReportType,
) {
    val tables: List<ReportBlock.Table>
        get() = blocks.filterIsInstance<ReportBlock.Table>()
}
