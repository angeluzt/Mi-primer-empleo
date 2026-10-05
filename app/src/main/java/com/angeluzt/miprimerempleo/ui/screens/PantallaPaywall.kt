package com.angeluzt.miprimerempleo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.angeluzt.miprimerempleo.billing.Productos
import com.angeluzt.miprimerempleo.ui.EstadoApp
import com.angeluzt.miprimerempleo.ui.components.Aviso
import com.angeluzt.miprimerempleo.ui.components.Hueco
import com.angeluzt.miprimerempleo.ui.components.HuecoH
import com.angeluzt.miprimerempleo.ui.components.Insignia
import com.angeluzt.miprimerempleo.ui.components.TarjetaMarca
import com.angeluzt.miprimerempleo.ui.components.TonoAviso
import com.angeluzt.miprimerempleo.ui.theme.SobreMarcaSuave

/**
 * Dos pases de pago único, y la recarga para quien ya tiene el completo. Cada beneficio de la
 * lista existe en la app: antes aquí se prometía una bitácora que no había.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPaywall(
    estado: EstadoApp,
    onComprar: (String) -> Unit,
    onRestaurar: () -> Unit,
    onAtras: () -> Unit,
) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TarjetaMarca {
                Text("Elige cómo seguir", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Hueco(6.dp)
                Text(
                    text = "Un pago. Una vez. Tuyo para siempre. Sin suscripción, nada se renueva ni se cobra otra vez.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SobreMarcaSuave,
                )
            }

            if (estado.compras.pagoPendiente) {
                Aviso(
                    tono = TonoAviso.INFO,
                    titulo = "Tu pago está en camino",
                    texto = "Si pagaste en efectivo, Google puede tardar hasta 48 horas en confirmarlo. " +
                        "En cuanto lo haga, todo se desbloquea solo. No vuelvas a pagar.",
                )
            }

            PlanCompleto(estado, onComprar)

            if (!estado.compras.puedeLeerTodo) {
                PlanLectura(estado, onComprar)
            }

            if (estado.compras.tienePase) {
                Recarga(estado, onComprar)
            }

            TextButton(onClick = onRestaurar, modifier = Modifier.fillMaxWidth()) {
                Text("Ya compré antes — restaurar mi compra")
            }

            Text(
                text = "No vendemos suscripciones ni ponemos anuncios que interrumpan. " +
                    "Algunos enlaces a cursos y libros son de afiliado y están marcados como tales; " +
                    "eso no cambia lo que recomendamos.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PlanCompleto(estado: EstadoApp, onComprar: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Pase Completo",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                HuecoH(10.dp)
                Insignia("Recomendado", MaterialTheme.colorScheme.primary)
            }
            Text(
                text = "La guía completa y todas las herramientas para conseguir el trabajo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            Beneficio("Los ${estado.indice?.modulos?.size ?: 10} módulos completos, sin candados ni anuncios")
            Beneficio("Tu CV con IA, en español e inglés, sin inventar nada")
            Beneficio("Adáptalo a cada vacante: qué cubres, qué te falta y el mensaje para postularte")
            Beneficio("Revisión de tu CV como la haría un reclutador")
            Beneficio("${Productos.CVS_INCLUIDOS_EN_PASE} generaciones incluidas")
            Beneficio("10 diseños de PDF para elegir, con vista previa")
            Beneficio("Bitácora de entrevistas con tu lista de estudio")

            Hueco(12.dp)
            if (estado.compras.tienePase) {
                Text(
                    text = "Ya tienes el Pase Completo. Gracias.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            } else {
                Button(
                    onClick = { onComprar(Productos.PASE_COMPLETO) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    Text(
                        text = estado.compras.precios[Productos.PASE_COMPLETO]
                            ?.let { "Desbloquear todo · $it" } ?: "Desbloquear todo",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanLectura(estado: EstadoApp, onComprar: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Solo la guía",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Si vienes por el contenido y tu CV ya lo tienes resuelto.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
            )
            Beneficio("Los ${estado.indice?.modulos?.size ?: 10} módulos completos, sin anuncios")
            Beneficio("Módulo de estafas y seguridad")
            Beneficio("Libros y cursos recomendados")
            Hueco(10.dp)
            OutlinedButton(
                onClick = { onComprar(Productos.PASE_LECTURA) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    estado.compras.precios[Productos.PASE_LECTURA]
                        ?.let { "Desbloquear la guía · $it" } ?: "Desbloquear la guía",
                )
            }
        }
    }
}

@Composable
private fun Recarga(estado: EstadoApp, onComprar: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = "¿Se te acabaron las generaciones?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Te quedan ${estado.creditosCv}. La recarga suma ${Productos.CVS_POR_RECARGA} más, " +
                    "para generar o adaptar tu CV a más vacantes. También es pago único.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
            )
            OutlinedButton(
                onClick = { onComprar(Productos.RECARGA_CV) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    estado.compras.precios[Productos.RECARGA_CV]
                        ?.let { "Comprar recarga · $it" } ?: "Comprar recarga",
                )
            }
        }
    }
}

@Composable
private fun Beneficio(texto: String) {
    Row(
        Modifier.padding(bottom = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp),
        )
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 11.dp),
        )
    }
}
