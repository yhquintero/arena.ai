package com.gis.supermercados.ui.customers

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.designsystem.AmountField
import com.gis.supermercados.core.designsystem.EmptyState
import com.gis.supermercados.core.designsystem.GisColors
import com.gis.supermercados.core.designsystem.GisTopBar
import com.gis.supermercados.core.designsystem.SearchField
import com.gis.supermercados.core.designsystem.SectionCard
import com.gis.supermercados.core.designsystem.StatCard
import com.gis.supermercados.core.designsystem.StatusChip
import com.gis.supermercados.domain.model.DocumentType
import com.gis.supermercados.ui.common.asString
import com.gis.supermercados.ui.common.documentTypeLabel

/** Clientes: ficha, credito disponible, puntos de fidelidad e historial. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    onBack: (() -> Unit)? = null,
    viewModel: CustomersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            GisTopBar(
                title = stringResource(R.string.customers_title),
                subtitle = stringResource(R.string.customers_subtitle, customers.size),
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openCreate,
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(stringResource(R.string.customers_new)) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        label = stringResource(R.string.customers_active),
                        value = customers.count { it.isActive }.toString(),
                        caption = stringResource(R.string.customers_with_credit, customers.count { it.creditLimitCents > 0L }),
                        icon = Icons.Rounded.Groups,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = stringResource(R.string.customers_receivable),
                        value = Money.formatCompact(customers.sumOf { it.balanceCents }),
                        caption = stringResource(R.string.customers_credit_limit, Money.formatCompact(customers.sumOf { it.creditLimitCents })),
                        modifier = Modifier.weight(1f)
                    )
                }
                SearchField(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    placeholder = stringResource(R.string.customers_search_placeholder)
                )
            }

            if (customers.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.customers_empty_title),
                    message = stringResource(R.string.customers_empty_message),
                    icon = Icons.Rounded.Groups,
                    actionLabel = stringResource(R.string.customers_new),
                    onAction = viewModel::openCreate
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(customers.size, key = { index -> customers[index].id }) { index ->
                        CustomerRow(
                            customer = customers[index],
                            onClick = { viewModel.openDetail(customers[index]) },
                            onEdit = { viewModel.openEdit(customers[index]) },
                            onToggleActive = { viewModel.toggleActive(customers[index]) }
                        )
                    }
                }
            }
        }
    }

    if (state.showForm) {
        ModalBottomSheet(onDismissRequest = viewModel::closeForm, sheetState = sheetState) {
            CustomerFormSheet(state = state, viewModel = viewModel)
        }
    }

    if (state.detailCustomer != null) {
        ModalBottomSheet(onDismissRequest = viewModel::closeDetail, sheetState = sheetState) {
            CustomerDetailSheet(state = state, viewModel = viewModel)
        }
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
private fun CustomerRow(
    customer: com.gis.supermercados.domain.model.Customer,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(customer.fullName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    "${documentTypeLabel(customer.documentType)} ${customer.documentId.ifBlank { "-" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GisColors.muted
                )
                Text(
                    listOf(
                        customer.phone.takeIf { it.isNotBlank() },
                        customer.email.takeIf { it.isNotBlank() }
                    ).filterNotNull().joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip(
                        text = stringResource(R.string.customer_points_label, customer.loyaltyPoints),
                        containerColor = GisColors.accent
                    )
                    if (customer.balanceCents != 0L) {
                        StatusChip(
                            text = stringResource(R.string.customer_balance_label, Money.formatCompact(customer.balanceCents)),
                            containerColor = if (customer.balanceCents > 0) GisColors.warning else GisColors.positive
                        )
                    }
                    if (customer.creditLimitCents > 0L) {
                        StatusChip(
                            text = stringResource(R.string.customer_available_credit, Money.formatCompact(customer.availableCreditCents)),
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(checked = customer.isActive, onCheckedChange = { onToggleActive() })
                TextButton(onClick = onEdit) { Text(stringResource(R.string.common_edit)) }
            }
        }
    }
}

@Composable
private fun CustomerFormSheet(state: CustomersUiState, viewModel: CustomersViewModel) {
    val form = state.form
    var documentMenu by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(if (form.id == 0L) R.string.customers_new else R.string.customers_edit_title),
            style = MaterialTheme.typography.titleLarge
        )

        SectionCard(title = stringResource(R.string.customer_section_data)) {
            OutlinedTextField(
                value = form.fullName,
                onValueChange = { value -> viewModel.updateForm { it.copy(fullName = value) } },
                label = { Text(stringResource(R.string.customer_field_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    FilledTonalButton(onClick = { documentMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(documentTypeLabel(form.documentType), Modifier.weight(1f))
                        Icon(Icons.Rounded.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = documentMenu, onDismissRequest = { documentMenu = false }) {
                        DocumentType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(documentTypeLabel(type)) },
                                onClick = {
                                    viewModel.updateForm { it.copy(documentType = type) }
                                    documentMenu = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = form.documentId,
                    onValueChange = { value -> viewModel.updateForm { it.copy(documentId = value) } },
                    label = { Text(stringResource(R.string.customer_field_document)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = form.phone,
                    onValueChange = { value -> viewModel.updateForm { it.copy(phone = value) } },
                    label = { Text(stringResource(R.string.customer_field_phone)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = form.email,
                    onValueChange = { value -> viewModel.updateForm { it.copy(email = value) } },
                    label = { Text(stringResource(R.string.customer_field_email)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = form.address,
                onValueChange = { value -> viewModel.updateForm { it.copy(address = value) } },
                label = { Text(stringResource(R.string.customer_field_address)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = form.city,
                    onValueChange = { value -> viewModel.updateForm { it.copy(city = value) } },
                    label = { Text(stringResource(R.string.customer_field_city)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                AmountField(
                    label = stringResource(R.string.customer_field_credit_limit),
                    value = form.creditLimitText,
                    onValueChange = { value -> viewModel.updateForm { it.copy(creditLimitText = value) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SectionCard(title = stringResource(R.string.customer_section_notes)) {
            OutlinedTextField(
                value = form.notes,
                onValueChange = { value -> viewModel.updateForm { it.copy(notes = value) } },
                label = { Text(stringResource(R.string.customer_field_notes)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            if (form.id != 0L) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.product_field_active), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.customer_field_active_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = form.isActive, onCheckedChange = { value -> viewModel.updateForm { it.copy(isActive = value) } })
                }
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
private fun CustomerDetailSheet(state: CustomersUiState, viewModel: CustomersViewModel) {
    val customer = state.detailCustomer ?: return
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(customer.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "${documentTypeLabel(customer.documentType)} ${customer.documentId.ifBlank { "-" }} · " +
                customer.phone.ifBlank { stringResource(R.string.common_no_data) },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                label = stringResource(R.string.customer_detail_purchases),
                value = customer.purchaseCount.toString(),
                caption = stringResource(
                    R.string.customer_detail_last,
                    if (customer.lastPurchaseAt > 0L) AppDateTime.formatDate(customer.lastPurchaseAt) else "-"
                ),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.customer_detail_points),
                value = customer.loyaltyPoints.toString(),
                caption = stringResource(R.string.customer_detail_balance, Money.format(customer.balanceCents)),
                modifier = Modifier.weight(1f)
            )
        }

        SectionCard(title = stringResource(R.string.customer_detail_credit)) {
            Row {
                Text(stringResource(R.string.customer_field_credit_limit), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(Money.format(customer.creditLimitCents), style = MaterialTheme.typography.bodyMedium)
            }
            Row {
                Text(stringResource(R.string.customer_available_credit), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(Money.format(customer.availableCreditCents), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.adjustBalance(-Money.parse("50.00")!!) }) {
                    Text(stringResource(R.string.customer_action_pay_50))
                }
                OutlinedButton(onClick = { viewModel.adjustBalance(Money.parse("50.00")!!) }) {
                    Text(stringResource(R.string.customer_action_charge_50))
                }
                OutlinedButton(onClick = { viewModel.addLoyaltyPoints(100) }) {
                    Text(stringResource(R.string.customer_action_points_100))
                }
            }
        }

        HorizontalDivider()
        Text(stringResource(R.string.customer_detail_history), style = MaterialTheme.typography.titleSmall)
        if (state.detailHistory.isEmpty()) {
            Text(
                stringResource(R.string.customer_detail_history_empty),
                style = MaterialTheme.typography.bodySmall,
                color = GisColors.muted
            )
        } else {
            state.detailHistory.forEach { sale ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.sales_row_ticket, sale.ticketNumber),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${AppDateTime.formatDateTime(sale.createdAt)} · ${sale.storeName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GisColors.muted
                        )
                    }
                    Text(Money.format(sale.totalCents), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
