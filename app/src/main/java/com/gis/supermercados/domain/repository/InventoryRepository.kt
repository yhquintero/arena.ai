package com.gis.supermercados.domain.repository

import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.domain.model.InventoryMovement
import com.gis.supermercados.domain.model.InventoryValue
import com.gis.supermercados.domain.model.MovementRequest
import com.gis.supermercados.domain.model.MovementType
import com.gis.supermercados.domain.model.StockLevel
import com.gis.supermercados.domain.model.TransferRequest
import kotlinx.coroutines.flow.Flow

/** Control de existencias por tienda, alertas y movimientos. */
interface InventoryRepository {

    fun observeStock(storeId: Long?, query: String?, onlyLowStock: Boolean): Flow<List<StockLevel>>

    fun observeLowStock(limit: Int = 200): Flow<List<StockLevel>>

    fun observeLowStockCount(): Flow<Int>

    fun observeOutOfStockCount(): Flow<Int>

    suspend fun inventoryValue(storeId: Long?): InventoryValue

    /** Registra un movimiento y actualiza el stock de forma atomica. */
    suspend fun registerMovement(request: MovementRequest): AppResult<Long>

    /** Transfiere existencias entre dos sucursales (dos movimientos ligados). */
    suspend fun transferStock(request: TransferRequest): AppResult<Long>

    /** Asigna un producto a una tienda con stock inicial y umbrales. */
    suspend fun assignProductToStore(
        productId: Long,
        storeId: Long,
        initialQuantity: Int,
        minStock: Int,
        maxStock: Int = 0,
        userId: Long = 0L,
    ): AppResult<Unit>

    suspend fun setThresholds(
        productId: Long,
        storeId: Long,
        minStock: Int,
        maxStock: Int,
    ): AppResult<Unit>

    suspend fun getStock(productId: Long, storeId: Long): StockLevel?

    fun observeMovements(
        storeId: Long?,
        productId: Long?,
        type: MovementType?,
    ): Flow<List<InventoryMovement>>

    suspend fun movementsBetween(start: Long, end: Long, storeId: Long?): List<InventoryMovement>
}
