package com.gis.supermercados.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppConstants
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.common.Validators
import com.gis.supermercados.domain.model.AppSettings
import com.gis.supermercados.domain.model.AuthSession
import com.gis.supermercados.domain.repository.AuthRepository
import com.gis.supermercados.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings.Default,
    val session: AuthSession? = null,
    val isSaving: Boolean = false,
    val showPasswordDialog: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val error: UiText? = null,
    val message: UiText? = null,
)

/** Configuracion general, datos del negocio, seguridad y accesos a utilidades. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            authRepository.session.collect { session ->
                _uiState.update { it.copy(session = session) }
            }
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_uiState.value.settings)
        _uiState.update { it.copy(settings = updated) }
        viewModelScope.launch {
            val result = settingsRepository.updateSettings(updated)
            _uiState.update {
                it.copy(
                    error = (result as? AppResult.Failure)?.error,
                    message = if (result.isSuccess) UiText.of(R.string.settings_saved) else null
                )
            }
        }
    }

    fun openPasswordDialog() = _uiState.update {
        it.copy(showPasswordDialog = true, currentPassword = "", newPassword = "", confirmPassword = "", error = null)
    }

    fun closePasswordDialog() = _uiState.update { it.copy(showPasswordDialog = false) }

    fun onPasswordFieldChange(field: PasswordField, value: String) = _uiState.update {
        when (field) {
            PasswordField.CURRENT -> it.copy(currentPassword = value)
            PasswordField.NEW -> it.copy(newPassword = value)
            PasswordField.CONFIRM -> it.copy(confirmPassword = value)
        }
    }

    /** Cambia la contrasena del usuario en sesion validando longitud y coincidencia. */
    fun submitPassword() {
        val state = _uiState.value
        val userId = state.session?.userId ?: return
        val passwordCheck = Validators.password(state.newPassword)
        if (!passwordCheck.isValid) {
            _uiState.update {
                it.copy(
                    error = passwordCheck.errorText
                        ?: UiText.of(R.string.settings_password_too_short, AppConstants.PASSWORD_MIN_LENGTH)
                )
            }
            return
        }
        val matchCheck = Validators.passwordsMatch(state.newPassword, state.confirmPassword)
        if (!matchCheck.isValid) {
            _uiState.update { it.copy(error = matchCheck.errorText ?: UiText.of(R.string.settings_password_mismatch)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            when (
                val result = authRepository.changePassword(
                    userId = userId,
                    currentPassword = state.currentPassword,
                    newPassword = state.newPassword
                )
            ) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isSaving = false, showPasswordDialog = false, message = UiText.of(R.string.settings_password_changed))
                }

                is AppResult.Failure -> _uiState.update { state2 -> state2.copy(isSaving = false, error = result.error) }
            }
        }
    }

    /** Activa/desactiva el desbloqueo biometrico del usuario actual. */
    fun toggleBiometric(enabled: Boolean) {
        val userId = _uiState.value.session?.userId ?: return
        viewModelScope.launch {
            val result = authRepository.setBiometricEnabled(userId, enabled)
            _uiState.update {
                it.copy(
                    error = (result as? AppResult.Failure)?.error,
                    message = if (result.isSuccess) UiText.of(R.string.settings_biometric_updated) else null
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }
}

/** Campos del dialogo de cambio de contrasena. */
enum class PasswordField { CURRENT, NEW, CONFIRM }
