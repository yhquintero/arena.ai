package com.gis.supermercados.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gis.supermercados.data.local.entity.StoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {

    @Query("SELECT * FROM stores ORDER BY is_active DESC, name ASC")
    fun observeAll(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM stores WHERE is_active = 1 ORDER BY name ASC")
    fun observeActive(): Flow<List<StoreEntity>>

    @Query("SELECT * FROM stores ORDER BY name ASC")
    suspend fun getAll(): List<StoreEntity>

    @Query("SELECT * FROM stores WHERE is_active = 1 ORDER BY name ASC")
    suspend fun getActive(): List<StoreEntity>

    @Query("SELECT * FROM stores WHERE id = :storeId")
    suspend fun getById(storeId: Long): StoreEntity?

    @Query("SELECT * FROM stores WHERE id = :storeId")
    fun observeById(storeId: Long): Flow<StoreEntity?>

    @Query("SELECT COUNT(*) FROM stores WHERE is_active = 1")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM stores WHERE is_active = 1")
    suspend fun countActive(): Int

    @Query("SELECT COUNT(*) FROM stores")
    suspend fun count(): Int

    @Query("SELECT code FROM stores")
    suspend fun getAllCodes(): List<String>

    @Query("SELECT id FROM stores WHERE UPPER(code) = UPPER(:code) AND id != :excludeId LIMIT 1")
    suspend fun getIdByCodeExcluding(code: String, excludeId: Long): Long?

    /** Registros vinculados a la tienda: impide borrar sucursales con historial. */
    @Query(
        """
        SELECT (SELECT COUNT(*) FROM sales WHERE store_id = :storeId) +
               (SELECT COUNT(*) FROM expenses WHERE store_id = :storeId) +
               (SELECT COUNT(*) FROM stocks WHERE store_id = :storeId) +
               (SELECT COUNT(*) FROM inventory_movements WHERE store_id = :storeId)
        """
    )
    suspend fun relatedRecordsCount(storeId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(store: StoreEntity): Long

    @Update
    suspend fun update(store: StoreEntity)

    @Query("UPDATE stores SET is_active = :isActive, updated_at = :now WHERE id = :storeId")
    suspend fun setActive(storeId: Long, isActive: Boolean, now: Long): Int

    @Query("DELETE FROM stores WHERE id = :storeId")
    suspend fun deleteById(storeId: Long): Int
}
