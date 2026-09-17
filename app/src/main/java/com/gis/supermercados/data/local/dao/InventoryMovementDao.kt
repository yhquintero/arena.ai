package com.gis.supermercados.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gis.supermercados.data.local.entity.InventoryMovementEntity
import com.gis.supermercados.data.local.entity.MovementDetailRow
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryMovementDao {

    @Query(
        """
        SELECT m.id AS movement_id, m.store_id AS store_id, m.product_id AS product_id,
               p.name AS product_name, p.sku AS sku,
               s.name AS store_name, ds.name AS destination_store_name,
               m.type AS type, m.quantity AS quantity, m.unit_cost_cents AS unit_cost_cents,
               m.reason AS reason, m.reference AS reference,
               IFNULL(u.full_name, '') AS user_name, m.created_at AS created_at
        FROM inventory_movements m
        INNER JOIN products p ON p.id = m.product_id
        INNER JOIN stores s ON s.id = m.store_id
        LEFT JOIN stores ds ON ds.id = m.destination_store_id
        LEFT JOIN users u ON u.id = m.user_id
        WHERE (:storeId IS NULL OR m.store_id = :storeId OR m.destination_store_id = :storeId)
          AND (:productId IS NULL OR m.product_id = :productId)
          AND (:type IS NULL OR m.type = :type)
        ORDER BY m.created_at DESC
        LIMIT :limit
        """
    )
    fun observeMovements(
        storeId: Long?,
        productId: Long?,
        type: String?,
        limit: Int = 500,
    ): Flow<List<MovementDetailRow>>

    @Query(
        """
        SELECT m.id AS movement_id, m.store_id AS store_id, m.product_id AS product_id,
               p.name AS product_name, p.sku AS sku,
               s.name AS store_name, ds.name AS destination_store_name,
               m.type AS type, m.quantity AS quantity, m.unit_cost_cents AS unit_cost_cents,
               m.reason AS reason, m.reference AS reference,
               IFNULL(u.full_name, '') AS user_name, m.created_at AS created_at
        FROM inventory_movements m
        INNER JOIN products p ON p.id = m.product_id
        INNER JOIN stores s ON s.id = m.store_id
        LEFT JOIN stores ds ON ds.id = m.destination_store_id
        LEFT JOIN users u ON u.id = m.user_id
        WHERE m.created_at BETWEEN :start AND :end
          AND (:storeId IS NULL OR m.store_id = :storeId)
        ORDER BY m.created_at DESC
        """
    )
    suspend fun getMovementsBetween(start: Long, end: Long, storeId: Long?): List<MovementDetailRow>

    @Insert
    suspend fun insert(movement: InventoryMovementEntity): Long

    @Insert
    suspend fun insertAll(movements: List<InventoryMovementEntity>)

    @Query("SELECT COUNT(*) FROM inventory_movements WHERE created_at BETWEEN :start AND :end")
    suspend fun countBetween(start: Long, end: Long): Int
}
