package com.gis.supermercados.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gis.supermercados.data.local.entity.AppSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Query("SELECT * FROM app_settings")
    fun observeAll(): Flow<List<AppSettingEntity>>

    @Query("SELECT * FROM app_settings")
    suspend fun getAll(): List<AppSettingEntity>

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Upsert
    suspend fun upsert(setting: AppSettingEntity)

    @Upsert
    suspend fun upsertAll(settings: List<AppSettingEntity>)

    @Query("DELETE FROM app_settings WHERE `key` = :key")
    suspend fun delete(key: String): Int

    @Query("DELETE FROM app_settings")
    suspend fun clear(): Int
}
