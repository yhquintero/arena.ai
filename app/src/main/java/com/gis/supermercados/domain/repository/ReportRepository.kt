package com.gis.supermercados.domain.repository

import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.reporting.model.ExportFormat
import com.gis.supermercados.core.reporting.model.ReportDocument
import com.gis.supermercados.core.reporting.model.ReportRequest
import com.gis.supermercados.domain.model.DashboardStats
import com.gis.supermercados.domain.model.ExportedFile

/** Generacion de informes economicos y exportacion multiformato. */
interface ReportRepository {

    /** Metricas de la pantalla de inicio para el rango indicado. */
    suspend fun dashboardStats(referenceMillis: Long = System.currentTimeMillis()): AppResult<DashboardStats>

    /** Construye el documento (datos + estructura) del informe solicitado. */
    suspend fun buildReport(request: ReportRequest): AppResult<ReportDocument>

    /** Renderiza y guarda el informe en el almacenamiento privado de la app. */
    suspend fun exportReport(
        document: ReportDocument,
        request: ReportRequest,
        format: ExportFormat,
    ): AppResult<ExportedFile>

    /** Copia un archivo ya exportado al destino elegido por el usuario (SAF). */
    suspend fun copyToUri(file: ExportedFile, destinationUri: String): AppResult<ExportedFile>
}
