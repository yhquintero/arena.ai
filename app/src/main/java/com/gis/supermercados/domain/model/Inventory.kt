package com.gis.supermercados.domain.model

/** Solicitud de movimiento de inventario (entrada, salida o ajuste). */
data class MovementRequest(
    val productId: Long,
    val storeId: Long,
    val type: MovementType,
    val quantity: Int,
    val unitCostCents: Long = 0L,
    val reason: String = "",
    val reference: String = "",
    val userId: Long = 0L,
    val destinationStoreId: Long? = null,
) {
    val totalCents: Long get() = unitCostCents * quantity
}

/** Solicitud de transferencia entre dos sucursales. */
data class TransferRequest(
    val productId: Long,
    val fromStoreId: Long,
    val toStoreId: Long,
    val quantity: Int,
    val userId: Long = 0L,
    val reference: String = "",
    val reason: String = "Transferencia entre tiendas",
)

/** Valoracion del inventario (costo, venta y alertas). */
data class InventoryValue(
    val totalQuantity: Long = 0L,
    val costValueCents: Long = 0L,
    val retailValueCents: Long = 0L,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
) {
    val potentialProfitCents: Long get() = retailValueCents - costValueCents
}
