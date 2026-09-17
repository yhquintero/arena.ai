package com.gis.supermercados.core.reporting.xlsx

/** Celda de una hoja Excel: texto, numero, fecha o vacia (todas con estilo). */
sealed class XlsxCell {
    abstract val style: Int

    data class Str(val value: String, override val style: Int = XlsStyle.TEXT) : XlsxCell()

    data class Num(val value: Double, override val style: Int = XlsStyle.INTEGER) : XlsxCell()

    /** Importe en centavos: se escribe como numero real con formato monetario. */
    data class MoneyCell(val cents: Long, override val style: Int = XlsStyle.MONEY) : XlsxCell() {
        val value: Double get() = cents / 100.0
    }

    /** Fecha: se escribe como numero de serie de Excel con formato dd/mm/aaaa. */
    data class DateCell(val millis: Long, override val style: Int = XlsStyle.DATE) : XlsxCell()

    data class EmptyCell(override val style: Int = XlsStyle.DEFAULT) : XlsxCell()

    data class Percent(val value: Double, override val style: Int = XlsStyle.PERCENT) : XlsxCell()

    companion object {
        fun text(value: String, style: Int = XlsStyle.TEXT) = Str(value, style)
        fun money(cents: Long, style: Int = XlsStyle.MONEY) = MoneyCell(cents, style)
        fun integer(value: Long, style: Int = XlsStyle.INTEGER) = Num(value.toDouble(), style)
        fun decimal(value: Double, style: Int = XlsStyle.NUMBER_DECIMAL) = Num(value, style)
        fun percent(value: Double, style: Int = XlsStyle.PERCENT) = Percent(value, style)
        fun date(millis: Long) = DateCell(millis)
        fun empty(style: Int = XlsStyle.DEFAULT) = EmptyCell(style)
    }
}
