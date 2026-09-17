package com.gis.supermercados.core.common

import android.content.Context
import androidx.annotation.StringRes

/**
 * Representa texto que puede venir de recursos (traducible) o dinamico.
 * Permite que las capas de dominio/datos comuniquen mensajes de error sin
 * depender directamente de Context, y que la UI los resuelva/localice.
 */
sealed interface UiText {

    data class Dynamic(val value: String) : UiText

    data class Resource(
        @StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    fun asString(context: Context): String = when (this) {
        is Dynamic -> value
        is Resource -> if (args.isEmpty()) {
            context.getString(id)
        } else {
            context.getString(id, *args.toTypedArray())
        }
    }

    companion object {
        fun of(@StringRes id: Int, vararg args: Any): UiText = Resource(id, args.toList())
        fun of(value: String): UiText = Dynamic(value)
    }
}
