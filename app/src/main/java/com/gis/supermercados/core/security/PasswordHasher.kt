package com.gis.supermercados.core.security

import android.util.Base64
import com.gis.supermercados.core.common.AppConstants
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hashing de contrasenas con PBKDF2-HMAC-SHA256 + sal aleatoria por usuario.
 *
 * Nunca se guarda la contrasena en claro ni un hash sin sal: aunque alguien
 * extraiga la base de datos, no puede reconstruir las credenciales.
 */
@Singleton
class PasswordHasher @Inject constructor() {

    private val random = SecureRandom()

    /** Genera una sal nueva de 16 bytes en Base64 (sin saltos de linea). */
    fun generateSalt(): String {
        val salt = ByteArray(SALT_BYTES)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    /** Calcula el hash de [password] con la [salt] indicada. */
    fun hash(password: String, salt: String): String {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val spec = PBEKeySpec(
            password.toCharArray(),
            saltBytes,
            AppConstants.PBKDF2_ITERATIONS,
            KEY_LENGTH_BITS
        )
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hashBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }

    /**
     * Verificacion en tiempo constante: se compara el hash recalculado byte a byte
     * con [MessageDigest.isEqual] para evitar ataques de temporizacion.
     */
    fun verify(password: String, salt: String, expectedHash: String): Boolean = runCatching {
        val candidate = hash(password, salt)
        MessageDigest.isEqual(
            Base64.decode(candidate, Base64.NO_WRAP),
            Base64.decode(expectedHash, Base64.NO_WRAP)
        )
    }.getOrDefault(false)

    private companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val SALT_BYTES = 16
        const val KEY_LENGTH_BITS = 256
    }
}
