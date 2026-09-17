package com.gis.supermercados.domain.model

/**
 * Sucursal del supermercado.
 * Incluye horario y permisos propios (cada tienda puede operar con reglas distintas).
 */
data class Store(
    val id: Long = 0L,
    val name: String,
    val code: String,
    val address: String = "",
    val city: String = "",
    val phone: String = "",
    val email: String = "",
    val managerName: String = "",
    val taxId: String = "",
    val isActive: Boolean = true,
    val schedule: StoreSchedule = StoreSchedule(),
    val permissions: StorePermissions = StorePermissions(),
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val displayName: String get() = if (code.isBlank()) name else "$code · $name"
}

/** Horario de atencion: dias abiertos (ISO 1 = lunes .. 7 = domingo) y franja horaria. */
data class StoreSchedule(
    val openDays: Set<Int> = DEFAULT_OPEN_DAYS,
    val openingTime: String = "08:00",
    val closingTime: String = "21:00",
    val hasExtendedHours: Boolean = false,
) {
    fun isOpenOn(isoDayOfWeek: Int): Boolean = isoDayOfWeek in openDays

    companion object {
        val DEFAULT_OPEN_DAYS: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7)
        val WEEKDAYS: Set<Int> = setOf(1, 2, 3, 4, 5)
    }
}

/**
 * Permisos operativos por sucursal.
 * Permiten, por ejemplo, que una tienda no pueda aplicar descuentos mayores al 10%
 * o que no pueda registrar gastos corporativos.
 */
data class StorePermissions(
    val allowSales: Boolean = true,
    val allowReturns: Boolean = true,
    val allowInventoryAdjust: Boolean = true,
    val allowTransfers: Boolean = true,
    val allowExpenses: Boolean = true,
    val allowReports: Boolean = true,
    val maxDiscountPercent: Int = 20,
)
