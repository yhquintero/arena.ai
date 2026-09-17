package com.gis.supermercados.core.designsystem

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * Graficos dibujados a mano con Canvas de Compose.
 *
 * No se usa ninguna libreria externa: menos dependencias, menos peso en el APK
 * y control total del estilo para que coincida con los informes exportados.
 */

data class ChartPoint(val label: String, val value: Double)

data class ChartSlice(val label: String, val value: Double, val color: Color? = null)

/** Grafico de lineas con area (series diarias de ventas, flujo de caja...). */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    areaColor: Color = lineColor.copy(alpha = 0.16f),
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueFormatter: (Double) -> String = { it.toString() },
    maxVisibleLabels: Int = 6,
    height: Dp = 180.dp,
) {
    if (points.isEmpty()) return
    val maxValue = max(points.maxOf { it.value }, 1.0)
    val minValue = min(points.minOf { it.value }, 0.0)
    val labelPaint = rememberAxisPaint(labelColor)
    val step = max(points.size / maxVisibleLabels, 1)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val leftPad = 8f
            val bottomPad = 28f
            val chartWidth = size.width - leftPad * 2
            val chartHeight = size.height - bottomPad
            val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0

            // Rejilla horizontal y etiquetas de valor.
            for (gridIndex in 0..GRID_LINES) {
                val y = chartHeight - chartHeight * gridIndex / GRID_LINES
                drawLine(gridColor, Offset(leftPad, y), Offset(size.width - leftPad, y), strokeWidth = 1f)
                val value = minValue + range * gridIndex / GRID_LINES
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        valueFormatter(value),
                        leftPad,
                        (y - 4f).coerceAtLeast(12f),
                        labelPaint
                    )
                }
            }

            fun xAt(index: Int): Float =
                if (points.size == 1) leftPad + chartWidth / 2f
                else leftPad + chartWidth * index / (points.size - 1)

            fun yAt(value: Double): Float =
                chartHeight - (chartHeight * ((value - minValue) / range)).toFloat()

            val linePath = Path()
            val areaPath = Path()
            points.forEachIndexed { index, point ->
                val x = xAt(index)
                val y = yAt(point.value)
                if (index == 0) {
                    linePath.moveTo(x, y)
                    areaPath.moveTo(x, chartHeight)
                    areaPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    areaPath.lineTo(x, y)
                }
            }
            areaPath.lineTo(xAt(points.lastIndex), chartHeight)
            areaPath.close()

            drawPath(areaPath, areaColor)
            drawPath(linePath, lineColor, style = Stroke(width = 3.5f))
            points.forEachIndexed { index, point ->
                drawCircle(lineColor, radius = if (points.size > 40) 2f else 4f, center = Offset(xAt(index), yAt(point.value)))
                if (index % step == 0 || index == points.lastIndex) {
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            point.label,
                            xAt(index) - 18f,
                            size.height - 6f,
                            labelPaint
                        )
                    }
                }
            }
        }
    }
}

/** Grafico de barras verticales (ventas por tienda, gastos por categoria...). */
@Composable
fun BarChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueFormatter: (Double) -> String = { it.toString() },
    height: Dp = 200.dp,
) {
    if (points.isEmpty()) return
    val maxValue = max(points.maxOf { it.value }, 1.0)
    val labelPaint = rememberAxisPaint(labelColor)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val leftPad = 8f
            val bottomPad = 34f
            val chartHeight = size.height - bottomPad
            val slot = size.width / points.size
            val barWidth = min(slot * 0.62f, 90f)

            for (gridIndex in 0..GRID_LINES) {
                val y = chartHeight - chartHeight * gridIndex / GRID_LINES
                drawLine(gridColor, Offset(leftPad, y), Offset(size.width - leftPad, y), strokeWidth = 1f)
            }

            points.forEachIndexed { index, point ->
                val barHeight = (chartHeight * (point.value / maxValue)).toFloat()
                val left = index * slot + (slot - barWidth) / 2f
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, chartHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        point.label,
                        left + barWidth / 2f - 20f,
                        size.height - 16f,
                        labelPaint
                    )
                    canvas.nativeCanvas.drawText(
                        valueFormatter(point.value),
                        left + barWidth / 2f - 26f,
                        (chartHeight - barHeight - 6f).coerceAtLeast(18f),
                        labelPaint
                    )
                }
            }
        }
    }
}

/** Grafico de dona (reparto por metodo de pago, categorias...). */
@Composable
fun DonutChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    centerTitle: String = "",
    centerSubtitle: String = "",
    palette: List<Color> = GisColors.chartPalette,
) {
    val total = slices.sumOf { it.value }
    if (total <= 0.0) return
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(170.dp)) {
            var startAngle = -90f
            val stroke = 34f
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            slices.forEachIndexed { index, slice ->
                val sweep = (slice.value / total * 360.0).toFloat()
                drawArc(
                    color = slice.color ?: palette[index.mod(palette.size)],
                    startAngle = startAngle,
                    sweepAngle = sweep - GAP_DEGREES,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = stroke)
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (centerTitle.isNotEmpty()) {
                Text(centerTitle, style = MaterialTheme.typography.titleMedium)
            }
            if (centerSubtitle.isNotEmpty()) {
                Text(
                    centerSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Leyenda de colores para acompanar a [DonutChart] y [BarChart]. */
@Composable
fun ChartLegend(
    entries: List<Pair<String, Color>>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        entries.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(color, androidx.compose.foundation.shape.CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Barra de progreso sencilla para participaciones (share) en tablas. */
@Composable
fun ShareBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .background(color, androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun rememberAxisPaint(color: Color): Paint {
    val density = LocalDensity.current.density
    return androidx.compose.runtime.remember(color, density) {
        Paint().apply {
            isAntiAlias = true
            this.color = android.graphics.Color.argb(
                (color.alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
            textSize = AXIS_TEXT_SP * density
        }
    }
}

private const val AXIS_TEXT_SP = 10f
private const val GRID_LINES = 4
private const val GAP_DEGREES = 1.6f
