package com.angeluzt.miprimerempleo.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.angeluzt.miprimerempleo.cv.ColorAcento
import com.angeluzt.miprimerempleo.cv.Cv
import com.angeluzt.miprimerempleo.cv.Diseno
import com.angeluzt.miprimerempleo.cv.FotoCv
import com.angeluzt.miprimerempleo.cv.Fuente
import com.angeluzt.miprimerempleo.cv.Plantilla
import com.angeluzt.miprimerempleo.cv.Plantillas
import com.angeluzt.miprimerempleo.cv.PreviewCv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * En vez de una lista cerrada de plantillas, la persona combina cuatro cosas.
 * La vista previa se regenera renderizando el PDF real en cada cambio, así que
 * lo que ve aquí es exactamente lo que va a exportar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPlantillas(
    cv: Cv,
    plantillaElegida: String,
    fotoRuta: String,
    onElegir: (String) -> Unit,
    onElegirFoto: (String) -> Unit,
    onAtras: () -> Unit,
) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val preview = remember { PreviewCv(contexto) }

    var plantilla by remember(plantillaElegida) {
        mutableStateOf(Plantillas.porId(plantillaElegida))
    }
    var imagen by remember { mutableStateOf<Bitmap?>(null) }
    var paginas by remember { mutableStateOf(1) }
    var generando by remember { mutableStateOf(true) }

    val foto by produceState<Bitmap?>(initialValue = null, fotoRuta) {
        value = withContext(Dispatchers.IO) { FotoCv.cargar(fotoRuta) }
    }

    val elegirFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { origen ->
        if (origen == null) return@rememberLauncherForActivityResult
        alcance.launch {
            val ruta = withContext(Dispatchers.IO) { FotoCv.guardar(contexto, origen) }
            if (ruta != null) onElegirFoto(ruta)
        }
    }

    LaunchedEffect(plantilla, cv, foto) {
        generando = true
        val vista = preview.completa(cv, plantilla, foto, ancho = 720)
        imagen = vista.imagen
        paginas = vista.paginas
        generando = false
        onElegir(plantilla.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Formato de tu CV", style = MaterialTheme.typography.titleMedium) },
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
                .padding(horizontal = 20.dp),
        ) {
            VistaPrevia(imagen, generando)

            if (paginas > 1) {
                Text(
                    text = "Tu CV ocupa $paginas hojas. Para un primer empleo conviene que quepa " +
                        "en una: prueba la letra Compacta o quita lo que menos aporte.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            AvisoFiltros(plantilla)
            Spacer(Modifier.height(20.dp))

            Grupo("Diseño") {
                Diseno.entries.forEach { diseno ->
                    FilterChip(
                        selected = plantilla.diseno == diseno,
                        onClick = { plantilla = plantilla.copy(diseno = diseno) },
                        label = { Text(diseno.etiqueta) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            Text(
                text = plantilla.diseno.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 18.dp),
            )

            Grupo("Letra") {
                Fuente.entries.forEach { fuente ->
                    FilterChip(
                        selected = plantilla.fuente == fuente,
                        onClick = { plantilla = plantilla.copy(fuente = fuente) },
                        label = { Text(fuente.etiqueta) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            Grupo("Color") {
                ColorAcento.entries.forEach { color ->
                    Muestra(
                        color = Color(color.valor),
                        elegido = plantilla.color == color,
                        onClick = { plantilla = plantilla.copy(color = color) },
                    )
                }
            }

            Foto(
                plantilla = plantilla,
                hayFoto = foto != null,
                onCambiar = {
                    plantilla = plantilla.copy(conFoto = it)
                    if (it && foto == null) {
                        elegirFoto.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }
                },
                onElegirArchivo = {
                    elegirFoto.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )

            Text(
                text = "${Plantillas.combinaciones} combinaciones posibles. " +
                    "Tu CV no va a verse igual al de los demás.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, bottom = 28.dp),
            )
        }
    }
}

@Composable
private fun VistaPrevia(imagen: Bitmap?, generando: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .aspectRatio(595f / 842f)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        imagen?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Vista previa de tu CV",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        if (generando) {
            CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun AvisoFiltros(plantilla: Plantilla) {
    val seguro = plantilla.aptaParaFiltros
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (seguro) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (seguro) Icons.Default.Check else Icons.Default.WarningAmber,
                contentDescription = null,
                tint = if (seguro) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = if (seguro) {
                    "Segura para bolsas de trabajo. Los filtros automáticos la leen bien."
                } else {
                    "A dos columnas: los filtros automáticos la revuelven. Úsala solo " +
                        "para entregar en mano o por correo directo a una persona."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun Foto(
    plantilla: Plantilla,
    hayFoto: Boolean,
    onCambiar: (Boolean) -> Unit,
    onElegirArchivo: () -> Unit,
) {
    Column(Modifier.padding(top = 18.dp, bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Incluir foto",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (plantilla.admiteFoto) {
                        "Déjala apagada para bolsas de trabajo. Enciéndela solo si vas a entregar en mano."
                    } else {
                        "El diseño «${plantilla.diseno.etiqueta}» no lleva foto: no hay dónde ponerla sin apretar el texto."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = plantilla.conFoto && plantilla.admiteFoto,
                enabled = plantilla.admiteFoto,
                onCheckedChange = onCambiar,
            )
        }
        if (plantilla.conFoto && plantilla.admiteFoto) {
            TextButton(onClick = onElegirArchivo) {
                Text(if (hayFoto) "Cambiar la foto" else "Elegir una foto")
            }
        }
    }
}

@Composable
private fun Grupo(titulo: String, contenido: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = 6.dp)) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) { contenido() }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun Muestra(color: Color, elegido: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 10.dp)
            .size(38.dp)
            .background(color, CircleShape)
            .border(
                width = if (elegido) 3.dp else 0.dp,
                color = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (elegido) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Elegido",
                tint = Color.White,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}
