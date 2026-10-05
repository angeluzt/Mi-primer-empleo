package com.angeluzt.miprimerempleo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.angeluzt.miprimerempleo.BuildConfig
import com.angeluzt.miprimerempleo.MiPrimerEmpleoApp
import com.angeluzt.miprimerempleo.cv.Adaptacion
import com.angeluzt.miprimerempleo.cv.Cv
import com.angeluzt.miprimerempleo.cv.DetectorEstafas
import com.angeluzt.miprimerempleo.cv.ExportadorCv
import com.angeluzt.miprimerempleo.cv.ParCv
import com.angeluzt.miprimerempleo.cv.SenalEstafa
import com.angeluzt.miprimerempleo.cv.Sospecha
import com.angeluzt.miprimerempleo.cv.VerificadorCv
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class EstadoVacante(
    val texto: String = "",
    /** Lo que el detector local encontró en la vacante, al instante y sin IA. */
    val senales: List<SenalEstafa> = emptyList(),
    val adaptando: Boolean = false,
    val resultado: Adaptacion? = null,
    /** Cifras o habilidades del CV adaptado que no estaban en el CV original. */
    val sospechas: List<Sospecha> = emptyList(),
    val error: String? = null,
    val exportando: Boolean = false,
    val archivo: ArchivoCv? = null,
    val aviso: String? = null,
) {
    /** Menos de esto no es una vacante: es un título suelto, y gastar una generación sería tirarla. */
    val textoSuficiente: Boolean get() = texto.trim().length >= 120
}

/**
 * Revisar una vacante: primero, gratis y al instante, si huele a fraude. Después, con IA y una
 * generación, qué tanto la cubre la persona y su CV reordenado para ese puesto.
 */
class VacanteViewModel(app: Application) : AndroidViewModel(app) {

    private val contexto = app as MiPrimerEmpleoApp
    private val _estado = MutableStateFlow(EstadoVacante())
    val estado: StateFlow<EstadoVacante> = _estado.asStateFlow()

    fun cambiarTexto(texto: String) {
        _estado.update { it.copy(texto = texto, senales = DetectorEstafas.revisar(texto), error = null) }
    }

    fun adaptar(par: ParCv) {
        val actual = _estado.value
        if (actual.adaptando || !actual.textoSuficiente) return
        _estado.update { it.copy(adaptando = true, error = null) }

        viewModelScope.launch {
            val guardado = contexto.progreso.estado.first()
            val llave = if (BuildConfig.LLAVE_LOCAL_PERMITIDA) guardado.llaveOpenAi else ""
            if (llave.isBlank() && BuildConfig.BACKEND_URL.contains("TU-PROYECTO")) {
                _estado.update {
                    it.copy(
                        adaptando = false,
                        error = if (BuildConfig.LLAVE_LOCAL_PERMITIDA) {
                            "Falta configurar la IA. Entra a Ajustes y pega tu llave de OpenAI."
                        } else {
                            "El servicio no está disponible en este momento."
                        },
                    )
                }
                return@launch
            }

            // Si la vacante está en inglés se parte del CV en inglés: la redacción sale mejor
            // y los hechos son los mismos.
            val original = if (idiomaDe(actual.texto) == "en") par.en else par.es

            contexto.clienteCv
                .adaptar(
                    purchaseToken = contexto.compras.tokenDelPase().orEmpty(),
                    cv = original,
                    vacante = actual.texto,
                    pais = guardado.pais,
                    llaveLocal = llave,
                )
                .onSuccess { adaptacion ->
                    // A las alertas de la IA se suman las del detector local: no dependen de
                    // que el modelo las haya visto.
                    val locales = actual.senales.map { it.explicacion }
                    val completa = adaptacion.copy(alertas = (locales + adaptacion.alertas).distinct())
                    contexto.adaptaciones.guardar(completa)
                    contexto.progreso.registrarCvGenerado()
                    _estado.update {
                        it.copy(
                            adaptando = false,
                            resultado = completa,
                            sospechas = verificar(completa.cv, original),
                        )
                    }
                }
                .onFailure { fallo ->
                    _estado.update {
                        it.copy(adaptando = false, error = fallo.message ?: "No pudimos adaptar tu CV.")
                    }
                }
        }
    }

    /** Abre una adaptación guardada. */
    fun abrir(adaptacion: Adaptacion, par: ParCv?) {
        val original = par?.let { if (adaptacion.idioma == "en") it.en else it.es }
        _estado.update {
            it.copy(
                resultado = adaptacion,
                texto = adaptacion.vacante,
                senales = DetectorEstafas.revisar(adaptacion.vacante),
                sospechas = original?.let { cv -> verificar(adaptacion.cv, cv) }.orEmpty(),
                error = null,
            )
        }
    }

    fun nueva() = _estado.update { EstadoVacante() }

    fun exportar(accion: AccionArchivo) {
        val adaptacion = _estado.value.resultado ?: return
        if (_estado.value.exportando) return
        _estado.update { it.copy(exportando = true) }
        viewModelScope.launch {
            val guardado = contexto.progreso.estado.first()
            val sufijo = adaptacion.empresa.ifBlank { adaptacion.puesto }.ifBlank { "vacante" }
            runCatching {
                ExportadorCv.pdf(contexto, adaptacion.cv, guardado.plantillaCv, guardado.fotoCv, sufijo)
            }
                .onSuccess { (archivo, nombre) ->
                    _estado.update { it.copy(exportando = false, archivo = ArchivoCv(archivo, nombre, accion)) }
                }
                .onFailure {
                    _estado.update { it.copy(exportando = false, error = "No pudimos armar el PDF. Inténtalo otra vez.") }
                }
        }
    }

    fun archivoEntregado(aviso: String?) = _estado.update { it.copy(archivo = null, aviso = aviso) }
    fun descartarAviso() = _estado.update { it.copy(aviso = null) }
    fun descartarError() = _estado.update { it.copy(error = null) }

    /**
     * El CV adaptado solo puede tener hechos del CV original. Se revisa con el mismo
     * verificador que el CV principal, usando el CV original como "lo que la persona dijo".
     */
    private fun verificar(adaptado: Cv, original: Cv): List<Sospecha> =
        VerificadorCv.revisar(adaptado, mapOf("cv" to Json.encodeToString(original)))

    private companion object {
        private val INGLES = Regex("\\b(the|and|with|you|your|experience|requirements|skills|we|our|years)\\b", RegexOption.IGNORE_CASE)
        private val ESPANOL = Regex("\\b(el|la|los|las|con|para|experiencia|requisitos|años|ofrecemos|nuestro)\\b", RegexOption.IGNORE_CASE)

        fun idiomaDe(texto: String): String =
            if (INGLES.findAll(texto).count() > ESPANOL.findAll(texto).count()) "en" else "es"
    }
}
