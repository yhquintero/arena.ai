package com.gis.supermercados.core.reporting.xlsx

/**
 * Indices de estilo (cellXfs) definidos en el styles.xml generado por [XlsxWriter].
 *
 * Se centralizan aqui para que el renderizador use nombres legibles en lugar de
 * numeros sueltos. Cualquier cambio debe reflejarse en [XlsxWriter.buildStyles].
 */
object XlsStyle {
    const val DEFAULT = 0
    const val TITLE = 1
    const val SUBTITLE = 2
    const val SECTION = 3
    const val TABLE_HEADER = 4
    const val TEXT = 5
    const val TEXT_BOLD = 6
    const val MONEY = 7
    const val MONEY_NEGATIVE = 8
    const val INTEGER = 9
    const val PERCENT = 10
    const val DATE = 11
    const val TOTAL_TEXT = 12
    const val TOTAL_MONEY = 13
    const val TOTAL_INTEGER = 14
    const val KPI_LABEL = 15
    const val KPI_VALUE = 16
    const val KPI_DETAIL = 17
    const val NOTE = 18
    const val META_LABEL = 19
    const val META_VALUE = 20
    const val CHART_BAR = 21
    const val NUMBER_DECIMAL = 22
}
