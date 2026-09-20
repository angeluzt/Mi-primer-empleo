package com.angeluzt.miprimerempleo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.angeluzt.miprimerempleo.model.ModuloMeta
import com.angeluzt.miprimerempleo.ui.EstadoApp

@Composable
fun PantallaRuta(
    estado: EstadoApp,
    onModulo: (String) -> Unit,
    onCv: () -> Unit,
    onPaywall: () -> Unit,
) {
    val ruta = estado.ruta ?: return

    Scaffold { relleno ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp,
            ),
        ) {
            item { TarjetaNivel(estado) }
            item {
                Spacer(Modifier.height(16.dp))
                TarjetaCv(estado, onCv)
                Spacer(Modifier.height(22.dp))
                Text(
                    text = "Tu ruta",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = ruta.mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
                )
            }

            items(estado.modulosEnOrden) { modulo ->
                TarjetaModulo(
                    modulo = modulo,
                    avance = estado.avanceDe(modulo.id),
                    desbloqueado = estado.compras.puedeLeerTodo || modulo.gratis,
                    onClick = { onModulo(modulo.id) },
                )
            }

            if (!estado.compras.puedeLeerTodo) {
                item {
                    Spacer(Modifier.height(10.dp))
                    TarjetaPase(estado, onPaywall)
                }
            }
        }
    }
}

@Composable
private fun TarjetaNivel(estado: EstadoApp) {
    val nivel = estado.nivel
    val siguiente = estado.siguienteNivel
    val puntos = estado.progreso.puntos
    val avance = if (siguiente != null && nivel != null) {
        val rango = (siguiente.puntosMinimos - nivel.puntosMinimos).coerceAtLeast(1)
        ((puntos - nivel.puntosMinimos).toFloat() / rango).coerceIn(0f, 1f)
    } else 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "NIVEL ${nivel?.nivel ?: 0} DE EMPLEABILIDAD",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
            )
            Text(
                text = nivel?.titulo.orEmpty(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = nivel?.descripcion.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
            )
            LinearProgressIndicator(
                progress = { avance },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
            )
            Text(
                text = if (siguiente != null) {
                    "$puntos puntos · te faltan ${siguiente.puntosMinimos - puntos} para «${siguiente.titulo}»"
                } else {
                    "$puntos puntos · nivel máximo alcanzado"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun TarjetaCv(estado: EstadoApp, onCv: () -> Unit) {
    val tienePase = estado.compras.tienePase
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCv),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(30.dp),
            )
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(
                    text = "Arma tu CV con IA",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = if (tienePase) {
                        "Te quedan ${estado.compras.creditosCv(estado.progreso.cvsGenerados)} generaciones"
                    } else {
                        "Gratis de armar. Se paga solo al exportar el PDF."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun TarjetaModulo(
    modulo: ModuloMeta,
    avance: Float,
    desbloqueado: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        if (avance >= 1f) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(11.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${modulo.numero}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (avance >= 1f) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary,
                )
            }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = modulo.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (modulo.gratis) {
                        Insignia("GRATIS", MaterialTheme.colorScheme.tertiary)
                    }
                }
                Text(
                    text = modulo.subtitulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (avance > 0f) {
                    LinearProgressIndicator(
                        progress = { avance },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            if (!desbloqueado) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Con el Pase Completo",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
private fun Insignia(texto: String, color: Color) {
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
private fun TarjetaPase(estado: EstadoApp, onPaywall: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPaywall),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Pase Completo",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "Un solo pago. Los 9 módulos, el generador de CV y todas las herramientas, para siempre.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = estado.compras.precios[com.angeluzt.miprimerempleo.billing.Productos.PASE_COMPLETO]
                        ?: "Ver precio",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Sin suscripción",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
