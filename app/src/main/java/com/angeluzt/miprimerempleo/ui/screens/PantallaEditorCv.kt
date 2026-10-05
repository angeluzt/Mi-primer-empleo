package com.angeluzt.miprimerempleo.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.angeluzt.miprimerempleo.cv.Certificacion
import com.angeluzt.miprimerempleo.cv.Cv
import com.angeluzt.miprimerempleo.cv.Experiencia
import com.angeluzt.miprimerempleo.cv.Formacion
import com.angeluzt.miprimerempleo.cv.IdiomaNivel
import com.angeluzt.miprimerempleo.cv.ParCv
import com.angeluzt.miprimerempleo.cv.Proyecto
import com.angeluzt.miprimerempleo.cv.ProteccionDatos
import com.angeluzt.miprimerempleo.ui.components.Aviso
import com.angeluzt.miprimerempleo.ui.components.EncabezadoSeccion
import com.angeluzt.miprimerempleo.ui.components.Hueco
import com.angeluzt.miprimerempleo.ui.components.TarjetaSuave
import com.angeluzt.miprimerempleo.ui.components.TonoAviso

/**
 * Editar el CV campo por campo, sin gastar una generación.
 *
 * Era uno de los pedidos originales ("el resultado, 2 CV, inglés y español, y poder editar")
 * y no existía: si la IA escribía mal una fecha, la única salida era volver a generar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEditorCv(
    par: ParCv,
    onGuardar: (ParCv) -> Unit,
    onAtras: () -> Unit,
) {
    var borrador by remember(par) { mutableStateOf(par) }
    var idioma by remember { mutableStateOf("es") }
    var confirmarSalida by remember { mutableStateOf(false) }

    val cv = if (idioma == "en") borrador.en else borrador.es
    val cambiar: ((Cv) -> Cv) -> Unit = { f ->
        borrador = if (idioma == "en") borrador.copy(en = f(borrador.en)) else borrador.copy(es = f(borrador.es))
    }
    val hayCambios = borrador != par
    val salir = { if (hayCambios) confirmarSalida = true else onAtras() }
    val sensibles = remember(cv) { ProteccionDatos.revisarCv(cv) }

    BackHandler(enabled = hayCambios) { confirmarSalida = true }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Editar tu CV", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = salir) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    TextButton(onClick = { onGuardar(limpiar(borrador)) }, enabled = hayCambios) {
                        Text("Guardar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = idioma == "es", onClick = { idioma = "es" }, label = { Text("Español") })
                FilterChip(selected = idioma == "en", onClick = { idioma = "en" }, label = { Text("English") })
            }
            Text(
                text = "Cambiar algo aquí no gasta generaciones. Cada idioma se edita aparte.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (sensibles.isNotEmpty()) {
                Aviso(
                    tono = TonoAviso.SEGURIDAD,
                    titulo = "Quita esto de tu CV",
                    texto = sensibles.joinToString("\n") { "• Tu ${it.tipo.nombre}: ${it.tipo.porQue}" },
                )
            }

            Seccion("Datos de contacto") {
                Campo("Nombre", cv.datos.nombre) { v -> cambiar { it.copy(datos = it.datos.copy(nombre = v)) } }
                Campo("Puesto que buscas", cv.datos.puesto) { v -> cambiar { it.copy(datos = it.datos.copy(puesto = v)) } }
                Campo("Ciudad", cv.datos.ciudad) { v -> cambiar { it.copy(datos = it.datos.copy(ciudad = v)) } }
                Campo("Teléfono", cv.datos.telefono, teclado = KeyboardType.Phone) { v ->
                    cambiar { it.copy(datos = it.datos.copy(telefono = v)) }
                }
                Campo("Correo", cv.datos.correo, teclado = KeyboardType.Email) { v ->
                    cambiar { it.copy(datos = it.datos.copy(correo = v)) }
                }
                Campo("LinkedIn", cv.datos.linkedin, teclado = KeyboardType.Uri) { v ->
                    cambiar { it.copy(datos = it.datos.copy(linkedin = v)) }
                }
                Campo("Portafolio o GitHub", cv.datos.portafolio, teclado = KeyboardType.Uri) { v ->
                    cambiar { it.copy(datos = it.datos.copy(portafolio = v)) }
                }
            }

            Seccion("Resumen") {
                Campo("2 o 3 líneas", cv.resumen, multilinea = true) { v -> cambiar { it.copy(resumen = v) } }
            }

            Seccion("Experiencia") {
                cv.experiencia.forEachIndexed { i, e ->
                    Bloque(onQuitar = { cambiar { it.copy(experiencia = it.experiencia.sinIndice(i)) } }) {
                        Campo("Puesto", e.puesto) { v -> cambiar { it.copy(experiencia = it.experiencia.conIndice(i) { x -> x.copy(puesto = v) }) } }
                        Campo("Empresa u organización", e.organizacion) { v ->
                            cambiar { it.copy(experiencia = it.experiencia.conIndice(i) { x -> x.copy(organizacion = v) }) }
                        }
                        Campo("Periodo", e.periodo) { v -> cambiar { it.copy(experiencia = it.experiencia.conIndice(i) { x -> x.copy(periodo = v) }) } }
                        Logros(e.logros) { nuevos -> cambiar { it.copy(experiencia = it.experiencia.conIndice(i) { x -> x.copy(logros = nuevos) }) } }
                    }
                }
                Agregar("Agregar experiencia") {
                    cambiar { it.copy(experiencia = it.experiencia + Experiencia("", "", "")) }
                }
            }

            Seccion("Proyectos") {
                cv.proyectos.forEachIndexed { i, p ->
                    Bloque(onQuitar = { cambiar { it.copy(proyectos = it.proyectos.sinIndice(i)) } }) {
                        Campo("Nombre del proyecto", p.nombre) { v -> cambiar { it.copy(proyectos = it.proyectos.conIndice(i) { x -> x.copy(nombre = v) }) } }
                        Campo("De qué se trata", p.descripcion, multilinea = true) { v ->
                            cambiar { it.copy(proyectos = it.proyectos.conIndice(i) { x -> x.copy(descripcion = v) }) }
                        }
                        Campo("Enlace", p.enlace, teclado = KeyboardType.Uri) { v ->
                            cambiar { it.copy(proyectos = it.proyectos.conIndice(i) { x -> x.copy(enlace = v) }) }
                        }
                        Logros(p.logros) { nuevos -> cambiar { it.copy(proyectos = it.proyectos.conIndice(i) { x -> x.copy(logros = nuevos) }) } }
                    }
                }
                Agregar("Agregar proyecto") { cambiar { it.copy(proyectos = it.proyectos + Proyecto("", "")) } }
            }

            Seccion("Formación") {
                cv.formacion.forEachIndexed { i, f ->
                    Bloque(onQuitar = { cambiar { it.copy(formacion = it.formacion.sinIndice(i)) } }) {
                        Campo("Carrera o título", f.titulo) { v -> cambiar { it.copy(formacion = it.formacion.conIndice(i) { x -> x.copy(titulo = v) }) } }
                        Campo("Escuela", f.institucion) { v -> cambiar { it.copy(formacion = it.formacion.conIndice(i) { x -> x.copy(institucion = v) }) } }
                        Campo("Periodo", f.periodo) { v -> cambiar { it.copy(formacion = it.formacion.conIndice(i) { x -> x.copy(periodo = v) }) } }
                        Campo("Nota (opcional)", f.nota) { v -> cambiar { it.copy(formacion = it.formacion.conIndice(i) { x -> x.copy(nota = v) }) } }
                    }
                }
                Agregar("Agregar formación") { cambiar { it.copy(formacion = it.formacion + Formacion("", "", "")) } }
            }

            Seccion("Cursos y certificaciones") {
                cv.certificaciones.forEachIndexed { i, c ->
                    Bloque(onQuitar = { cambiar { it.copy(certificaciones = it.certificaciones.sinIndice(i)) } }) {
                        Campo("Curso o certificación", c.nombre) { v ->
                            cambiar { it.copy(certificaciones = it.certificaciones.conIndice(i) { x -> x.copy(nombre = v) }) }
                        }
                        Campo("Quién lo dio", c.institucion) { v ->
                            cambiar { it.copy(certificaciones = it.certificaciones.conIndice(i) { x -> x.copy(institucion = v) }) }
                        }
                        Campo("Año", c.anio, teclado = KeyboardType.Number) { v ->
                            cambiar { it.copy(certificaciones = it.certificaciones.conIndice(i) { x -> x.copy(anio = v) }) }
                        }
                    }
                }
                Agregar("Agregar curso") { cambiar { it.copy(certificaciones = it.certificaciones + Certificacion("", "", "")) } }
            }

            Seccion("Habilidades") {
                Campo(
                    etiqueta = "Separadas por comas",
                    valor = cv.habilidades.joinToString(", "),
                    multilinea = true,
                ) { v -> cambiar { it.copy(habilidades = v.split(',').map(String::trim).filter(String::isNotEmpty)) } }
            }

            Seccion("Idiomas") {
                cv.idiomas.forEachIndexed { i, idiomaNivel ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = idiomaNivel.idioma,
                            onValueChange = { v -> cambiar { it.copy(idiomas = it.idiomas.conIndice(i) { x -> x.copy(idioma = v) }) } },
                            label = { Text("Idioma") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                        )
                        OutlinedTextField(
                            value = idiomaNivel.nivel,
                            onValueChange = { v -> cambiar { it.copy(idiomas = it.idiomas.conIndice(i) { x -> x.copy(nivel = v) }) } },
                            label = { Text("Nivel") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            shape = MaterialTheme.shapes.medium,
                        )
                        IconButton(onClick = { cambiar { it.copy(idiomas = it.idiomas.sinIndice(i)) } }) {
                            Icon(Icons.Default.Close, contentDescription = "Quitar idioma", modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Agregar("Agregar idioma") { cambiar { it.copy(idiomas = it.idiomas + IdiomaNivel("", "")) } }
            }

            Button(
                onClick = { onGuardar(limpiar(borrador)) },
                enabled = hayCambios,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("Guardar cambios")
            }
            Hueco(24.dp)
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Salir sin guardar?") },
            text = { Text("Los cambios que hiciste se pierden.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    onGuardar(limpiar(borrador))
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    onAtras()
                }) { Text("Salir sin guardar") }
            },
        )
    }
}

/** Al guardar se quitan las filas que quedaron vacías: un "Agregar" tocado por error no ensucia el PDF. */
private fun limpiar(par: ParCv): ParCv {
    fun Cv.limpio() = copy(
        experiencia = experiencia.filter { it.puesto.isNotBlank() || it.organizacion.isNotBlank() }
            .map { it.copy(logros = it.logros.filter(String::isNotBlank)) },
        proyectos = proyectos.filter { it.nombre.isNotBlank() || it.descripcion.isNotBlank() }
            .map { it.copy(logros = it.logros.filter(String::isNotBlank)) },
        formacion = formacion.filter { it.titulo.isNotBlank() || it.institucion.isNotBlank() },
        certificaciones = certificaciones.filter { it.nombre.isNotBlank() },
        idiomas = idiomas.filter { it.idioma.isNotBlank() },
    )
    return par.copy(es = par.es.limpio(), en = par.en.limpio())
}

private fun <T> List<T>.conIndice(indice: Int, cambio: (T) -> T): List<T> =
    mapIndexed { i, x -> if (i == indice) cambio(x) else x }

private fun <T> List<T>.sinIndice(indice: Int): List<T> = filterIndexed { i, _ -> i != indice }

@Composable
private fun Seccion(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Hueco(8.dp)
    EncabezadoSeccion(titulo)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = contenido)
}

@Composable
private fun Bloque(onQuitar: () -> Unit, contenido: @Composable ColumnScope.() -> Unit) {
    TarjetaSuave(relleno = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = contenido)
        TextButton(onClick = onQuitar, modifier = Modifier.align(Alignment.End)) {
            Text("Quitar", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun Campo(
    etiqueta: String,
    valor: String,
    multilinea: Boolean = false,
    teclado: KeyboardType = KeyboardType.Text,
    onCambio: (String) -> Unit,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = !multilinea,
        minLines = if (multilinea) 3 else 1,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(
            keyboardType = teclado,
            capitalization = if (teclado == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
        ),
    )
}

@Composable
private fun Logros(logros: List<String>, onCambio: (List<String>) -> Unit) {
    Text(
        text = "Logros: verbo + qué hiciste + resultado. Solo cifras que puedas defender.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    logros.forEachIndexed { i, logro ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = logro,
                onValueChange = { v -> onCambio(logros.conIndice(i) { v }) },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium,
                minLines = 2,
            )
            IconButton(onClick = { onCambio(logros.sinIndice(i)) }) {
                Icon(Icons.Default.Close, contentDescription = "Quitar logro", modifier = Modifier.size(18.dp))
            }
        }
    }
    TextButton(onClick = { onCambio(logros + "") }) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("Agregar logro", modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun Agregar(texto: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(texto, modifier = Modifier.padding(start = 6.dp))
    }
}
