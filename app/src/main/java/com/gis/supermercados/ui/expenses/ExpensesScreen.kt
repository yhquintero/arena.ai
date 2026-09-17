package com.gis.supermercados.ui.expenses

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.gis.supermercados.domain.model.Expense
import com.gis.supermercados.domain.model.ExpenseCategory
import com.gis.supermercados.ui.common.asString
import com.gis.supermercados.ui.common.expenseCategoryLabel
import com.gis.supermercados.ui.common.paymentLabel

/** Gastos operacionales: registro, justificantes y analisis rapido. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    onBack: (() -> Unit)? = null,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.attachReceipt(it.toString()) }
    }

    val total = expenses.sumOf { it.totalCents }
    val corporate = expenses.filter { it.isCorporate }.sumOf { it.totalCents }

    Scaffold(
        topBar = {
            GisTopBar(
                title = stringResource(R.string.expenses_title),
                subtitle = stringResource(R.string.expenses_subtitle, expenses.size),
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openCreate,
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(stringResource(R.string.expenses_new)) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        label = stringResource(R.string.expenses_total_period),
                        value = Money.formatCompact(total),
                        caption = stringResource(R.string.expenses_count, expenses.size),
                        icon = Icons.Rounded.ReceiptLong,
                        accent = GisColors.negative,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = stringResource(R.string.expenses_corporate),
                        value = Money.formatCompact(corporate),
                        caption = stringResource(R.string.expenses_by_store, Money.formatCompact(total - corporate)),
                        modifier = Modifier.weight(1f)
                    )
                }
                SearchField(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    placeholder = stringResource(R.string.expenses_search_placeholder)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ExpensePeriod.entries.size) { index ->
                        val period = ExpensePeriod.entries[index]
                        FilterChip(
                            selected = state.filterPeriod == period,
                            onClick = { viewModel.onPeriodFilter(period) },
                            label = { Text(periodLabel(period)) }
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.filterStoreId == null,
                            onClick = { viewModel.onStoreFilter(null) },
                            label = { Text(stringResource(R.string.expenses_all_stores)) }
                        )
                    }
                    items(state.stores.size) { index ->
                        val store = state.stores[index]
                        FilterChip(
                            selected = state.filterStoreId == store.id,
                            onClick = { viewModel.onStoreFilter(store.id) },
                            label = { Text(store.name) }
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.filterCategory == null,
                            onClick = { viewModel.onCategoryFilter(null) },
                            label = { Text(stringResource(R.string.expenses_all_categories)) }
                        )
                    }
                    items(ExpenseCategory.entries.size) { index ->
                        val category = ExpenseCategory.entries[index]
                        FilterChip(
                            selected = state.filterCategory == category,
                            onClick = { viewModel.onCategoryFilter(category) },
                            label = { Text(expenseCategoryLabel(category)) }
                        )
                    }
                }
            }

            if (expenses.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.expenses_empty_title),
                    message = stringResource(R.string.expenses_empty_message),
                    icon = Icons.Rounded.ReceiptLong,
                    actionLabel = stringResource(R.string.expenses_new),
                    onAction = viewModel::openCreate
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(expenses.size, key = { index -> expenses[index].id }) { index ->
                        ExpenseRow(
                            expense = expenses[index],
                            onClick = { viewModel.openEdit(expenses[index]) },
                            onDelete = { viewModel.requestDelete(expenses[index]) }
                        )
                    }
                }
            }
        }
    }

    if (state.showForm) {
        ModalBottomSheet(onDismissRequest = viewModel::closeForm, sheetState = sheetState) {
            ExpenseFormSheet(
                state = state,
                viewModel = viewModel,
                onPickReceipt = { receiptPicker.launch(arrayOf("*/*")) }
            )
        }
    }

    state.pendingDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.expenses_delete_title)) },
            text = { Text(stringResource(R.string.expenses_delete_message, expense.concept)) },
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
private fun ExpenseRow(expense: Expense, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(expense.concept, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    "${AppDateTime.formatDate(expense.expenseDate)} · ${expense.provider.ifBlank { "-" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GisColors.muted
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip(
                        text = expenseCategoryLabel(expense.category),
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                    StatusChip(
                        text = if (expense.isCorporate) stringResource(R.string.expense_tag_corporate)
                        else expense.storeName.ifBlank { stringResource(R.string.expense_tag_store) },
                        containerColor = if (expense.isCorporate) GisColors.accent else MaterialTheme.colorScheme.primary
                    )
                    if (expense.hasReceipt) {
                        StatusChip(text = stringResource(R.string.expense_tag_receipt), containerColor = GisColors.positive)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.format(expense.totalCents), style = MaterialTheme.typography.titleSmall, color = GisColors.negative)
                Text(paymentLabel(expense.paymentMethod), style = MaterialTheme.typography.labelSmall, color = GisColors.muted)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.common_delete), tint = GisColors.negative)
                }
            }
        }
    }
}

@Composable
private fun ExpenseFormSheet(
    state: ExpensesUiState,
    viewModel: ExpensesViewModel,
    onPickReceipt: () -> Unit,
) {
    val form = state.form
    var categoryMenu by remember { mutableStateOf(false) }
    var storeMenu by remember { mutableStateOf(false) }
    var paymentMenu by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(if (form.id == 0L) R.string.expenses_new else R.string.expenses_edit_title),
            style = MaterialTheme.typography.titleLarge
        )

        SectionCard(title = stringResource(R.string.expense_section_data)) {
            Box(Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = { categoryMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(expenseCategoryLabel(form.category), Modifier.weight(1f))
                    Icon(Icons.Rounded.ArrowDropDown, null)
                }
                DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                    ExpenseCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(expenseCategoryLabel(category)) },
                            onClick = {
                                viewModel.updateForm { it.copy(category = category) }
                                categoryMenu = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = { storeMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        state.stores.firstOrNull { it.id == form.storeId }?.name
                            ?: stringResource(R.string.expense_field_corporate),
                        Modifier.weight(1f)
                    )
                    Icon(Icons.Rounded.ArrowDropDown, null)
                }
                DropdownMenu(expanded = storeMenu, onDismissRequest = { storeMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.expense_field_corporate)) },
                        onClick = {
                            viewModel.updateForm { it.copy(storeId = null) }
                            storeMenu = false
                        }
                    )
                    state.stores.forEach { store ->
                        DropdownMenuItem(
                            text = { Text(store.name) },
                            onClick = {
                                viewModel.updateForm { it.copy(storeId = store.id) }
                                storeMenu = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = form.concept,
                onValueChange = { value -> viewModel.updateForm { it.copy(concept = value) } },
                label = { Text(stringResource(R.string.expense_field_concept)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = form.provider,
                onValueChange = { value -> viewModel.updateForm { it.copy(provider = value) } },
                label = { Text(stringResource(R.string.expense_field_provider)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = AppDateTime.formatIso(form.expenseDate),
                onValueChange = { },
                label = { Text(stringResource(R.string.expense_field_date)) },
                enabled = false,
                supportingText = { Text(stringResource(R.string.expense_field_date_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionCard(title = stringResource(R.string.expense_section_amount)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AmountField(
                    label = stringResource(R.string.expense_field_amount),
                    value = form.amountText,
                    onValueChange = { value -> viewModel.updateForm { it.copy(amountText = value) } },
                    modifier = Modifier.weight(1.4f)
                )
                AmountField(
                    label = stringResource(R.string.expense_field_tax_percent),
                    value = form.taxRatePercent,
                    onValueChange = { value -> viewModel.updateForm { it.copy(taxRatePercent = value) } },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Text(stringResource(R.string.pos_tax), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(Money.format(form.taxCents), style = MaterialTheme.typography.bodyMedium)
            }
            Row {
                Text(
                    stringResource(R.string.pos_total),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    Money.format(form.totalCents),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = { paymentMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(paymentLabel(form.paymentMethod), Modifier.weight(1f))
                    Icon(Icons.Rounded.ArrowDropDown, null)
                }
                DropdownMenu(expanded = paymentMenu, onDismissRequest = { paymentMenu = false }) {
                    com.gis.supermercados.domain.model.PaymentMethod.entries.forEach { method ->
                        DropdownMenuItem(
                            text = { Text(paymentLabel(method)) },
                            onClick = {
                                viewModel.updateForm { it.copy(paymentMethod = method) }
                                paymentMenu = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = form.reference,
                onValueChange = { value -> viewModel.updateForm { it.copy(reference = value) } },
                label = { Text(stringResource(R.string.expense_field_reference)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.expense_receipt_label), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (form.receiptPath.isBlank()) stringResource(R.string.expense_receipt_none)
                        else form.receiptPath.substringAfterLast('/'),
                        style = MaterialTheme.typography.labelSmall,
                        color = GisColors.muted,
                        maxLines = 1
                    )
                }
                FilledTonalButton(onClick = onPickReceipt, enabled = form.id != 0L) {
                    Icon(Icons.Rounded.AttachFile, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.expense_receipt_attach))
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
private fun periodLabel(period: ExpensePeriod): String = stringResource(
    when (period) {
        ExpensePeriod.HOY -> R.string.period_today
        ExpensePeriod.SEMANA -> R.string.period_this_week
        ExpensePeriod.MES -> R.string.period_this_month
        ExpensePeriod.TRIMESTRE -> R.string.period_this_quarter
        ExpensePeriod.TODO -> R.string.period_all
    }
)
