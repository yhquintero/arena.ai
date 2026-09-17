package com.gis.supermercados.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gis.supermercados.data.local.entity.AuditLogEntity
import com.gis.supermercados.data.local.entity.ReportLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO del log interno y del historial de reportes.
 * El log se mantiene acotado (purga automatica) para no crecer indefinidamente.
 */
@Dao
interface SystemLogDao {

    @Query(
        """
        SELECT * FROM audit_logs
        WHERE (:level IS NULL OR level = :level)
          AND (:query IS NULL OR message LIKE '%' || :query || '%' OR tag LIKE '%' || :query || '%')
        ORDER BY created_at DESC
        LIMIT :limit
        """
    )
    fun observeLogs(level: String?, query: String?, limit: Int = 500): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 1000): List<AuditLogEntity>

    @Insert
    suspend fun insertLog(log: AuditLogEntity): Long

    @Query("DELETE FROM audit_logs WHERE created_at < :olderThan")
    suspend fun purgeLogsOlderThan(olderThan: Long): Int

    @Query("DELETE FROM audit_logs WHERE id NOT IN (SELECT id FROM audit_logs ORDER BY created_at DESC LIMIT :keep)")
    suspend fun trimLogs(keep: Int): Int

    @Query("SELECT COUNT(*) FROM audit_logs")
    suspend fun countLogs(): Int

    @Query("DELETE FROM audit_logs")
    suspend fun clearLogs(): Int

    // ---- Historial de reportes ----

    @Query("SELECT * FROM report_logs ORDER BY created_at DESC LIMIT :limit")
    fun observeReportLogs(limit: Int = 100): Flow<List<ReportLogEntity>>

    @Insert
    suspend fun insertReportLog(log: ReportLogEntity): Long

    @Query("DELETE FROM report_logs WHERE created_at < :olderThan")
    suspend fun purgeReportLogs(olderThan: Long): Int
}
