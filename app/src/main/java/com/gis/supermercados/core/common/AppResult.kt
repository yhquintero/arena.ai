package com.gis.supermercados.core.common

import com.gis.supermercados.core.logging.AppLogger
import kotlinx.coroutines.CancellationException

/**
 * Resultado tipado de las operaciones de negocio.
 * Evita propagar excepciones hacia la UI y obliga a tratar el caso de error
 * (requisito de "manejo de errores profesional").
 */
sealed interface AppResult<out T> {

    data class Success<T>(val data: T) : AppResult<T>

    data class Failure(
        val error: UiText,
        val cause: Throwable? = null,
    ) : AppResult<Nothing>

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = (this as? Success)?.data

    fun errorOrNull(): UiText? = (this as? Failure)?.error

    fun getOrElse(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Failure -> default
    }

    fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    suspend fun <R> suspendMap(transform: suspend (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    companion object {
        fun <T> success(data: T): AppResult<T> = Success(data)
        fun failure(error: UiText, cause: Throwable? = null): AppResult<Nothing> =
            Failure(error, cause)
    }
}

/**
 * Ejecuta [block] capturando cualquier excepcion y registrandola en el log interno.
 * Es el unico punto por el que deben pasar las operaciones de repositorio/caso de uso:
 * la UI nunca recibe excepciones crudas, siempre un mensaje localizable.
 */
suspend fun <T> runCatchingApp(
    tag: String,
    errorMessage: UiText,
    block: suspend () -> T,
): AppResult<T> = try {
    AppResult.Success(block())
} catch (cancellation: CancellationException) {
    // Una corrutina cancelada NO es un error: se relanza para no romper el ciclo de vida.
    throw cancellation
} catch (error: IllegalArgumentException) {
    // Regla de negocio violada (stock insuficiente, SKU duplicado, etc.)
    AppLogger.w(tag, "Datos invalidos: ${error.message}")
    val message = error.message
    AppResult.Failure(
        if (message.isNullOrBlank()) errorMessage else UiText.Dynamic(message),
        error
    )
} catch (error: Exception) {
    AppLogger.e(tag, "Error no controlado", error)
    AppResult.Failure(errorMessage, error)
}

/** Version no-suspend para utilidades de calculo puras. */
inline fun <T> resultOf(errorMessage: UiText, block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (error: Exception) {
    AppResult.Failure(errorMessage, error)
}
