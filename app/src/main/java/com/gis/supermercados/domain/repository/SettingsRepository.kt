package com.gis.supermercados.domain.repository

import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/** Configuracion global de la aplicacion. */
interface SettingsRepository {

    fun observeSettings(): Flow<AppSettings>

    suspend fun getSettings(): AppSettings

    suspend fun updateSettings(settings: AppSettings): AppResult<Unit>

    suspend fun markFirstRunCompleted()

    suspend fun isFirstRunCompleted(): Boolean

    suspend fun markSampleDataLoaded()

    suspend fun isSampleDataLoaded(): Boolean
}
