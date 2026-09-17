package com.gis.supermercados.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.common.runCatchingApp
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.data.local.GisDatabase
import com.gis.supermercados.data.local.entity.InventoryMovementEntity
import com.gis.supermercados.data.local.entity.StockEntity
import com.gis.supermercados.data.mapper.toDomain
import com.gis.supermercados.di.IoDispatcher
import com.gis.supermercados.domain.model.InventoryMovement
import com.gis.supermercados.domain.model.InventoryValue
import com.gis.supermercados.domain.model.MovementRequest
import com.gis.supermercados.domain.model.MovementType
import com.gis.supermercados.domain.model.StockLevel
import com.gis.supermercados.domain.model.TransferRequest
import com.gis.supermercados.domain.repository.InventoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Existencias por sucursal, alertas y movimientos.
 *
 * Cada movimiento se escribe EN LA MISMA TRANSACCION que el ajuste de
 * existencias: el historial nunca puede desviarse del stock real.
 */
@Singleton
class InventoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GisDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : InventoryRepository {

    private val stockDao get() = database.stockDao()
    private val movementDao get() = database.inventoryMovementDao()
    private val productDao get() = database.productDao()
    private val storeDao get() = database.storeDao()

    override fun observeStock(storeId: Long?, query: String?, onlyLowStock: Boolean): Flow<List<StockLevel>> =
        stockDao.observeStock(storeId, query?.trim()?.takeIf { it.isNotEmpty() }, onlyLowStock)
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeLowStock(limit: Int): Flow<List<StockLevel>> =
        stockDao.observeLowStock(limit).map { rows -> rows.map { it.toDomain() } }.flowOn(ioDispatcher)

    override fun observeLowStockCount(): Flow<Int> = stockDao.observeLowStockCount().flowOn(ioDispatcher)

    override fun observeOutOfStockCount(): Flow<Int> = stockDao.observeOutOfStockCount().flowOn(ioDispatcher)

    override suspend fun inventoryValue(storeId: Long?): InventoryValue =
        withContext(ioDispatcher) { stockDao.inventoryValue(storeId).toDomain() }

    override suspend fun getStock(productId: Long, storeId: Long): StockLevel? =
        withContext(ioDispatcher) {
            val stock = stockDao.get(productId, storeId) ?: return@withContext null
            val product = productDao.getById(productId)
            val store = storeDao.getById(storeId)
            stock.toDomain().copy(
                productName = product?.name.orEmpty(),
                sku = product?.sku.orEmpty(),
                storeName = store?.name.orEmpty(),
                costCents = product?.costCents ?: 0L,
                priceCents = product?.priceCents ?: 0L
            )
        }

    override fun observeMovements(
        storeId: Long?,
        productId: Long?,
        type: MovementType?,
    ): Flow<List<InventoryMovement>> =
        movementDao.observeMovements(storeId, productId, type?.name)
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun movementsBetween(start: Long, end: Long, storeId: Long?): List<InventoryMovement> =
        withContext(ioDispatcher) {
            movementDao.getMovementsBetween(start, end, storeId).map { it.toDomain() }
        }

    override suspend fun registerMovement(request: MovementRequest): AppResult<Long> =
        runCatchingApp(TAG, UiText.of(R.string.inventory_error_movement)) {
            withContext(ioDispatcher) {
                database.withTransaction {
                    val now = System.currentTimeMillis()
                    val product = productDao.getById(request.productId)
                        ?: throw IllegalArgumentException(context.getString(R.string.inventory_error_product_not_found))
                    val store = storeDao.getById(request.storeId)
                        ?: throw IllegalArgumentException(context.getString(R.string.inventory_error_store_not_found))
                    if (!store.isActive) {
                        throw IllegalArgumentException(context.getString(R.string.inventory_error_store_inactive))
                    }
                    if (request.quantity <= 0) {
                        throw IllegalArgumentException(context.getString(R.string.inventory_error_invalid_quantity))
                    }

                    val stock = stockDao.get(request.productId, request.storeId)
                    val currentQuantity = stock?.quantity ?: 0
                    val delta = if (request.type.increasesStock) request.quantity else -request.quantity
                    val newQuantity = currentQuantity + delta
                    if (newQuantity < 0) {
                        throw IllegalArgumentException(
                            context.getString(
                                R.string.inventory_error_insufficient_stock,
                                product.name,
                                currentQuantity.toString()
                            )
                        )
                    }
                    val updated = (stock ?: StockEntity(
                        productId = request.productId,
                        storeId = request.storeId,
                        minStock = DEFAULT_MIN_STOCK
                    )).copy(quantity = newQuantity, updatedAt = now)
                    stockDao.upsert(updated)

                    val movementId = movementDao.insert(
                        InventoryMovementEntity(
                            productId = request.productId,
                            storeId = request.storeId,
                            destinationStoreId = request.destinationStoreId,
                            type = request.type,
                            quantity = request.quantity,
                            unitCostCents = request.unitCostCents.takeIf { it > 0L } ?: product.costCents,
                            reason = request.reason.trim(),
                            reference = request.reference.trim(),
                            userId = request.userId,
                            createdAt = now
                        )
                    )
                    AppLogger.i(
                        TAG,
                        "Movimiento ${request.type} de ${product.sku} en ${store.code}: " +
                            "$currentQuantity -> $newQuantity"
                    )
                    movementId
                }
            }
        }

    override suspend fun transferStock(request: TransferRequest): AppResult<Long> =
        runCatchingApp(TAG, UiText.of(R.string.inventory_error_transfer)) {
            withContext(ioDispatcher) {
                database.withTransaction {
                    val now = System.currentTimeMillis()
                    if (request.fromStoreId == request.toStoreId) {
                        throw IllegalArgumentException(context.getString(R.string.inventory_error_same_store))
                    }
                    if (request.quantity <= 0) {
                        throw IllegalArgumentException(context.getString(R.string.inventory_error_invalid_quantity))
                    }
                    val product = productDao.getById(request.productId)
                        ?: throw IllegalArgumentException(context.getString(R.string.inventory_error_product_not_found))

                    val origin = stockDao.get(request.productId, request.fromStoreId)
                    if (origin == null || origin.quantity < request.quantity) {
                        throw IllegalArgumentException(
                            context.getString(
                                R.string.inventory_error_insufficient_stock,
                                product.name,
                                (origin?.quantity ?: 0).toString()
                            )
                        )
                    }
                    val destination = stockDao.get(request.productId, request.toStoreId)

                    stockDao.upsert(origin.copy(quantity = origin.quantity - request.quantity, updatedAt = now))
                    if (destination == null) {
                        stockDao.insert(
                            StockEntity(
                                productId = request.productId,
                                storeId = request.toStoreId,
                                quantity = request.quantity,
                                minStock = origin.minStock,
                                maxStock = origin.maxStock,
                                updatedAt = now
                            )
                        )
                    } else {
                        stockDao.upsert(
                            destination.copy(quantity = destination.quantity + request.quantity, updatedAt = now)
                        )
                    }

                    // Un unico registro cubre ambas sucursales: la consulta filtra
                    // por store_id O destination_store_id.
                    val movementId = movementDao.insert(
                        InventoryMovementEntity(
                            productId = request.productId,
                            storeId = request.fromStoreId,
                            destinationStoreId = request.toStoreId,
                            type = MovementType.TRANSFERENCIA,
                            quantity = request.quantity,
                            unitCostCents = product.costCents,
                            reason = request.reason.trim(),
                            reference = request.reference.trim(),
                            userId = request.userId,
                            createdAt = now
                        )
                    )
                    AppLogger.i(
                        TAG,
                        "Transferencia de ${request.quantity} x ${product.sku} " +
                            "de la tienda ${request.fromStoreId} a la ${request.toStoreId}"
                    )
                    movementId
                }
            }
        }

    override suspend fun assignProductToStore(
        productId: Long,
        storeId: Long,
        initialQuantity: Int,
        minStock: Int,
        maxStock: Int,
        userId: Long,
    ): AppResult<Unit> = runCatchingApp(TAG, UiText.of(R.string.inventory_error_update)) {
        withContext(ioDispatcher) {
            database.withTransaction {
                val now = System.currentTimeMillis()
                if (productDao.getById(productId) == null) {
                    throw IllegalArgumentException(context.getString(R.string.inventory_error_product_not_found))
                }
                if (initialQuantity < 0 || minStock < 0 || maxStock < 0) {
                    throw IllegalArgumentException(context.getString(R.string.inventory_error_invalid_quantity))
                }
                val existing = stockDao.get(productId, storeId)
                if (existing == null) {
                    stockDao.insert(
                        StockEntity(
                            productId = productId,
                            storeId = storeId,
                            quantity = initialQuantity,
                            minStock = minStock,
                            maxStock = maxStock,
                            updatedAt = now
                        )
                    )
                } else {
                    stockDao.upsert(
                        existing.copy(minStock = minStock, maxStock = maxStock, updatedAt = now)
                    )
                }
                if (existing == null && initialQuantity > 0) {
                    movementDao.insert(
                        InventoryMovementEntity(
                            productId = productId,
                            storeId = storeId,
                            type = MovementType.ENTRADA,
                            quantity = initialQuantity,
                            reason = context.getString(R.string.inventory_initial_stock_reason),
                            userId = userId,
                            createdAt = now
                        )
                    )
                }
            }
            Unit
        }
    }

    override suspend fun setThresholds(
        productId: Long,
        storeId: Long,
        minStock: Int,
        maxStock: Int,
    ): AppResult<Unit> = runCatchingApp(TAG, UiText.of(R.string.inventory_error_update)) {
        withContext(ioDispatcher) {
            if (minStock < 0 || maxStock < 0 || (maxStock > 0 && maxStock < minStock)) {
                throw IllegalArgumentException(context.getString(R.string.inventory_error_invalid_thresholds))
            }
            val updated = stockDao.updateThresholds(productId, storeId, minStock, maxStock, System.currentTimeMillis())
            if (updated == 0) {
                throw IllegalArgumentException(context.getString(R.string.inventory_error_stock_not_found))
            }
            Unit
        }
    }

    private companion object {
        const val TAG = "InventoryRepository"
        const val DEFAULT_MIN_STOCK = 10
    }
}
