package com.gis.supermercados.core.reporting.pdf

import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.designsystem.Brand
import com.gis.supermercados.core.reporting.model.Cell
import com.gis.supermercados.core.reporting.model.CellAlign
import com.gis.supermercados.core.reporting.model.ChartKind
import com.gis.supermercados.core.reporting.model.Kpi
import com.gis.supermercados.core.reporting.model.ReportBlock
import com.gis.supermercados.core.reporting.model.ReportColumn
import com.gis.supermercados.core.reporting.model.ReportDocument
import com.gis.supermercados.core.reporting.model.alignment
import com.gis.supermercados.core.reporting.pdf.PdfWriter.PdfFont

/**
 * Motor de paginacion y maquetacion del informe PDF.
 *
 * Trabaja con coordenadas "desde arriba" (como un documento de texto) y las
 * traduce al sistema nativo de PDF (origen abajo-izquierda). Se ocupa de:
 * cabecera de portada, cabecera corrida, pie con numeracion, tarjetas KPI,
 * tablas con salto automatico de pagina y graficos vectoriales.
 */
internal class PdfReportLayout(
    private val writer: PdfWriter,
    private val document: ReportDocument,
    private val totalPages: Int?,
) {

    private val pageWidth: Float get() = writer.pageWidth
    private val pageHeight: Float get() = writer.pageHeight

    val margin = 42f
    val contentWidth: Float get() = pageWidth - margin * 2

    /** Posicion vertical actual medida desde el borde superior. */
    var cursor: Float = 0f
        private set

    private var pageNumber = 1

    // --------------------------- Ciclo de pagina ---------------------------

    /** Abre la primera pagina con la cabecera de portada. */
    fun start() {
        pageNumber = 1
        drawCoverHeader()
    }

    fun newPage() {
        closePage()
        pageNumber++
        writer.startPage()
        drawRunningHeader()
    }

    /** Termina el documento: dibuja el pie de la ultima pagina. */
    fun finish() = closePage()

    /** Salta de pagina si no cabe [height] puntos de contenido. */
    fun ensureSpace(height: Float) {
        if (cursor + height > pageHeight - BOTTOM_RESERVED) newPage()
    }

    private fun closePage() {
        drawFooter()
    }

    // ------------------------------ Cabeceras ------------------------------

    private fun drawCoverHeader() {
        val bandHeight = 104f

        // Banda de marca
        writer.setFillColor(Brand.PRIMARY)
        writer.rect(0f, top(0f) - bandHeight, pageWidth, bandHeight, fill = true)

        // Degradado sutil: rectangulo oscuro en el borde inferior de la banda
        writer.setFillColor(Brand.PRIMARY_DARK)
        writer.rect(0f, top(bandHeight) - 0f, pageWidth, 4f, fill = true)

        // Marca vectorial (cuadrado redondeado con barras de grafico)
        drawLogoMark(x = margin, yFromTop = 30f, size = 40f, background = Brand.ON_PRIMARY, bars = Brand.PRIMARY)

        // Nombre de la empresa y del informe
        val textX = margin + 54f
        writer.drawText(
            text = document.companyName.ifBlank { "Gestión Integral de Supermercados" },
            x = textX, y = top(46f), size = 17f, font = PdfFont.BOLD, color = Brand.ON_PRIMARY
        )
        writer.drawText(
            text = document.title,
            x = textX, y = top(64f), size = 11.5f, font = PdfFont.REGULAR, color = 0xFFD9F0E6
        )
        writer.drawText(
            text = document.subtitle,
            x = textX, y = top(79f), size = 8.5f, font = PdfFont.REGULAR, color = 0xFFBFE3D3
        )

        // Sello de fecha a la derecha
        writer.drawText(
            text = document.generatedAtLabel,
            x = pageWidth - margin, y = top(46f), size = 8.5f, font = PdfFont.BOLD,
            color = Brand.ON_PRIMARY, align = 1f
        )
        writer.drawText(
            text = document.periodLabel,
            x = pageWidth - margin, y = top(62f), size = 8.5f, font = PdfFont.REGULAR,
            color = 0xFFD9F0E6, align = 1f
        )

        cursor = bandHeight + 16f
        drawMetadataCard()
    }

    private fun drawMetadataCard() {
        val items = listOf(
            "Sucursal" to document.storeLabel,
            "Periodo" to document.periodLabel,
            "Generado por" to document.generatedBy.ifBlank { "-" },
            "Moneda" to document.currencySymbol,
        )
        val cardHeight = 40f
        ensureSpace(cardHeight + 14f)

        writer.setFillColor(Brand.SURFACE_VARIANT)
        writer.roundedRect(margin, top(cursor + cardHeight), contentWidth, cardHeight, 6f, fill = true)
        writer.setFillColor(Brand.PRIMARY)
        writer.roundedRect(margin, top(cursor + cardHeight), 3f, cardHeight, 1.5f, fill = true)

        val columnWidth = (contentWidth - 16f) / items.size
        items.forEachIndexed { index, (label, value) ->
            val x = margin + 12f + columnWidth * index
            writer.drawText(label.uppercase(), x, top(cursor + 16f), 6.8f, PdfFont.BOLD, Brand.TEXT_TERTIARY)
            val text = writer.ellipsize(value, columnWidth - 8f, 9f, PdfFont.BOLD)
            writer.drawText(text, x, top(cursor + 30f), 9f, PdfFont.BOLD, Brand.TEXT_PRIMARY)
        }
        cursor += cardHeight + 16f
    }

    private fun drawRunningHeader() {
        val height = 46f
        writer.setFillColor(Brand.PRIMARY)
        writer.rect(0f, top(0f) - 4f, pageWidth, 4f, fill = true)
        writer.drawText(
            text = document.companyName.ifBlank { "GIS Supermercados" },
            x = margin, y = top(24f), size = 9f, font = PdfFont.BOLD, color = Brand.PRIMARY
        )
        writer.drawText(
            text = writer.ellipsize(document.title, contentWidth * 0.5f, 8.5f, PdfFont.REGULAR),
            x = pageWidth - margin, y = top(24f), size = 8.5f, font = PdfFont.REGULAR,
            color = Brand.TEXT_SECONDARY, align = 1f
        )
        writer.setStrokeColor(Brand.OUTLINE)
        writer.setLineWidth(0.6f)
        writer.line(margin, top(height - 8f), pageWidth - margin, top(height - 8f))
        cursor = height + 4f
    }

    private fun drawFooter() {
        val y = pageHeight - 44f
        writer.setStrokeColor(Brand.OUTLINE)
        writer.setLineWidth(0.6f)
        writer.line(margin, top(y), pageWidth - margin, top(y))

        writer.setFillColor(Brand.PRIMARY)
        writer.rect(margin, top(y + 22f), 18f, 2.4f, fill = true)

        writer.drawText(
            text = writer.ellipsize(document.footerNote, contentWidth * 0.6f, 7.2f, PdfFont.REGULAR),
            x = margin, y = top(y + 16f), size = 7.2f, font = PdfFont.REGULAR, color = Brand.TEXT_TERTIARY
        )

        val pageLabel = totalPages?.let { "Página $pageNumber de $it" } ?: "Página $pageNumber"
        writer.drawText(
            text = pageLabel,
            x = pageWidth - margin, y = top(y + 16f), size = 7.8f, font = PdfFont.BOLD,
            color = Brand.TEXT_SECONDARY, align = 1f
        )
    }

    /** Marca vectorial reutilizable (no depende de imagenes externas). */
    private fun drawLogoMark(x: Float, yFromTop: Float, size: Float, background: Long, bars: Long) {
        writer.setFillColor(background)
        writer.roundedRect(x, top(yFromTop + size), size, size, 9f, fill = true)
        writer.setFillColor(bars)
        val padding = size * 0.24f
        val barWidth = (size - padding * 2 - padding * 0.6f) / 3f
        val heights = floatArrayOf(0.42f, 0.66f, 0.9f)
        heights.forEachIndexed { index, factor ->
            val barHeight = (size - padding * 2) * factor
            val barX = x + padding + index * (barWidth + padding * 0.3f)
            val barY = yFromTop + size - padding - barHeight
            writer.roundedRect(barX, top(barY + barHeight), barWidth, barHeight, 1.2f, fill = true)
        }
    }

    // ------------------------------ Bloques --------------------------------

    fun drawSectionTitle(text: String) {
        ensureSpace(34f)
        cursor += 10f
        writer.setFillColor(Brand.PRIMARY)
        writer.rect(margin, top(cursor + 13f), 3.2f, 13f, fill = true)
        writer.drawText(text.uppercase(), margin + 10f, top(cursor + 11f), 10.5f, PdfFont.BOLD, Brand.PRIMARY)
        cursor += 26f
    }

    fun drawParagraph(text: String, bold: Boolean = false) {
        val font = if (bold) PdfFont.BOLD else PdfFont.REGULAR
        val color = if (bold) Brand.TEXT_PRIMARY else Brand.TEXT_SECONDARY
        val lines = writer.wrap(text, contentWidth, 9.2f, font)
        ensureSpace(lines.size * 12f + 6f)
        lines.forEach { line ->
            writer.drawText(line, margin, top(cursor + 9f), 9.2f, font, color)
            cursor += 12f
        }
        cursor += 4f
    }

    fun drawKpiGrid(items: List<Kpi>) {
        if (items.isEmpty()) return
        val columns = if (items.size <= 2) items.size else if (items.size == 3) 3 else 4
        val gap = 8f
        val cardWidth = (contentWidth - gap * (columns - 1)) / columns
        val cardHeight = 58f
        val rows = (items.size + columns - 1) / columns

        items.forEachIndexed { index, kpi ->
            val row = index / columns
            val column = index % columns
            val totalRows = rows
            if (column == 0) ensureSpace(cardHeight * minOf(totalRows - row, 3) + gap * 2)
            val x = margin + column * (cardWidth + gap)
            val y = cursor + row * (cardHeight + gap)

            writer.setFillColor(if (kpi.highlight) Brand.PRIMARY_CONTAINER else Brand.SURFACE_VARIANT)
            writer.roundedRect(x, top(y + cardHeight), cardWidth, cardHeight, 7f, fill = true)
            writer.setStrokeColor(Brand.OUTLINE_VARIANT)
            writer.setLineWidth(0.6f)
            writer.roundedRect(x, top(y + cardHeight), cardWidth, cardHeight, 7f, fill = false, stroke = true)

            val accent = when {
                kpi.variationPercent == null -> Brand.PRIMARY
                kpi.isPositiveVariation -> Brand.POSITIVE
                else -> Brand.NEGATIVE
            }
            writer.setFillColor(accent)
            writer.roundedRect(x, top(y + cardHeight), 3f, cardHeight, 1.5f, fill = true)

            val label = writer.ellipsize(kpi.label.uppercase(), cardWidth - 18f, 6.6f, PdfFont.BOLD)
            writer.drawText(label, x + 11f, top(y + 15f), 6.6f, PdfFont.BOLD, Brand.TEXT_TERTIARY)

            val value = writer.ellipsize(kpi.value, cardWidth - 18f, 13f, PdfFont.BOLD)
            writer.drawText(value, x + 11f, top(y + 32f), 13f, PdfFont.BOLD, Brand.TEXT_PRIMARY)

            val detail = kpi.variationPercent?.let { variation ->
                val sign = if (variation >= 0) "+" else ""
                "${sign}${formatDecimal(variation)}% ${kpi.detail}".trim()
            } ?: kpi.detail
            if (detail.isNotBlank()) {
                val detailText = writer.ellipsize(detail, cardWidth - 18f, 7f, PdfFont.REGULAR)
                writer.drawText(detailText, x + 11f, top(y + 46f), 7f, PdfFont.REGULAR, accent)
            }
        }
        cursor += rows * (cardHeight + gap) + 6f
    }

    fun drawTable(table: ReportBlock.Table) {
        if (table.rows.isEmpty() && table.totalRow == null) {
            drawParagraph(table.title.ifBlank { "Sin datos" } + ": sin registros en el periodo.", bold = false)
            return
        }
        val widths = columnWidths(table.columns)

        if (table.title.isNotBlank()) {
            ensureSpace(26f)
            writer.drawText(table.title, margin, top(cursor + 12f), 10.5f, PdfFont.BOLD, Brand.TEXT_PRIMARY)
            cursor += 22f
        }

        ensureSpace(TABLE_HEADER_HEIGHT + 20f)
        drawTableHeader(table.columns, widths)

        table.rows.forEachIndexed { index, row ->
            val height = rowHeight(row, widths)
            if (cursor + height > pageHeight - BOTTOM_RESERVED) {
                newPage()
                drawTableHeader(table.columns, widths)
            }
            drawTableRow(row, widths, height, zebra = index % 2 == 1)
            cursor += height
        }

        table.totalRow?.let { totalRow ->
            val height = rowHeight(totalRow, widths, bold = true)
            if (cursor + height > pageHeight - BOTTOM_RESERVED) {
                newPage()
                drawTableHeader(table.columns, widths)
            }
            drawTotalRow(totalRow, widths, height)
            cursor += height
        }

        // Cierre inferior de la tabla
        writer.setStrokeColor(Brand.OUTLINE)
        writer.setLineWidth(0.7f)
        writer.line(margin, top(cursor), margin + contentWidth, top(cursor))

        table.note?.let { note ->
            cursor += 4f
            val lines = writer.wrap(note, contentWidth, 7.4f, PdfFont.OBLIQUE)
            ensureSpace(lines.size * 9.5f + 4f)
            lines.forEach { line ->
                writer.drawText(line, margin, top(cursor + 8f), 7.4f, PdfFont.OBLIQUE, Brand.TEXT_TERTIARY)
                cursor += 9.5f
            }
        }
        cursor += 12f
    }

    private fun columnWidths(columns: List<ReportColumn>): List<Float> {
        if (columns.isEmpty()) return emptyList()
        val totalWeight = columns.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
        return columns.map { contentWidth * (it.weight / totalWeight) }
    }

    private fun drawTableHeader(columns: List<ReportColumn>, widths: List<Float>) {
        writer.setFillColor(Brand.PRIMARY)
        writer.rect(margin, top(cursor + TABLE_HEADER_HEIGHT), contentWidth, TABLE_HEADER_HEIGHT, fill = true)

        var x = margin
        columns.forEachIndexed { index, column ->
            val width = widths[index]
            val label = writer.ellipsize(column.header.uppercase(), width - CELL_PADDING * 2, 7.4f, PdfFont.BOLD)
            val align = column.align
            val textX = when (align) {
                CellAlign.START -> x + CELL_PADDING
                CellAlign.CENTER -> x + width / 2f
                CellAlign.END -> x + width - CELL_PADDING
            }
            writer.drawText(
                text = label,
                x = textX,
                y = top(cursor + TABLE_HEADER_HEIGHT / 2f + 2.6f),
                size = 7.4f,
                font = PdfFont.BOLD,
                color = Brand.ON_PRIMARY,
                align = when (align) {
                    CellAlign.START -> 0f
                    CellAlign.CENTER -> 0.5f
                    CellAlign.END -> 1f
                }
            )
            x += width
        }
        cursor += TABLE_HEADER_HEIGHT
    }

    private fun drawTableRow(row: List<Cell>, widths: List<Float>, height: Float, zebra: Boolean) {
        if (zebra) {
            writer.setFillColor(Brand.SURFACE_VARIANT)
            writer.rect(margin, top(cursor + height), contentWidth, height, fill = true)
        }
        writer.setStrokeColor(Brand.OUTLINE_VARIANT)
        writer.setLineWidth(0.4f)
        writer.line(margin, top(cursor + height), margin + contentWidth, top(cursor + height))
        drawCells(row, widths, height, bold = false)
    }

    private fun drawTotalRow(row: List<Cell>, widths: List<Float>, height: Float) {
        writer.setFillColor(Brand.PRIMARY_CONTAINER)
        writer.rect(margin, top(cursor + height), contentWidth, height, fill = true)
        writer.setStrokeColor(Brand.PRIMARY)
        writer.setLineWidth(0.9f)
        writer.line(margin, top(cursor), margin + contentWidth, top(cursor))
        drawCells(row, widths, height, bold = true)
    }

    private fun drawCells(row: List<Cell>, widths: List<Float>, height: Float, bold: Boolean) {
        val font = if (bold) PdfFont.BOLD else PdfFont.REGULAR
        var x = margin
        row.forEachIndexed { index, cell ->
            if (index >= widths.size) return@forEachIndexed
            val width = widths[index]
            val available = width - CELL_PADDING * 2
            val text = cell.display(document.currencySymbol)
            val lines = writer.wrap(text, available, CELL_FONT_SIZE, font)
            val align = cell.alignment()
            val startY = cursor + (height - lines.size * CELL_LINE_HEIGHT) / 2f + CELL_FONT_SIZE + 1f

            lines.forEachIndexed { lineIndex, line ->
                val ellipsized = if (lines.size == 1) writer.ellipsize(line, available, CELL_FONT_SIZE, font) else line
                val textX = when (align) {
                    CellAlign.START -> x + CELL_PADDING
                    CellAlign.CENTER -> x + width / 2f
                    CellAlign.END -> x + width - CELL_PADDING
                }
                writer.drawText(
                    text = ellipsized,
                    x = textX,
                    y = top(startY + lineIndex * CELL_LINE_HEIGHT),
                    size = CELL_FONT_SIZE,
                    font = font,
                    color = cellColor(cell, bold),
                    align = when (align) {
                        CellAlign.START -> 0f
                        CellAlign.CENTER -> 0.5f
                        CellAlign.END -> 1f
                    }
                )
            }
            x += width
        }
    }

    private fun cellColor(cell: Cell, bold: Boolean): Long {
        if (bold) return Brand.TEXT_PRIMARY
        // Importes negativos en rojo: lectura inmediata en informes financieros.
        if (cell is Cell.Money && cell.cents < 0) return Brand.NEGATIVE
        if (cell is Cell.Decimal && cell.value < 0) return Brand.NEGATIVE
        return Brand.TEXT_PRIMARY
    }

    private fun rowHeight(row: List<Cell>, widths: List<Float>, bold: Boolean = false): Float {
        val font = if (bold) PdfFont.BOLD else PdfFont.REGULAR
        var maxLines = 1
        row.forEachIndexed { index, cell ->
            val width = widths.getOrElse(index) { 60f } - CELL_PADDING * 2
            val text = cell.display(document.currencySymbol)
            val lines = writer.wrap(text, width.coerceAtLeast(20f), CELL_FONT_SIZE, font).size
            if (lines > maxLines) maxLines = lines
        }
        return maxLines * CELL_LINE_HEIGHT + CELL_PADDING * 1.6f
    }

    // ------------------------------ Graficos -------------------------------

    fun drawChart(chart: ReportBlock.Chart) {
        if (chart.points.isEmpty()) return
        when (chart.kind) {
            ChartKind.HORIZONTAL_BARS, ChartKind.DONUT -> drawHorizontalBars(chart)
            ChartKind.BARS -> drawBars(chart)
            ChartKind.LINE -> drawLineChart(chart)
        }
        chart.note?.let { note ->
            val lines = writer.wrap(note, contentWidth, 7.4f, PdfFont.OBLIQUE)
            lines.forEach { line ->
                writer.drawText(line, margin, top(cursor + 8f), 7.4f, PdfFont.OBLIQUE, Brand.TEXT_TERTIARY)
                cursor += 9.5f
            }
        }
        cursor += 12f
    }

    private fun drawChartTitle(title: String) {
        ensureSpace(28f)
        writer.drawText(title, margin, top(cursor + 12f), 10.5f, PdfFont.BOLD, Brand.TEXT_PRIMARY)
        cursor += 22f
    }

    private fun drawBars(chart: ReportBlock.Chart) {
        val points = chart.points.take(MAX_VERTICAL_POINTS)
        val chartHeight = 150f
        val labelArea = 22f
        drawChartTitle(chart.title)
        ensureSpace(chartHeight + labelArea + 16f)

        val maxValue = (points.maxOf { maxOf(it.value, it.secondaryValue) }).coerceAtLeast(1L)
        val plotTop = cursor
        val plotHeight = chartHeight - labelArea

        // Rejilla y etiquetas del eje Y
        for (step in 0..GRID_STEPS) {
            val ratio = step.toFloat() / GRID_STEPS
            val y = plotTop + plotHeight * (1f - ratio)
            writer.setStrokeColor(Brand.OUTLINE_VARIANT)
            writer.setLineWidth(0.5f)
            if (step > 0) writer.setLineDash(2.5f, 2.5f)
            writer.line(margin, top(y), margin + contentWidth, top(y))
            writer.resetLineDash()
            val label = Money.formatCompact((maxValue * ratio).toLong(), document.currencySymbol)
            writer.drawText(label, margin - 4f, top(y + 2.6f), 6.6f, PdfFont.REGULAR, Brand.TEXT_TERTIARY, align = 1f)
        }

        val slot = contentWidth / points.size
        val barWidth = (slot * 0.62f).coerceAtMost(38f)
        points.forEachIndexed { index, point ->
            val ratio = point.value.toFloat() / maxValue
            val barHeight = (plotHeight * ratio).coerceAtLeast(if (point.value > 0) 1.2f else 0f)
            val x = margin + slot * index + (slot - barWidth) / 2f
            val y = plotTop + plotHeight - barHeight

            writer.setFillColor(point.colorArgb ?: Brand.chartColor(index))
            writer.roundedRect(x, top(y + barHeight), barWidth, barHeight, 2.5f, fill = true)

            val valueLabel = Money.formatCompact(point.value, document.currencySymbol)
            writer.drawText(
                text = valueLabel, x = x + barWidth / 2f, y = top(y - 3f),
                size = 6.6f, font = PdfFont.BOLD, color = Brand.TEXT_SECONDARY, align = 0.5f
            )

            val categoryLabel = writer.ellipsize(point.label, slot - 2f, 6.6f, PdfFont.REGULAR)
            writer.drawText(
                text = categoryLabel, x = x + barWidth / 2f, y = top(plotTop + plotHeight + 11f),
                size = 6.6f, font = PdfFont.REGULAR, color = Brand.TEXT_TERTIARY, align = 0.5f
            )
        }
        cursor = plotTop + chartHeight + 6f
    }

    private fun drawLineChart(chart: ReportBlock.Chart) {
        val points = chart.points.take(MAX_VERTICAL_POINTS)
        val chartHeight = 150f
        val labelArea = 22f
        drawChartTitle(chart.title)
        ensureSpace(chartHeight + labelArea + 16f)

        val maxValue = (points.maxOf { maxOf(it.value, it.secondaryValue) }).coerceAtLeast(1L)
        val plotTop = cursor
        val plotHeight = chartHeight - labelArea

        for (step in 0..GRID_STEPS) {
            val ratio = step.toFloat() / GRID_STEPS
            val y = plotTop + plotHeight * (1f - ratio)
            writer.setStrokeColor(Brand.OUTLINE_VARIANT)
            writer.setLineWidth(0.5f)
            if (step > 0) writer.setLineDash(2.5f, 2.5f)
            writer.line(margin, top(y), margin + contentWidth, top(y))
            writer.resetLineDash()
            writer.drawText(
                Money.formatCompact((maxValue * ratio).toLong(), document.currencySymbol),
                margin - 4f, top(y + 2.6f), 6.6f, PdfFont.REGULAR, Brand.TEXT_TERTIARY, align = 1f
            )
        }

        val step = if (points.size <= 1) contentWidth else contentWidth / (points.size - 1)
        val coordinates = points.mapIndexed { index, point ->
            val x = margin + step * index
            val ratio = point.value.toFloat() / maxValue
            val y = plotTop + plotHeight * (1f - ratio)
            x to y
        }

        // Area bajo la curva (relleno suave de color corporativo)
        val area = coordinates + listOf(
            coordinates.last().first to (plotTop + plotHeight),
            coordinates.first().first to (plotTop + plotHeight)
        )
        writer.setFillColor(Brand.PRIMARY_CONTAINER)
        writer.filledPolygon(area)

        // Linea principal
        writer.setStrokeColor(Brand.PRIMARY)
        writer.setLineWidth(1.6f)
        writer.polyline(coordinates)

        // Puntos
        coordinates.forEach { (x, y) ->
            writer.setFillColor(Brand.SURFACE)
            writer.circle(x, top(y), 2.6f, fill = true)
            writer.setStrokeColor(Brand.PRIMARY)
            writer.setLineWidth(1.1f)
            writer.circle(x, top(y), 2.6f, fill = false, stroke = true)
        }

        // Etiquetas del eje X (solo algunas para no saturar)
        val labelEvery = (points.size / MAX_X_LABELS).coerceAtLeast(1)
        points.forEachIndexed { index, point ->
            if (index % labelEvery != 0 && index != points.size - 1) return@forEachIndexed
            writer.drawText(
                text = writer.ellipsize(point.label, step * labelEvery - 2f, 6.6f, PdfFont.REGULAR),
                x = coordinates[index].first, y = top(plotTop + plotHeight + 11f),
                size = 6.6f, font = PdfFont.REGULAR, color = Brand.TEXT_TERTIARY, align = 0.5f
            )
        }
        cursor = plotTop + chartHeight + 6f
    }

    /** Barras horizontales: la mejor opcion cuando las etiquetas son largas. */
    private fun drawHorizontalBars(chart: ReportBlock.Chart) {
        val points = chart.points.take(MAX_HORIZONTAL_POINTS)
        val rowHeight = 17f
        drawChartTitle(chart.title)
        ensureSpace(points.size * rowHeight + 20f)

        val labelWidth = contentWidth * 0.34f
        val valueWidth = contentWidth * 0.18f
        val barAreaWidth = contentWidth - labelWidth - valueWidth
        val maxValue = points.maxOf { it.value }.coerceAtLeast(1L)
        val total = if (chart.kind == ChartKind.DONUT) points.sumOf { it.value } else 0L

        points.forEachIndexed { index, point ->
            val y = cursor + index * rowHeight
            val label = writer.ellipsize(point.label, labelWidth - 8f, 7.6f, PdfFont.REGULAR)
            writer.drawText(label, margin, top(y + 11f), 7.6f, PdfFont.REGULAR, Brand.TEXT_PRIMARY)

            // Pista de la barra
            writer.setFillColor(Brand.SURFACE_VARIANT)
            writer.roundedRect(margin + labelWidth, top(y + 12.5f), barAreaWidth, 8f, 4f, fill = true)

            val ratio = (point.value.toFloat() / maxValue).coerceIn(0f, 1f)
            if (ratio > 0f) {
                writer.setFillColor(point.colorArgb ?: Brand.chartColor(index))
                writer.roundedRect(
                    margin + labelWidth, top(y + 12.5f),
                    (barAreaWidth * ratio).coerceAtLeast(2f), 8f, 4f, fill = true
                )
            }

            val percentText = if (total > 0) {
                " (${formatDecimal(point.value * 100.0 / total)}%)"
            } else ""
            val valueText = writer.ellipsize(
                Money.formatCompact(point.value, document.currencySymbol) + percentText,
                valueWidth, 7.6f, PdfFont.BOLD
            )
            writer.drawText(
                valueText, margin + contentWidth, top(y + 11f), 7.6f, PdfFont.BOLD,
                Brand.TEXT_SECONDARY, align = 1f
            )
        }
        cursor += points.size * rowHeight + 8f
    }

    // ------------------------------ Utilidades ------------------------------

    /** Convierte una coordenada "desde arriba" a la coordenada Y nativa de PDF. */
    private fun top(yFromTop: Float): Float = pageHeight - yFromTop

    private fun formatDecimal(value: Double): String =
        String.format(java.util.Locale("es"), "%.1f", value).replace('.', ',')

    fun advance(amount: Float) {
        cursor += amount
    }

    companion object {
        private const val BOTTOM_RESERVED = 56f
        private const val TABLE_HEADER_HEIGHT = 19f
        private const val CELL_PADDING = 4.5f
        private const val CELL_FONT_SIZE = 7.8f
        private const val CELL_LINE_HEIGHT = 9.8f
        private const val GRID_STEPS = 4
        private const val MAX_VERTICAL_POINTS = 16
        private const val MAX_HORIZONTAL_POINTS = 14
        private const val MAX_X_LABELS = 8
    }
}
