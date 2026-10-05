package com.angeluzt.miprimerempleo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.angeluzt.miprimerempleo.ui.theme.DegradadoMarca

/*
 * Piezas visuales compartidas. Viven aquí para que todas las pantallas hablen el mismo idioma:
 * mismas esquinas, mismos tonos, mismo espaciado. Antes cada pantalla dibujaba su propia
 * insignia y su propia tarjeta, con radios y colores que no coincidían.
 */

/**
 * Barra de progreso propia. La de Material 3 dibuja un hueco entre lo avanzado y lo que falta
 * y un punto al final: en un teléfono parece una barra rota.
 */
@Composable
fun BarraProgreso(
    valor: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    fondo: Color = MaterialTheme.colorScheme.surfaceVariant,
    alto: Dp = 8.dp,
) {
    val avance = valor.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(alto)
            .clip(CircleShape)
            .background(fondo),
    ) {
        if (avance > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(avance)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/** Anillo de avance, con lo que se quiera en el centro (un porcentaje, un número). */
@Composable
fun AnilloProgreso(
    valor: Float,
    modifier: Modifier = Modifier,
    tamano: Dp = 56.dp,
    grosor: Dp = 6.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    fondo: Color = MaterialTheme.colorScheme.surfaceVariant,
    centro: @Composable () -> Unit = {},
) {
    Box(modifier.size(tamano), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val trazo = grosor.toPx()
            val esquina = Offset(trazo / 2, trazo / 2)
            val area = Size(size.width - trazo, size.height - trazo)
            drawArc(fondo, -90f, 360f, false, esquina, area, style = Stroke(trazo))
            drawArc(
                color, -90f, 360f * valor.coerceIn(0f, 1f), false, esquina, area,
                style = Stroke(trazo, cap = StrokeCap.Round),
            )
        }
        centro()
    }
}

/** La tarjeta de marca, con el degradado índigo-violeta. Para lo que celebra o lo que importa. */
@Composable
fun TarjetaMarca(
    modifier: Modifier = Modifier,
    fondo: Brush = DegradadoMarca,
    onClick: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(fondo)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(Modifier.padding(20.dp), content = contenido)
    }
}

/** Tarjeta blanca con borde fino: la base de casi todo lo que se toca. */
@Composable
fun TarjetaSuave(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    relleno: Dp = 16.dp,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(relleno), content = contenido)
    }
}

/** Un ícono dentro de un cuadro redondeado del mismo tono, más claro. */
@Composable
fun IconoEnCaja(
    icono: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    fondo: Color = color.copy(alpha = 0.12f),
    tamano: Dp = 44.dp,
) {
    Box(
        modifier
            .size(tamano)
            .clip(RoundedCornerShape(tamano * 0.32f))
            .background(fondo),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(tamano * 0.5f))
    }
}

/** Etiqueta corta en mayúsculas: GRATIS, RECOMENDADO, NUEVO. */
@Composable
fun Insignia(texto: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

/** Título de sección, con una acción opcional a la derecha ("Ver todo"). */
@Composable
fun EncabezadoSeccion(
    titulo: String,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    accion: String? = null,
    onAccion: () -> Unit = {},
) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (accion != null) {
                TextButton(onClick = onAccion) { Text(accion) }
            }
        }
        if (subtitulo != null) {
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Acceso directo de la pantalla de inicio: ícono, nombre y una línea de por qué entrar. */
@Composable
fun AccesoRapido(
    icono: ImageVector,
    titulo: String,
    detalle: String,
    color: Color,
    modifier: Modifier = Modifier,
    insignia: String? = null,
    onClick: () -> Unit,
) {
    TarjetaSuave(modifier = modifier, onClick = onClick, relleno = 14.dp) {
        Row(verticalAlignment = Alignment.Top) {
            IconoEnCaja(icono, color, tamano = 40.dp)
            Spacer(Modifier.weight(1f))
            if (insignia != null) Insignia(insignia, color)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = detalle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
            minLines = 2,
            maxLines = 2,
        )
    }
}

enum class TonoAviso { INFO, EXITO, ALERTA, PELIGRO, SEGURIDAD }

/** Aviso con tono: información, logro, cuidado, peligro o protección de datos. */
@Composable
fun Aviso(
    tono: TonoAviso,
    texto: String,
    modifier: Modifier = Modifier,
    titulo: String? = null,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val esquema = MaterialTheme.colorScheme
    val (fondo, frente, acento) = when (tono) {
        TonoAviso.INFO -> Triple(esquema.primaryContainer, esquema.onPrimaryContainer, esquema.primary)
        TonoAviso.EXITO -> Triple(esquema.tertiaryContainer, esquema.onTertiaryContainer, esquema.tertiary)
        TonoAviso.ALERTA -> Triple(esquema.secondaryContainer, esquema.onSecondaryContainer, esquema.secondary)
        TonoAviso.PELIGRO -> Triple(esquema.errorContainer, esquema.onErrorContainer, esquema.error)
        TonoAviso.SEGURIDAD -> Triple(esquema.primaryContainer, esquema.onPrimaryContainer, esquema.primary)
    }
    val icono = when (tono) {
        TonoAviso.INFO -> Icons.Default.Info
        TonoAviso.EXITO -> Icons.Default.CheckCircle
        TonoAviso.ALERTA, TonoAviso.PELIGRO -> Icons.Default.WarningAmber
        TonoAviso.SEGURIDAD -> Icons.Default.Shield
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(fondo)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icono, contentDescription = null, tint = acento, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            if (titulo != null) {
                Text(titulo, style = MaterialTheme.typography.titleSmall, color = frente)
                Spacer(Modifier.height(2.dp))
            }
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = frente)
            extra()
        }
    }
}

/** Separador de aire entre bloques de una lista: más legible que una línea. */
@Composable
fun Hueco(alto: Dp = 16.dp) = Spacer(Modifier.height(alto))

/** Espacio horizontal fijo, para filas con ícono y texto. */
@Composable
fun HuecoH(ancho: Dp = 12.dp) = Spacer(Modifier.width(ancho))
