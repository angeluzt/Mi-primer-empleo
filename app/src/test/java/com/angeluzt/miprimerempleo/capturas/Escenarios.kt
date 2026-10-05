package com.angeluzt.miprimerempleo.capturas

import android.content.Context
import com.angeluzt.miprimerempleo.billing.EstadoCompras
import com.angeluzt.miprimerempleo.bitacora.Entrevista
import com.angeluzt.miprimerempleo.bitacora.Pregunta
import com.angeluzt.miprimerempleo.bitacora.Resultado
import com.angeluzt.miprimerempleo.cv.Adaptacion
import com.angeluzt.miprimerempleo.cv.DatoSensible
import com.angeluzt.miprimerempleo.cv.DetectorEstafas
import com.angeluzt.miprimerempleo.cv.Extension
import com.angeluzt.miprimerempleo.cv.Falta
import com.angeluzt.miprimerempleo.cv.GuionEntrevista
import com.angeluzt.miprimerempleo.cv.ParCv
import com.angeluzt.miprimerempleo.cv.Requisito
import com.angeluzt.miprimerempleo.cv.RevisionCv
import com.angeluzt.miprimerempleo.cv.Sospecha
import com.angeluzt.miprimerempleo.cv.TipoDato
import com.angeluzt.miprimerempleo.cv.cvDeMuestra
import com.angeluzt.miprimerempleo.data.EstadoAnuncios
import com.angeluzt.miprimerempleo.data.EstadoProgreso
import com.angeluzt.miprimerempleo.data.RepositorioContenido
import com.angeluzt.miprimerempleo.ui.EstadoApp
import com.angeluzt.miprimerempleo.ui.EstadoCv
import com.angeluzt.miprimerempleo.ui.EstadoVacante
import com.angeluzt.miprimerempleo.ui.TurnoCv
import kotlinx.coroutines.runBlocking

/**
 * Estados de la app armados con el contenido real del APK, para dibujar cada pantalla
 * como la vería una persona en un momento concreto de su recorrido.
 */
object Escenarios {

    private const val DIA = 86_400_000L
    private const val HOY = 1_759_752_000_000L // 6 de octubre de 2025, mediodía en México

    val par = ParCv(cvDeMuestra(), cvDeMuestra().copy(idioma = "en"))

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

    /** Ya pagó, armó su CV y está en plena búsqueda. */
    fun buscando(contexto: Context): EstadoApp = enCamino(contexto, pase = true).let { estado ->
        estado.copy(
            progreso = estado.progreso.copy(puntos = 132, cvsGenerados = 3),
            cv = par,
            entrevistas = entrevistas,
            adaptaciones = listOf(adaptacion),
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

    // ---------- CV ----------

    /** A media entrevista: contestó algunas preguntas y va en la de idiomas. */
    val entrevistando = EstadoCv(
        turnos = listOf(
            TurnoCv(false, "Vamos a armar tu CV. Son 11 preguntas cortas y puedes saltarte las que no apliquen."),
            TurnoCv(false, "¿Qué estudiaste, dónde y en qué años?"),
            TurnoCv(true, "Ingeniería Industrial en la UdeG, de 2020 a 2025"),
            TurnoCv(false, "¿Qué herramientas o habilidades manejas?"),
            TurnoCv(true, "Excel avanzado, Power BI, SQL básico"),
            TurnoCv(false, "¿Qué idiomas hablas y en qué nivel?\n\nSé honesto: si pones inglés avanzado, te van a entrevistar en inglés."),
        ),
        respuestas = mapOf("nombre" to "Ana López", "puesto_buscado" to "Analista", "formacion" to "Ingeniería"),
        campo = GuionEntrevista.campos.first { it.id == "idiomas" },
    )

    /** CV generado, revisado, con un dato tachado y una cifra que no se dijo. */
    val cvListo = EstadoCv(
        turnos = listOf(
            TurnoCv(true, "Español nativo, inglés B2"),
            TurnoCv(false, "Listo. Lo estoy revisando como lo haría un reclutador; abajo te digo qué le falta."),
        ),
        respuestas = mapOf("nombre" to "Ana López Ramírez"),
        campo = null,
        cv = par,
        revision = RevisionCv(
            puntaje = 74,
            extension = Extension.BIEN,
            palabras = 236,
            veredicto = "Tienes un proyecto real con resultados medibles: eso pesa más que el promedio. Falta que se te pueda encontrar.",
            correoSirve = true,
            fuertes = listOf("El control de inventario tiene un antes y un después con números."),
            faltantes = listOf(
                Falta("enlaces", "Sin LinkedIn, el reclutador no puede verificar nada de lo que dices.", "¿Tienes LinkedIn o lo puedes crear hoy?"),
            ),
            arreglos = listOf("En la práctica, di cuántas personas había en la línea que mediste."),
        ),
        tachados = listOf(DatoSensible(TipoDato.CURP, "contacto", tachado = true)),
        sospechas = listOf(Sospecha("Experiencia · Practicante de Mejora Continua", "75%", "Reduje el desabasto 75%.")),
    )

    // ---------- Vacantes ----------

    private const val VACANTE_ESTAFA = "¡URGENTE! Gana hasta $5,000 a la semana desde casa, sin experiencia. " +
        "Solo cubre tu pago de capacitación de $350 y envía tu INE por WhatsApp para apartar tu lugar. " +
        "Forma parte de nuestra red de mercadeo e invita a tus amigos."

    val vacanteEstafa = EstadoVacante(texto = VACANTE_ESTAFA, senales = DetectorEstafas.revisar(VACANTE_ESTAFA))

    val adaptacion = Adaptacion(
        id = "a1",
        creada = HOY - 2 * DIA,
        puesto = "Analista de Datos Jr.",
        empresa = "Grupo Industrial del Bajío",
        idioma = "es",
        coincidencia = 68,
        cubres = listOf(
            Requisito("Excel avanzado", "Control de inventario en hojas de cálculo con 120 productos"),
            Requisito("Power BI", "Mencionado en tus habilidades"),
            Requisito("Ingeniería Industrial o afín", "Ingeniería Industrial, UdeG, 2020 – 2025"),
        ),
        teFalta = listOf(
            Requisito("Inglés avanzado", "Practica entrevistas en inglés dos veces por semana: tu B2 alcanza para empezar.", indispensable = true),
            Requisito("Python", "El curso gratuito «Python for Everybody» y un análisis de tus propios datos de inventario."),
        ),
        palabrasClave = listOf("Excel avanzado", "Power BI", "Mejora continua", "Inventarios"),
        mensaje = "Hola, me interesa la vacante de Analista de Datos Jr. Hice un control de inventario que bajó " +
            "el desabasto de 8 a 2 casos al mes, y ya trabajo con Excel y Power BI. ¿Podríamos agendar una entrevista?",
        cv = par.es,
        vacante = "Analista de Datos Jr. — Grupo Industrial del Bajío, León, Gto.",
    )

    val vacanteAnalizada = EstadoVacante(texto = adaptacion.vacante, resultado = adaptacion)

    // ---------- Bitácora ----------

    val entrevistas = listOf(
        Entrevista(
            id = "e3", fecha = HOY - 1 * DIA, empresa = "Liverpool", puesto = "Analista de inventarios",
            resultado = Resultado.ESPERANDO,
            preguntas = listOf(
                Pregunta("Dime cuál dirías que es tu mayor debilidad", laSupe = false),
                Pregunta("¿Dónde te ves en cinco años?"),
            ),
        ),
        Entrevista(
            id = "e2", fecha = HOY - 9 * DIA, empresa = "Femsa", puesto = "Becario de datos",
            resultado = Resultado.AVANCE,
            preguntas = listOf(
                Pregunta("Háblame de ti"),
                Pregunta("¿Qué debilidades tienes?", laSupe = false),
                Pregunta("¿Cómo usarías una tabla dinámica para esto?", laSupe = false, preparada = true),
            ),
        ),
        Entrevista(
            id = "e1", fecha = HOY - 20 * DIA, empresa = "Bimbo", puesto = "Practicante de mejora continua",
            resultado = Resultado.RECHAZO,
            preguntas = listOf(
                Pregunta("¿Cuál es tu mayor debilidad?", laSupe = false),
                Pregunta("Cuéntame de ti"),
            ),
            seEnfrio = "Cuando me preguntaron por SQL.",
            distinto = "Llevar un ejemplo de mi proyecto en el teléfono.",
        ),
    )
}
