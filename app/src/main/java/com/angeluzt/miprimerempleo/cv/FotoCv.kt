package com.angeluzt.miprimerempleo.cv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * La foto del CV, guardada como archivo nuestro.
 *
 * El selector de imágenes de Android da un permiso temporal sobre el URI: al reiniciar
 * el teléfono ese permiso desaparece y la foto se vuelve un hueco. Por eso se copia
 * una sola vez a la carpeta de la app y a partir de ahí es un archivo como cualquiera.
 *
 * Se guarda reducida: una foto de cámara moderna ocupa decenas de megas en memoria y
 * en el PDF se imprime a 88 puntos. Nadie nota la diferencia y la app no se muere.
 */
object FotoCv {

    private const val LADO_MAXIMO = 900
    private const val PREFIJO = "foto_cv_"

    /** Copia la imagen elegida y devuelve la ruta del archivo propio, o null si falla. */
    fun guardar(context: Context, origen: Uri): String? = runCatching {
        val mapa = leerReducida(context, origen) ?: return null
        borrarAnteriores(context)
        // El nombre cambia en cada foto para que la vista previa note el cambio.
        val destino = File(context.filesDir, "$PREFIJO${System.currentTimeMillis()}.jpg")
        destino.outputStream().use { mapa.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        destino.absolutePath
    }.getOrNull()

    fun cargar(ruta: String): Bitmap? {
        if (ruta.isBlank()) return null
        val archivo = File(ruta)
        if (!archivo.exists()) return null
        return runCatching { BitmapFactory.decodeFile(ruta) }.getOrNull()
    }

    fun borrar(ruta: String) {
        if (ruta.isBlank()) return
        runCatching { File(ruta).delete() }
    }

    private fun borrarAnteriores(context: Context) {
        context.filesDir.listFiles()
            ?.filter { it.name.startsWith(PREFIJO) }
            ?.forEach { runCatching { it.delete() } }
    }

    private fun leerReducida(context: Context, origen: Uri): Bitmap? {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(origen)?.use {
            BitmapFactory.decodeStream(it, null, limites)
        }
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null

        var muestreo = 1
        while (
            limites.outWidth / (muestreo * 2) >= LADO_MAXIMO &&
            limites.outHeight / (muestreo * 2) >= LADO_MAXIMO
        ) {
            muestreo *= 2
        }

        val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
        return context.contentResolver.openInputStream(origen)?.use {
            BitmapFactory.decodeStream(it, null, opciones)
        }
    }
}
