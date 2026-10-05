package com.angeluzt.miprimerempleo.capturas

import android.content.Context
import com.angeluzt.miprimerempleo.billing.EstadoCompras
import com.angeluzt.miprimerempleo.data.EstadoAnuncios
import com.angeluzt.miprimerempleo.data.EstadoProgreso
import com.angeluzt.miprimerempleo.data.RepositorioContenido
import com.angeluzt.miprimerempleo.ui.EstadoApp
import kotlinx.coroutines.runBlocking

/**
 * Estados de la app armados con el contenido real del APK, para dibujar cada pantalla
 * como la vería una persona en un momento concreto de su recorrido.
 */
object Escenarios {

    /** Alguien que lleva unos días: leyó algo, hizo una acción, no ha pagado. */
    fun enCamino(contexto: Context, pase: Boolean = false, lectura: Boolean = false): EstadoApp =
        base(contexto).let { estado ->
            estado.copy(
                progreso = estado.progreso.copy(
                    ruta = "egresado",
                    onboardingHecho = true,
                    puntos = 46,
                    capitulosLeidos = setOf("ev_por_que", "ev_proyecto", "bu_canales", "bu_referidos"),
                    accionesHechas = setOf("proyecto_elegido"),
                ),
                compras = EstadoCompras(conectado = true, tienePase = pase, tieneLectura = lectura),
            )
        }

    /** La primera vez que se abre la app: sin ruta elegida. */
    fun primeraVez(contexto: Context): EstadoApp = base(contexto)

    private fun base(contexto: Context): EstadoApp = runBlocking {
        val repositorio = RepositorioContenido(contexto)
        EstadoApp(
            contenidoListo = true,
            progresoListo = true,
            indice = repositorio.indice(),
            modulos = repositorio.todosLosModulos().associateBy { it.id },
            progreso = EstadoProgreso(pais = "MX"),
            compras = EstadoCompras(conectado = true, tienePase = false, tieneLectura = false),
            anuncios = EstadoAnuncios(vistosHoy = 1),
        )
    }
}
