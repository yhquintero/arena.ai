package com.gis.supermercados.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.domain.model.CartLine
import com.gis.supermercados.domain.model.Category
import com.gis.supermercados.domain.model.CheckoutRequest
import com.gis.supermercados.domain.model.Customer
import com.gis.supermercados.domain.model.PaymentMethod
import com.gis.supermercados.domain.model.Product
import com.gis.supermercados.domain.model.Sale
import com.gis.supermercados.domain.model.Store
import com.gis.supermercados.domain.repository.AuthRepository
import com.gis.supermercados.domain.repository.CatalogRepository
import com.gis.supermercados.domain.repository.CustomerRepository
import com.gis.supermercados.domain.repository.InventoryRepository
import com.gis.supermercados.domain.repository.SalesRepository
import com.gis.supermercados.domain.repository.SettingsRepository
import com.gis.supermercados.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado del punto de venta. */
data class PosUiState(
    val stores: List<Store> = emptyList(),
    val selectedStoreId: Long? = null,
    val maxDiscountPercent: Int = MAX_DISCOUNT_FALLBACK,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val query: String = "",
    val products: List<Product> = emptyList(),
    val stockByProduct: Map<Long, Int> = emptyMap(),
    val cart: List<CartLine> = emptyList(),
    val globalDiscountPercent: Int = 0,
    val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    val paymentReference: String = "",
    val cashReceived: String = "",
    val notes: String = "",
    val customers: List<Customer> = emptyList(),
    val selectedCustomerId: Long? = null,
    val isProcessing: Boolean = false,
    val error: UiText? = null,
    val completedSale: Sale? = null,
    val ticketPrefix: String = "V",
) {
    val cartItemCount: Int get() = cart.sumOf { it.quantity }
    val selectedCustomerName: String
        get() = customers.firstOrNull { it.id == selectedCustomerId }?.fullName.orEmpty()
}

private const val MAX_DISCOUNT_FALLBACK = 20

/**
 * Punto de venta: catalogo con busqueda, carrito, cobro y ticket.
 *
 * El listado de productos y las existencias se combinan en UN solo flujo
 * reactivo: al cambiar la tienda o el filtro, la disponibilidad se actualiza sola.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PosViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val storeRepository: StoreRepository,
    private val customerRepository: CustomerRepository,
    private val inventoryRepository: InventoryRepository,
    private val salesRepository: SalesRepository,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    private data class Filters(val query: String?, val categoryId: Long?, val storeId: Long?)

    private val filters = MutableStateFlow(Filters(null, null, null))

    init {
        viewModelScope.launch {
            val session = authRepository.session.value
            val stores = storeRepository.getActiveStores()
            val settings = settingsRepository.getSettings()
            val preferredStore = stores.firstOrNull { it.id == session?.storeId }?.id ?: stores.firstOrNull()?.id
            _uiState.update {
                it.copy(
                    stores = stores,
                    selectedStoreId = preferredStore,
                    maxDiscountPercent = stores.firstOrNull { store -> store.id == preferredStore }
                        ?.maxDiscountPercent ?: MAX_DISCOUNT_FALLBACK,
                    ticketPrefix = settings.ticketPrefix
                )
            }
            filters.value = Filters(null, null, preferredStore)

            catalogRepository.observeCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        observeCatalog()
        observeCustomers()
    }

    private fun observeCatalog() {
        viewModelScope.launch {
            filters
                .debounce(SEARCH_DEBOUNCE_MS)
                .flatMapLatest { current ->
                    combine(
                        catalogRepository.observeProducts(current.query, current.categoryId),
                        current.storeId?.let { storeId ->
                            inventoryRepository.observeStock(storeId, null, false)
                        } ?: flowOf(emptyList())
                    ) { products, stock ->
                        products to stock.associate { level -> level.productId to level.available }
                    }
                }
                .collect { (products, stock) ->
                    _uiState.update { it.copy(products = products, stockByProduct = stock) }
                }
        }
    }

    private fun observeCustomers() {
        viewModelScope.launch {
            customerRepository.observeCustomers(null).collect { customers ->
                _uiState.update { state -> state.copy(customers = customers.filter { it.isActive }) }
            }
        }
    }

    fun onQueryChange(value: String) {
        val current = filters.value
        filters.value = current.copy(query = value.trim().takeIf { it.isNotEmpty() })
        _uiState.update { it.copy(query = value) }
    }

    fun onCategorySelected(categoryId: Long?) {
        val current = filters.value
        filters.value = current.copy(categoryId = categoryId)
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun onStoreSelected(storeId: Long) {
        val current = filters.value
        filters.value = current.copy(storeId = storeId)
        _uiState.update { state ->
            state.copy(
                selectedStoreId = storeId,
                maxDiscountPercent = state.stores.firstOrNull { it.id == storeId }?.maxDiscountPercent
                    ?: MAX_DISCOUNT_FALLBACK,
                cart = emptyList(),
                globalDiscountPercent = 0
            )
        }
    }

    /** Anade el producto al carrito (o suma una unidad si ya estaba). */
    fun addToCart(product: Product) {
        val state = _uiState.value
        val available = if (product.isService) Int.MAX_VALUE else state.stockByProduct[product.id] ?: 0
        val currentQuantity = state.cart.firstOrNull { it.productId == product.id }?.quantity ?: 0

        if (currentQuantity + 1 > available) {
            _uiState.update {
                it.copy(error = UiText.of(R.string.pos_error_no_stock, product.name, available))
            }
            return
        }
        _uiState.update { current ->
            val exists = current.cart.any { it.productId == product.id }
            val cart = if (exists) {
                current.cart.map { line ->
                    if (line.productId == product.id) line.copy(quantity = line.quantity + 1) else line
                }
            } else {
                current.cart + CartLine(
                    productId = product.id,
                    productName = product.name,
                    sku = product.sku,
                    unitPriceCents = product.priceCents,
                    unitCostCents = product.costCents,
                    taxRate = product.taxRate,
                    quantity = 1,
                    availableStock = available,
                    unit = product.unit,
                    isService = product.isService
                )
            }
            current.copy(cart = cart, error = null)
        }
    }

    /** Lectura de codigo de barras / SKU. */
    fun onBarcode(term: String) {
        if (term.isBlank()) return
        viewModelScope.launch {
            val product = catalogRepository.findBySkuOrBarcode(term)
            if (product == null) {
                _uiState.update { it.copy(error = UiText.of(R.string.pos_error_product_not_found)) }
            } else {
                addToCart(product)
            }
        }
    }

    fun changeQuantity(productId: Long, quantity: Int) {
        _uiState.update { state ->
            val cart = if (quantity <= 0) {
                state.cart.filterNot { it.productId == productId }
            } else {
                state.cart.map { line ->
                    if (line.productId == productId) {
                        val maxAllowed = if (line.isService) quantity else maxOf(line.availableStock, quantity)
                        line.copy(quantity = quantity.coerceAtMost(maxAllowed))
                    } else {
                        line
                    }
                }
            }
            state.copy(cart = cart)
        }
    }

    fun setLineDiscount(productId: Long, percent: Int) {
        _uiState.update { state ->
            state.copy(
                cart = state.cart.map { line ->
                    if (line.productId == productId) {
                        line.copy(discountPercent = percent.coerceIn(0, MAX_LINE_DISCOUNT))
                    } else {
                        line
                    }
                }
            )
        }
    }

    fun removeLine(productId: Long) = changeQuantity(productId, 0)

    fun onGlobalDiscountChange(percent: Int) {
        _uiState.update {
            it.copy(globalDiscountPercent = percent.coerceIn(0, it.maxDiscountPercent))
        }
    }

    fun onPaymentMethodChange(method: PaymentMethod) =
        _uiState.update { it.copy(paymentMethod = method) }

    fun onPaymentReferenceChange(value: String) = _uiState.update { it.copy(paymentReference = value) }

    fun onCashReceivedChange(value: String) = _uiState.update { it.copy(cashReceived = value) }

    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }

    fun onCustomerSelected(customerId: Long?) = _uiState.update { it.copy(selectedCustomerId = customerId) }

    fun clearCart() = _uiState.update {
        it.copy(
            cart = emptyList(),
            globalDiscountPercent = 0,
            cashReceived = "",
            notes = "",
            paymentReference = "",
            selectedCustomerId = null,
            error = null
        )
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun dismissCompletedSale() = _uiState.update { it.copy(completedSale = null) }

    /** Cobra: valida existencias, descuenta stock y guarda todo en una transaccion. */
    fun checkout() {
        val state = _uiState.value
        val storeId = state.selectedStoreId
        val session = authRepository.session.value

        if (storeId == null) {
            _uiState.update { it.copy(error = UiText.of(R.string.pos_error_select_store)) }
            return
        }
        if (state.cart.isEmpty()) {
            _uiState.update { it.copy(error = UiText.of(R.string.pos_error_empty_cart)) }
            return
        }
        if (session == null) {
            _uiState.update { it.copy(error = UiText.of(R.string.pos_error_no_session)) }
            return
        }
        if (state.paymentMethod == PaymentMethod.CREDITO_CLIENTE && state.selectedCustomerId == null) {
            _uiState.update { it.copy(error = UiText.of(R.string.pos_error_credit_needs_customer)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            val result = salesRepository.checkout(
                CheckoutRequest(
                    storeId = storeId,
                    userId = session.userId,
                    customerId = state.selectedCustomerId,
                    lines = state.cart,
                    paymentMethod = state.paymentMethod,
                    paymentReference = state.paymentReference,
                    cashReceivedCents = Money.parse(state.cashReceived) ?: 0L,
                    globalDiscountPercent = state.globalDiscountPercent,
                    notes = state.notes
                )
            )
            when (result) {
                is AppResult.Success -> {
                    AppLogger.i(TAG, "Venta cobrada: ${result.data.ticketNumber}")
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            completedSale = result.data,
                            cart = emptyList(),
                            globalDiscountPercent = 0,
                            cashReceived = "",
                            notes = "",
                            paymentReference = "",
                            selectedCustomerId = null,
                            paymentMethod = PaymentMethod.EFECTIVO
                        )
                    }
                }

                is AppResult.Failure -> _uiState.update { it.copy(isProcessing = false, error = result.error) }
            }
        }
    }

    private companion object {
        const val TAG = "PosViewModel"
        const val MAX_LINE_DISCOUNT = 50
        const val SEARCH_DEBOUNCE_MS = 160L
    }
}
