package com.gis.supermercados.core.backup

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gis.supermercados.core.common.AppConstants
import com.gis.supermercados.core.logging.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Programa o cancela la copia de seguridad automatica (WorkManager). */
@Singleton
class BackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val workManager: WorkManager by lazy { WorkManager.getInstance(context) }

    /** Activa (o actualiza) la periodicidad del backup automatico. */
    fun schedule(everyHours: Int) {
        val hours = everyHours.coerceIn(MIN_HOURS, MAX_HOURS).toLong()
        runCatching {
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(hours, TimeUnit.HOURS)
                .setInitialDelay(hours, TimeUnit.HOURS)
                .build()
            workManager.enqueueUniquePeriodicWork(
                AppConstants.BACKUP_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            AppLogger.i(TAG, "Backup automatico programado cada $hours h")
        }.onFailure { AppLogger.e(TAG, "No se pudo programar el backup automatico", it) }
    }

    fun cancel() {
        runCatching {
            workManager.cancelUniqueWork(AppConstants.BACKUP_WORK_NAME)
            AppLogger.i(TAG, "Backup automatico cancelado")
        }.onFailure { AppLogger.e(TAG, "No se pudo cancelar el backup automatico", it) }
    }

    /** Lanza una copia inmediata (boton "Hacer copia ahora"). */
    fun runNow() {
        runCatching {
            workManager.enqueue(OneTimeWorkRequestBuilder<AutoBackupWorker>().build())
            AppLogger.i(TAG, "Copia manual en segundo plano solicitada")
        }.onFailure { AppLogger.e(TAG, "No se pudo lanzar la copia manual", it) }
    }

    private companion object {
        const val TAG = "BackupScheduler"
        const val MIN_HOURS = 6
        const val MAX_HOURS = 168
    }
}
