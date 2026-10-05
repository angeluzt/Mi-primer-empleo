package com.angeluzt.miprimerempleo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.angeluzt.miprimerempleo.bitacora.Entrevista
import com.angeluzt.miprimerempleo.bitacora.Pregunta
import com.angeluzt.miprimerempleo.bitacora.Resultado
import com.angeluzt.miprimerempleo.ui.components.EncabezadoSeccion
import com.angeluzt.miprimerempleo.ui.components.Hueco
import com.angeluzt.miprimerempleo.ui.components.HuecoH
import com.angeluzt.miprimerempleo.ui.components.TarjetaSuave
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Registrar una entrevista, con las cinco cosas que el capítulo "Usa las entrevistas que
 * fallaste" pide anotar saliendo, mientras está fresco.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PantallaEntrevista(
    existente: Entrevista?,
    onGuardar: (Entrevista) -> Unit,
    onBorrar: (String) -> Unit,
    onAtras: () -> Unit,
) {
    var borrador by remember(existente?.id) {
        mutableStateOf(
            existente ?: Entrevista(
                id = UUID.randomUUID().toString(),
                fecha = System.currentTimeMillis(),
                empresa = "",
                puesto = "",
            ),
        )
    }
    var nuevaPregunta by remember { mutableStateOf("") }
    var eligiendoFecha by remember { mutableStateOf(false) }
    var confirmarBorrado by remember { mutableStateOf(false) }

    val agregar = {
        if (nuevaPregunta.isNotBlank()) {
            borrador = borrador.copy(preguntas = borrador.preguntas + Pregunta(nuevaPregunta.trim()))
            nuevaPregunta = ""
        }
    }
    val puedeGuardar = borrador.empresa.isNotBlank() || borrador.puesto.isNotBlank()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (existente == null) "Nueva entrevista" else "Tu entrevista",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (existente != null) {
                        IconButton(onClick = { confirmarBorrado = true }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Borrar")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Button(
                onClick = { onGuardar(borrador.copy(preguntas = borrador.preguntas.filter { it.texto.isNotBlank() })) },
                enabled = puedeGuardar,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .height(52.dp),
            ) {
                Text("Guardar")
            }
        },
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Campo("Empresa", borrador.empresa) { borrador = borrador.copy(empresa = it) }
            Campo("Puesto", borrador.puesto) { borrador = borrador.copy(puesto = it) }

            TarjetaSuave(onClick = { eligiendoFecha = true }, relleno = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    HuecoH(12.dp)
                    Text(
                        text = SimpleDateFormat("EEEE d 'de' MMMM yyyy", Locale("es", "MX")).format(Date(borrador.fecha)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Hueco(4.dp)
            EncabezadoSeccion("¿Cómo te fue?")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Resultado.entries.forEach { resultado ->
                    FilterChip(
                        selected = borrador.resultado == resultado,
                        onClick = { borrador = borrador.copy(resultado = resultado) },
                        label = { Text(resultado.etiqueta) },
                    )
                }
            }

            Hueco(4.dp)
            EncabezadoSeccion(
                titulo = "Lo que te preguntaron",
                subtitulo = "Lo más literal que recuerdes. Toca «No la supe» en las que te atoraste: " +
                    "esa es la lista de oro.",
            )
            borrador.preguntas.forEachIndexed { indice, pregunta ->
                FilaPregunta(
                    pregunta = pregunta,
                    onAlternar = {
                        borrador = borrador.copy(
                            preguntas = borrador.preguntas.mapIndexed { i, p ->
                                if (i == indice) p.copy(laSupe = !p.laSupe) else p
                            },
                        )
                    },
                    onQuitar = {
                        borrador = borrador.copy(preguntas = borrador.preguntas.filterIndexed { i, _ -> i != indice })
                    },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = nuevaPregunta,
                    onValueChange = { nuevaPregunta = it },
                    placeholder = { Text("Ej. ¿Por qué quieres trabajar aquí?") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { agregar() }),
                )
                IconButton(onClick = agregar, enabled = nuevaPregunta.isNotBlank()) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar pregunta")
                }
            }

            Hueco(4.dp)
            EncabezadoSeccion("Para la próxima")
            Campo(
                "¿En qué momento sentiste que se enfrió?", borrador.seEnfrio, multilinea = true,
            ) { borrador = borrador.copy(seEnfrio = it) }
            Campo(
                "¿Qué te preguntaron que no habías investigado?", borrador.noInvestigue, multilinea = true,
            ) { borrador = borrador.copy(noInvestigue = it) }
            Campo(
                "Una cosa que harías distinto", borrador.distinto, multilinea = true,
            ) { borrador = borrador.copy(distinto = it) }
            Hueco(16.dp)
        }
    }

    if (eligiendoFecha) {
        val estadoFecha = rememberDatePickerState(initialSelectedDateMillis = aMedianocheUtc(borrador.fecha))
        DatePickerDialog(
            onDismissRequest = { eligiendoFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoFecha.selectedDateMillis?.let { borrador = borrador.copy(fecha = aMediodiaLocal(it)) }
                    eligiendoFecha = false
                }) { Text("Listo") }
            },
            dismissButton = {
                TextButton(onClick = { eligiendoFecha = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = estadoFecha)
        }
    }

    if (confirmarBorrado && existente != null) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("¿Borrar esta entrevista?") },
            text = { Text("Sus preguntas también salen de tu lista de estudio. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarBorrado = false
                    onBorrar(existente.id)
                }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun Campo(etiqueta: String, valor: String, multilinea: Boolean = false, onCambio: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        singleLine = !multilinea,
        minLines = if (multilinea) 2 else 1,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
}

@Composable
private fun FilaPregunta(pregunta: Pregunta, onAlternar: () -> Unit, onQuitar: () -> Unit) {
    TarjetaSuave(relleno = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pregunta.texto,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onQuitar) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Quitar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = pregunta.laSupe,
                onClick = { if (!pregunta.laSupe) onAlternar() },
                label = { Text("La supe") },
            )
            FilterChip(
                selected = !pregunta.laSupe,
                onClick = { if (pregunta.laSupe) onAlternar() },
                label = { Text("No la supe") },
            )
        }
    }
}

/*
 * El DatePicker de Material trabaja en medianoche UTC. En México (UTC-6) esa medianoche cae
 * a las 6 de la tarde del día anterior, y la entrevista del lunes aparecería el domingo.
 * Se convierte en los dos sentidos guardando el día a mediodía local.
 */
private fun aMedianocheUtc(local: Long): Long {
    val dia = Calendar.getInstance().apply { timeInMillis = local }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(dia.get(Calendar.YEAR), dia.get(Calendar.MONTH), dia.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun aMediodiaLocal(utc: Long): Long {
    val dia = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
    return Calendar.getInstance().apply {
        clear()
        set(dia.get(Calendar.YEAR), dia.get(Calendar.MONTH), dia.get(Calendar.DAY_OF_MONTH), 12, 0)
    }.timeInMillis
}
