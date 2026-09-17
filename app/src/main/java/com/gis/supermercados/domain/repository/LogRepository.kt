package com.gis.supermercados.domain.repository

import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.domain.model.AuditEntry
import com.gis.supermercados.domain.model.LogLevel
import com.gis.supermercados.domain.model.ReportRecord
import kotlinx.coroutines.flow.Flow

/** Log interno persistente e historial de reportes exportados. */
interface LogRepository {

    fun observeLogs(level: LogLevel?, query: String?): Flow<List<AuditEntry>>

    suspend fun recentLogs(limit: Int = 1_000): List<AuditEntry>

    suspend fun clearLogs(): AppResult<Int>

    /** Exporta el log a un archivo de texto para enviarlo a soporte tecnico. */
    suspend fun exportLogsToFile(): AppResult<String>

    fun observeReportRecords(limit: Int = 100): Flow<List<ReportRecord>>

    suspend fun saveReportRecord(record: ReportRecord): AppResult<Unit>
}
