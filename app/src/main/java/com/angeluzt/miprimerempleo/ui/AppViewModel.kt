package com.angeluzt.miprimerempleo.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.angeluzt.miprimerempleo.MiPrimerEmpleoApp
import com.angeluzt.miprimerempleo.billing.EstadoCompras
import com.angeluzt.miprimerempleo.billing.Productos
import com.angeluzt.miprimerempleo.bitacora.AnalisisBitacora
import com.angeluzt.miprimerempleo.bitacora.Entrevista
import com.angeluzt.miprimerempleo.cv.Adaptacion
import com.angeluzt.miprimerempleo.cv.ParCv
import com.angeluzt.miprimerempleo.data.EstadoAnuncios
import com.angeluzt.miprimerempleo.data.EstadoProgreso
import com.angeluzt.miprimerempleo.data.PoliticaAnuncios
import com.angeluzt.miprimerempleo.model.AccionBloque
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
import kotlinx.serialization.json.Json

/** El siguiente capítulo que le toca leer a la persona según su ruta. */
data class Siguiente(val modulo: ModuloMeta, val capitulo: Capitulo, val desbloqueado: Boolean)

data class EstadoApp(
    val contenidoListo: Boolean = false,
    val progresoListo: Boolean = false,
    val indice: Indice? = null,
    val modulos: Map<String, Modulo> = emptyMap(),
    val progreso: EstadoProgreso = EstadoProgreso(),
    val compras: EstadoCompras = EstadoCompras(),
    val anuncios: EstadoAnuncios = EstadoAnuncios(),
    val avisoAnuncio: String? = null,
    /** El CV generado, ya decodificado (o null si aún no lo arma). */
    val cv: ParCv? = null,
    val entrevistas: List<Entrevista> = emptyList(),
    val adaptaciones: List<Adaptacion> = emptyList(),
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

    /** Qué tan cerca está del siguiente nivel, de 0 a 1. */
    val avanceDeNivel: Float
        get() {
            val actual = nivel ?: return 0f
            val siguiente = siguienteNivel ?: return 1f
            val rango = (siguiente.puntosMinimos - actual.puntosMinimos).coerceAtLeast(1)
            return ((progreso.puntos - actual.puntosMinimos).toFloat() / rango).coerceIn(0f, 1f)
        }

    /** Generaciones que le quedan: las del pase más las recargas acreditadas. */
    val creditosCv: Int
        get() {
            if (!compras.tienePase) return 0
            val total = Productos.CVS_INCLUIDOS_EN_PASE +
                progreso.recargasAcreditadas * Productos.CVS_POR_RECARGA
            return (total - progreso.cvsGenerados).coerceAtLeast(0)
        }

    /**
     * El siguiente capítulo sin leer en el orden de su ruta. Prefiere uno que ya pueda abrir:
     * mandarla de frente a un candado no es "seguir leyendo".
     */
    val siguiente: Siguiente?
        get() {
            val pendientes = modulosEnOrden.flatMap { meta ->
                capitulosDe(meta.id)
                    .filter { it.id !in progreso.capitulosLeidos }
                    .map { Siguiente(meta, it, capituloDesbloqueado(meta, it)) }
            }
            return pendientes.firstOrNull { it.desbloqueado } ?: pendientes.firstOrNull()
        }

    val capitulosTotales: Int get() = modulos.values.sumOf { it.capitulos.size }

    /** Temas de la bitácora que aún no prepara. */
    val porEstudiar: Int
        get() = AnalisisBitacora.listaDeEstudio(entrevistas).count { !it.preparada }

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

    /** El capítulo que sigue al terminar uno, dentro del orden de su ruta. */
    fun despuesDe(moduloId: String, capituloId: String): Siguiente? {
        val todos = modulosEnOrden.flatMap { meta ->
            capitulosDe(meta.id).map { Siguiente(meta, it, capituloDesbloqueado(meta, it)) }
        }
        val indice = todos.indexOfFirst { it.modulo.id == moduloId && it.capitulo.id == capituloId }
        return if (indice >= 0) todos.getOrNull(indice + 1) else null
    }

    /** Los puntos que vale una acción del contenido, para completarla desde una herramienta. */
    fun puntosDeAccion(accionId: String): Int? = modulos.values.asSequence()
        .flatMap { it.capitulos.asSequence() }
        .flatMap { it.bloques.asSequence() }
        .filterIsInstance<AccionBloque>()
        .firstOrNull { it.accionId == accionId }
        ?.puntos
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val contexto = app as MiPrimerEmpleoApp
    private val _estado = MutableStateFlow(EstadoApp())
    val estado: StateFlow<EstadoApp> = _estado.asStateFlow()

    private val lector = Json { ignoreUnknownKeys = true }
    private var cvCrudo: String? = null
    private var cvDecodificado: ParCv? = null

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
                contexto.bitacora.entrevistas,
                contexto.adaptaciones.adaptaciones,
            ) { progreso, compras, anuncios, entrevistas, adaptaciones ->
                Registro(progreso, compras, anuncios, entrevistas, adaptaciones)
            }.collect { r ->
                _estado.update {
                    it.copy(
                        progreso = r.progreso,
                        compras = r.compras,
                        anuncios = r.anuncios,
                        entrevistas = r.entrevistas,
                        adaptaciones = r.adaptaciones,
                        cv = decodificarCv(r.progreso.cvGenerado),
                        progresoListo = true,
                    )
                }
            }
        }
    }

    private data class Registro(
        val progreso: EstadoProgreso,
        val compras: EstadoCompras,
        val anuncios: EstadoAnuncios,
        val entrevistas: List<Entrevista>,
        val adaptaciones: List<Adaptacion>,
    )

    /** Solo se decodifica cuando el texto cambia: el progreso cambia mucho más seguido que el CV. */
    private fun decodificarCv(crudo: String): ParCv? {
        if (crudo == cvCrudo) return cvDecodificado
        cvCrudo = crudo
        cvDecodificado = crudo.takeIf { it.isNotBlank() }
            ?.let { runCatching { lector.decodeFromString<ParCv>(it) }.getOrNull() }
        return cvDecodificado
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

    /** Lo guarda el editor: cambiar un texto no cuesta una generación. */
    fun guardarCv(par: ParCv) = viewModelScope.launch {
        contexto.progreso.guardarCv(par)
    }

    // ---------- Bitácora ----------

    fun guardarEntrevista(entrevista: Entrevista) = viewModelScope.launch {
        contexto.bitacora.guardar(entrevista)
        val todas = _estado.value.entrevistas.filterNot { it.id == entrevista.id } + entrevista
        acreditarBitacora(todas)
    }

    fun borrarEntrevista(id: String) = viewModelScope.launch {
        contexto.bitacora.borrar(id)
    }

    fun marcarPreparada(clave: String, preparada: Boolean) = viewModelScope.launch {
        val actualizadas = AnalisisBitacora.marcarPreparada(_estado.value.entrevistas, clave, preparada)
        contexto.bitacora.reemplazar(actualizadas)
        acreditarBitacora(actualizadas)
    }

    /**
     * Las acciones del capítulo de la bitácora se completan solas cuando la persona hace lo
     * que dicen: registrar su primera entrevista, y llevar tres trabajando su lista de estudio.
     */
    private suspend fun acreditarBitacora(entrevistas: List<Entrevista>) {
        val estado = _estado.value
        if (entrevistas.isNotEmpty()) {
            estado.puntosDeAccion(ACCION_PRIMERA)?.let { contexto.progreso.completarAccion(ACCION_PRIMERA, it) }
        }
        val estudio = AnalisisBitacora.listaDeEstudio(entrevistas)
        if (entrevistas.size >= 3 && estudio.any { it.preparada }) {
            estado.puntosDeAccion(ACCION_TRES)?.let { contexto.progreso.completarAccion(ACCION_TRES, it) }
        }
    }

    // ---------- Vacantes ----------

    fun borrarAdaptacion(id: String) = viewModelScope.launch {
        contexto.adaptaciones.borrar(id)
    }

    fun comprar(activity: Activity, productoId: String) =
        contexto.compras.comprar(activity, productoId)

    fun restaurarCompras() = viewModelScope.launch {
        contexto.compras.restaurarCompras()
    }

    private companion object {
        const val ACCION_PRIMERA = "primera_entrevista"
        const val ACCION_TRES = "tres_entrevistas"
    }
}
