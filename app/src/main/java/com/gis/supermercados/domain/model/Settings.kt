package com.gis.supermercados.domain.model

import com.gis.supermercados.core.common.AppConstants

/** Configuracion global de la aplicacion (persistida como clave/valor). */
data class AppSettings(
    val businessName: String = AppConstants.APP_NAME,
    val businessLegalName: String = "",
    val businessTaxId: String = "",
    val businessAddress: String = "",
    val businessPhone: String = "",
    val currencySymbol: String = AppConstants.DEFAULT_CURRENCY_SYMBOL,
    val defaultTaxRate: Double = AppConstants.DEFAULT_TAX_RATE,
    val lowStockThreshold: Int = AppConstants.DEFAULT_LOW_STOCK,
    val autoBackupEnabled: Boolean = true,
    val autoBackupHours: Int = 24,
    val backupEncrypted: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SISTEMA,
    val dynamicColors: Boolean = false,
    val requireAuthOnStart: Boolean = true,
    val biometricEnabled: Boolean = false,
    val ticketPrefix: String = "V",
    val creditNotePrefix: String = "NC",
) {
    /** Tasa de impuesto en porcentaje (para mostrar "16 %" en la UI). */
    val taxPercent: Double get() = defaultTaxRate * 100.0

    companion object {
        val Default = AppSettings()
    }
}

/** Entrada del log interno de la aplicacion (debugging en dispositivo). */
data class AuditEntry(
    val id: Long = 0L,
    val level: LogLevel = LogLevel.INFO,
    val tag: String = "",
    val message: String = "",
    val screen: String = "",
    val createdAt: Long = 0L,
)

/** Historial de reportes generados/exportados. */
data class ReportRecord(
    val id: Long = 0L,
    val reportType: String = "",
    val periodType: String = "",
    val startMillis: Long = 0L,
    val endMillis: Long = 0L,
    val format: String = "",
    val fileName: String = "",
    val filePath: String = "",
    val sizeBytes: Long = 0L,
    val storeId: Long? = null,
    val userName: String = "",
    val createdAt: Long = 0L,
)
