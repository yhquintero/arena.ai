package com.gis.supermercados.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.domain.model.InventoryValue
import com.gis.supermercados.domain.model.MovementRequest
import com.gis.supermercados.domain.model.MovementType
import com.gis.supermercados.domain.model.StockLevel
import com.gis.supermercados.domain.model.Store
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
import kotlin.math.abs
import javax.inject.Inject

data class InventoryUiState(
    val stores: List<Store> = emptyList(),
    val selectedStoreId: Long? = null,
    val query: String = "",
    val onlyLowStock: Boolean = false,
    val value: InventoryValue = InventoryValue(),
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val adjustTarget: StockLevel? = null,
    val adjustQuantity: String = "",
    val adjustMin: String = "",
    val adjustMax: String = "",
    val isProcessing: Boolean = false,
    val error: UiText? = null,
    val message: UiText? = null,
)

/** Existencias por sucursal, valoracion y alertas. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    storeRepository: StoreRepository,
) : ViewModel() {

    private data class Filters(val storeId: Long?, val query: String?, val onlyLowStock: Boolean)

    private val filters = MutableStateFlow(Filters(null, null, false))
    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState

    val stocks: StateFlow<List<StockLevel>> = filters
        .flatMapLatest { current ->
            inventoryRepository.observeStock(current.storeId, current.query, current.onlyLowStock)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        viewModelScope.launch {
            combine(storeRepository.observeActiveStores(), filters) { stores, current -> stores to current }
                .collect { (stores, current) ->
                    _uiState.update { it.copy(stores = stores, selectedStoreId = current.storeId) }
                    refreshValue(current.storeId)
                }
        }
        viewModelScope.launch {
            combine(
                inventoryRepository.observeLowStockCount(),
                inventoryRepository.observeOutOfStockCount()
            ) { low, out -> low to out }.collect { (low, out) ->
                _uiState.update { it.copy(lowStockCount = low, outOfStockCount = out) }
            }
        }
    }

    private suspend fun refreshValue(storeId: Long?) {
        val value = runCatching { inventoryRepository.inventoryValue(storeId) }.getOrDefault(InventoryValue())
        _uiState.update { it.copy(value = value) }
    }

    fun onStoreSelected(storeId: Long?) {
        filters.value = filters.value.copy(storeId = storeId)
    }

    fun onQueryChange(query: String) {
        filters.value = filters.value.copy(query = query.trim().takeIf { it.isNotEmpty() })
        _uiState.update { it.copy(query = query) }
    }

    fun onOnlyLowStockChange(enabled: Boolean) {
        filters.value = filters.value.copy(onlyLowStock = enabled)
        _uiState.update { it.copy(onlyLowStock = enabled) }
    }

    fun openAdjust(stock: StockLevel) {
        _uiState.update {
            it.copy(
                adjustTarget = stock,
                adjustQuantity = stock.quantity.toString(),
                adjustMin = stock.minStock.toString(),
                adjustMax = if (stock.maxStock > 0) stock.maxStock.toString() else ""
            )
        }
    }

    fun closeAdjust() = _uiState.update { it.copy(adjustTarget = null, error = null) }

    fun onAdjustQuantityChange(value: String) =
        _uiState.update { it.copy(adjustQuantity = value.filter(Char::isDigit)) }

    fun onAdjustMinChange(value: String) =
        _uiState.update { it.copy(adjustMin = value.filter(Char::isDigit)) }

    fun onAdjustMaxChange(value: String) =
        _uiState.update { it.copy(adjustMax = value.filter(Char::isDigit)) }

    /** Ajuste manual de existencias y umbrales (deja traza en movimientos). */
    fun saveAdjust() {
        val state = _uiState.value
        val target = state.adjustTarget ?: return
        val quantity = state.adjustQuantity.toIntOrNull()
        val min = state.adjustMin.toIntOrNull() ?: 0
        val max = state.adjustMax.toIntOrNull() ?: 0
        if (quantity == null || quantity < 0) {
            _uiState.update { it.copy(error = UiText.of(R.string.inventory_error_invalid_quantity)) }
            return
        }
        val delta = quantity - target.quantity
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            val result = if (delta != 0) {
                inventoryRepository.registerMovement(
                    MovementRequest(
                        productId = target.productId,
                        storeId = target.storeId,
                        type = if (delta > 0) MovementType.AJUSTE_POSITIVO else MovementType.AJUSTE_NEGATIVO,
                        quantity = abs(delta),
                        unitCostCents = target.costCents,
                        reason = "Ajuste manual de inventario"
                    )
                )
            } else {
                AppResult.Success(0L)
            }
            when (result) {
                is AppResult.Success -> {
                    val thresholds = inventoryRepository.setThresholds(target.productId, target.storeId, min, max)
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            adjustTarget = null,
                            message = UiText.of(R.string.inventory_adjust_saved),
                            error = (thresholds as? AppResult.Failure)?.error
                        )
                    }
                    refreshValue(filters.value.storeId)
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
