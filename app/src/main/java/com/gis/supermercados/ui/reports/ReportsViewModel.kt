package com.gis.supermercados.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.reporting.ReportPeriods
import com.gis.supermercados.core.reporting.ReportPreviewStore
import com.gis.supermercados.core.reporting.model.DateRangeModel
import com.gis.supermercados.core.reporting.model.ExportFormat
import com.gis.supermercados.core.reporting.model.PeriodType
import com.gis.supermercados.core.reporting.model.ReportDocument
import com.gis.supermercados.core.reporting.model.ReportRequest
import com.gis.supermercados.core.reporting.model.ReportType
import com.gis.supermercados.domain.model.AppSettings
import com.gis.supermercados.domain.model.ExportedFile
import com.gis.supermercados.domain.model.ReportRecord
import com.gis.supermercados.domain.model.Store
import com.gis.supermercados.domain.repository.AuthRepository
import com.gis.supermercados.domain.repository.LogRepository
import com.gis.supermercados.domain.repository.ReportRepository
import com.gis.supermercados.domain.repository.SettingsRepository
import com.gis.supermercados.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Seleccion de informe, periodo, sucursal y formato. */
data class ReportsUiState(
    val stores: List<Store> = emptyList(),
    val settings: AppSettings = AppSettings.Default,
    val type: ReportType = ReportType.RESUMEN_EJECUTIVO,
    val period: PeriodType = PeriodType.MENSUAL,
    val storeId: Long? = null,
    val customStart: Long = AppDateTime.startOfMonth(System.currentTimeMillis()),
    val customEnd: Long = AppDateTime.endOfMonth(System.currentTimeMillis()),
    val format: ExportFormat = ExportFormat.PDF,
    val range: DateRangeModel = ReportPeriods.rangeFor(PeriodType.MENSUAL),
    val isGenerating: Boolean = false,
    val isExporting: Boolean = false,
    val document: ReportDocument? = null,
    val exported: ExportedFile? = null,
    val error: UiText? = null,
    val message: UiText? = null,
)

/**
 * Centro de reportes: construye el documento, lo previsualiza y lo exporta a PDF, Excel o CSV.
 *
 * Cada exportacion queda registrada en el historial local (trazabilidad de quien genero que informe).
 */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val settingsRepository: SettingsRepository,
    private val storeRepository: StoreRepository,
    private val authRepository: AuthRepository,
    private val logRepository: LogRepository,
    private val previewStore: ReportPreviewStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState

    val history: StateFlow<List<ReportRecord>> = logRepository.observeReportRecords(HISTORY_LIMIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            val stores = storeRepository.getActiveStores()
            val session = authRepository.session.value
            _uiState.update {
                it.copy(
                    settings = settings,
                    stores = stores,
                    storeId = session?.storeId,
                    range = ReportPeriods.rangeFor(it.period, System.currentTimeMillis())
                )
            }
        }
    }

    fun onTypeChange(type: ReportType) {
        _uiState.update { it.copy(type = type, document = null) }
    }

    fun onPeriodChange(period: PeriodType) {
        _uiState.update {
            it.copy(
                period = period,
                document = null,
                range = rangeFor(period, it.customStart, it.customEnd)
            )
        }
    }

    fun onCustomStartChange(millis: Long) = _uiState.update {
        it.copy(customStart = millis, document = null, range = rangeFor(PeriodType.PERSONALIZADO, millis, it.customEnd))
    }

    fun onCustomEndChange(millis: Long) = _uiState.update {
        it.copy(customEnd = millis, document = null, range = rangeFor(PeriodType.PERSONALIZADO, it.customStart, millis))
    }

    fun onStoreChange(storeId: Long?) = _uiState.update { it.copy(storeId = storeId, document = null) }

    fun onFormatChange(format: ExportFormat) = _uiState.update { it.copy(format = format) }

    /** Construye el informe seleccionado (para previsualizar o exportar). */
    fun generate() {
        val request = currentRequest()
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            when (val result = reportRepository.buildReport(request)) {
                is AppResult.Success -> {
                    previewStore.publish(result.data)
                    _uiState.update { it.copy(isGenerating = false, document = result.data) }
                }

                is AppResult.Failure -> _uiState.update { state -> state.copy(isGenerating = false, error = result.error) }
            }
        }
    }

    /** Exporta el informe al formato elegido y registra la operacion en el historial. */
    fun export() {
        val state = _uiState.value
        val request = currentRequest()
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null) }
            val document = state.document ?: reportRepository.buildReport(request).let { built ->
                when (built) {
                    is AppResult.Success -> built.data
                    is AppResult.Failure -> {
                        _uiState.update { it.copy(isExporting = false, error = built.error) }
                        return@launch
                    }
                }
            }

            when (val result = reportRepository.exportReport(document, request, state.format)) {
                is AppResult.Success -> {
                    previewStore.publish(document)
                    previewStore.publishExported(result.data)
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            document = document,
                            exported = result.data,
                            message = UiText.of(R.string.reports_exported)
                        )
                    }
                    registerInHistory(request, document, result.data, state.format)
                }

                is AppResult.Failure -> _uiState.update { state2 -> state2.copy(isExporting = false, error = result.error) }
            }
        }
    }

    /** Copia el archivo exportado a una carpeta elegida por el usuario (SAF). */
    fun copyTo(destinationUri: String) {
        val file = _uiState.value.exported ?: return
        viewModelScope.launch {
            when (val result = reportRepository.copyToUri(file, destinationUri)) {
                is AppResult.Success -> _uiState.update { it.copy(message = UiText.of(R.string.reports_copied)) }
                is AppResult.Failure -> _uiState.update { state -> state.copy(error = result.error) }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun dismissMessage() = _uiState.update { it.copy(message = null, exported = null) }

    private fun registerInHistory(
        request: ReportRequest,
        document: ReportDocument,
        file: ExportedFile,
        format: ExportFormat,
    ) {
        val user = authRepository.session.value
        viewModelScope.launch {
            logRepository.saveReportRecord(
                ReportRecord(
                    reportType = document.type.name,
                    periodType = request.periodType.name,
                    startMillis = request.range.startMillis,
                    endMillis = request.range.endMillis,
                    format = format.name,
                    fileName = file.fileName,
                    filePath = file.filePath,
                    sizeBytes = file.sizeBytes,
                    storeId = request.storeId,
                    userName = user?.username.orEmpty(),
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    /** Solicitud con la informacion de la empresa tomada de la configuracion. */
    private fun currentRequest(): ReportRequest {
        val state = _uiState.value
        val store = state.stores.firstOrNull { it.id == state.storeId }
        return ReportRequest(
            type = state.type,
            periodType = state.period,
            range = state.range,
            storeId = state.storeId,
            storeName = store?.name.orEmpty(),
            companyName = state.settings.businessName,
            companyTaxId = state.settings.businessTaxId,
            companyAddress = state.settings.businessAddress,
            currencySymbol = state.settings.currencySymbol,
            generatedBy = authRepository.session.value?.username.orEmpty()
        )
    }

    private fun rangeFor(period: PeriodType, customStart: Long, customEnd: Long): DateRangeModel =
        ReportPeriods.rangeFor(
            type = period,
            referenceMillis = System.currentTimeMillis(),
            customStartMillis = if (period == PeriodType.PERSONALIZADO) customStart else null,
            customEndMillis = if (period == PeriodType.PERSONALIZADO) customEnd else null
        )

    private companion object {
        const val HISTORY_LIMIT = 100
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
