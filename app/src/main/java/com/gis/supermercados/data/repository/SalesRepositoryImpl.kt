package com.gis.supermercados.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.common.runCatchingApp
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.data.local.GisDatabase
import com.gis.supermercados.data.local.entity.CreditNoteEntity
import com.gis.supermercados.data.local.entity.CreditNoteItemEntity
import com.gis.supermercados.data.local.entity.InventoryMovementEntity
import com.gis.supermercados.data.local.entity.SaleEntity
import com.gis.supermercados.data.local.entity.SaleItemEntity
import com.gis.supermercados.data.local.entity.StockEntity
import com.gis.supermercados.data.mapper.toDomain
import com.gis.supermercados.di.IoDispatcher
import com.gis.supermercados.domain.calculator.CalculatedLine
import com.gis.supermercados.domain.calculator.SaleCalculator
import com.gis.supermercados.domain.model.CheckoutRequest
import com.gis.supermercados.domain.model.CreditNote
import com.gis.supermercados.domain.model.MovementType
import com.gis.supermercados.domain.model.PaymentMethod
import com.gis.supermercados.domain.model.ReturnRequest
import com.gis.supermercados.domain.model.Sale
import com.gis.supermercados.domain.model.SaleDetail
import com.gis.supermercados.domain.model.SaleFilter
import com.gis.supermercados.domain.model.SaleStatus
import com.gis.supermercados.domain.repository.SalesRepository
import com.gis.supermercados.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Punto de venta y devoluciones.
 *
 * Todo lo que toca dinero y existencias ocurre EN UNA TRANSACCION: si algo
 * falla (stock insuficiente, tienda cerrada, descuento no permitido) se revierte
 * por completo y la base de datos nunca queda a medias.
 */
@Singleton
class SalesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GisDatabase,
    private val settingsRepository: SettingsRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SalesRepository {

    private val saleDao get() = database.saleDao()
    private val creditNoteDao get() = database.creditNoteDao()
    private val stockDao get() = database.stockDao()
    private val movementDao get() = database.inventoryMovementDao()
    private val productDao get() = database.productDao()
    private val storeDao get() = database.storeDao()
    private val customerDao get() = database.customerDao()
    private val userDao get() = database.userDao()
    private val reportQueries get() = database.reportQueriesDao()

    override fun observeSales(filter: SaleFilter): Flow<List<Sale>> =
        saleDao.observeSales(
            storeId = filter.storeId,
            from = filter.from,
            to = filter.to,
            status = filter.status?.name,
            query = filter.query?.trim()?.takeIf { it.isNotEmpty() },
            limit = filter.limit
        ).map { rows -> rows.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun getSaleDetail(saleId: Long): SaleDetail? = withContext(ioDispatcher) {
        val row = saleDao.getWithItems(saleId) ?: return@withContext null
        val entity = row.sale
        val sale = entity.toDomain(
            storeName = storeDao.getById(entity.storeId)?.name.orEmpty(),
            customerName = entity.customerId?.let { customerDao.getById(it)?.fullName }.orEmpty(),
            userName = userDao.getById(entity.userId)?.fullName.orEmpty()
        )
        val notes = creditNoteDao.observeBySale(saleId).first().map { note ->
            note.toDomain(ticketNumber = entity.ticketNumber, storeName = sale.storeName)
        }
        SaleDetail(sale = sale, items = row.items.map { it.toDomain() }, creditNotes = notes)
    }

    override suspend fun checkout(request: CheckoutRequest): AppResult<Sale> =
        runCatchingApp(TAG, UiText.of(R.string.sale_error_checkout)) {
            withContext(ioDispatcher) {
                if (request.lines.isEmpty()) {
                    throw IllegalArgumentException(context.getString(R.string.sale_error_empty_cart))
                }
                val store = storeDao.getById(request.storeId)
                    ?: throw IllegalArgumentException(context.getString(R.string.sale_error_store_not_found))
                if (!store.isActive) {
                    throw IllegalArgumentException(context.getString(R.string.sale_error_store_inactive))
                }
                if (!store.allowSales) {
                    throw IllegalArgumentException(context.getString(R.string.sale_error_store_no_sales))
                }
                if (request.globalDiscountPercent > store.maxDiscountPercent) {
                    throw IllegalArgumentException(
                        context.getString(
                            R.string.sale_error_discount_exceeds_limit,
                            store.maxDiscountPercent
                        )
                    )
                }
                val settings = settingsRepository.getSettings()
                val calculation = SaleCalculator.calculate(
                    lines = request.lines,
                    globalDiscountPercent = request.globalDiscountPercent.toDouble()
                )
                if (request.paymentMethod == PaymentMethod.EFECTIVO &&
                    request.cashReceivedCents > 0L &&
                    request.cashReceivedCents < calculation.totalCents
                ) {
                    throw IllegalArgumentException(context.getString(R.string.sale_error_insufficient_cash))
                }

                val now = System.currentTimeMillis()
                val change = if (request.cashReceivedCents > 0L) {
                    SaleCalculator.change(calculation.totalCents, request.cashReceivedCents)
                } else {
                    0L
                }

                database.withTransaction {
                    val ticketNumber = "${settings.ticketPrefix}-${formatSerial(saleDao.maxId() + 1)}"

                    // 1) Se comprueba existencias antes de escribir nada.
                    calculation.lines.forEach { line -> validateStock(line, request.storeId) }

                    // 2) Cabecera de la venta.
                    val saleEntity = SaleEntity(
                        ticketNumber = ticketNumber,
                        storeId = request.storeId,
                        customerId = request.customerId,
                        userId = request.userId,
                        subtotalCents = calculation.subtotalCents,
                        discountCents = calculation.discountCents,
                        taxCents = calculation.taxCents,
                        totalCents = calculation.totalCents,
                        costCents = calculation.costCents,
                        paymentMethod = request.paymentMethod,
                        paymentReference = request.paymentReference.trim(),
                        cashReceivedCents = request.cashReceivedCents,
                        changeCents = change,
                        status = SaleStatus.COMPLETADA,
                        itemCount = calculation.itemCount,
                        notes = request.notes.trim(),
                        createdAt = now
                    )
                    val saleId = saleDao.insert(saleEntity)

                    // 3) Detalle con "foto" de nombre, SKU, precio y costo.
                    saleDao.insertItems(
                        calculation.lines.map { line ->
                            SaleItemEntity(
                                saleId = saleId,
                                productId = line.productId,
                                productName = line.productName,
                                sku = line.sku,
                                quantity = line.quantity,
                                unitPriceCents = line.unitPriceCents,
                                unitCostCents = line.unitCostCents,
                                discountPercent = line.discountPercent,
                                taxRate = line.taxRate,
                                lineTotalCents = line.totalCents
                            )
                        }
                    )

                    // 4) Descuento de existencias + movimiento de salida por venta.
                    calculation.lines.forEach { line ->
                        if (line.requiresStock) deductStock(line, request, saleId, now)
                    }

                    // 5) Cliente: credito y puntos de fidelidad.
                    request.customerId?.let { customerId ->
                        if (request.paymentMethod == PaymentMethod.CREDITO_CLIENTE) {
                            customerDao.adjustBalance(customerId, calculation.totalCents)
                        }
                        customerDao.addLoyaltyPoints(
                            customerId,
                            SaleCalculator.loyaltyPointsFor(calculation.totalCents)
                        )
                    }

                    AppLogger.i(
                        TAG,
                        "Venta $ticketNumber en ${store.code}: ${Money.format(calculation.totalCents)} " +
                            "(${calculation.itemCount} articulos, ${request.paymentMethod})"
                    )
                    saleEntity.copy(id = saleId).toDomain(
                        storeName = store.name,
                        customerName = request.customerId
                            ?.let { customerDao.getById(it)?.fullName }.orEmpty(),
                        userName = userDao.getById(request.userId)?.fullName.orEmpty()
                    )
                }
            }
        }

    override suspend fun createCreditNote(request: ReturnRequest): AppResult<CreditNote> =
        runCatchingApp(TAG, UiText.of(R.string.return_error_create)) {
            withContext(ioDispatcher) {
                if (request.lines.none { it.quantity > 0 }) {
                    throw IllegalArgumentException(context.getString(R.string.return_error_no_lines))
                }
                database.withTransaction {
                    val now = System.currentTimeMillis()
                    val sale = saleDao.getById(request.saleId)
                        ?: throw IllegalArgumentException(context.getString(R.string.return_error_sale_not_found))
                    val store = storeDao.getById(sale.storeId)
                    if (store != null && !store.allowReturns) {
                        throw IllegalArgumentException(context.getString(R.string.return_error_store_no_returns))
                    }
                    if (sale.status == SaleStatus.ANULADA) {
                        throw IllegalArgumentException(context.getString(R.string.return_error_sale_voided))
                    }
                    if (sale.status == SaleStatus.DEVUELTA) {
                        throw IllegalArgumentException(context.getString(R.string.return_error_already_returned))
                    }

                    val originalItems = saleDao.getItems(request.saleId).associateBy { it.id }
                    val noteItems = mutableListOf<CreditNoteItemEntity>()
                    var subtotal = 0L
                    var tax = 0L
                    var itemCount = 0

                    request.lines.filter { it.quantity > 0 }.forEach { line ->
                        val original = originalItems[line.saleItemId]
                            ?: throw IllegalArgumentException(context.getString(R.string.return_error_item_not_found))
                        val returnable = original.quantity - original.returnedQuantity
                        if (line.quantity > returnable) {
                            throw IllegalArgumentException(
                                context.getString(
                                    R.string.return_error_exceeds_quantity,
                                    original.productName,
                                    returnable
                                )
                            )
                        }
                        // El descuento de la linea original se aplica a la devolucion.
                        val netBase = Money.afterDiscount(
                            line.unitPriceCents * line.quantity,
                            original.discountPercent
                        )
                        val lineTax = Money.tax(netBase, line.taxRate)
                        subtotal += netBase
                        tax += lineTax
                        itemCount += line.quantity

                        noteItems += CreditNoteItemEntity(
                            creditNoteId = 0L,
                            saleItemId = original.id,
                            productId = original.productId,
                            productName = original.productName,
                            sku = original.sku,
                            quantity = line.quantity,
                            unitPriceCents = line.unitPriceCents,
                            taxRate = line.taxRate,
                            lineTotalCents = netBase + lineTax
                        )
                        saleDao.addReturnedQuantity(original.id, line.quantity)

                        if (request.restock) restock(original.productId, sale.storeId, line.quantity, now)
                    }

                    val settings = settingsRepository.getSettings()
                    val number = "${settings.creditNotePrefix}-${formatSerial(creditNoteDao.maxId() + 1)}"
                    val noteEntity = CreditNoteEntity(
                        creditNoteNumber = number,
                        saleId = sale.id,
                        storeId = sale.storeId,
                        customerId = sale.customerId,
                        userId = request.userId,
                        reason = request.reason.trim(),
                        subtotalCents = subtotal,
                        taxCents = tax,
                        totalCents = subtotal + tax,
                        restock = request.restock,
                        itemCount = itemCount,
                        createdAt = now
                    )
                    val noteId = creditNoteDao.insert(noteEntity)
                    creditNoteDao.insertItems(noteItems.map { it.copy(creditNoteId = noteId) })

                    // Estado de la venta segun lo devuelto acumulado.
                    val updatedItems = saleDao.getItems(sale.id)
                    val fullyReturned = updatedItems.all { it.returnedQuantity >= it.quantity }
                    saleDao.updateStatus(
                        sale.id,
                        if (fullyReturned) SaleStatus.DEVUELTA else SaleStatus.PARCIALMENTE_DEVUELTA
                    )

                    // Si se pago a credito, se descuenta del saldo del cliente.
                    if (sale.customerId != null && sale.paymentMethod == PaymentMethod.CREDITO_CLIENTE) {
                        customerDao.adjustBalance(sale.customerId, -(subtotal + tax))
                    }

                    AppLogger.i(
                        TAG,
                        "Nota de credito $number sobre ${sale.ticketNumber}: " +
                            "${Money.format(subtotal + tax)} ($itemCount articulos)"
                    )
                    noteEntity.copy(id = noteId).toDomain(
                        ticketNumber = sale.ticketNumber,
                        storeName = store?.name.orEmpty(),
                        customerName = sale.customerId?.let { customerDao.getById(it)?.fullName }.orEmpty(),
                        userName = userDao.getById(request.userId)?.fullName.orEmpty(),
                        items = noteItems.map { it.copy(creditNoteId = noteId).toDomain() }
                    )
                }
            }
        }

    override fun observeCreditNotes(storeId: Long?): Flow<List<CreditNote>> =
        creditNoteDao.observeAll(storeId).map { rows ->
            rows.map { row ->
                row.note.toDomain(items = row.items.map { it.toDomain() })
            }
        }.flowOn(ioDispatcher)

    override suspend fun voidSale(saleId: Long, userId: Long): AppResult<Unit> =
        runCatchingApp(TAG, UiText.of(R.string.sale_error_void)) {
            withContext(ioDispatcher) {
                database.withTransaction {
                    val now = System.currentTimeMillis()
                    val sale = saleDao.getById(saleId)
                        ?: throw IllegalArgumentException(context.getString(R.string.sale_error_not_found))
                    if (sale.status == SaleStatus.ANULADA) {
                        throw IllegalArgumentException(context.getString(R.string.sale_error_already_voided))
                    }
                    saleDao.getItems(saleId).forEach { item ->
                        restock(item.productId, sale.storeId, item.quantity, now)
                    }
                    saleDao.updateStatus(saleId, SaleStatus.ANULADA)
                    if (sale.customerId != null && sale.paymentMethod == PaymentMethod.CREDITO_CLIENTE) {
                        customerDao.adjustBalance(sale.customerId, -sale.totalCents)
                    }
                    AppLogger.w(TAG, "Venta ${sale.ticketNumber} anulada por el usuario $userId")
                    Unit
                }
            }
        }

    override suspend fun nextTicketNumber(prefix: String): String = withContext(ioDispatcher) {
        "$prefix-${formatSerial(saleDao.maxId() + 1)}"
    }

    override suspend fun nextCreditNoteNumber(prefix: String): String = withContext(ioDispatcher) {
        "$prefix-${formatSerial(creditNoteDao.maxId() + 1)}"
    }

    override suspend fun salesCountBetween(start: Long, end: Long): Int =
        withContext(ioDispatcher) { saleDao.countBetween(start, end) }

    override suspend fun totalBetween(start: Long, end: Long, storeId: Long?): Long =
        withContext(ioDispatcher) { reportQueries.salesTotals(start, end, storeId).totalCents }

    // ------------------------------- utilidades -------------------------------

    /** Comprueba que haya existencias suficientes antes de cobrar. */
    private suspend fun validateStock(line: CalculatedLine, storeId: Long) {
        if (!line.requiresStock) return
        val product = productDao.getById(line.productId)
            ?: throw IllegalArgumentException(context.getString(R.string.sale_error_product_not_found))
        val available = stockDao.getQuantity(line.productId, storeId) ?: 0
        if (available < line.quantity) {
            throw IllegalArgumentException(
                context.getString(
                    R.string.sale_error_insufficient_stock,
                    product.name,
                    available,
                    line.quantity
                )
            )
        }
    }

    /** Descuenta existencias y deja el movimiento VENTA auditado. */
    private suspend fun deductStock(
        line: CalculatedLine,
        request: CheckoutRequest,
        saleId: Long,
        now: Long,
    ) {
        stockDao.adjustQuantity(line.productId, request.storeId, -line.quantity, now)
        movementDao.insert(
            InventoryMovementEntity(
                productId = line.productId,
                storeId = request.storeId,
                type = MovementType.VENTA,
                quantity = line.quantity,
                unitCostCents = line.unitCostCents,
                reason = context.getString(R.string.inventory_sale_reason),
                reference = saleId.toString(),
                userId = request.userId,
                createdAt = now
            )
        )
    }

    /** Devuelve existencias a la tienda (devolucion o anulacion). */
    private suspend fun restock(productId: Long, storeId: Long, quantity: Int, now: Long) {
        val product = productDao.getById(productId)
        if (product?.isService == true) return
        val stock = stockDao.get(productId, storeId)
        if (stock == null) {
            stockDao.insert(
                StockEntity(
                    productId = productId,
                    storeId = storeId,
                    quantity = quantity,
                    minStock = DEFAULT_MIN_STOCK,
                    updatedAt = now
                )
            )
        } else {
            stockDao.upsert(stock.copy(quantity = stock.quantity + quantity, updatedAt = now))
        }
        movementDao.insert(
            InventoryMovementEntity(
                productId = productId,
                storeId = storeId,
                type = MovementType.DEVOLUCION_CLIENTE,
                quantity = quantity,
                unitCostCents = product?.costCents ?: 0L,
                reason = context.getString(R.string.inventory_return_reason),
                createdAt = now
            )
        )
    }

    private fun formatSerial(value: Long): String = value.toString().padStart(SERIAL_LENGTH, '0')

    private companion object {
        const val TAG = "SalesRepository"
        const val SERIAL_LENGTH = 6
        const val DEFAULT_MIN_STOCK = 10
    }
}
