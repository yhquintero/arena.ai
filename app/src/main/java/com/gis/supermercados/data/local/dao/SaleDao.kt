package com.gis.supermercados.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gis.supermercados.data.local.entity.SaleEntity
import com.gis.supermercados.data.local.entity.SaleItemEntity
import com.gis.supermercados.data.local.entity.SaleListRow
import com.gis.supermercados.data.local.entity.SaleWithItemsRow
import com.gis.supermercados.domain.model.SaleStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {

    /** Historial de ventas con filtros combinables (todos opcionales). */
    @Query(
        """
        SELECT s.id AS sale_id, s.store_id AS store_id, s.customer_id AS customer_id,
               s.user_id AS user_id, s.ticket_number AS ticket_number, st.name AS store_name,
               c.full_name AS customer_name, IFNULL(u.full_name, '') AS user_name,
               s.subtotal_cents AS subtotal_cents, s.discount_cents AS discount_cents,
               s.tax_cents AS tax_cents, s.total_cents AS total_cents, s.cost_cents AS cost_cents,
               s.payment_method AS payment_method, s.status AS status,
               s.item_count AS item_count, s.created_at AS created_at
        FROM sales s
        INNER JOIN stores st ON st.id = s.store_id
        LEFT JOIN customers c ON c.id = s.customer_id
        LEFT JOIN users u ON u.id = s.user_id
        WHERE (:storeId IS NULL OR s.store_id = :storeId)
          AND (:from IS NULL OR s.created_at >= :from)
          AND (:to IS NULL OR s.created_at <= :to)
          AND (:status IS NULL OR s.status = :status)
          AND (:query IS NULL OR s.ticket_number LIKE '%' || :query || '%'
               OR c.full_name LIKE '%' || :query || '%'
               OR st.name LIKE '%' || :query || '%')
        ORDER BY s.created_at DESC
        LIMIT :limit
        """
    )
    fun observeSales(
        storeId: Long?,
        from: Long?,
        to: Long?,
        status: String?,
        query: String?,
        limit: Int = 500,
    ): Flow<List<SaleListRow>>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :saleId")
    suspend fun getWithItems(saleId: Long): SaleWithItemsRow?

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :saleId")
    fun observeWithItems(saleId: Long): Flow<SaleWithItemsRow?>

    @Query("SELECT * FROM sales WHERE id = :saleId")
    suspend fun getById(saleId: Long): SaleEntity?

    @Query("SELECT * FROM sale_items WHERE sale_id = :saleId ORDER BY id ASC")
    suspend fun getItems(saleId: Long): List<SaleItemEntity>

    @Query("SELECT IFNULL(MAX(id), 0) FROM sales")
    suspend fun maxId(): Long

    @Query("SELECT COUNT(*) FROM sales WHERE created_at BETWEEN :start AND :end")
    suspend fun countBetween(start: Long, end: Long): Int

    @Insert
    suspend fun insert(sale: SaleEntity): Long

    @Insert
    suspend fun insertAll(sales: List<SaleEntity>): List<Long>

    @Insert
    suspend fun insertItems(items: List<SaleItemEntity>)

    /** Ventas recientes completadas (uso interno: datos de ejemplo). */
    @Query("SELECT * FROM sales WHERE status = 'COMPLETADA' ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentForSeed(limit: Int): List<SaleEntity>

    @Update
    suspend fun update(sale: SaleEntity)

    @Query("UPDATE sales SET status = :status WHERE id = :saleId")
    suspend fun updateStatus(saleId: Long, status: SaleStatus): Int

    /** Acumula unidades devueltas sobre una linea de venta. */
    @Query("UPDATE sale_items SET returned_quantity = returned_quantity + :quantity WHERE id = :saleItemId")
    suspend fun addReturnedQuantity(saleItemId: Long, quantity: Int): Int

    @Query("DELETE FROM sales WHERE id = :saleId")
    suspend fun deleteById(saleId: Long): Int
}
