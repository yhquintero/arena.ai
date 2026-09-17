package com.gis.supermercados.core.reporting.xlsx

import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Hoja del libro: filas, anchos, combinaciones, filtros y barras de datos. */
class XlsxSheet internal constructor(val name: String) {

    internal data class RowData(val index: Int, val cells: List<XlsxCell>, val height: Double?)

    internal data class DataBar(val reference: String, val colorArgb: Long, val priority: Int)

    internal val rows = mutableListOf<RowData>()
    internal val merges = mutableListOf<String>()
    internal val dataBars = mutableListOf<DataBar>()
    internal var widths: List<Int> = emptyList()
    internal var freezeRows: Int = 0
    internal var autoFilterRef: String? = null

    /** Columnas realmente utilizadas (para el rango de impresion y merges). */
    var maxColumns: Int = 0
        private set

    val rowCount: Int get() = rows.size

    fun setWidths(vararg columnWidths: Int) {
        widths = columnWidths.toList()
        if (columnWidths.size > maxColumns) maxColumns = columnWidths.size
    }

    /** Anade una fila y devuelve su numero (1-based), util para merges y filtros. */
    fun addRow(cells: List<XlsxCell>, height: Double? = null): Int {
        rows.add(RowData(rows.size + 1, cells, height))
        if (cells.size > maxColumns) maxColumns = cells.size
        return rows.size
    }

    /** Fila de texto que ocupa [span] columnas (se combina automaticamente). */
    fun addText(text: String, style: Int, span: Int = 0, height: Double? = null): Int {
        val columns = if (span > 0) span else maxColumns.coerceAtLeast(1)
        val cells = mutableListOf<XlsxCell>(XlsxCell.Str(text, style))
        repeat((columns - 1).coerceAtLeast(0)) { cells.add(XlsxCell.EmptyCell(style)) }
        val rowIndex = addRow(cells, height)
        if (columns > 1) merge("$ROW_A$rowIndex:${columnName(columns - 1)}$rowIndex")
        return rowIndex
    }

    fun addSpacer(height: Double = 8.0): Int = addRow(emptyList(), height)

    fun merge(reference: String) {
        merges.add(reference)
    }

    fun freezeAt(rows: Int) {
        freezeRows = rows
    }

    /** Filtro automatico sobre el rango de una tabla (cabecera + datos). */
    fun setAutoFilter(headerRow: Int, lastRow: Int, columns: Int) {
        if (lastRow > headerRow && columns > 0) {
            autoFilterRef = "$ROW_A$headerRow:${columnName(columns - 1)}$lastRow"
        }
    }

    /** Barras de datos (grafico nativo dentro de la celda). */
    fun addDataBar(firstRow: Int, lastRow: Int, columnIndex: Int, colorArgb: Long) {
        if (lastRow < firstRow) return
        val column = columnName(columnIndex)
        dataBars.add(
            DataBar(
                reference = "$column$firstRow:$column$lastRow",
                colorArgb = colorArgb,
                priority = dataBars.size + 1
            )
        )
    }

    companion object {
        const val ROW_A = "A"

        /** Nombre de columna a partir del indice 0-based (0 -> A, 27 -> AB). */
        fun columnName(index: Int): String {
            var remaining = index
            val builder = StringBuilder()
            while (remaining >= 0) {
                builder.insert(0, ('A' + remaining % 26))
                remaining = remaining / 26 - 1
            }
            return builder.toString()
        }
    }
}

/**
 * Generador de archivos .xlsx (Office Open XML) sin dependencias externas.
 *
 * Produce un libro multipagina con:
 * - estilos corporativos (cabeceras, totales, formatos de moneda y fecha),
 * - cadenas compartidas (sharedStrings) para maxima compatibilidad,
 * - paneles congelados, autofiltro y barras de datos (graficos en celda),
 * - configuracion de pagina lista para imprimir.
 *
 * Referencias de formato: ECMA-376 (SpreadsheetML).
 */
class XlsxWriter(
    private val currencySymbol: String = "$",
    private val documentTitle: String = "Reporte",
    private val companyName: String = "GIS Supermercados",
    private val author: String = "GIS Supermercados",
) {

    private val sheets = mutableListOf<XlsxSheet>()
    private val sharedStrings = LinkedHashMap<String, Int>()
    private var sharedStringReferences = 0

    fun addSheet(name: String): XlsxSheet {
        val unique = sanitizeSheetName(name, sheets.map { it.name })
        return XlsxSheet(unique).also { sheets.add(it) }
    }

    val sheetNames: List<String> get() = sheets.map { it.name }

    /** Ensambla el archivo .xlsx completo en memoria. */
    fun build(): ByteArray {
        sharedStrings.clear()
        sharedStringReferences = 0
        val sheetXmls = sheets.map { buildSheetXml(it) }

        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.setLevel(Deflater.DEFAULT_COMPRESSION)
            fun put(entryName: String, content: String) {
                zip.putNextEntry(ZipEntry(entryName))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            put("[Content_Types].xml", buildContentTypes())
            put("_rels/.rels", buildRootRelationships())
            put("docProps/core.xml", buildCoreProperties())
            put("docProps/app.xml", buildAppProperties())
            put("xl/workbook.xml", buildWorkbook())
            put("xl/_rels/workbook.xml.rels", buildWorkbookRelationships())
            put("xl/styles.xml", buildStyles())
            put("xl/sharedStrings.xml", buildSharedStrings())
            sheetXmls.forEachIndexed { index, xml ->
                put("xl/worksheets/sheet${index + 1}.xml", xml)
            }
        }
        return output.toByteArray()
    }

    // ------------------------------ Paquetes ------------------------------

    private fun buildContentTypes(): String = buildString {
        append(XML_HEADER)
        append("<Types xmlns=\"$NS_CONTENT_TYPES\">")
        append("<Default Extension=\"rels\" ContentType=\"$CT_RELATIONSHIPS\"/>")
        append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"$CT_WORKBOOK\"/>")
        sheets.indices.forEach { index ->
            append("<Override PartName=\"/xl/worksheets/sheet${index + 1}.xml\" ContentType=\"$CT_WORKSHEET\"/>")
        }
        append("<Override PartName=\"/xl/styles.xml\" ContentType=\"$CT_STYLES\"/>")
        append("<Override PartName=\"/xl/sharedStrings.xml\" ContentType=\"$CT_SHARED_STRINGS\"/>")
        append("<Override PartName=\"/docProps/core.xml\" ContentType=\"$CT_CORE\"/>")
        append("<Override PartName=\"/docProps/app.xml\" ContentType=\"$CT_APP\"/>")
        append("</Types>")
    }

    private fun buildRootRelationships(): String = buildString {
        append(XML_HEADER)
        append("<Relationships xmlns=\"$NS_RELATIONSHIPS\">")
        append("<Relationship Id=\"rId1\" Type=\"$REL_OFFICE_DOCUMENT\" Target=\"xl/workbook.xml\"/>")
        append("<Relationship Id=\"rId2\" Type=\"$REL_CORE_PROPERTIES\" Target=\"docProps/core.xml\"/>")
        append("<Relationship Id=\"rId3\" Type=\"$REL_EXTENDED_PROPERTIES\" Target=\"docProps/app.xml\"/>")
        append("</Relationships>")
    }

    private fun buildWorkbookRelationships(): String = buildString {
        append(XML_HEADER)
        append("<Relationships xmlns=\"$NS_RELATIONSHIPS\">")
        sheets.indices.forEach { index ->
            append("<Relationship Id=\"rId${index + 1}\" Type=\"$REL_WORKSHEET\" Target=\"worksheets/sheet${index + 1}.xml\"/>")
        }
        val stylesId = sheets.size + 1
        val sharedId = sheets.size + 2
        append("<Relationship Id=\"rId$stylesId\" Type=\"$REL_STYLES\" Target=\"styles.xml\"/>")
        append("<Relationship Id=\"rId$sharedId\" Type=\"$REL_SHARED_STRINGS\" Target=\"sharedStrings.xml\"/>")
        append("</Relationships>")
    }

    private fun buildWorkbook(): String = buildString {
        append(XML_HEADER)
        append("<workbook xmlns=\"$NS_MAIN\" xmlns:r=\"$NS_R\">")
        append("<workbookPr autoCompressPictures=\"0\"/>")
        append("<bookViews><workbookView activeTab=\"0\" firstSheet=\"0\"/></bookViews>")
        append("<sheets>")
        sheets.forEachIndexed { index, sheet ->
            append("<sheet name=\"${escape(sheet.name)}\" sheetId=\"${index + 1}\" state=\"visible\" r:id=\"rId${index + 1}\"/>")
        }
        append("</sheets>")
        append("<calcPr calcId=\"191029\" fullCalcOnLoad=\"1\"/>")
        append("</workbook>")
    }

    private fun buildCoreProperties(): String {
        val timestamp = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
            .format(java.time.format.DateTimeFormatter.ISO_INSTANT)
        return buildString {
            append(XML_HEADER)
            append("<cp:coreProperties xmlns:cp=\"$NS_CORE_PROPS\" xmlns:dc=\"$NS_DC\" " +
                "xmlns:dcterms=\"$NS_DCTERMS\" xmlns:dcmitype=\"http://purl.org/dc/dcmitype/\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">")
            append("<dc:title>${escape(documentTitle)}</dc:title>")
            append("<dc:creator>${escape(author)}</dc:creator>")
            append("<cp:lastModifiedBy>${escape(author)}</cp:lastModifiedBy>")
            append("<cp:revision>1</cp:revision>")
            append("<dcterms:created xsi:type=\"dcterms:W3CDTF\">$timestamp</dcterms:created>")
            append("<dcterms:modified xsi:type=\"dcterms:W3CDTF\">$timestamp</dcterms:modified>")
            append("</cp:coreProperties>")
        }
    }

    private fun buildAppProperties(): String = buildString {
        append(XML_HEADER)
        append("<Properties xmlns=\"$NS_EXTENDED_PROPS\" xmlns:vt=\"$NS_VT\">")
        append("<Application>GIS Supermercados</Application>")
        append("<AppVersion>1.0000</AppVersion>")
        append("<Company>${escape(companyName)}</Company>")
        append("<DocSecurity>0</DocSecurity>")
        append("<ScaleCrop>false</ScaleCrop>")
        append("<LinksUpToDate>false</LinksUpToDate>")
        append("<SharedDoc>false</SharedDoc>")
        append("<HyperlinksChanged>false</HyperlinksChanged>")
        append("</Properties>")
    }

    // ------------------------------- Hojas --------------------------------

    private fun buildSheetXml(sheet: XlsxSheet): String = buildString {
        val columns = sheet.maxColumns.coerceAtLeast(1)
        val lastRow = sheet.rowCount.coerceAtLeast(1)

        append(XML_HEADER)
        append("<worksheet xmlns=\"$NS_MAIN\" xmlns:r=\"$NS_R\">")
        append("<dimension ref=\"A1:${XlsxSheet.columnName(columns - 1)}$lastRow\"/>")
        append("<sheetViews><sheetView showGridLines=\"0\" workbookViewId=\"0\" tabSelected=\"${if (sheet === sheets.firstOrNull()) "1" else "0"}\">")
        if (sheet.freezeRows > 0) {
            val topLeft = "A${sheet.freezeRows + 1}"
            append("<pane ySplit=\"${sheet.freezeRows}\" topLeftCell=\"$topLeft\" activePane=\"bottomLeft\" state=\"frozen\"/>")
            append("<selection pane=\"bottomLeft\" activeCell=\"$topLeft\" sqref=\"$topLeft\"/>")
        }
        append("</sheetView></sheetViews>")
        append("<sheetFormatPr defaultRowHeight=\"15\" defaultColWidth=\"12\"/>")

        if (sheet.widths.isNotEmpty()) {
            append("<cols>")
            sheet.widths.forEachIndexed { index, width ->
                append("<col min=\"${index + 1}\" max=\"${index + 1}\" width=\"$width\" customWidth=\"1\"/>")
            }
            append("</cols>")
        }

        append("<sheetData>")
        sheet.rows.forEach { row ->
            append("<row r=\"${row.index}\"")
            if (row.height != null) append(" ht=\"${formatNumber(row.height)}\" customHeight=\"1\"")
            if (row.cells.isEmpty()) {
                append("/>")
            } else {
                append(">")
                row.cells.forEachIndexed { columnIndex, cell ->
                    append(buildCell(XlsxSheet.columnName(columnIndex) + row.index, cell))
                }
                append("</row>")
            }
        }
        append("</sheetData>")

        // Orden obligatorio segun ECMA-376: autoFilter, mergeCells, conditionalFormatting,
        // pageMargins, pageSetup.
        sheet.autoFilterRef?.let { append("<autoFilter ref=\"$it\"/>") }

        if (sheet.merges.isNotEmpty()) {
            append("<mergeCells count=\"${sheet.merges.size}\">")
            sheet.merges.forEach { append("<mergeCell ref=\"$it\"/>") }
            append("</mergeCells>")
        }

        if (sheet.dataBars.isNotEmpty()) {
            sheet.dataBars.forEach { bar ->
                append("<conditionalFormatting sqref=\"${bar.reference}\">")
                append("<cfRule type=\"dataBar\" priority=\"${bar.priority}\">")
                append("<dataBar><cfvo type=\"min\"/><cfvo type=\"max\"/>")
                append("<color rgb=\"${argb(bar.colorArgb)}\"/></dataBar>")
                append("</cfRule></conditionalFormatting>")
            }
        }

        append("<pageMargins left=\"0.4\" right=\"0.4\" top=\"0.6\" bottom=\"0.6\" header=\"0.3\" footer=\"0.3\"/>")
        append("<pageSetup paperSize=\"9\" orientation=\"portrait\" fitToWidth=\"1\" fitToHeight=\"0\"/>")
        append("</worksheet>")
    }

    private fun buildCell(reference: String, cell: XlsxCell): String = buildString {
        when (cell) {
            is XlsxCell.Str -> {
                val index = sharedStringIndex(cell.value)
                append("<c r=\"$reference\" s=\"${cell.style}\" t=\"s\"><v>$index</v></c>")
            }
            is XlsxCell.Num -> {
                append("<c r=\"$reference\" s=\"${cell.style}\"><v>${formatNumber(cell.value)}</v></c>")
            }
            is XlsxCell.MoneyCell -> {
                append("<c r=\"$reference\" s=\"${cell.style}\"><v>${formatNumber(cell.value)}</v></c>")
            }
            is XlsxCell.Percent -> {
                append("<c r=\"$reference\" s=\"${cell.style}\"><v>${formatNumber(cell.value)}</v></c>")
            }
            is XlsxCell.DateCell -> {
                val serial = excelSerialDate(cell.millis)
                append("<c r=\"$reference\" s=\"${cell.style}\"><v>${formatNumber(serial)}</v></c>")
            }
            is XlsxCell.EmptyCell -> {
                append("<c r=\"$reference\" s=\"${cell.style}\"/>")
            }
        }
    }

    private fun sharedStringIndex(value: String): Int {
        sharedStringReferences++
        return sharedStrings.getOrPut(value) { sharedStrings.size }
    }

    private fun buildSharedStrings(): String = buildString {
        append(XML_HEADER)
        append(
            "<sst xmlns=\"$NS_MAIN\" count=\"$sharedStringReferences\" " +
                "uniqueCount=\"${sharedStrings.size}\">"
        )
        sharedStrings.keys.forEach { value ->
            val needsSpacePreserve = value.startsWith(" ") || value.endsWith(" ") || value.contains("\n")
            append("<si><t")
            if (needsSpacePreserve) append(" xml:space=\"preserve\"")
            append(">${escape(value)}</t></si>")
        }
        append("</sst>")
    }

    // ------------------------------- Estilos -------------------------------

    private fun buildStyles(): String {
        val money = "\"${escape(currencySymbol)}\"#,##0.00"
        val moneyRed = "\"${escape(currencySymbol)}\"#,##0.00;[Red]-\"${escape(currencySymbol)}\"#,##0.00"

        return buildString {
            append(XML_HEADER)
            append("<styleSheet xmlns=\"$NS_MAIN\">")

            append("<numFmts count=\"5\">")
            append("<numFmt numFmtId=\"164\" formatCode=\"${escape(money)}\"/>")
            append("<numFmt numFmtId=\"165\" formatCode=\"${escape(moneyRed)}\"/>")
            append("<numFmt numFmtId=\"166\" formatCode=\"dd/mm/yyyy\"/>")
            append("<numFmt numFmtId=\"167\" formatCode=\"0.0&quot;%&quot;\"/>")
            append("<numFmt numFmtId=\"168\" formatCode=\"#,##0.00\"/>")
            append("</numFmts>")

            append("<fonts count=\"12\">")
            append(font(sz = 11, color = TEXT, bold = false))                                     // 0
            append(font(sz = 16, color = PRIMARY, bold = true))                                   // 1 titulo
            append(font(sz = 9, color = TEXT_SECONDARY, bold = false, italic = true))             // 2 subtitulo
            append(font(sz = 9, color = WHITE, bold = true))                                      // 3 cabecera tabla
            append(font(sz = 11, color = PRIMARY, bold = true))                                   // 4 seccion
            append(font(sz = 9, color = TEXT, bold = false))                                      // 5 texto
            append(font(sz = 9, color = TEXT, bold = true))                                       // 6 texto negrita
            append(font(sz = 8, color = TEXT_TERTIARY, bold = false, italic = true))              // 7 nota
            append(font(sz = 8, color = TEXT_TERTIARY, bold = true))                              // 8 etiqueta KPI
            append(font(sz = 14, color = PRIMARY_DARK, bold = true))                              // 9 valor KPI
            append(font(sz = 9, color = PRIMARY_DARK, bold = true))                               // 10 totales
            append(font(sz = 9, color = PRIMARY, bold = false, name = "Consolas"))                // 11 barras
            append("</fonts>")

            append("<fills count=\"6\">")
            append("<fill><patternFill patternType=\"none\"/></fill>")                             // 0
            append("<fill><patternFill patternType=\"gray125\"/></fill>")                          // 1
            append(fill(PRIMARY))                                                                  // 2
            append(fill(SURFACE_VARIANT))                                                          // 3
            append(fill(PRIMARY_CONTAINER))                                                        // 4
            append(fill(WHITE))                                                                    // 5
            append("</fills>")

            append("<borders count=\"3\">")
            append("<border><left/><right/><top/><bottom/><diagonal/></border>")                   // 0
            append(border(thin = OUTLINE, topThin = OUTLINE))                                      // 1 celda
            append(border(thin = OUTLINE, topThin = PRIMARY, topStyle = "medium"))                 // 2 totales
            append("</borders>")

            append("<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>")

            append("<cellXfs count=\"23\">")
            append(xf())                                                                           // 0 DEFAULT
            append(xf(fontId = 1, alignment = "left"))                                             // 1 TITLE
            append(xf(fontId = 2, alignment = "left"))                                             // 2 SUBTITLE
            append(xf(fontId = 4, fillId = 4, applyFill = true, alignment = "left"))                // 3 SECTION
            append(xf(fontId = 3, fillId = 2, borderId = 1, applyFill = true, applyBorder = true,
                alignment = "center", wrap = true, vertical = "center"))                            // 4 TABLE_HEADER
            append(xf(fontId = 5, borderId = 1, applyBorder = true, alignment = "left",
                wrap = true, vertical = "top"))                                                     // 5 TEXT
            append(xf(fontId = 6, borderId = 1, applyBorder = true, alignment = "left"))            // 6 TEXT_BOLD
            append(xf(fontId = 5, borderId = 1, numFmtId = 165, applyNumberFormat = true,
                applyBorder = true, alignment = "right"))                                           // 7 MONEY
            append(xf(fontId = 5, borderId = 1, numFmtId = 165, applyNumberFormat = true,
                applyBorder = true, alignment = "right"))                                           // 8 MONEY_NEGATIVE
            append(xf(fontId = 5, borderId = 1, numFmtId = 3, applyNumberFormat = true,
                applyBorder = true, alignment = "right"))                                           // 9 INTEGER
            append(xf(fontId = 5, borderId = 1, numFmtId = 167, applyNumberFormat = true,
                applyBorder = true, alignment = "right"))                                           // 10 PERCENT
            append(xf(fontId = 5, borderId = 1, numFmtId = 166, applyNumberFormat = true,
                applyBorder = true, alignment = "center"))                                          // 11 DATE
            append(xf(fontId = 10, fillId = 4, borderId = 2, applyFill = true, applyBorder = true,
                alignment = "left"))                                                                // 12 TOTAL_TEXT
            append(xf(fontId = 10, fillId = 4, borderId = 2, numFmtId = 165, applyNumberFormat = true,
                applyFill = true, applyBorder = true, alignment = "right"))                         // 13 TOTAL_MONEY
            append(xf(fontId = 10, fillId = 4, borderId = 2, numFmtId = 3, applyNumberFormat = true,
                applyFill = true, applyBorder = true, alignment = "right"))                         // 14 TOTAL_INTEGER
            append(xf(fontId = 8, fillId = 3, borderId = 1, applyFill = true, applyBorder = true,
                alignment = "left"))                                                                // 15 KPI_LABEL
            append(xf(fontId = 9, fillId = 3, borderId = 1, applyFill = true, applyBorder = true,
                alignment = "left"))                                                                // 16 KPI_VALUE
            append(xf(fontId = 7, fillId = 3, borderId = 1, applyFill = true, applyBorder = true,
                alignment = "left"))                                                                // 17 KPI_DETAIL
            append(xf(fontId = 7, alignment = "left", wrap = true))                                 // 18 NOTE
            append(xf(fontId = 8, alignment = "left"))                                              // 19 META_LABEL
            append(xf(fontId = 6, alignment = "left"))                                              // 20 META_VALUE
            append(xf(fontId = 11, alignment = "left"))                                             // 21 CHART_BAR
            append(xf(fontId = 5, borderId = 1, numFmtId = 168, applyNumberFormat = true,
                applyBorder = true, alignment = "right"))                                           // 22 NUMBER_DECIMAL
            append("</cellXfs>")

            append("<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>")
            append("<dxfs count=\"0\"/>")
            append("</styleSheet>")
        }
    }

    private fun font(
        sz: Int,
        color: Long,
        bold: Boolean,
        italic: Boolean = false,
        name: String = "Calibri",
    ): String = buildString {
        append("<font>")
        if (bold) append("<b/>")
        if (italic) append("<i/>")
        append("<sz val=\"$sz\"/>")
        append("<color rgb=\"${argb(color)}\"/>")
        append("<name val=\"$name\"/>")
        append("<family val=\"2\"/>")
        append("<charset val=\"0\"/>")
        append("</font>")
    }

    private fun fill(color: Long): String =
        "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"${argb(color)}\"/>" +
            "<bgColor indexed=\"64\"/></patternFill></fill>"

    private fun border(thin: Long, topThin: Long, topStyle: String = "thin"): String =
        "<border>" +
            "<left style=\"thin\"><color rgb=\"${argb(thin)}\"/></left>" +
            "<right style=\"thin\"><color rgb=\"${argb(thin)}\"/></right>" +
            "<top style=\"$topStyle\"><color rgb=\"${argb(topThin)}\"/></top>" +
            "<bottom style=\"thin\"><color rgb=\"${argb(thin)}\"/></bottom>" +
            "<diagonal/>" +
            "</border>"

    private fun xf(
        fontId: Int = 0,
        fillId: Int = 0,
        borderId: Int = 0,
        numFmtId: Int = 0,
        applyNumberFormat: Boolean = numFmtId != 0,
        applyFill: Boolean = fillId != 0,
        applyBorder: Boolean = borderId != 0,
        alignment: String? = null,
        wrap: Boolean = false,
        vertical: String = "bottom",
    ): String = buildString {
        append("<xf numFmtId=\"$numFmtId\" fontId=\"$fontId\" fillId=\"$fillId\" borderId=\"$borderId\" xfId=\"0\"")
        if (applyNumberFormat) append(" applyNumberFormat=\"1\"")
        if (fontId != 0) append(" applyFont=\"1\"")
        if (applyFill) append(" applyFill=\"1\"")
        if (applyBorder) append(" applyBorder=\"1\"")
        if (alignment != null || wrap) append(" applyAlignment=\"1\"")
        if (alignment == null && !wrap) {
            append("/>")
        } else {
            append(">")
            append("<alignment horizontal=\"$alignment\" vertical=\"$vertical\"")
            if (wrap) append(" wrapText=\"1\"")
            append("/>")
            append("</xf>")
        }
    }

    // ------------------------------ Utilidades -----------------------------

    /** Numero de serie de Excel (dias desde 1899-12-30) para una fecha local. */
    private fun excelSerialDate(millis: Long): Double {
        val localDate = com.gis.supermercados.core.common.AppDateTime.toLocalDate(millis)
        return (localDate.toEpochDay() + EXCEL_EPOCH_OFFSET).toDouble()
    }

    /** Formatea numeros sin notacion cientifica y con punto decimal (invariante). */
    private fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        val formatted = String.format(Locale.US, "%.10f", value)
            .trimEnd('0')
            .trimEnd('.')
        return formatted.ifEmpty { "0" }
    }

    private fun argb(color: Long): String =
        String.format(Locale.US, "%08X", color and 0xFFFFFFFFL)

    private fun escape(text: String): String = buildString(text.length + 8) {
        for (char in text) {
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> if (char.code < 0x20 && char != '\t' && char != '\n' && char != '\r') {
                    append(' ')
                } else {
                    append(char)
                }
            }
        }
    }

    companion object {
        /** Nombres de hoja: maximo 31 caracteres y sin [ ] : * ? / \ */
        fun sanitizeSheetName(name: String, existing: List<String>): String {
            val cleaned = name.replace(Regex("[\\[\\]:*?/\\\\]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .ifBlank { "Hoja" }
            var candidate = cleaned.take(31)
            var suffix = 2
            while (existing.any { it.equals(candidate, ignoreCase = true) }) {
                val tail = " ($suffix)"
                candidate = cleaned.take((31 - tail.length).coerceAtLeast(1)) + tail
                suffix++
            }
            return candidate
        }

        private const val EXCEL_EPOCH_OFFSET = 25569L // 1970-01-01 en serie Excel

        private const val XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        private const val NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        private const val NS_R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
        private const val NS_CONTENT_TYPES = "http://schemas.openxmlformats.org/package/2006/content-types"
        private const val NS_RELATIONSHIPS = "http://schemas.openxmlformats.org/package/2006/relationships"
        private const val NS_CORE_PROPS = "http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
        private const val NS_DC = "http://purl.org/dc/elements/1.1/"
        private const val NS_DCTERMS = "http://purl.org/dc/terms/"
        private const val NS_EXTENDED_PROPS = "http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"
        private const val NS_VT = "http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"

        private const val CT_RELATIONSHIPS = "application/vnd.openxmlformats-package.relationships+xml"
        private const val CT_WORKBOOK = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"
        private const val CT_WORKSHEET = "application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"
        private const val CT_STYLES = "application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"
        private const val CT_SHARED_STRINGS = "application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"
        private const val CT_CORE = "application/vnd.openxmlformats-package.core-properties+xml"
        private const val CT_APP = "application/vnd.openxmlformats-officedocument.extended-properties+xml"

        private const val REL_OFFICE_DOCUMENT = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"
        private const val REL_CORE_PROPERTIES = "http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties"
        private const val REL_EXTENDED_PROPERTIES = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties"
        private const val REL_WORKSHEET = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
        private const val REL_STYLES = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles"
        private const val REL_SHARED_STRINGS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings"

        private const val PRIMARY = 0xFF0B6E4FL
        private const val PRIMARY_DARK = 0xFF074536L
        private const val PRIMARY_CONTAINER = 0xFFD9F0E6L
        private const val SURFACE_VARIANT = 0xFFEFF3F1L
        private const val OUTLINE = 0xFFD5DEDA
        private const val TEXT = 0xFF111A17L
        private const val TEXT_SECONDARY = 0xFF53615CL
        private const val TEXT_TERTIARY = 0xFF7C8A85L
        private const val WHITE = 0xFFFFFFFFL
    }
}
