package com.gis.supermercados.ui.stores

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gis.supermercados.R
import com.gis.supermercados.core.designsystem.EmptyState
import com.gis.supermercados.core.designsystem.GisColors
import com.gis.supermercados.core.designsystem.GisTopBar
import com.gis.supermercados.core.designsystem.SectionCard
import com.gis.supermercados.core.designsystem.StatusChip
import com.gis.supermercados.domain.model.Store
import com.gis.supermercados.ui.common.asString

/** Sucursales: datos de contacto, horario y permisos operativos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoresScreen(
    onBack: (() -> Unit)? = null,
    viewModel: StoresViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val stores by viewModel.stores.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            GisTopBar(
                title = stringResource(R.string.stores_title),
                subtitle = stringResource(R.string.stores_subtitle, stores.count { it.isActive }),
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openCreate,
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(stringResource(R.string.stores_new)) }
            )
        }
    ) { padding ->
        if (stores.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.stores_empty_title),
                message = stringResource(R.string.stores_empty_message),
                icon = Icons.Rounded.Store,
                actionLabel = stringResource(R.string.stores_new),
                onAction = viewModel::openCreate,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(stores.size, key = { index -> stores[index].id }) { index ->
                    StoreRow(
                        store = stores[index],
                        onClick = { viewModel.openEdit(stores[index]) },
                        onToggleActive = { viewModel.toggleActive(stores[index]) },
                        onDelete = { viewModel.requestDelete(stores[index]) }
                    )
                }
            }
        }
    }

    if (state.showForm) {
        ModalBottomSheet(onDismissRequest = viewModel::closeForm, sheetState = sheetState) {
            StoreFormSheet(state = state, viewModel = viewModel)
        }
    }

    state.pendingDelete?.let { store ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.stores_delete_title)) },
            text = { Text(stringResource(R.string.stores_delete_message, store.name)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text(stringResource(R.string.common_delete), color = GisColors.negative)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text(stringResource(R.string.common_cancel)) } }
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
private fun StoreRow(store: Store, onClick: () -> Unit, onToggleActive: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(store.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(Modifier.width(8.dp))
                    StatusChip(
                        text = store.code,
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    listOf(store.address, store.city).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    stringResource(
                        R.string.stores_row_schedule,
                        store.schedule.openingTime,
                        store.schedule.closingTime,
                        store.schedule.openDays.size
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = GisColors.muted
                )
                if (store.managerName.isNotBlank()) {
                    Text(
                        stringResource(R.string.stores_row_manager, store.managerName),
                        style = MaterialTheme.typography.labelSmall,
                        color = GisColors.muted
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(checked = store.isActive, onCheckedChange = { onToggleActive() })
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.common_delete), tint = GisColors.negative)
                }
            }
        }
    }
}

@Composable
private fun StoreFormSheet(state: StoresUiState, viewModel: StoresViewModel) {
    val form = state.form
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(if (form.id == 0L) R.string.stores_new else R.string.stores_edit_title),
            style = MaterialTheme.typography.titleLarge
        )

        SectionCard(title = stringResource(R.string.stores_section_data)) {
            OutlinedTextField(
                value = form.name,
                onValueChange = { value -> viewModel.updateForm { it.copy(name = value) } },
                label = { Text(stringResource(R.string.store_field_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = form.code,
                    onValueChange = { value -> viewModel.updateForm { it.copy(code = value.uppercase()) } },
                    label = { Text(stringResource(R.string.store_field_code)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = form.taxId,
                    onValueChange = { value -> viewModel.updateForm { it.copy(taxId = value) } },
                    label = { Text(stringResource(R.string.store_field_tax_id)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = form.address,
                onValueChange = { value -> viewModel.updateForm { it.copy(address = value) } },
                label = { Text(stringResource(R.string.store_field_address)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = form.city,
                    onValueChange = { value -> viewModel.updateForm { it.copy(city = value) } },
                    label = { Text(stringResource(R.string.store_field_city)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = form.phone,
                    onValueChange = { value -> viewModel.updateForm { it.copy(phone = value) } },
                    label = { Text(stringResource(R.string.store_field_phone)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = form.email,
                onValueChange = { value -> viewModel.updateForm { it.copy(email = value) } },
                label = { Text(stringResource(R.string.store_field_email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = form.managerName,
                onValueChange = { value -> viewModel.updateForm { it.copy(managerName = value) } },
                label = { Text(stringResource(R.string.store_field_manager)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionCard(title = stringResource(R.string.stores_section_schedule)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(DAY_LABELS.size) { index ->
                    val day = index + 1
                    FilterChip(
                        selected = form.openDays.contains(day),
                        onClick = {
                            viewModel.updateForm { current ->
                                val days = current.openDays.toMutableSet()
                                if (!days.add(day)) days.remove(day)
                                current.copy(openDays = days)
                            }
                        },
                        label = { Text(stringResource(DAY_LABELS[index])) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = form.openingTime,
                    onValueChange = { value -> viewModel.updateForm { it.copy(openingTime = value) } },
                    label = { Text(stringResource(R.string.store_field_opening)) },
                    singleLine = true,
                    supportingText = { Text(stringResource(R.string.store_field_time_hint)) },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = form.closingTime,
                    onValueChange = { value -> viewModel.updateForm { it.copy(closingTime = value) } },
                    label = { Text(stringResource(R.string.store_field_closing)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            SwitchRow(
                title = stringResource(R.string.store_field_extended_hours),
                subtitle = stringResource(R.string.store_field_extended_hours_hint),
                checked = form.hasExtendedHours,
                onCheckedChange = { value -> viewModel.updateForm { it.copy(hasExtendedHours = value) } }
            )
        }

        SectionCard(title = stringResource(R.string.stores_section_permissions)) {
            SwitchRow(
                title = stringResource(R.string.store_permission_sales),
                checked = form.allowSales,
                onCheckedChange = { value -> viewModel.updateForm { it.copy(allowSales = value) } }
            )
            SwitchRow(
                title = stringResource(R.string.store_permission_returns),
                checked = form.allowReturns,
                onCheckedChange = { value -> viewModel.updateForm { it.copy(allowReturns = value) } }
            )
            SwitchRow(
                title = stringResource(R.string.store_permission_inventory),
                checked = form.allowInventoryAdjust,
                onCheckedChange = { value -> viewModel.updateForm { it.copy(allowInventoryAdjust = value) } }
            )
            SwitchRow(
                title = stringResource(R.string.store_permission_transfers),
                checked = form.allowTransfers,
                onCheckedChange = { value -> viewModel.updateForm { it.copy(allowTransfers = value) } }
            )
            SwitchRow(
                title = stringResource(R.string.store_permission_expenses),
                checked = form.allowExpenses,
                onCheckedChange = { value -> viewModel.updateForm { it.copy(allowExpenses = value) } }
            )
            SwitchRow(
                title = stringResource(R.string.store_permission_reports),
                checked = form.allowReports,
                onCheckedChange = { value -> viewModel.updateForm { it.copy(allowReports = value) } }
            )
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            OutlinedTextField(
                value = form.maxDiscountPercent.toString(),
                onValueChange = { value ->
                    viewModel.updateForm { it.copy(maxDiscountPercent = value.filter(Char::isDigit).toIntOrNull() ?: 0) }
                },
                label = { Text(stringResource(R.string.store_field_max_discount)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text(stringResource(R.string.store_field_max_discount_hint)) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionCard(title = stringResource(R.string.stores_section_notes)) {
            OutlinedTextField(
                value = form.notes,
                onValueChange = { value -> viewModel.updateForm { it.copy(notes = value) } },
                label = { Text(stringResource(R.string.store_field_notes)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            if (form.id != 0L) {
                SwitchRow(
                    title = stringResource(R.string.product_field_active),
                    subtitle = stringResource(R.string.store_field_active_hint),
                    checked = form.isActive,
                    onCheckedChange = { value -> viewModel.updateForm { it.copy(isActive = value) } }
                )
            }
        }

        Button(
            onClick = viewModel::save,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(stringResource(R.string.common_save), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Etiquetas de los dias (ISO 1 = lunes .. 7 = domingo). */
private val DAY_LABELS = listOf(
    R.string.day_monday,
    R.string.day_tuesday,
    R.string.day_wednesday,
    R.string.day_thursday,
    R.string.day_friday,
    R.string.day_saturday,
    R.string.day_sunday
)
