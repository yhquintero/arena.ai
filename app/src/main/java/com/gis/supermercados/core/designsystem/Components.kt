package com.gis.supermercados.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gis.supermercados.core.common.AppConstants
import com.gis.supermercados.core.common.Money
import com.gis.supermercados.core.common.UiText

/**
 * Piezas reutilizables de interfaz (Material 3). Todas las pantallas usan estos
 * componentes para mantener una identidad visual coherente.
 */

/** Barra superior con titulo, subtitulo opcional, boton atras y acciones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GisTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/** Estado vacio (sin datos) con icono, mensaje y accion opcional. */
@Composable
fun EmptyState(
    title: String,
    message: String,
    icon: ImageVector = Icons.Rounded.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

/** Estado de error con reintento. */
@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        title = "Algo salio mal",
        message = message,
        icon = Icons.Rounded.ErrorOutline,
        actionLabel = onRetry?.let { "Reintentar" },
        onAction = onRetry,
        modifier = modifier
    )
}

/** Cargador a pantalla completa. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Tarjeta con titulo y contenido, base de casi todas las secciones. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: @Composable (RowScope.() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(contentPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                action?.invoke(this)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** Tarjeta KPI (ventas del dia, margen, gastos...). */
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    icon: ImageVector? = null,
    trendPercent: Double? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Box(
                        Modifier.size(28.dp).background(accent.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, Modifier.size(16.dp), tint = accent)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trendPercent != null) {
                    val up = trendPercent >= 0
                    Text(
                        text = "${if (up) "▲" else "▼"} ${percentText(trendPercent)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (up) GisColors.positive else GisColors.negative
                    )
                    Spacer(Modifier.width(6.dp))
                }
                if (!caption.isNullOrBlank()) {
                    Text(
                        caption,
                        style = MaterialTheme.typography.labelSmall,
                        color = GisColors.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Importe formateado con color segun signo (verde ganancias, rojo perdidas). */
@Composable
fun AmountText(
    cents: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    colored: Boolean = true,
    compact: Boolean = false,
    symbol: String = AppConstants.DEFAULT_CURRENCY_SYMBOL,
) {
    val text = if (compact) Money.formatCompact(cents, symbol) else Money.format(cents, symbol)
    val color = when {
        !colored -> MaterialTheme.colorScheme.onSurface
        cents > 0 -> GisColors.positive
        cents < 0 -> GisColors.negative
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text, modifier = modifier, style = style, color = color, maxLines = 1)
}

/** Etiqueta de estado coloreada (COMPLETADA, DEVUELTA, bajo stock...). */
@Composable
fun StatusChip(
    text: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White,
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1
        )
    }
}

/** Campo de texto para importes (acepta 1234,56 / 1.234,56 / $1.234). */
@Composable
fun AmountField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() || it == ',' || it == '.' }) },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        isError = isError,
        enabled = enabled,
        supportingText = supportingText?.let { { Text(it) } }
    )
}

/** Buscador reutilizable de las listas. */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Buscar...",
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = MaterialTheme.shapes.medium
    )
}

/** Fila de chips de filtro (periodo, categoria, estado...). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FilterChipRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(options.size) { index ->
            val option = options[index]
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) }
            )
        }
    }
}

/** Fila de ajuste (icono + titulo + subtitulo + control a la derecha). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        onClick = onClick ?: {}
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

/** Dialogo de confirmacion reutilizable (borrar, anular venta, restaurar...). */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Cancelar",
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    color = if (destructive) GisColors.negative else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } }
    )
}

/** Lista con estados de carga/vacio/error integrados. */
@Composable
fun <T> StatefulList(
    items: List<T>,
    isLoading: Boolean,
    emptyTitle: String,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    key: ((T) -> Any)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    when {
        isLoading && items.isEmpty() -> LoadingState(modifier)
        items.isEmpty() -> EmptyState(emptyTitle, emptyMessage, modifier = modifier)
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items.size, key = key?.let { keyOf -> { index -> keyOf(items[index]) } }) { index ->
                itemContent(items[index])
            }
        }
    }
}

/** Convierte un [UiText] (recurso o texto dinamico) en cadena mostrable. */
@Composable
fun rememberUiText(text: UiText?): String {
    val context = LocalContext.current
    return androidx.compose.runtime.remember(text, context) { text?.asString(context).orEmpty() }
}

/** Texto de porcentaje con signo y un decimal (usado en tarjetas KPI). */
fun percentText(value: Double): String =
    (if (value > 0) "+" else "") + String.format(java.util.Locale("es"), "%.1f %%", value)

/** Barra de progreso indeterminada pequena (acciones en curso). */
@Composable
fun InlineLoader(visible: Boolean, modifier: Modifier = Modifier) {
    if (visible) {
        Box(modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        }
    }
}
