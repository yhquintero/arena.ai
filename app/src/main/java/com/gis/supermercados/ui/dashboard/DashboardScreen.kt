package com.gis.supermercados.ui.dashboard

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.gis.supermercados.core.designsystem.ChartPoint
import com.gis.supermercados.core.designsystem.ErrorState
import com.gis.supermercados.core.designsystem.GisColors
import com.gis.supermercados.core.designsystem.GisTopBar
import com.gis.supermercados.core.designsystem.LineChart
import com.gis.supermercados.core.designsystem.LoadingState
import com.gis.supermercados.core.designsystem.SectionCard
import com.gis.supermercados.core.designsystem.ShareBar
import com.gis.supermercados.core.designsystem.StatCard
import com.gis.supermercados.core.designsystem.StatusChip
import com.gis.supermercados.domain.model.StockLevel
import com.gis.supermercados.ui.common.asString

/** Panel de inicio: lo esencial del negocio de un vistazo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToPos: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToSales: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            GisTopBar(
                title = stringResource(R.string.dashboard_title),
                subtitle = state.todayLabel,
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.common_refresh))
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading && state.stats.todayTotalCents == 0L && state.stats.monthTotalCents == 0L ->
                LoadingState(Modifier.padding(padding))

            state.error != null && state.stats.monthTotalCents == 0L ->
                ErrorState(
                    message = state.error.asString(),
                    onRetry = viewModel::refresh,
                    modifier = Modifier.padding(padding)
                )

            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { KpiRow(state) }
                item { MonthRow(state) }
                item { TrendCard(state) }
                item { StoreCard(state, onNavigateToReports) }
                item { TopProductsCard(state) }
                item { AlertsCard(state.lowStock, onNavigateToInventory) }
                item { QuickActionsRow(onNavigateToPos, onNavigateToSales, onNavigateToExpenses, onNavigateToReports) }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun KpiRow(state: DashboardUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            label = stringResource(R.string.dashboard_today_sales),
            value = Money.formatCompact(state.stats.todayTotalCents),
            caption = stringResource(R.string.dashboard_tickets_today, state.stats.todayTickets),
            trendPercent = state.stats.dayVariationPercent,
            icon = Icons.Rounded.PointOfSale,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = stringResource(R.string.dashboard_today_profit),
            value = Money.formatCompact(state.stats.todayProfitCents),
            caption = stringResource(R.string.dashboard_week_total, Money.formatCompact(state.stats.weekTotalCents)),
            icon = Icons.Rounded.TrendingUp,
            accent = GisColors.positive,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MonthRow(state: DashboardUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            label = stringResource(R.string.dashboard_month_sales),
            value = Money.formatCompact(state.stats.monthTotalCents),
            caption = stringResource(R.string.dashboard_tickets_month, state.stats.monthTickets),
            icon = Icons.Rounded.Assessment,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = stringResource(R.string.dashboard_month_profit),
            value = Money.formatCompact(state.stats.monthProfitCents),
            caption = stringResource(
                R.string.dashboard_month_expenses,
                Money.formatCompact(state.stats.monthExpensesCents)
            ),
            icon = Icons.Rounded.Receipt,
            accent = if (state.stats.monthProfitCents >= 0) GisColors.positive else GisColors.negative,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TrendCard(state: DashboardUiState) {
    SectionCard(
        title = stringResource(R.string.dashboard_trend_title),
        subtitle = stringResource(R.string.dashboard_trend_subtitle)
    ) {
        if (state.stats.dailySeries.isEmpty()) {
            Text(
                stringResource(R.string.dashboard_no_data),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LineChart(
                points = state.stats.dailySeries.map {
                    ChartPoint(AppDateTime.formatShortDate(it.dayMillis), it.salesCents.toDouble())
                },
                valueFormatter = { Money.formatCompact(it.toLong()) }
            )
        }
    }
}

@Composable
private fun StoreCard(state: DashboardUiState, onSeeReports: () -> Unit) {
    val stores = state.stats.salesByStore
    val maxTotal = stores.maxOfOrNull { it.totalCents } ?: 0L
    SectionCard(
        title = stringResource(R.string.dashboard_by_store_title),
        subtitle = stringResource(R.string.dashboard_by_store_subtitle),
        action = {
            FilledTonalButton(onClick = onSeeReports) {
                Text(stringResource(R.string.dashboard_see_reports), style = MaterialTheme.typography.labelMedium)
            }
        }
    ) {
        if (stores.isEmpty()) {
            Text(
                stringResource(R.string.dashboard_no_data),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                stores.forEach { store ->
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(store.storeName, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            AmountText(store.totalCents, colored = false, style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.height(4.dp))
                        ShareBar(
                            fraction = if (maxTotal == 0L) 0f else store.totalCents.toFloat() / maxTotal.toFloat(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            stringResource(
                                R.string.dashboard_store_detail,
                                store.tickets,
                                Money.formatCompact(store.profitCents)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = GisColors.muted,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopProductsCard(state: DashboardUiState) {
    SectionCard(
        title = stringResource(R.string.dashboard_top_products_title),
        subtitle = stringResource(R.string.dashboard_top_products_subtitle)
    ) {
        if (state.stats.topProducts.isEmpty()) {
            Text(
                stringResource(R.string.dashboard_no_data),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.stats.topProducts.forEachIndexed { index, product ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .width(22.dp)
                                .height(22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                product.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                            Text(
                                stringResource(R.string.dashboard_product_units, product.units),
                                style = MaterialTheme.typography.labelSmall,
                                color = GisColors.muted
                            )
                        }
                        AmountText(product.totalCents, colored = false, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertsCard(lowStock: List<StockLevel>, onSeeAll: () -> Unit) {
    SectionCard(
        title = stringResource(R.string.dashboard_alerts_title),
        subtitle = stringResource(R.string.dashboard_alerts_subtitle),
        action = {
            FilledTonalButton(onClick = onSeeAll) {
                Text(stringResource(R.string.dashboard_see_all), style = MaterialTheme.typography.labelMedium)
            }
        }
    ) {
        if (lowStock.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.TrendingUp, null, tint = GisColors.positive)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.dashboard_no_alerts),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                lowStock.forEach { stock ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.WarningAmber,
                            null,
                            tint = if (stock.quantity <= 0) GisColors.negative else GisColors.warning,
                            modifier = Modifier.width(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stock.productName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text(
                                stringResource(R.string.dashboard_alert_detail, stock.storeName, stock.minStock),
                                style = MaterialTheme.typography.labelSmall,
                                color = GisColors.muted
                            )
                        }
                        StatusChip(
                            text = if (stock.quantity <= 0) {
                                stringResource(R.string.stock_out_of_stock)
                            } else {
                                stringResource(R.string.stock_low_short, stock.quantity)
                            },
                            containerColor = if (stock.quantity <= 0) GisColors.negative else GisColors.warning
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onPos: () -> Unit,
    onSales: () -> Unit,
    onExpenses: () -> Unit,
    onReports: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickAction(stringResource(R.string.dashboard_action_pos), Icons.Rounded.PointOfSale, Modifier.weight(1f), onPos)
        QuickAction(stringResource(R.string.dashboard_action_sales), Icons.Rounded.Receipt, Modifier.weight(1f), onSales)
        QuickAction(stringResource(R.string.dashboard_action_expenses), Icons.Rounded.Assessment, Modifier.weight(1f), onExpenses)
        QuickAction(stringResource(R.string.dashboard_action_reports), Icons.Rounded.TrendingUp, Modifier.weight(1f), onReports)
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FilledTonalButton(onClick = onClick, modifier = modifier.height(74.dp), contentPadding = PaddingValues(8.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
