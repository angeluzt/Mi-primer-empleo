package com.angeluzt.miprimerempleo.ui.screens

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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.angeluzt.miprimerempleo.cv.Extension
import com.angeluzt.miprimerempleo.ui.AccionArchivo
import com.angeluzt.miprimerempleo.ui.CvViewModel
import com.angeluzt.miprimerempleo.ui.EstadoApp
import com.angeluzt.miprimerempleo.ui.EstadoCv
import com.angeluzt.miprimerempleo.ui.TurnoCv
import com.angeluzt.miprimerempleo.ui.components.Aviso
import com.angeluzt.miprimerempleo.ui.components.BarraProgreso
import com.angeluzt.miprimerempleo.ui.components.EntregaDeArchivo
import com.angeluzt.miprimerempleo.ui.components.Hueco
import com.angeluzt.miprimerempleo.ui.components.HuecoH
import com.angeluzt.miprimerempleo.ui.components.IconoEnCaja
import com.angeluzt.miprimerempleo.ui.components.TarjetaSuave
import com.angeluzt.miprimerempleo.ui.components.TonoAviso

/**
 * No es un chat libre: es una entrevista guiada campo por campo.
 * Se arma gratis y completo; lo que se cobra es exportar el PDF.
 */
@Composable
fun PantallaCv(
    estado: EstadoApp,
    onPaywall: () -> Unit,
    onPlantillas: () -> Unit,
    onEditor: () -> Unit,
    onVacante: () -> Unit,
    onAtras: () -> Unit,
) {
    val vm: CvViewModel = viewModel()
    val cv by vm.estado.collectAsStateWithLifecycle()
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
        listState.animateScrollToItem((cv.turnos.size + 3).coerceAtLeast(0))
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

    EntregaDeArchivo(cv.archivo) { vm.archivoEntregado(it) }

    ContenidoCv(
        cv = cv,
        tienePase = estado.compras.tienePase,
        entrada = entrada,
        listState = listState,
        avisos = avisos,
        onEntrada = { entrada = it },
        onEnviar = {
            if (entrada.isNotBlank()) {
                vm.responder(entrada)
                entrada = ""
            }
        },
        onSugerencia = vm::responder,
        onSaltar = {
            entrada = ""
            vm.saltar()
        },
        onGenerar = vm::generar,
        onVolverA = vm::volverA,
        onRevisarConIa = vm::evaluar,
        onExportar = { idioma, accion -> if (estado.compras.tienePase) vm.exportar(idioma, accion) else onPaywall() },
        onDescartarError = vm::descartarError,
        onPlantillas = onPlantillas,
        onEditor = onEditor,
        onVacante = onVacante,
        onPaywall = onPaywall,
        onAtras = onAtras,
    )
}

/** Sin ViewModel, para poder dibujarla en las capturas con cualquier estado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContenidoCv(
    cv: EstadoCv,
    tienePase: Boolean,
    entrada: String,
    listState: LazyListState,
    avisos: SnackbarHostState,
    onEntrada: (String) -> Unit,
    onEnviar: () -> Unit,
    onSugerencia: (String) -> Unit,
    onSaltar: () -> Unit,
    onGenerar: () -> Unit,
    onVolverA: (String) -> Unit,
    onRevisarConIa: () -> Unit,
    onExportar: (String, AccionArchivo) -> Unit,
    onDescartarError: () -> Unit,
    onPlantillas: () -> Unit,
    onEditor: () -> Unit,
    onVacante: () -> Unit,
    onPaywall: () -> Unit,
    onAtras: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(avisos) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tu CV con IA", style = MaterialTheme.typography.titleMedium)
                        if (!cv.terminoElGuion) {
                            Text(
                                "Pregunta ${cv.posicion} de ${cv.total}",
                                style = MaterialTheme.typography.labelMedium,
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            BarraInferior(cv, entrada, onEntrada, onEnviar, onSugerencia, onSaltar, onGenerar, onDescartarError)
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
            item {
                Aviso(
                    tono = TonoAviso.SEGURIDAD,
                    texto = "No inventamos nada: solo redactamos lo que tú digas. Tus identificaciones " +
                        "(CURP, DNI, RUT…) se borran antes de salir del teléfono, y la IA nunca recibe tu teléfono.",
                )
            }
            items(cv.turnos) { turno -> Burbuja(turno) }

            if (cv.generando) {
                item { Pensando() }
            }

            cv.revision?.let { revision ->
                item {
                    TarjetaRevision(
                        cv = cv,
                        revision = revision,
                        onVolverA = onVolverA,
                        onRevisarConIa = onRevisarConIa,
                        onEditor = onEditor,
                    )
                }
            }

            if (cv.cv != null) {
                item {
                    BarraExportar(
                        tienePase = tienePase,
                        exportando = cv.exportando,
                        desactualizado = cv.cambiosSinGenerar,
                        onPlantillas = onPlantillas,
                        onEditor = onEditor,
                        onVacante = onVacante,
                        onPaywall = onPaywall,
                        onExportar = onExportar,
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun BarraInferior(
    cv: EstadoCv,
    entrada: String,
    onEntrada: (String) -> Unit,
    onEnviar: () -> Unit,
    onSugerencia: (String) -> Unit,
    onSaltar: () -> Unit,
    onGenerar: () -> Unit,
    onDescartarError: () -> Unit,
) {
    Column(
        Modifier
            .background(MaterialTheme.colorScheme.background)
            // Sin esto el teclado tapa el campo y la persona escribe a ciegas.
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        cv.error?.let { mensaje ->
            Aviso(TonoAviso.PELIGRO, mensaje) {
                TextButton(onClick = onDescartarError) { Text("Entendido") }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (cv.cv == null && cv.puedeGenerar) {
            BotonGenerar(cv.generando, cv.terminoElGuion, onGenerar)
            Spacer(Modifier.height(10.dp))
        } else if (cv.cambiosSinGenerar) {
            BotonRegenerar(cv.generando, onGenerar)
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
                        onClick = { onSugerencia(sugerencia) },
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
                    onValueChange = onEntrada,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe tu respuesta…") },
                    shape = MaterialTheme.shapes.large,
                    maxLines = 4,
                    enabled = !cv.generando,
                )
                IconButton(
                    onClick = onEnviar,
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
            if (cv.campo.opcional) {
                TextButton(onClick = onSaltar, enabled = !cv.generando) {
                    Text("No tengo esto, saltar")
                }
            }
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
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (turno.esUsuario) 18.dp else 4.dp,
                bottomEnd = if (turno.esUsuario) 4.dp else 18.dp,
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
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp),
        )
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
                .height(52.dp),
        ) {
            Text(if (generando) "Generando…" else "Generar mi CV")
        }
        if (!guionCompleto) {
            Text(
                text = "Ya tengo lo mínimo. Si sigues contestando, tu CV queda mejor.",
                style = MaterialTheme.typography.labelMedium,
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
            .height(52.dp),
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
 * para llenarlo de un toque. Arriba de todo, lo que protege a la persona: datos personales
 * que no deben ir y cifras que no salieron de su boca.
 */
@Composable
private fun TarjetaRevision(
    cv: EstadoCv,
    revision: com.angeluzt.miprimerempleo.cv.RevisionCv,
    onVolverA: (String) -> Unit,
    onRevisarConIa: () -> Unit,
    onEditor: () -> Unit,
) {
    TarjetaSuave {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Cómo se ve tu CV",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (cv.revisando) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else if (revision.puntaje > 0) {
                Text(
                    text = "${revision.puntaje}/100",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (revision.puntaje > 0) {
            Hueco(10.dp)
            BarraProgreso(valor = revision.puntaje / 100f, alto = 6.dp)
        }

        Hueco(10.dp)
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
            Hueco(8.dp)
            Text(revision.veredicto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }

        if (cv.tachados.any { it.tachado }) {
            Hueco(12.dp)
            Aviso(
                tono = TonoAviso.SEGURIDAD,
                titulo = "Protegimos tus datos",
                texto = "Quitamos tu " + cv.tachados.filter { it.tachado }.joinToString(", ") { it.tipo.nombre } +
                    " antes de mandar tus respuestas a la IA. Un CV no los necesita.",
            )
        }

        if (cv.sensibles.isNotEmpty()) {
            Hueco(12.dp)
            Aviso(
                tono = TonoAviso.ALERTA,
                titulo = "Quita esto de tu CV",
                texto = cv.sensibles.joinToString("\n") { "• Tu ${it.tipo.nombre}. ${it.tipo.porQue}" },
            ) {
                TextButton(onClick = onEditor) { Text("Editar mi CV") }
            }
        }

        if (cv.sospechas.isNotEmpty()) {
            Hueco(12.dp)
            Aviso(
                tono = TonoAviso.ALERTA,
                titulo = "Revisa esto antes de mandarlo",
                texto = "No aparece en lo que nos contaste. Si no es cierto, quítalo: te lo van a preguntar.\n" +
                    cv.sospechas.take(5).joinToString("\n") { "• «${it.dato}» en ${it.donde}" },
            ) {
                TextButton(onClick = onEditor) { Text("Corregir en el editor") }
            }
        }

        if (!revision.correoSirve && revision.notaCorreo.isNotBlank()) {
            Hueco(12.dp)
            Text("📧 ${revision.notaCorreo}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            if (revision.correoSugerido.isNotBlank()) {
                Text(
                    text = "Algo así te serviría: ${revision.correoSugerido}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        revision.arreglos.take(4).forEach { arreglo ->
            Hueco(8.dp)
            Text("• $arreglo", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }

        revision.fuertes.take(2).forEach { fuerte ->
            Hueco(8.dp)
            Text("✓ $fuerte", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
        }

        val accionables = revision.faltantesAccionables
        if (accionables.isNotEmpty()) {
            Hueco(14.dp)
            Text(
                text = "Agrégale esto y vuelve a generarlo:",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            accionables.forEach { falta ->
                Hueco(8.dp)
                Text(falta.porque, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AssistChip(
                    onClick = { onVolverA(falta.campo) },
                    label = { Text(falta.pregunta.ifBlank { "Agregar" }, maxLines = 2) },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Hueco(8.dp)
        TextButton(onClick = onRevisarConIa, enabled = !cv.revisando) {
            Text(if (cv.revisando) "Revisando…" else "Que la IA lo revise otra vez")
        }
    }
}

@Composable
private fun BarraExportar(
    tienePase: Boolean,
    exportando: Boolean,
    desactualizado: Boolean,
    onPlantillas: () -> Unit,
    onEditor: () -> Unit,
    onVacante: () -> Unit,
    onPaywall: () -> Unit,
    onExportar: (String, AccionArchivo) -> Unit,
) {
    TarjetaSuave(relleno = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!tienePase) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                HuecoH(8.dp)
            }
            Text(
                text = if (tienePase) "Tu CV está listo" else "Tu CV está listo — falta un paso",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Hueco(4.dp)
        Text(
            text = when {
                !tienePase -> "Ya lo armaste completo. Para descargarlo en PDF necesitas el Pase Completo."
                desactualizado -> "Agregaste cosas nuevas. Genera otra vez antes de descargarlo."
                else -> "Descárgalo en español o inglés, con el formato que elijas."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Hueco(14.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { if (tienePase) onExportar("es", AccionArchivo.GUARDAR) else onPaywall() },
                enabled = !exportando,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                HuecoH(6.dp)
                Text("Español")
            }
            Button(
                onClick = { if (tienePase) onExportar("en", AccionArchivo.GUARDAR) else onPaywall() },
                enabled = !exportando,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                HuecoH(6.dp)
                Text("English")
            }
        }

        Hueco(8.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEditor, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                HuecoH(6.dp)
                Text("Editar")
            }
            OutlinedButton(onClick = onPlantillas, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                HuecoH(6.dp)
                Text("Formato")
            }
            OutlinedButton(
                onClick = { if (tienePase) onExportar("es", AccionArchivo.COMPARTIR) else onPaywall() },
                enabled = !exportando,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }

        if (exportando) {
            Hueco(10.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Text(
                    text = "Dibujando tu PDF…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        Hueco(14.dp)
        FilledTonalButton(onClick = onVacante, modifier = Modifier.fillMaxWidth()) {
            IconoEnCaja(Icons.Default.TravelExplore, MaterialTheme.colorScheme.primary, tamano = 28.dp, fondo = Color.Transparent)
            HuecoH(4.dp)
            Text("Adaptarlo a una vacante")
        }
    }
}
