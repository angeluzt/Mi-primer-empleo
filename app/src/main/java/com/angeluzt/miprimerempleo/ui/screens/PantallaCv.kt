package com.angeluzt.miprimerempleo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.angeluzt.miprimerempleo.ui.CvViewModel
import com.angeluzt.miprimerempleo.ui.EstadoApp
import com.angeluzt.miprimerempleo.ui.EstadoCv
import com.angeluzt.miprimerempleo.ui.TurnoCv

/**
 * No es un chat libre: es una entrevista guiada campo por campo.
 * Se arma gratis y completo; lo que se cobra es exportar el PDF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCv(
    estado: EstadoApp,
    onPaywall: () -> Unit,
    onPlantillas: () -> Unit,
    onAtras: () -> Unit,
) {
    val vm: CvViewModel = viewModel()
    val cv by vm.estado.collectAsStateWithLifecycle()
    var entrada by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(cv.turnos.size, cv.pensando) {
        listState.animateScrollToItem((cv.turnos.size + 1).coerceAtLeast(0))
    }

    val enviar = {
        if (entrada.isNotBlank()) {
            vm.responder(entrada)
            entrada = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tu CV con IA", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                cv.error?.let { mensaje ->
                    AvisoError(mensaje) { vm.descartarError() }
                    Spacer(Modifier.height(10.dp))
                }

                if (cv.cv != null) {
                    BarraExportar(estado, onPaywall, onPlantillas)
                    Spacer(Modifier.height(10.dp))
                } else if (cv.listoParaGenerar) {
                    BotonGenerar(cv.generando) { vm.generar() }
                    Spacer(Modifier.height(10.dp))
                }

                if (cv.sugerencias.isNotEmpty() && !cv.pensando) {
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 10.dp),
                    ) {
                        cv.sugerencias.forEach { sugerencia ->
                            AssistChip(
                                onClick = { vm.responder(sugerencia) },
                                label = { Text(sugerencia) },
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = entrada,
                        onValueChange = { entrada = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Escribe tu respuesta…") },
                        shape = RoundedCornerShape(14.dp),
                        maxLines = 4,
                        enabled = !cv.pensando && !cv.generando,
                    )
                    IconButton(
                        onClick = enviar,
                        enabled = entrada.isNotBlank() && !cv.pensando && !cv.generando,
                        modifier = Modifier.padding(start = 6.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
    ) { relleno ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(cv.turnos) { turno -> Burbuja(turno) }
            if (cv.pensando || cv.generando) {
                item { Pensando(cv.generando) }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun Burbuja(turno: TurnoCv) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (turno.esUsuario) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (turno.esUsuario) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
            ),
            border = if (turno.esUsuario) null
            else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (turno.esUsuario) 16.dp else 4.dp,
                bottomEnd = if (turno.esUsuario) 4.dp else 16.dp,
            ),
            modifier = Modifier.fillMaxWidth(0.86f),
        ) {
            Text(
                text = turno.texto,
                style = MaterialTheme.typography.bodyMedium,
                color = if (turno.esUsuario) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(14.dp),
            )
        }
    }
}

@Composable
private fun Pensando(generando: Boolean) {
    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(
            text = if (generando) "Armando tu CV en español e inglés…" else "Escribiendo…",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun AvisoError(mensaje: String, onCerrar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            IconButton(onClick = onCerrar) {
                Text("✕", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun BotonGenerar(generando: Boolean, onGenerar: () -> Unit) {
    Button(
        onClick = onGenerar,
        enabled = !generando,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(if (generando) "Generando…" else "Generar mi CV")
    }
}

@Composable
private fun BarraExportar(
    estado: EstadoApp,
    onPaywall: () -> Unit,
    onPlantillas: () -> Unit,
) {
    val tienePase = estado.compras.tienePase
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (tienePase) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!tienePase) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = if (tienePase) "Tu CV está listo" else "Tu CV está listo — falta un paso",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (tienePase) {
                    "Elige el formato y expórtalo en español e inglés."
                } else {
                    "Ya lo armaste completo. Para exportarlo en PDF necesitas el Pase Completo."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onPlantillas, label = { Text("Ver formatos") })
                AssistChip(
                    onClick = { if (!tienePase) onPaywall() },
                    label = { Text("PDF Español") },
                )
                AssistChip(
                    onClick = { if (!tienePase) onPaywall() },
                    label = { Text("PDF English") },
                )
            }
        }
    }
}
