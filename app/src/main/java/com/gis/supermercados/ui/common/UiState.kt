package com.gis.supermercados.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.gis.supermercados.core.common.AppResult
import com.gis.supermercados.core.common.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Estado generico de pantalla: datos + carga + error + mensaje puntual.
 * Un solo tipo para todas las pantallas simplifica los ViewModels y la UI.
 */
data class UiState<T>(
    val isLoading: Boolean = false,
    val data: T? = null,
    val error: UiText? = null,
    val message: UiText? = null,
) {
    val hasError: Boolean get() = error != null
}

/** Extiende un [MutableStateFlow] con atajos de actualizacion inmutables. */
fun <T> MutableStateFlow<UiState<T>>.startLoading() = update { it.copy(isLoading = true) }

fun <T> MutableStateFlow<UiState<T>>.stopLoading() = update { it.copy(isLoading = false) }

fun <T> MutableStateFlow<UiState<T>>.setData(data: T) = update {
    it.copy(isLoading = false, data = data, error = null)
}

fun <T> MutableStateFlow<UiState<T>>.setError(error: UiText?) = update {
    it.copy(isLoading = false, error = error)
}

fun <T> MutableStateFlow<UiState<T>>.showMessage(message: UiText?) = update { it.copy(message = message) }

/** Traduce un [AppResult] al estado de la pantalla (exito -> mensaje, fallo -> error). */
fun <T> MutableStateFlow<UiState<T>>.applyResult(
    result: AppResult<T>,
    successMessage: UiText? = null,
) {
    when (result) {
        is AppResult.Success -> {
            setData(result.data)
            if (successMessage != null) showMessage(successMessage)
        }

        is AppResult.Failure -> {
            setError(result.error)
            stopLoading()
        }
    }
}

/** Resuelve un [UiText] a cadena dentro de la composicion. */
@Composable
fun UiText?.asString(): String {
    val context = LocalContext.current
    val text = this
    return androidx.compose.runtime.remember(text, context) { text?.asString(context).orEmpty() }
}

/** Ejecuta [block] una unica vez cuando [key] cambia (efecto de pantalla). */
@Composable
fun OnChange(key: Any?, block: suspend () -> Unit) {
    LaunchedEffect(key) { if (key != null) block() }
}
