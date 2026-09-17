package com.gis.supermercados.ui.sales

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.KeyboardReturn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.gis.supermercados.core.designsystem.AmountText
import com.gis.supermercados.core.designsystem.GisColors
import com.gis.supermercados.core.designsystem.GisTopBar
import com.gis.supermercados.core.designsystem.LoadingState
import com.gis.supermercados.core.designsystem.SectionCard
import com.gis.supermercados.core.designsystem.StatusChip
import com.gis.supermercados.domain.model.SaleDetail
import com.gis.supermercados.domain.model.SaleStatus
import com.gis.supermercados.ui.common.asString
import com.gis.supermercados.ui.common.paymentLabel
import com.gis.supermercados.ui.common.saleStatusLabel

/** Detalle de una venta: lineas, totales, devoluciones y anulacion. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailScreen(
    onBack: () -> Unit,
    viewModel: SaleDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            GisTopBar(
                title = state.detail?.sale?.ticketNumber ?: stringResource(R.string.sale_detail_title),
                subtitle = state.detail?.let { AppDateTime.formatDateTime(it.sale.createdAt) },
                onBack = onBack,
                actions = {
                    val sale = state.detail?.sale
                    if (sale != null && sale.status == SaleStatus.COMPLETADA) {
                        IconButton(onClick = viewModel::openVoidDialog) {
                            Icon(
                                Icons.Rounded.Block,
                                contentDescription = stringResource(R.string.sale_detail_void),
                                tint = GisColors.negative
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))

            state.detail == null -> Text(
                text = state.error?.asString().orEmpty(),
                modifier = Modifier.padding(padding).padding(24.dp),
                color = GisColors.negative
            )

            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { HeaderCard(state.detail!!) }
                item { ItemsCard(state.detail!!, state.returnQuantities, viewModel) }
                item { TotalsCard(state.detail!!) }
                if (state.detail!!.creditNotes.isNotEmpty()) {
                    item { CreditNotesCard(state.detail!!) }
                }
                if (state.detail!!.canBeReturned && state.canApproveReturns) {
                    item {
                        Button(
                            onClick = viewModel::openReturnDialog,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Rounded.KeyboardReturn, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.sale_detail_return_button))
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }

    if (state.showReturnDialog && state.detail != null) {
        ReturnDialog(
            detail = state.detail!!,
            quantities = state.returnQuantities,
            reason = state.returnReason,
            restock = state.restock,
            isProcessing = state.isProcessing,
            onQuantityChange = viewModel::onReturnQuantityChange,
            onReasonChange = viewModel::onReturnReasonChange,
            onRestockChange = viewModel::onRestockChange,
            onConfirm = viewModel::confirmReturn,
            onDismiss = viewModel::closeReturnDialog
        )
    }

    if (state.showVoidDialog) {
        AlertDialog(
            onDismissRequest = viewModel::closeVoidDialog,
            title = { Text(stringResource(R.string.sale_detail_void)) },
            text = { Text(stringResource(R.string.sale_detail_void_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::voidSale) {
                    Text(stringResource(R.string.common_confirm), color = GisColors.negative)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::closeVoidDialog) { Text(stringResource(R.string.common_cancel)) } }
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
private fun HeaderCard(detail: SaleDetail) {
    SectionCard(title = stringResource(R.string.sale_detail_header)) {
        InfoRow(stringResource(R.string.sale_detail_store), detail.sale.storeName)
        InfoRow(stringResource(R.string.sale_detail_customer), detail.sale.customerName.ifBlank { stringResource(R.string.sale_detail_no_customer) })
        InfoRow(stringResource(R.string.sale_detail_user), detail.sale.userName)
        InfoRow(stringResource(R.string.sale_detail_payment), paymentLabel(detail.sale.paymentMethod))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.sale_detail_status),
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StatusChip(
                text = saleStatusLabel(detail.sale.status),
                containerColor = when (detail.sale.status) {
                    SaleStatus.COMPLETADA -> GisColors.positive
                    SaleStatus.PARCIALMENTE_DEVUELTA -> GisColors.warning
                    SaleStatus.DEVUELTA -> MaterialTheme.colorScheme.secondary
                    SaleStatus.ANULADA -> GisColors.negative
                }
            )
        }
        if (detail.sale.notes.isNotBlank()) {
            InfoRow(stringResource(R.string.sale_detail_notes), detail.sale.notes)
        }
    }
}

@Composable
private fun ItemsCard(
    detail: SaleDetail,
    returnQuantities: Map<Long, Int>,
    viewModel: SaleDetailViewModel,
) {
    SectionCard(
        title = stringResource(R.string.sale_detail_items_title),
        subtitle = stringResource(R.string.sale_detail_items_subtitle, detail.items.size)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            detail.items.forEach { item ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                "${item.sku} · ${Money.format(item.unitPriceCents)} x ${item.quantity}",
                                style = MaterialTheme.typography.labelSmall,
                                color = GisColors.muted
                            )
                            if (item.returnedQuantity > 0) {
                                Text(
                                    stringResource(R.string.sale_detail_returned_qty, item.returnedQuantity),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GisColors.warning
                                )
                            }
                        }
                        AmountText(item.lineTotalCents, colored = false, style = MaterialTheme.typography.labelLarge)
                    }
                    if (detail.canBeReturned && item.returnableQuantity > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.sale_detail_return_qty_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = {
                                val current = returnQuantities[item.id] ?: 0
                                viewModel.onReturnQuantityChange(item.id, (current - 1).coerceAtLeast(0))
                            }) { Text("-", style = MaterialTheme.typography.titleMedium) }
                            Text("${returnQuantities[item.id] ?: 0}", style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = {
                                val current = returnQuantities[item.id] ?: 0
                                viewModel.onReturnQuantityChange(item.id, current + 1)
                            }) { Text("+", style = MaterialTheme.typography.titleMedium) }
                        }
                    }
                    HorizontalDivider(Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun TotalsCard(detail: SaleDetail) {
    SectionCard(title = stringResource(R.string.sale_detail_totals)) {
        InfoRow(stringResource(R.string.pos_subtotal), Money.format(detail.sale.subtotalCents))
        InfoRow(stringResource(R.string.pos_discount), "-" + Money.format(detail.sale.discountCents))
        InfoRow(stringResource(R.string.pos_tax), Money.format(detail.sale.taxCents))
        HorizontalDivider(Modifier.padding(vertical = 6.dp))
        InfoRow(stringResource(R.string.pos_total), Money.format(detail.sale.totalCents), bold = true)
        InfoRow(stringResource(R.string.sale_detail_cost), Money.format(detail.sale.costCents))
        InfoRow(stringResource(R.string.sale_detail_profit), Money.format(detail.sale.profitCents))
        if (detail.totalReturnedCents > 0L) {
            InfoRow(stringResource(R.string.sale_detail_returned_total), "-" + Money.format(detail.totalReturnedCents))
            InfoRow(stringResource(R.string.sale_detail_net_total), Money.format(detail.netTotalCents), bold = true)
        }
        if (detail.sale.cashReceivedCents > 0L) {
            InfoRow(stringResource(R.string.pos_cash_received), Money.format(detail.sale.cashReceivedCents))
            InfoRow(stringResource(R.string.pos_change), Money.format(detail.sale.changeCents))
        }
    }
}

@Composable
private fun CreditNotesCard(detail: SaleDetail) {
    SectionCard(
        title = stringResource(R.string.sale_detail_credit_notes),
        subtitle = stringResource(R.string.sale_detail_credit_notes_subtitle, detail.creditNotes.size)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            detail.creditNotes.forEach { note ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                note.creditNoteNumber,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            AmountText(-note.totalCents, style = MaterialTheme.typography.labelLarge)
                        }
                        Text(
                            "${AppDateTime.formatDateTime(note.createdAt)} · ${note.itemCount} ${stringResource(R.string.sale_detail_items_word)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GisColors.muted
                        )
                        Text(note.reason, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, bold: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ReturnDialog(
    detail: SaleDetail,
    quantities: Map<Long, Int>,
    reason: String,
    restock: Boolean,
    isProcessing: Boolean,
    onQuantityChange: (Long, Int) -> Unit,
    onReasonChange: (String) -> Unit,
    onRestockChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val returnableItems = detail.items.filter { it.returnableQuantity > 0 }
    val totalReturn = returnableItems.sumOf { item ->
        val quantity = quantities[item.id] ?: 0
        Money.afterDiscount(item.unitPriceCents * quantity, item.discountPercent) +
            Money.tax(Money.afterDiscount(item.unitPriceCents * quantity, item.discountPercent), item.taxRate)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.return_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                returnableItems.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.productName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text(
                                stringResource(R.string.return_dialog_max, item.returnableQuantity),
                                style = MaterialTheme.typography.labelSmall,
                                color = GisColors.muted
                            )
                        }
                        FilledTonalButton(onClick = { onQuantityChange(item.id, (quantities[item.id] ?: 0) - 1) }) {
                            Text("-")
                        }
                        Text(
                            "${quantities[item.id] ?: 0}",
                            Modifier.padding(horizontal = 10.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
                        FilledTonalButton(onClick = { onQuantityChange(item.id, (quantities[item.id] ?: 0) + 1) }) {
                            Text("+")
                        }
                    }
                }
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.return_dialog_total), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    AmountText(totalReturn, colored = false, style = MaterialTheme.typography.titleSmall)
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text(stringResource(R.string.return_dialog_reason)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.return_dialog_restock), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.return_dialog_restock_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = restock, onValueChange = onRestockChange)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = quantities.values.any { it > 0 } && !isProcessing) {
                Text(stringResource(R.string.return_dialog_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )
}
