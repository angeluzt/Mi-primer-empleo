package com.angeluzt.miprimerempleo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.angeluzt.miprimerempleo.model.Ruta
import com.angeluzt.miprimerempleo.ui.EstadoApp
import com.angeluzt.miprimerempleo.ui.components.BloqueVista

/**
 * Primera pantalla. Aquí se gana o se pierde a la persona: primero la tesis
 * (por qué esto le sirve), y solo después se le pide elegir su etapa.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantallaBienvenida(
    estado: EstadoApp,
    onElegirPais: (String) -> Unit,
    onElegirRuta: (String) -> Unit,
) {
    val indice = estado.indice ?: return

    Scaffold { relleno ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        ) {
            item {
                Text(
                    text = "MI PRIMER EMPLEO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = indice.tesis.titulo,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(22.dp))
            }

            items(indice.tesis.bloques) { bloque ->
                BloqueVista(
                    bloque = bloque,
                    accionesHechas = emptySet(),
                    onAccion = { _, _ -> },
                    onEnlace = {},
                    pais = estado.progreso.pais,
                )
            }

            if (indice.paises.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "¿Desde dónde buscas?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Las instituciones, prestaciones y bolsas de trabajo cambian por país.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        indice.paises.forEach { pais ->
                            FilterChip(
                                selected = estado.progreso.pais == pais.codigo,
                                onClick = { onElegirPais(pais.codigo) },
                                label = { Text(pais.nombre) },
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "¿En dónde estás ahora?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Elige tu etapa y la guía se reordena para ti. Puedes cambiarla después.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
                )
            }

            items(indice.rutas) { ruta ->
                TarjetaRuta(ruta) { onElegirRuta(ruta.id) }
            }

            item {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Pago único, sin suscripciones y sin anuncios que te interrumpan. La primera parte de cada módulo y todo «Dónde buscar» son gratis.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TarjetaRuta(ruta: Ruta, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = ruta.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = ruta.subtitulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
