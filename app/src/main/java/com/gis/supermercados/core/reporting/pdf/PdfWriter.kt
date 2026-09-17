package com.gis.supermercados.core.reporting.pdf

import java.io.ByteArrayOutputStream
import java.text.Normalizer
import java.util.Locale
import java.util.zip.Deflater

/**
 * Escritor de PDF 1.7 minimalista y autosuficiente (sin librerias de terceros).
 *
 * Por que propio y no iText/PDFBox:
 *  - La app es 100% offline y el APK debe seguir siendo pequeno.
 *  - iText tiene licencia AGPL (incompatible con apps comerciales cerradas).
 *  - Solo necesitamos texto, tablas, rectangulos y graficos vectoriales.
 *
 * Genera un documento multipagina con fuentes Helvetica (WinAnsiEncoding, con
 * acentos y eñe), compresion Flate de los flujos de contenido y tabla xref valida.
 *
 * Sistema de coordenadas: origen ABAJO-IZQUIERDA (nativo de PDF), en puntos
 * (1 pt = 1/72 pulgada). A4 = 595.28 x 841.89 pt.
 */
class PdfWriter(
    val pageWidth: Float = 595.28f,
    val pageHeight: Float = 841.89f,
) {

    /** Fuentes base-14 disponibles (no requieren incrustarse en el archivo). */
    enum class PdfFont(val resourceName: String, val baseFont: String, val bold: Boolean) {
        REGULAR("F1", "Helvetica", false),
        BOLD("F2", "Helvetica-Bold", true),
        OBLIQUE("F3", "Helvetica-Oblique", false),
        BOLD_OBLIQUE("F4", "Helvetica-BoldOblique", true),
    }

    // Metadatos del documento (visibles en las propiedades del PDF)
    var title: String = ""
    var author: String = ""
    var subject: String = ""
    var creator: String = ""
    var keywords: String = ""

    private val pages = mutableListOf<ByteArrayOutputStream>()
    private var content = ByteArrayOutputStream()

    val pageCount: Int get() = pages.size

    init {
        startPage()
    }

    // ------------------------------- Paginas -------------------------------

    fun startPage() {
        content = ByteArrayOutputStream()
        pages.add(content)
    }

    // --------------------------- Estado grafico ----------------------------

    fun setFillColor(rgb: Long) {
        val (r, g, b) = splitRgb(rgb)
        content.writeAscii("${nf(r)} ${nf(g)} ${nf(b)} rg\n")
    }

    fun setStrokeColor(rgb: Long) {
        val (r, g, b) = splitRgb(rgb)
        content.writeAscii("${nf(r)} ${nf(g)} ${nf(b)} RG\n")
    }

    fun setLineWidth(width: Float) {
        content.writeAscii("${nf(width)} w\n")
    }

    fun setLineDash(dash: Float, gap: Float) {
        content.writeAscii("[${nf(dash)} ${nf(gap)}] 0 d\n")
    }

    fun resetLineDash() {
        content.writeAscii("[] 0 d\n")
    }

    /** Guarda/restaura el estado grafico (permite anidar transformaciones). */
    fun saveState() = content.writeAscii("q\n")
    fun restoreState() = content.writeAscii("Q\n")

    // ------------------------------ Dibujo --------------------------------

    fun rect(x: Float, y: Float, width: Float, height: Float, fill: Boolean = true, stroke: Boolean = false) {
        content.writeAscii("${nf(x)} ${nf(y)} ${nf(width)} ${nf(height)} re ${paintOp(fill, stroke)}\n")
    }

    fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
        content.writeAscii("${nf(x1)} ${nf(y1)} m ${nf(x2)} ${nf(y2)} l S\n")
    }

    /** Rectangulo con esquinas redondeadas (aproximacion con curvas de Bezier). */
    fun roundedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float,
        fill: Boolean = true,
        stroke: Boolean = false,
    ) {
        val r = radius.coerceAtMost(minOf(width, height) / 2f)
        val k = BEZIER_CIRCLE_FACTOR * r
        val sb = StringBuilder()
        sb.append("${nf(x + r)} ${nf(y)} m\n")
        sb.append("${nf(x + width - r)} ${nf(y)} l\n")
        sb.append("${nf(x + width - r + k)} ${nf(y)} ${nf(x + width)} ${nf(y + r - k)} ${nf(x + width)} ${nf(y + r)} c\n")
        sb.append("${nf(x + width)} ${nf(y + height - r)} l\n")
        sb.append("${nf(x + width)} ${nf(y + height - r + k)} ${nf(x + width - r + k)} ${nf(y + height)} ${nf(x + width - r)} ${nf(y + height)} c\n")
        sb.append("${nf(x + r)} ${nf(y + height)} l\n")
        sb.append("${nf(x + r - k)} ${nf(y + height)} ${nf(x)} ${nf(y + height - r + k)} ${nf(x)} ${nf(y + height - r)} c\n")
        sb.append("${nf(x)} ${nf(y + r)} l\n")
        sb.append("${nf(x)} ${nf(y + r - k)} ${nf(x + r - k)} ${nf(y)} ${nf(x + r)} ${nf(y)} c\n")
        sb.append("h\n")
        content.writeAscii(sb.toString())
        content.writeAscii(paintOp(fill, stroke) + "\n")
    }

    /** Poligono cerrado y relleno (area bajo una curva). */
    fun filledPolygon(points: List<Pair<Float, Float>>) {
        if (points.size < 3) return
        val sb = StringBuilder()
        points.forEachIndexed { index, (x, y) ->
            sb.append(if (index == 0) "${nf(x)} ${nf(y)} m\n" else "${nf(x)} ${nf(y)} l\n")
        }
        sb.append("h\nf\n")
        content.writeAscii(sb.toString())
    }

    /** Polilinea (grafico de lineas). */
    fun polyline(points: List<Pair<Float, Float>>, close: Boolean = false) {
        if (points.isEmpty()) return
        val sb = StringBuilder()
        points.forEachIndexed { index, (x, y) ->
            sb.append(if (index == 0) "${nf(x)} ${nf(y)} m\n" else "${nf(x)} ${nf(y)} l\n")
        }
        if (close) sb.append("h\n")
        content.writeAscii(sb.toString())
        content.writeAscii("S\n")
    }

    fun circle(centerX: Float, centerY: Float, radius: Float, fill: Boolean = true, stroke: Boolean = false) {
        val k = BEZIER_CIRCLE_FACTOR * radius
        val sb = StringBuilder()
        sb.append("${nf(centerX - radius)} ${nf(centerY)} m\n")
        sb.append("${nf(centerX - radius)} ${nf(centerY + k)} ${nf(centerX - k)} ${nf(centerY + radius)} ${nf(centerX)} ${nf(centerY + radius)} c\n")
        sb.append("${nf(centerX + k)} ${nf(centerY + radius)} ${nf(centerX + radius)} ${nf(centerY + k)} ${nf(centerX + radius)} ${nf(centerY)} c\n")
        sb.append("${nf(centerX + radius)} ${nf(centerY - k)} ${nf(centerX + k)} ${nf(centerY - radius)} ${nf(centerX)} ${nf(centerY - radius)} c\n")
        sb.append("${nf(centerX - k)} ${nf(centerY - radius)} ${nf(centerX - radius)} ${nf(centerY - k)} ${nf(centerX - radius)} ${nf(centerY)} c\n")
        sb.append("h\n")
        content.writeAscii(sb.toString())
        content.writeAscii(paintOp(fill, stroke) + "\n")
    }

    // ------------------------------- Texto --------------------------------

    /**
     * Dibuja texto en [x],[y] (y = linea base).
     * [align] admite 0 = izquierda, 0.5 = centro, 1 = derecha.
     */
    fun drawText(
        text: String,
        x: Float,
        y: Float,
        size: Float,
        font: PdfFont = PdfFont.REGULAR,
        color: Long = COLOR_BLACK,
        align: Float = 0f,
    ) {
        val clean = sanitize(text)
        if (clean.isEmpty()) return
        val width = textWidth(clean, size, font)
        val startX = when {
            align >= 1f -> x - width
            align > 0f -> x - width * align
            else -> x
        }
        setFillColor(color)
        content.writeAscii("BT /${font.resourceName} ${nf(size)} Tf 1 0 0 1 ${nf(startX)} ${nf(y)} Tm ")
        content.write(PDF_TEXT_OPEN.toInt())
        content.write(escapeText(clean))
        content.write(PDF_TEXT_CLOSE.toInt())
        content.writeAscii(" Tj ET\n")
    }

    /** Ancho del texto en puntos (necesario para ajustar columnas y saltos de linea). */
    fun textWidth(text: String, size: Float, font: PdfFont): Float {
        var total = 0
        for (char in sanitize(text)) total += charWidth(char, font)
        return total * size / 1000f
    }

    /** Parte el texto en lineas que caben en [maxWidth] puntos. */
    fun wrap(text: String, maxWidth: Float, size: Float, font: PdfFont): List<String> {
        val source = sanitize(text)
        if (source.isEmpty()) return emptyList()
        if (textWidth(source, size, font) <= maxWidth) return listOf(source)

        val lines = mutableListOf<String>()
        val current = StringBuilder()
        for (word in source.split(' ')) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (textWidth(candidate, size, font) <= maxWidth) {
                current.clear()
                current.append(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current.clear()
                // Palabra mas larga que el ancho disponible: se corta a la fuerza.
                if (textWidth(word, size, font) > maxWidth) {
                    val chunk = StringBuilder()
                    for (char in word) {
                        if (textWidth("$chunk$char", size, font) > maxWidth && chunk.isNotEmpty()) {
                            lines.add(chunk.toString())
                            chunk.clear()
                        }
                        chunk.append(char)
                    }
                    current.append(chunk)
                } else {
                    current.append(word)
                }
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }

    /** Recorta el texto con puntos suspensivos si no cabe. */
    fun ellipsize(text: String, maxWidth: Float, size: Float, font: PdfFont): String {
        val source = sanitize(text)
        if (textWidth(source, size, font) <= maxWidth) return source
        var end = source.length
        while (end > 1 && textWidth(source.take(end - 1) + "…", size, font) > maxWidth) end--
        return source.take((end - 1).coerceAtLeast(1)) + "…"
    }

    // ------------------------- Ensamblado del PDF --------------------------

    /** Construye el archivo PDF completo en memoria. */
    fun build(): ByteArray {
        val out = ByteArrayOutputStream()
        val offsets = HashMap<Int, Int>(pages.size * 2 + 8)

        fun write(bytes: ByteArray) = out.write(bytes, 0, bytes.size)
        fun write(text: String) = write(text.toByteArray(Charsets.ISO_8859_1))

        // Cabecera: se marcan los bytes altos para que los lectores traten el
        // archivo como binario (recomendado por la especificacion).
        write("%PDF-1.7\n")
        write(byteArrayOf(0x25.toByte(), 0xE3.toByte(), 0xCF.toByte(), 0xE3.toByte(), 0x0A))

        val fontCount = PdfFont.entries.size
        val firstFontObject = 3
        val firstPageObject = firstFontObject + fontCount
        val infoObject = firstPageObject + pages.size * 2

        fun beginObject(number: Int) {
            offsets[number] = out.size()
            write("$number 0 obj\n")
        }

        // 1: Catalogo
        beginObject(1)
        write("<< /Type /Catalog /Pages 2 0 R /Lang (es-ES) >>\nendobj\n")

        // 2: Arbol de paginas
        beginObject(2)
        val kids = (0 until pages.size).joinToString(" ") { "${firstPageObject + it * 2} 0 R" }
        write("<< /Type /Pages /Kids [$kids] /Count ${pages.size} >>\nendobj\n")

        // Fuentes
        PdfFont.entries.forEachIndexed { index, font ->
            beginObject(firstFontObject + index)
            write(
                "<< /Type /Font /Subtype /Type1 /BaseFont /${font.baseFont} " +
                    "/Encoding /WinAnsiEncoding >>\nendobj\n"
            )
        }

        // Paginas + flujos de contenido (comprimidos con Flate)
        pages.forEachIndexed { index, stream ->
            val pageObject = firstPageObject + index * 2
            val contentObject = pageObject + 1

            beginObject(pageObject)
            write(
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${nf(pageWidth)} ${nf(pageHeight)}] " +
                    "/Resources << /Font << " +
                    PdfFont.entries.joinToString(" ") { "/${it.resourceName} ${firstFontObject + it.ordinal} 0 R" } +
                    " >> /ProcSet [/PDF /Text] >> /Contents $contentObject 0 R >>\nendobj\n"
            )

            val raw = stream.toByteArray()
            val compressed = deflate(raw)
            val useCompression = compressed.size < raw.size
            val payload = if (useCompression) compressed else raw

            beginObject(contentObject)
            write(
                "<< /Length ${payload.size}" +
                    (if (useCompression) " /Filter /FlateDecode" else "") +
                    " >>\nstream\n"
            )
            write(payload)
            write("\nendstream\nendobj\n")
        }

        // Informacion del documento
        beginObject(infoObject)
        write(
            "<< /Title (${escapeInfo(title)}) /Author (${escapeInfo(author)}) " +
                "/Subject (${escapeInfo(subject)}) /Creator (${escapeInfo(creator)}) " +
                "/Keywords (${escapeInfo(keywords)}) " +
                "/Producer (GIS Report Engine 1.0) >>\nendobj\n"
        )

        // Tabla de referencias cruzadas
        val xrefOffset = out.size()
        val lastObject = infoObject
        write("xref\n0 ${lastObject + 1}\n")
        write("0000000000 65535 f \n")
        for (number in 1..lastObject) {
            val offset = offsets[number] ?: 0
            write(String.format(Locale.US, "%010d 00000 n \n", offset))
        }
        write("trailer\n<< /Size ${lastObject + 1} /Root 1 0 R /Info $infoObject 0 R >>\n")
        write("startxref\n$xrefOffset\n%%EOF\n")

        return out.toByteArray()
    }

    // --------------------------- Utilidades -------------------------------

    private fun deflate(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_SPEED)
        try {
            deflater.setInput(data)
            deflater.finish()
            val buffer = ByteArray(COMPRESS_BUFFER)
            val out = ByteArrayOutputStream(data.size / 2 + 64)
            while (!deflater.finished()) {
                val count = deflater.deflate(buffer)
                out.write(buffer, 0, count)
            }
            return out.toByteArray()
        } finally {
            deflater.end()
        }
    }

    private fun paintOp(fill: Boolean, stroke: Boolean): String = when {
        fill && stroke -> "B"
        fill -> "f"
        stroke -> "S"
        else -> "n"
    }

    private fun splitRgb(rgb: Long): Triple<Float, Float, Float> {
        val value = rgb.toInt()
        val r = ((value shr 16) and 0xFF) / 255f
        val g = ((value shr 8) and 0xFF) / 255f
        val b = (value and 0xFF) / 255f
        return Triple(r, g, b)
    }

    /** Elimina caracteres de control que romperian la cadena de texto PDF. */
    private fun sanitize(text: String): String =
        text.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ')

    /** Escapa ( ) \ y codifica a WinAnsi (un byte por caracter). */
    private fun escapeText(text: String): ByteArray {
        val encoded = encodeWinAnsi(text)
        val out = ByteArrayOutputStream(encoded.size + 8)
        for (byte in encoded) {
            when (byte) {
                PDF_TEXT_OPEN, PDF_TEXT_CLOSE, PDF_BACKSLASH -> {
                    out.write(PDF_BACKSLASH.toInt())
                    out.write(byte.toInt())
                }
                else -> out.write(byte.toInt())
            }
        }
        return out.toByteArray()
    }

    private fun escapeInfo(text: String): String {
        val encoded = encodeWinAnsi(sanitize(text))
        val sb = StringBuilder(encoded.size + 8)
        for (byte in encoded) {
            val char = (byte.toInt() and 0xFF).toChar()
            when (char) {
                '(', ')', '\\' -> sb.append('\\').append(char)
                else -> if (char.code >= 0x20) sb.append(char) else sb.append(' ')
            }
        }
        return sb.toString()
    }

    /** Convierte texto Unicode a bytes WinAnsiEncoding (Latin-1 extendido). */
    private fun encodeWinAnsi(text: String): ByteArray {
        val out = ByteArray(text.length)
        var index = 0
        for (char in text) {
            val code = char.code
            out[index++] = when {
                code in 0x20..0x7E -> code.toByte()
                code in 0xA0..0xFF -> code.toByte()
                WIN_ANSI_SPECIAL.containsKey(char) -> WIN_ANSI_SPECIAL.getValue(char).toByte()
                else -> {
                    // Intento final: quitar el acento y usar la letra base.
                    val base = Normalizer.normalize(char.toString(), Normalizer.Form.NFD)
                        .firstOrNull() ?: '?'
                    if (base.code in 0x20..0x7E) base.code.toByte() else QUESTION_MARK.toByte()
                }
            }
        }
        return out
    }

    /** Ancho del glifo en millesimas de punto (tablas AFM de Helvetica). */
    private fun charWidth(char: Char, font: PdfFont): Int {
        val table = if (font.bold) HELVETICA_BOLD_WIDTHS else HELVETICA_WIDTHS
        val code = char.code
        if (code in 32..126) return table[code - 32]

        WIN_ANSI_WIDTHS[char]?.let { return it }

        val base = Normalizer.normalize(char.toString(), Normalizer.Form.NFD).firstOrNull()
        if (base != null && base.code in 32..126) return table[base.code - 32]
        return DEFAULT_GLYPH_WIDTH
    }

    private fun ByteArrayOutputStream.writeAscii(text: String) {
        val bytes = text.toByteArray(Charsets.ISO_8859_1)
        write(bytes, 0, bytes.size)
    }

    private companion object {
        const val BEZIER_CIRCLE_FACTOR = 0.5523f
        const val COMPRESS_BUFFER = 8192
        const val DEFAULT_GLYPH_WIDTH = 556
        const val QUESTION_MARK = 63
        val PDF_TEXT_OPEN = 0x28.toByte()   // (
        val PDF_TEXT_CLOSE = 0x29.toByte()  // )
        val PDF_BACKSLASH = 0x5C.toByte()   // \

        /** Colores por defecto usados por el motor de informes. */
        val COLOR_BLACK = 0x101418L

        /** Caracteres especiales de WinAnsi (rango 0x80-0x9F). */
        val WIN_ANSI_SPECIAL: Map<Char, Int> = mapOf(
            '€' to 0x80, '‚' to 0x82, 'ƒ' to 0x83, '„' to 0x84, '…' to 0x85,
            '†' to 0x86, '‡' to 0x87, 'ˆ' to 0x88, '‰' to 0x89, 'Š' to 0x8A,
            '‹' to 0x8B, 'Œ' to 0x8C, 'Ž' to 0x8E, '‘' to 0x91, '’' to 0x92,
            '“' to 0x93, '”' to 0x94, '•' to 0x95, '–' to 0x96, '—' to 0x97,
            '˜' to 0x98, '™' to 0x99, 'š' to 0x9A, '›' to 0x9B, 'œ' to 0x9C,
            'ž' to 0x9E, 'Ÿ' to 0x9F,
        )

        /** Anchos AFM aproximados para glifos fuera de ASCII. */
        val WIN_ANSI_WIDTHS: Map<Char, Int> = mapOf(
            '€' to 556, '‚' to 222, 'ƒ' to 278, '„' to 333, '…' to 1000,
            '†' to 556, '‡' to 556, 'ˆ' to 333, '‰' to 1000, 'Š' to 667,
            '‹' to 333, 'Œ' to 1000, 'Ž' to 611, '‘' to 222, '’' to 222,
            '“' to 333, '”' to 333, '•' to 350, '–' to 556, '—' to 1000,
            '˜' to 333, '™' to 1000, 'š' to 500, '›' to 333, 'œ' to 944,
            'ž' to 500, 'Ÿ' to 667, ' ' to 278, '¡' to 333, '¿' to 611,
            '«' to 556, '»' to 556, 'º' to 365, 'ª' to 370, '·' to 278,
        )

        /** Helvetica (regular/oblique): anchos AFM para ASCII 32..126. */
        val HELVETICA_WIDTHS = intArrayOf(
            278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
            556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
            1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
            667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
            333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
            556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584
        )

        /** Helvetica-Bold: anchos AFM para ASCII 32..126. */
        val HELVETICA_BOLD_WIDTHS = intArrayOf(
            278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278,
            556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 333, 333, 584, 584, 584, 611,
            975, 722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611, 833, 722, 778,
            667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556,
            333, 556, 611, 556, 611, 556, 333, 611, 611, 278, 278, 556, 278, 889, 611, 611,
            611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500, 389, 280, 389, 584
        )
    }
}

/** Formatea numeros en notacion PDF (punto decimal, sin ceros sobrantes). */
internal fun nf(value: Float): String {
    if (value == 0f) return "0"
    if (value == value.toInt().toFloat()) return value.toInt().toString()
    return String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
}
