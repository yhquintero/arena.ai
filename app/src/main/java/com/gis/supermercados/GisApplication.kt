package com.gis.supermercados

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.gis.supermercados.core.backup.BackupScheduler
import com.gis.supermercados.core.common.AppConstants
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.core.logging.LogSink
import com.gis.supermercados.data.local.seed.DatabaseSeeder
import com.gis.supermercados.domain.repository.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Punto de entrada de la aplicacion.
 *
 * Responsabilidades en el arranque:
 *  1. Conectar el log interno (Logcat + tabla `audit_logs`).
 *  2. Programar la copia de seguridad automatica segun los ajustes guardados.
 *  3. Cargar los datos de ejemplo si la base de datos esta vacia.
 *
 * Todo ocurre en segundo plano: el arranque visible sigue siendo instantaneo.
 */
@HiltAndroidApp
class GisApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var logSink: LogSink
    @Inject lateinit var seeder: DatabaseSeeder
    @Inject lateinit var backupScheduler: BackupScheduler
    @Inject lateinit var settingsRepository: SettingsRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        // 1) Log persistente disponible desde el primer instante.
        AppLogger.install(logSink)
        AppLogger.i(TAG, "Iniciando ${AppConstants.APP_SHORT_NAME} v${AppConstants.APP_VERSION_LABEL}")

        // 2) Backup automatico y 3) datos de ejemplo, sin bloquear el arranque.
        bootstrapScope.launch {
            runCatching {
                val settings = settingsRepository.getSettings()
                if (settings.autoBackupEnabled) {
                    backupScheduler.schedule(settings.autoBackupHours)
                } else {
                    backupScheduler.cancel()
                }
                AppLogger.d(TAG, "Ajustes cargados: backup=${settings.autoBackupEnabled}/${settings.autoBackupHours}h")
            }.onFailure { AppLogger.e(TAG, "No se pudo aplicar la configuracion de respaldo", it) }

            seeder.seedInBackground()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
    }

    private val bootstrapScope = CoroutineScope(SupervisorJob())

    private companion object {
        const val TAG = "GisApplication"
    }
}
