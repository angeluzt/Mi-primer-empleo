package com.angeluzt.miprimerempleo.bitacora

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BitacoraTest {

    private fun entrevista(id: String, dia: Long, empresa: String, vararg preguntas: Pair<String, Boolean>) =
        Entrevista(
            id = id,
            fecha = dia * 86_400_000L,
            empresa = empresa,
            puesto = "Analista Jr.",
            preguntas = preguntas.map { (texto, supe) -> Pregunta(texto, laSupe = supe) },
        )

    private val bitacora = listOf(
        entrevista(
            "1", 1, "Bimbo",
            "¿Cuál es tu mayor debilidad?" to false,
            "Cuéntame de ti" to true,
            "¿Por qué quieres trabajar aquí?" to true,
        ),
        entrevista(
            "2", 5, "Femsa",
            "Háblame de ti" to true,
            "¿Qué debilidades tienes?" to false,
            "¿Cuál es tu mayor fortaleza?" to true,
            "¿Cómo usarías una tabla dinámica?" to false,
        ),
        entrevista(
            "3", 9, "Liverpool",
            "Dime cuál dirías que es tu mayor debilidad" to false,
            "¿Dónde te ves en cinco años?" to true,
        ),
    )

    @Test
    fun `la misma pregunta escrita distinto cuenta como una sola`() {
        val estudio = AnalisisBitacora.listaDeEstudio(bitacora)
        val debilidad = estudio.first()
        assertEquals(3, debilidad.veces)
        assertEquals(listOf("Liverpool", "Femsa", "Bimbo"), debilidad.empresas)
        // La más reciente es la que se muestra.
        assertEquals("Dime cuál dirías que es tu mayor debilidad", debilidad.pregunta)
    }

    @Test
    fun `fortaleza y debilidad no se confunden`() {
        val estudio = AnalisisBitacora.listaDeEstudio(bitacora)
        assertFalse(estudio.any { it.pregunta.contains("fortaleza") })
        assertEquals(2, estudio.size)
        assertTrue(estudio.any { it.pregunta.contains("tabla dinámica") })
    }

    @Test
    fun `lo que se repite incluye lo que si supo`() {
        val repetidas = AnalisisBitacora.seRepiten(bitacora).map { it.veces to it.pregunta }
        assertTrue(repetidas.toString(), repetidas.any { it.first == 2 && it.second.contains("de ti") })
        assertTrue(repetidas.any { it.first == 3 })
    }

    @Test
    fun `marcar un tema como preparado lo marca en todas las entrevistas y lo manda al final`() {
        val clave = AnalisisBitacora.listaDeEstudio(bitacora).first().clave
        val actualizada = AnalisisBitacora.marcarPreparada(bitacora, clave, preparada = true)

        val estudio = AnalisisBitacora.listaDeEstudio(actualizada)
        assertTrue(estudio.last().preparada)
        assertTrue(estudio.last().pregunta.contains("debilidad"))
        assertFalse(estudio.first().preparada)
        assertEquals(3, actualizada.flatMap { it.preguntas }.count { it.preparada })
    }

    @Test
    fun `sin preguntas falladas no hay nada que estudiar`() {
        val bien = listOf(entrevista("1", 1, "X", "Cuéntame de ti" to true))
        assertTrue(AnalisisBitacora.listaDeEstudio(bien).isEmpty())
    }

    @Test
    fun `los avances cuentan ofertas y siguientes etapas`() {
        val conResultados = bitacora.mapIndexed { i, e ->
            e.copy(resultado = listOf(Resultado.AVANCE, Resultado.RECHAZO, Resultado.OFERTA)[i])
        }
        assertEquals(2, AnalisisBitacora.avances(conResultados))
    }
}
