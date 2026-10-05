package com.angeluzt.miprimerempleo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.angeluzt.miprimerempleo.cv.Adaptacion
import com.angeluzt.miprimerempleo.cv.Requisito
import com.angeluzt.miprimerempleo.ui.AccionArchivo
import com.angeluzt.miprimerempleo.ui.EstadoApp
import com.angeluzt.miprimerempleo.ui.EstadoVacante
import com.angeluzt.miprimerempleo.ui.VacanteViewModel
import com.angeluzt.miprimerempleo.ui.components.AnilloProgreso
import com.angeluzt.miprimerempleo.ui.components.Aviso
import com.angeluzt.miprimerempleo.ui.components.EncabezadoSeccion
import com.angeluzt.miprimerempleo.ui.components.EntregaDeArchivo
import com.angeluzt.miprimerempleo.ui.components.Hueco
import com.angeluzt.miprimerempleo.ui.components.HuecoH
import com.angeluzt.miprimerempleo.ui.components.Insignia
import com.angeluzt.miprimerempleo.ui.components.TarjetaMarca
import com.angeluzt.miprimerempleo.ui.components.TarjetaSuave
import com.angeluzt.miprimerempleo.ui.components.TonoAviso
import com.angeluzt.miprimerempleo.ui.theme.SobreMarcaSuave
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PantallaVacante(
    estado: EstadoApp,
    onCv: () -> Unit,
    onPaywall: () -> Unit,
    onAtras: () -> Unit,
) {
    val vm: VacanteViewModel = viewModel()
    val vacante by vm.estado.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }

    LaunchedEffect(vacante.aviso) {
        vacante.aviso?.let {
            avisos.showSnackbar(it)
            vm.descartarAviso()
        }
    }
    EntregaDeArchivo(vacante.archivo) { vm.archivoEntregado(it) }

    ContenidoVacante(
        vacante = vacante,
        estado = estado,
        avisos = avisos,
        onTexto = vm::cambiarTexto,
        onAdaptar = {
            val cv = estado.cv
            when {
                cv == null -> onCv()
                !estado.compras.tienePase -> onPaywall()
                else -> vm.adaptar(cv)
            }
        },
        onAbrir = { vm.abrir(it, estado.cv) },
        onNueva = vm::nueva,
        onExportar = { accion -> if (estado.compras.tienePase) vm.exportar(accion) else onPaywall() },
        onDescartarError = vm::descartarError,
        onAtras = {
            if (vacante.resultado != null) vm.nueva() else onAtras()
        },
    )
}

/** Sin ViewModel, para poder dibujarla en las capturas con cualquier estado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContenidoVacante(
    vacante: EstadoVacante,
    estado: EstadoApp,
    avisos: SnackbarHostState,
    onTexto: (String) -> Unit,
    onAdaptar: () -> Unit,
    onAbrir: (Adaptacion) -> Unit,
    onNueva: () -> Unit,
    onExportar: (AccionArchivo) -> Unit,
    onDescartarError: () -> Unit,
    onAtras: () -> Unit,
) {
    val resultado = vacante.resultado
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(avisos) },
        topBar = {
            TopAppBar(
                title = {
                    // Puesto y empresa en dos líneas: juntos en una se cortaban a media palabra.
                    Column {
                        Text(
                            text = resultado?.puesto?.ifBlank { null } ?: if (resultado != null) "Tu postulación" else "Revisar una vacante",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!resultado?.empresa.isNullOrBlank()) {
                            Text(
                                text = resultado!!.empresa,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
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
    ) { relleno ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            vacante.error?.let { mensaje ->
                item {
                    Aviso(TonoAviso.PELIGRO, mensaje) {
                        TextButton(onClick = onDescartarError) { Text("Entendido") }
                    }
                }
            }
            if (resultado == null) {
                entrada(vacante, estado, onTexto, onAdaptar, onAbrir)
            } else {
                analisis(resultado, vacante, onExportar, onNueva)
            }
        }
    }
}

// ---------- Pegar la vacante ----------

private fun androidx.compose.foundation.lazy.LazyListScope.entrada(
    vacante: EstadoVacante,
    estado: EstadoApp,
    onTexto: (String) -> Unit,
    onAdaptar: () -> Unit,
    onAbrir: (Adaptacion) -> Unit,
) {
    item {
        Text(
            text = "Pega el texto completo de la vacante. Te decimos al instante si huele a fraude, " +
                "y con la IA, qué tanto la cubres y tu CV reordenado para ese puesto.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    item { CampoVacante(vacante.texto, onTexto) }

    if (vacante.senales.isNotEmpty()) {
        item {
            Aviso(
                tono = TonoAviso.PELIGRO,
                titulo = "Ojo: esto se parece a una estafa",
                texto = "Encontramos ${vacante.senales.size} " +
                    (if (vacante.senales.size == 1) "señal" else "señales") + " de fraude de empleo:",
            ) {
                vacante.senales.forEach { senal ->
                    Hueco(8.dp)
                    Text("• ${senal.explicacion}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "«${senal.fragmento}»",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    )
                }
            }
        }
    } else if (vacante.textoSuficiente) {
        item {
            Aviso(
                tono = TonoAviso.EXITO,
                texto = "No vemos las señales típicas de fraude. Aun así: nunca pagues nada para entrar " +
                    "a un trabajo, ni des tu identificación antes de firmar.",
            )
        }
    }

    item {
        val sospechosa = vacante.senales.isNotEmpty()
        val contenido: @Composable RowScope.() -> Unit = {
            if (vacante.adaptando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                )
                HuecoH(10.dp)
                Text("Leyendo la vacante…")
            } else {
                Text(
                    when {
                        estado.cv == null -> "Primero arma tu CV"
                        !estado.compras.tienePase -> "Adaptar mi CV · con el Pase Completo"
                        sospechosa -> "Adaptarlo de todos modos"
                        else -> "Adaptar mi CV a esta vacante"
                    },
                )
            }
        }
        val habilitado = vacante.textoSuficiente && !vacante.adaptando
        val forma = Modifier
            .fillMaxWidth()
            .height(52.dp)
        // Si huele a fraude, adaptar deja de ser lo más llamativo de la pantalla: gastar una
        // generación en una estafa, y animarse a mandarle el CV, es justo lo que hay que evitar.
        if (sospechosa) {
            OutlinedButton(onClick = onAdaptar, enabled = habilitado, modifier = forma, content = contenido)
        } else {
            Button(onClick = onAdaptar, enabled = habilitado, modifier = forma, content = contenido)
        }
        if (estado.compras.tienePase && estado.cv != null) {
            Text(
                text = "Usa 1 de tus ${estado.creditosCv} generaciones. La revisión de fraude es gratis.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
        }
    }

    if (estado.adaptaciones.isNotEmpty()) {
        item {
            Hueco(8.dp)
            EncabezadoSeccion("Vacantes que ya revisaste")
        }
        items(estado.adaptaciones, key = { it.id }) { adaptacion ->
            TarjetaSuave(onClick = { onAbrir(adaptacion) }, relleno = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnilloProgreso(
                        valor = adaptacion.coincidencia / 100f,
                        tamano = 44.dp,
                        grosor = 4.dp,
                        color = colorCoincidencia(adaptacion.coincidencia),
                    ) {
                        Text(
                            "${adaptacion.coincidencia}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    HuecoH(14.dp)
                    Column(Modifier.weight(1f)) {
                        Text(adaptacion.titulo, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Text(
                            text = SimpleDateFormat("d MMM", Locale("es", "MX")).format(Date(adaptacion.creada)) +
                                if (adaptacion.alertas.isNotEmpty()) " · con alertas" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CampoVacante(texto: String, onTexto: (String) -> Unit) {
    val portapapeles = LocalClipboardManager.current
    OutlinedTextField(
        value = texto,
        onValueChange = onTexto,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Analista de Datos Jr. · Requisitos: Excel avanzado, SQL…") },
        minLines = 7,
        maxLines = 14,
        shape = MaterialTheme.shapes.medium,
        trailingIcon = {
            if (texto.isBlank()) {
                IconButton(onClick = { portapapeles.getText()?.text?.let(onTexto) }) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Pegar")
                }
            }
        },
    )
}

// ---------- El análisis ----------

@OptIn(ExperimentalLayoutApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.analisis(
    a: Adaptacion,
    vacante: EstadoVacante,
    onExportar: (AccionArchivo) -> Unit,
    onNueva: () -> Unit,
) {
    item { TarjetaCoincidencia(a) }

    if (a.alertas.isNotEmpty()) {
        item {
            Aviso(
                tono = TonoAviso.PELIGRO,
                titulo = "Antes de postularte",
                texto = a.alertas.joinToString("\n") { "• $it" },
            )
        }
    }

    if (vacante.sospechas.isNotEmpty()) {
        item {
            Aviso(
                tono = TonoAviso.ALERTA,
                titulo = "Revisa esto en el CV adaptado",
                texto = "No estaba en tu CV original. Si no es cierto, quítalo antes de mandarlo:\n" +
                    vacante.sospechas.joinToString("\n") { "• ${it.dato} — ${it.donde}" },
            )
        }
    }

    if (a.cubres.isNotEmpty()) {
        item {
            Hueco(4.dp)
            EncabezadoSeccion("Lo que ya cubres")
        }
        items(a.cubres) { r -> FilaRequisito(r, cubierto = true) }
    }
    if (a.teFalta.isNotEmpty()) {
        item {
            Hueco(4.dp)
            EncabezadoSeccion(
                titulo = "Lo que te falta",
                subtitulo = "No va en tu CV porque no es cierto todavía. Así lo puedes cubrir:",
            )
        }
        items(a.teFalta) { r -> FilaRequisito(r, cubierto = false) }
    }

    if (a.palabrasClave.isNotEmpty()) {
        item {
            Hueco(4.dp)
            EncabezadoSeccion(
                titulo = "Palabras que tu CV ya trae",
                subtitulo = "Las mismas que usa la vacante: así te encuentran los filtros automáticos.",
            )
            Hueco(8.dp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                a.palabrasClave.forEach { SuggestionChip(onClick = {}, label = { Text(it) }) }
            }
        }
    }

    if (a.mensaje.isNotBlank()) {
        item {
            Hueco(4.dp)
            TarjetaMensaje(a.mensaje)
        }
    }

    item {
        Hueco(4.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { onExportar(AccionArchivo.GUARDAR) },
                enabled = !vacante.exportando,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                HuecoH(8.dp)
                Text("CV en PDF")
            }
            OutlinedButton(
                onClick = { onExportar(AccionArchivo.COMPARTIR) },
                enabled = !vacante.exportando,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                HuecoH(8.dp)
                Text("Compartir")
            }
        }
        TextButton(onClick = onNueva, modifier = Modifier.fillMaxWidth()) {
            Text("Revisar otra vacante")
        }
    }
}

@Composable
private fun TarjetaCoincidencia(a: Adaptacion) {
    TarjetaMarca {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnilloProgreso(
                valor = a.coincidencia / 100f,
                tamano = 76.dp,
                grosor = 7.dp,
                color = Color.White,
                fondo = Color.White.copy(alpha = 0.25f),
            ) {
                Text("${a.coincidencia}%", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            HuecoH(16.dp)
            Column {
                Text("COINCIDENCIA", style = MaterialTheme.typography.labelSmall, color = SobreMarcaSuave)
                Text(
                    text = when {
                        a.coincidencia >= 75 -> "Postúlate"
                        a.coincidencia >= 50 -> "Vale la pena intentarlo"
                        else -> "Te falta camino"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Text(
                    text = "Cubres ${a.cubres.size} de ${a.cubres.size + a.teFalta.size} requisitos",
                    style = MaterialTheme.typography.bodySmall,
                    color = SobreMarcaSuave,
                )
            }
        }
        if (a.indispensablesQueFaltan > 0) {
            Text(
                text = "Te ${if (a.indispensablesQueFaltan == 1) "falta 1 requisito indispensable" else "faltan ${a.indispensablesQueFaltan} requisitos indispensables"}. " +
                    "Puedes postularte igual, pero prepárate para que te lo pregunten.",
                style = MaterialTheme.typography.bodySmall,
                color = SobreMarcaSuave,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun FilaRequisito(r: Requisito, cubierto: Boolean) {
    TarjetaSuave(relleno = 14.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = if (cubierto) Icons.Default.CheckCircle else Icons.Default.RemoveCircleOutline,
                contentDescription = null,
                tint = if (cubierto) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
            )
            HuecoH(12.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = r.requisito,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (r.indispensable) {
                        HuecoH(8.dp)
                        Insignia("Indispensable", MaterialTheme.colorScheme.secondary)
                    }
                }
                if (r.detalle.isNotBlank()) {
                    Text(
                        text = r.detalle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaMensaje(mensaje: String) {
    val portapapeles = LocalClipboardManager.current
    TarjetaSuave {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Mensaje para postularte",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { portapapeles.setText(AnnotatedString(mensaje)) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar el mensaje")
            }
        }
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Cámbialo con tus palabras: un reclutador nota cuando un mensaje no suena a ti.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun colorCoincidencia(valor: Int): Color = when {
    valor >= 75 -> MaterialTheme.colorScheme.tertiary
    valor >= 50 -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.secondary
}
