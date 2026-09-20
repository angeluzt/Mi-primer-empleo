package com.angeluzt.miprimerempleo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.angeluzt.miprimerempleo.model.AccionBloque
import com.angeluzt.miprimerempleo.model.Alerta
import com.angeluzt.miprimerempleo.model.Banderas
import com.angeluzt.miprimerempleo.model.Bloque
import com.angeluzt.miprimerempleo.model.Cita
import com.angeluzt.miprimerempleo.model.Columna
import com.angeluzt.miprimerempleo.model.Comparacion
import com.angeluzt.miprimerempleo.model.Lista
import com.angeluzt.miprimerempleo.model.Parrafo
import com.angeluzt.miprimerempleo.model.Recursos
import com.angeluzt.miprimerempleo.model.Subtitulo
import com.angeluzt.miprimerempleo.model.Tabla

@Composable
fun BloqueVista(
    bloque: Bloque,
    accionesHechas: Set<String>,
    onAccion: (String, Int) -> Unit,
    onEnlace: (String) -> Unit,
) {
    when (bloque) {
        is Parrafo -> Text(
            text = conNegritas(bloque.texto),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 18.dp),
        )

        is Subtitulo -> Text(
            text = bloque.texto,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
        )

        is Cita -> CitaVista(bloque.texto)
        is Lista -> ListaVista(bloque)
        is Tabla -> TablaVista(bloque)
        is Alerta -> AlertaVista(bloque)
        is Banderas -> BanderasVista(bloque)
        is Comparacion -> ComparacionVista(bloque)
        is Recursos -> RecursosVista(bloque, onEnlace)
        is AccionBloque -> AccionVista(bloque, bloque.accionId in accionesHechas, onAccion)
    }
}

@Composable
private fun CitaVista(texto: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .background(
                MaterialTheme.colorScheme.primaryContainer,
                RoundedCornerShape(14.dp),
            )
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(IntrinsicMinHeight)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)),
        )
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(16.dp),
        )
    }
}

private val IntrinsicMinHeight = 56.dp

@Composable
private fun ListaVista(lista: Lista) {
    Column(Modifier.padding(bottom = 18.dp)) {
        lista.items.forEachIndexed { indice, item ->
            Row(Modifier.padding(bottom = 10.dp)) {
                Text(
                    text = if (lista.ordenada) "${indice + 1}." else "•",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(26.dp),
                )
                Text(
                    text = conNegritas(item),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun TablaVista(tabla: Tabla) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 11.dp),
        ) {
            tabla.encabezados.forEachIndexed { indice, encabezado ->
                Text(
                    text = encabezado,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(if (indice == 0) 1f else 1.4f),
                )
            }
        }
        tabla.filas.forEach { fila ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                fila.forEachIndexed { indice, celda ->
                    Text(
                        text = conNegritas(celda),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(if (indice == 0) 1f else 1.4f)
                            .padding(end = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertaVista(alerta: Alerta) {
    val esquema = MaterialTheme.colorScheme
    val (fondo, acento) = when (alerta.nivel) {
        "peligro" -> esquema.errorContainer to esquema.error
        "aviso" -> esquema.secondaryContainer to esquema.secondary
        "clave" -> esquema.primaryContainer to esquema.primary
        else -> esquema.tertiaryContainer to esquema.tertiary
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        colors = CardDefaults.cardColors(containerColor = fondo),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(Modifier.padding(16.dp)) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(IntrinsicMinHeight)
                    .background(acento, RoundedCornerShape(2.dp)),
            )
            Column(Modifier.padding(start = 14.dp)) {
                Text(
                    text = alerta.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = acento,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = conNegritas(alerta.texto),
                    style = MaterialTheme.typography.bodyMedium,
                    color = esquema.onSurface,
                )
            }
        }
    }
}

@Composable
private fun BanderasVista(banderas: Banderas) {
    Column(Modifier.padding(bottom = 20.dp)) {
        ListaBandera("Banderas rojas", banderas.rojas, MaterialTheme.colorScheme.error, "!", MaterialTheme.colorScheme.errorContainer)
        Spacer(Modifier.height(12.dp))
        ListaBandera("Banderas verdes", banderas.verdes, MaterialTheme.colorScheme.tertiary, "+", MaterialTheme.colorScheme.tertiaryContainer)
    }
}

@Composable
private fun ListaBandera(
    titulo: String,
    items: List<String>,
    acento: Color,
    simbolo: String,
    fondo: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = fondo),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                color = acento,
            )
            Spacer(Modifier.height(10.dp))
            items.forEach { item ->
                Row(Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = simbolo,
                        style = MaterialTheme.typography.bodyLarge,
                        color = acento,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(22.dp),
                    )
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparacionVista(comparacion: Comparacion) {
    Column(Modifier.padding(bottom = 20.dp)) {
        Text(
            text = comparacion.titulo,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ColumnaVista(comparacion.izquierda, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            ColumnaVista(comparacion.derecha, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ColumnaVista(columna: Columna, acento: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, acento.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(columna.nombre, style = MaterialTheme.typography.headlineSmall, color = acento)
            Text(
                columna.etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            columna.puntos.forEach {
                Text(
                    text = "· $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun RecursosVista(recursos: Recursos, onEnlace: (String) -> Unit) {
    Column(Modifier.padding(bottom = 20.dp)) {
        recursos.items.forEach { recurso ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clickable { onEnlace(recurso.url) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = recurso.nombre,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (recurso.gratis) Etiqueta("GRATIS", MaterialTheme.colorScheme.tertiary)
                            // Transparencia obligatoria: si ganamos comisión, se dice.
                            if (recurso.afiliado) Etiqueta("AFILIADO", MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = recurso.descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = "Abrir",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(start = 8.dp),
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun AccionVista(
    accion: AccionBloque,
    hecha: Boolean,
    onAccion: (String, Int) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .clickable { onAccion(accion.accionId, accion.puntos) },
        colors = CardDefaults.cardColors(
            containerColor = if (hecha) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (hecha) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (hecha) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (hecha) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(
                    text = accion.texto,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "+${accion.puntos} puntos de empleabilidad",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Soporta **negritas** en el contenido sin arrastrar una librería de Markdown. */
@Composable
fun conNegritas(texto: String) = buildAnnotatedString {
    val partes = texto.split("**")
    partes.forEachIndexed { indice, parte ->
        if (indice % 2 == 1) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(parte) }
        } else {
            append(parte)
        }
    }
}
