package com.angeluzt.miprimerempleo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.angeluzt.miprimerempleo.model.Capitulo
import com.angeluzt.miprimerempleo.model.Modulo
import com.angeluzt.miprimerempleo.model.ModuloMeta
import com.angeluzt.miprimerempleo.ui.EstadoApp
import com.angeluzt.miprimerempleo.ui.components.BarraProgreso
import com.angeluzt.miprimerempleo.ui.components.EncabezadoSeccion
import com.angeluzt.miprimerempleo.ui.components.Hueco
import com.angeluzt.miprimerempleo.ui.components.HuecoH
import com.angeluzt.miprimerempleo.ui.components.IconoEnCaja
import com.angeluzt.miprimerempleo.ui.components.Insignia
import com.angeluzt.miprimerempleo.ui.components.TarjetaMarca
import com.angeluzt.miprimerempleo.ui.components.TarjetaSuave
import com.angeluzt.miprimerempleo.ui.theme.SobreMarcaSuave

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaModulo(
    estado: EstadoApp,
    moduloId: String,
    onCapitulo: (String, String) -> Unit,
    onPaywall: () -> Unit,
    onAtras: () -> Unit,
) {
    val meta = estado.indice?.modulos?.firstOrNull { it.id == moduloId } ?: return
    val modulo = estado.modulos[moduloId] ?: return
    val cerrados = modulo.capitulos.count { !estado.capituloDesbloqueado(meta, it) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Cabecera(
                    meta = meta,
                    modulo = modulo,
                    paso = estado.pasoDe(moduloId),
                    pasos = estado.modulosEnOrden.size,
                    leidos = modulo.capitulos.count { it.id in estado.progreso.capitulosLeidos },
                )
            }
            item {
                Hueco(6.dp)
                EncabezadoSeccion(titulo = "Capítulos")
            }
            itemsIndexed(modulo.capitulos, key = { _, capitulo -> capitulo.id }) { indice, capitulo ->
                FilaCapitulo(
                    numero = indice + 1,
                    capitulo = capitulo,
                    leido = capitulo.id in estado.progreso.capitulosLeidos,
                    desbloqueado = estado.capituloDesbloqueado(meta, capitulo),
                    marcarGratis = capitulo.gratis && !estado.compras.puedeLeerTodo,
                    // Uno cerrado también se abre: enseña el comienzo y ahí mismo ofrece el pase
                    // o un anuncio. Mandar directo a la pantalla de pago escondía el anuncio.
                    onClick = { onCapitulo(moduloId, capitulo.id) },
                )
            }
            if (cerrados > 0) {
                item {
                    Hueco(4.dp)
                    TarjetaDesbloquear(cerrados, onPaywall)
                }
            }
        }
    }
}

@Composable
private fun Cabecera(meta: ModuloMeta, modulo: Modulo, paso: Int, pasos: Int, leidos: Int) {
    val total = modulo.capitulos.size
    TarjetaMarca {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoEnCaja(iconoDe(meta.icono), Color.White, fondo = Color.White.copy(alpha = 0.18f))
            HuecoH(14.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (paso > 0) "PASO $paso DE $pasos" else "MÓDULO ${meta.numero}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SobreMarcaSuave,
                )
                Text(
                    text = meta.titulo,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
            }
        }
        Text(
            text = modulo.intro,
            style = MaterialTheme.typography.bodyMedium,
            color = SobreMarcaSuave,
            modifier = Modifier.padding(top = 14.dp, bottom = 16.dp),
        )
        BarraProgreso(
            valor = if (total == 0) 0f else leidos.toFloat() / total,
            color = Color.White,
            fondo = Color.White.copy(alpha = 0.25f),
        )
        Text(
            text = when (leidos) {
                total -> "Lo terminaste · ${modulo.capitulos.sumOf { it.minutos }} min de lectura"
                else -> "$leidos de $total capítulos · ${modulo.capitulos.sumOf { it.minutos }} min en total"
            },
            style = MaterialTheme.typography.labelMedium,
            color = SobreMarcaSuave,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun FilaCapitulo(
    numero: Int,
    capitulo: Capitulo,
    leido: Boolean,
    desbloqueado: Boolean,
    marcarGratis: Boolean,
    onClick: () -> Unit,
) {
    val esquema = MaterialTheme.colorScheme
    val acento = when {
        leido -> esquema.tertiary
        !desbloqueado -> esquema.secondary
        else -> esquema.primary
    }
    TarjetaSuave(onClick = onClick, relleno = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(acento.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    leido -> Icon(Icons.Default.Check, contentDescription = "Leído", tint = acento, modifier = Modifier.size(20.dp))
                    !desbloqueado -> Icon(Icons.Default.Lock, contentDescription = "Cerrado", tint = acento, modifier = Modifier.size(17.dp))
                    else -> Text("$numero", style = MaterialTheme.typography.titleSmall, color = acento)
                }
            }
            HuecoH(14.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = capitulo.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = esquema.onSurface,
                )
                Text(
                    text = when {
                        leido -> "Leído · ${capitulo.minutos} min"
                        !desbloqueado -> "${capitulo.minutos} min · ábrelo con un pase o un anuncio"
                        else -> "${capitulo.minutos} min de lectura"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = esquema.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (marcarGratis) {
                HuecoH(8.dp)
                Insignia("Gratis", esquema.tertiary)
            }
        }
    }
}

@Composable
private fun TarjetaDesbloquear(cerrados: Int, onPaywall: () -> Unit) {
    TarjetaSuave(onClick = onPaywall, relleno = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoEnCaja(Icons.Default.WorkspacePremium, MaterialTheme.colorScheme.secondary)
            HuecoH(14.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (cerrados == 1) "Te falta 1 capítulo" else "Te faltan $cerrados capítulos",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Ábrelos todos con un pase, de un solo pago. O uno por uno con un anuncio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** El ícono de cada módulo, por el nombre que trae el índice del contenido. */
private fun iconoDe(nombre: String): ImageVector = when (nombre) {
    "evidencia" -> Icons.Default.Verified
    "idioma" -> Icons.Default.Translate
    "documento" -> Icons.Default.Description
    "buscar" -> Icons.Default.TravelExplore
    "escudo" -> Icons.Default.Shield
    "conversacion" -> Icons.Default.QuestionAnswer
    "lupa" -> Icons.Default.Business
    "dinero" -> Icons.Default.Payments
    "ruta" -> Icons.AutoMirrored.Filled.TrendingUp
    else -> Icons.AutoMirrored.Filled.MenuBook
}
