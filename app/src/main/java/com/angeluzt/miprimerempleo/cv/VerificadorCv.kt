package com.angeluzt.miprimerempleo.cv

import java.text.Normalizer

/**
 * Algo del CV que no se puede rastrear hasta lo que la persona dijo.
 *
 * @param donde Sección y elemento, para encontrarlo: "Experiencia · Practicante de ventas".
 * @param dato La cifra o la habilidad sospechosa, tal como aparece.
 * @param contexto La frase completa donde aparece.
 */
data class Sospecha(val donde: String, val dato: String, val contexto: String)

/**
 * La promesa de la app es que el CV no inventa nada. El prompt lo pide, pero un modelo de
 * lenguaje no garantiza obedecer: a veces "redondea" un 8 a 2 como "reduje 75%", o agrega
 * "trabajo en equipo" porque suena bien. Esto lo revisa en código, sin IA y sin red.
 *
 * Cifras: toda cifra del CV en español tiene que aparecer en las respuestas, escrita con
 * número o con palabra. Un porcentaje calculado por la IA cuenta como invento aunque la
 * cuenta esté bien: la persona tiene que poder defender cada número en la entrevista.
 * Límite conocido: compara cifras, no significados. Si la persona dijo "tres personas", un
 * "3 años de experiencia" inventado pasa. Es una red que atrapa lo común, no un detector perfecto.
 *
 * Habilidades: cada una tiene que compartir al menos una palabra significativa con lo que
 * la persona escribió. Es deliberadamente generoso, para no acusar de inventar a alguien
 * que escribió "atendía clientes" y recibió "Atención a clientes".
 *
 * No se borra nada: se señala, y la persona decide en el editor.
 */
object VerificadorCv {

    fun revisar(cv: Cv, respuestas: Map<String, String>): List<Sospecha> {
        if (respuestas.values.all { it.isBlank() }) return emptyList()
        return cifrasSinRespaldo(cv, respuestas) + habilidadesSinRespaldo(cv, respuestas)
    }

    fun cifrasSinRespaldo(cv: Cv, respuestas: Map<String, String>): List<Sospecha> {
        val dichas = numerosEn(respuestas.values.joinToString(" "))
        val sospechas = mutableListOf<Sospecha>()

        fun revisarTexto(texto: String, donde: String) {
            CIFRA.findAll(texto).forEach { coincidencia ->
                val cifra = normalizarCifra(coincidencia.value)
                if (cifra.length == 1 && cifra.toInt() <= 1) return@forEach
                if (esNivelDeIdioma(texto, coincidencia.range.first)) return@forEach
                if (cifra !in dichas) sospechas += Sospecha(donde, coincidencia.value, texto)
            }
        }

        revisarTexto(cv.resumen, "Resumen")
        cv.experiencia.forEach { e ->
            val donde = "Experiencia · ${e.puesto.ifBlank { e.organizacion }}"
            revisarTexto(e.periodo, donde)
            e.logros.forEach { revisarTexto(it, donde) }
        }
        cv.proyectos.forEach { p ->
            val donde = "Proyecto · ${p.nombre}"
            revisarTexto(p.descripcion, donde)
            p.logros.forEach { revisarTexto(it, donde) }
        }
        cv.formacion.forEach { revisarTexto(it.periodo, "Formación · ${it.titulo}") }
        cv.certificaciones.forEach { revisarTexto(it.anio, "Certificación · ${it.nombre}") }

        return sospechas.distinctBy { it.donde to it.dato }
    }

    fun habilidadesSinRespaldo(cv: Cv, respuestas: Map<String, String>): List<Sospecha> {
        val dicho = sinAcentos(respuestas.values.joinToString(" ")).lowercase()
        return cv.habilidades.filterNot { habilidad ->
            val palabras = sinAcentos(habilidad).lowercase().split(Regex("[^\\p{L}\\p{N}+#]+"))
                .filter { it.length >= 3 && it !in VACIAS }
            // Sin palabras significativas ("C#", "R") no hay forma honesta de juzgar.
            palabras.isEmpty() || palabras.any { palabra -> dicho.contains(raiz(palabra)) }
        }.map { Sospecha("Habilidades", it, it) }
    }

    // ---------- Cifras ----------

    private val CIFRA = Regex("\\d+(?:[.,]\\d+)*")

    /** "1,200", "1.200" y "1200" son la misma cifra; "12%" y "12 %" también. */
    private fun normalizarCifra(texto: String) = texto.replace(".", "").replace(",", "").trimStart('0').ifEmpty { "0" }

    private fun numerosEn(texto: String): Set<String> {
        val conDigitos = CIFRA.findAll(texto).map { normalizarCifra(it.value) }
        val conPalabras = sinAcentos(texto).lowercase().split(Regex("[^a-z]+"))
            .mapNotNull { PALABRAS[it]?.toString() }
        return (conDigitos + conPalabras).toSet()
    }

    /** "B2", "C1": un nivel de idioma no es una cifra que haya que respaldar. */
    private fun esNivelDeIdioma(texto: String, posicion: Int): Boolean =
        posicion > 0 && texto[posicion - 1].uppercaseChar() in "ABC" &&
            (posicion < 2 || !texto[posicion - 2].isLetter())

    private val PALABRAS = mapOf(
        "un" to 1, "uno" to 1, "una" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5,
        "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10, "once" to 11,
        "doce" to 12, "trece" to 13, "catorce" to 14, "quince" to 15, "dieciseis" to 16,
        "diecisiete" to 17, "dieciocho" to 18, "diecinueve" to 19, "veinte" to 20,
        "treinta" to 30, "cuarenta" to 40, "cincuenta" to 50, "sesenta" to 60, "setenta" to 70,
        "ochenta" to 80, "noventa" to 90, "cien" to 100, "ciento" to 100, "doscientos" to 200,
        "trescientos" to 300, "quinientos" to 500, "mil" to 1000,
        "medio" to 50, "mitad" to 50,
        // "Quedé en segundo lugar" se redacta "2.º lugar".
        "primer" to 1, "primero" to 1, "primera" to 1, "segundo" to 2, "segunda" to 2,
        "tercer" to 3, "tercero" to 3, "tercera" to 3, "cuarto" to 4, "cuarta" to 4,
        "quinto" to 5, "quinta" to 5,
    )

    // ---------- Habilidades ----------

    private val VACIAS = setOf(
        "de", "del", "la", "las", "los", "el", "en", "con", "para", "por", "and", "the", "basico",
        "basica", "avanzado", "avanzada", "intermedio", "intermedia", "manejo", "uso", "conocimiento",
        "conocimientos", "nivel", "herramientas",
    )

    /** Raíz corta para que "atención" y "atendía", o "análisis" y "analicé", se reconozcan. */
    private fun raiz(palabra: String) = if (palabra.length > 5) palabra.take(5) else palabra

    private fun sinAcentos(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
}
