package com.gis.supermercados.ui.sales

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.domain.model.ReturnLine
import com.gis.supermercados.domain.model.ReturnRequest
import com.gis.supermercados.domain.model.Role
import com.gis.supermercados.domain.model.SaleDetail
import com.gis.supermercados.domain.repository.AuthRepository
import com.gis.supermercados.domain.repository.SalesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SaleDetailUiState(
    val isLoading: Boolean = true,
    val detail: SaleDetail? = null,
    val error: UiText? = null,
    val message: UiText? = null,
    val isProcessing: Boolean = false,
    val showReturnDialog: Boolean = false,
    val showVoidDialog: Boolean = false,
    val returnQuantities: Map<Long, Int> = emptyMap(),
    val returnReason: String = "",
    val restock: Boolean = true,
    val canApproveReturns: Boolean = false,
)

/** Detalle de venta, devolucion (nota de credito) y anulacion. */
@HiltViewModel
class SaleDetailViewModel @Inject constructor(
    private val salesRepository: SalesRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val saleId: Long = savedStateHandle.get<Long>(KEY_SALE_ID) ?: 0L

    private val _uiState = MutableStateFlow(SaleDetailUiState())
    val uiState: StateFlow<SaleDetailUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(canApproveReturns = authRepository.session.value?.role?.canApproveReturns ?: false)
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val detail = runCatching { salesRepository.getSaleDetail(saleId) }
                .onFailure { AppLogger.e(TAG, "No se pudo cargar la venta $saleId", it) }
                .getOrNull()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    detail = detail,
                    error = if (detail == null) UiText.of(R.string.sale_detail_error_load) else null,
                    returnQuantities = emptyMap()
                )
            }
        }
    }

    fun openReturnDialog() = _uiState.update {
        it.copy(showReturnDialog = true, returnReason = "", restock = true, returnQuantities = emptyMap())
    }

    fun closeReturnDialog() = _uiState.update { it.copy(showReturnDialog = false) }

    fun onReturnQuantityChange(saleItemId: Long, quantity: Int) = _uiState.update { state ->
        val max = state.detail?.items?.firstOrNull { it.id == saleItemId }?.returnableQuantity ?: 0
        val coerced = quantity.coerceIn(0, max)
        val map = state.returnQuantities.toMutableMap()
        if (coerced <= 0) map.remove(saleItemId) else map[saleItemId] = coerced
        state.copy(returnQuantities = map)
    }

    fun onReturnReasonChange(reason: String) = _uiState.update { it.copy(returnReason = reason) }

    fun onRestockChange(restock: Boolean) = _uiState.update { it.copy(restock = restock) }

    /** Crea la nota de credito y repon existencias si se indico. */
    fun confirmReturn() {
        val state = _uiState.value
        val detail = state.detail ?: return
        val session = authRepository.session.value
        if (session == null) {
            _uiState.update { it.copy(error = UiText.of(R.string.pos_error_no_session)) }
            return
        }
        val lines = state.returnQuantities.mapNotNull { (itemId, quantity) ->
            if (quantity <= 0) return@mapNotNull null
            val item = detail.items.firstOrNull { it.id == itemId } ?: return@mapNotNull null
            ReturnLine(
                saleItemId = item.id,
                productName = item.productName,
                sku = item.sku,
                maxQuantity = item.returnableQuantity,
                quantity = quantity,
                unitPriceCents = item.unitPriceCents,
                taxRate = item.taxRate
            )
        }
        if (lines.isEmpty()) {
            _uiState.update { it.copy(error = UiText.of(R.string.return_error_no_lines)) }
            return
        }
        if (state.returnReason.isBlank()) {
            _uiState.update { it.copy(error = UiText.of(R.string.return_error_reason_required)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            val result = salesRepository.createCreditNote(
                ReturnRequest(
                    saleId = detail.sale.id,
                    userId = session.userId,
                    lines = lines,
                    reason = state.returnReason,
                    restock = state.restock
                )
            )
            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            showReturnDialog = false,
                            message = UiText.of(R.string.return_success, result.data.creditNoteNumber)
                        )
                    }
                    load()
                }

                is AppResult.Failure -> _uiState.update { it.copy(isProcessing = false, error = result.error) }
            }
        }
    }

    fun openVoidDialog() = _uiState.update { it.copy(showVoidDialog = true) }

    fun closeVoidDialog() = _uiState.update { it.copy(showVoidDialog = false) }

    /** Anula la venta y devuelve existencias. Solo roles con permiso. */
    fun voidSale() {
        val session = authRepository.session.value ?: return
        if (session.role == Role.CAJERO) {
            _uiState.update { it.copy(error = UiText.of(R.string.sale_error_no_permission)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            val result = salesRepository.voidSale(saleId, session.userId)
            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(isProcessing = false, showVoidDialog = false, message = UiText.of(R.string.sale_void_success))
                    }
                    load()
                }

                is AppResult.Failure -> _uiState.update { it.copy(isProcessing = false, error = result.error) }
            }
        }
    }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    private companion object {
        const val TAG = "SaleDetailViewModel"
        const val KEY_SALE_ID = "saleId"
    }
}
