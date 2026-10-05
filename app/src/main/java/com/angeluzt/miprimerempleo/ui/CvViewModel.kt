package com.angeluzt.miprimerempleo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.angeluzt.miprimerempleo.BuildConfig
import com.angeluzt.miprimerempleo.MiPrimerEmpleoApp
import com.angeluzt.miprimerempleo.cv.Campo
import com.angeluzt.miprimerempleo.cv.Cv
import com.angeluzt.miprimerempleo.cv.DatoSensible
import com.angeluzt.miprimerempleo.cv.DiagnosticoLocal
import com.angeluzt.miprimerempleo.cv.ExportadorCv
import com.angeluzt.miprimerempleo.cv.GuionEntrevista
import com.angeluzt.miprimerempleo.cv.ParCv
import com.angeluzt.miprimerempleo.cv.ProteccionDatos
import com.angeluzt.miprimerempleo.cv.RevisionCv
import com.angeluzt.miprimerempleo.cv.Sospecha
import com.angeluzt.miprimerempleo.cv.VerificadorCv
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

data class TurnoCv(val esUsuario: Boolean, val texto: String)

/** Qué hacer con el PDF una vez dibujado. */
enum class AccionArchivo { GUARDAR, COMPARTIR }

data class ArchivoCv(
    val archivo: File,
    val nombre: String,
    val accion: AccionArchivo,
)

data class EstadoCv(
    val turnos: List<TurnoCv> = emptyList(),
    val respuestas: Map<String, String> = emptyMap(),
    val campo: Campo? = GuionEntrevista.campos.first(),
    val generando: Boolean = false,
    val cv: ParCv? = null,
    val error: String? = null,
    val revision: RevisionCv? = null,
    val revisando: Boolean = false,
    /** La persona volvió a una pregunta ya contestada para corregirla. */
    val editando: Boolean = false,
    /** Se le hizo una pregunta de seguimiento sobre el mismo campo (ver GuionEntrevista.repregunta). */
    val aclarando: Boolean = false,
    /** Cambió algo después de generar: el CV en pantalla ya no refleja sus respuestas. */
    val cambiosSinGenerar: Boolean = false,
    val exportando: Boolean = false,
    val archivo: ArchivoCv? = null,
    val aviso: String? = null,
    /** Identificadores que se quitaron de las respuestas antes de mandarlas a la IA. */
    val tachados: List<DatoSensible> = emptyList(),
    /** Cifras o habilidades del CV que no aparecen en lo que la persona dijo. */
    val sospechas: List<Sospecha> = emptyList(),
    /** Datos personales que están en el CV y no deberían (los pudo escribir en el editor). */
    val sensibles: List<DatoSensible> = emptyList(),
) {
    val puedeGenerar: Boolean get() = GuionEntrevista.sePuedeGenerar(respuestas)
    val posicion: Int get() = campo?.let { GuionEntrevista.posicionDe(it.id) } ?: GuionEntrevista.campos.size
    val total: Int get() = GuionEntrevista.campos.size
    val terminoElGuion: Boolean get() = campo == null
}

class CvViewModel(app: Application) : AndroidViewModel(app) {

    private val contexto = app as MiPrimerEmpleoApp
    private val _estado = MutableStateFlow(estadoInicial())
    val estado: StateFlow<EstadoCv> = _estado.asStateFlow()

    private val lector = Json { ignoreUnknownKeys = true }

    init {
        viewModelScope.launch {
            recuperarLoGuardado()
            // El CV guardado es la fuente de verdad: si el editor lo cambia, aquí se ve al volver.
            contexto.progreso.estado
                .map { it.cvGenerado to it.respuestasCv }
                .distinctUntilChanged()
                .drop(1)
                .collect { (crudo, respuestas) ->
                    if (crudo.isBlank() && respuestas.isBlank()) {
                        // Borró sus datos desde Ajustes: que no queden en memoria.
                        _estado.value = estadoInicial()
                    } else {
                        decodificar(crudo)?.let { alCambiarCv(it) }
                    }
                }
        }
    }

    private fun decodificar(crudo: String): ParCv? =
        crudo.takeIf { it.isNotBlank() }?.let { runCatching { lector.decodeFromString<ParCv>(it) }.getOrNull() }

    /** Revisión local de lo que no depende de la IA: inventos y datos personales. */
    private fun revisarLocal(par: ParCv, respuestas: Map<String, String>) =
        VerificadorCv.revisar(par.es, respuestas) to ProteccionDatos.revisarCv(par.es)

    private fun alCambiarCv(par: ParCv) {
        if (par == _estado.value.cv) return
        _estado.update {
            val (sospechas, sensibles) = revisarLocal(par, it.respuestas)
            it.copy(cv = par, sospechas = sospechas, sensibles = sensibles, revision = DiagnosticoLocal.revision(par.es))
        }
    }

    private fun estadoInicial(): EstadoCv {
        val primero = GuionEntrevista.campos.first()
        return EstadoCv(
            turnos = listOf(TurnoCv(false, bienvenida()), TurnoCv(false, textoDe(primero))),
            campo = primero,
        )
    }

    private fun bienvenida() =
        "Vamos a armar tu CV. Son ${GuionEntrevista.campos.size} preguntas cortas, unos 5 minutos, " +
            "y puedes saltarte las que no apliquen.\n\n" +
            "No invento nada: solo acomodo y redacto lo que tú me digas."

    /**
     * Devuelve la entrevista donde se quedó. Escribir todo esto en un teléfono toma
     * diez minutos; perderlo por tocar «atrás» es la clase de cosa por la que la gente
     * desinstala una app y no vuelve.
     */
    private suspend fun recuperarLoGuardado() {
        val guardado = contexto.progreso.estado.first()
        val respuestas = contexto.progreso.leerRespuestasCv(guardado.respuestasCv)
        val cv = decodificar(guardado.cvGenerado)
        if (respuestas.isEmpty() && cv == null) return
        if (_estado.value.respuestas.isNotEmpty()) return

        val pendiente = GuionEntrevista.campos.firstOrNull { it.id !in respuestas.keys }
        _estado.update {
            it.copy(
                respuestas = respuestas,
                campo = pendiente,
                cv = cv,
                // Al volver a la pantalla se muestra la revisión local, que es gratis.
                // Llamar a la IA en cada entrada gastaría tokens sin que nadie lo pida.
                revision = cv?.let { par -> DiagnosticoLocal.revision(par.es) },
                sospechas = cv?.let { par -> VerificadorCv.revisar(par.es, respuestas) }.orEmpty(),
                sensibles = cv?.let { par -> ProteccionDatos.revisarCv(par.es) }.orEmpty(),
                turnos = reconstruirTurnos(respuestas, pendiente, cv != null),
            )
        }
    }

    /** Rearma la conversación desde las respuestas guardadas, para no aparecer en blanco. */
    private fun reconstruirTurnos(
        respuestas: Map<String, String>,
        pendiente: Campo?,
        yaHayCv: Boolean,
    ): List<TurnoCv> = buildList {
        add(TurnoCv(false, bienvenida()))
        GuionEntrevista.campos.forEach { campo ->
            val respuesta = respuestas[campo.id] ?: return@forEach
            add(TurnoCv(false, textoDe(campo)))
            add(TurnoCv(true, respuesta.ifBlank { "Saltar" }))
        }
        when {
            pendiente != null -> add(TurnoCv(false, textoDe(pendiente)))
            yaHayCv -> add(TurnoCv(false, "Aquí está tu CV. Puedes revisarlo, corregir algo o exportarlo."))
            else -> add(TurnoCv(false, "Eso es todo. Dale a «Generar mi CV» y te lo armo en español e inglés."))
        }
    }

    private fun textoDe(campo: Campo): String =
        listOfNotNull(campo.pregunta, campo.ayuda.ifBlank { null }).joinToString("\n\n")

    /** Guarda la respuesta y avanza. Sin llamadas de red: es instantáneo. */
    fun responder(texto: String) {
        val limpio = texto.trim()
        val actual = _estado.value.campo ?: return
        if (limpio.isEmpty()) return
        avanzar(actual, limpio, TurnoCv(true, limpio))
    }

    fun saltar() {
        val actual = _estado.value.campo ?: return
        avanzar(actual, "", TurnoCv(true, "Saltar"))
    }

    private fun avanzar(actual: Campo, respuesta: String, turnoUsuario: TurnoCv) {
        val antes = _estado.value
        // La respuesta a una pregunta de seguimiento se suma a lo que ya dijo; saltarla no borra nada.
        val previa = if (antes.aclarando) antes.respuestas[actual.id].orEmpty().trim().trimEnd('.') else ""
        val completa = listOf(previa, respuesta.trim()).filter { it.isNotBlank() }.joinToString(". ")

        val repregunta = if (antes.aclarando) null else GuionEntrevista.repregunta(actual.id, completa)
        if (repregunta != null) {
            _estado.update {
                it.copy(
                    respuestas = it.respuestas + (actual.id to completa),
                    aclarando = true,
                    error = null,
                    turnos = it.turnos + turnoUsuario + TurnoCv(false, repregunta),
                )
            }
            guardarRespuestas()
            return
        }

        val editando = antes.editando
        // Al corregir se vuelve al final, no se repite el resto del guion.
        val siguiente = if (editando) null else GuionEntrevista.siguienteDespuesDe(actual.id)
        val reaccion = GuionEntrevista.reaccion(actual.id, completa)

        _estado.update { estado ->
            // Los saltos se guardan como respuesta vacía: así no se vuelven a preguntar
            // cuando la persona regresa a la app.
            val respuestas = estado.respuestas + (actual.id to completa)
            val cierre = when {
                siguiente != null -> listOfNotNull(reaccion, textoDe(siguiente)).joinToString("\n\n")
                estado.cv != null -> "Anotado. Dale a «Generar de nuevo» para que tu CV lo incluya."
                else -> "Eso es todo. Dale a «Generar mi CV» y te lo armo en español e inglés."
            }
            estado.copy(
                respuestas = respuestas,
                campo = siguiente,
                editando = false,
                aclarando = false,
                cambiosSinGenerar = estado.cambiosSinGenerar || estado.cv != null,
                error = null,
                turnos = estado.turnos + turnoUsuario + TurnoCv(false, cierre),
            )
        }
        guardarRespuestas()
    }

    /** Vuelve a preguntar un campo ya contestado, por si la persona quiere corregirlo. */
    fun volverA(campoId: String) {
        val campo = GuionEntrevista.campos.firstOrNull { it.id == campoId } ?: return
        _estado.update {
            it.copy(
                campo = campo,
                editando = true,
                aclarando = false,
                turnos = it.turnos + TurnoCv(false, textoDe(campo)),
            )
        }
    }

    private fun guardarRespuestas() = viewModelScope.launch {
        contexto.progreso.guardarRespuestasCv(_estado.value.respuestas)
    }

    fun generar() {
        val estado = _estado.value
        if (estado.generando || !estado.puedeGenerar) return
        _estado.update { it.copy(generando = true, error = null) }

        viewModelScope.launch {
            val llave = llaveLocal()
            faltaConfiguracion(llave)?.let { aviso ->
                _estado.update { it.copy(generando = false, error = aviso) }
                return@launch
            }

            contexto.clienteCv
                .generar(
                    purchaseToken = contexto.compras.tokenDelPase().orEmpty(),
                    respuestas = estado.respuestas,
                    pais = contexto.progreso.estado.first().pais,
                    llaveLocal = llave,
                )
                .onSuccess { generacion ->
                    val par = generacion.par
                    contexto.progreso.registrarCvGenerado()
                    contexto.progreso.guardarCv(par)
                    val (sospechas, sensibles) = revisarLocal(par, estado.respuestas)
                    _estado.update {
                        it.copy(
                            generando = false,
                            cv = par,
                            revision = null,
                            tachados = generacion.tachados,
                            sospechas = sospechas,
                            sensibles = sensibles,
                            cambiosSinGenerar = false,
                            turnos = it.turnos + TurnoCv(
                                false,
                                "Listo. Lo estoy revisando como lo haría un reclutador; " +
                                    "abajo te digo qué le falta.",
                            ),
                        )
                    }
                    evaluar()
                }
                .onFailure { fallo ->
                    _estado.update {
                        it.copy(generando = false, error = fallo.message ?: "No pudimos generar tu CV.")
                    }
                }
        }
    }

    /**
     * Revisa el CV ya armado. Primero con lo que se puede calcular aquí mismo (gratis y
     * al instante) y luego con la IA, que afina el juicio. Si la IA falla, se queda la
     * revisión local: menos fina, pero cierta.
     */
    fun evaluar() {
        val cv = _estado.value.cv?.es ?: return
        if (_estado.value.revisando) return
        _estado.update {
            it.copy(revisando = true, revision = it.revision ?: DiagnosticoLocal.revision(cv))
        }

        viewModelScope.launch {
            val llave = llaveLocal()
            contexto.clienteCv
                .evaluar(
                    purchaseToken = contexto.compras.tokenDelPase().orEmpty(),
                    cv = cv,
                    llaveLocal = llave,
                )
                .onSuccess { revision ->
                    _estado.update {
                        it.copy(revisando = false, revision = conDatosReales(revision, cv))
                    }
                }
                .onFailure {
                    _estado.update {
                        it.copy(revisando = false, revision = DiagnosticoLocal.revision(cv))
                    }
                }
        }
    }

    /**
     * El conteo de palabras y el veredicto de extensión los calculamos nosotros.
     * Un modelo de lenguaje no cuenta bien, y aquí no hay por qué adivinar.
     */
    private fun conDatosReales(revision: RevisionCv, cv: Cv) = revision.copy(
        palabras = DiagnosticoLocal.palabras(cv),
        extension = DiagnosticoLocal.extension(cv),
    )

    // ---------- Exportar ----------

    fun exportar(idioma: String, accion: AccionArchivo) {
        val par = _estado.value.cv ?: return
        if (_estado.value.exportando) return
        _estado.update { it.copy(exportando = true, error = null) }

        viewModelScope.launch {
            val guardado = contexto.progreso.estado.first()
            val cv = if (idioma == "en") par.en else par.es

            runCatching {
                ExportadorCv.pdf(contexto, cv, guardado.plantillaCv, guardado.fotoCv, idioma.uppercase())
            }
                .onSuccess { (archivo, nombre) ->
                    _estado.update {
                        it.copy(exportando = false, archivo = ArchivoCv(archivo, nombre, accion))
                    }
                }
                .onFailure {
                    _estado.update {
                        it.copy(exportando = false, error = "No pudimos armar el PDF. Inténtalo otra vez.")
                    }
                }
        }
    }

    /** El archivo ya se entregó al sistema: se limpia para no volver a lanzarlo solo. */
    fun archivoEntregado(aviso: String? = null) =
        _estado.update { it.copy(archivo = null, aviso = aviso) }

    fun descartarAviso() = _estado.update { it.copy(aviso = null) }

    fun descartarError() = _estado.update { it.copy(error = null) }

    private suspend fun llaveLocal(): String =
        if (BuildConfig.LLAVE_LOCAL_PERMITIDA) contexto.progreso.estado.first().llaveOpenAi else ""

    /**
     * Sin llave pegada y sin backend desplegado no hay a quién pedirle la redacción.
     * Decirlo de frente evita dejar a la persona esperando sin entender.
     */
    private fun faltaConfiguracion(llave: String): String? {
        if (llave.isNotBlank()) return null
        if (!BuildConfig.BACKEND_URL.contains("TU-PROYECTO")) return null
        return if (BuildConfig.LLAVE_LOCAL_PERMITIDA) {
            "Falta configurar la IA. Entra a Ajustes y pega tu llave de OpenAI."
        } else {
            "El servicio no está disponible en este momento."
        }
    }
}
