package com.angeluzt.miprimerempleo.cv

/**
 * Lo que una persona escribió en la entrevista y que no debe salir del teléfono.
 *
 * Un CV pasa por muchas manos: reclutadores, bolsas de trabajo, correos reenviados. Con un CURP,
 * un RFC o un DNI alguien puede abrir cuentas, pedir créditos o suplantar a la persona; el
 * módulo de estafas de la guía lo explica. Aquí no se confía en que la IA los omita: se tachan
 * ANTES de mandar nada a OpenAI, así que ni siquiera llegan a un servidor ajeno.
 *
 * Solo se tacha lo inequívoco (un CURP tiene una forma que nada más tiene). Lo que depende de
 * contexto, como el estado civil o una dirección, se detecta y se avisa, pero no se borra:
 * quitar palabras de una frase a ciegas cambia lo que la persona quiso decir.
 */
enum class TipoDato(val nombre: String, val porQue: String) {
    CURP("CURP", "Con tu CURP pueden suplantarte en trámites."),
    RFC("RFC", "Tu RFC sirve para facturar a tu nombre."),
    NSS("número de seguro social", "Tu NSS da acceso a tu historial laboral."),
    RUT("RUT", "Con tu RUT pueden hacer trámites a tu nombre."),
    DNI("DNI", "Con tu DNI pueden abrir cuentas a tu nombre."),
    CEDULA("cédula", "Con tu cédula pueden hacer trámites a tu nombre."),
    PASAPORTE("pasaporte", "Tu pasaporte no tiene nada que hacer en un CV."),
    NACIMIENTO("fecha de nacimiento", "Tu fecha de nacimiento abre la puerta a descartarte por edad."),
    ESTADO_CIVIL("estado civil", "Tu estado civil no le importa a nadie que te va a contratar."),
    EDAD("edad", "Tu edad abre la puerta a descartarte por ella."),
    DIRECCION("dirección exacta", "Basta con la ciudad: tu dirección expone dónde vives."),
}

data class DatoSensible(
    val tipo: TipoDato,
    /** Dónde apareció, para decírselo a la persona: "contacto", "resumen"... */
    val donde: String,
    /** Si ya se quitó antes de mandarlo a la IA, o solo se detectó. */
    val tachado: Boolean,
)

object ProteccionDatos {

    const val TACHADO = "[dato personal omitido]"

    private val opciones = setOf(RegexOption.IGNORE_CASE)

    // El primer bloque de un CURP es igual al de un RFC, así que el CURP se busca primero.
    private val CURP = Regex(
        "\\b[A-Z][AEIOUX][A-Z]{2}\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])[HMX][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z\\d]\\d\\b",
        opciones,
    )
    private val RFC = Regex(
        "\\b[A-ZÑ&]{4}\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])[A-Z\\d]{2}[A\\d]\\b",
        opciones,
    )
    private val RUT = Regex("\\b\\d{1,2}\\.\\d{3}\\.\\d{3}-[\\dK]\\b|\\b\\d{7,8}-[\\dK]\\b", opciones)

    // Estos solo se reconocen por la palabra que los presenta: un número suelto de 8 dígitos
    // también puede ser un teléfono, y borrar un teléfono sería peor que no hacer nada.
    private val NSS = Regex("(\\bNSS\\b|seguro social|\\bIMSS\\b)\\D{0,15}(\\d[\\d -]{9,13}\\d)", opciones)
    private val DNI = Regex("(\\bD\\.?N\\.?I\\.?)\\D{0,10}(\\d{1,2}\\.?\\d{3}\\.?\\d{3})\\b", opciones)
    private val CEDULA = Regex("(c[ée]dula|\\bC\\.?C\\.?)(\\s+de\\s+ciudadan[ií]a)?\\D{0,10}(\\d[\\d.]{5,13}\\d)\\b", opciones)
    private val PASAPORTE = Regex("(pasaporte|passport)\\W{0,10}([A-Z]{0,3}\\d{6,9})\\b", opciones)
    private val NACIMIENTO = Regex(
        "(fecha de nacimiento|nac[ií] el|naci[oó] el|f\\.?\\s?nac\\.?)\\W{0,5}" +
            "(\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}|\\d{1,2} de \\w+ (de|del) \\d{4})",
        opciones,
    )

    private val ESTADO_CIVIL = Regex(
        "estado civil|\\b(solter[oa]|casad[oa]|divorciad[oa]|viud[oa]|en uni[oó]n libre)\\b",
        opciones,
    )
    private val EDAD = Regex("\\b\\d{2} años de edad\\b|\\btengo \\d{2} años\\b|\\bedad:?\\s*\\d{2}\\b", opciones)
    // Sin IGNORE_CASE a propósito: el nombre de la calle va con mayúscula. Así "Calle Morelos 45"
    // cuenta como dirección y "vendí en la calle por 2 años" no.
    private val DIRECCION = Regex(
        "\\b([Cc]alle|[Aa]venida|[Aa]v\\.)\\s+[A-ZÁÉÍÓÚÑ0-9][\\wÁÉÍÓÚÑáéíóúñ.]*" +
            "(\\s+[A-ZÁÉÍÓÚÑ][\\wÁÉÍÓÚÑáéíóúñ.]*){0,3}\\s+(#\\s?|[Nn][oºo°.]*\\s?)?\\d{1,5}\\b" +
            "|\\b([Mm]z\\.?|[Mm]anzana|[Ll]ote|[Ll]t\\.)\\s*\\d" +
            "|\\b[Cc]\\.\\s?[Pp]\\.?\\s*\\d{4,5}\\b|\\bCP\\s*\\d{5}\\b" +
            "|[Cc][óo]digo [Pp]ostal\\W{0,3}\\d{4,5}",
    )

    /** Quita los identificadores de un texto y dice cuáles había. */
    fun tachar(texto: String, donde: String = ""): Pair<String, List<DatoSensible>> {
        var limpio = texto
        val encontrados = mutableListOf<DatoSensible>()

        fun quitar(patron: Regex, tipo: TipoDato, grupo: Int = 0) {
            limpio = patron.replace(limpio) { coincidencia ->
                encontrados += DatoSensible(tipo, donde, tachado = true)
                if (grupo == 0) {
                    TACHADO
                } else {
                    val valor = coincidencia.groups[grupo]?.value ?: return@replace coincidencia.value
                    coincidencia.value.replace(valor, TACHADO)
                }
            }
        }

        quitar(CURP, TipoDato.CURP)
        quitar(RFC, TipoDato.RFC)
        quitar(RUT, TipoDato.RUT)
        quitar(NSS, TipoDato.NSS, grupo = 2)
        quitar(DNI, TipoDato.DNI, grupo = 2)
        quitar(CEDULA, TipoDato.CEDULA, grupo = 3)
        quitar(PASAPORTE, TipoDato.PASAPORTE, grupo = 2)
        quitar(NACIMIENTO, TipoDato.NACIMIENTO, grupo = 2)

        return limpio to encontrados.distinctBy { it.tipo }
    }

    /** Lo que no se tacha porque depende del contexto, pero conviene avisar. */
    fun detectar(texto: String, donde: String = ""): List<DatoSensible> = buildList {
        if (ESTADO_CIVIL.containsMatchIn(texto)) add(DatoSensible(TipoDato.ESTADO_CIVIL, donde, false))
        if (EDAD.containsMatchIn(texto)) add(DatoSensible(TipoDato.EDAD, donde, false))
        if (DIRECCION.containsMatchIn(texto)) add(DatoSensible(TipoDato.DIRECCION, donde, false))
    }

    /** Prepara las respuestas para mandarlas a la IA: tachadas, y con la lista de lo que se quitó. */
    fun prepararRespuestas(respuestas: Map<String, String>): Pair<Map<String, String>, List<DatoSensible>> {
        val avisos = mutableListOf<DatoSensible>()
        val limpias = respuestas.mapValues { (campo, valor) ->
            val (texto, tachados) = tachar(valor, campo)
            avisos += tachados
            avisos += detectar(texto, campo)
            texto
        }
        return limpias to avisos.distinctBy { it.tipo }
    }

    // ---------- Minimización ----------
    //
    // La IA no necesita saber cómo se llama la persona, ni su teléfono, ni su correo, ni sus
    // enlaces para redactar un CV: esos datos se copian tal cual y la app los pone después
    // (NormalizadorCv.conContactoReal). Así no salen del teléfono. La marca le dice al modelo
    // que el dato existe, para que no lo reporte como faltante.

    const val OMITIDO = "[omitido]"

    /** Respuestas de la entrevista sin los datos de contacto, para generar el CV. */
    fun sinContacto(respuestas: Map<String, String>): Map<String, String> =
        respuestas.mapValues { (campo, valor) ->
            if (campo in CAMPOS_DE_CONTACTO && valor.isNotBlank()) OMITIDO else valor
        }

    /**
     * El CV para revisarlo. Nombre, correo y enlaces sí van: revisar si el correo se ve
     * profesional y si los enlaces están completos es parte de lo que la persona pidió, y
     * los enlaces de un CV son públicos de todos modos. El teléfono no le sirve a la revisión.
     */
    fun paraRevisar(cv: Cv): Cv = cv.copy(
        datos = cv.datos.copy(telefono = marcar(cv.datos.telefono)),
    )

    /** El CV para adaptarlo a una vacante: ningún dato de contacto hace falta para eso. */
    fun paraAdaptar(cv: Cv): Cv = cv.copy(
        datos = cv.datos.copy(
            nombre = marcar(cv.datos.nombre),
            telefono = marcar(cv.datos.telefono),
            correo = marcar(cv.datos.correo),
            linkedin = marcar(cv.datos.linkedin),
            portafolio = marcar(cv.datos.portafolio),
        ),
    )

    private fun marcar(valor: String) = if (valor.isBlank()) "" else OMITIDO

    private val CAMPOS_DE_CONTACTO = setOf("nombre", "contacto", "enlaces")

    /** Revisa el CV ya redactado: por si algo se coló, o la persona lo escribió en el editor. */
    fun revisarCv(cv: Cv): List<DatoSensible> {
        val partes = buildList {
            add("datos" to with(cv.datos) { listOf(nombre, puesto, ciudad, telefono, correo, linkedin, portafolio).joinToString(" ") })
            add("resumen" to cv.resumen)
            cv.experiencia.forEach { add("experiencia" to (it.logros + it.puesto + it.organizacion).joinToString(" ")) }
            cv.proyectos.forEach { add("proyectos" to (it.logros + it.nombre + it.descripcion).joinToString(" ")) }
            cv.formacion.forEach { add("formacion" to listOf(it.titulo, it.institucion, it.nota).joinToString(" ")) }
        }
        return partes.flatMap { (donde, texto) -> tachar(texto, donde).second + detectar(texto, donde) }
            .distinctBy { it.tipo }
            // En el CV ya no se tacha nada: lo encontrado se avisa para que la persona lo quite.
            .map { it.copy(tachado = false) }
    }
}
