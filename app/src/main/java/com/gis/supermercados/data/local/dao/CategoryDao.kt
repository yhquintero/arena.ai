package com.gis.supermercados.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gis.supermercados.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY sort_order ASC, name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE is_active = 1 ORDER BY sort_order ASC, name ASC")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sort_order ASC, name ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getById(categoryId: Long): CategoryEntity?

    @Query("SELECT id FROM categories WHERE name = :name LIMIT 1")
    suspend fun getIdByName(name: String): Long?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>): List<Long>

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteById(categoryId: Long): Int
}
