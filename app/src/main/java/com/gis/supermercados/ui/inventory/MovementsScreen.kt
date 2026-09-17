package com.gis.supermercados.ui.inventory

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.designsystem.EmptyState
import com.gis.supermercados.core.designsystem.GisColors
import com.gis.supermercados.core.designsystem.GisTopBar
import com.gis.supermercados.domain.model.InventoryMovement
import com.gis.supermercados.domain.model.MovementType
import com.gis.supermercados.ui.common.asString
import com.gis.supermercados.ui.common.movementLabel

/** Bitacora de movimientos de inventario + registro de entradas y transferencias. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementsScreen(
    onBack: () -> Unit,
    viewModel: MovementsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val movements by viewModel.movements.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            GisTopBar(
                title = stringResource(R.string.movements_title),
                subtitle = stringResource(R.string.movements_subtitle, movements.size),
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::openTransferForm) {
                        Icon(Icons.Rounded.SwapHoriz, contentDescription = stringResource(R.string.movements_transfer_action))
                    }
                    IconButton(onClick = viewModel::openMovementForm) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.movements_add_action))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.filterStoreId == null,
                        onClick = { viewModel.onFilterStore(null) },
                        label = { Text(stringResource(R.string.common_all_stores)) }
                    )
                }
                items(state.stores.size) { index ->
                    val store = state.stores[index]
                    FilterChip(
                        selected = state.filterStoreId == store.id,
                        onClick = { viewModel.onFilterStore(store.id) },
                        label = { Text(store.name) }
                    )
                }
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.filterType == null,
                        onClick = { viewModel.onFilterType(null) },
                        label = { Text(stringResource(R.string.movements_all_types)) }
                    )
                }
                items(MovementType.entries.size) { index ->
                    val type = MovementType.entries[index]
                    FilterChip(
                        selected = state.filterType == type,
                        onClick = { viewModel.onFilterType(type) },
                        label = { Text(movementLabel(type)) }
                    )
                }
            }

            if (movements.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.movements_empty_title),
                    message = stringResource(R.string.movements_empty_message),
                    icon = Icons.Rounded.SwapHoriz
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(movements.size, key = { index -> movements[index].id }) { index ->
                        MovementRow(movements[index])
                    }
                }
            }
        }
    }

    if (state.showForm) {
        MovementFormDialog(state = state, viewModel = viewModel)
    }
    if (state.showTransferForm) {
        TransferFormDialog(state = state, viewModel = viewModel)
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
private fun MovementRow(movement: InventoryMovement) {
    val color = when {
        movement.type.increasesStock -> GisColors.positive
        movement.type.isTransfer -> GisColors.accent
        else -> GisColors.negative
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(42.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(movement.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    "${AppDateTime.formatDateTime(movement.createdAt)} · ${movement.storeName}" +
                        if (movement.destinationStoreName.isNotBlank()) " → ${movement.destinationStoreName}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = GisColors.muted
                )
                if (movement.reason.isNotBlank()) {
                    Text(movement.reason, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (movement.type.increasesStock) "+" else if (movement.type.isTransfer) "↔" else "-") +
                        " ${movement.quantity}",
                    style = MaterialTheme.typography.titleSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Text(movementLabel(movement.type), style = MaterialTheme.typography.labelSmall, color = color)
                if (movement.totalCents > 0L) {
                    Text(Money.format(movement.totalCents), style = MaterialTheme.typography.labelSmall, color = GisColors.muted)
                }
            }
        }
    }
}

@Composable
private fun MovementFormDialog(state: MovementsUiState, viewModel: MovementsViewModel) {
    var productMenu by remember { mutableStateOf(false) }
    var storeMenu by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = viewModel::closeMovementForm,
        title = { Text(stringResource(R.string.movements_form_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerField(
                    label = stringResource(R.string.movements_field_product),
                    value = state.products.firstOrNull { it.id == state.formProductId }?.name.orEmpty(),
                    menuOpen = productMenu,
                    onMenuChange = { productMenu = it },
                    options = state.products.map { it.name },
                    onSelect = { index -> viewModel.onFormProduct(state.products[index].id) }
                )
                PickerField(
                    label = stringResource(R.string.movements_field_store),
                    value = state.stores.firstOrNull { it.id == state.formStoreId }?.name.orEmpty(),
                    menuOpen = storeMenu,
                    onMenuChange = { storeMenu = it },
                    options = state.stores.map { it.name },
                    onSelect = { index -> viewModel.onFormStore(state.stores[index].id) }
                )
                PickerField(
                    label = stringResource(R.string.movements_field_type),
                    value = movementLabel(state.formType),
                    menuOpen = typeMenu,
                    onMenuChange = { typeMenu = it },
                    options = MovementType.entries.filter { !it.isTransfer && it != MovementType.VENTA }
                        .map { type -> movementLabel(type) },
                    onSelect = { index ->
                        viewModel.onFormType(
                            MovementType.entries.filter { !it.isTransfer && it != MovementType.VENTA }[index]
                        )
                    }
                )
                OutlinedTextField(
                    value = state.formQuantity,
                    onValueChange = viewModel::onFormQuantity,
                    label = { Text(stringResource(R.string.movements_field_quantity)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.formUnitCost,
                    onValueChange = viewModel::onFormUnitCost,
                    label = { Text(stringResource(R.string.movements_field_unit_cost)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text(stringResource(R.string.movements_field_unit_cost_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.formReason,
                    onValueChange = viewModel::onFormReason,
                    label = { Text(stringResource(R.string.movements_field_reason)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                OutlinedTextField(
                    value = state.formReference,
                    onValueChange = viewModel::onFormReference,
                    label = { Text(stringResource(R.string.movements_field_reference)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = viewModel::submitMovement, enabled = !state.isProcessing) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = { TextButton(onClick = viewModel::closeMovementForm) { Text(stringResource(R.string.common_cancel)) } }
    )
}

@Composable
private fun TransferFormDialog(state: MovementsUiState, viewModel: MovementsViewModel) {
    var productMenu by remember { mutableStateOf(false) }
    var fromMenu by remember { mutableStateOf(false) }
    var toMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = viewModel::closeTransferForm,
        title = { Text(stringResource(R.string.transfer_form_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PickerField(
                    label = stringResource(R.string.movements_field_product),
                    value = state.products.firstOrNull { it.id == state.transferProductId }?.name.orEmpty(),
                    menuOpen = productMenu,
                    onMenuChange = { productMenu = it },
                    options = state.products.filter { !it.isService }.map { it.name },
                    onSelect = { index ->
                        viewModel.onTransferProduct(state.products.filter { product -> !product.isService }[index].id)
                    }
                )
                PickerField(
                    label = stringResource(R.string.transfer_field_from),
                    value = state.stores.firstOrNull { it.id == state.transferFromStoreId }?.name.orEmpty(),
                    menuOpen = fromMenu,
                    onMenuChange = { fromMenu = it },
                    options = state.stores.map { it.name },
                    onSelect = { index -> viewModel.onTransferFrom(state.stores[index].id) }
                )
                PickerField(
                    label = stringResource(R.string.transfer_field_to),
                    value = state.stores.firstOrNull { it.id == state.transferToStoreId }?.name.orEmpty(),
                    menuOpen = toMenu,
                    onMenuChange = { toMenu = it },
                    options = state.stores.map { it.name },
                    onSelect = { index -> viewModel.onTransferTo(state.stores[index].id) }
                )
                OutlinedTextField(
                    value = state.transferQuantity,
                    onValueChange = viewModel::onTransferQuantity,
                    label = { Text(stringResource(R.string.movements_field_quantity)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.transferReference,
                    onValueChange = viewModel::onTransferReference,
                    label = { Text(stringResource(R.string.movements_field_reference)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    stringResource(R.string.transfer_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = GisColors.muted
                )
            }
        },
        confirmButton = {
            Button(onClick = viewModel::submitTransfer, enabled = !state.isProcessing) {
                Text(stringResource(R.string.transfer_confirm))
            }
        },
        dismissButton = { TextButton(onClick = viewModel::closeTransferForm) { Text(stringResource(R.string.common_cancel)) } }
    )
}

/** Campo desplegable sencillo (evita dependencias extra de formulario). */
@Composable
private fun PickerField(
    label: String,
    value: String,
    menuOpen: Boolean,
    onMenuChange: (Boolean) -> Unit,
    options: List<String>,
    onSelect: (Int) -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
        FilledTonalButton(onClick = { onMenuChange(true) }, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    value.ifBlank { label },
                    Modifier.weight(1f),
                    maxLines = 1,
                    color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenuChange(false) }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(index)
                        onMenuChange(false)
                    }
                )
            }
        }
    }
}
