package com.gis.supermercados.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gis.supermercados.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Brand.PrimaryColor,
    onPrimary = Color.White,
    primaryContainer = Color(Brand.PRIMARY_CONTAINER.toInt()),
    onPrimaryContainer = Brand.PrimaryDarkColor,
    inversePrimary = Color(Brand.PRIMARY_CONTAINER.toInt()),
    secondary = Brand.SecondaryColor,
    onSecondary = Color.White,
    secondaryContainer = Color(Brand.SECONDARY_CONTAINER.toInt()),
    onSecondaryContainer = Color(Brand.SECONDARY.toInt()),
    tertiary = Brand.AccentColor,
    onTertiary = Color.White,
    tertiaryContainer = Color(Brand.ACCENT_CONTAINER.toInt()),
    onTertiaryContainer = Color(Brand.ACCENT.toInt()),
    background = Brand.BackgroundColor,
    onBackground = Color(Brand.TEXT_PRIMARY.toInt()),
    surface = Color(Brand.SURFACE.toInt()),
    onSurface = Color(Brand.TEXT_PRIMARY.toInt()),
    surfaceVariant = Brand.SurfaceVariantColor,
    onSurfaceVariant = Brand.TextSecondaryColor,
    surfaceTint = Brand.PrimaryColor,
    outline = Brand.OutlineColor,
    outlineVariant = Color(Brand.OUTLINE_VARIANT.toInt()),
    error = Brand.NegativeColor,
    onError = Color.White,
    errorContainer = Color(Brand.ACCENT_CONTAINER.toInt()),
    onErrorContainer = Brand.NegativeColor,
    scrim = Color.Black
)

private val DarkColors = darkColorScheme(
    primary = Color(Brand.DARK_PRIMARY.toInt()),
    onPrimary = Color(Brand.DARK_ON_PRIMARY.toInt()),
    primaryContainer = Brand.PrimaryDarkColor,
    onPrimaryContainer = Color(Brand.PRIMARY_CONTAINER.toInt()),
    secondary = Color(Brand.SECONDARY_CONTAINER.toInt()),
    onSecondary = Color(Brand.SECONDARY.toInt()),
    secondaryContainer = Color(Brand.SECONDARY.toInt()),
    onSecondaryContainer = Color.White,
    tertiary = Brand.AccentColor,
    onTertiary = Color.Black,
    tertiaryContainer = Color(Brand.ACCENT.toInt()),
    onTertiaryContainer = Color.White,
    background = Color(Brand.DARK_BACKGROUND.toInt()),
    onBackground = Color(Brand.DARK_TEXT_PRIMARY.toInt()),
    surface = Color(Brand.DARK_SURFACE.toInt()),
    onSurface = Color(Brand.DARK_TEXT_PRIMARY.toInt()),
    surfaceVariant = Color(Brand.DARK_SURFACE_VARIANT.toInt()),
    onSurfaceVariant = Color(Brand.DARK_TEXT_SECONDARY.toInt()),
    outline = Color(Brand.DARK_OUTLINE.toInt()),
    outlineVariant = Color(Brand.DARK_OUTLINE.toInt()),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color.Black
)

/** Colores semanticos que Material 3 no define (positivo, negativo, aviso, graficos). */
@Immutable
data class GisExtendedColors(
    val positive: Color = Brand.PositiveColor,
    val negative: Color = Brand.NegativeColor,
    val warning: Color = Brand.WarningColor,
    val accent: Color = Brand.AccentColor,
    val chartPalette: List<Color> = Brand.CHART_COLORS.map { Color(it.toInt()) },
    val muted: Color = Brand.TextTertiaryColor,
)

val LocalGisColors = staticCompositionLocalOf { GisExtendedColors() }

private val GisTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)

private val GisShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Tema de la aplicacion. Respeta el modo elegido en Ajustes
 * (sistema / claro / oscuro) y, en Android 12+, los colores dinamicos.
 */
@Composable
fun GisTheme(
    themeMode: ThemeMode = ThemeMode.SISTEMA,
    dynamicColors: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.CLARO -> false
        ThemeMode.OSCURO -> true
        ThemeMode.SISTEMA -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }
    val extended = GisExtendedColors(
        positive = if (darkTheme) Color(0xFF7BD6A0) else Brand.PositiveColor,
        negative = if (darkTheme) Color(0xFFFFB4AB) else Brand.NegativeColor,
        warning = if (darkTheme) Color(0xFFFFD08A) else Brand.WarningColor,
        accent = Brand.AccentColor,
        muted = if (darkTheme) Color(Brand.DARK_TEXT_SECONDARY.toInt()) else Brand.TextTertiaryColor
    )

    CompositionLocalProvider(LocalGisColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GisTypography,
            shapes = GisShapes,
            content = content
        )
    }
}

/** Acceso comodo a los colores semanticos desde cualquier composicion. */
object GisColors {
    val positive: Color
        @Composable @ReadOnlyComposable get() = LocalGisColors.current.positive

    val negative: Color
        @Composable @ReadOnlyComposable get() = LocalGisColors.current.negative

    val warning: Color
        @Composable @ReadOnlyComposable get() = LocalGisColors.current.warning

    val accent: Color
        @Composable @ReadOnlyComposable get() = LocalGisColors.current.accent

    val muted: Color
        @Composable @ReadOnlyComposable get() = LocalGisColors.current.muted

    val chartPalette: List<Color>
        @Composable @ReadOnlyComposable get() = LocalGisColors.current.chartPalette
}
