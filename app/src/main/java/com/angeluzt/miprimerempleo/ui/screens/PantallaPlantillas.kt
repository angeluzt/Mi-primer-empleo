package com.angeluzt.miprimerempleo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import com.angeluzt.miprimerempleo.cv.Cv
import com.angeluzt.miprimerempleo.cv.Plantilla
import com.angeluzt.miprimerempleo.cv.Plantillas
import com.angeluzt.miprimerempleo.cv.PreviewCv

/**
 * Selector de formato. Las miniaturas se generan renderizando el PDF real,
 * así que lo que la persona ve aquí es exactamente lo que va a exportar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPlantillas(
    cv: Cv,
    plantillaElegida: String,
    onElegir: (String) -> Unit,
    onAtras: () -> Unit,
) {
    val contexto = LocalContext.current
    val preview = remember { PreviewCv(contexto) }
    val miniaturas = remember { mutableStateMapOf<String, Bitmap?>() }
    var soloSeguras by remember { mutableStateOf(false) }

    val visibles = if (soloSeguras) Plantillas.seguras() else Plantillas.catalogo

    LaunchedEffect(cv) {
        Plantillas.catalogo.forEach { plantilla ->
            if (!miniaturas.containsKey(plantilla.id)) {
                miniaturas[plantilla.id] = preview.miniatura(cv, plantilla, null)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Elige el formato", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
    ) { relleno ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                Column {
                    AvisoFiltros()
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !soloSeguras,
                            onClick = { soloSeguras = false },
                            label = { Text("Todas (${Plantillas.catalogo.size})") },
                        )
                        FilterChip(
                            selected = soloSeguras,
                            onClick = { soloSeguras = true },
                            label = { Text("Seguras (${Plantillas.seguras().size})") },
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            items(visibles, key = { it.id }) { plantilla ->
                TarjetaPlantilla(
                    plantilla = plantilla,
                    miniatura = miniaturas[plantilla.id],
                    elegida = plantilla.id == plantillaElegida,
                    onClick = { onElegir(plantilla.id) },
                )
            }
        }
    }
}

@Composable
private fun AvisoFiltros() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Antes de elegir por bonito",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Las bolsas de trabajo y casi todas las empresas grandes leen tu CV con " +
                    "un programa antes que una persona. Los formatos a dos columnas se ven muy " +
                    "bien y ese programa los revuelve. Marcamos cuáles se leen sin problema.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun TarjetaPlantilla(
    plantilla: Plantilla,
    miniatura: Bitmap?,
    elegida: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            if (elegida) 2.5.dp else 1.dp,
            if (elegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(595f / 842f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (miniatura != null) {
                    Image(
                        bitmap = miniatura.asImageBitmap(),
                        contentDescription = plantilla.nombre,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
            if (elegida) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Elegida",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp),
                )
            }
        }

        Column(Modifier.padding(12.dp)) {
            Text(
                text = plantilla.nombre,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            if (plantilla.aptaParaFiltros) {
                Text(
                    text = "Segura para filtros",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = "Solo entrega directa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}
