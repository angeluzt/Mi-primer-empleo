package com.angeluzt.miprimerempleo.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
