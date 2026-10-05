package com.angeluzt.miprimerempleo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.angeluzt.miprimerempleo.data.PoliticaAnuncios
import com.angeluzt.miprimerempleo.model.Bloque
import com.angeluzt.miprimerempleo.model.Subtitulo
import com.angeluzt.miprimerempleo.ui.EstadoApp
import com.angeluzt.miprimerempleo.ui.Siguiente
import com.angeluzt.miprimerempleo.ui.components.BloqueVista
import com.angeluzt.miprimerempleo.ui.components.Hueco
import com.angeluzt.miprimerempleo.ui.components.HuecoH
import com.angeluzt.miprimerempleo.ui.components.IconoEnCaja
import com.angeluzt.miprimerempleo.ui.components.TarjetaMarca
import com.angeluzt.miprimerempleo.ui.components.TarjetaSuave
import com.angeluzt.miprimerempleo.ui.theme.Indigo
import com.angeluzt.miprimerempleo.ui.theme.SobreMarcaSuave

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaLector(
    estado: EstadoApp,
    moduloId: String,
    capituloId: String,
    onLeido: (String) -> Unit,
    onAccion: (String, Int) -> Unit,
    onEnlace: (String) -> Unit,
    onPaywall: () -> Unit,
    onPrepararAnuncio: () -> Unit,
    onVerAnuncio: (String) -> Unit,
    onAvisoVisto: () -> Unit,
    onSiguiente: (String, String) -> Unit,
    onAtras: () -> Unit,
) {
    val meta = estado.indice?.modulos?.firstOrNull { it.id == moduloId } ?: return
    val capitulo = estado.capitulosDe(moduloId).firstOrNull { it.id == capituloId } ?: return
    val desbloqueado = estado.capituloDesbloqueado(meta, capitulo)
    val siguiente = remember(moduloId, capituloId, estado.compras, estado.anuncios) {
        estado.despuesDe(moduloId, capituloId)
    }
    val avisos = remember { SnackbarHostState() }

    LaunchedEffect(capituloId, desbloqueado) {
        if (desbloqueado) onLeido(capituloId) else onPrepararAnuncio()
    }

    LaunchedEffect(estado.avisoAnuncio) {
        estado.avisoAnuncio?.let {
            avisos.showSnackbar(it)
            onAvisoVisto()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(avisos) },
        topBar = {
            TopAppBar(
                title = { Text(meta.titulo, style = MaterialTheme.typography.labelLarge) },
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
        ) {
            item {
                Text(
                    text = capitulo.titulo,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${capitulo.minutos} min de lectura",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 22.dp),
                )
            }

            if (desbloqueado) {
                items(capitulo.bloques) { bloque ->
                    BloqueVista(
                        bloque = bloque,
                        accionesHechas = estado.progreso.accionesHechas,
                        onAccion = onAccion,
                        onEnlace = onEnlace,
                        pais = estado.progreso.pais,
                    )
                }
                item {
                    Hueco(12.dp)
                    if (siguiente != null) {
                        TarjetaSiguienteCapitulo(siguiente) { onSiguiente(siguiente.modulo.id, siguiente.capitulo.id) }
                    } else {
                        FinDeRuta()
                    }
                }
            } else {
                item { Adelanto(adelanto(capitulo.bloques), estado.progreso.pais, onEnlace) }
                item {
                    CorteDePago(
                        anunciosRestantes = estado.anuncios.restantesHoy,
                        onPaywall = onPaywall,
                        onVerAnuncio = { onVerAnuncio(capituloId) },
                    )
                }
            }
        }
    }
}

/**
 * Lo que se enseña de un capítulo cerrado. Antes era el primer bloque, y en varios capítulos
 * el primer bloque es un subtítulo: la persona llegaba al muro sin haber leído una sola línea.
 * Ahora se muestran bloques hasta juntar dos con contenido de verdad.
 */
private fun adelanto(bloques: List<Bloque>): List<Bloque> {
    val elegidos = mutableListOf<Bloque>()
    var conContenido = 0
    for (bloque in bloques) {
        elegidos += bloque
        if (bloque !is Subtitulo) conContenido++
        if (conContenido >= 2 || elegidos.size >= 4) break
    }
    return elegidos
}

/** El adelanto se desvanece hacia abajo: se nota que hay más, sin cortar a media frase. */
@Composable
private fun Adelanto(bloques: List<Bloque>, pais: String, onEnlace: (String) -> Unit) {
    val fondo = MaterialTheme.colorScheme.background
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .clipToBounds()
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to fondo,
                    ),
                )
            },
    ) {
        bloques.forEach { bloque ->
            BloqueVista(
                bloque = bloque,
                accionesHechas = emptySet(),
                onAccion = { _, _ -> },
                onEnlace = onEnlace,
                pais = pais,
            )
        }
    }
}

@Composable
private fun TarjetaSiguienteCapitulo(siguiente: Siguiente, onClick: () -> Unit) {
    TarjetaSuave(onClick = onClick) {
        Text(
            text = "SIGUIENTE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = siguiente.capitulo.titulo,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${siguiente.modulo.titulo} · ${siguiente.capitulo.minutos} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HuecoH(12.dp)
            Icon(
                imageVector = if (siguiente.desbloqueado) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun FinDeRuta() {
    TarjetaSuave {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoEnCaja(Icons.Default.EmojiEvents, MaterialTheme.colorScheme.tertiary)
            HuecoH(14.dp)
            Column {
                Text(
                    text = "Terminaste tu ruta",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Ya leíste todo. Lo que sigue pasa fuera de la app: postúlate y registra tus entrevistas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CorteDePago(
    anunciosRestantes: Int,
    onPaywall: () -> Unit,
    onVerAnuncio: () -> Unit,
) {
    TarjetaMarca {
        Text(
            text = "Aquí sigue lo importante",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
        Hueco(8.dp)
        Text(
            text = "Con un pase abres este capítulo y todos los demás, para siempre. Un solo pago, " +
                "sin suscripción. Si solo te interesa leer, hay uno más barato.",
            style = MaterialTheme.typography.bodyMedium,
            color = SobreMarcaSuave,
        )
        Hueco(18.dp)
        Button(
            onClick = onPaywall,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Indigo),
        ) {
            Text("Ver los pases")
        }

        // La salida para quien de verdad no puede pagar. El anuncio nunca
        // aparece solo: se abre porque la persona tocó este botón, y abre
        // este capítulo, no el módulo entero.
        if (anunciosRestantes > 0) {
            Hueco(10.dp)
            OutlinedButton(
                onClick = onVerAnuncio,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
            ) {
                Text("Ver un anuncio y abrir este capítulo")
            }
            Text(
                text = "Te quedan $anunciosRestantes de ${PoliticaAnuncios.MAXIMO_POR_DIA} hoy. " +
                    "Con un pase no vuelves a ver ninguno.",
                style = MaterialTheme.typography.labelMedium,
                color = SobreMarcaSuave,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Hueco(10.dp)
            Text(
                text = "Hoy ya usaste los ${PoliticaAnuncios.MAXIMO_POR_DIA} anuncios que " +
                    "desbloquean capítulos. Mañana hay más.",
                style = MaterialTheme.typography.labelMedium,
                color = SobreMarcaSuave,
            )
        }
    }
}
