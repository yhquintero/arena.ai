package com.gis.supermercados.core.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.logging.AppLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Trabajo periodico que crea una copia de seguridad automatica cifrada.
 * Se ejecuta en segundo plano, sin red y sin intervencion del usuario.
 */
@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupManager: BackupManager,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        AppLogger.i(TAG, "Iniciando copia de seguridad automatica")
        return try {
            when (val outcome = backupManager.createBackup(passphrase = null)) {
                is AppResult.Success -> {
                    AppLogger.i(TAG, "Copia automatica completada: ${outcome.data.fileName}")
                    Result.success()
                }
                is AppResult.Failure -> {
                    AppLogger.w(TAG, "Copia automatica fallida: ${outcome.error}")
                    // Reintentos limitados para no consumir bateria inutilmente.
                    if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
                }
            }
        } catch (error: Exception) {
            AppLogger.e(TAG, "Error inesperado en la copia automatica", error)
            Result.failure()
        }
    }

    private companion object {
        const val TAG = "AutoBackupWorker"
        const val MAX_ATTEMPTS = 2
    }
}
