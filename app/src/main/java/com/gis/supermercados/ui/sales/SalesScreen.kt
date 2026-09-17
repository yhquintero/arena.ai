package com.gis.supermercados.ui.sales

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.gis.supermercados.core.designsystem.EmptyState
import com.gis.supermercados.core.designsystem.GisColors
import com.gis.supermercados.core.designsystem.GisTopBar
import com.gis.supermercados.core.designsystem.SearchField
import com.gis.supermercados.core.designsystem.StatCard
import com.gis.supermercados.core.designsystem.StatusChip
import com.gis.supermercados.domain.model.Sale
import com.gis.supermercados.domain.model.SaleStatus
import com.gis.supermercados.ui.common.paymentLabel
import com.gis.supermercados.ui.common.saleStatusLabel

/** Historial de ventas con filtros y totales del periodo seleccionado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    onOpenSale: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: SalesViewModel = hiltViewModel(),
) {
    val filters by viewModel.uiState.collectAsStateWithLifecycle()
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val stores by viewModel.stores.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            GisTopBar(
                title = stringResource(R.string.sales_title),
                subtitle = stringResource(R.string.sales_subtitle, sales.size),
                onBack = onBack
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchField(
                    query = filters.query,
                    onQueryChange = viewModel::onQueryChange,
                    placeholder = stringResource(R.string.sales_search_placeholder)
                )
                FilterRow(
                    label = stringResource(R.string.sales_filter_period),
                    options = SalesPeriod.entries.toList(),
                    selected = filters.period,
                    optionLabel = { periodLabel(it) },
                    onSelect = viewModel::onPeriodSelected
                )
                FilterRow(
                    label = stringResource(R.string.sales_filter_store),
                    options = listOf(null) + stores.map { it.id },
                    selected = filters.storeId,
                    optionLabel = { id ->
                        if (id == null) stringResource(R.string.common_all_stores)
                        else stores.firstOrNull { it.id == id }?.name.orEmpty()
                    },
                    onSelect = viewModel::onStoreSelected
                )
                FilterRow(
                    label = stringResource(R.string.sales_filter_status),
                    options = listOf(null) + SaleStatus.entries.toList(),
                    selected = filters.status,
                    optionLabel = { status ->
                        if (status == null) stringResource(R.string.sales_status_all) else saleStatusLabel(status)
                    },
                    onSelect = viewModel::onStatusSelected
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        label = stringResource(R.string.sales_total_period),
                        value = Money.formatCompact(totals.totalCents),
                        caption = stringResource(R.string.sales_tickets_count, totals.tickets),
                        icon = Icons.Rounded.ReceiptLong,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = stringResource(R.string.sales_profit_period),
                        value = Money.formatCompact(totals.profitCents),
                        caption = stringResource(R.string.sales_average_ticket, Money.formatCompact(totals.averageTicketCents)),
                        accent = GisColors.positive,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (sales.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.sales_empty_title),
                    message = stringResource(R.string.sales_empty_message),
                    icon = Icons.Rounded.PointOfSale
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sales.size, key = { index -> sales[index].id }) { index ->
                        SaleRow(sale = sales[index], onClick = { onOpenSale(sales[index].id) })
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SaleRow(sale: Sale, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(sale.ticketNumber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    StatusChip(
                        text = saleStatusLabel(sale.status),
                        containerColor = when (sale.status) {
                            SaleStatus.COMPLETADA -> GisColors.positive
                            SaleStatus.PARCIALMENTE_DEVUELTA -> GisColors.warning
                            SaleStatus.DEVUELTA -> MaterialTheme.colorScheme.secondary
                            SaleStatus.ANULADA -> GisColors.negative
                        }
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${AppDateTime.formatDateTime(sale.createdAt)} · ${sale.storeName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GisColors.muted
                )
                Text(
                    stringResource(R.string.sales_row_detail, sale.itemCount, paymentLabel(sale.paymentMethod)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AmountText(
                cents = sale.totalCents,
                colored = false,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun <T> FilterRow(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(options.size) { index ->
            val option = options[index]
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(optionLabel(option)) }
            )
        }
    }
}

@Composable
private fun periodLabel(period: SalesPeriod): String = stringResource(
    when (period) {
        SalesPeriod.HOY -> R.string.period_today
        SalesPeriod.SEMANA -> R.string.period_this_week
        SalesPeriod.MES -> R.string.period_this_month
        SalesPeriod.TODO -> R.string.period_all
    }
)
