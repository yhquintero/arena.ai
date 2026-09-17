package com.gis.supermercados.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gis.supermercados.domain.model.LogLevel

/** Ajustes clave/valor de la aplicacion (moneda, impuestos, backup, tema...). */
@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
)

/**
 * Log interno persistente para depurar en el propio dispositivo
 * (la app no depende de un servidor, asi que el diagnostico es local).
 */
@Entity(
    tableName = "audit_logs",
    indices = [Index(value = ["created_at"]), Index(value = ["level"])]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val level: LogLevel = LogLevel.INFO,
    val tag: String = "",
    val message: String = "",
    val screen: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
)

/** Historial de reportes generados y exportados (trazabilidad). */
@Entity(
    tableName = "report_logs",
    indices = [Index(value = ["created_at"]), Index(value = ["report_type"])]
)
data class ReportLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "report_type") val reportType: String,
    @ColumnInfo(name = "period_type") val periodType: String = "",
    @ColumnInfo(name = "start_millis") val startMillis: Long = 0L,
    @ColumnInfo(name = "end_millis") val endMillis: Long = 0L,
    val format: String = "",
    @ColumnInfo(name = "file_name") val fileName: String = "",
    @ColumnInfo(name = "file_path") val filePath: String = "",
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long = 0L,
    @ColumnInfo(name = "store_id") val storeId: Long? = null,
    @ColumnInfo(name = "user_id") val userId: Long = 0L,
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
)
