package com.gis.supermercados.domain.repository

import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.domain.model.Customer
import com.gis.supermercados.domain.model.Sale
import kotlinx.coroutines.flow.Flow

/** CRUD de clientes, historial de compras y credito. */
interface CustomerRepository {

    fun observeCustomers(query: String?): Flow<List<Customer>>

    fun observeActiveCustomerCount(): Flow<Int>

    suspend fun getCustomer(customerId: Long): Customer?

    suspend fun getActiveCustomers(): List<Customer>

    suspend fun findByDocument(documentId: String): Customer?

    suspend fun saveCustomer(customer: Customer): AppResult<Long>

    suspend fun setActive(customerId: Long, isActive: Boolean): AppResult<Unit>

    fun observePurchaseHistory(customerId: Long): Flow<List<Sale>>

    suspend fun addLoyaltyPoints(customerId: Long, points: Int): AppResult<Unit>

    suspend fun adjustBalance(customerId: Long, deltaCents: Long): AppResult<Unit>
}
