package com.gis.supermercados.domain.model

/** Cliente con perfil, historial agregado y credito. */
data class Customer(
    val id: Long = 0L,
    val fullName: String,
    val documentType: DocumentType = DocumentType.CEDULA,
    val documentId: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val loyaltyPoints: Int = 0,
    val creditLimitCents: Long = 0L,
    /** Saldo pendiente de pago (ventas a credito). */
    val balanceCents: Long = 0L,
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    // Campos calculados (solo se rellenan en el detalle del cliente)
    val purchaseCount: Int = 0,
    val totalPurchasesCents: Long = 0L,
    val lastPurchaseAt: Long = 0L,
) {
    val availableCreditCents: Long get() = (creditLimitCents - balanceCents).coerceAtLeast(0L)
    val initials: String
        get() = fullName.trim().split(Regex("\\s+"))
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .joinToString("")
            .ifBlank { "?" }
}
