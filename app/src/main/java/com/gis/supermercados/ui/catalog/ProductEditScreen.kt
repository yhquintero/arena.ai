package com.gis.supermercados.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gis.supermercados.R
import com.gis.supermercados.core.common.formatPercent
import com.gis.supermercados.core.designsystem.AmountField
import com.gis.supermercados.core.designsystem.GisColors
import com.gis.supermercados.core.designsystem.GisTopBar
import com.gis.supermercados.core.designsystem.LoadingState
import com.gis.supermercados.core.designsystem.SectionCard
import com.gis.supermercados.domain.model.ProductUnit
import com.gis.supermercados.ui.common.asString
import com.gis.supermercados.ui.common.unitLabel

/** Formulario de alta/edicion de producto o servicio. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreen(
    onBack: () -> Unit,
    viewModel: ProductEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var unitMenuOpen by remember { mutableStateOf(false) }
    var categoryMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            GisTopBar(
                title = stringResource(
                    if (state.productId == 0L) R.string.product_edit_new_title else R.string.product_edit_title
                ),
                subtitle = state.categories.firstOrNull { it.id == state.categoryId }?.name,
                onBack = onBack,
                actions = {
                    androidx.compose.material3.IconButton(
                        onClick = viewModel::save,
                        enabled = !state.isSaving && !state.isLoading
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = stringResource(R.string.common_save))
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SectionCard(title = stringResource(R.string.product_section_general)) {
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = viewModel::onNameChange,
                            label = { Text(stringResource(R.string.product_field_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = state.sku,
                                onValueChange = viewModel::onSkuChange,
                                label = { Text(stringResource(R.string.product_field_sku)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = state.barcode,
                                onValueChange = viewModel::onBarcodeChange,
                                label = { Text(stringResource(R.string.product_field_barcode)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = state.description,
                            onValueChange = viewModel::onDescriptionChange,
                            label = { Text(stringResource(R.string.product_field_description)) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                        Spacer(Modifier.height(10.dp))

                        Box(Modifier.fillMaxWidth()) {
                            FilledTonalButton(
                                onClick = { categoryMenuOpen = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    state.categories.firstOrNull { it.id == state.categoryId }?.name
                                        ?: stringResource(R.string.product_field_category),
                                    Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                )
                                Icon(Icons.Rounded.ArrowDropDown, null)
                            }
                            DropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                                state.categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.name) },
                                        onClick = {
                                            viewModel.onCategorySelected(category.id)
                                            categoryMenuOpen = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.product_new_category)) },
                                    onClick = {
                                        viewModel.showNewCategory(true)
                                        categoryMenuOpen = false
                                    }
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.product_field_is_service), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    stringResource(R.string.product_field_is_service_hint),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = state.isService, onCheckedChange = viewModel::onServiceChange)
                        }

                        if (!state.isService) {
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.fillMaxWidth()) {
                                FilledTonalButton(onClick = { unitMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(unitLabel(state.unit), Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                                    Icon(Icons.Rounded.ArrowDropDown, null)
                                }
                                DropdownMenu(expanded = unitMenuOpen, onDismissRequest = { unitMenuOpen = false }) {
                                    ProductUnit.entries.forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unitLabel(unit)) },
                                            onClick = {
                                                viewModel.onUnitChange(unit)
                                                unitMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SectionCard(title = stringResource(R.string.product_section_pricing)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AmountField(
                                label = stringResource(R.string.product_field_cost),
                                value = state.costText,
                                onValueChange = viewModel::onCostChange,
                                modifier = Modifier.weight(1f)
                            )
                            AmountField(
                                label = stringResource(R.string.product_field_price),
                                value = state.priceText,
                                onValueChange = viewModel::onPriceChange,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AmountField(
                                label = stringResource(R.string.product_field_tax_percent),
                                value = state.taxRatePercent,
                                onValueChange = viewModel::onTaxRateChange,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    stringResource(R.string.product_margin_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    formatPercent(state.marginPercent),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.marginPercent > 0) GisColors.positive else GisColors.negative
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.product_pricing_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = GisColors.muted
                        )
                    }
                }

                if (!state.isService) {
                    item {
                        SectionCard(title = stringResource(R.string.product_section_stock)) {
                            OutlinedTextField(
                                value = state.minStockGlobal.toString(),
                                onValueChange = viewModel::onMinStockChange,
                                label = { Text(stringResource(R.string.inventory_min_stock)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (state.productId == 0L && state.storeNames.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    stringResource(R.string.product_initial_stock_title),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    stringResource(R.string.product_initial_stock_hint),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                state.storeNames.forEach { (storeId, storeName) ->
                                    OutlinedTextField(
                                        value = state.initialStock[storeId].orEmpty(),
                                        onValueChange = { viewModel.onInitialStockChange(storeId, it) },
                                        label = { Text(storeName) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SectionCard(title = stringResource(R.string.product_section_supplier)) {
                        OutlinedTextField(
                            value = state.supplierName,
                            onValueChange = viewModel::onSupplierNameChange,
                            label = { Text(stringResource(R.string.product_field_supplier)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = state.supplierPhone,
                            onValueChange = viewModel::onSupplierPhoneChange,
                            label = { Text(stringResource(R.string.product_field_supplier_phone)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.product_field_active), style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    stringResource(R.string.product_field_active_hint),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = state.isActive, onCheckedChange = viewModel::onActiveChange)
                        }
                    }
                }

                item {
                    Button(
                        onClick = viewModel::save,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(stringResource(R.string.common_save), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    if (state.showNewCategory) {
        AlertDialog(
            onDismissRequest = { viewModel.showNewCategory(false) },
            title = { Text(stringResource(R.string.product_new_category)) },
            text = {
                OutlinedTextField(
                    value = state.newCategoryName,
                    onValueChange = viewModel::onNewCategoryNameChange,
                    label = { Text(stringResource(R.string.category_field_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = { TextButton(onClick = viewModel::createCategory) { Text(stringResource(R.string.common_save)) } },
            dismissButton = {
                TextButton(onClick = { viewModel.showNewCategory(false) }) { Text(stringResource(R.string.common_cancel)) }
            }
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
}
