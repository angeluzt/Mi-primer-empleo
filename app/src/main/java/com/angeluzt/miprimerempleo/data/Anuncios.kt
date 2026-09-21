package com.angeluzt.miprimerempleo.data

import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.angeluzt.miprimerempleo.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
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
 * La recompensa es desbloquear UN capítulo, no el módulo entero. Quien no puede pagar
 * avanza viendo anuncios; quien paga no ve ninguno, nunca.
 *
 * El único lugar donde se ofrece es el corte de un capítulo cerrado. Nunca en medio
 * de una lectura ni encima de una acción que la persona ya pidió.
 */
object PoliticaAnuncios {
    /** Pocos al día a propósito: si cansa, la persona desinstala. */
    const val MAXIMO_POR_DIA = 3
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

/**
 * Carga y muestra el anuncio con recompensa.
 *
 * Se pide a la red solo cuando alguien se topa con un capítulo cerrado, nunca al
 * arrancar: quien ya pagó no debería gastar sus datos en anuncios que no va a ver.
 *
 * Todo lo de AdMob vive aquí. Si mañana cambiamos de red publicitaria o la quitamos,
 * se cambia esta clase y nada más.
 */
class GestorAnuncios(private val context: Context) {

    private var anuncio: RewardedAd? = null
    private var cargando = false

    /** Se llama al entrar a un capítulo cerrado, para que el anuncio esté listo al tocar. */
    fun precargar() {
        if (anuncio != null || cargando) return
        cargando = true
        RewardedAd.load(
            context,
            BuildConfig.ADMOB_RECOMPENSADO,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(cargado: RewardedAd) {
                    anuncio = cargado
                    cargando = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    anuncio = null
                    cargando = false
                }
            },
        )
    }

    /**
     * `onRecompensa` solo se llama si la persona vio el anuncio completo. Si se sale
     * antes, o no había anuncio, se avisa y no se desbloquea nada: cobrar la recompensa
     * sin que el anunciante haya sido visto es justo lo que hace que te cierren la cuenta.
     */
    fun mostrar(actividad: Activity, onRecompensa: () -> Unit, onSinAnuncio: (String) -> Unit) {
        val listo = anuncio
        if (listo == null) {
            precargar()
            onSinAnuncio("No hay anuncios disponibles ahorita. Inténtalo en un momento.")
            return
        }
        anuncio = null
        var premiado = false

        listo.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                precargar()
                if (!premiado) onSinAnuncio("Se cerró antes de terminar, así que no se desbloqueó.")
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                precargar()
                onSinAnuncio("No se pudo mostrar el anuncio. Inténtalo otra vez.")
            }
        }
        listo.show(actividad) {
            premiado = true
            onRecompensa()
        }
    }
}
