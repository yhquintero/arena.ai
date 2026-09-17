package com.gis.supermercados.data.repository

import android.content.Context
import com.gis.supermercados.R
import com.gis.supermercados.core.common.AppConstants
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import com.gis.supermercados.core.common.runCatchingApp
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.core.security.BiometricHelper
import com.gis.supermercados.core.security.PasswordHasher
import com.gis.supermercados.core.security.SessionStore
import com.gis.supermercados.data.local.GisDatabase
import com.gis.supermercados.data.local.entity.UserEntity
import com.gis.supermercados.data.mapper.toDomain
import com.gis.supermercados.di.IoDispatcher
import com.gis.supermercados.domain.model.AuthSession
import com.gis.supermercados.domain.model.Role
import com.gis.supermercados.domain.model.User
import com.gis.supermercados.domain.model.UserCredentials
import com.gis.supermercados.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Autenticacion local con hash PBKDF2, bloqueo por intentos fallidos y sesion
 * cifrada. Todo ocurre en el dispositivo: la app no envia credenciales a ningun sitio.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GisDatabase,
    private val passwordHasher: PasswordHasher,
    private val sessionStore: SessionStore,
    private val biometricHelper: BiometricHelper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    private val userDao get() = database.userDao()

    override val session: StateFlow<AuthSession?> = sessionStore.session

    override val isLoggedIn: Boolean get() = sessionStore.isLoggedIn

    override suspend fun hasAnyUser(): Boolean = withContext(ioDispatcher) { userDao.count() > 0 }

    override suspend fun login(username: String, password: String): AppResult<AuthSession> =
        runCatchingApp(TAG, UiText.of(R.string.auth_error_generic)) {
            withContext(ioDispatcher) {
                val now = System.currentTimeMillis()
                val user = userDao.getByUsername(username.trim())
                    ?: throw IllegalArgumentException(context.getString(R.string.auth_error_invalid_credentials))

                if (!user.isActive) {
                    throw IllegalArgumentException(context.getString(R.string.auth_error_user_inactive))
                }

                if (user.lockedUntil > now) {
                    val minutes = ((user.lockedUntil - now) / MILLIS_PER_MINUTE) + 1
                    throw IllegalArgumentException(context.getString(R.string.auth_error_account_locked, minutes.toInt()))
                }

                if (!passwordHasher.verify(password, user.passwordSalt, user.passwordHash)) {
                    val attempts = user.failedAttempts + 1
                    val lockedUntil = if (attempts >= AppConstants.MAX_FAILED_LOGINS) {
                        now + AppConstants.LOCK_MINUTES * MILLIS_PER_MINUTE
                    } else {
                        0L
                    }
                    userDao.updateFailedAttempts(user.id, attempts, lockedUntil)
                    AppLogger.w(TAG, "Login fallido para '${user.username}' (intento $attempts)")
                    val remaining = (AppConstants.MAX_FAILED_LOGINS - attempts).coerceAtLeast(0)
                    throw IllegalArgumentException(
                        if (lockedUntil > 0) {
                            context.getString(R.string.auth_error_account_locked, AppConstants.LOCK_MINUTES)
                        } else {
                            context.getString(R.string.auth_error_invalid_credentials_with_attempts, remaining)
                        }
                    )
                }

                userDao.registerSuccessfulLogin(user.id, now)
                val storeName = user.storeId?.let { database.storeDao().getById(it)?.name }.orEmpty()
                val session = AuthSession(
                    userId = user.id,
                    username = user.username,
                    fullName = user.fullName.ifBlank { user.username },
                    role = user.role,
                    storeId = user.storeId,
                    storeName = storeName,
                    loginAt = now
                )
                sessionStore.saveSession(session)
                AppLogger.i(TAG, "Sesion iniciada: ${user.username} (${user.role})")
                session
            }
        }

    override suspend fun createUser(credentials: UserCredentials, password: String): AppResult<User> =
        runCatchingApp(TAG, UiText.of(R.string.auth_error_generic)) {
            withContext(ioDispatcher) {
                val username = credentials.username.trim()
                if (userDao.getByUsername(username) != null) {
                    throw IllegalArgumentException(context.getString(R.string.auth_error_username_taken))
                }
                val now = System.currentTimeMillis()
                val salt = passwordHasher.generateSalt()
                val entity = UserEntity(
                    username = username,
                    fullName = credentials.fullName.trim().ifBlank { username },
                    passwordHash = passwordHasher.hash(password, salt),
                    passwordSalt = salt,
                    role = credentials.role,
                    storeId = credentials.storeId,
                    isActive = true,
                    createdAt = now,
                    lastLoginAt = 0L
                )
                val id = userDao.insert(entity)
                val storeName = entity.storeId?.let { database.storeDao().getById(it)?.name }.orEmpty()
                AppLogger.i(TAG, "Usuario creado: $username (${entity.role})")
                entity.copy(id = id).toDomain(storeName)
            }
        }

    override suspend fun changePassword(
        userId: Long,
        currentPassword: String,
        newPassword: String,
    ): AppResult<Unit> = runCatchingApp(TAG, UiText.of(R.string.auth_error_generic)) {
        withContext(ioDispatcher) {
            val user = userDao.getById(userId)
                ?: throw IllegalArgumentException(context.getString(R.string.auth_error_user_not_found))
            if (!passwordHasher.verify(currentPassword, user.passwordSalt, user.passwordHash)) {
                throw IllegalArgumentException(context.getString(R.string.auth_error_current_password))
            }
            val salt = passwordHasher.generateSalt()
            userDao.updatePassword(userId, passwordHasher.hash(newPassword, salt), salt)
            AppLogger.i(TAG, "Contrasena actualizada para el usuario $userId")
            Unit
        }
    }

    override suspend fun resetPassword(userId: Long, newPassword: String): AppResult<Unit> =
        runCatchingApp(TAG, UiText.of(R.string.auth_error_generic)) {
            withContext(ioDispatcher) {
                val salt = passwordHasher.generateSalt()
                userDao.updatePassword(userId, passwordHasher.hash(newPassword, salt), salt)
                userDao.updateFailedAttempts(userId, 0, 0L)
                AppLogger.w(TAG, "Contrasena restablecida por un administrador (usuario $userId)")
                Unit
            }
        }

    override suspend fun setBiometricEnabled(userId: Long, enabled: Boolean): AppResult<Unit> =
        runCatchingApp(TAG, UiText.of(R.string.auth_error_generic)) {
            withContext(ioDispatcher) {
                userDao.setBiometricEnabled(userId, enabled)
                Unit
            }
        }

    override fun canUseBiometrics(): Boolean = biometricHelper.canAuthenticate()

    override fun biometricUsername(): String? = sessionStore.lastUsername()

    override suspend fun unlockWithBiometrics(): AppResult<AuthSession> =
        runCatchingApp(TAG, UiText.of(R.string.auth_error_generic)) {
            withContext(ioDispatcher) {
                val username = sessionStore.lastUsername()
                    ?: throw IllegalArgumentException(context.getString(R.string.auth_error_biometric_unavailable))
                val user = userDao.getByUsername(username)
                    ?: throw IllegalArgumentException(context.getString(R.string.auth_error_biometric_unavailable))
                if (!user.isActive || !user.biometricEnabled) {
                    throw IllegalArgumentException(context.getString(R.string.auth_error_biometric_unavailable))
                }
                val now = System.currentTimeMillis()
                userDao.registerSuccessfulLogin(user.id, now)
                val storeName = user.storeId?.let { database.storeDao().getById(it)?.name }.orEmpty()
                val session = AuthSession(
                    userId = user.id,
                    username = user.username,
                    fullName = user.fullName.ifBlank { user.username },
                    role = user.role,
                    storeId = user.storeId,
                    storeName = storeName,
                    loginAt = now
                )
                sessionStore.saveSession(session)
                AppLogger.i(TAG, "Sesion reabierta con biometria: ${user.username}")
                session
            }
        }

    override fun touchSession() = sessionStore.touch()

    override fun isSessionExpired(): Boolean = sessionStore.isExpired()

    override suspend fun logout() {
        sessionStore.clear()
    }

    override fun observeUsers(): Flow<List<User>> =
        userDao.observeAllWithStore()
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun updateUserRole(userId: Long, role: Role): AppResult<Unit> =
        runCatchingApp(TAG, UiText.of(R.string.auth_error_generic)) {
            withContext(ioDispatcher) {
                userDao.updateRole(userId, role)
                Unit
            }
        }

    override suspend fun updateUserStore(userId: Long, storeId: Long?): AppResult<Unit> =
        runCatchingApp(TAG, UiText.of(R.string.auth_error_generic)) {
            withContext(ioDispatcher) {
                userDao.updateStore(userId, storeId)
                Unit
            }
        }

    override suspend fun setUserActive(userId: Long, isActive: Boolean): AppResult<Unit> =
        runCatchingApp(TAG, UiText.of(R.string.auth_error_generic)) {
            withContext(ioDispatcher) {
                userDao.setActive(userId, isActive)
                Unit
            }
        }

    private companion object {
        const val TAG = "AuthRepository"
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
