package com.gis.supermercados.data.repository

import android.content.Context
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppConstants
import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.common.runCatchingApp
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.data.local.GisDatabase
import com.gis.supermercados.data.local.entity.ReportLogEntity
import com.gis.supermercados.data.mapper.toDomain
import com.gis.supermercados.di.IoDispatcher
import com.gis.supermercados.domain.model.AuditEntry
import com.gis.supermercados.domain.model.LogLevel
import com.gis.supermercados.domain.model.ReportRecord
import com.gis.supermercados.domain.repository.LogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lectura del log interno y exportacion a texto plano (util para enviar el
 * diagnostico por correo sin compartir la base de datos completa).
 */
@Singleton
class LogRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GisDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LogRepository {

    private val dao get() = database.systemLogDao()

    override fun observeLogs(level: LogLevel?, query: String?): Flow<List<AuditEntry>> =
        dao.observeLogs(level?.name, query?.trim()?.takeIf { it.isNotEmpty() })
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun recentLogs(limit: Int): List<AuditEntry> =
        withContext(ioDispatcher) { dao.getRecentLogs(limit).map { it.toDomain() } }

    override suspend fun clearLogs(): AppResult<Int> =
        runCatchingApp(TAG, UiText.of(R.string.logs_error_clear)) {
            withContext(ioDispatcher) {
                val deleted = dao.clearLogs()
                AppLogger.w(TAG, "Registro interno borrado ($deleted entradas)")
                deleted
            }
        }

    override suspend fun exportLogsToFile(): AppResult<String> = withContext(Dispatchers.IO) {
        runCatching {
            val logs = dao.getRecentLogs(AppConstants.LOG_EXPORT_LIMIT)
            val base = context.getExternalFilesDir(null) ?: context.filesDir
            val dir = File(base, AppConstants.DIR_LOGS).apply { if (!exists()) mkdirs() }
            val file = File(
                dir,
                "registro_${AppDateTime.formatIso(System.currentTimeMillis()).replace("-", "")}.txt"
            )
            file.bufferedWriter().use { writer ->
                writer.write("Registro interno - Gestion Integral Supermercados")
                writer.newLine()
                writer.write("Exportado: ${AppDateTime.formatDateTime(System.currentTimeMillis())}")
                writer.newLine()
                writer.write("Entradas: ${logs.size}")
                writer.newLine()
                writer.newLine()
                logs.forEach { log ->
                    writer.write(
                        "${AppDateTime.formatDateTime(log.createdAt)} | ${log.level.name.padEnd(5)} | " +
                            "${log.tag.padEnd(24)} | ${log.message}"
                    )
                    writer.newLine()
                }
            }
            AppResult.Success(file.absolutePath)
        }.getOrElse { error ->
            AppLogger.e(TAG, "No se pudo exportar el registro interno", error)
            AppResult.Failure(UiText.of(R.string.logs_error_export), error)
        }
    }

    override fun observeReportRecords(limit: Int): Flow<List<ReportRecord>> =
        dao.observeReportLogs(limit).map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun saveReportRecord(record: ReportRecord): AppResult<Unit> =
        runCatchingApp(TAG, UiText.of(R.string.logs_error_save)) {
            withContext(ioDispatcher) {
                dao.insertReportLog(
                    ReportLogEntity(
                        reportType = record.reportType,
                        periodType = record.periodType,
                        startMillis = record.startMillis,
                        endMillis = record.endMillis,
                        format = record.format,
                        fileName = record.fileName,
                        filePath = record.filePath,
                        sizeBytes = record.sizeBytes,
                        storeId = record.storeId,
                        createdAt = record.createdAt
                    )
                )
                Unit
            }
        }

    private companion object {
        const val TAG = "LogRepository"
    }
}
