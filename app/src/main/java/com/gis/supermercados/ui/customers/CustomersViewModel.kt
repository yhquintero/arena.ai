package com.gis.supermercados.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.domain.model.Customer
import com.gis.supermercados.domain.model.DocumentType
import com.gis.supermercados.domain.model.Sale
import com.gis.supermercados.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerForm(
    val id: Long = 0L,
    val fullName: String = "",
    val documentType: DocumentType = DocumentType.CEDULA,
    val documentId: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val creditLimitText: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val loyaltyPoints: Int = 0,
    val balanceCents: Long = 0L,
) {
    fun toCustomer(createdAt: Long): Customer = Customer(
        id = id,
        fullName = fullName.trim(),
        documentType = documentType,
        documentId = documentId.trim(),
        phone = phone.trim(),
        email = email.trim(),
        address = address.trim(),
        city = city.trim(),
        loyaltyPoints = loyaltyPoints,
        creditLimitCents = Money.parse(creditLimitText) ?: 0L,
        balanceCents = balanceCents,
        notes = notes.trim(),
        isActive = isActive,
        createdAt = createdAt
    )

    companion object {
        fun from(customer: Customer) = CustomerForm(
            id = customer.id,
            fullName = customer.fullName,
            documentType = customer.documentType,
            documentId = customer.documentId,
            phone = customer.phone,
            email = customer.email,
            address = customer.address,
            city = customer.city,
            creditLimitText = if (customer.creditLimitCents > 0L) Money.formatNumber(customer.creditLimitCents) else "",
            notes = customer.notes,
            isActive = customer.isActive,
            loyaltyPoints = customer.loyaltyPoints,
            balanceCents = customer.balanceCents
        )
    }
}

data class CustomersUiState(
    val query: String = "",
    val showForm: Boolean = false,
    val form: CustomerForm = CustomerForm(),
    val isSaving: Boolean = false,
    val error: UiText? = null,
    val message: UiText? = null,
    val detailCustomer: Customer? = null,
    val detailHistory: List<Sale> = emptyList(),
)

/** Clientes: ficha, credito, puntos e historial de compras. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CustomersViewModel @Inject constructor(
    private val customerRepository: CustomerRepository,
) : ViewModel() {

    private val query = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(CustomersUiState())
    val uiState: StateFlow<CustomersUiState> = _uiState

    val customers: StateFlow<List<Customer>> = query
        .flatMapLatest { current -> customerRepository.observeCustomers(current) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun onQueryChange(value: String) {
        query.value = value.trim().takeIf { it.isNotEmpty() }
        _uiState.update { it.copy(query = value) }
    }

    fun openCreate() = _uiState.update { it.copy(showForm = true, form = CustomerForm(), error = null) }

    fun openEdit(customer: Customer) =
        _uiState.update { it.copy(showForm = true, form = CustomerForm.from(customer), error = null) }

    fun closeForm() = _uiState.update { it.copy(showForm = false) }

    fun updateForm(transform: (CustomerForm) -> CustomerForm) =
        _uiState.update { it.copy(form = transform(it.form)) }

    fun save() {
        val form = _uiState.value.form
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val result = customerRepository.saveCustomer(form.toCustomer(System.currentTimeMillis()))
            when (result) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        showForm = false,
                        message = UiText.of(if (form.id == 0L) R.string.customer_created else R.string.customer_updated)
                    )
                }

                is AppResult.Failure -> _uiState.update { state -> state.copy(isSaving = false, error = result.error) }
            }
        }
    }

    fun toggleActive(customer: Customer) {
        viewModelScope.launch {
            val result = customerRepository.setActive(customer.id, !customer.isActive)
            _uiState.update { it.copy(error = (result as? AppResult.Failure)?.error) }
        }
    }

    /** Abre la ficha con su historial de compras. */
    fun openDetail(customer: Customer) {
        _uiState.update { it.copy(detailCustomer = customer, detailHistory = emptyList()) }
        viewModelScope.launch {
            val history = customerRepository.observePurchaseHistory(customer.id).first()
            _uiState.update { state ->
                if (state.detailCustomer?.id == customer.id) state.copy(detailHistory = history) else state
            }
        }
    }

    fun closeDetail() = _uiState.update { it.copy(detailCustomer = null, detailHistory = emptyList()) }

    /** Ajusta el saldo pendiente (cobro de credito o nueva venta a credito). */
    fun adjustBalance(deltaCents: Long) {
        val customer = _uiState.value.detailCustomer ?: return
        viewModelScope.launch {
            val result = customerRepository.adjustBalance(customer.id, deltaCents)
            when (result) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        message = UiText.of(R.string.customer_balance_updated),
                        detailCustomer = customerRepository.getCustomer(customer.id) ?: it.detailCustomer
                    )
                }

                is AppResult.Failure -> _uiState.update { state -> state.copy(error = result.error) }
            }
        }
    }

    fun addLoyaltyPoints(points: Int) {
        val customer = _uiState.value.detailCustomer ?: return
        viewModelScope.launch {
            val result = customerRepository.addLoyaltyPoints(customer.id, points)
            _uiState.update {
                it.copy(
                    error = (result as? AppResult.Failure)?.error,
                    message = if (result.isSuccess) UiText.of(R.string.customer_points_updated) else null
                )
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
