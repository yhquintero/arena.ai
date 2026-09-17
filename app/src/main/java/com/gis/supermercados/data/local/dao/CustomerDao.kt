package com.gis.supermercados.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gis.supermercados.data.local.entity.CustomerEntity
import com.gis.supermercados.data.local.entity.CustomerWithStatsRow
import com.gis.supermercados.data.local.entity.SaleListRow
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Query(
        """
        SELECT c.*,
               (SELECT COUNT(*) FROM sales s WHERE s.customer_id = c.id AND s.status != 'ANULADA') AS purchase_count,
               IFNULL((SELECT SUM(s.total_cents) FROM sales s WHERE s.customer_id = c.id AND s.status != 'ANULADA'), 0) AS total_purchases_cents,
               IFNULL((SELECT MAX(s.created_at) FROM sales s WHERE s.customer_id = c.id), 0) AS last_purchase_at
        FROM customers c
        WHERE c.is_active = 1
          AND (:query IS NULL OR c.full_name LIKE '%' || :query || '%'
               OR c.document_id LIKE '%' || :query || '%'
               OR c.phone LIKE '%' || :query || '%')
        ORDER BY c.full_name ASC
        LIMIT :limit
        """
    )
    fun observeCustomers(query: String?, limit: Int = 500): Flow<List<CustomerWithStatsRow>>

    @Query("SELECT COUNT(*) FROM customers WHERE is_active = 1")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM customers WHERE is_active = 1")
    suspend fun countActive(): Int

    @Transaction
    @Query(
        """
        SELECT c.*,
               (SELECT COUNT(*) FROM sales s WHERE s.customer_id = c.id AND s.status != 'ANULADA') AS purchase_count,
               IFNULL((SELECT SUM(s.total_cents) FROM sales s WHERE s.customer_id = c.id AND s.status != 'ANULADA'), 0) AS total_purchases_cents,
               IFNULL((SELECT MAX(s.created_at) FROM sales s WHERE s.customer_id = c.id), 0) AS last_purchase_at
        FROM customers c WHERE c.id = :customerId
        """
    )
    suspend fun getWithStats(customerId: Long): CustomerWithStatsRow?

    @Query("SELECT * FROM customers WHERE id = :customerId")
    suspend fun getById(customerId: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE is_active = 1 ORDER BY full_name ASC")
    suspend fun getActive(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE document_id = :documentId AND document_id != '' LIMIT 1")
    suspend fun getByDocument(documentId: String): CustomerEntity?

    /** Historial de compras del cliente. */
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
        WHERE s.customer_id = :customerId
        ORDER BY s.created_at DESC
        LIMIT :limit
        """
    )
    fun observePurchaseHistory(customerId: Long, limit: Int = 100): Flow<List<SaleListRow>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(customers: List<CustomerEntity>): List<Long>

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("UPDATE customers SET loyalty_points = loyalty_points + :points WHERE id = :customerId")
    suspend fun addLoyaltyPoints(customerId: Long, points: Int): Int

    @Query("UPDATE customers SET balance_cents = balance_cents + :deltaCents WHERE id = :customerId")
    suspend fun adjustBalance(customerId: Long, deltaCents: Long): Int

    @Query("UPDATE customers SET is_active = :isActive WHERE id = :customerId")
    suspend fun setActive(customerId: Long, isActive: Boolean): Int
}
