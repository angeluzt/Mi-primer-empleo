package com.angeluzt.miprimerempleo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.angeluzt.miprimerempleo.ui.EstadoApp
import com.angeluzt.miprimerempleo.ui.components.BloqueVista

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLector(
    estado: EstadoApp,
    moduloId: String,
    capituloId: String,
    onLeido: (String) -> Unit,
    onAccion: (String, Int) -> Unit,
    onEnlace: (String) -> Unit,
    onPaywall: () -> Unit,
    onAtras: () -> Unit,
) {
    val meta = estado.indice?.modulos?.firstOrNull { it.id == moduloId } ?: return
    val capitulo = estado.capitulosDe(moduloId).firstOrNull { it.id == capituloId } ?: return
    val desbloqueado = estado.capituloDesbloqueado(meta, capitulo)

    LaunchedEffect(capituloId, desbloqueado) {
        if (desbloqueado) onLeido(capituloId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(meta.titulo, style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
    ) { relleno ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp,
            ),
        ) {
            item {
                Text(
                    text = capitulo.titulo,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${capitulo.minutos} min de lectura",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 22.dp),
                )
            }

            if (desbloqueado) {
                items(capitulo.bloques) { bloque ->
                    BloqueVista(
                        bloque = bloque,
                        accionesHechas = estado.progreso.accionesHechas,
                        onAccion = onAccion,
                        onEnlace = onEnlace,
                    )
                }
            } else {
                // Se muestra el primer bloque y se corta: la persona ve que hay algo real detrás.
                capitulo.bloques.take(1).forEach { bloque ->
                    item {
                        BloqueVista(
                            bloque = bloque,
                            accionesHechas = emptySet(),
                            onAccion = { _, _ -> },
                            onEnlace = onEnlace,
                        )
                    }
                }
                item { CorteDePago(onPaywall) }
            }
        }
    }
}

@Composable
private fun CorteDePago(onPaywall: () -> Unit) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(22.dp)) {
            Text(
                text = "Aquí sigue lo importante",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Con el Pase Completo desbloqueas este capítulo y todos los demás, para siempre. Un solo pago, sin suscripción.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onPaywall,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Ver el Pase Completo")
            }
        }
    }
}
