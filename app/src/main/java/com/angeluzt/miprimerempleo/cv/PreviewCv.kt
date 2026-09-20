package com.angeluzt.miprimerempleo.cv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Convierte el PDF en imagen para que la persona vea cómo va a quedar su CV
 * antes de decidir. Se dibuja el PDF de verdad, no una maqueta parecida:
 * lo que se ve aquí es exactamente lo que se exporta.
 */
class PreviewCv(private val context: Context) {

    private val renderizador = RenderizadorCv(context)

    /** Miniatura para la cuadrícula de plantillas. */
    suspend fun miniatura(cv: Cv, plantilla: Plantilla, foto: Bitmap?, ancho: Int = 320): Bitmap? =
        generar(cv, plantilla, foto, ancho, "preview_${plantilla.id}")

    /** Vista grande para revisar el resultado antes de exportar. */
    suspend fun completa(cv: Cv, plantilla: Plantilla, foto: Bitmap?, ancho: Int = 1080): Bitmap? =
        generar(cv, plantilla, foto, ancho, "preview_grande_${plantilla.id}")

    private suspend fun generar(
        cv: Cv,
        plantilla: Plantilla,
        foto: Bitmap?,
        ancho: Int,
        nombre: String,
    ): Bitmap? = withContext(Dispatchers.IO) {
        var archivo: File? = null
        try {
            archivo = renderizador.exportar(cv, plantilla, foto, nombre)
            ParcelFileDescriptor.open(archivo, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { pdf ->
                    pdf.openPage(0).use { pagina ->
                        val alto = (ancho.toFloat() * pagina.height / pagina.width).toInt()
                        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
                        // Sin esto las zonas sin tinta salen transparentes y se ven negras.
                        bitmap.eraseColor(Color.WHITE)
                        pagina.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (e: Exception) {
            null
        } finally {
            archivo?.delete()
        }
    }
}
