package com.gis.supermercados.data.repository

import android.content.Context
import android.net.Uri
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppConstants
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.common.runCatchingApp
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.data.local.GisDatabase
import com.gis.supermercados.data.mapper.toDomain
import com.gis.supermercados.data.mapper.toEntity
import com.gis.supermercados.di.IoDispatcher
import com.gis.supermercados.domain.model.Expense
import com.gis.supermercados.domain.model.ExpenseFilter
import com.gis.supermercados.domain.repository.ExpenseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gastos operacionales. `storeId == null` significa gasto CORPORATIVO
 * (no imputable a una sucursal), que los informes tratan por separado.
 */
@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GisDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ExpenseRepository {

    private val dao get() = database.expenseDao()
    private val storeDao get() = database.storeDao()

    override fun observeExpenses(filter: ExpenseFilter): Flow<List<Expense>> =
        dao.observeExpenses(
            storeId = filter.storeId,
            category = filter.category?.name,
            from = filter.from,
            to = filter.to,
            query = filter.query?.trim()?.takeIf { it.isNotEmpty() },
            limit = filter.limit
        ).map { rows -> rows.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun getExpense(expenseId: Long): Expense? = withContext(ioDispatcher) {
        dao.getWithNames(expenseId)?.toDomain()
            ?: dao.getById(expenseId)?.toDomain()
    }

    override suspend fun saveExpense(expense: Expense): AppResult<Long> =
        runCatchingApp(TAG, UiText.of(R.string.expense_error_save)) {
            withContext(ioDispatcher) {
                val concept = expense.concept.trim()
                if (concept.isBlank()) {
                    throw IllegalArgumentException(context.getString(R.string.expense_error_concept_required))
                }
                if (expense.amountCents <= 0L) {
                    throw IllegalArgumentException(context.getString(R.string.expense_error_amount_required))
                }
                if (expense.taxCents < 0L) {
                    throw IllegalArgumentException(context.getString(R.string.expense_error_invalid_tax))
                }
                // La sucursal debe existir y permitir gastos (salvo corporativos).
                expense.storeId?.let { storeId ->
                    val store = storeDao.getById(storeId)
                        ?: throw IllegalArgumentException(context.getString(R.string.expense_error_store_not_found))
                    if (!store.isActive) {
                        throw IllegalArgumentException(context.getString(R.string.expense_error_store_inactive))
                    }
                    if (!store.allowExpenses) {
                        throw IllegalArgumentException(context.getString(R.string.expense_error_store_no_expenses))
                    }
                }

                val now = System.currentTimeMillis()
                val total = expense.amountCents + expense.taxCents
                val entity = expense.toEntity().copy(
                    concept = concept,
                    totalCents = total,
                    expenseDate = if (expense.expenseDate > 0L) expense.expenseDate else now,
                    createdAt = if (expense.id == 0L) now else expense.createdAt
                )
                val id = if (expense.id == 0L) dao.insert(entity) else {
                    dao.update(entity)
                    entity.id
                }
                AppLogger.i(
                    TAG,
                    "Gasto ${expense.category} ${Money.format(total)} " +
                        if (expense.storeId == null) "(corporativo)" else "(tienda ${expense.storeId})"
                )
                id
            }
        }

    override suspend fun deleteExpense(expenseId: Long): AppResult<Unit> =
        runCatchingApp(TAG, UiText.of(R.string.expense_error_delete)) {
            withContext(ioDispatcher) {
                val deleted = dao.deleteById(expenseId)
                if (deleted == 0) {
                    throw IllegalArgumentException(context.getString(R.string.expense_error_not_found))
                }
                AppLogger.w(TAG, "Gasto $expenseId eliminado")
                Unit
            }
        }

    override suspend fun sumBetween(start: Long, end: Long, storeId: Long?): Long =
        withContext(ioDispatcher) { dao.sumBetween(start, end, storeId) }

    override suspend fun countBetween(start: Long, end: Long): Int =
        withContext(ioDispatcher) { dao.countBetween(start, end) }

    override suspend fun attachReceipt(expenseId: Long, sourceUri: String): AppResult<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val expense = dao.getById(expenseId)
                    ?: throw IllegalArgumentException(context.getString(R.string.expense_error_not_found))
                val base = context.getExternalFilesDir(null) ?: context.filesDir
                val dir = File(base, AppConstants.DIR_RECEIPTS).apply { if (!exists()) mkdirs() }
                val target = File(dir, "justificante_${expenseId}_${System.currentTimeMillis()}${extensionOf(sourceUri)}")

                context.contentResolver.openInputStream(Uri.parse(sourceUri))?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("No se pudo leer el justificante seleccionado")

                dao.update(expense.copy(receiptPath = target.absolutePath))
                AppLogger.i(TAG, "Justificante adjunto al gasto $expenseId (${target.length()} bytes)")
                AppResult.Success(target.absolutePath)
            }.getOrElse { error ->
                AppLogger.e(TAG, "No se pudo adjuntar el justificante", error)
                AppResult.Failure(UiText.of(R.string.expense_error_receipt), error)
            }
        }

    private fun extensionOf(uri: String): String = runCatching {
        val path = Uri.parse(uri).lastPathSegment.orEmpty()
        val dot = path.lastIndexOf('.')
        if (dot >= 0) path.substring(dot).take(6) else ".jpg"
    }.getOrDefault(".jpg")

    private companion object {
        const val TAG = "ExpenseRepository"
    }
}
