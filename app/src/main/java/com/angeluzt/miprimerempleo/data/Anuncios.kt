package com.angeluzt.miprimerempleo.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

private val Context.anunciosStore by preferencesDataStore("anuncios")

/**
 * Política de publicidad de la app, en un solo lugar.
 *
 * Solo anuncios CON RECOMPENSA y solo cuando la persona los pide: nunca
 * intersticiales al tocar un botón. Un anuncio a pantalla completa sobre una
 * acción que el usuario pidió genera clics accidentales, reseñas de una estrella
 * y Google lo penaliza como anuncio disruptivo. Además contradice lo que el
 * paywall promete.
 *
 * La recompensa es desbloquear UN capítulo. Quien no puede pagar avanza viendo
 * anuncios; quien paga no ve ninguno, nunca.
 */
object PoliticaAnuncios {
    /** Pocos al día a propósito: si cansa, la persona desinstala. */
    const val MAXIMO_POR_DIA = 3

    /** Un anuncio desbloquea un capítulo, no el módulo entero. */
    const val CAPITULOS_POR_ANUNCIO = 1

    /**
     * Dónde se puede ofrecer. Fuera de estos lugares no se muestra nada:
     * jamás en medio de una lectura ni sobre una acción que la persona ya pidió.
     */
    val lugaresPermitidos = setOf("capitulo_bloqueado", "paywall")
}

data class EstadoAnuncios(
    val vistosHoy: Int = 0,
    val capitulosDesbloqueados: Set<String> = emptySet(),
) {
    val restantesHoy: Int get() = (PoliticaAnuncios.MAXIMO_POR_DIA - vistosHoy).coerceAtLeast(0)
    val puedeVerOtro: Boolean get() = restantesHoy > 0
}

class Anuncios(private val context: Context) {

    private object Llaves {
        val vistos = intPreferencesKey("vistos_hoy")
        val dia = longPreferencesKey("dia")
        val desbloqueados = androidx.datastore.preferences.core
            .stringSetPreferencesKey("capitulos_por_anuncio")
    }

    val estado: Flow<EstadoAnuncios> = context.anunciosStore.data.map { p ->
        EstadoAnuncios(
            vistosHoy = if (p[Llaves.dia] == diaDeHoy()) p[Llaves.vistos] ?: 0 else 0,
            capitulosDesbloqueados = p[Llaves.desbloqueados] ?: emptySet(),
        )
    }

    /** Se llama al terminar de ver el anuncio, nunca antes. */
    suspend fun registrarAnuncioVisto(capituloId: String) {
        context.anunciosStore.edit { p ->
            val hoy = diaDeHoy()
            val vistos = if (p[Llaves.dia] == hoy) p[Llaves.vistos] ?: 0 else 0
            p[Llaves.dia] = hoy
            p[Llaves.vistos] = vistos + 1
            p[Llaves.desbloqueados] = (p[Llaves.desbloqueados] ?: emptySet()) + capituloId
        }
    }

    private fun diaDeHoy(): Long {
        val c = Calendar.getInstance()
        return c.get(Calendar.YEAR) * 1000L + c.get(Calendar.DAY_OF_YEAR)
    }
}
