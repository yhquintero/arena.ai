package com.gis.supermercados.data.repository

import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppConstants
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.common.runCatchingApp
import com.gis.supermercados.data.local.GisDatabase
import com.gis.supermercados.data.local.entity.AppSettingEntity
import com.gis.supermercados.data.mapper.toEntities
import com.gis.supermercados.data.mapper.toSettings
import com.gis.supermercados.di.IoDispatcher
import com.gis.supermercados.domain.model.AppSettings
import com.gis.supermercados.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Ajustes globales (clave/valor) y banderas de primera ejecucion. */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val database: GisDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SettingsRepository {

    private val dao get() = database.settingsDao()

    override fun observeSettings(): Flow<AppSettings> =
        dao.observeAll().map { list -> list.toSettings() }.flowOn(ioDispatcher)

    override suspend fun getSettings(): AppSettings =
        withContext(ioDispatcher) { dao.getAll().toSettings() }

    override suspend fun updateSettings(settings: AppSettings): AppResult<Unit> =
        runCatchingApp(TAG, UiText.of(R.string.settings_error_save)) {
            withContext(ioDispatcher) {
                dao.upsertAll(settings.toEntities())
                Unit
            }
        }

    override suspend fun markFirstRunCompleted() = withContext(ioDispatcher) {
        dao.upsert(AppSettingEntity(AppConstants.Keys.FIRST_RUN_COMPLETED, "true", System.currentTimeMillis()))
    }

    override suspend fun isFirstRunCompleted(): Boolean = withContext(ioDispatcher) {
        dao.getValue(AppConstants.Keys.FIRST_RUN_COMPLETED)?.toBoolean() ?: false
    }

    override suspend fun markSampleDataLoaded() = withContext(ioDispatcher) {
        dao.upsert(AppSettingEntity(AppConstants.Keys.SAMPLE_DATA_LOADED, "true", System.currentTimeMillis()))
    }

    override suspend fun isSampleDataLoaded(): Boolean = withContext(ioDispatcher) {
        dao.getValue(AppConstants.Keys.SAMPLE_DATA_LOADED)?.toBoolean() ?: false
    }

    private companion object {
        const val TAG = "SettingsRepository"

    }
}
