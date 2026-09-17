package com.gis.supermercados.ui.reports

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.core.common.FileSharing
import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.designsystem.EmptyState
import com.gis.supermercados.core.designsystem.GisColors
import com.gis.supermercados.core.designsystem.GisTopBar
import com.gis.supermercados.core.designsystem.SectionCard
import com.gis.supermercados.core.designsystem.StatusChip
import com.gis.supermercados.core.reporting.model.ExportFormat
import com.gis.supermercados.core.reporting.model.PeriodType
import com.gis.supermercados.core.reporting.model.ReportType
import com.gis.supermercados.domain.model.ReportRecord
import com.gis.supermercados.ui.common.asString
import com.gis.supermercados.ui.common.exportFormatLabel
import com.gis.supermercados.ui.common.periodTypeLabel
import com.gis.supermercados.ui.common.reportTypeLabel
import java.io.File

/** Centro de reportes: seleccion del informe, periodo, sucursal y formato de exportacion. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: (() -> Unit)? = null,
    onPreview: (String) -> Unit,
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    var dateTarget by remember { mutableStateOf<String?>(null) }
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        uri?.let { viewModel.copyTo(it.toString()) }
    }

    Scaffold(
        topBar = {
            GisTopBar(
                title = stringResource(R.string.reports_title),
                subtitle = stringResource(R.string.reports_subtitle),
                onBack = onBack
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 16.dp) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.reports_tab_new)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.reports_tab_history)) })
            }

            if (tab == 0) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SectionCard(title = stringResource(R.string.reports_section_type)) {
                            ReportType.entries.forEach { type ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.onTypeChange(type) }
                                        .padding(vertical = 10.dp)
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            reportTypeLabel(type),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (state.type == type) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            reportTypeDescription(type),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GisColors.muted
                                        )
                                    }
                                    if (state.type == type) {
                                        StatusChip(text = stringResource(R.string.reports_selected), containerColor = GisColors.accent)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SectionCard(title = stringResource(R.string.reports_section_period)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(PeriodType.entries.size) { index ->
                                    val period = PeriodType.entries[index]
                                    FilterChip(
                                        selected = state.period == period,
                                        onClick = { viewModel.onPeriodChange(period) },
                                        label = { Text(periodTypeLabel(period)) }
                                    )
                                }
                            }
                            if (state.period == PeriodType.PERSONALIZADO) {
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    DateButton(
                                        label = stringResource(R.string.reports_field_from),
                                        value = AppDateTime.formatDate(state.customStart),
                                        modifier = Modifier.weight(1f)
                                    ) { dateTarget = DATE_START }
                                    DateButton(
                                        label = stringResource(R.string.reports_field_to),
                                        value = AppDateTime.formatDate(state.customEnd),
                                        modifier = Modifier.weight(1f)
                                    ) { dateTarget = DATE_END }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(
                                    R.string.reports_range_summary,
                                    state.range.label.ifBlank {
                                        "${AppDateTime.formatDate(state.range.startMillis)} - ${AppDateTime.formatDate(state.range.endMillis)}"
                                    },
                                    state.range.days
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    item {
                        SectionCard(title = stringResource(R.string.reports_section_scope)) {
                            StorePickerField(
                                stores = state.stores.map { it.id to it.name },
                                selectedId = state.storeId,
                                allLabel = stringResource(R.string.reports_all_stores),
                                onSelect = viewModel::onStoreChange
                            )
                            Spacer(Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(ExportFormat.entries.size) { index ->
                                    val format = ExportFormat.entries[index]
                                    FilterChip(
                                        selected = state.format == format,
                                        onClick = { viewModel.onFormatChange(format) },
                                        label = { Text(exportFormatLabel(format)) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = viewModel::generate,
                                enabled = !state.isGenerating,
                                modifier = Modifier.weight(1f).height(52.dp)
                            ) {
                                Icon(Icons.Rounded.Visibility, null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.reports_preview))
                            }
                            Button(
                                onClick = viewModel::export,
                                enabled = !state.isExporting,
                                modifier = Modifier.weight(1f).height(52.dp)
                            ) {
                                Icon(Icons.Rounded.Share, null)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    stringResource(
                                        if (state.isExporting) R.string.reports_exporting else R.string.reports_export
                                    )
                                )
                            }
                        }
                    }

                    item {
                        if (state.document != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onPreview("current") },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Assessment, null)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            state.document!!.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            state.document!!.periodLabel,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Text(stringResource(R.string.reports_open_preview), style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            } else {
                if (history.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.reports_history_empty_title),
                        message = stringResource(R.string.reports_history_empty_message),
                        icon = Icons.Rounded.History
                    )
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history.size, key = { index -> history[index].id }) { index ->
                            HistoryRow(
                                record = history[index],
                                onShare = {
                                    val file = File(history[index].filePath)
                                    if (file.exists()) {
                                        shareLauncher.launch(
                                            FileSharing.shareIntent(
                                                context = context,
                                                file = file,
                                                fileName = history[index].fileName,
                                                mimeType = mimeTypeOf(history[index].fileName),
                                                title = context.getString(R.string.reports_share_title)
                                            )
                                        )
                                    }
                                },
                                onSaveAs = { saveLauncher.launch(history[index].fileName) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (dateTarget != null) {
        val initial = if (dateTarget == DATE_START) state.customStart else state.customEnd
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = AppDateTime.startOfDay(initial))
        DatePickerDialog(
            onDismissRequest = { dateTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        if (dateTarget == DATE_START) {
                            viewModel.onCustomStartChange(AppDateTime.startOfDay(millis))
                        } else {
                            viewModel.onCustomEndChange(AppDateTime.endOfDay(millis))
                        }
                    }
                    dateTarget = null
                }) { Text(stringResource(R.string.common_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { dateTarget = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        ) {
            DatePicker(state = pickerState)
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
private fun HistoryRow(record: ReportRecord, onShare: () -> Unit, onSaveAs: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(record.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    "${AppDateTime.formatDateTime(record.createdAt)} · ${FileSharing.readableSize(record.sizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GisColors.muted
                )
                Text(
                    stringResource(
                        R.string.reports_history_detail,
                        record.userName.ifBlank { "-" },
                        AppDateTime.formatDate(record.startMillis),
                        AppDateTime.formatDate(record.endMillis)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onSaveAs) { Text(stringResource(R.string.reports_save_as)) }
            TextButton(onClick = onShare) {
                Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.common_share))
            }
        }
    }
}

@Composable
private fun DateButton(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StorePickerField(
    stores: List<Pair<Long, String>>,
    selectedId: Long?,
    allLabel: String,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        FilledTonalButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                stores.firstOrNull { it.first == selectedId }?.second ?: allLabel,
                Modifier.weight(1f),
                maxLines = 1
            )
            Icon(Icons.Rounded.ArrowDropDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(allLabel) }, onClick = { onSelect(null); expanded = false })
            stores.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
}

@Composable
private fun reportTypeDescription(type: ReportType): String = stringResource(
    when (type) {
        ReportType.RESUMEN_EJECUTIVO -> R.string.report_desc_executive
        ReportType.BALANCE_GENERAL -> R.string.report_desc_balance
        ReportType.FLUJO_CAJA -> R.string.report_desc_cash_flow
        ReportType.VENTAS_POR_TIENDA -> R.string.report_desc_sales_by_store
        ReportType.PRODUCTOS_MAS_VENDIDOS -> R.string.report_desc_top_products
        ReportType.RENTABILIDAD_CATEGORIA -> R.string.report_desc_category
        ReportType.ANALISIS_GASTOS -> R.string.report_desc_expenses
        ReportType.COMPARATIVA_PERIODOS -> R.string.report_desc_comparison
        ReportType.PROYECCIONES -> R.string.report_desc_projections
        ReportType.INVENTARIO_VALORADO -> R.string.report_desc_inventory
    }
)

/** Tipo MIME segun la extension del archivo exportado. */
private fun mimeTypeOf(fileName: String): String = when (fileName.substringAfterLast('.', "pdf").lowercase()) {
    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    "csv" -> "text/csv"
    else -> "application/pdf"
}

private const val DATE_START = "start"
private const val DATE_END = "end"
