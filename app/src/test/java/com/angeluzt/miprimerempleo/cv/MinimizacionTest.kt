package com.angeluzt.miprimerempleo.cv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nombre, teléfono, correo y enlaces no viajan a OpenAI cuando la IA no los necesita, y el CV
 * que ve la persona los tiene igual. Las dos mitades importan: si se ocultan pero no se
 * reponen, el CV sale sin contacto; si se reponen mal, sale con "[omitido]" impreso.
 */
class MinimizacionTest {

    private val respuestas = mapOf(
        "nombre" to "ana lópez de la cruz",
        "puesto_buscado" to "Analista de datos",
        "ciudad" to "Guadalajara",
        "contacto" to "33 1234 5678, ana.lopez.datos@gmail.com",
        "enlaces" to "linkedin.com/in/analopez y github.com/analopez",
        "experiencia" to "Practicante en Manufacturas del Valle",
        "cursos" to "",
    )

    // ---------- Lo que sale del teléfono ----------

    @Test
    fun `al generar no viaja ningun dato de contacto`() {
        val enviadas = ProteccionDatos.sinContacto(respuestas)
        val texto = enviadas.values.joinToString(" ")

        listOf("lópez", "1234", "gmail", "linkedin", "github").forEach { dato ->
            assertFalse("se coló «$dato»: $texto", texto.contains(dato, ignoreCase = true))
        }
        assertEquals(ProteccionDatos.OMITIDO, enviadas["nombre"])
        assertEquals(ProteccionDatos.OMITIDO, enviadas["contacto"])
        assertEquals(ProteccionDatos.OMITIDO, enviadas["enlaces"])
    }

    @Test
    fun `al generar lo demas llega intacto y lo vacio sigue vacio`() {
        val enviadas = ProteccionDatos.sinContacto(respuestas + ("enlaces" to "  "))

        assertEquals("Guadalajara", enviadas["ciudad"])
        assertEquals("Analista de datos", enviadas["puesto_buscado"])
        assertEquals("Practicante en Manufacturas del Valle", enviadas["experiencia"])
        // Sin enlaces no se marca nada: la IA debe saber que de verdad faltan.
        assertEquals("", enviadas["enlaces"]!!.trim())
        assertEquals("", enviadas["cursos"])
    }

    @Test
    fun `la revision recibe nombre correo y enlaces pero no el telefono`() {
        val cv = ProteccionDatos.paraRevisar(cvDeMuestra())

        assertEquals(ProteccionDatos.OMITIDO, cv.datos.telefono)
        assertEquals("Ana López Ramírez", cv.datos.nombre)
        assertEquals("ana.lopez.datos@gmail.com", cv.datos.correo)
        assertEquals("linkedin.com/in/analopezr", cv.datos.linkedin)
        assertEquals(cvDeMuestra().experiencia, cv.experiencia)
    }

    @Test
    fun `la adaptacion no recibe ningun dato de contacto`() {
        val cv = ProteccionDatos.paraAdaptar(cvDeMuestra().copy(datos = cvDeMuestra().datos.copy(portafolio = "")))

        with(cv.datos) {
            listOf(nombre, telefono, correo, linkedin).forEach { assertEquals(ProteccionDatos.OMITIDO, it) }
            assertEquals("", portafolio)
            assertEquals("Guadalajara, Jal.", ciudad)
            assertEquals("Analista de Datos Junior", puesto)
        }
        assertEquals(cvDeMuestra().resumen, cv.resumen)
    }

    @Test
    fun `un telefono vacio no se marca como si existiera`() {
        val sinTelefono = cvDeMuestra().copy(datos = cvDeMuestra().datos.copy(telefono = ""))
        assertEquals("", ProteccionDatos.paraRevisar(sinTelefono).datos.telefono)
    }

    // ---------- Lo que vuelve ----------

    @Test
    fun `el CV vuelve con el contacto real aunque la IA haya copiado la marca`() {
        val crudo = """
            {"es": {"datos": {"nombre": "[omitido]", "puesto": "Analista de datos", "ciudad": "Guadalajara",
                              "telefono": "[omitido]", "correo": "[omitido]", "linkedin": "", "portafolio": "[omitido]"},
                    "resumen": "Egresada de Ingeniería Industrial."},
             "en": {"datos": {"nombre": "", "puesto": "Data Analyst", "ciudad": "Guadalajara",
                              "telefono": "", "correo": "", "linkedin": "", "portafolio": ""},
                    "resumen": "Industrial Engineering graduate."}}
        """.trimIndent()

        val par = NormalizadorCv.conContactoReal(NormalizadorCv.aParCv(crudo, respuestas), respuestas)

        listOf(par.es, par.en).forEach { cv ->
            with(cv.datos) {
                assertEquals("Ana López de la Cruz", nombre)
                assertEquals("33 1234 5678", telefono)
                assertEquals("ana.lopez.datos@gmail.com", correo)
                assertEquals("linkedin.com/in/analopez", linkedin)
                assertEquals("github.com/analopez", portafolio)
            }
        }
        assertEquals("Data Analyst", par.en.datos.puesto)
    }

    @Test
    fun `si la persona no dio un dato vale el del modelo pero nunca la marca`() {
        val par = ParCv(
            cvDeMuestra().copy(datos = cvDeMuestra().datos.copy(telefono = "[omitido]", correo = "ana@correo.com")),
            cvDeMuestra().copy(idioma = "en"),
        )

        val real = NormalizadorCv.conContactoReal(par, mapOf("nombre" to "Ana López Ramírez"))

        assertEquals("", real.es.datos.telefono)
        // Lo pudo leer de un CV pegado: no hay de dónde más reponerlo.
        assertEquals("ana@correo.com", real.es.datos.correo)
        assertEquals("33 1234 5678", real.en.datos.telefono)
    }

    @Test
    fun `el nombre se escribe como nombre propio`() {
        assertEquals("Ana López de la Cruz", NormalizadorCv.nombrePropio("ana   lópez de la cruz "))
        assertEquals("José Luis y Pérez", NormalizadorCv.nombrePropio("josé luis y pérez"))
        // Si la persona ya usó mayúsculas, sabe cómo se escribe su nombre.
        assertEquals("María DeLuca", NormalizadorCv.nombrePropio("María DeLuca"))
        assertEquals("ANA LÓPEZ", NormalizadorCv.nombrePropio("ANA LÓPEZ"))
        assertEquals("", NormalizadorCv.nombrePropio("  "))
    }

    // ---------- El mensaje para postularse ----------

    @Test
    fun `el mensaje se firma con el nombre despues de la despedida`() {
        val mensaje = NormalizadorCv.firmar("Hola, me interesa la vacante.\n\nSaludos,", "Ana López")
        assertEquals("Hola, me interesa la vacante.\n\nSaludos,\nAna López", mensaje)
    }

    @Test
    fun `la marca y las plantillas no llegan al mensaje`() {
        val mensaje = NormalizadorCv.firmar(
            "Hola, soy [omitido] y me interesa el puesto.\n\nSaludos,\n[Tu nombre]\n[Teléfono]",
            "Ana López",
        )
        assertEquals("Hola, soy Ana López y me interesa el puesto.\n\nSaludos,", mensaje)
        assertFalse(mensaje.contains("["))
    }

    @Test
    fun `sin nombre conocido la marca simplemente desaparece`() {
        val mensaje = NormalizadorCv.firmar("Me interesa el puesto.\nSaludos,\n[omitido]", "")
        assertEquals("Me interesa el puesto.\nSaludos,", mensaje)
    }

    @Test
    fun `la adaptacion conserva el contacto original y firma el mensaje`() {
        val original = cvDeMuestra()
        val crudo = """
            {"puesto": "Analista", "empresa": "Bimbo", "idioma": "es", "coincidencia": 70,
             "cubres": [], "teFalta": [], "palabrasClave": [], "alertas": [],
             "mensaje": "Me interesa el puesto de Analista.\n\nSaludos,",
             "cv": {"datos": {"nombre": "[omitido]", "puesto": "Analista", "ciudad": "",
                              "telefono": "[omitido]", "correo": "[omitido]", "linkedin": "[omitido]", "portafolio": "[omitido]"},
                    "resumen": "Ingeniería Industrial con un proyecto de inventarios."}}
        """.trimIndent()

        val adaptacion = NormalizadorCv.aAdaptacion(crudo, original, "Vacante de Analista", "a1", 0L)

        assertEquals(original.datos.copy(puesto = "Analista"), adaptacion.cv.datos)
        assertTrue(adaptacion.mensaje, adaptacion.mensaje.endsWith("Saludos,\nAna López Ramírez"))
        assertFalse(adaptacion.cv.toString().contains(ProteccionDatos.OMITIDO))
    }
}
