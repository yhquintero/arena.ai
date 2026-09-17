package com.gis.supermercados.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.domain.model.DashboardStats
import com.gis.supermercados.domain.model.StockLevel
import com.gis.supermercados.domain.repository.InventoryRepository
import com.gis.supermercados.domain.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val stats: DashboardStats = DashboardStats(),
    val lowStock: List<StockLevel> = emptyList(),
    val error: UiText? = null,
    val todayLabel: String = "",
)

/** Datos del panel de inicio: KPIs del dia/mes, tendencia y alertas. */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val inventoryRepository: InventoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(todayLabel = AppDateTime.formatDate(System.currentTimeMillis())))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
        observeAlerts()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = reportRepository.dashboardStats()
            when (result) {
                is com.gis.supermercados.core.common.AppResult.Success ->
                    _uiState.update { it.copy(isLoading = false, stats = result.data) }

                is com.gis.supermercados.core.common.AppResult.Failure -> {
                    AppLogger.e(TAG, "No se pudieron cargar las metricas del panel")
                    _uiState.update { it.copy(isLoading = false, error = result.error) }
                }
            }
        }
    }

    /** Las alertas de inventario se actualizan solas (Room Flow). */
    private fun observeAlerts() {
        viewModelScope.launch {
            inventoryRepository.observeLowStock(LOW_STOCK_PREVIEW).collect { levels ->
                _uiState.update { it.copy(lowStock = levels) }
            }
        }
    }

    private companion object {
        const val TAG = "DashboardViewModel"
        const val LOW_STOCK_PREVIEW = 6
    }
}
