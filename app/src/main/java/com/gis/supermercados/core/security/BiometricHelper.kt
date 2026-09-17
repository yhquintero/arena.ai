package com.gis.supermercados.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.gis.supermercados.core.logging.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Envoltorio de autenticacion biometrica (huella / rostro / PIN del dispositivo).
 *
 * Es una opcion de CONVENIENCIA: si el dispositivo no tiene biometria, o el
 * prompt falla, la app sigue funcionando con usuario y contrasena.
 * Toda excepcion se captura para garantizar que nunca bloquea el acceso.
 */
@Singleton
class BiometricHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val tag = "BiometricHelper"
    private val executor = Executors.newSingleThreadExecutor()

    private val allowedAuthenticators =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /** true si el dispositivo puede autenticar con biometria o credencial. */
    fun canAuthenticate(): Boolean = runCatching {
        BiometricManager.from(context).canAuthenticate(allowedAuthenticators) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }.onFailure { AppLogger.w(tag, "canAuthenticate fallo", it) }.getOrDefault(false)

    /**
     * Lanza el prompt del sistema.
     * @param onResult se invoca una unica vez con true (autenticado) o false.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        description: String,
        onResult: (Boolean, String?) -> Unit,
    ) {
        if (!canAuthenticate()) {
            onResult(false, null)
            return
        }
        runCatching {
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    AppLogger.i(tag, "Autenticacion biometrica correcta")
                    onResult(true, null)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    AppLogger.w(tag, "Error biometrico ($errorCode): $errString")
                    // ERROR_NEGATIVE_BUTTON / USER_CANCELED no son fallos reales:
                    // simplemente el usuario prefiero usar su contrasena.
                    onResult(false, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    AppLogger.w(tag, "Biometria no reconocida")
                }
            }

            val prompt = BiometricPrompt(activity, executor, callback)
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setAllowedAuthenticators(allowedAuthenticators)
                .setConfirmationRequired(false)
                .build()
            prompt.authenticate(info)
        }.onFailure { error ->
            AppLogger.e(tag, "No se pudo iniciar la biometria", error)
            onResult(false, error.message)
        }
    }

    fun release() {
        runCatching { executor.shutdown() }
    }
}
