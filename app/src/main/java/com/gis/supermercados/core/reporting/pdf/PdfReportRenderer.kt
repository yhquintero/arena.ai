package com.gis.supermercados.core.reporting.pdf

import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.core.reporting.model.ReportBlock
import com.gis.supermercados.core.reporting.model.ReportDocument
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Convierte un [ReportDocument] en un archivo PDF con identidad corporativa.
 *
 * Se renderiza en DOS pasadas: la primera cuenta las paginas para poder
 * escribir "Pagina X de Y" en el pie, la segunda genera el documento final.
 * La paginacion es identica en ambas porque el pie no afecta al flujo del contenido.
 */
@Singleton
class PdfReportRenderer @Inject constructor() {

    private val tag = "PdfReportRenderer"

    /** Devuelve los bytes del PDF listo para guardar o compartir. */
    fun render(document: ReportDocument): ByteArray = try {
        val probe = compose(document, totalPages = null)
        val pageCount = probe.pageCount
        val writer = compose(document, totalPages = pageCount)
        val bytes = writer.build()
        AppLogger.i(tag, "PDF generado: ${document.title} ($pageCount paginas, ${bytes.size} bytes)")
        bytes
    } catch (error: Exception) {
        AppLogger.e(tag, "Fallo al generar el PDF", error)
        throw error
    }

    private fun compose(document: ReportDocument, totalPages: Int?): PdfWriter {
        val writer = PdfWriter()
        writer.title = document.title
        writer.author = document.companyName.ifBlank { "GIS Supermercados" }
        writer.subject = "${document.title} · ${document.periodLabel} · ${document.storeLabel}"
        writer.creator = "GIS Supermercados · Motor de informes"
        writer.keywords = "reporte,ventas,gastos,inventario,supermercado"

        val layout = PdfReportLayout(writer, document, totalPages)
        layout.start()

        document.blocks.forEach { block ->
            when (block) {
                is ReportBlock.SectionTitle -> layout.drawSectionTitle(block.text)
                is ReportBlock.Paragraph -> layout.drawParagraph(block.text, block.bold)
                is ReportBlock.KpiGrid -> layout.drawKpiGrid(block.items)
                is ReportBlock.Table -> layout.drawTable(block)
                is ReportBlock.Chart -> layout.drawChart(block)
                ReportBlock.PageBreak -> layout.newPage()
            }
        }

        layout.finish()
        return writer
    }
}
