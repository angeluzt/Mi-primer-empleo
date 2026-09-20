package com.angeluzt.miprimerempleo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.angeluzt.miprimerempleo.model.Capitulo
import com.angeluzt.miprimerempleo.ui.EstadoApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaModulo(
    estado: EstadoApp,
    moduloId: String,
    onCapitulo: (String, String) -> Unit,
    onPaywall: () -> Unit,
    onAtras: () -> Unit,
) {
    val meta = estado.indice?.modulos?.firstOrNull { it.id == moduloId } ?: return
    val modulo = estado.modulos[moduloId] ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(meta.titulo, style = MaterialTheme.typography.titleMedium) },
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        ) {
            item {
                Text(
                    text = "MÓDULO ${meta.numero}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = modulo.intro,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                )
            }

            items(modulo.capitulos) { capitulo ->
                TarjetaCapitulo(
                    capitulo = capitulo,
                    leido = capitulo.id in estado.progreso.capitulosLeidos,
                    desbloqueado = estado.capituloDesbloqueado(meta, capitulo),
                    onClick = {
                        if (estado.capituloDesbloqueado(meta, capitulo)) {
                            onCapitulo(moduloId, capitulo.id)
                        } else {
                            onPaywall()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun TarjetaCapitulo(
    capitulo: Capitulo,
    leido: Boolean,
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
            Icon(
                imageVector = when {
                    !desbloqueado -> Icons.Default.Lock
                    leido -> Icons.Default.CheckCircle
                    else -> Icons.Outlined.PlayCircleOutline
                },
                contentDescription = null,
                tint = when {
                    !desbloqueado -> MaterialTheme.colorScheme.secondary
                    leido -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(24.dp),
            )
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(
                    text = capitulo.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (desbloqueado) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = when {
                        !desbloqueado -> "${capitulo.minutos} min · Con el Pase Completo"
                        capitulo.gratis -> "${capitulo.minutos} min · Gratis"
                        else -> "${capitulo.minutos} min de lectura"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
