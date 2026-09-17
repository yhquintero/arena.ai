package com.gis.supermercados.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Relation
import com.gis.supermercados.domain.model.ExpenseCategory
import com.gis.supermercados.domain.model.MovementType
import com.gis.supermercados.domain.model.PaymentMethod
import com.gis.supermercados.domain.model.ProductUnit
import com.gis.supermercados.domain.model.SaleStatus

/*
 * POJOs de proyeccion: resultados de consultas con JOIN y agregados.
 * Room los rellena automaticamente por nombre de columna, evitando
 * cargar entidades completas cuando solo se necesitan unos campos
 * (rendimiento y menos memoria en listados y reportes).
 */

/** Producto + nombre/color de su categoria. */
data class ProductWithCategoryRow(
    @Embedded val product: ProductEntity,
    @ColumnInfo(name = "category_name") val categoryName: String = "",
    @ColumnInfo(name = "category_color") val categoryColor: Long = 0L,
)

/** Existencias de un producto en una tienda, con nombres resueltos. */
data class StockDetailRow(
    @ColumnInfo(name = "stock_id") val stockId: Long = 0L,
    @ColumnInfo(name = "product_id") val productId: Long = 0L,
    @ColumnInfo(name = "store_id") val storeId: Long = 0L,
    @ColumnInfo(name = "product_name") val productName: String = "",
    val sku: String = "",
    @ColumnInfo(name = "category_name") val categoryName: String = "",
    @ColumnInfo(name = "store_name") val storeName: String = "",
    val quantity: Int = 0,
    val reserved: Int = 0,
    @ColumnInfo(name = "min_stock") val minStock: Int = 0,
    @ColumnInfo(name = "max_stock") val maxStock: Int = 0,
    @ColumnInfo(name = "cost_cents") val costCents: Long = 0L,
    @ColumnInfo(name = "price_cents") val priceCents: Long = 0L,
    val unit: ProductUnit = ProductUnit.UNIDAD,
    @ColumnInfo(name = "is_service") val isService: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0L,
)

/** Movimiento de inventario con nombres de producto, tiendas y usuario. */
data class MovementDetailRow(
    @ColumnInfo(name = "movement_id") val movementId: Long = 0L,
    @ColumnInfo(name = "store_id") val storeId: Long = 0L,
    @ColumnInfo(name = "product_id") val productId: Long = 0L,
    @ColumnInfo(name = "product_name") val productName: String = "",
    val sku: String = "",
    @ColumnInfo(name = "store_name") val storeName: String = "",
    @ColumnInfo(name = "destination_store_name") val destinationStoreName: String? = null,
    val type: MovementType = MovementType.ENTRADA,
    val quantity: Int = 0,
    @ColumnInfo(name = "unit_cost_cents") val unitCostCents: Long = 0L,
    val reason: String = "",
    val reference: String = "",
    @ColumnInfo(name = "user_name") val userName: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
)

/** Fila de la lista de ventas (cabecera con nombres resueltos). */
data class SaleListRow(
    @ColumnInfo(name = "sale_id") val saleId: Long = 0L,
    @ColumnInfo(name = "store_id") val storeId: Long = 0L,
    @ColumnInfo(name = "customer_id") val customerId: Long? = null,
    @ColumnInfo(name = "user_id") val userId: Long = 0L,
    @ColumnInfo(name = "ticket_number") val ticketNumber: String = "",
    @ColumnInfo(name = "store_name") val storeName: String = "",
    @ColumnInfo(name = "customer_name") val customerName: String? = null,
    @ColumnInfo(name = "user_name") val userName: String = "",
    @ColumnInfo(name = "subtotal_cents") val subtotalCents: Long = 0L,
    @ColumnInfo(name = "discount_cents") val discountCents: Long = 0L,
    @ColumnInfo(name = "tax_cents") val taxCents: Long = 0L,
    @ColumnInfo(name = "total_cents") val totalCents: Long = 0L,
    @ColumnInfo(name = "cost_cents") val costCents: Long = 0L,
    @ColumnInfo(name = "payment_method") val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    val status: SaleStatus = SaleStatus.COMPLETADA,
    @ColumnInfo(name = "item_count") val itemCount: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = 0L,
)

/** Venta completa con su detalle (relacion 1:N resuelta por Room). */
data class SaleWithItemsRow(
    @Embedded val sale: SaleEntity,
    @Relation(parentColumn = "id", entityColumn = "sale_id")
    val items: List<SaleItemEntity> = emptyList(),
)

/** Nota de credito con su detalle. */
data class CreditNoteWithItemsRow(
    @Embedded val note: CreditNoteEntity,
    @Relation(parentColumn = "id", entityColumn = "credit_note_id")
    val items: List<CreditNoteItemEntity> = emptyList(),
)

/** Gasto con nombre de tienda y usuario. */
data class ExpenseDetailRow(
    @Embedded val expense: ExpenseEntity,
    @ColumnInfo(name = "store_name") val storeName: String? = null,
    @ColumnInfo(name = "user_name") val userName: String = "",
)

/** Cliente con estadisticas de compra agregadas. */
data class CustomerWithStatsRow(
    @Embedded val customer: CustomerEntity,
    @ColumnInfo(name = "purchase_count") val purchaseCount: Int = 0,
    @ColumnInfo(name = "total_purchases_cents") val totalPurchasesCents: Long = 0L,
    @ColumnInfo(name = "last_purchase_at") val lastPurchaseAt: Long = 0L,
)

// ------------------------- Agregados para reportes -------------------------

/** Suma + conteo + costo (resultado generico de agregados de ventas). */
data class MoneyCountRow(
    @ColumnInfo(name = "total_cents") val totalCents: Long = 0L,
    @ColumnInfo(name = "cost_cents") val costCents: Long = 0L,
    @ColumnInfo(name = "cnt") val count: Int = 0,
)

/** Ventas agrupadas por sucursal. */
data class StoreSalesRow(
    @ColumnInfo(name = "store_id") val storeId: Long = 0L,
    @ColumnInfo(name = "store_name") val storeName: String = "",
    @ColumnInfo(name = "tickets") val tickets: Int = 0,
    @ColumnInfo(name = "subtotal_cents") val subtotalCents: Long = 0L,
    @ColumnInfo(name = "discount_cents") val discountCents: Long = 0L,
    @ColumnInfo(name = "tax_cents") val taxCents: Long = 0L,
    @ColumnInfo(name = "total_cents") val totalCents: Long = 0L,
    @ColumnInfo(name = "cost_cents") val costCents: Long = 0L,
)

/** Ventas agrupadas por dia natural (serie temporal). */
data class DayTotalRow(
    @ColumnInfo(name = "day_bucket") val dayBucket: Long = 0L,
    @ColumnInfo(name = "total_cents") val totalCents: Long = 0L,
    @ColumnInfo(name = "cost_cents") val costCents: Long = 0L,
    @ColumnInfo(name = "tickets") val tickets: Int = 0,
)

/** Gastos agrupados por dia natural. */
data class DayExpenseRow(
    @ColumnInfo(name = "day_bucket") val dayBucket: Long = 0L,
    @ColumnInfo(name = "total_cents") val totalCents: Long = 0L,
)

/** Productos mas vendidos del periodo. */
data class TopProductRow(
    @ColumnInfo(name = "product_id") val productId: Long = 0L,
    @ColumnInfo(name = "product_name") val productName: String = "",
    val sku: String = "",
    @ColumnInfo(name = "category_name") val categoryName: String = "",
    @ColumnInfo(name = "units") val units: Int = 0,
    @ColumnInfo(name = "total_cents") val totalCents: Long = 0L,
    @ColumnInfo(name = "cost_cents") val costCents: Long = 0L,
)

/** Rentabilidad agrupada por categoria. */
data class CategoryProfitRow(
    @ColumnInfo(name = "category_id") val categoryId: Long = 0L,
    @ColumnInfo(name = "category_name") val categoryName: String = "",
    @ColumnInfo(name = "units") val units: Int = 0,
    @ColumnInfo(name = "sales_cents") val salesCents: Long = 0L,
    @ColumnInfo(name = "cost_cents") val costCents: Long = 0L,
    val color: Long = 0L,
)

/** Ventas agrupadas por metodo de pago (flujo de caja). */
data class PaymentMethodRow(
    @ColumnInfo(name = "payment_method") val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    @ColumnInfo(name = "total_cents") val totalCents: Long = 0L,
    @ColumnInfo(name = "tickets") val tickets: Int = 0,
)

/** Gastos agrupados por categoria. */
data class ExpenseCategoryRow(
    val category: ExpenseCategory = ExpenseCategory.OTROS,
    @ColumnInfo(name = "total_cents") val totalCents: Long = 0L,
    @ColumnInfo(name = "cnt") val count: Int = 0,
)

/** Gastos agrupados por tienda (null = corporativo). */
data class ExpenseStoreRow(
    @ColumnInfo(name = "store_id") val storeId: Long? = null,
    @ColumnInfo(name = "store_name") val storeName: String? = null,
    @ColumnInfo(name = "total_cents") val totalCents: Long = 0L,
    @ColumnInfo(name = "cnt") val count: Int = 0,
)

/** Valoracion global del inventario. */
data class InventoryValueRow(
    @ColumnInfo(name = "total_quantity") val totalQuantity: Long = 0L,
    @ColumnInfo(name = "cost_value_cents") val costValueCents: Long = 0L,
    @ColumnInfo(name = "retail_value_cents") val retailValueCents: Long = 0L,
    @ColumnInfo(name = "low_stock_count") val lowStockCount: Int = 0,
    @ColumnInfo(name = "out_of_stock_count") val outOfStockCount: Int = 0,
)

/** Stock agregado de un producto en todas las tiendas. */
data class ProductStockRow(
    @ColumnInfo(name = "product_id") val productId: Long = 0L,
    @ColumnInfo(name = "total_quantity") val totalQuantity: Int = 0,
    @ColumnInfo(name = "low_stock_stores") val lowStockStores: Int = 0,
    @ColumnInfo(name = "out_of_stock_stores") val outOfStockStores: Int = 0,
    @ColumnInfo(name = "stock_value_cents") val stockValueCents: Long = 0L,
)

/** Devoluciones agrupadas por tienda o por periodo. */
data class ReturnsRow(
    @ColumnInfo(name = "store_id") val storeId: Long = 0L,
    @ColumnInfo(name = "store_name") val storeName: String = "",
    @ColumnInfo(name = "total_cents") val totalCents: Long = 0L,
    @ColumnInfo(name = "cnt") val count: Int = 0,
)

/** Clientes con mayor gasto en el periodo (valor agregado del modulo de clientes). */
data class CustomerSpendRow(
    @ColumnInfo(name = "customer_id") val customerId: Long = 0L,
    @ColumnInfo(name = "customer_name") val customerName: String = "",
    @ColumnInfo(name = "tickets") val tickets: Int = 0,
    @ColumnInfo(name = "total_cents") val totalCents: Long = 0L,
)
