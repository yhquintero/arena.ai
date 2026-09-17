package com.gis.supermercados.domain.model

/** Usuario del sistema (autenticacion local). La contrasena NUNCA sale del repositorio. */
data class User(
    val id: Long = 0L,
    val username: String,
    val fullName: String,
    val role: Role = Role.CAJERO,
    val storeId: Long? = null,
    val storeName: String = "",
    val biometricEnabled: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val lastLoginAt: Long = 0L,
)

/** Sesion activa: se guarda cifrada en almacenamiento local. */
data class AuthSession(
    val userId: Long,
    val username: String,
    val fullName: String,
    val role: Role,
    val storeId: Long?,
    val storeName: String = "",
    val loginAt: Long,
) {
    val isGlobal: Boolean get() = storeId == null
}

/** Credenciales de alta / cambio de contrasena. */
data class UserCredentials(
    val username: String,
    val password: String,
    val fullName: String = "",
    val role: Role = Role.ADMIN,
    val storeId: Long? = null,
)
