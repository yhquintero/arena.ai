package com.gis.supermercados.data.repository

import android.content.Context
import androidx.annotation.StringRes
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.common.ValidationResult
import com.gis.supermercados.core.common.Validators
import com.gis.supermercados.core.common.runCatchingApp
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.data.local.GisDatabase
import com.gis.supermercados.data.mapper.toDomain
import com.gis.supermercados.data.mapper.toEntity
import com.gis.supermercados.di.IoDispatcher
import com.gis.supermercados.domain.model.Customer
import com.gis.supermercados.domain.model.Sale
import com.gis.supermercados.domain.repository.CustomerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Clientes opcionales: perfil, historial de compras, credito y fidelizacion. */
@Singleton
class CustomerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GisDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CustomerRepository {

    private val dao get() = database.customerDao()

    override fun observeCustomers(query: String?): Flow<List<Customer>> =
        dao.observeCustomers(query?.trim()?.takeIf { it.isNotEmpty() })
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeActiveCustomerCount(): Flow<Int> = dao.observeActiveCount().flowOn(ioDispatcher)

    override suspend fun getCustomer(customerId: Long): Customer? = withContext(ioDispatcher) {
        dao.getWithStats(customerId)?.toDomain() ?: dao.getById(customerId)?.toDomain()
    }

    override suspend fun getActiveCustomers(): List<Customer> =
        withContext(ioDispatcher) { dao.getActive().map { it.toDomain() } }

    override suspend fun findByDocument(documentId: String): Customer? = withContext(ioDispatcher) {
        val clean = documentId.trim()
        if (clean.isEmpty()) null else dao.getByDocument(clean)?.toDomain()
    }

    override suspend fun saveCustomer(customer: Customer): AppResult<Long> =
        runCatchingApp(TAG, UiText.of(R.string.customer_error_save)) {
            withContext(ioDispatcher) {
                val name = customer.fullName.trim()
                Validators.text(name).throwIfInvalid(R.string.customer_error_name_required)
                Validators.email(customer.email, optional = true)
                    .throwIfInvalid(R.string.customer_error_invalid_email)
                Validators.phone(customer.phone, optional = true)
                    .throwIfInvalid(R.string.customer_error_invalid_phone)
                if (customer.creditLimitCents < 0L) {
                    throw IllegalArgumentException(context.getString(R.string.customer_error_invalid_credit))
                }
                val document = customer.documentId.trim()
                if (document.isNotEmpty()) {
                    val existing = dao.getByDocument(document)
                    if (existing != null && existing.id != customer.id) {
                        throw IllegalArgumentException(
                            context.getString(R.string.customer_error_document_taken, document)
                        )
                    }
                }

                val now = System.currentTimeMillis()
                val entity = customer.toEntity().copy(
                    fullName = name,
                    documentId = document,
                    createdAt = if (customer.id == 0L) now else customer.createdAt
                )
                val id = if (customer.id == 0L) dao.insert(entity) else {
                    dao.update(entity)
                    entity.id
                }
                AppLogger.i(TAG, "Cliente guardado: $name")
                id
            }
        }

    override suspend fun setActive(customerId: Long, isActive: Boolean): AppResult<Unit> =
        runCatchingApp(TAG, UiText.of(R.string.customer_error_update)) {
            withContext(ioDispatcher) {
                val updated = dao.setActive(customerId, isActive)
                if (updated == 0) throw IllegalArgumentException(context.getString(R.string.customer_error_not_found))
                Unit
            }
        }

    override fun observePurchaseHistory(customerId: Long): Flow<List<Sale>> =
        dao.observePurchaseHistory(customerId).map { rows -> rows.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun addLoyaltyPoints(customerId: Long, points: Int): AppResult<Unit> =
        runCatchingApp(TAG, UiText.of(R.string.customer_error_update)) {
            withContext(ioDispatcher) {
                val updated = dao.addLoyaltyPoints(customerId, points.coerceAtLeast(0))
                if (updated == 0) throw IllegalArgumentException(context.getString(R.string.customer_error_not_found))
                Unit
            }
        }

    override suspend fun adjustBalance(customerId: Long, deltaCents: Long): AppResult<Unit> =
        runCatchingApp(TAG, UiText.of(R.string.customer_error_update)) {
            withContext(ioDispatcher) {
                val customer = dao.getById(customerId)
                    ?: throw IllegalArgumentException(context.getString(R.string.customer_error_not_found))
                val newBalance = customer.balanceCents + deltaCents
                if (newBalance < 0L) {
                    throw IllegalArgumentException(context.getString(R.string.customer_error_negative_balance))
                }
                if (deltaCents > 0L && customer.creditLimitCents > 0L && newBalance > customer.creditLimitCents) {
                    throw IllegalArgumentException(
                        context.getString(
                            R.string.customer_error_credit_limit_exceeded,
                            Money.format(customer.creditLimitCents),
                            Money.format(newBalance - customer.creditLimitCents)
                        )
                    )
                }
                dao.adjustBalance(customerId, deltaCents)
                AppLogger.i(TAG, "Saldo del cliente $customerId ajustado en ${Money.format(deltaCents)}")
                Unit
            }
        }

    /** Convierte un fallo de validacion en excepcion con mensaje localizable. */
    private fun ValidationResult.throwIfInvalid(@StringRes fallback: Int) {
        if (isValid) return
        throw IllegalArgumentException(
            errorText?.asString(context) ?: context.getString(fallback)
        )
    }

    private companion object {
        const val TAG = "CustomerRepository"
    }
}
