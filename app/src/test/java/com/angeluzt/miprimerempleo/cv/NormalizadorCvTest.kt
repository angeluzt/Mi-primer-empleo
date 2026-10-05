package com.angeluzt.miprimerempleo.cv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Estas pruebas existen por un error real que vio un usuario en su teléfono:
 *
 *   Unexpected JSON token at offset 602: Expected beginning of the string,
 *   but got { at path: $.es.experiencia[0].logros[0]
 *
 * La IA devolvió los logros como objetos en vez de como texto y la generación se perdió
 * entera. Un modelo de lenguaje no garantiza el esquema, así que la app tiene que
 * aguantarlo. Cada caso de aquí es una forma en la que ya vimos (o esperamos ver) que
 * el modelo se sale del formato.
 */
class NormalizadorCvTest {

    @Test
    fun `logros como objetos no revientan la generacion`() {
        val crudo = """
            {"es":{"idioma":"es","datos":{"nombre":"Ana López","puesto":"Analista junior"},
            "resumen":"Recién egresada.",
            "experiencia":[{"puesto":"Practicante","organizacion":"Manufacturas del Valle",
            "periodo":"Ene 2025 – Jun 2025",
            "logros":[{"accion":"Realicé mantenimiento preventivo a 12 equipos"},
                      {"accion":"Medí tiempos de la línea de empaque"}]}]},
            "en":{"idioma":"en","datos":{"nombre":"Ana López","puesto":"Junior Analyst"},
            "resumen":"Recent graduate.","experiencia":[]}}
        """.trimIndent()

        val par = NormalizadorCv.aParCv(crudo)

        assertEquals("Ana López", par.es.datos.nombre)
        assertEquals(
            listOf(
                "Realicé mantenimiento preventivo a 12 equipos",
                "Medí tiempos de la línea de empaque",
            ),
            par.es.experiencia[0].logros,
        )
    }

    @Test
    fun `un logro partido en varios campos se junta en una frase`() {
        val crudo = """
            {"es":{"idioma":"es","datos":{"nombre":"Luis","puesto":"Almacén"},"resumen":"",
            "experiencia":[{"puesto":"Auxiliar","organizacion":"Tienda","periodo":"2024",
            "logros":[{"accion":"Reacomodé el almacén","resultado":"Bajó 30% el tiempo de surtido"}]}]}}
        """.trimIndent()

        val logro = NormalizadorCv.aParCv(crudo).es.experiencia[0].logros.single()

        assertTrue(logro, logro.contains("Reacomodé el almacén"))
        assertTrue(logro, logro.contains("Bajó 30% el tiempo de surtido"))
    }

    @Test
    fun `habilidades e idiomas en cualquier forma se leen igual`() {
        val crudo = """
            {"es":{"idioma":"es","datos":{"nombre":"Mara","puesto":"Diseño"},"resumen":"Hola.",
            "habilidades":"Illustrator, Figma; Photoshop",
            "idiomas":[{"idioma":"Español","nivel":"Nativo"},"Inglés: B2",{"Francés":"Básico"}]}}
        """.trimIndent()

        val cv = NormalizadorCv.aParCv(crudo).es

        assertEquals(listOf("Illustrator", "Figma", "Photoshop"), cv.habilidades)
        assertEquals(listOf("Español", "Inglés", "Francés"), cv.idiomas.map { it.idioma })
        assertEquals(listOf("Nativo", "B2", "Básico"), cv.idiomas.map { it.nivel })
    }

    @Test
    fun `numeros donde esperabamos texto no tumban nada`() {
        val crudo = """
            {"es":{"idioma":"es","datos":{"nombre":"Pedro","puesto":"Becario","telefono":5512345678},
            "resumen":"Hola.",
            "certificaciones":[{"nombre":"Scrum","institucion":"Platzi","anio":2025}]}}
        """.trimIndent()

        val cv = NormalizadorCv.aParCv(crudo).es

        assertEquals("5512345678", cv.datos.telefono)
        assertEquals("2025", cv.certificaciones[0].anio)
    }

    @Test
    fun `sin version en ingles se reusa la espanola en vez de fallar`() {
        val crudo = """
            {"es":{"idioma":"es","datos":{"nombre":"Rosa","puesto":"Ventas"},"resumen":"Hola."}}
        """.trimIndent()

        val par = NormalizadorCv.aParCv(crudo)

        assertEquals("Rosa", par.en.datos.nombre)
        assertEquals("en", par.en.idioma)
    }

    @Test
    fun `el JSON envuelto en un bloque de codigo se lee igual`() {
        val crudo = """
            Claro, aquí tienes el CV:
            ```json
            {"es":{"idioma":"es","datos":{"nombre":"Iván","puesto":"Soporte"},"resumen":"Hola."}}
            ```
        """.trimIndent()

        assertEquals("Iván", NormalizadorCv.aParCv(crudo).es.datos.nombre)
    }

    @Test
    fun `el contacto que falte se copia de lo que la persona escribio`() {
        val crudo = """
            {"es":{"idioma":"es","datos":{"nombre":"","puesto":""},"resumen":"Hola."}}
        """.trimIndent()
        val respuestas = mapOf(
            "nombre" to "Ana López Ramírez",
            "puesto_buscado" to "Analista de datos junior",
            "ciudad" to "Guadalajara",
            "contacto" to "33 1234 5678 y ana.lopez.datos@gmail.com",
            "enlaces" to "linkedin.com/in/analopezr y github.com/analopezr",
        )

        val datos = NormalizadorCv.aParCv(crudo, respuestas).es.datos

        assertEquals("Ana López Ramírez", datos.nombre)
        assertEquals("Analista de datos junior", datos.puesto)
        assertEquals("ana.lopez.datos@gmail.com", datos.correo)
        assertEquals("33 1234 5678", datos.telefono)
        assertEquals("linkedin.com/in/analopezr", datos.linkedin)
        assertEquals("github.com/analopezr", datos.portafolio)
    }

    @Test
    fun `una respuesta vacia falla con un mensaje que la persona entiende`() {
        val fallo = runCatching { NormalizadorCv.aParCv("no soy JSON") }.exceptionOrNull()

        assertTrue(fallo is IllegalStateException)
        assertTrue(fallo?.message.orEmpty().contains("otra vez"))
    }

    @Test
    fun `la revision tolera que el modelo use guion bajo en las llaves`() {
        val crudo = """
            {"puntaje":72,"extension":"corto","palabras":140,"veredicto":"Le falta sustancia.",
            "correo_sirve":false,"nota_correo":"Tu correo parece un apodo.",
            "correo_sugerido":"ana.lopez",
            "faltantes":[{"campo":"experiencia","porque":"No hay nada de trabajo","pregunta":"¿Hiciste servicio social?"},
                         {"campo":"inventado","porque":"x","pregunta":"y"}],
            "mejoras":["Agrega números a tus logros"]}
        """.trimIndent()

        val revision = NormalizadorCv.aRevision(crudo)

        assertEquals(72, revision.puntaje)
        assertEquals(Extension.CORTO, revision.extension)
        assertEquals(false, revision.correoSirve)
        assertEquals(listOf("Agrega números a tus logros"), revision.arreglos)
        // Un campo que no existe en el guion no se ofrece: el botón no llevaría a ningún lado.
        assertEquals(listOf("experiencia"), revision.faltantesAccionables.map { it.campo })
    }
}
