package com.angeluzt.miprimerempleo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.angeluzt.miprimerempleo.BuildConfig
import com.angeluzt.miprimerempleo.model.Ruta
import com.angeluzt.miprimerempleo.ui.EstadoApp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PantallaAjustes(
    estado: EstadoApp,
    onElegirRuta: (String) -> Unit,
    onElegirPais: (String) -> Unit,
    onGuardarLlave: (String) -> Unit,
    onRestaurar: () -> Unit,
    onAtras: () -> Unit,
) {
    val indice = estado.indice ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", style = MaterialTheme.typography.titleMedium) },
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
                text = "Tu etapa",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Cambiarla reordena los módulos. No pierdes tu progreso ni tus puntos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            )
            indice.rutas.forEach { ruta ->
                TarjetaEtapa(
                    ruta = ruta,
                    elegida = ruta.id == estado.progreso.ruta,
                    onClick = { onElegirRuta(ruta.id) },
                )
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text = "Tu país",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Cambia las prestaciones, las bolsas de trabajo y las autoridades que te mostramos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
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

            Spacer(Modifier.height(26.dp))

            OutlinedButton(onClick = onRestaurar, modifier = Modifier.fillMaxWidth()) {
                Text("Restaurar mis compras")
            }

            if (BuildConfig.LLAVE_LOCAL_PERMITIDA) {
                Spacer(Modifier.height(26.dp))
                LlaveDePrueba(estado.progreso.llaveOpenAi, onGuardarLlave)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TarjetaEtapa(ruta: Ruta, elegida: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (elegida) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            if (elegida) 2.dp else 1.dp,
            if (elegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = ruta.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = ruta.subtitulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            if (elegida) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Tu etapa actual",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
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
        shape = RoundedCornerShape(16.dp),
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
            Spacer(Modifier.height(12.dp))
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
