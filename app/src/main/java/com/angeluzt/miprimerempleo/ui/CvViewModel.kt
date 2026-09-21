package com.angeluzt.miprimerempleo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.angeluzt.miprimerempleo.BuildConfig
import com.angeluzt.miprimerempleo.MiPrimerEmpleoApp
import com.angeluzt.miprimerempleo.cv.Campo
import com.angeluzt.miprimerempleo.cv.Cv
import com.angeluzt.miprimerempleo.cv.DiagnosticoLocal
import com.angeluzt.miprimerempleo.cv.FotoCv
import com.angeluzt.miprimerempleo.cv.GuionEntrevista
import com.angeluzt.miprimerempleo.cv.ParCv
import com.angeluzt.miprimerempleo.cv.Plantillas
import com.angeluzt.miprimerempleo.cv.RenderizadorCv
import com.angeluzt.miprimerempleo.cv.RevisionCv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    /** Cambió algo después de generar: el CV en pantalla ya no refleja sus respuestas. */
    val cambiosSinGenerar: Boolean = false,
    val exportando: Boolean = false,
    val archivo: ArchivoCv? = null,
    val aviso: String? = null,
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

    init {
        viewModelScope.launch { recuperarLoGuardado() }
    }

    private fun estadoInicial(): EstadoCv {
        val primero = GuionEntrevista.campos.first()
        return EstadoCv(
            turnos = listOf(TurnoCv(false, bienvenida()), TurnoCv(false, textoDe(primero))),
            campo = primero,
        )
    }

    private fun bienvenida() =
        "Vamos a armar tu CV. Son ${GuionEntrevista.campos.size} preguntas cortas " +
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
        val cv = guardado.cvGenerado.takeIf { it.isNotBlank() }?.let { crudo ->
            runCatching { Json { ignoreUnknownKeys = true }.decodeFromString<ParCv>(crudo) }.getOrNull()
        }
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
        val editando = _estado.value.editando
        // Al corregir se vuelve al final, no se repite el resto del guion.
        val siguiente = if (editando) null else GuionEntrevista.siguienteDespuesDe(actual.id)

        _estado.update { estado ->
            // Los saltos se guardan como respuesta vacía: así no se vuelven a preguntar
            // cuando la persona regresa a la app.
            val respuestas = estado.respuestas + (actual.id to respuesta)
            val cierre = when {
                siguiente != null -> textoDe(siguiente)
                estado.cv != null -> "Anotado. Dale a «Generar de nuevo» para que tu CV lo incluya."
                else -> "Eso es todo. Dale a «Generar mi CV» y te lo armo en español e inglés."
            }
            estado.copy(
                respuestas = respuestas,
                campo = siguiente,
                editando = false,
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
                    llaveLocal = llave,
                )
                .onSuccess { par ->
                    contexto.progreso.registrarCvGenerado()
                    contexto.progreso.guardarCv(par)
                    _estado.update {
                        it.copy(
                            generando = false,
                            cv = par,
                            revision = null,
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
            val plantilla = Plantillas.porId(guardado.plantillaCv)
            val cv = if (idioma == "en") par.en else par.es
            val nombre = nombreDeArchivo(cv.datos.nombre, idioma)

            runCatching {
                withContext(Dispatchers.IO) {
                    RenderizadorCv(contexto)
                        .exportar(cv, plantilla, FotoCv.cargar(guardado.fotoCv), nombre)
                }
            }
                .onSuccess { archivo ->
                    _estado.update {
                        it.copy(
                            exportando = false,
                            archivo = ArchivoCv(archivo, "$nombre.pdf", accion),
                        )
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

    private fun nombreDeArchivo(nombre: String, idioma: String): String {
        val limpio = nombre.trim().ifBlank { "CV" }
            .replace(Regex("[^\\p{L}\\p{N} ]"), "")
            .replace(' ', '_')
            .take(40)
        return "CV_${limpio}_${idioma.uppercase()}"
    }

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
