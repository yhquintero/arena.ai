package com.gis.supermercados.ui.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gis.supermercados.R
import com.gis.supermercados.core.designsystem.EmptyState
import com.gis.supermercados.core.designsystem.GisColors
import com.gis.supermercados.core.designsystem.GisTopBar
import com.gis.supermercados.core.designsystem.StatusChip
import com.gis.supermercados.domain.model.Role
import com.gis.supermercados.ui.common.asString
import com.gis.supermercados.ui.common.roleLabel

/** Usuarios del sistema: roles, sucursal asignada y contrasenas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    onBack: (() -> Unit)? = null,
    viewModel: UsersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            GisTopBar(
                title = stringResource(R.string.users_title),
                subtitle = stringResource(R.string.users_subtitle, users.count { it.isActive }),
                onBack = onBack
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openCreate,
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(stringResource(R.string.users_new)) }
            )
        }
    ) { padding ->
        if (users.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.users_empty_title),
                message = stringResource(R.string.users_empty_message),
                icon = Icons.Rounded.Groups,
                actionLabel = stringResource(R.string.users_new),
                onAction = viewModel::openCreate,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users.size, key = { index -> users[index].id }) { index ->
                    val user = users[index]
                    val isCurrentUser = user.id == state.session?.userId
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(user.fullName.ifBlank { user.username }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1)
                                    if (isCurrentUser) {
                                        Text(
                                            stringResource(R.string.users_tag_you),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(start = 6.dp)
                                        )
                                    }
                                }
                                Text("@${user.username}", style = MaterialTheme.typography.labelSmall, color = GisColors.muted)
                                Text(
                                    user.storeName.ifBlank { stringResource(R.string.users_all_stores) },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusChip(text = roleLabel(user.role), containerColor = MaterialTheme.colorScheme.secondary)
                                    if (user.biometricEnabled) {
                                        StatusChip(text = stringResource(R.string.users_tag_biometric), containerColor = GisColors.positive)
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (!isCurrentUser) {
                                    Switch(checked = user.isActive, onCheckedChange = { viewModel.toggleActive(user) })
                                }
                                IconButton(onClick = { viewModel.openEdit(user) }, enabled = !isCurrentUser) {
                                    Text(stringResource(R.string.common_edit), style = MaterialTheme.typography.labelLarge)
                                }
                                IconButton(onClick = { viewModel.openResetPassword(user) }) {
                                    Icon(Icons.Rounded.Key, contentDescription = stringResource(R.string.users_reset_password), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showForm) {
        UserFormDialog(state = state, viewModel = viewModel)
    }

    state.resetTarget?.let { user ->
        AlertDialog(
            onDismissRequest = viewModel::cancelResetPassword,
            title = { Text(stringResource(R.string.users_reset_password)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.users_reset_password_message, user.fullName.ifBlank { user.username }),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.resetPassword,
                        onValueChange = viewModel::onResetPasswordChange,
                        label = { Text(stringResource(R.string.users_field_new_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        supportingText = { Text(stringResource(R.string.settings_password_rules)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmResetPassword, enabled = !state.isSaving) {
                    Text(stringResource(R.string.common_save))
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelResetPassword) { Text(stringResource(R.string.common_cancel)) } }
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
private fun UserFormDialog(state: UsersUiState, viewModel: UsersViewModel) {
    val form = state.form
    val isNew = form.id == 0L
    var roleMenu by remember { mutableStateOf(false) }
    var storeMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = viewModel::closeForm,
        title = { Text(stringResource(if (isNew) R.string.users_new else R.string.users_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = form.username,
                    onValueChange = { value -> viewModel.updateForm { it.copy(username = value) } },
                    label = { Text(stringResource(R.string.users_field_username)) },
                    singleLine = true,
                    enabled = isNew,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = form.fullName,
                    onValueChange = { value -> viewModel.updateForm { it.copy(fullName = value) } },
                    label = { Text(stringResource(R.string.users_field_full_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isNew) {
                    OutlinedTextField(
                        value = form.password,
                        onValueChange = { value -> viewModel.updateForm { it.copy(password = value) } },
                        label = { Text(stringResource(R.string.users_field_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = form.confirmPassword,
                        onValueChange = { value -> viewModel.updateForm { it.copy(confirmPassword = value) } },
                        label = { Text(stringResource(R.string.users_field_confirm_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(Modifier.fillMaxWidth()) {
                    FilledTonalButton(onClick = { roleMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(roleLabel(form.role), Modifier.weight(1f))
                        Icon(Icons.Rounded.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = roleMenu, onDismissRequest = { roleMenu = false }) {
                        Role.entries.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(roleLabel(role)) },
                                onClick = {
                                    viewModel.updateForm { it.copy(role = role) }
                                    roleMenu = false
                                }
                            )
                        }
                    }
                }
                Box(Modifier.fillMaxWidth()) {
                    FilledTonalButton(onClick = { storeMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            state.stores.firstOrNull { it.id == form.storeId }?.name
                                ?: stringResource(R.string.users_all_stores),
                            Modifier.weight(1f),
                            maxLines = 1
                        )
                        Icon(Icons.Rounded.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = storeMenu, onDismissRequest = { storeMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.users_all_stores)) },
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
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::save, enabled = !state.isSaving) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = viewModel::closeForm) { Text(stringResource(R.string.common_cancel)) } }
    )
}
