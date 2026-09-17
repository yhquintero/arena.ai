package com.gis.supermercados.core.reporting.csv

import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.reporting.model.Cell
import com.gis.supermercados.core.reporting.model.ReportBlock
import com.gis.supermercados.core.reporting.model.ReportDocument
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exportacion CSV (universally compatible: Excel, Google Sheets, Power BI).
 *
 * Detalles de compatibilidad:
 * - Separador ';' (Excel en espanol lo detecta automaticamente).
 * - Prefijo BOM UTF-8 para que los acentos se vean bien al abrir con doble clic.
 * - Escapado RFC 4180 de comillas.
 */
@Singleton
class CsvReportRenderer @Inject constructor() {

    fun render(document: ReportDocument): ByteArray {
        val builder = StringBuilder()

        // Cabecera del documento
        builder.appendLine(document.companyName.ifBlank { "GIS Supermercados" })
        builder.appendLine(document.title)
        builder.appendLine(document.periodLabel)
        builder.appendLine("Sucursal;${escape(document.storeLabel)}")
        builder.appendLine("Generado;${escape(document.generatedAtLabel)}")
        builder.appendLine("Responsable;${escape(document.generatedBy)}")
        builder.appendLine()

        document.blocks.forEach { block ->
            when (block) {
                is ReportBlock.SectionTitle -> {
                    builder.appendLine()
                    builder.appendLine(escape(block.text.uppercase()))
                }

                is ReportBlock.Paragraph -> builder.appendLine(escape(block.text))

                is ReportBlock.KpiGrid -> block.items.forEach { kpi ->
                    val variation = kpi.variationPercent?.let { ";${formatDecimal(it)}%" } ?: ""
                    builder.appendLine("${escape(kpi.label)};${escape(kpi.value)};${escape(kpi.detail)}$variation")
                }

                is ReportBlock.Table -> {
                    if (block.title.isNotBlank()) builder.appendLine(escape(block.title))
                    builder.appendLine(block.columns.joinToString(SEPARATOR) { escape(it.header) })
                    block.rows.forEach { row ->
                        builder.appendLine(row.joinToString(SEPARATOR) { cell(it, document.currencySymbol) })
                    }
                    block.totalRow?.let { total ->
                        builder.appendLine(total.joinToString(SEPARATOR) { cell(it, document.currencySymbol) })
                    }
                    block.note?.let { builder.appendLine(escape(it)) }
                    builder.appendLine()
                }

                is ReportBlock.Chart -> {
                    builder.appendLine(escape(block.title))
                    builder.appendLine("Concepto;Valor")
                    block.points.forEach { point ->
                        builder.appendLine("${escape(point.label)};${point.value}")
                    }
                    builder.appendLine()
                }

                ReportBlock.PageBreak -> Unit
            }
        }

        builder.appendLine()
        builder.appendLine(escape(document.footerNote))

        // BOM UTF-8 + contenido
        val body = builder.toString().toByteArray(Charsets.UTF_8)
        return UTF8_BOM + body
    }

    /** En CSV se exportan los NUMEROS en bruto (sin simbolo) para poder operar con ellos. */
    private fun cell(cell: Cell, currencySymbol: String): String = when (cell) {
        is Cell.Money -> escape(Money.formatNumber(cell.cents))
        is Cell.Count -> cell.value.toString()
        is Cell.Decimal -> formatDecimal(cell.value).let { "$it${cell.suffix}" }
        else -> escape(cell.display(currencySymbol))
    }

    private fun formatDecimal(value: Double): String =
        String.format(java.util.Locale("es"), "%.2f", value).replace('.', ',')

    private fun escape(value: String): String {
        val needsQuotes = value.contains(SEPARATOR) || value.contains('"') || value.contains('\n')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    private companion object {
        const val SEPARATOR = ";"
        val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    }
}
