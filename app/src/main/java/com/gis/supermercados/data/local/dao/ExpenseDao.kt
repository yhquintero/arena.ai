package com.gis.supermercados.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gis.supermercados.data.local.entity.ExpenseDetailRow
import com.gis.supermercados.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query(
        """
        SELECT e.*, st.name AS store_name, IFNULL(u.full_name, '') AS user_name
        FROM expenses e
        LEFT JOIN stores st ON st.id = e.store_id
        LEFT JOIN users u ON u.id = e.user_id
        WHERE (:storeId IS NULL OR e.store_id = :storeId)
          AND (:category IS NULL OR e.category = :category)
          AND (:from IS NULL OR e.expense_date >= :from)
          AND (:to IS NULL OR e.expense_date <= :to)
          AND (:query IS NULL OR e.concept LIKE '%' || :query || '%'
               OR e.provider LIKE '%' || :query || '%'
               OR e.reference LIKE '%' || :query || '%')
        ORDER BY e.expense_date DESC, e.id DESC
        LIMIT :limit
        """
    )
    fun observeExpenses(
        storeId: Long?,
        category: String?,
        from: Long?,
        to: Long?,
        query: String?,
        limit: Int = 500,
    ): Flow<List<ExpenseDetailRow>>

    @Query(
        """
        SELECT e.*, st.name AS store_name, IFNULL(u.full_name, '') AS user_name
        FROM expenses e
        LEFT JOIN stores st ON st.id = e.store_id
        LEFT JOIN users u ON u.id = e.user_id
        WHERE e.id = :expenseId
        """
    )
    suspend fun getWithNames(expenseId: Long): ExpenseDetailRow?

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getById(expenseId: Long): ExpenseEntity?

    @Query("SELECT IFNULL(SUM(total_cents), 0) FROM expenses WHERE expense_date BETWEEN :start AND :end AND (:storeId IS NULL OR store_id = :storeId)")
    suspend fun sumBetween(start: Long, end: Long, storeId: Long?): Long

    @Query("SELECT COUNT(*) FROM expenses WHERE expense_date BETWEEN :start AND :end")
    suspend fun countBetween(start: Long, end: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(expenses: List<ExpenseEntity>): List<Long>

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteById(expenseId: Long): Int
}
