package com.angeluzt.miprimerempleo.cv

/**
 * La revisión del CV ya armado: si se quedó corto, si sobra relleno, si el correo
 * se ve serio y qué falta por contar.
 *
 * Existe porque el error más caro de un primer CV no es la redacción, es el hueco:
 * la persona hizo cosas y no las puso. Un reclutador lo detecta en diez segundos y
 * nadie se lo dice nunca. Aquí sí se le dice, con la pregunta exacta para llenarlo.
 */
enum class Extension { CORTO, BIEN, LARGO }

data class Falta(
    /** Id del campo de la entrevista, para poder reabrir esa pregunta. */
    val campo: String,
    val porque: String,
    val pregunta: String,
)

data class RevisionCv(
    val puntaje: Int = 0,
    val extension: Extension = Extension.BIEN,
    val palabras: Int = 0,
    val veredicto: String = "",
    val correoSirve: Boolean = true,
    val notaCorreo: String = "",
    val correoSugerido: String = "",
    val fuertes: List<String> = emptyList(),
    val faltantes: List<Falta> = emptyList(),
    val arreglos: List<String> = emptyList(),
) {
    val tieneAlgoQueDecir: Boolean
        get() = veredicto.isNotBlank() || faltantes.isNotEmpty() || arreglos.isNotEmpty()

    /** Solo se ofrece reabrir preguntas que existen de verdad en el guion. */
    val faltantesAccionables: List<Falta>
        get() = faltantes.filter { falta -> GuionEntrevista.campos.any { it.id == falta.campo } }
}

/**
 * Lo que se puede revisar sin gastar un solo token ni conexión.
 *
 * Sirve para dos cosas: que la persona reciba algo útil aunque la IA falle, y que el
 * conteo de palabras sea un hecho y no una estimación del modelo.
 */
object DiagnosticoLocal {

    private const val CORTO = 160
    private const val LARGO = 520

    /**
     * Apodos que cuestan entrevistas. Todos de cuatro letras o más a propósito: con
     * fragmentos cortos ("xd") se acusaría de poco serio a un "alexdavid@…", y decirle
     * eso a alguien que se llama así es peor que no decirle nada. Tampoco se juzga el
     * dominio: un correo de hotmail no tiene nada de malo.
     */
    private val CORREO_FLOJO = listOf(
        "sexy", "loco", "baby", "princes", "chamaco", "morrit", "diablo",
        "gamer", "bebe", "cute", "killer", "chikis", "hermoso", "hermosa",
    )

    fun palabras(cv: Cv): Int = textoPlano(cv).split(Regex("\\s+")).count { it.isNotBlank() }

    fun extension(cv: Cv): Extension = when {
        palabras(cv) < CORTO -> Extension.CORTO
        palabras(cv) > LARGO -> Extension.LARGO
        else -> Extension.BIEN
    }

    /** Un correo se ve serio cuando se parece al nombre de quien lo manda. */
    fun correoSirve(cv: Cv): Boolean {
        val correo = cv.datos.correo.lowercase()
        if (correo.isBlank()) return false
        val usuario = correo.substringBefore('@')
        if (CORREO_FLOJO.any { correo.contains(it) }) return false
        if (usuario.count { it.isDigit() } > 4) return false
        val partesNombre = cv.datos.nombre.lowercase().split(' ').filter { it.length > 2 }
        return partesNombre.isEmpty() || partesNombre.any { usuario.contains(it.take(4)) }
    }

    /** Avisos inmediatos, en el orden en que más cuestan una entrevista. */
    fun avisos(cv: Cv): List<String> = buildList {
        if (cv.datos.correo.isBlank()) add("No hay correo en tu CV. Sin correo no te pueden contestar.")
        else if (!correoSirve(cv)) {
            add("Tu correo no se ve profesional. Crea uno con tu nombre y apellido; es gratis y cambia la primera impresión.")
        }
        if (cv.datos.telefono.isBlank()) add("Falta tu teléfono.")
        if (cv.experiencia.isEmpty() && cv.proyectos.isEmpty()) {
            add("No hay experiencia ni proyectos. Un proyecto propio, un servicio social o algo que hayas hecho para alguien cuenta, y es lo primero que buscan.")
        }
        if (cv.resumen.isBlank()) add("Falta el resumen de arriba: son las dos líneas que sí se leen siempre.")
        if (cv.datos.linkedin.isBlank() && cv.datos.portafolio.isBlank()) {
            add("No hay LinkedIn ni portafolio. Un enlace donde te puedan ver vale más que una línea más de texto.")
        }
        val conNumero = (cv.experiencia.flatMap { it.logros } + cv.proyectos.flatMap { it.logros })
            .count { linea -> linea.any { it.isDigit() } }
        if (conNumero == 0 && (cv.experiencia.isNotEmpty() || cv.proyectos.isNotEmpty())) {
            add("Ninguno de tus logros tiene un número. Cuántas personas, cuánto bajó, cuántas piezas: eso es lo que separa un CV de otro.")
        }
        when (extension(cv)) {
            Extension.CORTO -> add("Tu CV se ve corto (${palabras(cv)} palabras). Con menos de $CORTO parece que no hiciste nada, aunque sí lo hayas hecho.")
            Extension.LARGO -> add("Tu CV se ve largo (${palabras(cv)} palabras). Arriba de $LARGO nadie lo lee completo; deja lo que más peso tenga.")
            Extension.BIEN -> Unit
        }
    }

    /** Revisión de respaldo cuando la IA no contestó: mejor esto que nada. */
    fun revision(cv: Cv): RevisionCv {
        val avisos = avisos(cv)
        return RevisionCv(
            puntaje = (100 - avisos.size * 12).coerceIn(20, 100),
            extension = extension(cv),
            palabras = palabras(cv),
            veredicto = if (avisos.isEmpty()) {
                "Tu CV tiene lo básico completo. Revísalo una vez más en voz alta y mándalo."
            } else {
                "Revisamos tu CV aquí en el teléfono. Esto es lo que le falta."
            },
            correoSirve = correoSirve(cv),
            arreglos = avisos,
        )
    }

    private fun textoPlano(cv: Cv): String = buildString {
        append(cv.resumen).append(' ')
        cv.proyectos.forEach { append(it.nombre).append(' ').append(it.descripcion).append(' ').append(it.logros.joinToString(" ")).append(' ') }
        cv.experiencia.forEach { append(it.puesto).append(' ').append(it.organizacion).append(' ').append(it.logros.joinToString(" ")).append(' ') }
        cv.formacion.forEach { append(it.titulo).append(' ').append(it.institucion).append(' ').append(it.nota).append(' ') }
        cv.certificaciones.forEach { append(it.nombre).append(' ').append(it.institucion).append(' ') }
        append(cv.habilidades.joinToString(" "))
    }
}
