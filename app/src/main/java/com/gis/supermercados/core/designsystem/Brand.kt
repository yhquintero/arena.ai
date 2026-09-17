package com.gis.supermercados.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Paleta corporativa unica de la aplicacion.
 *
 * Se define en formato ARGB (Long) para poder reutilizarla TAMBIEN en los
 * documentos PDF/Excel exportados: el informe impreso y la interfaz comparten
 * exactamente la misma identidad visual.
 */
object Brand {
    const val PRIMARY: Long = 0xFF0B6E4FL          // Verde esmeralda (gestion/crecimiento)
    const val PRIMARY_DARK: Long = 0xFF074536L
    const val PRIMARY_CONTAINER: Long = 0xFFD9F0E6L
    const val ON_PRIMARY: Long = 0xFFFFFFFFL
    const val SECONDARY: Long = 0xFF1B5FA8L        // Azul corporativo (datos)
    const val SECONDARY_CONTAINER: Long = 0xFFDDE8F7L
    const val ACCENT: Long = 0xFFD98324L           // Ambar (alertas/destacados)
    const val ACCENT_CONTAINER: Long = 0xFFFBEEDA

    const val POSITIVE: Long = 0xFF1B7F4B
    const val NEGATIVE: Long = 0xFFB3261E
    const val WARNING: Long = 0xFFB26A00

    const val BACKGROUND: Long = 0xFFF6F8F7L
    const val SURFACE: Long = 0xFFFFFFFFL
    const val SURFACE_VARIANT: Long = 0xFFEFF3F1L
    const val OUTLINE: Long = 0xFFD5DEDA
    const val OUTLINE_VARIANT: Long = 0xFFE7ECEA

    const val TEXT_PRIMARY: Long = 0xFF111A17L
    const val TEXT_SECONDARY: Long = 0xFF53615CL
    const val TEXT_TERTIARY: Long = 0xFF7C8A85L

    // Modo oscuro
    const val DARK_BACKGROUND: Long = 0xFF0F1513L
    const val DARK_SURFACE: Long = 0xFF171E1BL
    const val DARK_SURFACE_VARIANT: Long = 0xFF222B27L
    const val DARK_PRIMARY: Long = 0xFF63D6A8L
    const val DARK_ON_PRIMARY: Long = 0xFF00381FL
    const val DARK_TEXT_PRIMARY: Long = 0xFFE6EDE9L
    const val DARK_TEXT_SECONDARY: Long = 0xFFAEBBB5L
    const val DARK_OUTLINE: Long = 0xFF3A443FL

    /** Colores categoricos para graficos (rotan en orden). */
    val CHART_COLORS: LongArray = longArrayOf(
        0xFF0B6E4F, 0xFF1B5FA8, 0xFFD98324, 0xFF7A4FA3, 0xFF128A8A,
        0xFFB3261E, 0xFF4A7C2A, 0xFF2F5FA0, 0xFFA86B1F, 0xFF5C6BC0,
        0xFF00897B, 0xFF8E5B9F
    )

    fun chartColor(index: Int): Long = CHART_COLORS[index.mod(CHART_COLORS.size)]

    // ---- Puentes a Compose ----
    val PrimaryColor: Color get() = Color(PRIMARY.toInt())
    val PrimaryDarkColor: Color get() = Color(PRIMARY_DARK.toInt())
    val PrimaryContainerColor: Color get() = Color(PRIMARY_CONTAINER.toInt())
    val SecondaryColor: Color get() = Color(SECONDARY.toInt())
    val AccentColor: Color get() = Color(ACCENT.toInt())
    val PositiveColor: Color get() = Color(POSITIVE.toInt())
    val NegativeColor: Color get() = Color(NEGATIVE.toInt())
    val WarningColor: Color get() = Color(WARNING.toInt())
    val BackgroundColor: Color get() = Color(BACKGROUND.toInt())
    val SurfaceVariantColor: Color get() = Color(SURFACE_VARIANT.toInt())
    val OutlineColor: Color get() = Color(OUTLINE.toInt())
    val TextSecondaryColor: Color get() = Color(TEXT_SECONDARY.toInt())
    val TextTertiaryColor: Color get() = Color(TEXT_TERTIARY.toInt())

    fun composeColor(argb: Long): Color = Color(argb.toInt())
}
