package com.angeluzt.miprimerempleo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.angeluzt.miprimerempleo.BuildConfig
import com.angeluzt.miprimerempleo.MiPrimerEmpleoApp
import com.angeluzt.miprimerempleo.cv.ParCv
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TurnoCv(val esUsuario: Boolean, val texto: String)

data class EstadoCv(
    val turnos: List<TurnoCv> = APERTURA,
    val respuestas: Map<String, String> = emptyMap(),
    val campoActual: String = "nombre",
    val sugerencias: List<String> = emptyList(),
    val pensando: Boolean = false,
    val listoParaGenerar: Boolean = false,
    val generando: Boolean = false,
    val cv: ParCv? = null,
    val error: String? = null,
) {
    companion object {
        /** La primera pregunta va fija para no gastar una llamada en algo que siempre es igual. */
        val APERTURA = listOf(
            TurnoCv(
                false,
                "Vamos a armar tu CV. Te voy a hacer preguntas cortas y con tus " +
                    "respuestas genero dos versiones: español e inglés.\n\n" +
                    "No invento nada: solo acomodo y redacto lo que tú me digas.",
            ),
            TurnoCv(false, "¿Cuál es tu nombre completo?"),
        )
    }
}

class CvViewModel(app: Application) : AndroidViewModel(app) {

    private val contexto = app as MiPrimerEmpleoApp
    private val _estado = MutableStateFlow(EstadoCv())
    val estado: StateFlow<EstadoCv> = _estado.asStateFlow()

    private suspend fun llaveLocal(): String =
        if (BuildConfig.LLAVE_LOCAL_PERMITIDA) contexto.progreso.estado.first().llaveOpenAi else ""

    private suspend fun token(): String = contexto.compras.tokenDelPase().orEmpty()

    /**
     * Sin llave pegada y sin backend desplegado no hay a quién preguntarle. Decirlo
     * de frente evita que la persona se quede mirando «Escribiendo…» sin entender.
     */
    private fun faltaConfiguracion(llave: String): String? {
        if (llave.isNotBlank()) return null
        if (!BuildConfig.BACKEND_URL.contains("TU-PROYECTO")) return null
        return if (BuildConfig.LLAVE_LOCAL_PERMITIDA) {
            "Todavía no hay a quién preguntarle. Entra a Ajustes y pega tu llave de OpenAI, " +
                "o compila la app apuntando a tu backend."
        } else {
            "El servicio no está disponible en este momento."
        }
    }

    fun responder(texto: String) {
        val limpio = texto.trim()
        if (limpio.isEmpty() || _estado.value.pensando) return

        _estado.update {
            it.copy(
                turnos = it.turnos + TurnoCv(true, limpio),
                respuestas = it.respuestas + (it.campoActual to limpio),
                sugerencias = emptyList(),
                pensando = true,
                error = null,
            )
        }

        viewModelScope.launch {
            val llave = llaveLocal()
            faltaConfiguracion(llave)?.let { aviso ->
                _estado.update { it.copy(pensando = false, error = aviso) }
                return@launch
            }

            contexto.clienteCv
                .siguientePregunta(token(), _estado.value.respuestas, llave)
                .onSuccess { siguiente ->
                    _estado.update {
                        if (siguiente.terminado) {
                            it.copy(
                                pensando = false,
                                listoParaGenerar = true,
                                turnos = it.turnos + TurnoCv(
                                    false,
                                    "Ya tengo lo necesario. Genera tu CV cuando quieras, " +
                                        "o sigue contándome si falta algo.",
                                ),
                            )
                        } else {
                            it.copy(
                                pensando = false,
                                campoActual = siguiente.campo,
                                sugerencias = siguiente.sugerencias,
                                turnos = it.turnos + TurnoCv(
                                    false,
                                    listOfNotNull(
                                        siguiente.pregunta,
                                        siguiente.ayuda.ifBlank { null },
                                    ).joinToString("\n\n"),
                                ),
                            )
                        }
                    }
                }
                .onFailure { fallo ->
                    _estado.update {
                        it.copy(pensando = false, error = fallo.message ?: "No pudimos continuar.")
                    }
                }
        }
    }

    fun generar() {
        if (_estado.value.generando) return
        _estado.update { it.copy(generando = true, error = null) }

        viewModelScope.launch {
            val llave = llaveLocal()
            faltaConfiguracion(llave)?.let { aviso ->
                _estado.update { it.copy(generando = false, error = aviso) }
                return@launch
            }

            contexto.clienteCv
                .generar(token(), _estado.value.respuestas, llaveLocal = llave)
                .onSuccess { par ->
                    contexto.progreso.registrarCvGenerado()
                    contexto.progreso.guardarCv(par)
                    _estado.update { it.copy(generando = false, cv = par) }
                }
                .onFailure { fallo ->
                    _estado.update {
                        it.copy(generando = false, error = fallo.message ?: "No pudimos generar tu CV.")
                    }
                }
        }
    }

    fun descartarError() = _estado.update { it.copy(error = null) }
}
