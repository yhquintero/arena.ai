package com.gis.supermercados.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gis.supermercados.domain.model.DocumentType
import com.gis.supermercados.domain.model.PaymentMethod
import com.gis.supermercados.domain.model.SaleStatus

/** Cliente: perfil, contacto, puntos de fidelidad y credito. */
@Entity(
    tableName = "customers",
    indices = [Index(value = ["document_id"]), Index(value = ["full_name"]), Index(value = ["is_active"])]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "full_name") val fullName: String,
    @ColumnInfo(name = "document_type") val documentType: DocumentType = DocumentType.CEDULA,
    @ColumnInfo(name = "document_id") val documentId: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    @ColumnInfo(name = "loyalty_points") val loyaltyPoints: Int = 0,
    @ColumnInfo(name = "credit_limit_cents") val creditLimitCents: Long = 0L,
    @ColumnInfo(name = "balance_cents") val balanceCents: Long = 0L,
    val notes: String = "",
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
)

/** Cabecera de venta. */
@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(
            entity = StoreEntity::class,
            parentColumns = ["id"],
            childColumns = ["store_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["ticket_number"], unique = true),
        Index(value = ["store_id"]),
        Index(value = ["customer_id"]),
        Index(value = ["user_id"]),
        Index(value = ["created_at"]),
        Index(value = ["status"])
    ]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "ticket_number") val ticketNumber: String,
    @ColumnInfo(name = "store_id") val storeId: Long,
    @ColumnInfo(name = "customer_id") val customerId: Long? = null,
    @ColumnInfo(name = "user_id") val userId: Long,
    @ColumnInfo(name = "subtotal_cents") val subtotalCents: Long = 0L,
    @ColumnInfo(name = "discount_cents") val discountCents: Long = 0L,
    @ColumnInfo(name = "tax_cents") val taxCents: Long = 0L,
    @ColumnInfo(name = "total_cents") val totalCents: Long = 0L,
    @ColumnInfo(name = "cost_cents") val costCents: Long = 0L,
    @ColumnInfo(name = "payment_method") val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    @ColumnInfo(name = "payment_reference") val paymentReference: String = "",
    @ColumnInfo(name = "cash_received_cents") val cashReceivedCents: Long = 0L,
    @ColumnInfo(name = "change_cents") val changeCents: Long = 0L,
    val status: SaleStatus = SaleStatus.COMPLETADA,
    @ColumnInfo(name = "item_count") val itemCount: Int = 0,
    val notes: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
)

/**
 * Detalle de venta. Se guardan "fotos" del nombre, SKU y costos en el momento
 * de la venta para que el historico no cambie si el producto se edita despues.
 */
@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["sale_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sale_id"]), Index(value = ["product_id"])]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "sale_id") val saleId: Long,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "product_name") val productName: String,
    val sku: String = "",
    val quantity: Int = 1,
    @ColumnInfo(name = "returned_quantity") val returnedQuantity: Int = 0,
    @ColumnInfo(name = "unit_price_cents") val unitPriceCents: Long = 0L,
    @ColumnInfo(name = "unit_cost_cents") val unitCostCents: Long = 0L,
    @ColumnInfo(name = "discount_percent") val discountPercent: Int = 0,
    @ColumnInfo(name = "tax_rate") val taxRate: Double = 0.0,
    @ColumnInfo(name = "line_total_cents") val lineTotalCents: Long = 0L,
)

/** Nota de credito (devolucion). */
@Entity(
    tableName = "credit_notes",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["sale_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StoreEntity::class,
            parentColumns = ["id"],
            childColumns = ["store_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["credit_note_number"], unique = true),
        Index(value = ["sale_id"]),
        Index(value = ["store_id"]),
        Index(value = ["created_at"])
    ]
)
data class CreditNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "credit_note_number") val creditNoteNumber: String,
    @ColumnInfo(name = "sale_id") val saleId: Long,
    @ColumnInfo(name = "store_id") val storeId: Long,
    @ColumnInfo(name = "customer_id") val customerId: Long? = null,
    @ColumnInfo(name = "user_id") val userId: Long = 0L,
    val reason: String = "",
    @ColumnInfo(name = "subtotal_cents") val subtotalCents: Long = 0L,
    @ColumnInfo(name = "tax_cents") val taxCents: Long = 0L,
    @ColumnInfo(name = "total_cents") val totalCents: Long = 0L,
    val restock: Boolean = true,
    @ColumnInfo(name = "item_count") val itemCount: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
)

@Entity(
    tableName = "credit_note_items",
    foreignKeys = [
        ForeignKey(
            entity = CreditNoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["credit_note_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["credit_note_id"]), Index(value = ["sale_item_id"])]
)
data class CreditNoteItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "credit_note_id") val creditNoteId: Long,
    @ColumnInfo(name = "sale_item_id") val saleItemId: Long,
    @ColumnInfo(name = "product_id") val productId: Long = 0L,
    @ColumnInfo(name = "product_name") val productName: String = "",
    val sku: String = "",
    val quantity: Int = 0,
    @ColumnInfo(name = "unit_price_cents") val unitPriceCents: Long = 0L,
    @ColumnInfo(name = "tax_rate") val taxRate: Double = 0.0,
    @ColumnInfo(name = "line_total_cents") val lineTotalCents: Long = 0L,
)
