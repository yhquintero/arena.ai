package com.gis.supermercados.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppConstants
import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.domain.model.Expense
import com.gis.supermercados.domain.model.ExpenseCategory
import com.gis.supermercados.domain.model.ExpenseFilter
import com.gis.supermercados.domain.model.PaymentMethod
import com.gis.supermercados.domain.model.Store
import com.gis.supermercados.domain.repository.AuthRepository
import com.gis.supermercados.domain.repository.ExpenseRepository
import com.gis.supermercados.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Formulario de gasto. `storeId == null` => gasto corporativo. */
data class ExpenseForm(
    val id: Long = 0L,
    val category: ExpenseCategory = ExpenseCategory.OTROS,
    val storeId: Long? = null,
    val concept: String = "",
    val provider: String = "",
    val amountText: String = "",
    val taxRatePercent: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    val reference: String = "",
    val expenseDate: Long = System.currentTimeMillis(),
    val receiptPath: String = "",
) {
    val amountCents: Long get() = Money.parse(amountText) ?: 0L
    val taxRate: Double get() = (taxRatePercent.replace(',', '.').toDoubleOrNull() ?: 0.0) / 100.0
    val taxCents: Long get() = Money.tax(amountCents, taxRate)
    val totalCents: Long get() = amountCents + taxCents

    fun toExpense(userId: Long, createdAt: Long): Expense = Expense(
        id = id,
        category = category,
        storeId = storeId,
        concept = concept.trim(),
        provider = provider.trim(),
        amountCents = amountCents,
        taxCents = taxCents,
        totalCents = totalCents,
        paymentMethod = paymentMethod,
        reference = reference.trim(),
        receiptPath = receiptPath,
        hasReceipt = receiptPath.isNotBlank(),
        userId = userId,
        expenseDate = expenseDate,
        createdAt = createdAt
    )

    companion object {
        fun from(expense: Expense) = ExpenseForm(
            id = expense.id,
            category = expense.category,
            storeId = expense.storeId,
            concept = expense.concept,
            provider = expense.provider,
            amountText = Money.formatNumber(expense.amountCents),
            taxRatePercent = if (expense.amountCents > 0L) {
                (expense.taxCents * 100.0 / expense.amountCents).let { percent ->
                    String.format(java.util.Locale("es"), "%.1f", percent)
                }
            } else {
                "0"
            },
            paymentMethod = expense.paymentMethod,
            reference = expense.reference,
            expenseDate = expense.expenseDate,
            receiptPath = expense.receiptPath
        )
    }
}

data class ExpensesUiState(
    val stores: List<Store> = emptyList(),
    val filterStoreId: Long? = null,
    val filterCategory: ExpenseCategory? = null,
    val filterPeriod: ExpensePeriod = ExpensePeriod.MONTH,
    val query: String = "",
    val showForm: Boolean = false,
    val form: ExpenseForm = ExpenseForm(),
    val isSaving: Boolean = false,
    val error: UiText? = null,
    val message: UiText? = null,
    val pendingDelete: Expense? = null,
)

/** Periodos rapidos del listado de gastos. */
enum class ExpensePeriod {
    HOY, SEMANA, MES, TRIMESTRE, TODO;

    fun range(now: Long = System.currentTimeMillis()): Pair<Long?, Long?> = when (this) {
        HOY -> AppDateTime.startOfDay(now) to AppDateTime.endOfDay(now)
        SEMANA -> AppDateTime.startOfWeek(now) to AppDateTime.endOfWeek(now)
        MES -> AppDateTime.startOfMonth(now) to AppDateTime.endOfMonth(now)
        TRIMESTRE -> AppDateTime.startOfMonth(AppDateTime.minusDays(now, 60)) to AppDateTime.endOfMonth(now)
        TODO -> null to null
    }
}

/** Gastos operacionales por categoria y sucursal (o corporativos). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    storeRepository: StoreRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private data class Filters(val storeId: Long?, val category: ExpenseCategory?, val period: ExpensePeriod, val query: String?)

    private val filters = MutableStateFlow(Filters(null, null, ExpensePeriod.MONTH, null))
    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState

    val expenses: StateFlow<List<Expense>> = filters
        .flatMapLatest { current ->
            val (from, to) = current.period.range()
            expenseRepository.observeExpenses(
                ExpenseFilter(
                    storeId = current.storeId,
                    category = current.category,
                    from = from,
                    to = to,
                    query = current.query,
                    limit = MAX_ROWS
                )
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        viewModelScope.launch {
            val stores = storeRepository.getActiveStores()
            val session = authRepository.session.value
            _uiState.update {
                it.copy(
                    stores = stores,
                    form = it.form.copy(
                        storeId = session?.storeId,
                        taxRatePercent = (AppConstants.DEFAULT_TAX_RATE * 100).toString()
                    )
                )
            }
        }
    }

    fun onStoreFilter(storeId: Long?) {
        filters.value = filters.value.copy(storeId = storeId)
        _uiState.update { it.copy(filterStoreId = storeId) }
    }

    fun onCategoryFilter(category: ExpenseCategory?) {
        filters.value = filters.value.copy(category = category)
        _uiState.update { it.copy(filterCategory = category) }
    }

    fun onPeriodFilter(period: ExpensePeriod) {
        filters.value = filters.value.copy(period = period)
        _uiState.update { it.copy(filterPeriod = period) }
    }

    fun onQueryChange(query: String) {
        filters.value = filters.value.copy(query = query.trim().takeIf { it.isNotEmpty() })
        _uiState.update { it.copy(query = query) }
    }

    fun openCreate() = _uiState.update {
        it.copy(
            showForm = true,
            error = null,
            form = ExpenseForm(
                storeId = it.filterStoreId ?: authRepository.session.value?.storeId,
                category = it.filterCategory ?: ExpenseCategory.OTROS,
                taxRatePercent = (AppConstants.DEFAULT_TAX_RATE * 100).toString()
            )
        )
    }

    fun openEdit(expense: Expense) =
        _uiState.update { it.copy(showForm = true, form = ExpenseForm.from(expense), error = null) }

    fun closeForm() = _uiState.update { it.copy(showForm = false) }

    fun updateForm(transform: (ExpenseForm) -> ExpenseForm) =
        _uiState.update { it.copy(form = transform(it.form)) }

    fun save() {
        val form = _uiState.value.form
        val userId = authRepository.session.value?.userId ?: 0L
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val result = expenseRepository.saveExpense(
                form.toExpense(userId = userId, createdAt = System.currentTimeMillis())
            )
            when (result) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        showForm = false,
                        message = UiText.of(if (form.id == 0L) R.string.expense_created else R.string.expense_updated)
                    )
                }

                is AppResult.Failure -> _uiState.update { state -> state.copy(isSaving = false, error = result.error) }
            }
        }
    }

    /** Adjunta el justificante elegido con el selector de archivos del sistema. */
    fun attachReceipt(uri: String) {
        val expenseId = _uiState.value.form.id
        if (expenseId == 0L) {
            _uiState.update { it.copy(message = UiText.of(R.string.expense_receipt_save_first)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = expenseRepository.attachReceipt(expenseId, uri)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isSaving = false, form = it.form.copy(receiptPath = result.data), message = UiText.of(R.string.expense_receipt_attached))
                }

                is AppResult.Failure -> _uiState.update { state -> state.copy(isSaving = false, error = result.error) }
            }
        }
    }

    fun requestDelete(expense: Expense) = _uiState.update { it.copy(pendingDelete = expense) }

    fun cancelDelete() = _uiState.update { it.copy(pendingDelete = null) }

    fun confirmDelete() {
        val expense = _uiState.value.pendingDelete ?: return
        viewModelScope.launch {
            when (val result = expenseRepository.deleteExpense(expense.id)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(pendingDelete = null, message = UiText.of(R.string.expense_deleted))
                }

                is AppResult.Failure -> _uiState.update { state -> state.copy(pendingDelete = null, error = result.error) }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    private companion object {
        const val MAX_ROWS = 500
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
