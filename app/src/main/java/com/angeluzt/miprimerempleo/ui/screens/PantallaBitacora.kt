package com.angeluzt.miprimerempleo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.angeluzt.miprimerempleo.bitacora.AnalisisBitacora
import com.angeluzt.miprimerempleo.bitacora.Entrevista
import com.angeluzt.miprimerempleo.bitacora.Resultado
import com.angeluzt.miprimerempleo.bitacora.Tema
import com.angeluzt.miprimerempleo.ui.components.Aviso
import com.angeluzt.miprimerempleo.ui.components.EncabezadoSeccion
import com.angeluzt.miprimerempleo.ui.components.Hueco
import com.angeluzt.miprimerempleo.ui.components.HuecoH
import com.angeluzt.miprimerempleo.ui.components.IconoEnCaja
import com.angeluzt.miprimerempleo.ui.components.Insignia
import com.angeluzt.miprimerempleo.ui.components.TarjetaMarca
import com.angeluzt.miprimerempleo.ui.components.TarjetaSuave
import com.angeluzt.miprimerempleo.ui.components.TonoAviso
import com.angeluzt.miprimerempleo.ui.theme.SobreMarcaSuave
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * La bitácora: arriba lo que toca estudiar, abajo las entrevistas. En ese orden porque lo útil
 * no es el historial, es la lista de preguntas que se te atoran.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaBitacora(
    entrevistas: List<Entrevista>,
    onNueva: () -> Unit,
    onAbrir: (String) -> Unit,
    onPreparada: (String, Boolean) -> Unit,
    onAtras: () -> Unit,
) {
    val estudio = remember(entrevistas) { AnalisisBitacora.listaDeEstudio(entrevistas) }
    val repetidas = remember(entrevistas) { AnalisisBitacora.seRepiten(entrevistas) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Bitácora de entrevistas", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNueva,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registrar entrevista") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { relleno ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (entrevistas.isEmpty()) {
                item { Vacia() }
                return@LazyColumn
            }

            item { Resumen(entrevistas, estudio) }

            item {
                Hueco(4.dp)
                EncabezadoSeccion(
                    titulo = "Tu lista de estudio",
                    subtitulo = if (estudio.isEmpty()) {
                        "No has marcado preguntas que no supiste. Cuando te atores con una, márcala: aquí aparece."
                    } else {
                        "Las preguntas que no supiste contestar. Prepara una respuesta y márcala."
                    },
                )
            }
            items(estudio, key = { "estudio-" + it.clave }) { tema ->
                FilaTema(tema) { onPreparada(tema.clave, !tema.preparada) }
            }

            val soloRepetidas = repetidas.filter { r -> estudio.none { it.clave == r.clave } }
            if (soloRepetidas.isNotEmpty()) {
                item {
                    Hueco(4.dp)
                    EncabezadoSeccion(
                        titulo = "Te las hacen siempre",
                        subtitulo = "Las supiste contestar, pero se repiten: tenlas afinadas.",
                    )
                }
                items(soloRepetidas, key = { "repite-" + it.clave }) { tema -> FilaRepetida(tema) }
            }

            item {
                Hueco(4.dp)
                EncabezadoSeccion("Tus entrevistas")
            }
            items(entrevistas, key = { it.id }) { entrevista ->
                FilaEntrevista(entrevista) { onAbrir(entrevista.id) }
            }
        }
    }
}

@Composable
private fun Vacia() {
    TarjetaMarca {
        IconoEnCaja(Icons.Default.QuestionAnswer, Color.White, fondo = Color.White.copy(alpha = 0.2f))
        Hueco(14.dp)
        Text(
            text = "Cada entrevista te enseña algo. Si no lo anotas, se te olvida.",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )
        Hueco(8.dp)
        Text(
            text = "Saliendo de una entrevista, registra qué te preguntaron y qué no supiste contestar. " +
                "Después de tres o cuatro vas a ver el patrón: siempre te atoras en lo mismo. " +
                "Aquí se arma tu lista de estudio sola.",
            style = MaterialTheme.typography.bodyMedium,
            color = SobreMarcaSuave,
        )
    }
    Hueco(12.dp)
    Aviso(
        tono = TonoAviso.SEGURIDAD,
        texto = "Tus notas se quedan en tu teléfono. No se mandan a ningún servidor.",
    )
}

@Composable
private fun Resumen(entrevistas: List<Entrevista>, estudio: List<Tema>) {
    val porEstudiar = estudio.count { !it.preparada }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Cifra("${entrevistas.size}", "entrevistas", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        Cifra("$porEstudiar", "por estudiar", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        Cifra(
            "${AnalisisBitacora.avances(entrevistas)}", "avanzaste",
            MaterialTheme.colorScheme.tertiary, Modifier.weight(1f),
        )
    }
}

@Composable
private fun Cifra(valor: String, etiqueta: String, color: Color, modifier: Modifier) {
    TarjetaSuave(modifier = modifier, relleno = 14.dp) {
        Text(valor, style = MaterialTheme.typography.headlineMedium, color = color)
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FilaTema(tema: Tema, onAlternar: () -> Unit) {
    TarjetaSuave(onClick = onAlternar, relleno = 14.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = if (tema.preparada) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (tema.preparada) "Preparada" else "Sin preparar",
                tint = if (tema.preparada) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(22.dp),
            )
            HuecoH(12.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = tema.pregunta,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (tema.preparada) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    if (tema.veces > 1) {
                        Insignia("Se repite ${tema.veces} veces", MaterialTheme.colorScheme.secondary)
                        HuecoH(8.dp)
                    }
                    Text(
                        text = tema.empresas.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaRepetida(tema: Tema) {
    TarjetaSuave(relleno = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Repeat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            HuecoH(12.dp)
            Text(
                text = tema.pregunta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            HuecoH(8.dp)
            Text(
                text = "×${tema.veces}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun FilaEntrevista(entrevista: Entrevista, onClick: () -> Unit) {
    val (color, tono) = when (entrevista.resultado) {
        Resultado.OFERTA, Resultado.AVANCE -> MaterialTheme.colorScheme.tertiary to "bien"
        Resultado.RECHAZO, Resultado.SIN_RESPUESTA -> MaterialTheme.colorScheme.onSurfaceVariant to "mal"
        Resultado.ESPERANDO -> MaterialTheme.colorScheme.primary to "espera"
    }
    TarjetaSuave(onClick = onClick, relleno = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = entrevista.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = fechaCorta(entrevista.fecha) + " · " + entrevista.resultado.etiqueta,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (tono == "bien") color else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (entrevista.noSupo > 0) {
                Insignia("${entrevista.noSupo} por estudiar", MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

private fun fechaCorta(milis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale("es", "MX")).format(Date(milis))
