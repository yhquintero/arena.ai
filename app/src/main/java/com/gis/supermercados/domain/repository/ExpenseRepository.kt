package com.gis.supermercados.domain.repository

import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.domain.model.Expense
import com.gis.supermercados.domain.model.ExpenseFilter
import kotlinx.coroutines.flow.Flow

/** CRUD de gastos operacionales (por tienda o corporativos). */
interface ExpenseRepository {

    fun observeExpenses(filter: ExpenseFilter): Flow<List<Expense>>

    suspend fun getExpense(expenseId: Long): Expense?

    suspend fun saveExpense(expense: Expense): AppResult<Long>

    suspend fun deleteExpense(expenseId: Long): AppResult<Unit>

    suspend fun sumBetween(start: Long, end: Long, storeId: Long?): Long

    suspend fun countBetween(start: Long, end: Long): Int

    /** Copia el justificante elegido por el usuario al almacenamiento privado. */
    suspend fun attachReceipt(expenseId: Long, sourceUri: String): AppResult<String>
}
