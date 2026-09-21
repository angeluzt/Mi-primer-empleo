package com.angeluzt.miprimerempleo.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.angeluzt.miprimerempleo.cv.ParCv
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("progreso")

data class EstadoProgreso(
    val ruta: String? = null,
    val capitulosLeidos: Set<String> = emptySet(),
    val accionesHechas: Set<String> = emptySet(),
    val puntos: Int = 0,
    val onboardingHecho: Boolean = false,
    val cvsGenerados: Int = 0,
    val plantillaCv: String = "",
    val pais: String = "MX",
    val llaveOpenAi: String = "",
    val cvGenerado: String = "",
    val respuestasCv: String = "",
    val fotoCv: String = "",
)

class Progreso(private val context: Context) {

    private object Llaves {
        val ruta = stringPreferencesKey("ruta")
        val capitulos = stringSetPreferencesKey("capitulos_leidos")
        val acciones = stringSetPreferencesKey("acciones_hechas")
        val puntos = intPreferencesKey("puntos")
        val onboarding = booleanPreferencesKey("onboarding_hecho")
        val cvs = intPreferencesKey("cvs_generados")
        val plantilla = stringPreferencesKey("plantilla_cv")
        val pais = stringPreferencesKey("pais")
        val llave = stringPreferencesKey("llave_openai")
        val cvJson = stringPreferencesKey("cv_generado")
        val respuestasCv = stringPreferencesKey("respuestas_cv")
        val fotoCv = stringPreferencesKey("foto_cv")
    }

    val estado: Flow<EstadoProgreso> = context.dataStore.data.map { p ->
        EstadoProgreso(
            ruta = p[Llaves.ruta],
            capitulosLeidos = p[Llaves.capitulos] ?: emptySet(),
            accionesHechas = p[Llaves.acciones] ?: emptySet(),
            puntos = p[Llaves.puntos] ?: 0,
            onboardingHecho = p[Llaves.onboarding] ?: false,
            cvsGenerados = p[Llaves.cvs] ?: 0,
            plantillaCv = p[Llaves.plantilla].orEmpty(),
            pais = p[Llaves.pais] ?: "MX",
            llaveOpenAi = p[Llaves.llave].orEmpty(),
            cvGenerado = p[Llaves.cvJson].orEmpty(),
            respuestasCv = p[Llaves.respuestasCv].orEmpty(),
            fotoCv = p[Llaves.fotoCv].orEmpty(),
        )
    }

    suspend fun elegirRuta(rutaId: String) {
        context.dataStore.edit {
            it[Llaves.ruta] = rutaId
            it[Llaves.onboarding] = true
        }
    }

    suspend fun marcarCapituloLeido(capituloId: String) {
        context.dataStore.edit { p ->
            val actuales = p[Llaves.capitulos] ?: emptySet()
            if (capituloId !in actuales) {
                p[Llaves.capitulos] = actuales + capituloId
                p[Llaves.puntos] = (p[Llaves.puntos] ?: 0) + PUNTOS_POR_CAPITULO
            }
        }
    }

    /**
     * Las acciones son del mundo real (mandar CV, conseguir una carta), no de la app.
     * Por eso se pueden desmarcar: el progreso debe poder decir la verdad.
     */
    suspend fun alternarAccion(accionId: String, puntos: Int) {
        context.dataStore.edit { p ->
            val actuales = p[Llaves.acciones] ?: emptySet()
            val total = p[Llaves.puntos] ?: 0
            if (accionId in actuales) {
                p[Llaves.acciones] = actuales - accionId
                p[Llaves.puntos] = (total - puntos).coerceAtLeast(0)
            } else {
                p[Llaves.acciones] = actuales + accionId
                p[Llaves.puntos] = total + puntos
            }
        }
    }

    /** Solo la usa la pantalla de Ajustes, y solo en compilaciones de depuración. */
    suspend fun guardarLlaveOpenAi(llave: String) {
        context.dataStore.edit { it[Llaves.llave] = llave.trim() }
    }

    suspend fun elegirPais(codigo: String) {
        context.dataStore.edit { it[Llaves.pais] = codigo }
    }

    suspend fun elegirPlantilla(plantillaId: String) {
        context.dataStore.edit { it[Llaves.plantilla] = plantillaId }
    }

    /** El CV se guarda para que la vista previa de formatos use el real, no el de muestra. */
    suspend fun guardarCv(par: ParCv) {
        context.dataStore.edit { it[Llaves.cvJson] = Json.encodeToString(par) }
    }

    /**
     * La entrevista se guarda respuesta por respuesta.
     *
     * Son diez minutos de escribir en el teléfono: si la persona toca «atrás», le entra
     * una llamada o Android mata la app, perder todo sería motivo suficiente para no
     * volver a abrirla.
     */
    suspend fun guardarRespuestasCv(respuestas: Map<String, String>) {
        context.dataStore.edit { it[Llaves.respuestasCv] = Json.encodeToString(respuestas) }
    }

    fun leerRespuestasCv(crudo: String): Map<String, String> =
        if (crudo.isBlank()) emptyMap()
        else runCatching { Json.decodeFromString<Map<String, String>>(crudo) }.getOrDefault(emptyMap())

    /** Ruta del archivo propio, no el URI del selector: ese permiso se pierde al reiniciar. */
    suspend fun guardarFotoCv(ruta: String) {
        context.dataStore.edit { it[Llaves.fotoCv] = ruta }
    }

    suspend fun registrarCvGenerado() {
        context.dataStore.edit { it[Llaves.cvs] = (it[Llaves.cvs] ?: 0) + 1 }
    }

    companion object {
        /**
         * Leer vale poco a propósito. El nivel de empleabilidad tiene que reflejar
         * lo que la persona hizo en el mundo real, no cuánto avanzó en la app:
         * leer la guía completa no te hace contratable.
         */
        const val PUNTOS_POR_CAPITULO = 2
    }
}
