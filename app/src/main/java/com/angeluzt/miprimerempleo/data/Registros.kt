package com.angeluzt.miprimerempleo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.angeluzt.miprimerempleo.bitacora.Entrevista
import com.angeluzt.miprimerempleo.cv.Adaptacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.bitacoraStore by preferencesDataStore("bitacora")
private val Context.adaptacionesStore by preferencesDataStore("adaptaciones")

/**
 * Una lista de registros guardada como JSON en su propio DataStore.
 *
 * Son pocos (decenas de entrevistas, unas cuantas vacantes), así que un archivo por lista
 * alcanza y sobra. Nada de esto sale del teléfono.
 */
private class ListaGuardada<T>(
    private val store: DataStore<Preferences>,
    private val serializador: KSerializer<T>,
    private val id: (T) -> String,
    private val maximo: Int,
) {
    private val llave = stringPreferencesKey("registros")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val lista = ListSerializer(serializador)

    val todos: Flow<List<T>> = store.data.map { p ->
        p[llave]?.let { crudo -> runCatching { json.decodeFromString(lista, crudo) }.getOrNull() }.orEmpty()
    }

    suspend fun guardar(registro: T) = cambiar { actuales ->
        (listOf(registro) + actuales.filterNot { id(it) == id(registro) }).take(maximo)
    }

    suspend fun borrar(clave: String) = cambiar { actuales -> actuales.filterNot { id(it) == clave } }

    suspend fun reemplazar(nuevos: List<T>) = cambiar { nuevos }

    private suspend fun cambiar(transformar: (List<T>) -> List<T>) {
        store.edit { p ->
            val actuales = p[llave]?.let { runCatching { json.decodeFromString(lista, it) }.getOrNull() }.orEmpty()
            p[llave] = json.encodeToString(lista, transformar(actuales))
        }
    }
}

class RepositorioBitacora(context: Context) {
    private val registros = ListaGuardada(context.bitacoraStore, Entrevista.serializer(), { it.id }, maximo = 200)

    val entrevistas: Flow<List<Entrevista>> = registros.todos.map { lista -> lista.sortedByDescending { it.fecha } }
    suspend fun guardar(entrevista: Entrevista) = registros.guardar(entrevista)
    suspend fun borrar(id: String) = registros.borrar(id)
    suspend fun reemplazar(entrevistas: List<Entrevista>) = registros.reemplazar(entrevistas)
}

class RepositorioAdaptaciones(context: Context) {
    // Cada adaptación guarda un CV completo; con las últimas 20 basta para no llenar el teléfono.
    private val registros = ListaGuardada(context.adaptacionesStore, Adaptacion.serializer(), { it.id }, maximo = 20)

    val adaptaciones: Flow<List<Adaptacion>> = registros.todos.map { lista -> lista.sortedByDescending { it.creada } }
    suspend fun guardar(adaptacion: Adaptacion) = registros.guardar(adaptacion)
    suspend fun borrar(id: String) = registros.borrar(id)
}
