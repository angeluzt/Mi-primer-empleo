package com.angeluzt.miprimerempleo.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.angeluzt.miprimerempleo.BuildConfig
import com.angeluzt.miprimerempleo.ui.AccionArchivo
import com.angeluzt.miprimerempleo.ui.ArchivoCv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Entrega un PDF ya dibujado: lo guarda donde la persona elija con el selector de Android, o
 * abre el menú de compartir. Lo usan el CV principal y los CV adaptados a vacantes.
 *
 * @param onEntregado Se llama al terminar, con un aviso para mostrar (o null si no hace falta).
 */
@Composable
fun EntregaDeArchivo(archivo: ArchivoCv?, onEntregado: (String?) -> Unit) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    val pendienteActual by rememberUpdatedState(archivo)
    val alTerminar by rememberUpdatedState(onEntregado)

    val guardarEn = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { destino ->
        val pendiente = pendienteActual
        if (destino == null || pendiente == null) {
            alTerminar(null)
        } else {
            alcance.launch {
                val guardado = withContext(Dispatchers.IO) {
                    runCatching {
                        contexto.contentResolver.openOutputStream(destino)?.use { salida ->
                            pendiente.archivo.inputStream().use { it.copyTo(salida) }
                        } ?: error("El sistema no dio dónde escribir.")
                    }.isSuccess
                }
                alTerminar(
                    if (guardado) "Listo, tu CV quedó guardado en PDF."
                    else "No pudimos guardar el archivo. Intenta con «Compartir».",
                )
            }
        }
    }

    LaunchedEffect(archivo) {
        val pendiente = archivo ?: return@LaunchedEffect
        when (pendiente.accion) {
            AccionArchivo.GUARDAR -> guardarEn.launch(pendiente.nombre)
            AccionArchivo.COMPARTIR -> {
                val uri = FileProvider.getUriForFile(
                    contexto,
                    "${BuildConfig.APPLICATION_ID}.fileprovider",
                    pendiente.archivo,
                )
                val envio = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, pendiente.nombre)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val abierto = runCatching {
                    contexto.startActivity(Intent.createChooser(envio, "Enviar tu CV"))
                }.isSuccess
                // Si no se abrió nada, decirlo: un botón que no hace nada y no explica
                // por qué es peor que un error.
                alTerminar(if (abierto) null else "No encontramos con qué compartir el PDF.")
            }
        }
    }
}
