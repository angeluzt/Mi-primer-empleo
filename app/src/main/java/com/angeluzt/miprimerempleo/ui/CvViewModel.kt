package com.angeluzt.miprimerempleo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.angeluzt.miprimerempleo.BuildConfig
import com.angeluzt.miprimerempleo.MiPrimerEmpleoApp
import com.angeluzt.miprimerempleo.cv.Campo
import com.angeluzt.miprimerempleo.cv.GuionEntrevista
import com.angeluzt.miprimerempleo.cv.ParCv
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TurnoCv(val esUsuario: Boolean, val texto: String)

data class EstadoCv(
    val turnos: List<TurnoCv> = emptyList(),
    val respuestas: Map<String, String> = emptyMap(),
    val campo: Campo? = GuionEntrevista.campos.first(),
    val generando: Boolean = false,
    val cv: ParCv? = null,
    val error: String? = null,
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

    private fun estadoInicial(): EstadoCv {
        val primero = GuionEntrevista.campos.first()
        return EstadoCv(
            turnos = listOf(
                TurnoCv(
                    false,
                    "Vamos a armar tu CV. Son ${GuionEntrevista.campos.size} preguntas cortas " +
                        "y puedes saltarte las que no apliquen.\n\n" +
                        "No invento nada: solo acomodo y redacto lo que tú me digas.",
                ),
                TurnoCv(false, textoDe(primero)),
            ),
            campo = primero,
        )
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
        val siguiente = GuionEntrevista.siguienteDespuesDe(actual.id)
        _estado.update { estado ->
            val respuestas =
                if (respuesta.isBlank()) estado.respuestas
                else estado.respuestas + (actual.id to respuesta)

            estado.copy(
                respuestas = respuestas,
                campo = siguiente,
                error = null,
                turnos = estado.turnos + turnoUsuario + listOf(
                    if (siguiente != null) {
                        TurnoCv(false, textoDe(siguiente))
                    } else {
                        TurnoCv(
                            false,
                            "Eso es todo. Dale a «Generar mi CV» y te lo armo en español e inglés.",
                        )
                    },
                ),
            )
        }
    }

    /** Vuelve a preguntar un campo ya contestado, por si la persona quiere corregirlo. */
    fun volverA(campoId: String) {
        val campo = GuionEntrevista.campos.firstOrNull { it.id == campoId } ?: return
        _estado.update {
            it.copy(campo = campo, turnos = it.turnos + TurnoCv(false, textoDe(campo)))
        }
    }

    fun generar() {
        val estado = _estado.value
        if (estado.generando || !estado.puedeGenerar) return
        _estado.update { it.copy(generando = true, error = null) }

        viewModelScope.launch {
            val llave =
                if (BuildConfig.LLAVE_LOCAL_PERMITIDA) contexto.progreso.estado.first().llaveOpenAi
                else ""

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
                            turnos = it.turnos + TurnoCv(
                                false,
                                "Listo. Revisa el resultado en «Ver formatos» y dime si algo " +
                                    "no coincide con lo que me contaste.",
                            ),
                        )
                    }
                }
                .onFailure { fallo ->
                    _estado.update {
                        it.copy(generando = false, error = fallo.message ?: "No pudimos generar tu CV.")
                    }
                }
        }
    }

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

    fun descartarError() = _estado.update { it.copy(error = null) }
}
