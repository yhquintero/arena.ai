package com.gis.supermercados.ui.catalog

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gis.supermercados.R
import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.designsystem.EmptyState
import com.gis.supermercados.core.designsystem.GisColors
import com.gis.supermercados.core.designsystem.GisTopBar
import com.gis.supermercados.core.designsystem.SearchField
import com.gis.supermercados.core.designsystem.StatusChip
import com.gis.supermercados.core.common.formatPercent
import com.gis.supermercados.domain.model.Product
import com.gis.supermercados.ui.common.asString

/** Catalogo de productos y servicios. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    onNewProduct: () -> Unit,
    onEditProduct: (Long) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: ProductsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            GisTopBar(
                title = stringResource(R.string.products_title),
                subtitle = stringResource(R.string.products_subtitle, products.count { it.isActive }),
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewProduct,
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(stringResource(R.string.products_new)) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchField(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    placeholder = stringResource(R.string.products_search_placeholder)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.categoryId == null,
                            onClick = { viewModel.onCategorySelected(null) },
                            label = { Text(stringResource(R.string.products_all_categories)) }
                        )
                    }
                    items(categories.size) { index ->
                        val category = categories[index]
                        FilterChip(
                            selected = state.categoryId == category.id,
                            onClick = { viewModel.onCategorySelected(category.id) },
                            label = { Text(category.name) }
                        )
                    }
                }
            }

            if (products.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.products_empty_title),
                    message = stringResource(R.string.products_empty_message),
                    icon = Icons.Rounded.Inventory,
                    actionLabel = stringResource(R.string.products_new),
                    onAction = onNewProduct
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products.size, key = { index -> products[index].id }) { index ->
                        ProductRow(
                            product = products[index],
                            categoryName = categories.firstOrNull { it.id == products[index].categoryId }?.name.orEmpty(),
                            onClick = { onEditProduct(products[index].id) },
                            onToggleActive = { viewModel.toggleActive(products[index]) },
                            onDelete = { viewModel.requestDelete(products[index]) }
                        )
                    }
                }
            }
        }
    }

    state.pendingDelete?.let { product ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.products_delete_title)) },
            text = { Text(stringResource(R.string.products_delete_message, product.name)) },
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
private fun ProductRow(
    product: Product,
    categoryName: String,
    onClick: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${product.sku} · $categoryName",
                    style = MaterialTheme.typography.labelSmall,
                    color = GisColors.muted,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(Money.format(product.priceCents), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.products_cost_label, Money.format(product.costCents)),
                        style = MaterialTheme.typography.labelSmall,
                        color = GisColors.muted
                    )
                    StatusChip(
                        text = stringResource(R.string.products_margin_label, formatPercent(product.marginPercent)),
                        containerColor = if (product.marginPercent >= MIN_HEALTHY_MARGIN) GisColors.positive else GisColors.warning
                    )
                    if (product.isService) {
                        StatusChip(text = stringResource(R.string.products_service_tag), containerColor = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(checked = product.isActive, onCheckedChange = { onToggleActive() })
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.common_delete), tint = GisColors.negative)
                }
            }
        }
    }
}

/** Margen minimo considerado saludable para resaltar en verde. */
private const val MIN_HEALTHY_MARGIN = 15.0
