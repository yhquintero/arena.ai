package com.gis.supermercados.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.domain.model.AuditEntry
import com.gis.supermercados.domain.model.LogLevel
import com.gis.supermercados.domain.repository.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogsUiState(
    val level: LogLevel? = null,
    val query: String = "",
    val isWorking: Boolean = false,
    val exportedPath: String? = null,
    val error: UiText? = null,
    val message: UiText? = null,
)

/** Log interno de la aplicacion para diagnostico en dispositivo. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LogsViewModel @Inject constructor(
    private val logRepository: LogRepository,
) : ViewModel() {

    private data class Filters(val level: LogLevel?, val query: String?)

    private val filters = MutableStateFlow(Filters(null, null))
    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState

    val logs: StateFlow<List<AuditEntry>> = filters
        .flatMapLatest { current -> logRepository.observeLogs(current.level, current.query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun onLevelChange(level: LogLevel?) {
        filters.value = filters.value.copy(level = level)
        _uiState.update { it.copy(level = level) }
    }

    fun onQueryChange(query: String) {
        filters.value = filters.value.copy(query = query.trim().takeIf { it.isNotEmpty() })
        _uiState.update { it.copy(query = query) }
    }

    /** Exporta el log a un archivo de texto dentro del almacenamiento privado de la app. */
    fun exportLogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, error = null) }
            when (val result = logRepository.exportLogsToFile()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isWorking = false, exportedPath = result.data, message = UiText.of(R.string.logs_exported))
                }

                is AppResult.Failure -> _uiState.update { state -> state.copy(isWorking = false, error = result.error) }
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, error = null) }
            when (val result = logRepository.clearLogs()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isWorking = false, message = UiText.of(R.string.logs_cleared, result.data))
                }

                is AppResult.Failure -> _uiState.update { state -> state.copy(isWorking = false, error = result.error) }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
