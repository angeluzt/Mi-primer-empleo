package com.angeluzt.miprimerempleo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.angeluzt.miprimerempleo.ui.EstadoApp

private data class Turno(val esUsuario: Boolean, val texto: String)

/**
 * No es un chat libre: es una entrevista guiada por campos.
 * Se arma gratis y completo; lo que se cobra es exportar el PDF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCv(
    estado: EstadoApp,
    onPaywall: () -> Unit,
    onAtras: () -> Unit,
) {
    var turnos by remember {
        mutableStateOf(
            listOf(
                Turno(
                    false,
                    "Vamos a armar tu CV. Te voy a hacer preguntas cortas y con tus respuestas genero dos versiones: español e inglés.\n\nNo invento nada: solo acomodo y redacto lo que tú me digas.",
                ),
                Turno(false, "¿Cuál es tu nombre completo?"),
            )
        )
    }
    var entrada by remember { mutableStateOf("") }
    var pensando by remember { mutableStateOf(false) }
    var camposRespondidos by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()

    val listo = camposRespondidos >= 6

    LaunchedEffect(turnos.size) {
        if (turnos.isNotEmpty()) listState.animateScrollToItem(turnos.size)
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
                if (listo) {
                    BarraExportar(estado, onPaywall)
                    Spacer(Modifier.height(10.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = entrada,
                        onValueChange = { entrada = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Escribe tu respuesta…") },
                        shape = RoundedCornerShape(14.dp),
                        maxLines = 4,
                    )
                    IconButton(
                        onClick = {
                            if (entrada.isBlank()) return@IconButton
                            turnos = turnos + Turno(true, entrada.trim())
                            entrada = ""
                            camposRespondidos++
                            pensando = true
                        },
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
            items(turnos) { turno -> Burbuja(turno) }
            if (pensando) {
                item {
                    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = "Escribiendo…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun Burbuja(turno: Turno) {
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
private fun BarraExportar(estado: EstadoApp, onPaywall: () -> Unit) {
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
                    "Exporta el PDF en español e inglés, o sigue editando."
                } else {
                    "Ya lo armaste completo. Para exportarlo en PDF, español e inglés, necesitas el Pase Completo."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
