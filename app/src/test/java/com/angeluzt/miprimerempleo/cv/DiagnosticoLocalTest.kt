package com.angeluzt.miprimerempleo.cv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La revisión local le dice a la persona cosas incómodas sobre su CV ("tu correo no se
 * ve profesional"). Acertar importa: un falso positivo aquí es insultar a alguien por
 * su propio nombre. Por eso estas pruebas cargan más contra los falsos positivos que
 * contra los falsos negativos.
 */
class DiagnosticoLocalTest {

    private fun conCorreo(nombre: String, correo: String) = cvDeMuestra().let {
        it.copy(datos = it.datos.copy(nombre = nombre, correo = correo))
    }

    @Test
    fun `un correo con el nombre de la persona pasa`() {
        assertTrue(DiagnosticoLocal.correoSirve(conCorreo("Ana López Ramírez", "ana.lopez@gmail.com")))
        assertTrue(DiagnosticoLocal.correoSirve(conCorreo("José Alberto Hernández", "jahernandez@gmail.com")))
        assertTrue(DiagnosticoLocal.correoSirve(conCorreo("María Fernanda Ruiz", "mafer.ruiz@outlook.com")))
    }

    @Test
    fun `el dominio no decide nada`() {
        assertTrue(DiagnosticoLocal.correoSirve(conCorreo("Ana López", "ana.lopez@hotmail.es")))
        assertTrue(DiagnosticoLocal.correoSirve(conCorreo("Ana López", "ana.lopez@yahoo.com.mx")))
    }

    @Test
    fun `un nombre que contiene un apodo por accidente no se acusa`() {
        // "alexdavid" contiene "xd" y no tiene nada de malo.
        assertTrue(DiagnosticoLocal.correoSirve(conCorreo("Alex David Soto", "alexdavid.soto@gmail.com")))
    }

    @Test
    fun `los apodos y los correos llenos de numeros sí se señalan`() {
        assertFalse(DiagnosticoLocal.correoSirve(conCorreo("Ana López", "lachikisbaby@gmail.com")))
        assertFalse(DiagnosticoLocal.correoSirve(conCorreo("Ana López", "anita19980312@gmail.com")))
        assertFalse(DiagnosticoLocal.correoSirve(conCorreo("Ana López", "")))
    }

    @Test
    fun `sin logros con numeros lo dice, con numeros se calla`() {
        val sinCifras = cvDeMuestra().let {
            it.copy(
                proyectos = it.proyectos.map { p -> p.copy(logros = listOf("Hice un sistema")) },
                experiencia = it.experiencia.map { e -> e.copy(logros = listOf("Ayudé en la línea")) },
            )
        }
        assertTrue(DiagnosticoLocal.avisos(sinCifras).any { it.contains("número") })
        assertFalse(DiagnosticoLocal.avisos(cvDeMuestra()).any { it.contains("número") })
    }

    @Test
    fun `la extension se mide con las palabras del CV, no con lo que diga la IA`() {
        val vacio = Cv("es", Datos("Ana", "Analista"), "")
        assertEquals(Extension.CORTO, DiagnosticoLocal.extension(vacio))
        assertEquals(0, DiagnosticoLocal.palabras(vacio))

        val inflado = cvDeMuestra().copy(resumen = "palabra ".repeat(600))
        assertEquals(Extension.LARGO, DiagnosticoLocal.extension(inflado))

        assertEquals(Extension.BIEN, DiagnosticoLocal.extension(cvDeMuestra().copy(
            resumen = "palabra ".repeat(200),
        )))
    }

    @Test
    fun `un CV sin experiencia ni proyectos recibe el aviso que más cuesta`() {
        val hueco = cvDeMuestra().copy(experiencia = emptyList(), proyectos = emptyList())
        assertTrue(DiagnosticoLocal.avisos(hueco).any { it.contains("experiencia ni proyectos") })
    }

    @Test
    fun `la revision de respaldo nunca deja a la persona sin nada que hacer`() {
        val pelado = Cv("es", Datos("Ana López", "Analista"), "")
        val revision = DiagnosticoLocal.revision(pelado)

        assertTrue(revision.arreglos.isNotEmpty())
        assertTrue(revision.veredicto.isNotBlank())
        assertTrue(revision.puntaje in 20..100)
        assertEquals(Extension.CORTO, revision.extension)
    }
}
