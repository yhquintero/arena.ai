package com.gis.supermercados.ui.auth

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gis.supermercados.R
import dagger.hilt.android.qualifiers.ApplicationContext
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.common.Validators
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.core.security.BiometricHelper
import com.gis.supermercados.data.local.seed.DatabaseSeeder
import com.gis.supermercados.domain.model.Role
import com.gis.supermercados.domain.model.UserCredentials
import com.gis.supermercados.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado del formulario de acceso / alta inicial. */
data class LoginUiState(
    val isFirstRun: Boolean = false,
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullName: String = "",
    val seedSampleData: Boolean = true,
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val info: UiText? = null,
    val biometricAvailable: Boolean = false,
    val biometricUser: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val biometricHelper: BiometricHelper,
    private val seeder: DatabaseSeeder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val hasUsers = authRepository.hasAnyUser()
            val biometricUser = authRepository.biometricUsername()
            _uiState.update {
                it.copy(
                    isFirstRun = !hasUsers,
                    biometricAvailable = authRepository.canUseBiometrics() && biometricUser != null,
                    biometricUser = biometricUser,
                    username = biometricUser.orEmpty()
                )
            }
        }
    }

    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value, error = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun onConfirmPasswordChange(value: String) = _uiState.update { it.copy(confirmPassword = value, error = null) }

    fun onFullNameChange(value: String) = _uiState.update { it.copy(fullName = value, error = null) }

    fun onSeedSampleDataChange(value: Boolean) = _uiState.update { it.copy(seedSampleData = value) }

    fun dismissError() = _uiState.update { it.copy(error = null, info = null) }

    /** Acceso con usuario y contrasena. */
    fun login() {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isEmpty()) {
            _uiState.update { it.copy(error = UiText.of(R.string.login_error_empty_fields)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.login(state.username, state.password)
            _uiState.update {
                it.copy(isLoading = false, error = (result as? AppResult.Failure)?.error)
            }
        }
    }

    /** Primer arranque: crea el usuario administrador y entra directamente. */
    fun createAdministrator() {
        val state = _uiState.value
        val usernameCheck = Validators.username(state.username)
        if (!usernameCheck.isValid) {
            _uiState.update { it.copy(error = usernameCheck.errorText) }
            return
        }
        val passwordCheck = Validators.password(state.password)
        if (!passwordCheck.isValid) {
            _uiState.update { it.copy(error = passwordCheck.errorText) }
            return
        }
        val matchCheck = Validators.passwordsMatch(state.password, state.confirmPassword)
        if (!matchCheck.isValid) {
            _uiState.update { it.copy(error = matchCheck.errorText) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            if (state.seedSampleData) {
                runCatching { seeder.seedIfEmpty() }
                    .onSuccess { AppLogger.i(TAG, "Datos de ejemplo solicitados en el primer arranque") }
                    .onFailure { AppLogger.e(TAG, "Fallo al cargar datos de ejemplo", it) }
            }

            val created = authRepository.createUser(
                UserCredentials(
                    username = state.username.trim(),
                    password = state.password,
                    fullName = state.fullName.trim(),
                    role = Role.ADMIN,
                    storeId = null
                ),
                state.password
            )
            when (created) {
                is AppResult.Success -> {
                    val login = authRepository.login(state.username.trim(), state.password)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = (login as? AppResult.Failure)?.error,
                            info = if (login.isSuccess) UiText.of(R.string.login_admin_created) else null
                        )
                    }
                }

                is AppResult.Failure ->
                    _uiState.update { it.copy(isLoading = false, error = created.error) }
            }
        }
    }

    /** Desbloqueo con huella/PIN del dispositivo (sin volver a teclear la contrasena). */
    fun unlockWithBiometrics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepository.unlockWithBiometrics()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = (result as? AppResult.Failure)?.error
                )
            }
        }
    }

    fun canUseBiometrics(): Boolean = biometricHelper.canAuthenticate()

    /**
     * Lanza el prompt biometrico del sistema y, si tiene exito, reabre la sesion
     * del ultimo usuario sin pedir contrasena.
     */
    fun requestBiometricUnlock(activity: FragmentActivity) {
        biometricHelper.authenticate(
            activity = activity,
            title = context.getString(R.string.login_biometric_title),
            subtitle = context.getString(R.string.login_biometric_subtitle),
            description = context.getString(R.string.login_biometric_description),
            onResult = { success, error ->
                if (success) {
                    unlockWithBiometrics()
                } else if (error != null) {
                    AppLogger.w(TAG, "Biometria no completada: $error")
                }
            }
        )
    }

    private companion object {
        const val TAG = "LoginViewModel"
    }
}
