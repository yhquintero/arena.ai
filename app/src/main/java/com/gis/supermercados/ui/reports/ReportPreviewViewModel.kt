package com.gis.supermercados.ui.reports

import androidx.lifecycle.ViewModel
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.reporting.ReportPreviewStore
import com.gis.supermercados.core.reporting.model.ReportDocument
import com.gis.supermercados.domain.model.ExportedFile
import com.gis.supermercados.domain.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportPreviewUiState(
    val document: ReportDocument? = null,
    val exported: ExportedFile? = null,
    val isExporting: Boolean = false,
    val error: UiText? = null,
    val message: UiText? = null,
)

/** Previsualizacion del informe generado y acceso directo a su exportacion. */
@HiltViewModel
class ReportPreviewViewModel @Inject constructor(
    private val previewStore: ReportPreviewStore,
    private val reportRepository: ReportRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ReportPreviewUiState(
            document = previewStore.document.value,
            exported = previewStore.exportedFile.value
        )
    )
    val uiState: StateFlow<ReportPreviewUiState> = _uiState.asStateFlow()

    /** Copia el archivo ya exportado al destino elegido por el usuario (SAF). */
    fun copyTo(destinationUri: String) {
        val file = _uiState.value.exported ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null) }
            when (val result = reportRepository.copyToUri(file, destinationUri)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isExporting = false, message = UiText.of(com.gis.supermercados.R.string.reports_copied))
                }

                is AppResult.Failure -> _uiState.update { state -> state.copy(isExporting = false, error = result.error) }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    override fun onCleared() {
        previewStore.clear()
        super.onCleared()
    }
}
