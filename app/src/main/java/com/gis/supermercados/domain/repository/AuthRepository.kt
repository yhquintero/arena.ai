package com.gis.supermercados.domain.repository

import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.domain.model.AuthSession
import com.gis.supermercados.domain.model.Role
import com.gis.supermercados.domain.model.User
import com.gis.supermercados.domain.model.UserCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Autenticacion local, gestion de usuarios y control de sesion. */
interface AuthRepository {

    val session: StateFlow<AuthSession?>

    val isLoggedIn: Boolean

    /** true si aun no existe ningun usuario (primer arranque -> alta inicial). */
    suspend fun hasAnyUser(): Boolean

    suspend fun login(username: String, password: String): AppResult<AuthSession>

    suspend fun createUser(credentials: UserCredentials, password: String): AppResult<User>

    suspend fun changePassword(
        userId: Long,
        currentPassword: String,
        newPassword: String,
    ): AppResult<Unit>

    /** Solo administradores: reestablece la contrasena de otro usuario. */
    suspend fun resetPassword(userId: Long, newPassword: String): AppResult<Unit>

    suspend fun setBiometricEnabled(userId: Long, enabled: Boolean): AppResult<Unit>

    fun canUseBiometrics(): Boolean

    /** Ultimo usuario recordado (para el desbloqueo biometrico). */
    fun biometricUsername(): String?

    /** Reabre la sesion del ultimo usuario tras verificar la biometria. */
    suspend fun unlockWithBiometrics(): AppResult<AuthSession>

    fun touchSession()

    fun isSessionExpired(): Boolean

    suspend fun logout()

    fun observeUsers(): Flow<List<User>>

    suspend fun updateUserRole(userId: Long, role: Role): AppResult<Unit>

    suspend fun updateUserStore(userId: Long, storeId: Long?): AppResult<Unit>

    suspend fun setUserActive(userId: Long, isActive: Boolean): AppResult<Unit>
}
