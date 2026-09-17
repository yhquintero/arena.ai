package com.gis.supermercados.domain.model

/** Metadatos de una copia de seguridad. */
data class BackupInfo(
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val createdAt: Long,
    val encrypted: Boolean,
    val protectedWithPassphrase: Boolean,
    val databaseVersion: Int,
    val appVersion: String,
    val records: Int = 0,
) {
    val readableSize: String
        get() = when {
            sizeBytes >= 1_048_576 -> String.format(java.util.Locale.US, "%.1f MB", sizeBytes / 1_048_576.0)
            sizeBytes >= 1_024 -> String.format(java.util.Locale.US, "%.0f KB", sizeBytes / 1_024.0)
            else -> "$sizeBytes B"
        }
}

/** Archivo exportado (reporte PDF/Excel/CSV o copia de seguridad). */
data class ExportedFile(
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val mimeType: String,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val readableSize: String
        get() = when {
            sizeBytes >= 1_048_576 -> String.format(java.util.Locale.US, "%.1f MB", sizeBytes / 1_048_576.0)
            sizeBytes >= 1_024 -> String.format(java.util.Locale.US, "%.0f KB", sizeBytes / 1_024.0)
            else -> "$sizeBytes B"
        }
}
