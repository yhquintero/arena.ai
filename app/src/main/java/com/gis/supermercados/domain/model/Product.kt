package com.gis.supermercados.domain.model

/** Categoria de producto (base de la rentabilidad por categoria). */
data class Category(
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    /** Color ARGB usado en graficos y etiquetas. */
    val color: Long = DEFAULT_COLOR,
    val targetMarginPercent: Int = 25,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
) {
    companion object {
        const val DEFAULT_COLOR: Long = 0xFF1B6EF3L
    }
}

/**
 * Producto o servicio del catalogo.
 * Los precios son CENTAVOS (ver [com.gis.supermercados.core.common.Money]).
 */
data class Product(
    val id: Long = 0L,
    val name: String,
    val sku: String,
    val barcode: String = "",
    val categoryId: Long,
    val categoryName: String = "",
    val costCents: Long = 0L,
    val priceCents: Long = 0L,
    val taxRate: Double = 0.0,
    val unit: ProductUnit = ProductUnit.UNIDAD,
    /** Un supermercado mixto vende tambien servicios (recargas, cafeteria, farmacia...). */
    val isService: Boolean = false,
    val supplierName: String = "",
    val supplierPhone: String = "",
    val minStockGlobal: Int = 10,
    val isActive: Boolean = true,
    val description: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val marginPercent: Double
        get() = com.gis.supermercados.core.common.Money.marginPercent(costCents, priceCents)
}

/** Existencias de un producto en una tienda concreta. */
data class StockLevel(
    val id: Long = 0L,
    val productId: Long,
    val storeId: Long,
    val productName: String = "",
    val sku: String = "",
    val storeName: String = "",
    val categoryName: String = "",
    val quantity: Int = 0,
    val reserved: Int = 0,
    val minStock: Int = 10,
    val maxStock: Int = 0,
    val costCents: Long = 0L,
    val priceCents: Long = 0L,
    val unit: ProductUnit = ProductUnit.UNIDAD,
    val updatedAt: Long = 0L,
) {
    val available: Int get() = (quantity - reserved).coerceAtLeast(0)

    /** Valor del inventario a precio de costo. */
    val stockValueCents: Long get() = costCents * quantity

    /** Valor del inventario a precio de venta. */
    val retailValueCents: Long get() = priceCents * quantity

    val isLowStock: Boolean get() = quantity <= minStock
    val isOutOfStock: Boolean get() = quantity <= 0
}

/** Estado del stock de un producto agregado en todas las tiendas. */
data class ProductStockSummary(
    val product: Product,
    val totalQuantity: Int,
    val lowStockStores: Int,
    val outOfStockStores: Int,
    val stockValueCents: Long,
)

/** Movimiento de inventario (entrada, salida, transferencia o ajuste). */
data class InventoryMovement(
    val id: Long = 0L,
    val productId: Long,
    val productName: String = "",
    val sku: String = "",
    val storeId: Long,
    val storeName: String = "",
    val destinationStoreId: Long? = null,
    val destinationStoreName: String = "",
    val type: MovementType,
    val quantity: Int,
    val unitCostCents: Long = 0L,
    val reason: String = "",
    val reference: String = "",
    val userId: Long = 0L,
    val userName: String = "",
    val createdAt: Long = 0L,
) {
    val totalCents: Long get() = unitCostCents.toLong() * quantity
}
