package com.gis.supermercados.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Sucursal: incluye horario y permisos operativos propios. */
@Entity(
    tableName = "stores",
    indices = [Index(value = ["code"], unique = true), Index(value = ["is_active"])]
)
data class StoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val code: String,
    val address: String = "",
    val city: String = "",
    val phone: String = "",
    val email: String = "",
    @ColumnInfo(name = "manager_name") val managerName: String = "",
    @ColumnInfo(name = "tax_id") val taxId: String = "",
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,

    // --- Horario ---
    @ColumnInfo(name = "open_days") val openDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    @ColumnInfo(name = "opening_time") val openingTime: String = "08:00",
    @ColumnInfo(name = "closing_time") val closingTime: String = "21:00",
    @ColumnInfo(name = "extended_hours") val hasExtendedHours: Boolean = false,

    // --- Permisos por sucursal ---
    @ColumnInfo(name = "allow_sales") val allowSales: Boolean = true,
    @ColumnInfo(name = "allow_returns") val allowReturns: Boolean = true,
    @ColumnInfo(name = "allow_inventory_adjust") val allowInventoryAdjust: Boolean = true,
    @ColumnInfo(name = "allow_transfers") val allowTransfers: Boolean = true,
    @ColumnInfo(name = "allow_expenses") val allowExpenses: Boolean = true,
    @ColumnInfo(name = "allow_reports") val allowReports: Boolean = true,
    @ColumnInfo(name = "max_discount_percent") val maxDiscountPercent: Int = 20,

    val notes: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
)
