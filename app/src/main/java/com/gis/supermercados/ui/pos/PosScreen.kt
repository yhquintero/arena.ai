package com.gis.supermercados.ui.pos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gis.supermercados.R
import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.designsystem.AmountText
import com.gis.supermercados.core.designsystem.EmptyState
import com.gis.supermercados.core.designsystem.GisColors
import com.gis.supermercados.core.designsystem.GisTopBar
import com.gis.supermercados.core.designsystem.SearchField
import com.gis.supermercados.core.designsystem.StatusChip
import com.gis.supermercados.domain.calculator.SaleCalculator
import com.gis.supermercados.domain.model.CartLine
import com.gis.supermercados.domain.model.PaymentMethod
import com.gis.supermercados.domain.model.Product
import com.gis.supermercados.ui.common.asString
import com.gis.supermercados.ui.common.paymentLabel
import kotlinx.coroutines.launch

/** Punto de venta (telefono: carrito en hoja inferior; tablet: panel lateral). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    onNavigateToSales: () -> Unit,
    viewModel: PosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isExpanded = configuration.screenWidthDp >= TABLET_WIDTH_DP
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var sheetOpen by remember { mutableStateOf(false) }
    var storeMenuOpen by remember { mutableStateOf(false) }
    var barcode by remember { mutableStateOf("") }

    val calculation = remember(state.cart, state.globalDiscountPercent) {
        SaleCalculator.calculate(state.cart, globalDiscountPercent = state.globalDiscountPercent.toDouble())
    }
    val selectedStoreName = state.stores.firstOrNull { it.id == state.selectedStoreId }?.name.orEmpty()

    Scaffold(
        topBar = {
            GisTopBar(
                title = stringResource(R.string.pos_title),
                subtitle = selectedStoreName,
                actions = {
                    Box {
                        IconButton(onClick = { storeMenuOpen = true }) {
                            Icon(Icons.Rounded.Store, contentDescription = stringResource(R.string.pos_select_store))
                        }
                        DropdownMenu(expanded = storeMenuOpen, onDismissRequest = { storeMenuOpen = false }) {
                            state.stores.forEach { store ->
                                DropdownMenuItem(
                                    text = { Text(store.name) },
                                    onClick = {
                                        viewModel.onStoreSelected(store.id)
                                        storeMenuOpen = false
                                    },
                                    trailingIcon = {
                                        if (store.id == state.selectedStoreId) {
                                            Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.weight(if (isExpanded) 1.5f else 1f).fillMaxHeight()) {
                CatalogPanel(
                    state = state,
                    barcode = barcode,
                    onBarcodeChange = { barcode = it },
                    onBarcodeSubmit = {
                        viewModel.onBarcode(barcode)
                        barcode = ""
                    },
                    onQueryChange = viewModel::onQueryChange,
                    onCategorySelected = viewModel::onCategorySelected,
                    onProductClick = viewModel::addToCart
                )
                if (!isExpanded) {
                    CartSummaryBar(
                        itemCount = state.cartItemCount,
                        total = calculation.totalCents,
                        onClick = { sheetOpen = true }
                    )
                }
            }
            if (isExpanded) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    CartPanel(state = state, calculation = calculation, viewModel = viewModel, onSeeTicket = onNavigateToSales)
                }
            }
        }
    }

    if (sheetOpen && !isExpanded) {
        ModalBottomSheet(onDismissRequest = { sheetOpen = false }, sheetState = sheetState) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                CartPanel(
                    state = state,
                    calculation = calculation,
                    viewModel = viewModel,
                    onSeeTicket = {
                        sheetOpen = false
                        scope.launch { sheetState.hide() }
                        onNavigateToSales()
                    }
                )
            }
        }
    }

    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text(stringResource(R.string.pos_error_title)) },
            text = { Text(error.asString()) },
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text(stringResource(R.string.common_close)) } }
        )
    }

    state.completedSale?.let { sale ->
        TicketDialog(
            ticketNumber = sale.ticketNumber,
            total = sale.totalCents,
            change = sale.changeCents,
            itemCount = sale.itemCount,
            onDismiss = viewModel::dismissCompletedSale,
            onSeeHistory = {
                viewModel.dismissCompletedSale()
                onNavigateToSales()
            }
        )
    }
}

/** Buscador, lector de codigo y rejilla de productos. */
@Composable
private fun CatalogPanel(
    state: PosUiState,
    barcode: String,
    onBarcodeChange: (String) -> Unit,
    onBarcodeSubmit: () -> Unit,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onProductClick: (Product) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchField(
                query = state.query,
                onQueryChange = onQueryChange,
                placeholder = stringResource(R.string.pos_search_placeholder),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = barcode,
                onValueChange = onBarcodeChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(stringResource(R.string.pos_barcode)) },
                leadingIcon = { Icon(Icons.Rounded.QrCodeScanner, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    IconButton(onClick = onBarcodeSubmit) {
                        Icon(Icons.Rounded.AddCircle, contentDescription = stringResource(R.string.pos_barcode_add))
                    }
                }
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = state.selectedCategoryId == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text(stringResource(R.string.pos_all_categories)) }
                )
            }
            items(state.categories.size) { index ->
                val category = state.categories[index]
                FilterChip(
                    selected = state.selectedCategoryId == category.id,
                    onClick = { onCategorySelected(category.id) },
                    label = { Text(category.name) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (state.products.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.pos_no_products_title),
                message = stringResource(R.string.pos_no_products_message)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.products, key = { it.id }) { product ->
                    ProductTile(
                        product = product,
                        stock = if (product.isService) null else state.stockByProduct[product.id] ?: 0,
                        onClick = { onProductClick(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductTile(product: Product, stock: Int?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(product.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(
                Money.format(product.priceCents),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            when {
                stock == null -> StatusChip(
                    text = stringResource(R.string.pos_service),
                    containerColor = MaterialTheme.colorScheme.secondary
                )

                stock <= 0 -> StatusChip(
                    text = stringResource(R.string.stock_out_of_stock),
                    containerColor = GisColors.negative
                )

                else -> Text(
                    stringResource(R.string.pos_stock_available, stock),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (stock <= product.minStockGlobal) GisColors.warning else GisColors.muted
                )
            }
        }
    }
}

/** Barra inferior con el total y el acceso al carrito (telefono). */
@Composable
private fun CartSummaryBar(itemCount: Int, total: Long, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(12.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.ShoppingCart, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.pos_cart_summary, itemCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    Money.format(total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                stringResource(R.string.pos_open_cart),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/** Carrito + pago. Se usa igual en panel lateral y en hoja inferior. */
@Composable
private fun CartPanel(
    state: PosUiState,
    calculation: com.gis.supermercados.domain.calculator.SaleCalculation,
    viewModel: PosViewModel,
    onSeeTicket: () -> Unit,
) {
    var customerMenuOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.pos_cart_title), style = MaterialTheme.typography.titleMedium)

        if (state.cart.isEmpty()) {
            Text(
                stringResource(R.string.pos_cart_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            state.cart.forEach { line ->
                CartLineRow(
                    line = line,
                    onQuantityChange = { viewModel.changeQuantity(line.productId, it) },
                    onRemove = { viewModel.removeLine(line.productId) }
                )
            }
            HorizontalDivider()
            TotalsBlock(calculation = calculation, state = state, viewModel = viewModel)
            HorizontalDivider()
            PaymentBlock(
                state = state,
                totalCents = calculation.totalCents,
                viewModel = viewModel,
                customerMenuOpen = customerMenuOpen,
                onCustomerMenu = { customerMenuOpen = it }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = viewModel::clearCart, enabled = state.cart.isNotEmpty(), modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.pos_clear_cart))
            }
            Button(
                onClick = viewModel::checkout,
                enabled = state.cart.isNotEmpty() && !state.isProcessing,
                modifier = Modifier.weight(2f).height(52.dp)
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    stringResource(R.string.pos_checkout, Money.format(calculation.totalCents)),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        TextButton(onClick = onSeeTicket, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.pos_see_history))
        }
    }
}

@Composable
private fun CartLineRow(line: CartLine, onQuantityChange: (Int) -> Unit, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(line.productName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                "${Money.format(line.unitPriceCents)} x ${line.quantity}",
                style = MaterialTheme.typography.labelSmall,
                color = GisColors.muted
            )
        }
        IconButton(onClick = { onQuantityChange(line.quantity - 1) }) {
            Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.pos_decrease))
        }
        Text("${line.quantity}", style = MaterialTheme.typography.titleSmall)
        IconButton(onClick = { onQuantityChange(line.quantity + 1) }) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.pos_increase))
        }
        AmountText(line.totalCents, colored = false, style = MaterialTheme.typography.labelLarge)
        IconButton(onClick = onRemove) {
            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.common_delete), tint = GisColors.negative)
        }
    }
}

@Composable
private fun TotalsBlock(
    calculation: com.gis.supermercados.domain.calculator.SaleCalculation,
    state: PosUiState,
    viewModel: PosViewModel,
) {
    SummaryRow(stringResource(R.string.pos_subtotal), Money.format(calculation.subtotalCents))
    if (calculation.lineDiscountCents > 0L) {
        SummaryRow(stringResource(R.string.pos_line_discounts), "-" + Money.format(calculation.lineDiscountCents))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.pos_global_discount), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = state.globalDiscountPercent.toString(),
            onValueChange = { value -> viewModel.onGlobalDiscountChange(value.filter { it.isDigit() }.toIntOrNull() ?: 0) },
            modifier = Modifier.width(86.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = { Text("%") }
        )
    }
    SummaryRow(stringResource(R.string.pos_tax), Money.format(calculation.taxCents))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.pos_total),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            Money.format(calculation.totalCents),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

@Composable
private fun PaymentBlock(
    state: PosUiState,
    totalCents: Long,
    viewModel: PosViewModel,
    customerMenuOpen: Boolean,
    onCustomerMenu: (Boolean) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(PaymentMethod.entries.size) { index ->
            val method = PaymentMethod.entries[index]
            FilterChip(
                selected = state.paymentMethod == method,
                onClick = { viewModel.onPaymentMethodChange(method) },
                label = { Text(paymentLabel(method)) }
            )
        }
    }

    if (state.paymentMethod == PaymentMethod.EFECTIVO) {
        OutlinedTextField(
            value = state.cashReceived,
            onValueChange = { value -> viewModel.onCashReceivedChange(value.filter { it.isDigit() || it == ',' || it == '.' }) },
            label = { Text(stringResource(R.string.pos_cash_received)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        val received = Money.parse(state.cashReceived) ?: 0L
        if (received > 0L) {
            val change = SaleCalculator.change(totalCents, received)
            SummaryRow(
                stringResource(R.string.pos_change),
                Money.format(change),
                valueColor = if (change >= 0L) GisColors.positive else GisColors.negative
            )
        }
    } else {
        OutlinedTextField(
            value = state.paymentReference,
            onValueChange = viewModel::onPaymentReferenceChange,
            label = { Text(stringResource(R.string.pos_payment_reference)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Box {
        FilledTonalButton(onClick = { onCustomerMenu(true) }, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (state.selectedCustomerId == null) stringResource(R.string.pos_select_customer)
                else state.selectedCustomerName
            )
        }
        DropdownMenu(expanded = customerMenuOpen, onDismissRequest = { onCustomerMenu(false) }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pos_no_customer)) },
                onClick = {
                    viewModel.onCustomerSelected(null)
                    onCustomerMenu(false)
                }
            )
            state.customers.forEach { customer ->
                DropdownMenuItem(
                    text = { Text(customer.fullName) },
                    onClick = {
                        viewModel.onCustomerSelected(customer.id)
                        onCustomerMenu(false)
                    }
                )
            }
        }
    }

    OutlinedTextField(
        value = state.notes,
        onValueChange = viewModel::onNotesChange,
        label = { Text(stringResource(R.string.pos_notes)) },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 2
    )
}

/** Ticket resumido tras cobrar. */
@Composable
private fun TicketDialog(
    ticketNumber: String,
    total: Long,
    change: Long,
    itemCount: Int,
    onDismiss: () -> Unit,
    onSeeHistory: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.ShoppingCart, null, tint = GisColors.positive) },
        title = { Text(stringResource(R.string.pos_sale_completed_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.pos_sale_ticket, ticketNumber), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.pos_sale_items, itemCount))
                Text(stringResource(R.string.pos_sale_total, Money.format(total)))
                if (change > 0L) Text(stringResource(R.string.pos_sale_change, Money.format(change)))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.pos_new_sale)) } },
        dismissButton = { TextButton(onClick = onSeeHistory) { Text(stringResource(R.string.pos_see_history)) } }
    )
}

private const val TABLET_WIDTH_DP = 720
