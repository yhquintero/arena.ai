package com.gis.supermercados.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.core.common.FileSharing
import com.gis.supermercados.core.designsystem.EmptyState
import com.gis.supermercados.core.designsystem.GisColors
import com.gis.supermercados.core.designsystem.GisTopBar
import com.gis.supermercados.core.designsystem.SectionCard
import com.gis.supermercados.core.designsystem.StatusChip
import com.gis.supermercados.domain.model.BackupInfo
import com.gis.supermercados.ui.common.asString

/** Copias de seguridad locales cifradas: crear, restaurar, exportar e importar. */
@Composable
fun BackupScreen(
    onBack: (() -> Unit)? = null,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingExport by remember { mutableStateOf<BackupInfo?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importBackup(it.toString()) }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val backup = pendingExport
        if (uri != null && backup != null) viewModel.exportBackup(backup, uri.toString())
        pendingExport = null
    }

    Scaffold(topBar = { GisTopBar(title = stringResource(R.string.backup_title), onBack = onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard(title = stringResource(R.string.backup_section_auto)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.backup_field_auto), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stringResource(R.string.backup_field_auto_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.autoEnabled,
                            onCheckedChange = { value -> viewModel.setAutoBackup(value, state.autoHours) }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.autoHours.toString(),
                        onValueChange = { value ->
                            value.filter(Char::isDigit).toIntOrNull()?.takeIf { it in 1..168 }
                                ?.let { hours -> viewModel.setAutoBackup(state.autoEnabled, hours) }
                        },
                        label = { Text(stringResource(R.string.backup_field_hours)) },
                        singleLine = true,
                        enabled = state.autoEnabled,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = viewModel::runBackupNow, enabled = !state.isWorking, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Backup, null)
                        Text(stringResource(R.string.backup_run_now), Modifier.padding(start = 8.dp))
                    }
                }
            }

            item {
                SectionCard(title = stringResource(R.string.backup_section_manual)) {
                    OutlinedTextField(
                        value = state.passphrase,
                        onValueChange = viewModel::onPassphraseChange,
                        label = { Text(stringResource(R.string.backup_field_passphrase)) },
                        supportingText = { Text(stringResource(R.string.backup_field_passphrase_hint)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = viewModel::createBackup,
                            enabled = !state.isWorking,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.backup_create))
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("*/*")) },
                            enabled = !state.isWorking,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Upload, null)
                            Text(stringResource(R.string.backup_import), Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.backup_section_list, state.backups.size),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.backups.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.backup_empty_title),
                        message = stringResource(R.string.backup_empty_message),
                        icon = Icons.Rounded.Backup
                    )
                }
            } else {
                items(state.backups.size, key = { index -> state.backups[index].fileName }) { index ->
                    BackupRow(
                        backup = state.backups[index],
                        onRestore = { viewModel.requestRestore(state.backups[index]) },
                        onExport = {
                            pendingExport = state.backups[index]
                            exportLauncher.launch(state.backups[index].fileName)
                        },
                        onDelete = { viewModel.requestDelete(state.backups[index]) }
                    )
                }
            }
        }
    }

    state.pendingRestore?.let { backup ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            title = { Text(stringResource(R.string.backup_restore_title)) },
            text = { Text(stringResource(R.string.backup_restore_message, backup.fileName)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRestore, enabled = !state.isWorking) {
                    Text(stringResource(R.string.backup_restore_confirm))
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelRestore) { Text(stringResource(R.string.common_cancel)) } }
        )
    }

    state.pendingDelete?.let { backup ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.backup_delete_title)) },
            text = { Text(stringResource(R.string.backup_delete_message, backup.fileName)) },
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
private fun BackupRow(backup: BackupInfo, onRestore: () -> Unit, onExport: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(backup.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    "${AppDateTime.formatDateTime(backup.createdAt)} · ${FileSharing.readableSize(backup.sizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GisColors.muted
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (backup.encrypted) {
                        StatusChip(text = stringResource(R.string.backup_tag_encrypted), containerColor = GisColors.positive)
                    }
                    if (backup.protectedWithPassphrase) {
                        StatusChip(text = stringResource(R.string.backup_tag_passphrase), containerColor = GisColors.warning)
                    }
                    StatusChip(
                        text = stringResource(R.string.backup_tag_records, backup.records),
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRestore) {
                        Icon(Icons.Rounded.Restore, null)
                        Text(stringResource(R.string.backup_restore_action), Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(onClick = onExport) {
                        Icon(Icons.Rounded.Download, null)
                        Text(stringResource(R.string.backup_export), Modifier.padding(start = 6.dp))
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.common_delete), tint = GisColors.negative)
            }
        }
    }
}
