package com.gis.supermercados.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gis.supermercados.core.common.AppConstants
import com.gis.supermercados.data.local.dao.CategoryDao
import com.gis.supermercados.data.local.dao.CreditNoteDao
import com.gis.supermercados.data.local.dao.CustomerDao
import com.gis.supermercados.data.local.dao.ExpenseDao
import com.gis.supermercados.data.local.dao.InventoryMovementDao
import com.gis.supermercados.data.local.dao.ProductDao
import com.gis.supermercados.data.local.dao.ReportQueriesDao
import com.gis.supermercados.data.local.dao.SettingsDao
import com.gis.supermercados.data.local.dao.SaleDao
import com.gis.supermercados.data.local.dao.StockDao
import com.gis.supermercados.data.local.dao.StoreDao
import com.gis.supermercados.data.local.dao.SystemLogDao
import com.gis.supermercados.data.local.dao.UserDao
import com.gis.supermercados.data.local.entity.AppSettingEntity
import com.gis.supermercados.data.local.entity.AuditLogEntity
import com.gis.supermercados.data.local.entity.CategoryEntity
import com.gis.supermercados.data.local.entity.CreditNoteEntity
import com.gis.supermercados.data.local.entity.CreditNoteItemEntity
import com.gis.supermercados.data.local.entity.CustomerEntity
import com.gis.supermercados.data.local.entity.ExpenseEntity
import com.gis.supermercados.data.local.entity.InventoryMovementEntity
import com.gis.supermercados.data.local.entity.ProductEntity
import com.gis.supermercados.data.local.entity.ReportLogEntity
import com.gis.supermercados.data.local.entity.SaleEntity
import com.gis.supermercados.data.local.entity.SaleItemEntity
import com.gis.supermercados.data.local.entity.StockEntity
import com.gis.supermercados.data.local.entity.StoreEntity
import com.gis.supermercados.data.local.entity.UserEntity

/**
 * Base de datos local (SQLite via Room).
 *
 * - 15 tablas con claves foraneas e indices para consultas rapidas.
 * - [exportSchema] = true genera app/schemas/[version].json, imprescindible
 *   para escribir y verificar migraciones (ver docs/BASE_DE_DATOS.md).
 * - La app es offline-first: esta BD es la unica fuente de verdad.
 */
@Database(
    entities = [
        StoreEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        StockEntity::class,
        InventoryMovementEntity::class,
        CustomerEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        CreditNoteEntity::class,
        CreditNoteItemEntity::class,
        ExpenseEntity::class,
        UserEntity::class,
        AppSettingEntity::class,
        AuditLogEntity::class,
        ReportLogEntity::class,
    ],
    version = GisDatabase.VERSION,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class GisDatabase : RoomDatabase() {

    abstract fun storeDao(): StoreDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun stockDao(): StockDao
    abstract fun inventoryMovementDao(): InventoryMovementDao
    abstract fun customerDao(): CustomerDao
    abstract fun saleDao(): SaleDao
    abstract fun creditNoteDao(): CreditNoteDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun userDao(): UserDao
    abstract fun settingsDao(): SettingsDao
    abstract fun systemLogDao(): SystemLogDao
    abstract fun reportQueriesDao(): ReportQueriesDao

    companion object {
        const val VERSION = 1
        const val NAME = AppConstants.DATABASE_NAME
    }
}
