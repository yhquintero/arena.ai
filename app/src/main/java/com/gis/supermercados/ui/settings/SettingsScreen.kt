package com.gis.supermercados.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gis.supermercados.R
import com.gis.supermercados.core.designsystem.AmountField
import com.gis.supermercados.core.designsystem.GisColors
import com.gis.supermercados.core.designsystem.GisTopBar
import com.gis.supermercados.core.designsystem.SectionCard
import com.gis.supermercados.domain.model.ThemeMode
import com.gis.supermercados.ui.common.asString
import com.gis.supermercados.ui.common.roleLabel
import com.gis.supermercados.ui.common.themeLabel

/** Configuracion: negocio, preferencias, seguridad y utilidades. */
@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null,
    onOpenBackup: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenUsers: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings
    val session = state.session

    Scaffold(
        topBar = { GisTopBar(title = stringResource(R.string.settings_title), onBack = onBack) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard(title = stringResource(R.string.settings_section_business)) {
                    SettingsTextField(
                        label = stringResource(R.string.settings_field_business_name),
                        value = settings.businessName,
                        onValueChange = { value -> viewModel.updateSettings { it.copy(businessName = value) } }
                    )
                    SettingsTextField(
                        label = stringResource(R.string.settings_field_legal_name),
                        value = settings.businessLegalName,
                        onValueChange = { value -> viewModel.updateSettings { it.copy(businessLegalName = value) } }
                    )
                    SettingsTextField(
                        label = stringResource(R.string.settings_field_tax_id),
                        value = settings.businessTaxId,
                        onValueChange = { value -> viewModel.updateSettings { it.copy(businessTaxId = value) } }
                    )
                    SettingsTextField(
                        label = stringResource(R.string.settings_field_address),
                        value = settings.businessAddress,
                        onValueChange = { value -> viewModel.updateSettings { it.copy(businessAddress = value) } }
                    )
                    SettingsTextField(
                        label = stringResource(R.string.settings_field_phone),
                        value = settings.businessPhone,
                        keyboardType = KeyboardType.Phone,
                        onValueChange = { value -> viewModel.updateSettings { it.copy(businessPhone = value) } }
                    )
                }
            }

            item {
                SectionCard(title = stringResource(R.string.settings_section_defaults)) {
                    AmountField(
                        label = stringResource(R.string.settings_field_tax_rate),
                        value = settings.taxPercent.toString(),
                        onValueChange = { value ->
                            val percent = value.replace(',', '.').toDoubleOrNull()
                            if (percent != null) {
                                viewModel.updateSettings { it.copy(defaultTaxRate = percent / 100.0) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = settings.lowStockThreshold.toString(),
                        onValueChange = { value ->
                            value.filter(Char::isDigit).toIntOrNull()?.let { threshold ->
                                viewModel.updateSettings { it.copy(lowStockThreshold = threshold) }
                            }
                        },
                        label = { Text(stringResource(R.string.settings_field_low_stock)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text(stringResource(R.string.settings_field_low_stock_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsTextField(
                            label = stringResource(R.string.settings_field_ticket_prefix),
                            value = settings.ticketPrefix,
                            onValueChange = { value -> viewModel.updateSettings { it.copy(ticketPrefix = value.uppercase()) } },
                            modifier = Modifier.weight(1f)
                        )
                        SettingsTextField(
                            label = stringResource(R.string.settings_field_credit_note_prefix),
                            value = settings.creditNotePrefix,
                            onValueChange = { value -> viewModel.updateSettings { it.copy(creditNotePrefix = value.uppercase()) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    SettingsTextField(
                        label = stringResource(R.string.settings_field_currency),
                        value = settings.currencySymbol,
                        onValueChange = { value -> viewModel.updateSettings { it.copy(currencySymbol = value) } }
                    )
                }
            }

            item {
                SectionCard(title = stringResource(R.string.settings_section_appearance)) {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateSettings { it.copy(themeMode = mode) } }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                themeLabel(mode),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (settings.themeMode == mode) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (settings.themeMode == mode) {
                                Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    SwitchRow(
                        title = stringResource(R.string.settings_field_dynamic_colors),
                        subtitle = stringResource(R.string.settings_field_dynamic_colors_hint),
                        checked = settings.dynamicColors,
                        onCheckedChange = { value -> viewModel.updateSettings { it.copy(dynamicColors = value) } }
                    )
                }
            }

            item {
                SectionCard(title = stringResource(R.string.settings_section_security)) {
                    if (session != null) {
                        Text(
                            stringResource(R.string.settings_current_user, session.fullName, roleLabel(session.role)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    SwitchRow(
                        title = stringResource(R.string.settings_field_require_auth),
                        subtitle = stringResource(R.string.settings_field_require_auth_hint),
                        checked = settings.requireAuthOnStart,
                        onCheckedChange = { value -> viewModel.updateSettings { it.copy(requireAuthOnStart = value) } }
                    )
                    SwitchRow(
                        title = stringResource(R.string.settings_field_biometric),
                        subtitle = stringResource(R.string.settings_field_biometric_hint),
                        checked = settings.biometricEnabled,
                        onCheckedChange = { value ->
                            viewModel.updateSettings { it.copy(biometricEnabled = value) }
                            viewModel.toggleBiometric(value)
                        }
                    )
                    SwitchRow(
                        title = stringResource(R.string.settings_field_auto_backup),
                        subtitle = stringResource(R.string.settings_field_auto_backup_hint, settings.autoBackupHours),
                        checked = settings.autoBackupEnabled,
                        onCheckedChange = { value -> viewModel.updateSettings { it.copy(autoBackupEnabled = value) } }
                    )
                    OutlinedTextField(
                        value = settings.autoBackupHours.toString(),
                        onValueChange = { value ->
                            value.filter(Char::isDigit).toIntOrNull()?.takeIf { it in 1..168 }?.let { hours ->
                                viewModel.updateSettings { it.copy(autoBackupHours = hours) }
                            }
                        },
                        label = { Text(stringResource(R.string.settings_field_backup_hours)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = viewModel::openPasswordDialog) {
                        Icon(Icons.Rounded.Lock, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_change_password))
                    }
                }
            }

            item {
                SectionCard(title = stringResource(R.string.settings_section_tools)) {
                    NavigationRow(
                        icon = Icons.Rounded.Backup,
                        title = stringResource(R.string.settings_open_backup),
                        subtitle = stringResource(R.string.settings_open_backup_hint),
                        onClick = onOpenBackup
                    )
                    NavigationRow(
                        icon = Icons.Rounded.Groups,
                        title = stringResource(R.string.settings_open_users),
                        subtitle = stringResource(R.string.settings_open_users_hint),
                        onClick = onOpenUsers
                    )
                    NavigationRow(
                        icon = Icons.Rounded.ListAlt,
                        title = stringResource(R.string.settings_open_logs),
                        subtitle = stringResource(R.string.settings_open_logs_hint),
                        onClick = onOpenLogs
                    )
                }
            }

            item {
                SectionCard(title = stringResource(R.string.settings_section_about)) {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.settings_about_version, com.gis.supermercados.BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, null, tint = GisColors.muted)
                        Spacer(Modifier.width(0.dp))
                        Text(
                            stringResource(R.string.settings_about_description),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = viewModel::logout,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Rounded.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_logout), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (state.showPasswordDialog) {
        AlertDialog(
            onDismissRequest = viewModel::closePasswordDialog,
            title = { Text(stringResource(R.string.settings_change_password)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.currentPassword,
                        onValueChange = { value -> viewModel.onPasswordFieldChange(PasswordField.CURRENT, value) },
                        label = { Text(stringResource(R.string.settings_field_current_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.newPassword,
                        onValueChange = { value -> viewModel.onPasswordFieldChange(PasswordField.NEW, value) },
                        label = { Text(stringResource(R.string.settings_field_new_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        supportingText = { Text(stringResource(R.string.settings_password_rules)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.confirmPassword,
                        onValueChange = { value -> viewModel.onPasswordFieldChange(PasswordField.CONFIRM, value) },
                        label = { Text(stringResource(R.string.settings_field_confirm_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::submitPassword, enabled = !state.isSaving) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closePasswordDialog) { Text(stringResource(R.string.common_cancel)) }
            }
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
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NavigationRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = GisColors.muted)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = GisColors.muted)
    }
}
