package com.gis.supermercados.core.security

import android.content.Context
import android.content.SharedPreferences
import com.gis.supermercados.core.common.AppConstants
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.domain.model.AuthSession
import com.gis.supermercados.domain.model.Role
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Almacena la sesion activa CIFRADA en almacenamiento privado.
 * Tambien controla el cierre de sesion automatico por inactividad.
 */
@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext context: Context,
    private val cryptoManager: CryptoManager,
) {

    private val tag = "SessionStore"

    private val prefs: SharedPreferences =
        context.getSharedPreferences(AppConstants.SECURE_PREFS_NAME, Context.MODE_PRIVATE)

    private val _session = MutableStateFlow(readStoredSession())

    /** Sesion actual (null = usuario no autenticado). */
    val session: StateFlow<AuthSession?> = _session.asStateFlow()

    val isLoggedIn: Boolean get() = _session.value != null

    fun saveSession(session: AuthSession) {
        val json = JSONObject().apply {
            put(KEY_USER_ID, session.userId)
            put(KEY_USERNAME, session.username)
            put(KEY_FULL_NAME, session.fullName)
            put(KEY_ROLE, session.role.name)
            put(KEY_STORE_ID, session.storeId ?: JSONObject.NULL)
            put(KEY_STORE_NAME, session.storeName)
            put(KEY_LOGIN_AT, session.loginAt)
        }
        val encrypted = cryptoManager.encryptToBase64(json.toString())
        if (encrypted == null) {
            AppLogger.e(tag, "No se pudo cifrar la sesion; no se persiste")
            _session.value = session
            return
        }
        prefs.edit().putString(KEY_SESSION, encrypted).apply()
        rememberUser(session.username)
        touch()
        _session.value = session
        AppLogger.i(tag, "Sesion iniciada: ${session.username} (${session.role.name})")
    }

    /**
     * Recuerda el ultimo usuario que inicio sesion (solo el nombre, sin secretos)
     * para poder ofrecer el desbloqueo con biometria.
     */
    fun rememberUser(username: String) {
        prefs.edit().putString(KEY_LAST_USERNAME, username).apply()
    }

    /** Ultimo usuario conocido, o null si nadie ha iniciado sesion aun. */
    fun lastUsername(): String? = prefs.getString(KEY_LAST_USERNAME, null)

    fun clear() {
        prefs.edit().remove(KEY_SESSION).remove(KEY_LAST_INTERACTION).apply()
        _session.value = null
        AppLogger.i(tag, "Sesion cerrada")
    }

    /** Marca actividad del usuario (reinicia el contador de inactividad). */
    fun touch() {
        prefs.edit().putLong(KEY_LAST_INTERACTION, System.currentTimeMillis()).apply()
    }

    /** Indica si la sesion caduco por inactividad. */
    fun isExpired(timeoutMinutes: Int = AppConstants.SESSION_IDLE_TIMEOUT_MINUTES): Boolean {
        if (_session.value == null) return true
        val last = prefs.getLong(KEY_LAST_INTERACTION, System.currentTimeMillis())
        val elapsedMinutes = (System.currentTimeMillis() - last) / MILLIS_PER_MINUTE
        return elapsedMinutes > timeoutMinutes
    }

    private fun readStoredSession(): AuthSession? {
        val encrypted = prefs.getString(KEY_SESSION, null) ?: return null
        val json = cryptoManager.decryptFromBase64(encrypted) ?: run {
            AppLogger.w(tag, "Sesion almacenada no descifrable: se descarta")
            prefs.edit().remove(KEY_SESSION).apply()
            return null
        }
        return runCatching {
            val obj = JSONObject(json)
            AuthSession(
                userId = obj.getLong(KEY_USER_ID),
                username = obj.getString(KEY_USERNAME),
                fullName = obj.optString(KEY_FULL_NAME, ""),
                role = runCatching { Role.valueOf(obj.getString(KEY_ROLE)) }.getOrDefault(Role.CAJERO),
                storeId = if (obj.isNull(KEY_STORE_ID)) null else obj.getLong(KEY_STORE_ID),
                storeName = obj.optString(KEY_STORE_NAME, ""),
                loginAt = obj.optLong(KEY_LOGIN_AT, System.currentTimeMillis())
            )
        }.onFailure { AppLogger.e(tag, "Sesion corrupta", it) }.getOrNull()
    }

    private companion object {
        const val KEY_SESSION = "encrypted_session"
        const val KEY_LAST_INTERACTION = "last_interaction"
        const val KEY_LAST_USERNAME = "last_username"
        const val KEY_USER_ID = "userId"
        const val KEY_USERNAME = "username"
        const val KEY_FULL_NAME = "fullName"
        const val KEY_ROLE = "role"
        const val KEY_STORE_ID = "storeId"
        const val KEY_STORE_NAME = "storeName"
        const val KEY_LOGIN_AT = "loginAt"
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
