package com.gis.supermercados.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gis.supermercados.domain.model.MovementType
import com.gis.supermercados.domain.model.ProductUnit

/** Categoria de productos. */
@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String = "",
    /** Color ARGB como Long para graficos. */
    val color: Long = 0xFF1B6EF3L,
    @ColumnInfo(name = "target_margin_percent") val targetMarginPercent: Int = 25,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
)

/** Producto o servicio del catalogo. Precios en CENTAVOS. */
@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["sku"], unique = true),
        Index(value = ["barcode"]),
        Index(value = ["category_id"]),
        Index(value = ["is_active"]),
        Index(value = ["name"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val sku: String,
    val barcode: String = "",
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "cost_cents") val costCents: Long = 0L,
    @ColumnInfo(name = "price_cents") val priceCents: Long = 0L,
    @ColumnInfo(name = "tax_rate") val taxRate: Double = 0.0,
    val unit: ProductUnit = ProductUnit.UNIDAD,
    @ColumnInfo(name = "is_service") val isService: Boolean = false,
    @ColumnInfo(name = "supplier_name") val supplierName: String = "",
    @ColumnInfo(name = "supplier_phone") val supplierPhone: String = "",
    @ColumnInfo(name = "min_stock_global") val minStockGlobal: Int = 10,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    val description: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
)

/**
 * Existencias por producto y tienda (relacion N:M con control de stock).
 * La unicidad (product_id, store_id) impide registros duplicados.
 */
@Entity(
    tableName = "stocks",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StoreEntity::class,
            parentColumns = ["id"],
            childColumns = ["store_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["product_id", "store_id"], unique = true),
        Index(value = ["store_id"]),
        Index(value = ["quantity"])
    ]
)
data class StockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "store_id") val storeId: Long,
    val quantity: Int = 0,
    /** Cantidad reservada (pedidos pendientes de cobro). */
    val reserved: Int = 0,
    @ColumnInfo(name = "min_stock") val minStock: Int = 10,
    @ColumnInfo(name = "max_stock") val maxStock: Int = 0,
    @ColumnInfo(name = "last_count_at") val lastCountAt: Long = 0L,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
)

/** Movimiento de inventario: bitacora auditable de entradas, salidas, transferencias y ajustes. */
@Entity(
    tableName = "inventory_movements",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StoreEntity::class,
            parentColumns = ["id"],
            childColumns = ["store_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["product_id"]),
        Index(value = ["store_id"]),
        Index(value = ["destination_store_id"]),
        Index(value = ["created_at"]),
        Index(value = ["type"])
    ]
)
data class InventoryMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "store_id") val storeId: Long,
    @ColumnInfo(name = "destination_store_id") val destinationStoreId: Long? = null,
    val type: MovementType,
    val quantity: Int,
    @ColumnInfo(name = "unit_cost_cents") val unitCostCents: Long = 0L,
    val reason: String = "",
    val reference: String = "",
    @ColumnInfo(name = "user_id") val userId: Long = 0L,
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
)
