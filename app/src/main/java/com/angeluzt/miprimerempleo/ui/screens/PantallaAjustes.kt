package com.angeluzt.miprimerempleo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.angeluzt.miprimerempleo.BuildConfig
import com.angeluzt.miprimerempleo.model.Ruta
import com.angeluzt.miprimerempleo.ui.EstadoApp
import com.angeluzt.miprimerempleo.ui.components.Aviso
import com.angeluzt.miprimerempleo.ui.components.EncabezadoSeccion
import com.angeluzt.miprimerempleo.ui.components.Hueco
import com.angeluzt.miprimerempleo.ui.components.HuecoH
import com.angeluzt.miprimerempleo.ui.components.IconoEnCaja
import com.angeluzt.miprimerempleo.ui.components.TarjetaSuave
import com.angeluzt.miprimerempleo.ui.components.TonoAviso

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PantallaAjustes(
    estado: EstadoApp,
    onElegirRuta: (String) -> Unit,
    onElegirPais: (String) -> Unit,
    onGuardarLlave: (String) -> Unit,
    onRestaurar: () -> Unit,
    onBorrarDatos: () -> Unit,
    onAtras: () -> Unit,
) {
    val indice = estado.indice ?: return
    var confirmarBorrado by remember { mutableStateOf(false) }
    var borrado by rememberSaveable { mutableStateOf(false) }

    if (confirmarBorrado) {
        ConfirmarBorrado(
            onBorrar = {
                confirmarBorrado = false
                borrado = true
                onBorrarDatos()
            },
            onCancelar = { confirmarBorrado = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
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
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            EncabezadoSeccion(
                titulo = "Tu etapa",
                subtitulo = "Cambiarla reordena los módulos. No pierdes tu progreso ni tus puntos.",
            )
            indice.rutas.forEach { ruta ->
                TarjetaEtapa(
                    ruta = ruta,
                    elegida = ruta.id == estado.progreso.ruta,
                    onClick = { onElegirRuta(ruta.id) },
                )
            }

            Hueco(12.dp)
            EncabezadoSeccion(
                titulo = "Tu país",
                subtitulo = "Cambia las prestaciones, las bolsas de trabajo y las autoridades que te mostramos.",
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                indice.paises.forEach { pais ->
                    FilterChip(
                        selected = estado.progreso.pais == pais.codigo,
                        onClick = { onElegirPais(pais.codigo) },
                        label = { Text(pais.nombre) },
                    )
                }
            }

            Hueco(12.dp)
            EncabezadoSeccion("Tus compras")
            TarjetaCompras(estado, onRestaurar)

            Hueco(12.dp)
            EncabezadoSeccion("Tu privacidad")
            TarjetaPrivacidad(borrado = borrado, onBorrar = { confirmarBorrado = true })

            if (BuildConfig.LLAVE_LOCAL_PERMITIDA) {
                Hueco(12.dp)
                LlaveDePrueba(estado.progreso.llaveOpenAi, onGuardarLlave)
            }

            Hueco(12.dp)
            Text(
                text = "Conseguir Trabajo: Mi primer empleo · versión ${BuildConfig.VERSION_NAME}\n" +
                    "Tipografía Plus Jakarta Sans, con licencia SIL Open Font License 1.1.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaEtapa(ruta: Ruta, elegida: Boolean, onClick: () -> Unit) {
    val esquema = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (elegida) esquema.primaryContainer else esquema.surface),
        border = BorderStroke(if (elegida) 2.dp else 1.dp, if (elegida) esquema.primary else esquema.outline),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ruta.titulo, style = MaterialTheme.typography.titleMedium, color = esquema.onSurface)
                Text(
                    text = ruta.subtitulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = esquema.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            if (elegida) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Tu etapa actual",
                    tint = esquema.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun TarjetaCompras(estado: EstadoApp, onRestaurar: () -> Unit) {
    val compras = estado.compras
    TarjetaSuave(relleno = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoEnCaja(Icons.Default.WorkspacePremium, MaterialTheme.colorScheme.secondary)
            HuecoH(14.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = when {
                        compras.tienePase -> "Tienes el Pase Completo"
                        compras.tieneLectura -> "Tienes el pase de lectura"
                        else -> "Todavía no tienes un pase"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when {
                        compras.tienePase -> "Te quedan ${estado.creditosCv} generaciones de CV. Sin suscripción."
                        compras.tieneLectura -> "Toda la guía, sin anuncios. Un solo pago."
                        else -> "¿Ya compraste en otro teléfono? Con la misma cuenta de Google lo recuperas aquí."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Hueco(12.dp)
        OutlinedButton(onClick = onRestaurar, modifier = Modifier.fillMaxWidth()) {
            Text("Restaurar mis compras")
        }
    }
}

/**
 * Lo que pasa con los datos de la persona, dicho sin letra chica. Cada frase tiene que seguir
 * siendo cierta: si cambia lo que se manda a la IA o lo que se guarda, se cambia aquí.
 */
@Composable
private fun TarjetaPrivacidad(borrado: Boolean, onBorrar: () -> Unit) {
    TarjetaSuave(relleno = 16.dp) {
        Dato(
            Icons.Default.PhoneAndroid,
            "Tu CV, tus respuestas, tu foto y tu bitácora se guardan en este teléfono. No tenemos una cuenta tuya donde guardarlos.",
        )
        Dato(
            Icons.Default.Shield,
            "Antes de mandar algo a la IA quitamos tus identificaciones (CURP, DNI, RUT…). Tu teléfono nunca se manda.",
        )
        Dato(
            Icons.Default.AutoAwesome,
            "La IA (OpenAI) recibe solo lo necesario para redactar, revisar o adaptar tu CV, y pasa por nuestro servidor, " +
                "que no guarda el contenido. Tu bitácora nunca sale del teléfono.",
        )
        Dato(
            Icons.Default.Backup,
            "Si tienes activado el respaldo de Android, tus datos se incluyen en el respaldo de tu cuenta de Google.",
        )
        Hueco(6.dp)
        if (borrado) {
            Aviso(TonoAviso.EXITO, "Listo: tu CV y tus notas se borraron de este teléfono.")
        } else {
            OutlinedButton(
                onClick = onBorrar,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                HuecoH(8.dp)
                Text("Borrar mi CV y mis notas")
            }
        }
    }
}

@Composable
private fun Dato(icono: ImageVector, texto: String) {
    Row(Modifier.padding(bottom = 12.dp)) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        HuecoH(12.dp)
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ConfirmarBorrado(onBorrar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("¿Borrar tu CV y tus notas?") },
        text = {
            Text(
                "Se borran de este teléfono tu CV, tus respuestas, tu foto, las vacantes que revisaste, " +
                    "los PDF que exportaste y tu bitácora. Tu avance en la guía y tus compras se quedan.\n\n" +
                    "No se puede deshacer.",
            )
        },
        confirmButton = {
            TextButton(
                onClick = onBorrar,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Borrar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

/**
 * Solo aparece en compilaciones de depuración. Sirve para probar el generador de CV
 * en un teléfono real sin haber desplegado el backend.
 */
@Composable
private fun LlaveDePrueba(llaveGuardada: String, onGuardar: (String) -> Unit) {
    var llave by remember(llaveGuardada) { mutableStateOf(llaveGuardada) }
    var guardada by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = "Modo desarrollador",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "Pega tu llave de OpenAI y la app llamará directo a la API, sin backend. " +
                    "Se guarda solo en este teléfono y esta sección no existe en la versión publicada.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            )
            OutlinedTextField(
                value = llave,
                onValueChange = { llave = it; guardada = false },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("sk-…") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
            )
            Hueco(12.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onGuardar(llave); guardada = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (guardada) "Guardada" else "Guardar llave")
                }
                OutlinedButton(
                    onClick = { llave = ""; onGuardar(""); guardada = false },
                ) {
                    Text("Borrar")
                }
            }
            Text(
                text = "Usa una llave de pruebas con límite de gasto bajo, nunca la de producción.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
