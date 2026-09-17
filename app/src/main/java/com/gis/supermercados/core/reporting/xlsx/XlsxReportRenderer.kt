package com.gis.supermercados.core.reporting.xlsx

import com.gis.supermercados.core.designsystem.Brand
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.core.reporting.model.Cell
import com.gis.supermercados.core.reporting.model.ReportBlock
import com.gis.supermercados.core.reporting.model.ReportColumn
import com.gis.supermercados.core.reporting.model.ReportDocument
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Convierte un [ReportDocument] en un libro Excel profesional de varias hojas:
 *
 *  1. "Resumen"  -> identificacion del informe, KPIs y tablas principales.
 *  2. Una hoja por tabla -> datos detallados con autofiltro y panel congelado.
 *  3. "Graficos" -> series con barras de datos nativas de Excel.
 *
 * Los importes se escriben como NUMEROS reales (no texto), de modo que el usuario
 * puede sumar, filtrar y crear tablas dinamicas sobre el archivo exportado.
 */
@Singleton
class XlsxReportRenderer @Inject constructor() {

    private val tag = "XlsxReportRenderer"

    fun render(document: ReportDocument): ByteArray {
        val writer = XlsxWriter(
            currencySymbol = document.currencySymbol,
            documentTitle = document.title,
            companyName = document.companyName,
            author = document.companyName.ifBlank { "GIS Supermercados" }
        )

        buildSummarySheet(writer, document)
        buildDetailSheets(writer, document)
        if (document.blocks.any { it is ReportBlock.Chart }) {
            buildChartSheet(writer, document)
        }

        val bytes = writer.build()
        AppLogger.i(tag, "Excel generado: ${document.title} (${writer.sheetNames.size} hojas, ${bytes.size} bytes)")
        return bytes
    }

    // ------------------------------- Resumen -------------------------------

    private fun buildSummarySheet(writer: XlsxWriter, document: ReportDocument) {
        val sheet = writer.addSheet("Resumen")
        sheet.setWidths(34, 24, 24, 24, 22, 20)
        val span = 6

        sheet.addText(document.companyName.ifBlank { "Gestión Integral de Supermercados" }, XlsStyle.TITLE, span, 26.0)
        sheet.addText(document.title, XlsStyle.SECTION, span, 20.0)
        sheet.addText(document.subtitle, XlsStyle.SUBTITLE, span)
        sheet.addSpacer()

        metaRow(sheet, "Sucursal", document.storeLabel, span)
        metaRow(sheet, "Periodo", document.periodLabel, span)
        metaRow(sheet, "Generado", document.generatedAtLabel, span)
        metaRow(sheet, "Responsable", document.generatedBy, span)
        metaRow(sheet, "Moneda", document.currencySymbol, span)
        sheet.addSpacer(12.0)

        val kpiBlocks = document.blocks.filterIsInstance<ReportBlock.KpiGrid>()
        if (kpiBlocks.isNotEmpty()) {
            sheet.addText("Indicadores clave", XlsStyle.SECTION, span, 20.0)
            val header = sheet.addRow(
                listOf(
                    XlsxCell.Str("Indicador", XlsStyle.TABLE_HEADER),
                    XlsxCell.Str("Valor", XlsStyle.TABLE_HEADER),
                    XlsxCell.Str("Detalle", XlsStyle.TABLE_HEADER),
                    XlsxCell.Str("Variación", XlsStyle.TABLE_HEADER),
                    XlsxCell.EmptyCell(XlsStyle.TABLE_HEADER),
                    XlsxCell.EmptyCell(XlsStyle.TABLE_HEADER),
                ),
                26.0
            )
            val firstKpiRow = header + 1
            kpiBlocks.forEach { block ->
                block.items.forEach { kpi ->
                    val variation = kpi.variationPercent?.let { XlsxCell.Percent(it / 100.0) }
                        ?: XlsxCell.EmptyCell(XlsStyle.TEXT)
                    sheet.addRow(
                        listOf(
                            XlsxCell.Str(kpi.label, XlsStyle.TEXT_BOLD),
                            XlsxCell.Str(kpi.value, XlsStyle.META_VALUE),
                            XlsxCell.Str(kpi.detail, XlsStyle.TEXT),
                            variation,
                            XlsxCell.EmptyCell(XlsStyle.TEXT),
                            XlsxCell.EmptyCell(XlsStyle.TEXT),
                        )
                    )
                }
            }
            sheet.setAutoFilter(header, sheet.rowCount, 4)
            sheet.freezeAt(firstKpiRow - 1)
            sheet.addSpacer(12.0)
        }

        // Tablas principales dentro del resumen (maximo 2 para no duplicar todo)
        document.tables.take(MAX_TABLES_IN_SUMMARY).forEach { table ->
            writeTable(sheet, table, document, withFilter = false, span = span)
            sheet.addSpacer(10.0)
        }

        sheet.addText(document.footerNote, XlsStyle.NOTE, span)
    }

    private fun metaRow(sheet: XlsxSheet, label: String, value: String, span: Int) {
        val cells = mutableListOf<XlsxCell>(
            XlsxCell.Str(label, XlsStyle.META_LABEL),
            XlsxCell.Str(value.ifBlank { "-" }, XlsStyle.META_VALUE)
        )
        repeat((span - 2).coerceAtLeast(0)) { cells.add(XlsxCell.EmptyCell(XlsStyle.DEFAULT)) }
        sheet.addRow(cells)
    }

    // ------------------------------ Detalle --------------------------------

    private fun buildDetailSheets(writer: XlsxWriter, document: ReportDocument) {
        document.tables.forEach { table ->
            val sheet = writer.addSheet(table.title.ifBlank { "Detalle" })
            sheet.setWidths(*computeWidths(table.columns, table.rows, document.currencySymbol).toIntArray())

            sheet.addText(document.companyName.ifBlank { "GIS Supermercados" }, XlsStyle.SUBTITLE, table.columns.size.coerceAtLeast(2))
            sheet.addText(table.title.ifBlank { document.title }, XlsStyle.TITLE, table.columns.size.coerceAtLeast(2), 24.0)
            sheet.addText("${document.periodLabel} · ${document.storeLabel}", XlsStyle.SUBTITLE, table.columns.size.coerceAtLeast(2))
            sheet.addSpacer()

            val headerRow = writeTable(sheet, table, document, withFilter = true, span = table.columns.size)
            sheet.freezeAt(headerRow)
            sheet.addSpacer()
            sheet.addText(document.footerNote, XlsStyle.NOTE, table.columns.size.coerceAtLeast(2))
        }
    }

    /** Devuelve el numero de fila de la cabecera (para congelar el panel). */
    private fun writeTable(
        sheet: XlsxSheet,
        table: ReportBlock.Table,
        document: ReportDocument,
        withFilter: Boolean,
        span: Int,
    ): Int {
        val columns = table.columns.size.coerceAtLeast(1)
        if (table.title.isNotBlank() && !withFilter) {
            sheet.addText(table.title, XlsStyle.SECTION, span.coerceAtLeast(columns), 20.0)
        }

        val headerRow = sheet.addRow(
            (0 until columns).map { index ->
                XlsxCell.Str(table.columns.getOrNull(index)?.header.orEmpty(), XlsStyle.TABLE_HEADER)
            },
            28.0
        )

        table.rows.forEach { row ->
            sheet.addRow((0 until columns).map { index -> toXlsx(row.getOrNull(index), document, total = false) })
        }

        table.totalRow?.let { totalRow ->
            sheet.addRow((0 until columns).map { index -> toXlsx(totalRow.getOrNull(index), document, total = true) })
        }

        if (withFilter) {
            sheet.setAutoFilter(headerRow, headerRow + table.rows.size, columns)
        }

        table.note?.let { note -> sheet.addText(note, XlsStyle.NOTE, span.coerceAtLeast(columns)) }
        return headerRow
    }

    private fun toXlsx(cell: Cell?, document: ReportDocument, total: Boolean): XlsxCell = when (cell) {
        null -> XlsxCell.EmptyCell(if (total) XlsStyle.TOTAL_TEXT else XlsStyle.TEXT)
        is Cell.Text -> XlsxCell.Str(cell.value, if (total) XlsStyle.TOTAL_TEXT else XlsStyle.TEXT)
        is Cell.Money -> XlsxCell.MoneyCell(cell.cents, if (total) XlsStyle.TOTAL_MONEY else XlsStyle.MONEY)
        is Cell.Count -> XlsxCell.Num(cell.value.toDouble(), if (total) XlsStyle.TOTAL_INTEGER else XlsStyle.INTEGER)
        is Cell.Decimal -> if (cell.suffix.contains('%')) {
            XlsxCell.Percent(cell.value, if (total) XlsStyle.TOTAL_MONEY else XlsStyle.PERCENT)
        } else {
            XlsxCell.Num(cell.value, if (total) XlsStyle.TOTAL_MONEY else XlsStyle.NUMBER_DECIMAL)
        }
        is Cell.DateCell -> XlsxCell.DateCell(cell.millis)
        Cell.Empty -> XlsxCell.EmptyCell(if (total) XlsStyle.TOTAL_TEXT else XlsStyle.TEXT)
    }

    // ------------------------------ Graficos -------------------------------

    private fun buildChartSheet(writer: XlsxWriter, document: ReportDocument) {
        val sheet = writer.addSheet("Gráficos")
        sheet.setWidths(38, 20, 46)

        sheet.addText(document.companyName.ifBlank { "GIS Supermercados" }, XlsStyle.SUBTITLE, 3)
        sheet.addText("Gráficos · ${document.title}", XlsStyle.TITLE, 3, 24.0)
        sheet.addText(document.periodLabel, XlsStyle.SUBTITLE, 3)
        sheet.addSpacer(12.0)

        document.blocks.filterIsInstance<ReportBlock.Chart>().forEach { chart ->
            sheet.addText(chart.title, XlsStyle.SECTION, 3, 20.0)
            val headerRow = sheet.addRow(
                listOf(
                    XlsxCell.Str("Concepto", XlsStyle.TABLE_HEADER),
                    XlsxCell.Str("Valor", XlsStyle.TABLE_HEADER),
                    XlsxCell.Str("Distribución", XlsStyle.TABLE_HEADER),
                ),
                26.0
            )

            val maxValue = chart.points.maxOf { it.value }.coerceAtLeast(1L)
            chart.points.forEachIndexed { index, point ->
                val barLength = ((point.value.toDouble() / maxValue) * MAX_BAR_CHARS).toInt().coerceAtLeast(0)
                sheet.addRow(
                    listOf(
                        XlsxCell.Str(point.label, XlsStyle.TEXT),
                        XlsxCell.MoneyCell(point.value),
                        XlsxCell.Str(BAR_CHAR.repeat(barLength), XlsStyle.CHART_BAR),
                    )
                )
            }

            if (chart.points.isNotEmpty()) {
                sheet.addDataBar(
                    firstRow = headerRow + 1,
                    lastRow = headerRow + chart.points.size,
                    columnIndex = 1,
                    colorArgb = pointColor(index = 0)
                )
            }
            chart.note?.let { sheet.addText(it, XlsStyle.NOTE, 3) }
            sheet.addSpacer(14.0)
        }
    }

    private fun pointColor(index: Int): Long = Brand.chartColor(index)

    /** Calcula anchos de columna a partir del contenido (maximo 60 filas muestreadas). */
    private fun computeWidths(
        columns: List<ReportColumn>,
        rows: List<List<Cell>>,
        currencySymbol: String,
    ): List<Int> {
        val sample = rows.take(60)
        return columns.mapIndexed { index, column ->
            val headerLength = column.header.length
            val contentLength = sample.maxOfOrNull { row ->
                row.getOrNull(index)?.display(currencySymbol)?.length ?: 0
            } ?: 0
            val width = maxOf(headerLength + 4, contentLength + 3, MIN_COLUMN_WIDTH)
            width.coerceAtMost(MAX_COLUMN_WIDTH)
        }
    }

    private companion object {
        const val MAX_TABLES_IN_SUMMARY = 2
        const val MAX_BAR_CHARS = 34
        const val MIN_COLUMN_WIDTH = 11
        const val MAX_COLUMN_WIDTH = 46
        const val BAR_CHAR = "█"
    }
}
