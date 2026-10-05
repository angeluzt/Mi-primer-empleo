package com.angeluzt.miprimerempleo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.angeluzt.miprimerempleo.billing.Productos
import com.angeluzt.miprimerempleo.model.ModuloMeta
import com.angeluzt.miprimerempleo.ui.EstadoApp
import com.angeluzt.miprimerempleo.ui.Siguiente
import com.angeluzt.miprimerempleo.ui.components.AccesoRapido
import com.angeluzt.miprimerempleo.ui.components.AnilloProgreso
import com.angeluzt.miprimerempleo.ui.components.BarraProgreso
import com.angeluzt.miprimerempleo.ui.components.EncabezadoSeccion
import com.angeluzt.miprimerempleo.ui.components.Hueco
import com.angeluzt.miprimerempleo.ui.components.HuecoH
import com.angeluzt.miprimerempleo.ui.components.IconoEnCaja
import com.angeluzt.miprimerempleo.ui.components.Insignia
import com.angeluzt.miprimerempleo.ui.components.TarjetaMarca
import com.angeluzt.miprimerempleo.ui.components.TarjetaSuave
import com.angeluzt.miprimerempleo.ui.theme.DegradadoCalido
import com.angeluzt.miprimerempleo.ui.theme.SobreMarcaSuave

/**
 * La pantalla a la que la persona vuelve todos los días. Responde tres preguntas, en ese orden:
 * cómo voy, qué sigue, y con qué herramienta lo hago. La lista de módulos va después: es el
 * mapa, no el siguiente paso.
 */
@Composable
fun PantallaRuta(
    estado: EstadoApp,
    onModulo: (String) -> Unit,
    onCapitulo: (String, String) -> Unit,
    onCv: () -> Unit,
    onVacante: () -> Unit,
    onBitacora: () -> Unit,
    onPaywall: () -> Unit,
    onAjustes: () -> Unit,
) {
    val ruta = estado.ruta ?: return

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { relleno ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Encabezado(ruta.titulo, onAjustes) }
            item { TarjetaNivel(estado) }
            estado.siguiente?.let { siguiente ->
                item { TarjetaSiguiente(siguiente) { onCapitulo(siguiente.modulo.id, siguiente.capitulo.id) } }
            }

            item {
                Hueco(8.dp)
                EncabezadoSeccion("Tus herramientas")
            }
            item { TarjetaCv(estado, onCv) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccesoRapido(
                        icono = Icons.Default.TravelExplore,
                        titulo = "Revisar una vacante",
                        detalle = "¿Es real? ¿Qué te falta? Tu CV adaptado.",
                        color = MaterialTheme.colorScheme.primary,
                        insignia = "Nuevo",
                        modifier = Modifier.weight(1f),
                        onClick = onVacante,
                    )
                    AccesoRapido(
                        icono = Icons.Default.QuestionAnswer,
                        titulo = "Bitácora",
                        detalle = when {
                            estado.entrevistas.isEmpty() -> "Aprende de cada entrevista que tengas."
                            estado.porEstudiar > 0 ->
                                "${estado.entrevistas.size} entrevistas · ${estado.porEstudiar} por estudiar"
                            else -> "${estado.entrevistas.size} entrevistas · todo preparado"
                        },
                        color = MaterialTheme.colorScheme.tertiary,
                        insignia = if (estado.compras.tienePase) null else "Pase",
                        modifier = Modifier.weight(1f),
                        onClick = if (estado.compras.tienePase) onBitacora else onPaywall,
                    )
                }
            }

            item {
                Hueco(8.dp)
                EncabezadoSeccion(titulo = "Tu ruta", subtitulo = ruta.mensaje)
            }
            items(estado.modulosEnOrden, key = { it.id }) { modulo ->
                FilaModulo(
                    modulo = modulo,
                    avance = estado.avanceDe(modulo.id),
                    desbloqueado = estado.compras.puedeLeerTodo || modulo.gratis,
                    onClick = { onModulo(modulo.id) },
                )
            }

            if (!estado.compras.puedeLeerTodo) {
                item {
                    Hueco(4.dp)
                    TarjetaPase(estado, onPaywall)
                }
            }
        }
    }
}

@Composable
private fun Encabezado(ruta: String, onAjustes: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "CONSEGUIR TRABAJO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Tu camino",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = ruta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAjustes),
            )
        }
        IconButton(onClick = onAjustes) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Ajustes",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TarjetaNivel(estado: EstadoApp) {
    val nivel = estado.nivel
    val siguiente = estado.siguienteNivel
    val puntos = estado.progreso.puntos

    TarjetaMarca {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnilloProgreso(
                valor = estado.avanceDeNivel,
                tamano = 62.dp,
                grosor = 6.dp,
                color = Color.White,
                fondo = Color.White.copy(alpha = 0.25f),
            ) {
                Text(
                    text = "${nivel?.nivel ?: 0}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
            }
            HuecoH(16.dp)
            Column {
                Text(
                    text = "NIVEL DE EMPLEABILIDAD",
                    style = MaterialTheme.typography.labelSmall,
                    color = SobreMarcaSuave,
                )
                Text(
                    text = nivel?.titulo.orEmpty(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
            }
        }
        Text(
            text = nivel?.descripcion.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = SobreMarcaSuave,
            modifier = Modifier.padding(top = 12.dp, bottom = 14.dp),
        )
        BarraProgreso(
            valor = estado.avanceDeNivel,
            color = Color.White,
            fondo = Color.White.copy(alpha = 0.25f),
        )
        Text(
            text = if (siguiente != null) {
                "$puntos puntos · te faltan ${siguiente.puntosMinimos - puntos} para «${siguiente.titulo}»"
            } else {
                "$puntos puntos · nivel máximo"
            },
            style = MaterialTheme.typography.labelMedium,
            color = SobreMarcaSuave,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun TarjetaSiguiente(siguiente: Siguiente, onClick: () -> Unit) {
    TarjetaSuave(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoEnCaja(Icons.AutoMirrored.Filled.MenuBook, MaterialTheme.colorScheme.primary)
            HuecoH(14.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = "SIGUE AQUÍ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = siguiente.capitulo.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${siguiente.modulo.titulo} · ${siguiente.capitulo.minutos} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HuecoH(8.dp)
            Icon(
                imageVector = if (siguiente.desbloqueado) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TarjetaCv(estado: EstadoApp, onCv: () -> Unit) {
    val listo = estado.cv != null
    TarjetaMarca(fondo = DegradadoCalido, onClick = onCv) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoEnCaja(
                icono = Icons.Default.Description,
                color = Color.White,
                fondo = Color.White.copy(alpha = 0.2f),
            )
            HuecoH(14.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (listo) "Tu CV" else "Arma tu CV con IA",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    text = when {
                        listo -> "En español e inglés. Revísalo, edítalo o descárgalo."
                        estado.compras.tienePase -> "Once preguntas y queda en español e inglés."
                        else -> "Gratis de armar. Se paga solo al descargar el PDF."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SobreMarcaSuave,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
            )
        }
        if (estado.compras.tienePase) {
            Text(
                text = "Te quedan ${estado.creditosCv} generaciones",
                style = MaterialTheme.typography.labelMedium,
                color = SobreMarcaSuave,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun FilaModulo(
    modulo: ModuloMeta,
    avance: Float,
    desbloqueado: Boolean,
    onClick: () -> Unit,
) {
    val completo = avance >= 1f
    TarjetaSuave(onClick = onClick, relleno = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnilloProgreso(
                valor = avance,
                tamano = 44.dp,
                grosor = 4.dp,
                color = if (completo) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            ) {
                if (completo) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Terminado",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(
                        text = "${modulo.numero}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            HuecoH(14.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = modulo.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (modulo.gratis) {
                        HuecoH(8.dp)
                        Insignia("Gratis", MaterialTheme.colorScheme.tertiary)
                    }
                }
                Text(
                    text = modulo.subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (!desbloqueado) {
                HuecoH(8.dp)
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Con un pase",
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun TarjetaPase(estado: EstadoApp, onPaywall: () -> Unit) {
    TarjetaSuave(onClick = onPaywall, relleno = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoEnCaja(Icons.Default.WorkspacePremium, MaterialTheme.colorScheme.secondary)
            HuecoH(14.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Pase Completo",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Un solo pago. Sin suscripción.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "Los ${estado.indice?.modulos?.size ?: 10} módulos, tu CV en PDF, adaptarlo a cada " +
                "vacante y la bitácora de entrevistas. Para siempre.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 12.dp, bottom = 14.dp),
        )
        Button(
            onClick = onPaywall,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
        ) {
            Text(
                estado.compras.precios[Productos.PASE_COMPLETO]?.let { "Ver el pase · $it" } ?: "Ver el pase",
            )
        }
    }
}
