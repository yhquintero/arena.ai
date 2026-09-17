package com.gis.supermercados.core.common

import java.util.Locale

/**
 * Formateo numerico compartido por interfaz e informes.
 * Tenerlo en un unico sitio garantiza que un porcentaje se vea igual en la
 * pantalla y en el PDF/Excel exportado.
 */

/** Porcentaje con signo neutro y un decimal: `12,5 %`. */
fun formatPercent(value: Double, decimals: Int = 1): String =
    String.format(Locale("es"), "%.${decimals}f %%", value)

/** Porcentaje con signo explicito para variaciones: `+12,5 %` / `-3,0 %`. */
fun formatSignedPercent(value: Double, decimals: Int = 1): String =
    (if (value > 0) "+" else "") + formatPercent(value, decimals)

/** Cantidad con separador español y hasta dos decimales: `1.234` / `12,5`. */
fun formatQuantity(value: Double): String =
    if (value % 1.0 == 0.0) {
        String.format(Locale("es"), "%,.0f", value)
    } else {
        String.format(Locale("es"), "%,.2f", value)
    }

/** Numero entero con separador de miles: `1.234`. */
fun formatInt(value: Int): String = String.format(Locale("es"), "%,d", value)

/** Numero largo con separador de miles: `1.234.567`. */
fun formatLong(value: Long): String = String.format(Locale("es"), "%,d", value)
