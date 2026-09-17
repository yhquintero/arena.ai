package com.gis.supermercados.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.domain.model.Category
import com.gis.supermercados.domain.model.Product
import com.gis.supermercados.domain.repository.CatalogRepository
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

data class ProductsUiState(
    val query: String = "",
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val message: UiText? = null,
    val error: UiText? = null,
    val pendingDelete: Product? = null,
)

/** Listado de productos/servicios con busqueda y filtro por categoria. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private data class Filters(val query: String?, val categoryId: Long?)

    private val filters = MutableStateFlow(Filters(null, null))
    private val _uiState = MutableStateFlow(ProductsUiState())
    val uiState: StateFlow<ProductsUiState> = _uiState

    val products: StateFlow<List<Product>> = filters
        .flatMapLatest { current -> catalogRepository.observeProducts(current.query, current.categoryId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val categories: StateFlow<List<Category>> = catalogRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun onQueryChange(query: String) {
        filters.value = filters.value.copy(query = query.trim().takeIf { it.isNotEmpty() })
        _uiState.update { it.copy(query = query) }
    }

    fun onCategorySelected(categoryId: Long?) {
        filters.value = filters.value.copy(categoryId = categoryId)
        _uiState.update { it.copy(categoryId = categoryId) }
    }

    fun requestDelete(product: Product) = _uiState.update { it.copy(pendingDelete = product) }

    fun cancelDelete() = _uiState.update { it.copy(pendingDelete = null) }

    /** Baja del producto: conserva el historico de ventas (foto del momento). */
    fun confirmDelete() {
        val product = _uiState.value.pendingDelete ?: return
        viewModelScope.launch {
            val result = catalogRepository.deleteProduct(product.id)
            when (result) {
                is AppResult.Success -> _uiState.update {
                    it.copy(pendingDelete = null, message = UiText.of(R.string.product_deleted))
                }

                is AppResult.Failure -> _uiState.update { it.copy(pendingDelete = null, error = result.error) }
            }
        }
    }

    fun toggleActive(product: Product) {
        viewModelScope.launch {
            val result = catalogRepository.setActive(product.id, !product.isActive)
            _uiState.update {
                it.copy(error = (result as? AppResult.Failure)?.error)
            }
        }
    }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
