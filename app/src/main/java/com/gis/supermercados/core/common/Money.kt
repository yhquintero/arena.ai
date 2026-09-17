package com.gis.supermercados.core.common

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Todo el dinero de la aplicacion se almacena y calcula en CENTAVOS (Long).
 *
 * Motivo: los tipos flotantes (Float/Double) introducen errores de redondeo
 * inaceptables en contabilidad (0.1 + 0.2 != 0.3). Con enteros la aritmetica
 * es exacta y las sumatorias de reportes cuadran al centavo.
 *
 * Convencion de nombres: cualquier campo monetario termina en "Cents".
 */
object Money {

    const val CENTS_PER_UNIT = 100L
    private const val DEFAULT_PATTERN = "#,##0.00"

    /** 12.5 -> 1250 (redondeo HALF_UP, el estandar contable). */
    fun fromDecimal(value: Double): Long =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()

    fun fromBigDecimal(value: BigDecimal): Long =
        value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()

    /** 1250 -> BigDecimal 12.50 (para calculos intermedios de alta precision). */
    fun toBigDecimal(cents: Long): BigDecimal = BigDecimal.valueOf(cents).movePointLeft(2)

    fun toDouble(cents: Long): Double = cents / 100.0

    /**
     * Interpreta texto escrito por el usuario en cualquier formato comun:
     * "1234.56", "1.234,56", "$ 1,234.56", "12", "-50" y "(50,00)" (negativos contables).
     * Devuelve null si no es un importe valido.
     */
    fun parse(input: String?): Long? {
        val raw = input?.trim().orEmpty()
        if (raw.isEmpty()) return null

        // Dos convenciones contables de negativo: entre parentesis "(50,00)" o con signo "-50".
        // El signo se detecta ANTES de limpiar, porque la limpieza elimina todo lo que no sea digito.
        val negative = (raw.startsWith("(") && raw.endsWith(")")) || raw.contains('-')
        val cleaned = raw
            .replace(Regex("[^0-9,.]"), "")
            .trim(',', '.')
        if (cleaned.isEmpty()) return null

        val normalized = when {
            cleaned.contains(',') && cleaned.contains('.') ->
                if (cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')) {
                    cleaned.replace(".", "").replace(',', '.')   // 1.234,56
                } else {
                    cleaned.replace(",", "")                     // 1,234.56
                }
            cleaned.contains(',') -> cleaned.replace(',', '.')
            cleaned.count { it == '.' } > 1 -> cleaned.replace(".", "")
            else -> cleaned
        }

        val value = normalized.toDoubleOrNull() ?: return null
        val cents = fromDecimal(abs(value))
        return if (negative || value < 0) -cents else cents
    }

    /** Formatea con separador de miles y decimales configurables (por defecto estilo es: $1.234,56). */
    fun format(
        cents: Long,
        symbol: String = AppConstants.DEFAULT_CURRENCY_SYMBOL,
        groupingSeparator: Char = '.',
        decimalSeparator: Char = ',',
    ): String {
        val body = decimalFormat(groupingSeparator, decimalSeparator).format(abs(cents) / 100.0)
        val sign = if (cents < 0) "-" else ""
        return "$sign$symbol$body"
    }

    /** Igual que [format] pero sin simbolo (util para columnas y exportaciones). */
    fun formatNumber(
        cents: Long,
        groupingSeparator: Char = '.',
        decimalSeparator: Char = ',',
    ): String {
        val body = decimalFormat(groupingSeparator, decimalSeparator).format(abs(cents) / 100.0)
        return if (cents < 0) "-$body" else body
    }

    /** Formato compacto para tarjetas KPI: 12.345.678 -> $12,3 M */
    fun formatCompact(
        cents: Long,
        symbol: String = AppConstants.DEFAULT_CURRENCY_SYMBOL,
    ): String {
        val value = abs(cents) / 100.0
        val sign = if (cents < 0) "-" else ""
        val nf = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale("es")))
        return when {
            value >= 1_000_000_000 -> "$sign$symbol${nf.format(value / 1_000_000_000)} MM"
            value >= 1_000_000 -> "$sign$symbol${nf.format(value / 1_000_000)} M"
            value >= 10_000 -> "$sign$symbol${nf.format(value / 1_000)} K"
            else -> format(cents, symbol)
        }
    }

    // ---- Aritmetica exacta sobre centavos ----

    fun sum(vararg amounts: Long): Long = amounts.sum()

    fun subtract(a: Long, b: Long): Long = a - b

    /** Aplica un porcentaje (0-100) y devuelve el importe resultante. */
    fun percentOf(cents: Long, percent: Double): Long = (cents * percent / 100.0).roundToLong()

    /** Descuento: importe final tras restar [percent]%. */
    fun afterDiscount(cents: Long, percent: Int): Long =
        if (percent <= 0) cents else cents - percentOf(cents, percent.toDouble())

    /** Impuestos: base * tasa (0.16 = 16%). */
    fun tax(baseCents: Long, rate: Double): Long =
        if (rate <= 0.0) 0L else BigDecimal.valueOf(baseCents)
            .multiply(BigDecimal.valueOf(rate))
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()

    /** Importe con impuestos a partir de base y tasa. */
    fun withTax(baseCents: Long, rate: Double): Long = baseCents + tax(baseCents, rate)

    /** Margen bruto en porcentaje: (venta - costo) / venta * 100. */
    fun marginPercent(costCents: Long, saleCents: Long): Double =
        if (saleCents <= 0L) 0.0 else (saleCents - costCents).toDouble() * 100.0 / saleCents.toDouble()

    /** Rentabilidad sobre costo (markup) en porcentaje. */
    fun markupPercent(costCents: Long, saleCents: Long): Double =
        if (costCents <= 0L) 0.0 else (saleCents - costCents).toDouble() * 100.0 / costCents.toDouble()

    /** Division segura: devuelve 0.0 si el denominador es cero (evita NaN en reportes). */
    fun safeDivide(numerator: Long, denominator: Long): Double =
        if (denominator == 0L) 0.0 else numerator.toDouble() / denominator.toDouble()

    fun safeDivide(numerator: Double, denominator: Double): Double =
        if (denominator == 0.0) 0.0 else numerator / denominator

    /** Promedio en centavos (redondeado). */
    fun average(values: List<Long>): Long =
        if (values.isEmpty()) 0L else values.sum() / values.size

    /** Variacion porcentual entre dos periodos (usada en reportes comparativos). */
    fun variationPercent(previous: Long, current: Long): Double =
        if (previous == 0L) {
            when {
                current > 0 -> 100.0
                current < 0 -> -100.0
                else -> 0.0
            }
        } else {
            (current - previous).toDouble() * 100.0 / abs(previous).toDouble()
        }

    private fun decimalFormat(groupingSeparator: Char, decimalSeparator: Char): DecimalFormat {
        val symbols = DecimalFormatSymbols(Locale.ROOT).apply {
            this.groupingSeparator = groupingSeparator
            this.decimalSeparator = decimalSeparator
            this.minusSign = '-'
        }
        return DecimalFormat(DEFAULT_PATTERN, symbols)
    }
}
