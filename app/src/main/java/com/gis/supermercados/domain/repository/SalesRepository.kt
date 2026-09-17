package com.gis.supermercados.domain.repository

import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.domain.model.CheckoutRequest
import com.gis.supermercados.domain.model.CreditNote
import com.gis.supermercados.domain.model.ReturnRequest
import com.gis.supermercados.domain.model.Sale
import com.gis.supermercados.domain.model.SaleDetail
import com.gis.supermercados.domain.model.SaleFilter
import kotlinx.coroutines.flow.Flow

/** Punto de venta, historial de transacciones y devoluciones. */
interface SalesRepository {

    fun observeSales(filter: SaleFilter): Flow<List<Sale>>

    suspend fun getSaleDetail(saleId: Long): SaleDetail?

    /**
     * Registra la venta: valida stock, descuenta existencias, guarda cabecera,
     * detalle y movimientos en UNA transaccion (todo o nada).
     */
    suspend fun checkout(request: CheckoutRequest): AppResult<Sale>

    suspend fun createCreditNote(request: ReturnRequest): AppResult<CreditNote>

    fun observeCreditNotes(storeId: Long?): Flow<List<CreditNote>>

    suspend fun voidSale(saleId: Long, userId: Long): AppResult<Unit>

    suspend fun nextTicketNumber(prefix: String): String

    suspend fun nextCreditNoteNumber(prefix: String): String

    suspend fun salesCountBetween(start: Long, end: Long): Int

    suspend fun totalBetween(start: Long, end: Long, storeId: Long?): Long
}
