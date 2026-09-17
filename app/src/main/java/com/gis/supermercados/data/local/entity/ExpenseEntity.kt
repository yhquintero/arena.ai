package com.gis.supermercados.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gis.supermercados.domain.model.ExpenseCategory
import com.gis.supermercados.domain.model.PaymentMethod

/**
 * Gasto operacional. [storeId] nulo = gasto CORPORATIVO (no asignado a sucursal).
 * Los importes se guardan en centavos.
 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = StoreEntity::class,
            parentColumns = ["id"],
            childColumns = ["store_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["store_id"]),
        Index(value = ["category"]),
        Index(value = ["expense_date"]),
        Index(value = ["user_id"]),
        Index(value = ["created_at"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val category: ExpenseCategory,
    @ColumnInfo(name = "store_id") val storeId: Long? = null,
    val concept: String,
    val provider: String = "",
    @ColumnInfo(name = "amount_cents") val amountCents: Long = 0L,
    @ColumnInfo(name = "tax_cents") val taxCents: Long = 0L,
    @ColumnInfo(name = "total_cents") val totalCents: Long = 0L,
    @ColumnInfo(name = "payment_method") val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    val reference: String = "",
    @ColumnInfo(name = "receipt_path") val receiptPath: String = "",
    @ColumnInfo(name = "user_id") val userId: Long = 0L,
    @ColumnInfo(name = "expense_date") val expenseDate: Long = 0L,
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
)
