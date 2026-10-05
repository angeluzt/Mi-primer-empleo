package com.angeluzt.miprimerempleo.cv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** La entrevista pide lo que hace bueno a un CV sin volverse un interrogatorio. */
class GuionEntrevistaTest {

    @Test
    fun `una experiencia sin numeros pide uno`() {
        val pregunta = GuionEntrevista.repregunta(
            "experiencia",
            "Fui practicante en una fábrica de empaques y ayudaba a medir tiempos de la línea",
        )
        assertNotNull(pregunta)
        assertTrue(pregunta!!, pregunta.contains("número"))
    }

    @Test
    fun `con una cifra ya no se pregunta nada`() {
        assertNull(GuionEntrevista.repregunta("proyectos", "Hice un inventario para 120 productos de la papelería"))
        assertNull(GuionEntrevista.repregunta("proyectos", "Hice un inventario y bajé los faltantes a la mitad"))
        assertNull(GuionEntrevista.repregunta("experiencia", "Atendí a unos veinte clientes por turno en la tienda"))
    }

    @Test
    fun `una respuesta muy corta pide detalle`() {
        val pregunta = GuionEntrevista.repregunta("experiencia", "Sí, en una tienda")
        assertTrue(pregunta!!, pregunta.contains("puesto"))
    }

    @Test
    fun `solo experiencia y proyectos tienen seguimiento y saltar nunca lo dispara`() {
        assertNull(GuionEntrevista.repregunta("habilidades", "Excel"))
        assertNull(GuionEntrevista.repregunta("nombre", "Ana"))
        assertNull(GuionEntrevista.repregunta("experiencia", ""))
    }

    @Test
    fun `la reaccion celebra los numeros y calla si salto`() {
        assertTrue(GuionEntrevista.reaccion("proyectos", "bajé de 8 a 2")!!.contains("número"))
        assertNull(GuionEntrevista.reaccion("experiencia", ""))
    }

    @Test
    fun `las habilidades repetidas se quitan sin perder ninguna distinta`() {
        val par = NormalizadorCv.aParCv(
            """{"es": {"datos": {"nombre": "Ana"}, "resumen": "x", "habilidades": ["Excel", "excel", "Power BI", "SQL", "Excel"]}}""",
        )
        assertEquals(listOf("Excel", "Power BI", "SQL"), par.es.habilidades)
    }
}
