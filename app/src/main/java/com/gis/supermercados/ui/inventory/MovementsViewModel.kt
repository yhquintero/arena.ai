package com.gis.supermercados.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.domain.model.InventoryMovement
import com.gis.supermercados.domain.model.MovementRequest
import com.gis.supermercados.domain.model.MovementType
import com.gis.supermercados.domain.model.Product
import com.gis.supermercados.domain.model.Store
import com.gis.supermercados.domain.model.TransferRequest
import com.gis.supermercados.domain.repository.AuthRepository
import com.gis.supermercados.domain.repository.CatalogRepository
import com.gis.supermercados.domain.repository.InventoryRepository
import com.gis.supermercados.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovementsUiState(
    val stores: List<Store> = emptyList(),
    val products: List<Product> = emptyList(),
    val filterStoreId: Long? = null,
    val filterType: MovementType? = null,
    val showForm: Boolean = false,
    val showTransferForm: Boolean = false,
    val isProcessing: Boolean = false,
    val error: UiText? = null,
    val message: UiText? = null,
    // formulario de movimiento
    val formProductId: Long? = null,
    val formStoreId: Long? = null,
    val formType: MovementType = MovementType.ENTRADA,
    val formQuantity: String = "",
    val formUnitCost: String = "",
    val formReason: String = "",
    val formReference: String = "",
    // formulario de transferencia
    val transferProductId: Long? = null,
    val transferFromStoreId: Long? = null,
    val transferToStoreId: Long? = null,
    val transferQuantity: String = "",
    val transferReference: String = "",
)

/** Historial de movimientos, altas/bajas y transferencias entre sucursales. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MovementsViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val catalogRepository: CatalogRepository,
    private val storeRepository: StoreRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private data class Filters(val storeId: Long?, val type: MovementType?)

    private val filters = MutableStateFlow(Filters(null, null))
    private val _uiState = MutableStateFlow(MovementsUiState())
    val uiState: StateFlow<MovementsUiState> = _uiState

    val movements: StateFlow<List<InventoryMovement>> = filters
        .flatMapLatest { current ->
            inventoryRepository.observeMovements(current.storeId, null, current.type)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        viewModelScope.launch {
            val stores = storeRepository.getActiveStores()
            val session = authRepository.session.value
            val defaultStore = stores.firstOrNull { it.id == session?.storeId }?.id ?: stores.firstOrNull()?.id
            _uiState.update {
                it.copy(stores = stores, formStoreId = defaultStore, transferFromStoreId = defaultStore)
            }
        }
        viewModelScope.launch {
            catalogRepository.observeProducts(null, null).collect { products ->
                _uiState.update { it.copy(products = products.filter { product -> product.isActive }) }
            }
        }
    }

    fun onFilterStore(storeId: Long?) {
        filters.value = filters.value.copy(storeId = storeId)
        _uiState.update { it.copy(filterStoreId = storeId) }
    }

    fun onFilterType(type: MovementType?) {
        filters.value = filters.value.copy(type = type)
        _uiState.update { it.copy(filterType = type) }
    }

    fun openMovementForm() = _uiState.update { it.copy(showForm = true, error = null) }

    fun closeMovementForm() = _uiState.update { it.copy(showForm = false) }

    fun openTransferForm() = _uiState.update { it.copy(showTransferForm = true, error = null) }

    fun closeTransferForm() = _uiState.update { it.copy(showTransferForm = false) }

    fun onFormProduct(productId: Long) = _uiState.update { it.copy(formProductId = productId) }

    fun onFormStore(storeId: Long) = _uiState.update { it.copy(formStoreId = storeId) }

    fun onFormType(type: MovementType) = _uiState.update { it.copy(formType = type) }

    fun onFormQuantity(value: String) = _uiState.update { it.copy(formQuantity = value.filter(Char::isDigit)) }

    fun onFormUnitCost(value: String) =
        _uiState.update { it.copy(formUnitCost = value.filter { char -> char.isDigit() || char == ',' || char == '.' }) }

    fun onFormReason(value: String) = _uiState.update { it.copy(formReason = value) }

    fun onFormReference(value: String) = _uiState.update { it.copy(formReference = value) }

    fun onTransferProduct(productId: Long) = _uiState.update { it.copy(transferProductId = productId) }

    fun onTransferFrom(storeId: Long) = _uiState.update { it.copy(transferFromStoreId = storeId) }

    fun onTransferTo(storeId: Long) = _uiState.update { it.copy(transferToStoreId = storeId) }

    fun onTransferQuantity(value: String) = _uiState.update { it.copy(transferQuantity = value.filter(Char::isDigit)) }

    fun onTransferReference(value: String) = _uiState.update { it.copy(transferReference = value) }

    /** Registra entrada/salida/ajuste con su movimiento de existencias. */
    fun submitMovement() {
        val state = _uiState.value
        val productId = state.formProductId
        val storeId = state.formStoreId
        val quantity = state.formQuantity.toIntOrNull() ?: 0
        if (productId == null || storeId == null || quantity <= 0) {
            _uiState.update { it.copy(error = UiText.of(R.string.inventory_error_form_incomplete)) }
            return
        }
        val userId = authRepository.session.value?.userId ?: 0L
        val unitCost = com.gis.supermercados.core.common.Money.parse(state.formUnitCost)
            ?: state.products.firstOrNull { it.id == productId }?.costCents
            ?: 0L

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            val result = inventoryRepository.registerMovement(
                MovementRequest(
                    productId = productId,
                    storeId = storeId,
                    type = state.formType,
                    quantity = quantity,
                    unitCostCents = unitCost,
                    reason = state.formReason.trim(),
                    reference = state.formReference.trim(),
                    userId = userId
                )
            )
            when (result) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isProcessing = false,
                        showForm = false,
                        message = UiText.of(R.string.inventory_movement_saved),
                        formQuantity = "",
                        formReason = "",
                        formReference = ""
                    )
                }

                is AppResult.Failure -> _uiState.update { it.copy(isProcessing = false, error = result.error) }
            }
        }
    }

    /** Transfiere existencias entre dos sucursales. */
    fun submitTransfer() {
        val state = _uiState.value
        val productId = state.transferProductId
        val from = state.transferFromStoreId
        val to = state.transferToStoreId
        val quantity = state.transferQuantity.toIntOrNull() ?: 0
        if (productId == null || from == null || to == null || quantity <= 0) {
            _uiState.update { it.copy(error = UiText.of(R.string.inventory_error_form_incomplete)) }
            return
        }
        if (from == to) {
            _uiState.update { it.copy(error = UiText.of(R.string.inventory_error_same_store)) }
            return
        }
        val userId = authRepository.session.value?.userId ?: 0L
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            val result = inventoryRepository.transferStock(
                TransferRequest(
                    productId = productId,
                    fromStoreId = from,
                    toStoreId = to,
                    quantity = quantity,
                    userId = userId,
                    reference = state.transferReference.trim()
                )
            )
            when (result) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isProcessing = false,
                        showTransferForm = false,
                        message = UiText.of(R.string.inventory_transfer_saved),
                        transferQuantity = "",
                        transferReference = ""
                    )
                }

                is AppResult.Failure -> _uiState.update { it.copy(isProcessing = false, error = result.error) }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
