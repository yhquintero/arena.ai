package com.gis.supermercados.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gis.supermercados.core.common.AppDateTime
import com.gis.supermercados.domain.model.Sale
import com.gis.supermercados.domain.model.SaleFilter
import com.gis.supermercados.domain.model.SaleStatus
import com.gis.supermercados.domain.model.Store
import com.gis.supermercados.domain.repository.SalesRepository
import com.gis.supermercados.domain.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Presets de periodo del historial de ventas. */
enum class SalesPeriod {
    HOY,
    SEMANA,
    MES,
    TODO;

    fun range(now: Long = System.currentTimeMillis()): Pair<Long?, Long?> = when (this) {
        HOY -> AppDateTime.startOfDay(now) to AppDateTime.endOfDay(now)
        SEMANA -> AppDateTime.startOfWeek(now) to AppDateTime.endOfWeek(now)
        MES -> AppDateTime.startOfMonth(now) to AppDateTime.endOfMonth(now)
        TODO -> null to null
    }
}

data class SalesUiState(
    val stores: List<Store> = emptyList(),
    val storeId: Long? = null,
    val status: SaleStatus? = null,
    val period: SalesPeriod = SalesPeriod.MES,
    val query: String = "",
)

/** Historial de ventas con filtros combinables. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SalesViewModel @Inject constructor(
    salesRepository: SalesRepository,
    storeRepository: StoreRepository,
) : ViewModel() {

    private val filters = MutableStateFlow(SalesUiState())

    val uiState: StateFlow<SalesUiState> = filters

    val sales: StateFlow<List<Sale>> = filters
        .flatMapLatest { state ->
            val (from, to) = state.period.range()
            salesRepository.observeSales(
                SaleFilter(
                    storeId = state.storeId,
                    from = from,
                    to = to,
                    status = state.status,
                    query = state.query.trim().takeIf { it.isNotEmpty() },
                    limit = MAX_ROWS
                )
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val totals: StateFlow<SalesTotalsView> = sales.map { list ->
        val completed = list.filter { it.status != SaleStatus.ANULADA }
        SalesTotalsView(
            tickets = completed.size,
            totalCents = completed.sumOf { it.totalCents },
            profitCents = completed.sumOf { it.profitCents },
            returnedCents = list.filter { it.isReturned }.sumOf { it.totalCents },
            averageTicketCents = if (completed.isEmpty()) 0L else completed.sumOf { it.totalCents } / completed.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SalesTotalsView())

    val stores: StateFlow<List<Store>> = storeRepository.observeActiveStores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun onStoreSelected(storeId: Long?) {
        filters.value = filters.value.copy(storeId = storeId)
    }

    fun onStatusSelected(status: SaleStatus?) {
        filters.value = filters.value.copy(status = status)
    }

    fun onPeriodSelected(period: SalesPeriod) {
        filters.value = filters.value.copy(period = period)
    }

    fun onQueryChange(query: String) {
        filters.value = filters.value.copy(query = query)
    }

    private companion object {
        const val MAX_ROWS = 500
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Totales resumidos del listado actual. */
data class SalesTotalsView(
    val tickets: Int = 0,
    val totalCents: Long = 0L,
    val profitCents: Long = 0L,
    val returnedCents: Long = 0L,
    val averageTicketCents: Long = 0L,
)
