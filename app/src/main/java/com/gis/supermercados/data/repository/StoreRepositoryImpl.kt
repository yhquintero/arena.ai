package com.gis.supermercados.data.repository

import android.content.Context
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.common.runCatchingApp
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.data.local.GisDatabase
import com.gis.supermercados.data.mapper.toDomain
import com.gis.supermercados.data.mapper.toEntity
import com.gis.supermercados.di.IoDispatcher
import com.gis.supermercados.domain.model.Store
import com.gis.supermercados.domain.repository.StoreRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** CRUD de sucursales con validacion de codigos unicos y baja logica. */
@Singleton
class StoreRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GisDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : StoreRepository {

    private val dao get() = database.storeDao()

    override fun observeStores(): Flow<List<Store>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override fun observeActiveStores(): Flow<List<Store>> =
        dao.observeActive().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override fun observeStore(storeId: Long): Flow<Store?> =
        dao.observeById(storeId).map { it?.toDomain() }.flowOn(ioDispatcher)

    override fun observeActiveStoreCount(): Flow<Int> = dao.observeActiveCount().flowOn(ioDispatcher)

    override suspend fun getStore(storeId: Long): Store? =
        withContext(ioDispatcher) { dao.getById(storeId)?.toDomain() }

    override suspend fun getActiveStores(): List<Store> =
        withContext(ioDispatcher) { dao.getActive().map { it.toDomain() } }

    override suspend fun saveStore(store: Store): AppResult<Long> =
        runCatchingApp(TAG, UiText.of(R.string.store_error_save)) {
            withContext(ioDispatcher) {
                val code = store.code.trim().uppercase()
                if (code.isBlank()) {
                    throw IllegalArgumentException(context.getString(R.string.store_error_code_required))
                }
                if (store.name.isBlank()) {
                    throw IllegalArgumentException(context.getString(R.string.store_error_name_required))
                }
                val duplicate = dao.getIdByCodeExcluding(code, store.id)
                if (duplicate != null) {
                    throw IllegalArgumentException(context.getString(R.string.store_error_code_taken, code))
                }

                val now = System.currentTimeMillis()
                val entity = store.toEntity().copy(
                    code = code,
                    createdAt = if (store.id == 0L) now else store.createdAt,
                    updatedAt = now
                )
                val id = if (store.id == 0L) {
                    dao.insert(entity).also { AppLogger.i(TAG, "Sucursal creada: ${entity.name} ($code)") }
                } else {
                    dao.update(entity)
                    AppLogger.i(TAG, "Sucursal actualizada: ${entity.name} ($code)")
                    entity.id
                }
                id
            }
        }

    override suspend fun setActive(storeId: Long, isActive: Boolean): AppResult<Unit> =
        runCatchingApp(TAG, UiText.of(R.string.store_error_update)) {
            withContext(ioDispatcher) {
                dao.setActive(storeId, isActive, System.currentTimeMillis())
                AppLogger.i(TAG, "Sucursal $storeId ${if (isActive) "activada" else "desactivada"}")
                Unit
            }
        }

    override suspend fun deleteStore(storeId: Long): AppResult<Unit> =
        runCatchingApp(TAG, UiText.of(R.string.store_error_delete)) {
            withContext(ioDispatcher) {
                val related = dao.relatedRecordsCount(storeId)
                if (related > 0) {
                    throw IllegalArgumentException(
                        context.getString(R.string.store_error_has_records, related)
                    )
                }
                dao.deleteById(storeId)
                AppLogger.w(TAG, "Sucursal $storeId eliminada definitivamente")
                Unit
            }
        }

    override suspend fun isCodeTaken(code: String, excludeId: Long): Boolean =
        withContext(ioDispatcher) { dao.getIdByCodeExcluding(code.trim().uppercase(), excludeId) != null }

    private companion object {
        const val TAG = "StoreRepository"
    }
}
