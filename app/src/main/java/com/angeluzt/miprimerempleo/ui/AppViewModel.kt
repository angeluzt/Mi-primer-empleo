package com.angeluzt.miprimerempleo.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.angeluzt.miprimerempleo.MiPrimerEmpleoApp
import com.angeluzt.miprimerempleo.billing.EstadoCompras
import com.angeluzt.miprimerempleo.data.EstadoAnuncios
import com.angeluzt.miprimerempleo.data.EstadoProgreso
import com.angeluzt.miprimerempleo.data.PoliticaAnuncios
import com.angeluzt.miprimerempleo.model.Capitulo
import com.angeluzt.miprimerempleo.model.Indice
import com.angeluzt.miprimerempleo.model.Modulo
import com.angeluzt.miprimerempleo.model.ModuloMeta
import com.angeluzt.miprimerempleo.model.Nivel
import com.angeluzt.miprimerempleo.model.Ruta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EstadoApp(
    val contenidoListo: Boolean = false,
    val progresoListo: Boolean = false,
    val indice: Indice? = null,
    val modulos: Map<String, Modulo> = emptyMap(),
    val progreso: EstadoProgreso = EstadoProgreso(),
    val compras: EstadoCompras = EstadoCompras(),
    val anuncios: EstadoAnuncios = EstadoAnuncios(),
    val avisoAnuncio: String? = null,
) {
    /**
     * NavHost fija su destino inicial en la primera composición. Hasta no saber si la
     * persona ya eligió ruta, mostrar cualquier pantalla la mandaría al lugar equivocado.
     */
    val listo: Boolean get() = contenidoListo && progresoListo

    val ruta: Ruta?
        get() = indice?.rutas?.firstOrNull { it.id == progreso.ruta }

    /** Los módulos en el orden que le toca a la etapa que eligió la persona. */
    val modulosEnOrden: List<ModuloMeta>
        get() {
            val todos = indice?.modulos ?: return emptyList()
            val orden = ruta?.orden ?: return todos
            return todos.sortedBy { orden.indexOf(it.id).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }
        }

    val nivel: Nivel?
        get() = indice?.niveles?.lastOrNull { progreso.puntos >= it.puntosMinimos }

    val siguienteNivel: Nivel?
        get() = indice?.niveles?.firstOrNull { it.puntosMinimos > progreso.puntos }

    /**
     * Leer no exige el pase completo: el de lectura, más barato, alcanza. Y quien no
     * puede pagar nada abre capítulos sueltos viendo un anuncio.
     */
    fun capituloDesbloqueado(modulo: ModuloMeta, capitulo: Capitulo): Boolean =
        compras.puedeLeerTodo || modulo.gratis || capitulo.gratis ||
            capitulo.id in anuncios.capitulosDesbloqueados

    fun capitulosDe(moduloId: String): List<Capitulo> = modulos[moduloId]?.capitulos.orEmpty()

    fun avanceDe(moduloId: String): Float {
        val capitulos = capitulosDe(moduloId)
        if (capitulos.isEmpty()) return 0f
        val leidos = capitulos.count { it.id in progreso.capitulosLeidos }
        return leidos.toFloat() / capitulos.size
    }
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val contexto = app as MiPrimerEmpleoApp
    private val _estado = MutableStateFlow(EstadoApp())
    val estado: StateFlow<EstadoApp> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val indice = contexto.contenido.indice()
            val modulos = contexto.contenido.todosLosModulos().associateBy { it.id }
            _estado.update { it.copy(contenidoListo = true, indice = indice, modulos = modulos) }
        }
        viewModelScope.launch {
            combine(
                contexto.progreso.estado,
                contexto.compras.estado,
                contexto.anuncios.estado,
            ) { progreso, compras, anuncios ->
                Triple(progreso, compras, anuncios)
            }.collect { (progreso, compras, anuncios) ->
                _estado.update {
                    it.copy(
                        progreso = progreso,
                        compras = compras,
                        anuncios = anuncios,
                        progresoListo = true,
                    )
                }
            }
        }
    }

    /**
     * Se llama al abrir un capítulo cerrado. Pedir el anuncio aquí y no al arrancar
     * evita gastarle datos a quien ya pagó y nunca va a ver uno.
     */
    fun prepararAnuncio() {
        if (_estado.value.compras.puedeLeerTodo) return
        contexto.gestorAnuncios.precargar()
    }

    /**
     * Un anuncio visto completo abre un capítulo. No hay intersticiales ni nada que
     * aparezca solo: esto pasa únicamente cuando la persona toca el botón.
     */
    fun verAnuncio(actividad: Activity, capituloId: String) {
        if (!_estado.value.anuncios.puedeVerOtro) {
            _estado.update {
                it.copy(
                    avisoAnuncio = "Ya viste los ${PoliticaAnuncios.MAXIMO_POR_DIA} anuncios de hoy. " +
                        "Mañana puedes abrir más capítulos así.",
                )
            }
            return
        }
        contexto.gestorAnuncios.mostrar(
            actividad = actividad,
            onRecompensa = {
                viewModelScope.launch { contexto.anuncios.registrarAnuncioVisto(capituloId) }
            },
            onSinAnuncio = { motivo -> _estado.update { it.copy(avisoAnuncio = motivo) } },
        )
    }

    fun descartarAvisoAnuncio() = _estado.update { it.copy(avisoAnuncio = null) }

    fun elegirRuta(rutaId: String) = viewModelScope.launch {
        contexto.progreso.elegirRuta(rutaId)
    }

    fun marcarLeido(capituloId: String) = viewModelScope.launch {
        contexto.progreso.marcarCapituloLeido(capituloId)
    }

    fun alternarAccion(accionId: String, puntos: Int) = viewModelScope.launch {
        contexto.progreso.alternarAccion(accionId, puntos)
    }

    fun guardarLlaveOpenAi(llave: String) = viewModelScope.launch {
        contexto.progreso.guardarLlaveOpenAi(llave)
    }

    fun elegirPais(codigo: String) = viewModelScope.launch {
        contexto.progreso.elegirPais(codigo)
    }

    fun elegirPlantilla(plantillaId: String) = viewModelScope.launch {
        contexto.progreso.elegirPlantilla(plantillaId)
    }

    fun elegirFotoCv(ruta: String) = viewModelScope.launch {
        contexto.progreso.guardarFotoCv(ruta)
    }

    fun comprar(activity: Activity, productoId: String) =
        contexto.compras.comprar(activity, productoId)

    fun restaurarCompras() = viewModelScope.launch {
        contexto.compras.restaurarCompras()
    }
}
