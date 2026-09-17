package com.gis.supermercados.core.reporting

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppConstants
import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.core.reporting.csv.CsvReportRenderer
import com.gis.supermercados.core.reporting.model.ExportFormat
import com.gis.supermercados.core.reporting.model.ReportDocument
import com.gis.supermercados.core.reporting.model.ReportRequest
import com.gis.supermercados.core.reporting.pdf.PdfReportRenderer
import com.gis.supermercados.core.reporting.xlsx.XlsxReportRenderer
import com.gis.supermercados.domain.model.ExportedFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda los informes generados en el almacenamiento privado de la aplicacion
 * (no requiere ningun permiso de almacenamiento) y permite:
 *  - abrirlos con visores externos (via FileProvider),
 *  - compartirlos por cualquier app del dispositivo,
 *  - copiarlos a una carpeta elegida por el usuario (Storage Access Framework).
 */
@Singleton
class ReportExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfRenderer: PdfReportRenderer,
    private val xlsxRenderer: XlsxReportRenderer,
    private val csvRenderer: CsvReportRenderer,
) {

    private val tag = "ReportExporter"

    fun reportsDir(): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, AppConstants.DIR_REPORTS)
            .apply { if (!exists()) mkdirs() }

    /** Renderiza el documento en el formato pedido y lo guarda en disco. */
    suspend fun export(
        document: ReportDocument,
        request: ReportRequest,
        format: ExportFormat,
    ): AppResult<ExportedFile> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = when (format) {
                ExportFormat.PDF -> pdfRenderer.render(document)
                ExportFormat.EXCEL -> xlsxRenderer.render(document)
                ExportFormat.CSV -> csvRenderer.render(document)
            }
            val fileName = buildFileName(document, request, format)
            val file = File(reportsDir(), fileName)
            file.writeBytes(bytes)
            AppLogger.i(tag, "Informe exportado: $fileName (${bytes.size} bytes)")
            AppResult.Success(
                ExportedFile(
                    fileName = fileName,
                    filePath = file.absolutePath,
                    sizeBytes = bytes.size.toLong(),
                    mimeType = format.mimeType
                )
            )
        }.getOrElse { error ->
            AppLogger.e(tag, "No se pudo exportar el informe", error)
            AppResult.Failure(UiText.of(R.string.report_error_export), error)
        }
    }

    /** Copia un archivo exportado al destino elegido por el usuario (SAF). */
    suspend fun copyToUri(file: ExportedFile, destinationUri: String): AppResult<ExportedFile> =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(destinationUri)
                val source = File(file.filePath)
                if (!source.exists()) throw IllegalStateException("Archivo origen no encontrado")
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    source.inputStream().use { input -> input.copyTo(output) }
                } ?: throw IllegalStateException("No se pudo abrir el destino")
                AppLogger.i(tag, "Informe copiado a destino elegido por el usuario")
                AppResult.Success(file)
            }.getOrElse { error ->
                AppLogger.e(tag, "Fallo al guardar el informe en el destino elegido", error)
                AppResult.Failure(UiText.of(R.string.report_error_copy), error)
            }
        }

    /** URI compartible (content://) para abrir o enviar el archivo. */
    fun contentUri(file: ExportedFile): Uri? = runCatching {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(file.filePath)
        )
    }.onFailure { AppLogger.e(tag, "No se pudo generar la URI compartible", it) }.getOrNull()

    /** Lista los informes exportados previamente (mas recientes primero). */
    fun listExported(): List<ExportedFile> = runCatching {
        reportsDir().listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                ExportedFile(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    sizeBytes = file.length(),
                    mimeType = mimeOf(file.name)
                )
            }
            .orEmpty()
    }.getOrDefault(emptyList())

    fun delete(fileName: String): Boolean {
        val file = File(reportsDir(), fileName)
        return file.exists() && file.delete()
    }

    /** Nombre de archivo legible, sin caracteres invalidos y con rango de fechas. */
    fun buildFileName(
        document: ReportDocument,
        request: ReportRequest,
        format: ExportFormat,
    ): String {
        val title = sanitize(document.title)
        val store = sanitize(document.storeLabel).ifBlank { "Todas" }
        val start = AppDateTime.formatIso(request.range.startMillis).replace("-", "")
        val end = AppDateTime.formatIso(request.range.endMillis).replace("-", "")
        val name = "GIS_${title}_${store}_$start-$end.${format.extension}"
        return name.take(MAX_FILE_NAME_LENGTH)
    }

    private fun sanitize(value: String): String = value
        .replace(Regex("[^A-Za-z0-9ÁÉÍÓÚÑáéíóúñ ]"), "")
        .trim()
        .replace(Regex("\\s+"), "_")
        .take(28)

    private fun mimeOf(fileName: String): String = when {
        fileName.endsWith(".pdf", true) -> ExportFormat.PDF.mimeType
        fileName.endsWith(".xlsx", true) -> ExportFormat.EXCEL.mimeType
        else -> ExportFormat.CSV.mimeType
    }

    private companion object {
        const val MAX_FILE_NAME_LENGTH = 120
    }
}
