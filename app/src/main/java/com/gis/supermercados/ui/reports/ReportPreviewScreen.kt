package com.gis.supermercados.ui.reports

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gis.supermercados.R
import com.gis.supermercados.core.common.FileSharing
import com.gis.supermercados.core.designsystem.ChartPoint
import com.gis.supermercados.core.designsystem.DonutChart
import com.gis.supermercados.core.designsystem.EmptyState
import com.gis.supermercados.core.designsystem.GisColors
import com.gis.supermercados.core.designsystem.GisTopBar
import com.gis.supermercados.core.designsystem.LineChart
import com.gis.supermercados.core.designsystem.ShareBar
import com.gis.supermercados.core.designsystem.ChartSlice
import com.gis.supermercados.core.designsystem.StatusChip
import com.gis.supermercados.core.reporting.model.CellAlign
import com.gis.supermercados.core.reporting.model.ChartKind
import com.gis.supermercados.core.reporting.model.ReportBlock
import com.gis.supermercados.core.reporting.model.ReportDocument
import com.gis.supermercados.ui.common.asString
import java.io.File

/** Previsualizacion fiel del informe: indicadores, tablas y graficas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportPreviewScreen(
    onBack: () -> Unit,
    viewModel: ReportPreviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val document = state.document
    val context = LocalContext.current
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        uri?.let { viewModel.copyTo(it.toString()) }
    }
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    Scaffold(
        topBar = {
            GisTopBar(
                title = stringResource(R.string.report_preview_title),
                subtitle = document?.periodLabel ?: stringResource(R.string.report_preview_no_document),
                onBack = onBack,
                actions = {
                    if (state.exported != null) {
                        TextButton(onClick = {
                            val file = File(state.exported!!.filePath)
                            if (file.exists()) {
                                shareLauncher.launch(
                                    FileSharing.shareIntent(
                                        context = context,
                                        file = file,
                                        fileName = state.exported!!.fileName,
                                        mimeType = state.exported!!.mimeType,
                                        title = context.getString(R.string.reports_share_title)
                                    )
                                )
                            }
                        }) {
                            Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.common_share))
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (document == null) {
            EmptyState(
                title = stringResource(R.string.report_preview_empty_title),
                message = stringResource(R.string.report_preview_empty_message),
                icon = Icons.Rounded.Description,
                actionLabel = stringResource(R.string.common_back),
                onAction = onBack,
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { DocumentHeader(document) }
                    items(document.blocks.size) { index ->
                        ReportBlockView(block = document.blocks[index], currencySymbol = document.currencySymbol)
                    }
                    if (document.footerNote.isNotBlank()) {
                        item {
                            Text(
                                document.footerNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = GisColors.muted
                            )
                        }
                    }
                }

                if (state.exported != null) {
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                state.exported!!.fileName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                            Text(
                                FileSharing.readableSize(state.exported!!.sizeBytes),
                                style = MaterialTheme.typography.labelSmall,
                                color = GisColors.muted
                            )
                        }
                        Button(
                            onClick = { saveLauncher.launch(state.exported!!.fileName) },
                            enabled = !state.isExporting
                        ) {
                            Text(stringResource(R.string.reports_save_as))
                        }
                    }
                }
            }
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

/** Cabecera del documento: empresa, sucursal, periodo y responsable. */
@Composable
private fun DocumentHeader(document: ReportDocument) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(document.companyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (document.companyTaxId.isNotBlank()) {
                Text(
                    stringResource(R.string.report_preview_tax_id, document.companyTaxId),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (document.companyAddress.isNotBlank()) {
                Text(document.companyAddress, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(8.dp))
            Text(document.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (document.subtitle.isNotBlank()) {
                Text(document.subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { StatusChip(text = document.storeLabel, containerColor = MaterialTheme.colorScheme.primary) }
                item { StatusChip(text = document.periodLabel, containerColor = MaterialTheme.colorScheme.secondary) }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.report_preview_generated, document.generatedAtLabel, document.generatedBy),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReportBlockView(block: ReportBlock, currencySymbol: String) {
    when (block) {
        is ReportBlock.SectionTitle -> Text(
            block.text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        is ReportBlock.Paragraph -> Text(
            block.text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (block.bold) FontWeight.Bold else FontWeight.Normal
        )

        is ReportBlock.KpiGrid -> KpiGridView(block.items, currencySymbol)

        is ReportBlock.Table -> TableView(block, currencySymbol)

        is ReportBlock.Chart -> ChartView(block)

        ReportBlock.PageBreak -> HorizontalDivider(Modifier.padding(vertical = 6.dp))
    }
}

@Composable
private fun KpiGridView(items: List<com.gis.supermercados.core.reporting.model.Kpi>, currencySymbol: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { kpi ->
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = if (kpi.highlight) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                kpi.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                kpi.value,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (kpi.variationPercent != null) {
                                val positive = kpi.variationPercent >= 0.0
                                Text(
                                    stringResource(
                                        if (positive) R.string.report_preview_variation_up else R.string.report_preview_variation_down,
                                        kotlin.math.abs(kpi.variationPercent)
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (positive) GisColors.positive else GisColors.negative
                                )
                            }
                            if (kpi.detail.isNotBlank()) {
                                Text(kpi.detail, style = MaterialTheme.typography.labelSmall, color = GisColors.muted)
                            }
                        }
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Tabla del informe renderizada con pesos de columna y alineacion del modelo. */
@Composable
private fun TableView(block: ReportBlock.Table, currencySymbol: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            if (block.title.isNotBlank()) {
                Text(block.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                block.columns.forEach { column ->
                    Text(
                        column.header,
                        modifier = Modifier.weight(column.weight.coerceAtLeast(0.4f)),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = when (column.align) {
                            CellAlign.START -> TextAlign.Start
                            CellAlign.CENTER -> TextAlign.Center
                            CellAlign.END -> TextAlign.End
                        }
                    )
                }
            }
            HorizontalDivider()
            block.rows.forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    row.forEachIndexed { index, cell ->
                        val weight = block.columns.getOrNull(index)?.weight?.coerceAtLeast(0.4f) ?: 1f
                        val align = cell.alignOf()
                        Text(
                            cell.display(currencySymbol),
                            modifier = Modifier.weight(weight),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = when (align) {
                                CellAlign.START -> TextAlign.Start
                                CellAlign.CENTER -> TextAlign.Center
                                CellAlign.END -> TextAlign.End
                            },
                            maxLines = 2
                        )
                    }
                }
            }
            if (block.totalRow != null) {
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    block.totalRow.forEachIndexed { index, cell ->
                        val weight = block.columns.getOrNull(index)?.weight?.coerceAtLeast(0.4f) ?: 1f
                        Text(
                            cell.display(currencySymbol),
                            modifier = Modifier.weight(weight),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = when (cell.alignOf()) {
                                CellAlign.START -> TextAlign.Start
                                CellAlign.CENTER -> TextAlign.Center
                                CellAlign.END -> TextAlign.End
                            }
                        )
                    }
                }
            }
            if (block.note != null) {
                Spacer(Modifier.height(6.dp))
                Text(block.note, style = MaterialTheme.typography.labelSmall, color = GisColors.muted)
            }
        }
    }
}

/** Graficas del informe: lineas para series, barras/dona para distribuciones. */
@Composable
private fun ChartView(block: ReportBlock.Chart) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(block.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            when (block.kind) {
                ChartKind.LINE -> LineChart(
                    points = block.points.map { ChartPoint(it.label, it.value.toDouble()) },
                    modifier = Modifier.fillMaxWidth()
                )

                ChartKind.DONUT -> DonutChart(
                    slices = block.points.map { ChartSlice(it.label, it.value.toDouble()) },
                    modifier = Modifier.fillMaxWidth(),
                    centerTitle = stringResource(R.string.chart_total)
                )

                ChartKind.BARS, ChartKind.HORIZONTAL_BARS -> {
                    val maxValue = block.points.maxOfOrNull { it.value }?.toDouble() ?: 1.0
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        block.points.forEach { point ->
                            Column {
                                Row {
                                    Text(
                                        point.label,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                    Text(
                                        com.gis.supermercados.core.common.formatLong(point.value),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(Modifier.height(3.dp))
                                ShareBar(fraction = (point.value / maxValue).toFloat())
                            }
                        }
                    }
                }
            }
            if (block.note != null) {
                Spacer(Modifier.height(6.dp))
                Text(block.note, style = MaterialTheme.typography.labelSmall, color = GisColors.muted)
            }
        }
    }
}

/** Alineacion efectiva de la celda (las celdas vacias heredan la de la columna). */
private fun com.gis.supermercados.core.reporting.model.Cell.alignOf(): CellAlign = when (this) {
    is com.gis.supermercados.core.reporting.model.Cell.Text -> align
    is com.gis.supermercados.core.reporting.model.Cell.Money -> align
    is com.gis.supermercados.core.reporting.model.Cell.Count -> align
    is com.gis.supermercados.core.reporting.model.Cell.Decimal -> align
    is com.gis.supermercados.core.reporting.model.Cell.DateCell -> align
    com.gis.supermercados.core.reporting.model.Cell.Empty -> CellAlign.START
}
