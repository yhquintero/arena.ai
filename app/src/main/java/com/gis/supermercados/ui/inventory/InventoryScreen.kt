package com.gis.supermercados.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
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
import com.gis.supermercados.core.designsystem.StatCard
import com.gis.supermercados.core.designsystem.StatusChip
import com.gis.supermercados.domain.model.StockLevel
import com.gis.supermercados.ui.common.asString

/** Inventario: existencias por sucursal, valoracion y ajuste rapido. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onNavigateToMovements: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val stocks by viewModel.stocks.collectAsStateWithLifecycle()
    var storeMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            GisTopBar(
                title = stringResource(R.string.inventory_title),
                subtitle = stringResource(R.string.inventory_subtitle, stocks.size),
                onBack = onBack,
                actions = {
                    Box {
                        FilterChip(
                            selected = state.selectedStoreId != null,
                            onClick = { storeMenuOpen = true },
                            label = {
                                Text(
                                    state.stores.firstOrNull { it.id == state.selectedStoreId }?.name
                                        ?: stringResource(R.string.common_all_stores)
                                )
                            },
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        DropdownMenu(expanded = storeMenuOpen, onDismissRequest = { storeMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.common_all_stores)) },
                                onClick = {
                                    viewModel.onStoreSelected(null)
                                    storeMenuOpen = false
                                }
                            )
                            state.stores.forEach { store ->
                                DropdownMenuItem(
                                    text = { Text(store.name) },
                                    onClick = {
                                        viewModel.onStoreSelected(store.id)
                                        storeMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToMovements,
                icon = { Icon(Icons.Rounded.SwapHoriz, null) },
                text = { Text(stringResource(R.string.inventory_movements_button)) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        label = stringResource(R.string.inventory_value_cost),
                        value = Money.formatCompact(state.value.costValueCents),
                        caption = stringResource(R.string.inventory_value_retail, Money.formatCompact(state.value.retailValueCents)),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = stringResource(R.string.inventory_alerts),
                        value = "${state.lowStockCount + state.outOfStockCount}",
                        caption = stringResource(
                            R.string.inventory_alerts_detail,
                            state.lowStockCount,
                            state.outOfStockCount
                        ),
                        icon = Icons.Rounded.WarningAmber,
                        accent = if (state.outOfStockCount > 0) GisColors.negative else GisColors.warning,
                        modifier = Modifier.weight(1f)
                    )
                }
                SearchField(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    placeholder = stringResource(R.string.inventory_search_placeholder)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.onlyLowStock,
                            onClick = { viewModel.onOnlyLowStockChange(!state.onlyLowStock) },
                            label = { Text(stringResource(R.string.inventory_only_low_stock)) }
                        )
                    }
                }
            }

            if (stocks.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.inventory_empty_title),
                    message = stringResource(R.string.inventory_empty_message),
                    icon = Icons.Rounded.WarningAmber
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stocks.size, key = { index -> stocks[index].id }) { index ->
                        StockRow(stock = stocks[index], onClick = { viewModel.openAdjust(stocks[index]) })
                    }
                }
            }
        }
    }

    state.adjustTarget?.let { target ->
        AdjustDialog(
            stock = target,
            quantity = state.adjustQuantity,
            min = state.adjustMin,
            max = state.adjustMax,
            isProcessing = state.isProcessing,
            onQuantityChange = viewModel::onAdjustQuantityChange,
            onMinChange = viewModel::onAdjustMinChange,
            onMaxChange = viewModel::onAdjustMaxChange,
            onConfirm = viewModel::saveAdjust,
            onDismiss = viewModel::closeAdjust
        )
    }

    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text(stringResource(R.string.common_error_title)) },
            text = { Text(error.asString()) },
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text(stringResource(R.string.common_close)) } }
        )
    }

    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            title = { Text(stringResource(R.string.common_done_title)) },
            text = { Text(message.asString()) },
            confirmButton = { TextButton(onClick = viewModel::dismissMessage) { Text(stringResource(R.string.common_close)) } }
        )
    }
}

@Composable
private fun StockRow(stock: StockLevel, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stock.productName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    "${stock.sku} · ${stock.storeName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GisColors.muted
                )
                Text(
                    stringResource(R.string.inventory_row_thresholds, stock.minStock, stock.maxStock),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${stock.available}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        stock.quantity <= 0 -> GisColors.negative
                        stock.isLowStock -> GisColors.warning
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                AmountText(stock.stockValueCents, colored = false, style = MaterialTheme.typography.labelSmall)
                if (stock.quantity <= 0) {
                    StatusChip(stringResource(R.string.stock_out_of_stock), GisColors.negative)
                } else if (stock.isLowStock) {
                    StatusChip(stringResource(R.string.stock_low), GisColors.warning)
                }
            }
        }
    }
}

@Composable
private fun AdjustDialog(
    stock: StockLevel,
    quantity: String,
    min: String,
    max: String,
    isProcessing: Boolean,
    onQuantityChange: (String) -> Unit,
    onMinChange: (String) -> Unit,
    onMaxChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.inventory_adjust_title, stock.productName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.inventory_adjust_current, stock.quantity, stock.storeName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = onQuantityChange,
                    label = { Text(stringResource(R.string.inventory_adjust_quantity)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = min,
                        onValueChange = onMinChange,
                        label = { Text(stringResource(R.string.inventory_min_stock)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = max,
                        onValueChange = onMaxChange,
                        label = { Text(stringResource(R.string.inventory_max_stock)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    stringResource(R.string.inventory_adjust_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = GisColors.muted
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isProcessing) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )
}
