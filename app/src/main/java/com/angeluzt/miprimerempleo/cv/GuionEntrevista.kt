package com.angeluzt.miprimerempleo.cv

/**
 * Las preguntas del CV son siempre las mismas, así que van escritas aquí en vez de
 * pedírselas a la IA una por una.
 *
 * Dejar que el modelo decidiera la siguiente pregunta salía caro, lento y, sobre todo,
 * no terminaba: sin memoria de lo ya preguntado volvía a pedir lo mismo y la respuesta
 * nueva pisaba la anterior. Un guion fijo responde al instante, no cuesta nada y llega
 * al final siempre. La IA sigue haciendo lo único que de verdad necesita hacer:
 * redactar el CV a partir de estas respuestas.
 */
data class Campo(
    val id: String,
    val pregunta: String,
    val ayuda: String = "",
    /** Si se puede saltar. Los que no, hacen falta para que el CV sirva de algo. */
    val opcional: Boolean = true,
    val sugerencias: List<String> = emptyList(),
)

object GuionEntrevista {

    val campos = listOf(
        Campo(
            id = "nombre",
            pregunta = "¿Cuál es tu nombre completo?",
            opcional = false,
        ),
        Campo(
            id = "puesto_buscado",
            pregunta = "¿Qué puesto estás buscando?",
            ayuda = "Aunque no te sientas listo todavía, escribe el que quieres.",
            opcional = false,
        ),
        Campo(
            id = "ciudad",
            pregunta = "¿En qué ciudad vives?",
            ayuda = "Solo ciudad y estado. Nunca pongas tu dirección exacta en un CV.",
            opcional = false,
        ),
        Campo(
            id = "contacto",
            pregunta = "¿Tu teléfono y tu correo?",
            ayuda = "Usa un correo serio. El que hiciste en secundaria no.",
            opcional = false,
        ),
        Campo(
            id = "enlaces",
            pregunta = "¿Tienes LinkedIn o un portafolio?",
            ayuda = "Pega los links. Si no tienes, sáltalo.",
        ),
        Campo(
            id = "formacion",
            pregunta = "¿Qué estudiaste, dónde y en qué años?",
            ayuda = "Ejemplo: Ingeniería Industrial, Universidad de Guadalajara, 2020 a 2025.",
            opcional = false,
        ),
        Campo(
            id = "experiencia",
            pregunta = "¿Has trabajado, hecho prácticas o servicio social?",
            ayuda = "Cuéntame el puesto, dónde, cuándo y qué hacías. Si recuerdas algún " +
                "número (cuántas personas, cuánto bajó algo, cuántos productos), dímelo: " +
                "es lo que más peso tiene.",
        ),
        Campo(
            id = "proyectos",
            pregunta = "¿Has hecho algún proyecto por tu cuenta o para la escuela?",
            ayuda = "Qué problema resolvía, qué hiciste tú y qué resultado dio. " +
                "Si está en internet, pega el link.",
        ),
        Campo(
            id = "cursos",
            pregunta = "¿Tienes cursos o certificaciones?",
            ayuda = "Nombre del curso, quién lo dio y el año.",
        ),
        Campo(
            id = "habilidades",
            pregunta = "¿Qué herramientas o habilidades manejas?",
            ayuda = "Programas, técnicas, maquinaria. Solo lo que de verdad sabes usar.",
        ),
        Campo(
            id = "idiomas",
            pregunta = "¿Qué idiomas hablas y en qué nivel?",
            ayuda = "Sé honesto: si pones inglés avanzado, te van a entrevistar en inglés.",
            sugerencias = listOf(
                "Español nativo",
                "Español nativo, inglés básico",
                "Español nativo, inglés intermedio",
            ),
        ),
    )

    /** Con esto ya se puede armar un CV decente; lo demás lo enriquece. */
    private val minimos = setOf("nombre", "puesto_buscado", "formacion")

    fun siguienteDespuesDe(id: String): Campo? {
        val indice = campos.indexOfFirst { it.id == id }
        return campos.getOrNull(indice + 1)
    }

    fun posicionDe(id: String): Int = campos.indexOfFirst { it.id == id } + 1

    /**
     * Se puede generar en cuanto están los mínimos y hay algo que contar:
     * experiencia, un proyecto o cursos. Así nadie queda atrapado respondiendo.
     */
    fun sePuedeGenerar(respuestas: Map<String, String>): Boolean {
        val completos = respuestas.filterValues { it.isNotBlank() }.keys
        if (!completos.containsAll(minimos)) return false
        return completos.any { it in setOf("experiencia", "proyectos", "cursos") }
    }
}
