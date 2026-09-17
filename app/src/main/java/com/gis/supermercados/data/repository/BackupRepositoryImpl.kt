package com.gis.supermercados.data.repository

import android.net.Uri
import com.gis.supermercados.core.backup.BackupManager
import com.gis.supermercados.core.backup.BackupScheduler
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.di.IoDispatcher
import com.gis.supermercados.domain.model.BackupInfo
import com.gis.supermercados.domain.repository.BackupRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fachada del repositorio de respaldo: delega en [BackupManager] (archivo .gisbak)
 * y en [BackupScheduler] (copia automatica con WorkManager), y recuerda la
 * configuracion elegida en los ajustes de la aplicacion.
 */
@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val backupManager: BackupManager,
    private val backupScheduler: BackupScheduler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BackupRepository {

    override suspend fun createBackup(passphrase: String?): AppResult<BackupInfo> =
        backupManager.createBackup(passphrase)

    override suspend fun restoreBackup(filePath: String, passphrase: String?): AppResult<BackupInfo> {
        val file = File(filePath)
        if (!file.exists()) AppLogger.e(TAG, "No existe el archivo de respaldo: $filePath")
        return backupManager.restoreBackup(file, passphrase)
    }

    override fun listBackups(): List<BackupInfo> = backupManager.listBackups()

    override suspend fun deleteBackup(fileName: String): AppResult<Boolean> =
        backupManager.deleteBackup(fileName)

    override suspend fun exportBackup(fileName: String, destinationUri: String): AppResult<BackupInfo> =
        withContext(ioDispatcher) {
            when (val exported = backupManager.exportTo(Uri.parse(destinationUri), fileName)) {
                is AppResult.Failure -> exported
                is AppResult.Success -> {
                    val bytes = exported.data
                    val info = backupManager.listBackups().firstOrNull { it.fileName == fileName }
                        ?: BackupInfo(
                            fileName = fileName,
                            filePath = File(backupManager.backupsDir(), fileName).absolutePath,
                            sizeBytes = bytes,
                            createdAt = System.currentTimeMillis(),
                            encrypted = false,
                            protectedWithPassphrase = false,
                            databaseVersion = 0,
                            appVersion = ""
                        )
                    AppLogger.i(TAG, "Respaldo exportado a destino externo: $fileName ($bytes bytes)")
                    AppResult.Success(info)
                }
            }
        }

    override suspend fun importBackup(sourceUri: String): AppResult<BackupInfo> =
        backupManager.importFrom(Uri.parse(sourceUri))

    override fun configureAutoBackup(enabled: Boolean, everyHours: Int) {
        if (enabled) {
            backupScheduler.schedule(everyHours)
        } else {
            backupScheduler.cancel()
        }
        AppLogger.i(TAG, "Respaldo automatico ${if (enabled) "cada $everyHours h" else "desactivado"}")
    }

    override fun runBackupNow() {
        AppLogger.i(TAG, "Respaldo manual solicitado por el usuario")
        backupScheduler.runNow()
    }

    override fun restartApp() = backupManager.restartApp()

    private companion object {
        const val TAG = "BackupRepository"
    }
}
