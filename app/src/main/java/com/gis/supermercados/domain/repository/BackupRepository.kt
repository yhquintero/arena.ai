package com.gis.supermercados.domain.repository

import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.domain.model.BackupInfo

/** Copia de seguridad local cifrada, restauracion y programacion automatica. */
interface BackupRepository {

    suspend fun createBackup(passphrase: String?): AppResult<BackupInfo>

    suspend fun restoreBackup(filePath: String, passphrase: String?): AppResult<BackupInfo>

    fun listBackups(): List<BackupInfo>

    suspend fun deleteBackup(fileName: String): AppResult<Boolean>

    /** Exporta la copia al destino elegido por el usuario (SAF). */
    suspend fun exportBackup(fileName: String, destinationUri: String): AppResult<BackupInfo>

    /** Importa un archivo .gisbak desde el almacenamiento del usuario. */
    suspend fun importBackup(sourceUri: String): AppResult<BackupInfo>

    fun configureAutoBackup(enabled: Boolean, everyHours: Int)

    fun runBackupNow()

    /** Reinicia la aplicacion despues de restaurar (obligatorio). */
    fun restartApp()
}
