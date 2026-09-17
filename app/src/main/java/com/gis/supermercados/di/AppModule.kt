package com.gis.supermercados.di

import android.content.Context
import androidx.room.Room
import com.gis.supermercados.core.common.AppConstants
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.core.logging.LogSink
import com.gis.supermercados.core.logging.PersistentLogSink
import com.gis.supermercados.data.local.GisDatabase
import com.gis.supermercados.data.local.dao.CategoryDao
import com.gis.supermercados.data.local.dao.CreditNoteDao
import com.gis.supermercados.data.local.dao.CustomerDao
import com.gis.supermercados.data.local.dao.ExpenseDao
import com.gis.supermercados.data.local.dao.InventoryMovementDao
import com.gis.supermercados.data.local.dao.ProductDao
import com.gis.supermercados.data.local.dao.ReportQueriesDao
import com.gis.supermercados.data.local.dao.SaleDao
import com.gis.supermercados.data.local.dao.SettingsDao
import com.gis.supermercados.data.local.dao.StockDao
import com.gis.supermercados.data.local.dao.StoreDao
import com.gis.supermercados.data.local.dao.SystemLogDao
import com.gis.supermercados.data.local.dao.UserDao
import com.gis.supermercados.data.local.migrations.Migrations
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/** Base de datos Room y sus DAOs (unica fuente de verdad, todo local). */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GisDatabase =
        Room.databaseBuilder(context, GisDatabase::class.java, AppConstants.DATABASE_NAME)
            // Migraciones explicitas: nunca se borran los datos del usuario.
            .addMigrations(*Migrations.ALL)
            .build()

    @Provides fun provideStoreDao(db: GisDatabase): StoreDao = db.storeDao()
    @Provides fun provideCategoryDao(db: GisDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideProductDao(db: GisDatabase): ProductDao = db.productDao()
    @Provides fun provideStockDao(db: GisDatabase): StockDao = db.stockDao()
    @Provides fun provideMovementDao(db: GisDatabase): InventoryMovementDao = db.inventoryMovementDao()
    @Provides fun provideCustomerDao(db: GisDatabase): CustomerDao = db.customerDao()
    @Provides fun provideSaleDao(db: GisDatabase): SaleDao = db.saleDao()
    @Provides fun provideCreditNoteDao(db: GisDatabase): CreditNoteDao = db.creditNoteDao()
    @Provides fun provideExpenseDao(db: GisDatabase): ExpenseDao = db.expenseDao()
    @Provides fun provideUserDao(db: GisDatabase): UserDao = db.userDao()
    @Provides fun provideSettingsDao(db: GisDatabase): SettingsDao = db.settingsDao()
    @Provides fun provideSystemLogDao(db: GisDatabase): SystemLogDao = db.systemLogDao()
    @Provides fun provideReportQueriesDao(db: GisDatabase): ReportQueriesDao = db.reportQueriesDao()
}

/** Dispatchers y ambito global de la aplicacion. */
@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    /**
     * Ambito que vive mientras el proceso: lo usan el log persistente, las
     * semillas y los respaldos automaticos. `SupervisorJob` evita que un fallo
     * puntual cancele el resto de trabajos en segundo plano.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(@IoDispatcher dispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcher)

    /**
     * [PersistentLogSink] se inyecta sin calificador: se le entrega el mismo
     * ambito de aplicacion para no crear un segundo ciclo de vida.
     */
    @Provides
    @Singleton
    fun provideUnqualifiedScope(@ApplicationScope scope: CoroutineScope): CoroutineScope = scope
}

/** Log interno: Logcat + tabla audit_logs. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LoggingModule {

    @Binds
    @Singleton
    abstract fun bindLogSink(impl: PersistentLogSink): LogSink
}
