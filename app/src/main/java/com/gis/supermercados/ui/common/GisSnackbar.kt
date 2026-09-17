package com.gis.supermercados.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.gis.supermercados.core.common.UiText
import kotlinx.coroutines.launch

/**
 * Canal unico de mensajes emergentes. Las pantallas no reciben el
 * [SnackbarHostState] por parametro: lo toman de este CompositionLocal, lo que
 * evita atravesar media jerarquia de composables.
 */
val LocalSnackbar = compositionLocalOf<SnackbarHostState?> { null }

/** Devuelve una funcion lista para mostrar mensajes (UiText o texto plano). */
@Composable
fun rememberMessageEmitter(): (UiText?) -> Unit {
    val snackbarHostState = LocalSnackbar.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return { uiText ->
        val message = uiText?.asString(context)
        if (!message.isNullOrBlank() && snackbarHostState != null) {
            scope.launch {
                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            }
        }
    }
}

/** Version para texto ya localizado. */
@Composable
fun rememberTextEmitter(): (String) -> Unit {
    val snackbarHostState = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    return { message ->
        if (message.isNotBlank() && snackbarHostState != null) {
            scope.launch {
                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            }
        }
    }
}
