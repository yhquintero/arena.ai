package com.gis.supermercados.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.gis.supermercados.core.logging.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cifrado simetrico AES-256/GCM cuya clave vive en el Android Keystore
 * (hardware-backed cuando el dispositivo lo soporta). La clave NUNCA es
 * exportable, por lo que los datos cifrados solo pueden descifrarse en el
 * mismo dispositivo y la misma instalacion de la app.
 *
 * Se usa para proteger la sesion y las preferencias sensibles.
 */
@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val tag = "CryptoManager"

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private fun getOrCreateKey(): SecretKey? = runCatching {
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) {
            // Comprobamos que la clave sigue siendo utilizable (puede invalidarse
            // tras una actualizacion del sistema: error conocido del Keystore).
            Cipher.getInstance(TRANSFORMATION).init(Cipher.ENCRYPT_MODE, existing)
            existing
        } else {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generator.generateKey()
        }
    }.recoverCatching { error ->
        AppLogger.e(tag, "Clave inutilizable, se regenera", error)
        runCatching { keyStore.deleteEntry(KEY_ALIAS) }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build()
        )
        generator.generateKey()
    }.getOrNull()

    /** Devuelve [IV (12 bytes) | texto cifrado | etiqueta GCM]. */
    fun encrypt(plain: ByteArray): ByteArray? {
        val key = getOrCreateKey() ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encrypted = cipher.doFinal(plain)
            cipher.iv + encrypted
        }.onFailure { AppLogger.e(tag, "Fallo al cifrar", it) }.getOrNull()
    }

    fun decrypt(payload: ByteArray): ByteArray? {
        if (payload.size <= IV_LENGTH) return null
        val key = getOrCreateKey() ?: return null
        return runCatching {
            val iv = payload.copyOfRange(0, IV_LENGTH)
            val data = payload.copyOfRange(IV_LENGTH, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            cipher.doFinal(data)
        }.onFailure { AppLogger.w(tag, "Datos cifrados no descifrables (clave regenerada o archivo corrupto)") }
            .getOrNull()
    }

    fun encryptToBase64(text: String): String? =
        encrypt(text.toByteArray(Charsets.UTF_8))?.let { Base64.encodeToString(it, Base64.NO_WRAP) }

    fun decryptFromBase64(encoded: String?): String? {
        if (encoded.isNullOrBlank()) return null
        val payload = runCatching { Base64.decode(encoded, Base64.NO_WRAP) }.getOrNull() ?: return null
        return decrypt(payload)?.toString(Charsets.UTF_8)
    }

    fun isAvailable(): Boolean = getOrCreateKey() != null

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "gis_supermercados_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val TAG_LENGTH_BITS = 128
        private const val IV_LENGTH = 12
    }
}
