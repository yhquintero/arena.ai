package com.gis.supermercados.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppConstants
import com.gis.supermercados.core.designsystem.GisColors
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.data.local.seed.DatabaseSeeder
import com.gis.supermercados.ui.common.asString

/**
 * Pantalla de acceso. En el primer arranque se convierte en el asistente de alta
 * del usuario administrador (sin el, la aplicacion no puede usarse).
 */
@Composable
fun LoginScreen(
    isFirstRun: Boolean = false,
    businessName: String = "",
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    var passwordVisible by remember { mutableStateOf(false) }

    val firstRun = state.isFirstRun || isFirstRun

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(76.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            if (businessName.isNotBlank()) {
                Text(
                    text = businessName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    if (firstRun) R.string.login_first_run_subtitle else R.string.login_subtitle
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            Card(
                Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (firstRun) {
                        OutlinedTextField(
                            value = state.fullName,
                            onValueChange = viewModel::onFullNameChange,
                            label = { Text(stringResource(R.string.login_full_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )
                    }

                    OutlinedTextField(
                        value = state.username,
                        onValueChange = viewModel::onUsernameChange,
                        label = { Text(stringResource(R.string.login_username)) },
                        singleLine = true,
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        supportingText = if (firstRun) {
                            { Text(stringResource(R.string.login_username_hint)) }
                        } else {
                            null
                        }
                    )

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = { Text(stringResource(R.string.login_password)) },
                        singleLine = true,
                        enabled = !state.isLoading,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = stringResource(R.string.login_toggle_password)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (firstRun) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboard?.hide()
                                if (!firstRun) viewModel.login()
                            }
                        ),
                        supportingText = if (firstRun) {
                            {
                                Text(
                                    stringResource(
                                        R.string.login_password_hint,
                                        AppConstants.PASSWORD_MIN_LENGTH
                                    )
                                )
                            }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (firstRun) {
                        OutlinedTextField(
                            value = state.confirmPassword,
                            onValueChange = viewModel::onConfirmPasswordChange,
                            label = { Text(stringResource(R.string.login_confirm_password)) },
                            singleLine = true,
                            enabled = !state.isLoading,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboard?.hide()
                                    viewModel.createAdministrator()
                                }
                            ),
                            isError = state.confirmPassword.isNotEmpty() &&
                                state.confirmPassword != state.password,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = state.seedSampleData,
                                onValueChange = viewModel::onSeedSampleDataChange,
                                enabled = !state.isLoading
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    stringResource(R.string.login_seed_sample_data),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    stringResource(R.string.login_seed_sample_data_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    state.error?.let { error ->
                        Text(
                            text = error.asString(),
                            color = GisColors.negative,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    state.info?.let { info ->
                        Text(
                            text = info.asString(),
                            color = GisColors.positive,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            keyboard?.hide()
                            if (firstRun) viewModel.createAdministrator() else viewModel.login()
                        },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            stringResource(
                                if (firstRun) R.string.login_create_admin else R.string.login_button
                            ),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!firstRun && state.biometricAvailable) {
                        OutlinedButton(
                            onClick = {
                                val activity = context as? FragmentActivity
                                if (activity == null) {
                                    AppLogger.w(TAG, "Biometria no disponible en este contexto")
                                } else {
                                    viewModel.requestBiometricUnlock(activity)
                                }
                            },
                            enabled = !state.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Fingerprint, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.login_biometric_button))
                        }
                    }
                }
            }

            if (!firstRun) {
                Spacer(Modifier.height(16.dp))
                DemoCredentialsCard(state.biometricUser)
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Lock,
                    null,
                    Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.login_offline_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Recordatorio de las credenciales de la cuenta de demostracion. */
@Composable
private fun DemoCredentialsCard(lastUser: String?) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                stringResource(R.string.login_demo_title),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.login_demo_body, lastUser ?: DatabaseSeeder.DEMO_ADMIN),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private const val TAG = "LoginScreen"
