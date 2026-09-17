package com.gis.supermercados.domain.repository

import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.domain.model.Store
import kotlinx.coroutines.flow.Flow

/** CRUD de sucursales, con horarios y permisos por tienda. */
interface StoreRepository {

    fun observeStores(): Flow<List<Store>>

    fun observeActiveStores(): Flow<List<Store>>

    fun observeStore(storeId: Long): Flow<Store?>

    fun observeActiveStoreCount(): Flow<Int>

    suspend fun getStore(storeId: Long): Store?

    suspend fun getActiveStores(): List<Store>

    /** Crea (id = 0) o actualiza la sucursal. Devuelve el id. */
    suspend fun saveStore(store: Store): AppResult<Long>

    /** Baja logica: conserva el historico de ventas y gastos asociado. */
    suspend fun setActive(storeId: Long, isActive: Boolean): AppResult<Unit>

    /** Borrado fisico: solo permitido si la tienda no tiene movimientos. */
    suspend fun deleteStore(storeId: Long): AppResult<Unit>

    suspend fun isCodeTaken(code: String, excludeId: Long = 0L): Boolean
}
