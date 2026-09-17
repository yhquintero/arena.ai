package com.gis.supermercados.ui.stores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.domain.model.Store
import com.gis.supermercados.domain.model.StorePermissions
import com.gis.supermercados.domain.model.StoreSchedule
import com.gis.supermercados.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Formulario de sucursal (horario y permisos incluidos). */
data class StoreForm(
    val id: Long = 0L,
    val name: String = "",
    val code: String = "",
    val address: String = "",
    val city: String = "",
    val phone: String = "",
    val email: String = "",
    val managerName: String = "",
    val taxId: String = "",
    val openingTime: String = "08:00",
    val closingTime: String = "21:00",
    val openDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val hasExtendedHours: Boolean = false,
    val allowSales: Boolean = true,
    val allowReturns: Boolean = true,
    val allowInventoryAdjust: Boolean = true,
    val allowTransfers: Boolean = true,
    val allowExpenses: Boolean = true,
    val allowReports: Boolean = true,
    val maxDiscountPercent: Int = 20,
    val notes: String = "",
    val isActive: Boolean = true,
) {
    /** Convierte el formulario al modelo de dominio (horario y permisos anidados). */
    fun toStore(): Store = Store(
        id = id,
        name = name.trim(),
        code = code.trim().uppercase(),
        address = address.trim(),
        city = city.trim(),
        phone = phone.trim(),
        email = email.trim(),
        managerName = managerName.trim(),
        taxId = taxId.trim(),
        isActive = isActive,
        schedule = StoreSchedule(
            openDays = openDays,
            openingTime = openingTime.trim(),
            closingTime = closingTime.trim(),
            hasExtendedHours = hasExtendedHours
        ),
        permissions = StorePermissions(
            allowSales = allowSales,
            allowReturns = allowReturns,
            allowInventoryAdjust = allowInventoryAdjust,
            allowTransfers = allowTransfers,
            allowExpenses = allowExpenses,
            allowReports = allowReports,
            maxDiscountPercent = maxDiscountPercent
        ),
        notes = notes.trim()
    )

    companion object {
        fun from(store: Store) = StoreForm(
            id = store.id,
            name = store.name,
            code = store.code,
            address = store.address,
            city = store.city,
            phone = store.phone,
            email = store.email,
            managerName = store.managerName,
            taxId = store.taxId,
            openingTime = store.schedule.openingTime,
            closingTime = store.schedule.closingTime,
            openDays = store.schedule.openDays,
            hasExtendedHours = store.schedule.hasExtendedHours,
            allowSales = store.permissions.allowSales,
            allowReturns = store.permissions.allowReturns,
            allowInventoryAdjust = store.permissions.allowInventoryAdjust,
            allowTransfers = store.permissions.allowTransfers,
            allowExpenses = store.permissions.allowExpenses,
            allowReports = store.permissions.allowReports,
            maxDiscountPercent = store.permissions.maxDiscountPercent,
            notes = store.notes,
            isActive = store.isActive
        )
    }
}

data class StoresUiState(
    val showForm: Boolean = false,
    val form: StoreForm = StoreForm(),
    val isSaving: Boolean = false,
    val error: UiText? = null,
    val message: UiText? = null,
    val pendingDelete: Store? = null,
)

/** CRUD de sucursales con horario y permisos. */
@HiltViewModel
class StoresViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoresUiState())
    val uiState: StateFlow<StoresUiState> = _uiState

    val stores: StateFlow<List<Store>> = storeRepository.observeStores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun openCreate() = _uiState.update { it.copy(showForm = true, form = StoreForm(), error = null) }

    fun openEdit(store: Store) =
        _uiState.update { it.copy(showForm = true, form = StoreForm.from(store), error = null) }

    fun closeForm() = _uiState.update { it.copy(showForm = false) }

    fun updateForm(transform: (StoreForm) -> StoreForm) =
        _uiState.update { it.copy(form = transform(it.form)) }

    fun save() {
        val form = _uiState.value.form
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            when (val result = storeRepository.saveStore(form.toStore())) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        showForm = false,
                        message = UiText.of(if (form.id == 0L) R.string.store_created else R.string.store_updated)
                    )
                }

                is AppResult.Failure -> _uiState.update { state -> state.copy(isSaving = false, error = result.error) }
            }
        }
    }

    fun toggleActive(store: Store) {
        viewModelScope.launch {
            val result = storeRepository.setActive(store.id, !store.isActive)
            _uiState.update { it.copy(error = (result as? AppResult.Failure)?.error) }
        }
    }

    fun requestDelete(store: Store) = _uiState.update { it.copy(pendingDelete = store) }

    fun cancelDelete() = _uiState.update { it.copy(pendingDelete = null) }

    fun confirmDelete() {
        val store = _uiState.value.pendingDelete ?: return
        viewModelScope.launch {
            when (val result = storeRepository.deleteStore(store.id)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(pendingDelete = null, message = UiText.of(R.string.store_deleted))
                }

                is AppResult.Failure -> _uiState.update { state -> state.copy(pendingDelete = null, error = result.error) }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
