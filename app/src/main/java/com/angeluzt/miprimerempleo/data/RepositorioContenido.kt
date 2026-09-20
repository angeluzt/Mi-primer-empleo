package com.angeluzt.miprimerempleo.data

import android.content.Context
import com.angeluzt.miprimerempleo.model.Indice
import com.angeluzt.miprimerempleo.model.Modulo
import com.angeluzt.miprimerempleo.model.ModuloMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * El contenido viaja dentro del APK: la guía funciona sin internet y sin costo de servidor.
 * Solo el generador de CV necesita red.
 */
class RepositorioContenido(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val modulosCargados = mutableMapOf<String, Modulo>()
    private var indiceCargado: Indice? = null

    suspend fun indice(): Indice = withContext(Dispatchers.IO) {
        indiceCargado ?: json.decodeFromString<Indice>(leer("indice.json")).also { indiceCargado = it }
    }

    suspend fun modulo(meta: ModuloMeta): Modulo = withContext(Dispatchers.IO) {
        modulosCargados.getOrPut(meta.id) {
            json.decodeFromString<Modulo>(leer(meta.archivo))
        }
    }

    suspend fun todosLosModulos(): List<Modulo> = indice().modulos.map { modulo(it) }

    private fun leer(archivo: String): String =
        context.assets.open("contenido/$archivo").bufferedReader().use { it.readText() }
}
