package com.angeluzt.miprimerempleo.cv

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Dibuja un CV en PDF con la plantilla y la foto que la persona eligió.
 * Lo usan el CV principal y los CV adaptados a vacantes, que se exportan igual.
 */
object ExportadorCv {

    /** Devuelve el archivo y el nombre con que se ofrece guardarlo. */
    suspend fun pdf(
        context: Context,
        cv: Cv,
        plantillaId: String,
        fotoRuta: String,
        sufijo: String,
    ): Pair<File, String> = withContext(Dispatchers.IO) {
        val nombre = nombreDeArchivo(cv.datos.nombre, sufijo)
        val archivo = RenderizadorCv(context)
            .exportar(cv, Plantillas.porId(plantillaId), FotoCv.cargar(fotoRuta), nombre)
        archivo to "$nombre.pdf"
    }

    /** "CV_Ana_López_ES": sin símbolos ni espacios, que algunos correos y bolsas rechazan. */
    fun nombreDeArchivo(nombre: String, sufijo: String): String {
        val limpio = nombre.trim().ifBlank { "CV" }
            .replace(Regex("[^\\p{L}\\p{N} ]"), "")
            .trim()
            .replace(Regex("\\s+"), "_")
            .take(40)
        val final = sufijo.replace(Regex("[^\\p{L}\\p{N}]+"), "_").trim('_').take(30)
        return listOf("CV", limpio, final).filter { it.isNotBlank() }.joinToString("_")
    }
}
