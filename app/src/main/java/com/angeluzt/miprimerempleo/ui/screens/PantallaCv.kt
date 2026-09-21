package com.angeluzt.miprimerempleo.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.angeluzt.miprimerempleo.BuildConfig
import com.angeluzt.miprimerempleo.cv.Extension
import com.angeluzt.miprimerempleo.cv.RevisionCv
import com.angeluzt.miprimerempleo.ui.AccionArchivo
import com.angeluzt.miprimerempleo.ui.CvViewModel
import com.angeluzt.miprimerempleo.ui.EstadoApp
import com.angeluzt.miprimerempleo.ui.TurnoCv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var entrada by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val avisos = remember { SnackbarHostState() }

    // Saber si el teclado está arriba para bajar la conversación hasta el final.
    // Se lee dentro de derivedStateOf para no recomponer en cada fotograma de la animación.
    val insetsTeclado = WindowInsets.ime
    val densidad = LocalDensity.current
    val tecladoAbierto by remember(insetsTeclado, densidad) {
        derivedStateOf { insetsTeclado.getBottom(densidad) > 0 }
    }

    LaunchedEffect(cv.turnos.size, cv.generando, cv.cv != null, tecladoAbierto) {
        listState.animateScrollToItem((cv.turnos.size + 2).coerceAtLeast(0))
    }

    // Al volver a una pregunta ya contestada, se precarga lo que había escrito
    // para poder agregarle en vez de escribirlo todo otra vez.
    LaunchedEffect(cv.campo?.id, cv.editando) {
        entrada = if (cv.editando) cv.respuestas[cv.campo?.id].orEmpty() else ""
    }

    LaunchedEffect(cv.aviso) {
        cv.aviso?.let {
            avisos.showSnackbar(it)
            vm.descartarAviso()
        }
    }

    val guardarEn = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { destino ->
        val pendiente = cv.archivo
        if (destino == null || pendiente == null) {
            vm.archivoEntregado()
        } else {
            alcance.launch {
                val guardado = withContext(Dispatchers.IO) {
                    runCatching {
                        contexto.contentResolver.openOutputStream(destino)?.use { salida ->
                            pendiente.archivo.inputStream().use { it.copyTo(salida) }
                        } ?: error("El sistema no dio dónde escribir.")
                    }.isSuccess
                }
                vm.archivoEntregado(
                    if (guardado) "Listo, tu CV quedó guardado en PDF."
                    else "No pudimos guardar el archivo. Intenta con «Compartir».",
                )
            }
        }
    }

    LaunchedEffect(cv.archivo) {
        val pendiente = cv.archivo ?: return@LaunchedEffect
        when (pendiente.accion) {
            AccionArchivo.GUARDAR -> guardarEn.launch(pendiente.nombre)
            AccionArchivo.COMPARTIR -> {
                val uri = FileProvider.getUriForFile(
                    contexto,
                    "${BuildConfig.APPLICATION_ID}.fileprovider",
                    pendiente.archivo,
                )
                val envio = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, pendiente.nombre)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val abierto = runCatching {
                    contexto.startActivity(Intent.createChooser(envio, "Enviar tu CV"))
                }.isSuccess
                // Si no se abrió nada, decirlo. Un botón que no hace nada y no explica
                // por qué es peor que un error.
                vm.archivoEntregado(
                    if (abierto) null else "No encontramos con qué compartir el PDF.",
                )
            }
        }
    }

    val enviar = {
        if (entrada.isNotBlank()) {
            vm.responder(entrada)
            entrada = ""
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(avisos) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tu CV con IA", style = MaterialTheme.typography.titleMedium)
                        if (!cv.terminoElGuion) {
                            Text(
                                "Pregunta ${cv.posicion} de ${cv.total}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
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
                    // Sin esto el teclado tapa el campo y la persona escribe a ciegas.
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                cv.error?.let { mensaje ->
                    AvisoError(mensaje) { vm.descartarError() }
                    Spacer(Modifier.height(10.dp))
                }

                if (cv.cv == null && cv.puedeGenerar) {
                    BotonGenerar(cv.generando, cv.terminoElGuion) { vm.generar() }
                    Spacer(Modifier.height(10.dp))
                } else if (cv.cambiosSinGenerar) {
                    BotonRegenerar(cv.generando) { vm.generar() }
                    Spacer(Modifier.height(10.dp))
                }

                cv.campo?.sugerencias?.takeIf { it.isNotEmpty() }?.let { sugerencias ->
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 10.dp),
                    ) {
                        sugerencias.forEach { sugerencia ->
                            AssistChip(
                                onClick = { vm.responder(sugerencia) },
                                label = { Text(sugerencia) },
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        }
                    }
                }

                if (cv.campo != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = entrada,
                            onValueChange = { entrada = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Escribe tu respuesta…") },
                            shape = RoundedCornerShape(14.dp),
                            maxLines = 4,
                            enabled = !cv.generando,
                        )
                        IconButton(
                            onClick = enviar,
                            enabled = entrada.isNotBlank() && !cv.generando,
                            modifier = Modifier.padding(start = 6.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (cv.campo?.opcional == true) {
                        TextButton(
                            onClick = { entrada = ""; vm.saltar() },
                            enabled = !cv.generando,
                        ) {
                            Text("No tengo esto, saltar")
                        }
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(cv.turnos) { turno -> Burbuja(turno) }

            if (cv.generando) {
                item { Pensando() }
            }

            cv.revision?.let { revision ->
                item {
                    TarjetaRevision(
                        revision = revision,
                        revisando = cv.revisando,
                        onVolverA = { vm.volverA(it) },
                        onRevisarConIa = { vm.evaluar() },
                    )
                }
            }

            if (cv.cv != null) {
                item {
                    BarraExportar(
                        tienePase = estado.compras.tienePase,
                        exportando = cv.exportando,
                        desactualizado = cv.cambiosSinGenerar,
                        onPlantillas = onPlantillas,
                        onPaywall = onPaywall,
                        onExportar = { idioma, accion -> vm.exportar(idioma, accion) },
                    )
                }
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
private fun Pensando() {
    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(
            text = "Armando tu CV en español e inglés…",
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
private fun BotonGenerar(generando: Boolean, guionCompleto: Boolean, onGenerar: () -> Unit) {
    Column {
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
        if (!guionCompleto) {
            Text(
                text = "Ya tengo lo mínimo. Si sigues contestando, tu CV queda mejor.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun BotonRegenerar(generando: Boolean, onGenerar: () -> Unit) {
    Button(
        onClick = onGenerar,
        enabled = !generando,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(
            text = if (generando) "Generando…" else "Generar de nuevo con lo que agregaste",
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/**
 * La revisión del CV. No es un adorno: el hueco que nadie le señala a un recién egresado
 * es lo que hace que su CV no pase el primer filtro, y aquí viene con la pregunta exacta
 * para llenarlo de un toque.
 */
@Composable
private fun TarjetaRevision(
    revision: RevisionCv,
    revisando: Boolean,
    onVolverA: (String) -> Unit,
    onRevisarConIa: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Cómo se ve tu CV",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (revisando) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else if (revision.puntaje > 0) {
                    Text(
                        text = "${revision.puntaje}/100",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (revision.puntaje > 0) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { revision.puntaje / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = when (revision.extension) {
                    Extension.CORTO -> "${revision.palabras} palabras · se ve corto"
                    Extension.LARGO -> "${revision.palabras} palabras · se ve largo"
                    Extension.BIEN -> "${revision.palabras} palabras · buena extensión"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (revision.veredicto.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = revision.veredicto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (!revision.correoSirve && revision.notaCorreo.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "📧 ${revision.notaCorreo}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (revision.correoSugerido.isNotBlank()) {
                    Text(
                        text = "Algo así te serviría: ${revision.correoSugerido}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            revision.arreglos.take(4).forEach { arreglo ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "• $arreglo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            revision.fuertes.take(2).forEach { fuerte ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "✓ $fuerte",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val accionables = revision.faltantesAccionables
            if (accionables.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Agrégale esto y vuelve a generarlo:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                accionables.forEach { falta ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = falta.porque,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AssistChip(
                        onClick = { onVolverA(falta.campo) },
                        label = { Text(falta.pregunta.ifBlank { "Agregar" }, maxLines = 2) },
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onRevisarConIa, enabled = !revisando) {
                Text(if (revisando) "Revisando…" else "Que la IA lo revise otra vez")
            }
        }
    }
}

@Composable
private fun BarraExportar(
    tienePase: Boolean,
    exportando: Boolean,
    desactualizado: Boolean,
    onPlantillas: () -> Unit,
    onPaywall: () -> Unit,
    onExportar: (String, AccionArchivo) -> Unit,
) {
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
                text = when {
                    !tienePase -> "Ya lo armaste completo. Para descargarlo en PDF necesitas el Pase Completo."
                    desactualizado -> "Agregaste cosas nuevas. Genera otra vez antes de descargarlo."
                    else -> "Elige el formato y descárgalo en español o inglés."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonPdf("PDF Español", exportando) {
                    if (!tienePase) onPaywall() else onExportar("es", AccionArchivo.GUARDAR)
                }
                BotonPdf("PDF English", exportando) {
                    if (!tienePase) onPaywall() else onExportar("en", AccionArchivo.GUARDAR)
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onPlantillas, label = { Text("Ver formatos") })
                AssistChip(
                    onClick = {
                        if (!tienePase) onPaywall() else onExportar("es", AccionArchivo.COMPARTIR)
                    },
                    label = { Text("Compartir") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }

            if (exportando) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Text(
                        text = "Dibujando tu PDF…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BotonPdf(etiqueta: String, exportando: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = !exportando,
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
        Text(etiqueta, modifier = Modifier.padding(start = 6.dp))
    }
}
