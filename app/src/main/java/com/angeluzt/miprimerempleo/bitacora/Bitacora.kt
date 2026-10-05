package com.angeluzt.miprimerempleo.bitacora

import kotlinx.serialization.Serializable
import java.text.Normalizer

/**
 * La Bitácora de entrevistas que prometen el capítulo "Usa las entrevistas que fallaste" y el
 * paywall: registras cada entrevista, marcas qué no supiste contestar, y la app arma tu lista
 * de estudio y te dice qué pregunta se te repite.
 *
 * Todo vive en el teléfono. Son notas personales sobre procesos de selección: no hay ninguna
 * razón para mandarlas a un servidor, y sí muchas para no hacerlo.
 */
@Serializable
data class Pregunta(
    val texto: String,
    /** false = no supo contestarla bien. Esa es "la lista de oro". */
    val laSupe: Boolean = true,
    /** La persona ya la estudió y preparó una respuesta. */
    val preparada: Boolean = false,
)

@Serializable
enum class Resultado(val etiqueta: String) {
    ESPERANDO("Esperando respuesta"),
    AVANCE("Pasé a la siguiente etapa"),
    OFERTA("Me ofrecieron el puesto"),
    RECHAZO("No quedé"),
    SIN_RESPUESTA("Nunca respondieron"),
}

@Serializable
data class Entrevista(
    val id: String,
    /** Milisegundos desde 1970: la fecha de la entrevista, no la de registro. */
    val fecha: Long,
    val empresa: String,
    val puesto: String,
    val resultado: Resultado = Resultado.ESPERANDO,
    val preguntas: List<Pregunta> = emptyList(),
    /** En qué momento sintió que se enfrió la conversación. */
    val seEnfrio: String = "",
    /** Qué le preguntaron de la empresa o el puesto que no había investigado. */
    val noInvestigue: String = "",
    /** Una cosa que haría distinto la próxima vez. */
    val distinto: String = "",
) {
    val titulo: String get() = listOf(puesto, empresa).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Entrevista" }
    val noSupo: Int get() = preguntas.count { !it.laSupe }
}

/** Un tema de la lista de estudio: una pregunta que no supo contestar, quizá en varias entrevistas. */
data class Tema(
    /** La pregunta como la escribió la vez más reciente. */
    val pregunta: String,
    /** En cuántas entrevistas apareció (sabiéndola o no). */
    val veces: Int,
    val empresas: List<String>,
    val preparada: Boolean,
    /** Para marcarla como preparada en todas las entrevistas donde aparece. */
    val clave: String,
)

object AnalisisBitacora {

    /**
     * Las preguntas que no supo contestar, agrupadas aunque las haya escrito distinto cada vez
     * ("¿Cuál es tu mayor debilidad?" y "qué debilidades tienes"). Primero las que más se
     * repiten y las que aún no prepara: es exactamente lo que le toca estudiar.
     */
    fun listaDeEstudio(entrevistas: List<Entrevista>): List<Tema> {
        val grupos = agrupar(entrevistas)
        return grupos
            .filter { grupo -> grupo.any { !it.pregunta.laSupe } }
            .map { grupo -> aTema(grupo) }
            .sortedWith(compareBy<Tema> { it.preparada }.thenByDescending { it.veces })
    }

    /** Las preguntas que aparecieron en dos o más entrevistas: el mercado se las está pidiendo. */
    fun seRepiten(entrevistas: List<Entrevista>): List<Tema> =
        agrupar(entrevistas).map { aTema(it) }.filter { it.veces >= 2 }.sortedByDescending { it.veces }

    /** Marca como preparada la pregunta en todas las entrevistas donde aparece. */
    fun marcarPreparada(entrevistas: List<Entrevista>, clave: String, preparada: Boolean): List<Entrevista> {
        val objetivo = palabras(clave)
        return entrevistas.map { entrevista ->
            entrevista.copy(
                preguntas = entrevista.preguntas.map { p ->
                    if (parecidas(palabras(p.texto), objetivo)) p.copy(preparada = preparada) else p
                },
            )
        }
    }

    /** Cuánto le está yendo mejor: de las últimas entrevistas, qué parte avanzó. */
    fun avances(entrevistas: List<Entrevista>): Int =
        entrevistas.count { it.resultado == Resultado.AVANCE || it.resultado == Resultado.OFERTA }

    // ---------- Agrupar ----------

    private data class Aparicion(val pregunta: Pregunta, val entrevista: Entrevista, val palabras: Set<String>)

    private fun agrupar(entrevistas: List<Entrevista>): List<List<Aparicion>> {
        val grupos = mutableListOf<MutableList<Aparicion>>()
        entrevistas.sortedByDescending { it.fecha }.forEach { entrevista ->
            entrevista.preguntas.filter { it.texto.isNotBlank() }.forEach { pregunta ->
                val aparicion = Aparicion(pregunta, entrevista, palabras(pregunta.texto))
                val grupo = grupos.firstOrNull { parecidas(it.first().palabras, aparicion.palabras) }
                if (grupo != null) grupo += aparicion else grupos += mutableListOf(aparicion)
            }
        }
        return grupos
    }

    private fun aTema(grupo: List<Aparicion>) = Tema(
        pregunta = grupo.first().pregunta.texto.trim(),
        veces = grupo.map { it.entrevista.id }.distinct().size,
        empresas = grupo.map { it.entrevista.empresa }.filter { it.isNotBlank() }.distinct(),
        preparada = grupo.filter { !it.pregunta.laSupe }.all { it.pregunta.preparada } &&
            grupo.any { !it.pregunta.laSupe },
        clave = grupo.first().pregunta.texto,
    )

    /**
     * Dos preguntas son la misma si comparten la mayoría de sus palabras con significado.
     * Se usa el coeficiente de superposición (no Jaccard) porque las preguntas son cortas y
     * una puede tener palabras de más ("¿Cuál dirías que es tu mayor debilidad?").
     */
    private fun parecidas(a: Set<String>, b: Set<String>): Boolean {
        if (a.isEmpty() || b.isEmpty()) return false
        val comunes = a.intersect(b).size
        val menor = minOf(a.size, b.size)
        val minimoComun = if (menor >= 3) 2 else 1
        return comunes >= minimoComun && comunes.toFloat() / menor >= 0.6f
    }

    private fun palabras(texto: String): Set<String> {
        val todas = Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotEmpty() && it !in VACIAS }
        val fuertes = todas.filter { it.length >= 3 }.map { if (it.length > 5) it.take(5) else it }.toSet()
        // "Cuéntame de ti" se queda sin palabras fuertes, y es la pregunta más común de todas:
        // en ese caso se compara con lo poco que queda ("de", "ti").
        return fuertes.ifEmpty { todas.toSet() }
    }

    /** Palabras que no distinguen una pregunta de otra, incluidas las muletillas de entrevista. */
    private val VACIAS = setOf(
        "que", "cual", "cuales", "como", "por", "para", "con", "una", "uno", "unos", "unas", "los", "las",
        "del", "the", "and", "you", "your", "what", "why", "how", "tus", "mas", "pero", "este", "esta",
        "esto", "eso", "esa", "ese", "algo", "alguna", "algun", "alguno", "donde", "cuando", "quien",
        "dime", "cuentame", "hablame", "platicame", "describe", "describeme", "explica", "explicame",
        "dirias", "crees", "consideras", "piensas", "puedes", "podrias", "seria", "tienes", "tiene",
        "tener", "haces", "hace", "hacer", "hiciste", "eres", "estas", "son", "has", "hay", "tuya",
        "tuyo", "mayor", "menor", "principal", "principales", "mejor", "peor", "sobre", "acerca",
        "vez", "alguna", "ejemplo", "situacion", "momento",
    )
}
