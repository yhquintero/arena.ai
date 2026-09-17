package com.gis.supermercados.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gis.supermercados.data.local.entity.ProductEntity
import com.gis.supermercados.data.local.entity.ProductStockRow
import com.gis.supermercados.data.local.entity.ProductWithCategoryRow
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Transaction
    @Query(
        """
        SELECT p.*, c.name AS category_name, c.color AS category_color
        FROM products p
        LEFT JOIN categories c ON c.id = p.category_id
        ORDER BY p.name ASC
        """
    )
    fun observeAllWithCategory(): Flow<List<ProductWithCategoryRow>>

    @Transaction
    @Query(
        """
        SELECT p.*, c.name AS category_name, c.color AS category_color
        FROM products p
        LEFT JOIN categories c ON c.id = p.category_id
        WHERE p.is_active = 1
          AND (:categoryId IS NULL OR p.category_id = :categoryId)
          AND (:query IS NULL OR p.name LIKE '%' || :query || '%'
               OR p.sku LIKE '%' || :query || '%'
               OR p.barcode LIKE '%' || :query || '%')
        ORDER BY p.name ASC
        LIMIT :limit
        """
    )
    fun observeFiltered(
        query: String?,
        categoryId: Long?,
        limit: Int = 500,
    ): Flow<List<ProductWithCategoryRow>>

    @Query("SELECT COUNT(*) FROM products WHERE is_active = 1")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE is_active = 1")
    suspend fun countActive(): Int

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getById(productId: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun getBySku(sku: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND barcode != '' LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductEntity?

    @Query("SELECT id FROM products WHERE sku = :sku AND id != :excludeId LIMIT 1")
    suspend fun getIdBySkuExcluding(sku: String, excludeId: Long): Long?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(products: List<ProductEntity>): List<Long>

    @Update
    suspend fun update(product: ProductEntity)

    @Query("UPDATE products SET is_active = :isActive, updated_at = :now WHERE id = :productId")
    suspend fun setActive(productId: Long, isActive: Boolean, now: Long): Int

    /** Stock agregado de un producto en todas las tiendas. */
    @Query(
        """
        SELECT s.product_id AS product_id,
               IFNULL(SUM(s.quantity), 0) AS total_quantity,
               IFNULL(SUM(CASE WHEN s.quantity <= s.min_stock AND s.quantity > 0 THEN 1 ELSE 0 END), 0) AS low_stock_stores,
               IFNULL(SUM(CASE WHEN s.quantity <= 0 THEN 1 ELSE 0 END), 0) AS out_of_stock_stores,
               IFNULL(SUM(s.quantity * p.cost_cents), 0) AS stock_value_cents
        FROM stocks s
        INNER JOIN products p ON p.id = s.product_id
        WHERE s.product_id = :productId
        GROUP BY s.product_id
        """
    )
    suspend fun stockSummary(productId: Long): ProductStockRow?
}
