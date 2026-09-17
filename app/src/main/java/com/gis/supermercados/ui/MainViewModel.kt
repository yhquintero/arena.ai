package com.gis.supermercados.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.domain.model.AppSettings
import com.gis.supermercados.domain.model.AuthSession
import com.gis.supermercados.domain.repository.AuthRepository
import com.gis.supermercados.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado global de la aplicacion: ajustes (tema, moneda), sesion activa y
 * decision de que flujo mostrar al arrancar (alta inicial, login o app principal).
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** null = aun no se sabe; false = no hay usuarios (primer arranque). */
    private val _hasUsers = MutableStateFlow<Boolean?>(null)
    val hasUsers: StateFlow<Boolean?> = _hasUsers.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings.Default)

    val session: StateFlow<AuthSession?> = authRepository.session

    init {
        viewModelScope.launch {
            runCatching {
                val firstRun = !settingsRepository.isFirstRunCompleted()
                _hasUsers.value = authRepository.hasAnyUser()
                if (firstRun) settingsRepository.markFirstRunCompleted()
                if (_hasUsers.value == false) {
                    AppLogger.i(TAG, "Primer arranque: no hay usuarios, se pedira crear el administrador")
                }
            }.onFailure {
                AppLogger.e(TAG, "No se pudo comprobar el estado inicial", it)
                _hasUsers.value = true
            }
            _isLoading.value = false
        }
    }

    /** La sesion caduca por inactividad: se revisa al volver a primer plano. */
    fun onActivityResumed() {
        authRepository.touchSession()
        if (authRepository.isLoggedIn && authRepository.isSessionExpired()) {
            AppLogger.w(TAG, "Sesion expirada por inactividad: se cierra automaticamente")
            viewModelScope.launch { authRepository.logout() }
        }
    }

    fun onUserInteraction() = authRepository.touchSession()

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
