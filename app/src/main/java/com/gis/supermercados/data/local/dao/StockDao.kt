package com.gis.supermercados.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.gis.supermercados.data.local.entity.InventoryValueRow
import com.gis.supermercados.data.local.entity.StockDetailRow
import com.gis.supermercados.data.local.entity.StockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {

    /** Existencias de una tienda (o de todas si [storeId] es null). */
    @Query(
        """
        SELECT st.id AS stock_id, st.product_id AS product_id, st.store_id AS store_id,
               p.name AS product_name, p.sku AS sku, c.name AS category_name,
               s.name AS store_name, st.quantity AS quantity, st.reserved AS reserved,
               st.min_stock AS min_stock, st.max_stock AS max_stock,
               p.cost_cents AS cost_cents, p.price_cents AS price_cents,
               p.unit AS unit, p.is_service AS is_service, st.updated_at AS updated_at
        FROM stocks st
        INNER JOIN products p ON p.id = st.product_id
        INNER JOIN stores s ON s.id = st.store_id
        LEFT JOIN categories c ON c.id = p.category_id
        WHERE (:storeId IS NULL OR st.store_id = :storeId)
          AND (:query IS NULL OR p.name LIKE '%' || :query || '%' OR p.sku LIKE '%' || :query || '%')
          AND (:onlyLowStock = 0 OR st.quantity <= st.min_stock)
        ORDER BY st.quantity ASC, p.name ASC
        LIMIT :limit
        """
    )
    fun observeStock(
        storeId: Long?,
        query: String?,
        onlyLowStock: Boolean = false,
        limit: Int = 1000,
    ): Flow<List<StockDetailRow>>

    @Query(
        """
        SELECT st.id AS stock_id, st.product_id AS product_id, st.store_id AS store_id,
               p.name AS product_name, p.sku AS sku, c.name AS category_name,
               s.name AS store_name, st.quantity AS quantity, st.reserved AS reserved,
               st.min_stock AS min_stock, st.max_stock AS max_stock,
               p.cost_cents AS cost_cents, p.price_cents AS price_cents,
               p.unit AS unit, p.is_service AS is_service, st.updated_at AS updated_at
        FROM stocks st
        INNER JOIN products p ON p.id = st.product_id
        INNER JOIN stores s ON s.id = st.store_id
        LEFT JOIN categories c ON c.id = p.category_id
        WHERE st.quantity <= st.min_stock AND p.is_active = 1 AND s.is_active = 1
        ORDER BY st.quantity ASC
        LIMIT :limit
        """
    )
    fun observeLowStock(limit: Int = 200): Flow<List<StockDetailRow>>

    @Query(
        """
        SELECT IFNULL(SUM(st.quantity), 0) AS total_quantity,
               IFNULL(SUM(st.quantity * p.cost_cents), 0) AS cost_value_cents,
               IFNULL(SUM(st.quantity * p.price_cents), 0) AS retail_value_cents,
               IFNULL(SUM(CASE WHEN st.quantity <= st.min_stock AND st.quantity > 0 THEN 1 ELSE 0 END), 0) AS low_stock_count,
               IFNULL(SUM(CASE WHEN st.quantity <= 0 THEN 1 ELSE 0 END), 0) AS out_of_stock_count
        FROM stocks st
        INNER JOIN products p ON p.id = st.product_id
        WHERE (:storeId IS NULL OR st.store_id = :storeId) AND p.is_service = 0
        """
    )
    suspend fun inventoryValue(storeId: Long?): InventoryValueRow

    /** Version suspend de [observeStock] para el informe de inventario valorado. */
    @Query(
        """
        SELECT st.id AS stock_id, st.product_id AS product_id, st.store_id AS store_id,
               p.name AS product_name, p.sku AS sku, c.name AS category_name,
               s.name AS store_name, st.quantity AS quantity, st.reserved AS reserved,
               st.min_stock AS min_stock, st.max_stock AS max_stock,
               p.cost_cents AS cost_cents, p.price_cents AS price_cents,
               p.unit AS unit, p.is_service AS is_service, st.updated_at AS updated_at
        FROM stocks st
        INNER JOIN products p ON p.id = st.product_id
        INNER JOIN stores s ON s.id = st.store_id
        LEFT JOIN categories c ON c.id = p.category_id
        WHERE (:storeId IS NULL OR st.store_id = :storeId) AND p.is_service = 0
        ORDER BY s.name ASC, p.name ASC
        """
    )
    suspend fun getStockForReport(storeId: Long?): List<StockDetailRow>

    @Query("SELECT * FROM stocks WHERE product_id = :productId AND store_id = :storeId LIMIT 1")
    suspend fun get(productId: Long, storeId: Long): StockEntity?

    @Query("SELECT * FROM stocks WHERE product_id = :productId ORDER BY store_id ASC")
    suspend fun getByProduct(productId: Long): List<StockEntity>

    @Query("SELECT quantity FROM stocks WHERE product_id = :productId AND store_id = :storeId")
    suspend fun getQuantity(productId: Long, storeId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(stock: StockEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(stocks: List<StockEntity>): List<Long>

    @Upsert
    suspend fun upsert(stock: StockEntity)

    /**
     * Ajuste atomico de existencias. Devuelve el numero de filas afectadas:
     * si es 0, el registro no existia y el llamador debe crearlo.
     */
    @Query(
        """
        UPDATE stocks
        SET quantity = quantity + :delta, updated_at = :now
        WHERE product_id = :productId AND store_id = :storeId
        """
    )
    suspend fun adjustQuantity(productId: Long, storeId: Long, delta: Int, now: Long): Int

    /** Fija una cantidad absoluta (usada en conteos fisicos y ajustes). */
    @Query("UPDATE stocks SET quantity = :quantity, last_count_at = :now, updated_at = :now WHERE product_id = :productId AND store_id = :storeId")
    suspend fun setQuantity(productId: Long, storeId: Long, quantity: Int, now: Long): Int

    @Query("UPDATE stocks SET min_stock = :minStock, max_stock = :maxStock, updated_at = :now WHERE product_id = :productId AND store_id = :storeId")
    suspend fun updateThresholds(productId: Long, storeId: Long, minStock: Int, maxStock: Int, now: Long): Int

    @Query("SELECT COUNT(*) FROM stocks WHERE quantity <= min_stock")
    fun observeLowStockCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM stocks WHERE quantity <= min_stock AND quantity > 0")
    suspend fun countLowStock(): Int

    @Query("SELECT COUNT(*) FROM stocks WHERE quantity <= 0")
    suspend fun countOutOfStock(): Int

    @Query("SELECT COUNT(*) FROM stocks WHERE quantity <= 0")
    fun observeOutOfStockCount(): Flow<Int>

    @Query("DELETE FROM stocks WHERE product_id = :productId AND store_id = :storeId")
    suspend fun delete(productId: Long, storeId: Long): Int
}
