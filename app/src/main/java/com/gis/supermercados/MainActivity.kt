package com.gis.supermercados

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.gis.supermercados.core.logging.AppLogger
import com.gis.supermercados.ui.GisApp
import com.gis.supermercados.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Unica actividad de la aplicacion (Compose).
 *
 * Extiende [FragmentActivity] porque BiometricPrompt (autenticacion biometrica)
 * necesita un FragmentManager. Muestra la pantalla de bienvenida del sistema
 * mientras se comprueba el estado inicial (ajustes + sesion + usuarios).
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // El splash se mantiene hasta que MainViewModel termina de leer el estado.
        splashScreen.setKeepOnScreenCondition { viewModel.isLoading.value }

        enableEdgeToEdge()
        AppLogger.i(TAG, "MainActivity onCreate")

        setContent {
            // La clase de tamano permite adaptar la navegacion a telefono o tablet.
            val windowSizeClass = calculateWindowSizeClass(this)
            GisApp(viewModel = viewModel, windowSizeClass = windowSizeClass)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onActivityResumed()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        viewModel.onUserInteraction()
    }

    override fun onDestroy() {
        AppLogger.d(TAG, "MainActivity onDestroy")
        super.onDestroy()
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
