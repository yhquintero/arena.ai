package com.gis.supermercados.ui.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppConstants
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.common.Validators
import com.gis.supermercados.domain.model.Category
import com.gis.supermercados.domain.model.Product
import com.gis.supermercados.domain.model.ProductUnit
import com.gis.supermercados.domain.repository.CatalogRepository
import com.gis.supermercados.domain.repository.InventoryRepository
import com.gis.supermercados.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductEditUiState(
    val productId: Long = 0L,
    val name: String = "",
    val sku: String = "",
    val barcode: String = "",
    val description: String = "",
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val costText: String = "",
    val priceText: String = "",
    val taxRatePercent: String = "",
    val unit: ProductUnit = ProductUnit.UNIDAD,
    val isService: Boolean = false,
    val supplierName: String = "",
    val supplierPhone: String = "",
    val minStockGlobal: Int = AppConstants.DEFAULT_LOW_STOCK,
    val isActive: Boolean = true,
    val initialStock: Map<Long, String> = emptyMap(),
    val storeNames: Map<Long, String> = emptyMap(),
    val newCategoryName: String = "",
    val showNewCategory: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: UiText? = null,
    val saved: Boolean = false,
    val marginPercent: Double = 0.0,
)

/** Alta/edicion de producto con existencias iniciales por sucursal. */
@HiltViewModel
class ProductEditViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val inventoryRepository: InventoryRepository,
    storeRepository: StoreRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val productId: Long = savedStateHandle.get<Long>(KEY_PRODUCT_ID) ?: 0L
    private val presetStoreId: Long = savedStateHandle.get<Long>(KEY_STORE_ID) ?: 0L

    private val _uiState = MutableStateFlow(ProductEditUiState(productId = productId))
    val uiState: StateFlow<ProductEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val categories = catalogRepository.getCategories()
            val stores = storeRepository.getActiveStores()
            _uiState.update {
                it.copy(
                    categories = categories,
                    storeNames = stores.associate { store -> store.id to store.name },
                    initialStock = stores.associate { store -> store.id to "" }
                )
            }
            if (productId > 0L) {
                val product = catalogRepository.getProduct(productId)
                product?.let { existing ->
                    _uiState.update {
                        it.copy(
                            name = existing.name,
                            sku = existing.sku,
                            barcode = existing.barcode,
                            description = existing.description,
                            categoryId = existing.categoryId,
                            costText = Money.formatNumber(existing.costCents),
                            priceText = Money.formatNumber(existing.priceCents),
                            taxRatePercent = (existing.taxRate * 100).toString(),
                            unit = existing.unit,
                            isService = existing.isService,
                            supplierName = existing.supplierName,
                            supplierPhone = existing.supplierPhone,
                            minStockGlobal = existing.minStockGlobal,
                            isActive = existing.isActive,
                            isLoading = false
                        )
                    }
                } ?: _uiState.update { it.copy(isLoading = false, error = UiText.of(R.string.product_error_not_found)) }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        categoryId = categories.firstOrNull()?.id,
                        taxRatePercent = (AppConstants.DEFAULT_TAX_RATE * 100).toString()
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }

    fun onSkuChange(value: String) = _uiState.update { it.copy(sku = value.uppercase()) }

    fun onBarcodeChange(value: String) = _uiState.update { it.copy(barcode = value) }

    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    fun onCategorySelected(categoryId: Long) = _uiState.update { it.copy(categoryId = categoryId) }

    fun onCostChange(value: String) = _uiState.update { state ->
        val cleaned = value.filter { char -> char.isDigit() || char == ',' || char == '.' }
        val updated = state.copy(costText = cleaned)
        updated.copy(marginPercent = marginOf(updated))
    }

    fun onPriceChange(value: String) = _uiState.update { state ->
        val cleaned = value.filter { char -> char.isDigit() || char == ',' || char == '.' }
        val updated = state.copy(priceText = cleaned)
        updated.copy(marginPercent = marginOf(updated))
    }

    fun onTaxRateChange(value: String) = _uiState.update {
        it.copy(taxRatePercent = value.filter { char -> char.isDigit() || char == ',' || char == '.' })
    }

    fun onUnitChange(unit: ProductUnit) = _uiState.update { it.copy(unit = unit) }

    fun onServiceChange(isService: Boolean) = _uiState.update {
        it.copy(isService = isService, unit = if (isService) ProductUnit.SERVICIO else it.unit)
    }

    fun onSupplierNameChange(value: String) = _uiState.update { it.copy(supplierName = value) }

    fun onSupplierPhoneChange(value: String) = _uiState.update { it.copy(supplierPhone = value) }

    fun onMinStockChange(value: String) = _uiState.update {
        it.copy(minStockGlobal = value.filter(Char::isDigit).toIntOrNull() ?: 0)
    }

    fun onActiveChange(isActive: Boolean) = _uiState.update { it.copy(isActive = isActive) }

    fun onInitialStockChange(storeId: Long, value: String) = _uiState.update {
        it.copy(initialStock = it.initialStock + (storeId to value.filter(Char::isDigit)))
    }

    fun showNewCategory(show: Boolean) = _uiState.update { it.copy(showNewCategory = show, newCategoryName = "") }

    fun onNewCategoryNameChange(value: String) = _uiState.update { it.copy(newCategoryName = value) }

    /** Crea la categoria y la deja seleccionada. */
    fun createCategory() {
        val name = _uiState.value.newCategoryName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(error = UiText.of(R.string.category_error_name_required)) }
            return
        }
        viewModelScope.launch {
            val result = catalogRepository.saveCategory(Category(name = name))
            when (result) {
                is AppResult.Success -> {
                    val categories = catalogRepository.getCategories()
                    _uiState.update {
                        it.copy(
                            categories = categories,
                            categoryId = result.data,
                            showNewCategory = false,
                            newCategoryName = "",
                            error = null
                        )
                    }
                }

                is AppResult.Failure -> _uiState.update { it.copy(error = result.error) }
            }
        }
    }

    /** Valida y guarda el producto (y las existencias iniciales si es nuevo). */
    fun save() {
        val state = _uiState.value
        val nameCheck = Validators.text(state.name)
        if (!nameCheck.isValid) {
            _uiState.update { it.copy(error = nameCheck.errorText) }
            return
        }
        val skuCheck = Validators.sku(state.sku)
        if (!skuCheck.isValid) {
            _uiState.update { it.copy(error = skuCheck.errorText) }
            return
        }
        val barcodeCheck = Validators.barcode(state.barcode, optional = true)
        if (!barcodeCheck.isValid) {
            _uiState.update { it.copy(error = barcodeCheck.errorText) }
            return
        }
        val cost = Money.parse(state.costText) ?: 0L
        val price = Money.parse(state.priceText) ?: 0L
        if (price <= 0L) {
            _uiState.update { it.copy(error = UiText.of(R.string.product_error_price_required)) }
            return
        }
        val categoryId = state.categoryId
        if (categoryId == null) {
            _uiState.update { it.copy(error = UiText.of(R.string.product_error_invalid_category)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val product = Product(
                id = state.productId,
                name = state.name.trim(),
                sku = state.sku.trim().uppercase(),
                barcode = state.barcode.trim(),
                categoryId = categoryId,
                costCents = cost,
                priceCents = price,
                taxRate = (state.taxRatePercent.replace(',', '.').toDoubleOrNull() ?: 0.0) / 100.0,
                unit = state.unit,
                isService = state.isService,
                supplierName = state.supplierName.trim(),
                supplierPhone = state.supplierPhone.trim(),
                minStockGlobal = state.minStockGlobal,
                isActive = state.isActive,
                description = state.description.trim()
            )
            when (val result = catalogRepository.saveProduct(product)) {
                is AppResult.Success -> {
                    val savedId = result.data
                    // Existencias iniciales solo al crear (editar se hace en Inventario).
                    if (state.productId == 0L && !state.isService) {
                        state.initialStock.forEach { (storeId, quantityText) ->
                            val quantity = quantityText.toIntOrNull() ?: 0
                            if (quantity > 0) {
                                inventoryRepository.assignProductToStore(
                                    productId = savedId,
                                    storeId = storeId,
                                    initialQuantity = quantity,
                                    minStock = state.minStockGlobal
                                )
                            }
                        }
                    }
                    _uiState.update { it.copy(isSaving = false, saved = true, productId = savedId) }
                }

                is AppResult.Failure -> _uiState.update { it.copy(isSaving = false, error = result.error) }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    private fun marginOf(state: ProductEditUiState): Double {
        val cost = Money.parse(state.costText) ?: return 0.0
        val price = Money.parse(state.priceText) ?: return 0.0
        return Money.marginPercent(cost, price)
    }

    private companion object {
        const val KEY_PRODUCT_ID = "productId"
        const val KEY_STORE_ID = "storeId"
    }
}
