package com.gis.supermercados.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.gis.supermercados.core.designsystem.EmptyState
import com.gis.supermercados.core.designsystem.GisColors
import com.gis.supermercados.core.designsystem.GisTopBar
import com.gis.supermercados.core.designsystem.SearchField
import com.gis.supermercados.core.designsystem.StatusChip
import com.gis.supermercados.domain.model.LogLevel
import com.gis.supermercados.ui.common.asString
import java.io.File

/** Log interno: diagnostico, exportacion y limpieza. */
@Composable
fun LogsScreen(
    onBack: (() -> Unit)? = null,
    viewModel: LogsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            GisTopBar(
                title = stringResource(R.string.logs_title),
                subtitle = stringResource(R.string.logs_subtitle, logs.size),
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::exportLogs, enabled = !state.isWorking) {
                        Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.logs_export))
                    }
                    IconButton(onClick = viewModel::clearLogs, enabled = !state.isWorking) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.logs_clear))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchField(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    placeholder = stringResource(R.string.logs_search_placeholder)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.level == null,
                            onClick = { viewModel.onLevelChange(null) },
                            label = { Text(stringResource(R.string.logs_level_all)) }
                        )
                    }
                    items(LogLevel.entries.size) { index ->
                        val level = LogLevel.entries[index]
                        FilterChip(
                            selected = state.level == level,
                            onClick = { viewModel.onLevelChange(level) },
                            label = { Text(level.name) }
                        )
                    }
                }
            }

            if (logs.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.logs_empty_title),
                    message = stringResource(R.string.logs_empty_message),
                    icon = Icons.Rounded.Description
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(logs.size, key = { index -> logs[index].id }) { index ->
                        LogRow(level = logs[index].level, tag = logs[index].tag, screen = logs[index].screen, message = logs[index].message, createdAt = logs[index].createdAt)
                    }
                }
            }
        }
    }

    // Compartir el archivo exportado (si existe) usando el FileProvider de la app.
    LaunchedEffect(state.exportedPath) {
        val path = state.exportedPath ?: return@LaunchedEffect
        val file = File(path)
        if (file.exists()) {
            context.startActivity(
                FileSharing.shareIntent(
                    context = context,
                    file = file,
                    fileName = file.name,
                    mimeType = "text/plain",
                    title = context.getString(R.string.logs_share_title)
                )
            )
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
private fun LogRow(level: LogLevel, tag: String, screen: String, message: String, createdAt: Long) {
    val color = when (level) {
        LogLevel.DEBUG -> GisColors.muted
        LogLevel.INFO -> MaterialTheme.colorScheme.primary
        LogLevel.WARN -> GisColors.warning
        LogLevel.ERROR -> GisColors.negative
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(text = level.name, containerColor = color)
                Text(
                    AppDateTime.formatDateTime(createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = GisColors.muted,
                    modifier = Modifier.padding(start = 8.dp)
                )
                if (screen.isNotBlank()) {
                    Text(
                        " · $screen",
                        style = MaterialTheme.typography.labelSmall,
                        color = GisColors.muted
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            if (tag.isNotBlank()) {
                Text(tag, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Text(message, style = MaterialTheme.typography.bodySmall)
        }
    }
}
