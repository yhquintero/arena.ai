package com.gis.supermercados.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.domain.model.BackupInfo
import com.gis.supermercados.domain.repository.BackupRepository
import com.gis.supermercados.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val backups: List<BackupInfo> = emptyList(),
    val autoEnabled: Boolean = true,
    val autoHours: Int = 24,
    val encrypted: Boolean = true,
    val passphrase: String = "",
    val isWorking: Boolean = false,
    val pendingRestore: BackupInfo? = null,
    val pendingDelete: BackupInfo? = null,
    val error: UiText? = null,
    val message: UiText? = null,
)

/** Copias de seguridad locales: creacion, restauracion, exportacion e importacion. */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState

    init {
        refresh()
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            _uiState.update {
                it.copy(autoEnabled = settings.autoBackupEnabled, autoHours = settings.autoBackupHours, encrypted = settings.backupEncrypted)
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(backups = backupRepository.listBackups()) }
    }

    fun onPassphraseChange(value: String) = _uiState.update { it.copy(passphrase = value) }

    fun createBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, error = null) }
            val passphrase = _uiState.value.passphrase.trim().takeIf { it.isNotEmpty() }
            when (val result = backupRepository.createBackup(passphrase)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(isWorking = false, backups = backupRepository.listBackups(), message = UiText.of(R.string.backup_created))
                    }
                }

                is AppResult.Failure -> _uiState.update { state -> state.copy(isWorking = false, error = result.error) }
            }
        }
    }

    fun requestRestore(backup: BackupInfo) = _uiState.update { it.copy(pendingRestore = backup) }

    fun cancelRestore() = _uiState.update { it.copy(pendingRestore = null) }

    /** Restaura la copia y reinicia la aplicacion (obligatorio para consistency de la base). */
    fun confirmRestore() {
        val backup = _uiState.value.pendingRestore ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, error = null) }
            val passphrase = _uiState.value.passphrase.trim().takeIf { it.isNotEmpty() }
            when (val result = backupRepository.restoreBackup(backup.filePath, passphrase)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isWorking = false, pendingRestore = null) }
                    backupRepository.restartApp()
                }

                is AppResult.Failure -> _uiState.update { state -> state.copy(isWorking = false, pendingRestore = null, error = result.error) }
            }
        }
    }

    fun requestDelete(backup: BackupInfo) = _uiState.update { it.copy(pendingDelete = backup) }

    fun cancelDelete() = _uiState.update { it.copy(pendingDelete = null) }

    fun confirmDelete() {
        val backup = _uiState.value.pendingDelete ?: return
        viewModelScope.launch {
            when (val result = backupRepository.deleteBackup(backup.fileName)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(pendingDelete = null, backups = backupRepository.listBackups(), message = UiText.of(R.string.backup_deleted))
                }

                is AppResult.Failure -> _uiState.update { state -> state.copy(pendingDelete = null, error = result.error) }
            }
        }
    }

    fun exportBackup(backup: BackupInfo, destinationUri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, error = null) }
            when (val result = backupRepository.exportBackup(backup.fileName, destinationUri)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isWorking = false, message = UiText.of(R.string.backup_exported))
                }

                is AppResult.Failure -> _uiState.update { state -> state.copy(isWorking = false, error = result.error) }
            }
        }
    }

    fun importBackup(sourceUri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, error = null) }
            when (val result = backupRepository.importBackup(sourceUri)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isWorking = false,
                        backups = backupRepository.listBackups(),
                        message = UiText.of(R.string.backup_imported)
                    )
                }

                is AppResult.Failure -> _uiState.update { state -> state.copy(isWorking = false, error = result.error) }
            }
        }
    }

    fun setAutoBackup(enabled: Boolean, hours: Int) {
        _uiState.update { it.copy(autoEnabled = enabled, autoHours = hours) }
        backupRepository.configureAutoBackup(enabled, hours)
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            settingsRepository.updateSettings(settings.copy(autoBackupEnabled = enabled, autoBackupHours = hours))
        }
    }

    fun runBackupNow() {
        backupRepository.runBackupNow()
        _uiState.update { it.copy(message = UiText.of(R.string.backup_scheduled_now)) }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }
}
