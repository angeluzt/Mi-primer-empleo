package com.angeluzt.miprimerempleo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.angeluzt.miprimerempleo.billing.Productos
import com.angeluzt.miprimerempleo.ui.EstadoApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPaywall(
    estado: EstadoApp,
    onComprar: (String) -> Unit,
    onRestaurar: () -> Unit,
    onAtras: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Pase Completo",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Un pago. Una vez. Tuyo para siempre.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 22.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(Modifier.padding(22.dp)) {
                    listOf(
                        "Los 9 módulos completos, sin candados",
                        "Generador de CV con IA en español e inglés",
                        "${Productos.CVS_INCLUIDOS_EN_PASE} generaciones de CV incluidas",
                        "Bitácora de entrevistas",
                        "Banderas rojas y verdes para evaluar empresas",
                        "Módulo de estafas y seguridad completo",
                        "Actualizaciones futuras incluidas",
                    ).forEach { beneficio ->
                        Row(
                            Modifier.padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(19.dp),
                            )
                            Text(
                                text = beneficio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 11.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

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
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(
                                text = estado.compras.precios[Productos.PASE_COMPLETO]
                                    ?.let { "Desbloquear todo · $it" }
                                    ?: "Desbloquear todo",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Text(
                            text = "Sin suscripción. No se renueva ni se cobra otra vez.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            if (estado.compras.tienePase) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            text = "¿Se te acabaron las generaciones de CV?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Recarga de ${Productos.CVS_POR_RECARGA} generaciones más. También es pago único.",
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
                Spacer(Modifier.height(12.dp))
            }

            TextButton(
                onClick = onRestaurar,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Ya compré antes — restaurar mi compra")
            }

            Text(
                text = "Esta app no vende suscripciones ni muestra publicidad. Algunos enlaces a cursos son de afiliado y están marcados como tales; eso no cambia lo que recomendamos.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, bottom = 24.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}
