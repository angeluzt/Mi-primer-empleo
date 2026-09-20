package com.angeluzt.miprimerempleo.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.angeluzt.miprimerempleo.MiPrimerEmpleoApp
import com.angeluzt.miprimerempleo.billing.EstadoCompras
import com.angeluzt.miprimerempleo.data.EstadoProgreso
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
    val cargando: Boolean = true,
    val indice: Indice? = null,
    val modulos: Map<String, Modulo> = emptyMap(),
    val progreso: EstadoProgreso = EstadoProgreso(),
    val compras: EstadoCompras = EstadoCompras(),
) {
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

    fun capituloDesbloqueado(modulo: ModuloMeta, capitulo: Capitulo): Boolean =
        compras.tienePase || modulo.gratis || capitulo.gratis

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
            _estado.update { it.copy(cargando = false, indice = indice, modulos = modulos) }
        }
        viewModelScope.launch {
            combine(contexto.progreso.estado, contexto.compras.estado) { progreso, compras ->
                progreso to compras
            }.collect { (progreso, compras) ->
                _estado.update { it.copy(progreso = progreso, compras = compras) }
            }
        }
    }

    fun elegirRuta(rutaId: String) = viewModelScope.launch {
        contexto.progreso.elegirRuta(rutaId)
    }

    fun marcarLeido(capituloId: String) = viewModelScope.launch {
        contexto.progreso.marcarCapituloLeido(capituloId)
    }

    fun alternarAccion(accionId: String, puntos: Int) = viewModelScope.launch {
        contexto.progreso.alternarAccion(accionId, puntos)
    }

    fun comprar(activity: Activity, productoId: String) =
        contexto.compras.comprar(activity, productoId)

    fun restaurarCompras() = viewModelScope.launch {
        contexto.compras.restaurarCompras()
    }
}
