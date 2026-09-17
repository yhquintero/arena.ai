package com.gis.supermercados.di

import com.gis.supermercados.data.repository.AuthRepositoryImpl
import com.gis.supermercados.data.repository.BackupRepositoryImpl
import com.gis.supermercados.data.repository.CatalogRepositoryImpl
import com.gis.supermercados.data.repository.CustomerRepositoryImpl
import com.gis.supermercados.data.repository.ExpenseRepositoryImpl
import com.gis.supermercados.data.repository.InventoryRepositoryImpl
import com.gis.supermercados.data.repository.LogRepositoryImpl
import com.gis.supermercados.data.repository.ReportRepositoryImpl
import com.gis.supermercados.data.repository.SalesRepositoryImpl
import com.gis.supermercados.data.repository.SettingsRepositoryImpl
import com.gis.supermercados.data.repository.StoreRepositoryImpl
import com.gis.supermercados.domain.repository.AuthRepository
import com.gis.supermercados.domain.repository.BackupRepository
import com.gis.supermercados.domain.repository.CatalogRepository
import com.gis.supermercados.domain.repository.CustomerRepository
import com.gis.supermercados.domain.repository.ExpenseRepository
import com.gis.supermercados.domain.repository.InventoryRepository
import com.gis.supermercados.domain.repository.LogRepository
import com.gis.supermercados.domain.repository.ReportRepository
import com.gis.supermercados.domain.repository.SalesRepository
import com.gis.supermercados.domain.repository.SettingsRepository
import com.gis.supermercados.domain.repository.StoreRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Vincula cada interfaz del dominio con su implementacion de datos.
 * Las capas superiores (ViewModels) solo conocen las interfaces: asi se pueden
 * sustituir por dobles en los tests sin tocar la logica de negocio.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindStoreRepository(impl: StoreRepositoryImpl): StoreRepository

    @Binds @Singleton
    abstract fun bindCatalogRepository(impl: CatalogRepositoryImpl): CatalogRepository

    @Binds @Singleton
    abstract fun bindInventoryRepository(impl: InventoryRepositoryImpl): InventoryRepository

    @Binds @Singleton
    abstract fun bindSalesRepository(impl: SalesRepositoryImpl): SalesRepository

    @Binds @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds @Singleton
    abstract fun bindCustomerRepository(impl: CustomerRepositoryImpl): CustomerRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds @Singleton
    abstract fun bindLogRepository(impl: LogRepositoryImpl): LogRepository

    @Binds @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository
}
