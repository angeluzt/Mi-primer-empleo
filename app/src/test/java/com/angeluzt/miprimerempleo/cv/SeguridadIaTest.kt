package com.angeluzt.miprimerempleo.cv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Las tres capas que protegen a la persona sin depender de que la IA obedezca:
 * qué datos no salen del teléfono, qué cifras no tienen respaldo y qué vacantes huelen a fraude.
 *
 * En las tres, un falso positivo cuesta: acusar de inventar a quien no inventó, o borrarle un
 * teléfono pensando que era un DNI. Por eso hay tantos casos de "esto NO se toca".
 */
class SeguridadIaTest {

    // ---------- Datos personales ----------

    @Test
    fun `el CURP y el RFC se tachan antes de salir del telefono`() {
        val (limpio, datos) = ProteccionDatos.tachar(
            "Mi CURP es LOPA950312MJCPRN09 y mi RFC LOPA950312AB1, tel 33 1234 5678",
        )

        assertFalse(limpio, limpio.contains("LOPA950312"))
        assertTrue(limpio, limpio.contains("33 1234 5678"))
        assertEquals(setOf(TipoDato.CURP, TipoDato.RFC), datos.map { it.tipo }.toSet())
        assertTrue(datos.all { it.tachado })
    }

    @Test
    fun `un CURP escrito en minusculas tambien se reconoce`() {
        val (limpio, _) = ProteccionDatos.tachar("curp: lopa950312mjcprn09")
        assertFalse(limpio, limpio.contains("lopa950312"))
    }

    @Test
    fun `DNI cedula y RUT se tachan solo cuando se presentan como tales`() {
        val (limpio, datos) = ProteccionDatos.tachar(
            "DNI 35.123.456. Cédula 1020304050. RUT 12.345.678-9. Teléfono 1145678901.",
        )

        assertFalse(limpio.contains("35.123.456"))
        assertFalse(limpio.contains("1020304050"))
        assertFalse(limpio.contains("12.345.678-9"))
        assertTrue("el teléfono no se toca: $limpio", limpio.contains("1145678901"))
        assertEquals(setOf(TipoDato.DNI, TipoDato.CEDULA, TipoDato.RUT), datos.map { it.tipo }.toSet())
    }

    @Test
    fun `un numero suelto de ocho digitos no se confunde con un DNI`() {
        val (limpio, datos) = ProteccionDatos.tachar("Mi celular es 55123456 y atiendo de 9 a 6")
        assertEquals("Mi celular es 55123456 y atiendo de 9 a 6", limpio)
        assertTrue(datos.isEmpty())
    }

    @Test
    fun `la fecha de nacimiento se quita pero el resto de la frase queda`() {
        val (limpio, datos) = ProteccionDatos.tachar("Nací el 12/03/1999 en Guadalajara")
        assertFalse(limpio.contains("1999"))
        assertTrue(limpio.contains("Guadalajara"))
        assertEquals(TipoDato.NACIMIENTO, datos.single().tipo)
    }

    @Test
    fun `estado civil edad y direccion se avisan sin borrar nada`() {
        val avisos = ProteccionDatos.detectar(
            "Soltera, tengo 23 años. Vivo en Calle Morelos 45, C.P. 44100",
        ).map { it.tipo }.toSet()

        assertEquals(setOf(TipoDato.ESTADO_CIVIL, TipoDato.EDAD, TipoDato.DIRECCION), avisos)
    }

    @Test
    fun `trabajar en la calle no es una direccion`() {
        assertTrue(ProteccionDatos.detectar("Vendí en la calle por 2 años y atendí a 30 clientes al día").isEmpty())
    }

    @Test
    fun `las respuestas se preparan campo por campo y dicen que se quito`() {
        val (limpias, avisos) = ProteccionDatos.prepararRespuestas(
            mapOf(
                "nombre" to "Ana López",
                "contacto" to "ana.lopez@gmail.com, CURP LOPA950312MJCPRN09",
            ),
        )
        assertEquals("Ana López", limpias["nombre"])
        assertFalse(limpias.getValue("contacto").contains("LOPA"))
        assertTrue(limpias.getValue("contacto").contains("ana.lopez@gmail.com"))
        assertEquals("contacto", avisos.single().donde)
    }

    // ---------- Inventos ----------

    private val respuestas = mapOf(
        "experiencia" to "Practicante en Manufacturas del Valle de enero a junio 2025. " +
            "Medí tiempos de la línea de empaque. El tiempo de ciclo bajó como 12%.",
        "proyectos" to "Control de inventario para la papelería de mi tío, como 120 productos. " +
            "El desabasto pasó de 8 casos al mes a 2. Capacité a tres personas.",
        "habilidades" to "Excel, Power BI, atención a clientes",
        "idiomas" to "Inglés B2",
        "formacion" to "Ingeniería Industrial, UdeG, 2020 a 2025",
    )

    private fun cvCon(logros: List<String>, habilidades: List<String> = emptyList()) = Cv(
        idioma = "es",
        datos = Datos("Ana", "Analista"),
        resumen = "",
        experiencia = listOf(Experiencia("Practicante", "Manufacturas del Valle", "Ene 2025 – Jun 2025", logros)),
        formacion = listOf(Formacion("Ingeniería Industrial", "UdeG", "2020 – 2025")),
        habilidades = habilidades,
        idiomas = listOf(IdiomaNivel("Inglés", "B2")),
    )

    @Test
    fun `las cifras que la persona dijo pasan, aunque las haya escrito con letra`() {
        val cv = cvCon(
            listOf(
                "Reduje el tiempo de ciclo 12%.",
                "Organicé un catálogo de 120 productos.",
                "Bajé el desabasto de 8 a 2 casos al mes.",
                "Capacité a 3 personas.",
            ),
        )
        assertTrue(VerificadorCv.cifrasSinRespaldo(cv, respuestas).isEmpty())
    }

    @Test
    fun `un porcentaje calculado por la IA se marca aunque la cuenta este bien`() {
        // De 8 a 2 es bajar 75%, pero la persona nunca dijo 75: no lo podría defender.
        val sospechas = VerificadorCv.cifrasSinRespaldo(cvCon(listOf("Reduje el desabasto 75%.")), respuestas)
        assertEquals("75", sospechas.single().dato)
        assertTrue(sospechas.single().donde.startsWith("Experiencia"))
    }

    @Test
    fun `una cifra inventada en el resumen se marca`() {
        val cv = cvCon(emptyList()).copy(resumen = "Ingeniera con 4 años de experiencia en mejora continua.")
        assertEquals("4", VerificadorCv.cifrasSinRespaldo(cv, respuestas).single().dato)
    }

    @Test
    fun `limite conocido - una cifra dicha en otro contexto la respalda`() {
        // La persona dijo "capacité a tres personas"; "3 años de experiencia" pasa sin aviso.
        // El verificador compara cifras, no significados: es una red, no un detector perfecto.
        // Si algún día se vuelve más listo, esta prueba debe cambiar a propósito.
        val cv = cvCon(emptyList()).copy(resumen = "Ingeniera con 3 años de experiencia.")
        assertTrue(VerificadorCv.cifrasSinRespaldo(cv, respuestas).isEmpty())
    }

    @Test
    fun `el nivel de idioma B2 no cuenta como cifra`() {
        val cv = cvCon(emptyList()).copy(resumen = "Inglés B2, puedo sostener una junta.")
        assertTrue(VerificadorCv.cifrasSinRespaldo(cv, respuestas).isEmpty())
    }

    @Test
    fun `las habilidades con raiz en lo que se dijo pasan y las agregadas no`() {
        val cv = cvCon(
            emptyList(),
            habilidades = listOf("Excel avanzado", "Power BI", "Atención a clientes", "Trabajo en equipo", "SAP"),
        )
        val sospechosas = VerificadorCv.habilidadesSinRespaldo(cv, respuestas).map { it.dato }
        assertEquals(listOf("Trabajo en equipo", "SAP"), sospechosas)
    }

    @Test
    fun `sin respuestas no se acusa a nadie`() {
        assertTrue(VerificadorCv.revisar(cvDeMuestra(), emptyMap()).isEmpty())
    }

    // ---------- Fraudes ----------

    @Test
    fun `una vacante que cobra capacitacion y pide INE se marca`() {
        val senales = DetectorEstafas.revisar(
            "¡Urgente! Gana hasta $5,000 a la semana desde casa. Sin experiencia. " +
                "Solo cubre tu pago de capacitación de $350 y envía tu INE por WhatsApp.",
        )
        assertTrue(senales.size >= 3)
        assertTrue(senales.any { it.explicacion.contains("pagar") })
        assertTrue(senales.any { it.explicacion.contains("documentos") })
        assertTrue(senales.any { it.fragmento.contains("capacitación") })
    }

    @Test
    fun `el multinivel se reconoce`() {
        val senales = DetectorEstafas.revisar("Forma parte de nuestra red de mercadeo e invita a tus amigos.")
        assertTrue(senales.any { it.explicacion.contains("multinivel") })
    }

    @Test
    fun `una vacante normal no levanta ninguna alerta`() {
        val senales = DetectorEstafas.revisar(
            """
            Analista de Datos Jr. — Grupo Industrial del Bajío, León, Gto.
            Requisitos: Ingeniería Industrial o afín, Excel avanzado, SQL básico, inglés intermedio.
            Ofrecemos: sueldo competitivo, prestaciones de ley, seguro de gastos médicos.
            Envía tu CV a reclutamiento@gib.com.mx con el asunto "Analista Jr".
            """.trimIndent(),
        )
        assertTrue(senales.toString(), senales.isEmpty())
    }
}
