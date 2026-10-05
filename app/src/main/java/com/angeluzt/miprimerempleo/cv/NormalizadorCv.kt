package com.angeluzt.miprimerempleo.cv

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.Json

/**
 * Traduce lo que la IA devolvió a nuestros modelos, aguantando que no respete el esquema.
 *
 * Un modelo de lenguaje no es una API: el mismo prompt a veces contesta
 * `"logros": ["texto"]` y a veces `"logros": [{"accion": "texto"}]`. Con
 * `decodeFromString` eso revienta y la persona pierde una generación que pagó,
 * además de diez minutos contestando preguntas. Aquí se lee el JSON campo por campo
 * y se acepta cualquier forma razonable: objeto donde esperábamos texto, número donde
 * esperábamos cadena, una sección ausente, nombres de campo en inglés.
 *
 * Reglas: nunca inventa contenido. Si un dato no viene, queda vacío. Lo único que
 * rellena son los datos de contacto, y solo copiándolos de lo que la propia persona
 * escribió en la entrevista.
 */
object NormalizadorCv {

    /** Convierte la respuesta cruda del modelo en el par español/inglés. */
    fun aParCv(crudo: String, respuestas: Map<String, String> = emptyMap()): ParCv {
        val raiz = objetoRaiz(crudo)
        val contenedor = when {
            raiz.containsKey("es") || raiz.containsKey("en") -> raiz
            else -> raiz.values.filterIsInstance<JsonObject>()
                .firstOrNull { it.containsKey("es") || it.containsKey("en") } ?: raiz
        }

        val crudoEs = contenedor["es"] ?: contenedor["español"] ?: contenedor["spanish"]
        val crudoEn = contenedor["en"] ?: contenedor["ingles"] ?: contenedor["english"]

        // El idioma lo manda la ranura, no lo que el modelo escribió en "idioma":
        // cuando solo devuelve una versión, la reusamos y hay que etiquetarla bien.
        val es = completar(aCv(crudoEs ?: contenedor), respuestas).copy(idioma = "es")
        val en = completar(aCv(crudoEn ?: crudoEs ?: contenedor), respuestas).copy(idioma = "en")

        if (es.datos.nombre.isBlank() && es.resumen.isBlank() && es.experiencia.isEmpty()) {
            error("La IA devolvió una respuesta que no pudimos leer. Inténtalo otra vez.")
        }
        return ParCv(es, en)
    }

    /** Convierte la revisión cruda del modelo en [RevisionCv]. */
    fun aRevision(crudo: String): RevisionCv {
        val o = objetoRaiz(crudo)
        val extension = texto(o["extension"] ?: o["extensión"]).lowercase()
        return RevisionCv(
            puntaje = entero(o["puntaje"] ?: o["score"]).coerceIn(0, 100),
            extension = when {
                extension.startsWith("cort") -> Extension.CORTO
                extension.startsWith("larg") -> Extension.LARGO
                else -> Extension.BIEN
            },
            palabras = entero(o["palabras"]),
            veredicto = texto(o["veredicto"] ?: o["resumen"]),
            correoSirve = booleano(o["correoSirve"] ?: o["correo_sirve"], porDefecto = true),
            notaCorreo = texto(o["notaCorreo"] ?: o["nota_correo"]),
            correoSugerido = texto(o["correoSugerido"] ?: o["correo_sugerido"]),
            fuertes = textos(o["fuertes"] ?: o["fortalezas"]),
            faltantes = lista(
                o["faltantes"],
                { f ->
                    Falta(
                        campo = texto(f["campo"] ?: f["campoId"] ?: f["seccion"]),
                        porque = texto(f["porque"] ?: f["porqué"] ?: f["motivo"]),
                        pregunta = texto(f["pregunta"]),
                    )
                },
                { t -> Falta(campo = "", porque = t, pregunta = "") },
            ).filter { it.porque.isNotBlank() || it.pregunta.isNotBlank() },
            arreglos = textos(o["arreglos"] ?: o["mejoras"]),
        )
    }

    /**
     * La adaptación a una vacante. Si el modelo no devolvió un CV legible se conserva el
     * original: el análisis sigue sirviendo aunque el CV no haya cambiado.
     */
    fun aAdaptacion(crudo: String, original: Cv, vacante: String, id: String, ahora: Long): Adaptacion {
        val o = objetoRaiz(crudo)
        val idioma = if (texto(o["idioma"]).lowercase().startsWith("en")) "en" else "es"
        val adaptado = (o["cv"] ?: o["cvAdaptado"])
            ?.let { aCv(it) }
            ?.takeIf { it.resumen.isNotBlank() || it.experiencia.isNotEmpty() || it.proyectos.isNotEmpty() }

        val cv = if (adaptado == null) {
            original
        } else {
            // Nombre, teléfono y correo no se adaptan: si el modelo los tocó, valen los de la persona.
            // El puesto sí puede tomar el nombre de la vacante: es al que se está postulando.
            adaptado.copy(
                idioma = idioma,
                datos = original.datos.copy(puesto = adaptado.datos.puesto.ifBlank { original.datos.puesto }),
            )
        }

        return Adaptacion(
            id = id,
            creada = ahora,
            puesto = texto(o["puesto"]),
            empresa = texto(o["empresa"]),
            idioma = cv.idioma,
            coincidencia = entero(o["coincidencia"]).coerceIn(0, 100),
            cubres = lista(
                o["cubres"],
                { r -> Requisito(texto(r["requisito"]), texto(r["evidencia"] ?: r["detalle"])) },
                { t -> Requisito(t) },
            ).filter { it.requisito.isNotBlank() },
            teFalta = lista(
                o["teFalta"] ?: o["faltan"],
                { r ->
                    Requisito(
                        requisito = texto(r["requisito"]),
                        detalle = texto(r["comoCubrirlo"] ?: r["detalle"]),
                        indispensable = booleano(r["indispensable"], porDefecto = false),
                    )
                },
                { t -> Requisito(t) },
            ).filter { it.requisito.isNotBlank() },
            palabrasClave = textos(o["palabrasClave"]),
            alertas = textos(o["alertas"]),
            mensaje = firmar(texto(o["mensaje"]), original.datos.nombre),
            cv = cv,
            vacante = vacante.take(4000),
        )
    }

    // ---------- CV ----------

    private fun aCv(crudo: JsonElement?): Cv {
        val o = crudo as? JsonObject ?: JsonObject(emptyMap())
        return Cv(
            idioma = "es",
            // Algunos modelos aplanan los datos en la raíz en vez de anidarlos.
            datos = aDatos(o["datos"] ?: o["contacto"] ?: o),
            resumen = texto(o["resumen"] ?: o["perfil"] ?: o["summary"] ?: o["profile"]),
            proyectos = lista(
                o["proyectos"] ?: o["projects"],
                { p ->
                    Proyecto(
                        nombre = texto(p["nombre"] ?: p["titulo"] ?: p["name"] ?: p["title"]),
                        descripcion = texto(p["descripcion"] ?: p["description"] ?: p["detalle"]),
                        enlace = texto(p["enlace"] ?: p["link"] ?: p["url"]),
                        logros = textos(p["logros"] ?: p["achievements"] ?: p["resultados"]),
                    )
                },
                { t -> Proyecto(nombre = t, descripcion = "") },
            ).filter { it.nombre.isNotBlank() || it.descripcion.isNotBlank() || it.logros.isNotEmpty() },
            experiencia = lista(
                o["experiencia"] ?: o["experience"],
                { e ->
                    Experiencia(
                        puesto = texto(e["puesto"] ?: e["cargo"] ?: e["title"] ?: e["position"]),
                        organizacion = texto(
                            e["organizacion"] ?: e["organización"] ?: e["empresa"]
                                ?: e["company"] ?: e["organization"]
                        ),
                        periodo = texto(e["periodo"] ?: e["período"] ?: e["fechas"] ?: e["dates"]),
                        logros = textos(e["logros"] ?: e["achievements"] ?: e["responsabilidades"]),
                    )
                },
                { t -> Experiencia(puesto = t, organizacion = "", periodo = "") },
            ).filter { it.puesto.isNotBlank() || it.organizacion.isNotBlank() || it.logros.isNotEmpty() },
            formacion = lista(
                o["formacion"] ?: o["formación"] ?: o["educacion"] ?: o["education"],
                { f ->
                    Formacion(
                        titulo = texto(f["titulo"] ?: f["título"] ?: f["degree"] ?: f["carrera"]),
                        institucion = texto(
                            f["institucion"] ?: f["institución"] ?: f["escuela"]
                                ?: f["institution"] ?: f["school"]
                        ),
                        periodo = texto(f["periodo"] ?: f["período"] ?: f["fechas"] ?: f["dates"]),
                        nota = texto(f["nota"] ?: f["detalle"] ?: f["note"]),
                    )
                },
                { t -> Formacion(titulo = t, institucion = "", periodo = "") },
            ).filter { it.titulo.isNotBlank() || it.institucion.isNotBlank() },
            certificaciones = lista(
                o["certificaciones"] ?: o["certifications"] ?: o["cursos"],
                { c ->
                    Certificacion(
                        nombre = texto(c["nombre"] ?: c["name"] ?: c["curso"] ?: c["titulo"]),
                        institucion = texto(
                            c["institucion"] ?: c["institución"] ?: c["issuer"]
                                ?: c["organizacion"] ?: c["emisor"]
                        ),
                        anio = texto(c["anio"] ?: c["año"] ?: c["year"] ?: c["fecha"]),
                        enlace = texto(c["enlace"] ?: c["link"] ?: c["url"]),
                    )
                },
                { t -> Certificacion(nombre = t, institucion = "", anio = "") },
            ).filter { it.nombre.isNotBlank() },
            // Repetidas no suman: "Excel" y "excel" ocupan dos renglones de una sola hoja.
            habilidades = textos(o["habilidades"] ?: o["skills"], separar = true).distinctBy { it.lowercase() },
            idiomas = lista(
                o["idiomas"] ?: o["languages"],
                { i -> aIdioma(i) },
                { t -> partirIdioma(t) },
            ).filter { it.idioma.isNotBlank() },
        )
    }

    private fun aDatos(crudo: JsonElement?): Datos {
        val o = crudo as? JsonObject ?: JsonObject(emptyMap())
        return Datos(
            nombre = texto(o["nombre"] ?: o["name"] ?: o["nombreCompleto"]),
            puesto = texto(o["puesto"] ?: o["title"] ?: o["cargo"] ?: o["puestoBuscado"]),
            ciudad = texto(o["ciudad"] ?: o["city"] ?: o["ubicacion"] ?: o["location"]),
            telefono = texto(o["telefono"] ?: o["teléfono"] ?: o["phone"] ?: o["celular"]),
            correo = texto(o["correo"] ?: o["email"] ?: o["mail"]),
            linkedin = texto(o["linkedin"] ?: o["linkedIn"]),
            portafolio = texto(
                o["portafolio"] ?: o["portfolio"] ?: o["github"] ?: o["sitio"] ?: o["web"]
            ),
        )
    }

    private fun aIdioma(o: JsonObject): IdiomaNivel {
        val nombre = texto(o["idioma"] ?: o["language"] ?: o["nombre"])
        val nivel = texto(o["nivel"] ?: o["level"] ?: o["dominio"])
        // Forma {"Inglés": "B2"}: la llave es el idioma y el valor el nivel.
        if (nombre.isBlank() && o.size == 1) {
            return IdiomaNivel(o.keys.first().trim(), texto(o.values.first()))
        }
        return IdiomaNivel(nombre, nivel)
    }

    /** "Inglés: B2" o "Inglés B2" vienen como una sola cadena más veces de las que uno cree. */
    private fun partirIdioma(entrada: String): IdiomaNivel {
        val corte = entrada.indexOfFirst { it == ':' || it == '-' || it == '–' }
        if (corte > 0) {
            return IdiomaNivel(
                entrada.take(corte).trim(),
                entrada.drop(corte + 1).trim(),
            )
        }
        val espacio = entrada.indexOf(' ')
        return if (espacio > 0) {
            IdiomaNivel(entrada.take(espacio).trim(), entrada.drop(espacio + 1).trim())
        } else {
            IdiomaNivel(entrada.trim(), "")
        }
    }

    /**
     * Los datos de contacto se le ocultan a la IA (no los necesita para redactar) y aquí se
     * ponen de vuelta, tal como los escribió la persona. Se imponen sobre lo que haya devuelto
     * el modelo: un teléfono o un correo no se "redactan", se copian.
     */
    fun conContactoReal(par: ParCv, respuestas: Map<String, String>): ParCv {
        val contacto = respuestas["contacto"].orEmpty()
        val enlaces = respuestas["enlaces"].orEmpty()
        val nombre = nombrePropio(respuestas["nombre"].orEmpty())
        // Si la persona no lo dijo en la entrevista vale lo del modelo (lo pudo leer de un CV
        // pegado), salvo que sea una de nuestras marcas: esas nunca llegan al CV.
        fun respaldo(delModelo: String) = delModelo.takeUnless { it.trimStart().startsWith("[") }.orEmpty()
        fun Cv.real() = copy(
            datos = datos.copy(
                nombre = nombre.ifBlank { respaldo(datos.nombre) },
                telefono = telefonoEn(contacto).ifBlank { respaldo(datos.telefono) },
                correo = correoEn(contacto).ifBlank { respaldo(datos.correo) },
                linkedin = enlaceCon(enlaces, "linkedin").ifBlank { respaldo(datos.linkedin) },
                portafolio = otroEnlace(enlaces).ifBlank { respaldo(datos.portafolio) },
            ),
        )
        return ParCv(par.es.real(), par.en.real())
    }

    /**
     * El mensaje para postularse, con el nombre de la persona. La IA no lo conoce (no se lo
     * mandamos), así que lo pone la app: donde el modelo dejó la marca y como firma al final.
     * Las líneas de plantilla que a veces deja ("[Tu nombre]", "[Teléfono]") se quitan.
     */
    fun firmar(mensaje: String, nombre: String): String {
        val firma = nombre.trim().takeUnless { it.startsWith("[") }.orEmpty()
        val limpio = mensaje
            .replace(MARCA_EN_TEXTO, if (firma.isBlank()) "" else " $firma")
            .lines()
            .map { it.trim() }
            .filterNot { PLANTILLA.matches(it) }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
        if (limpio.isBlank() || firma.isBlank() || limpio.contains(firma)) return limpio
        return limpio + (if (limpio.endsWith(",")) "\n" else "\n\n") + firma
    }

    private val MARCA_EN_TEXTO = Regex("[ \\t]*" + Regex.escape(ProteccionDatos.OMITIDO))
    private val PLANTILLA = Regex("\\[[^\\]\\n]{1,40}\\]")

    /** "ana lópez de la cruz" → "Ana López de la Cruz". Si ya trae mayúsculas, se respeta. */
    fun nombrePropio(nombre: String): String {
        val limpio = nombre.trim().replace(Regex("\\s+"), " ")
        if (limpio.any { it.isUpperCase() }) return limpio
        val particulas = setOf("de", "del", "la", "las", "los", "y")
        return limpio.split(' ').mapIndexed { i, palabra ->
            if (i > 0 && palabra in particulas) palabra else palabra.replaceFirstChar { it.uppercaseChar() }
        }.joinToString(" ")
    }

    /**
     * Rellena los datos de contacto que la IA se haya saltado, copiándolos tal cual de
     * las respuestas de la entrevista. No es inventar: son palabras de la propia persona.
     */
    private fun completar(cv: Cv, respuestas: Map<String, String>): Cv {
        if (respuestas.isEmpty()) return cv
        val contacto = respuestas["contacto"].orEmpty()
        val enlaces = respuestas["enlaces"].orEmpty()
        val d = cv.datos
        return cv.copy(
            datos = d.copy(
                nombre = d.nombre.ifBlank { respuestas["nombre"].orEmpty().trim() },
                puesto = d.puesto.ifBlank { respuestas["puesto_buscado"].orEmpty().trim() },
                ciudad = d.ciudad.ifBlank { respuestas["ciudad"].orEmpty().trim() },
                correo = d.correo.ifBlank { correoEn(contacto) },
                telefono = d.telefono.ifBlank { telefonoEn(contacto) },
                linkedin = d.linkedin.ifBlank { enlaceCon(enlaces, "linkedin") },
                portafolio = d.portafolio.ifBlank { otroEnlace(enlaces) },
            ),
        )
    }

    private val CORREO = Regex("[\\w.+-]+@[\\w-]+\\.[\\w.-]+")
    private val TELEFONO = Regex("[+(]?\\d[\\d\\s()+-]{6,}\\d")

    private fun correoEn(texto: String) = CORREO.find(texto)?.value.orEmpty()

    private fun telefonoEn(texto: String): String =
        TELEFONO.find(CORREO.replace(texto, " "))?.value?.trim().orEmpty()

    private fun enlaceCon(texto: String, aguja: String): String =
        trozos(texto).firstOrNull { it.contains(aguja, ignoreCase = true) }.orEmpty()

    private fun otroEnlace(texto: String): String =
        trozos(texto).firstOrNull {
            it.contains('.') && !it.contains("linkedin", true) && !it.contains('@')
        }.orEmpty()

    private fun trozos(texto: String): List<String> =
        texto.split(' ', ',', ';', '\n').map { it.trim().trimEnd('.', ')') }.filter { it.isNotEmpty() }

    // ---------- Lectura tolerante de JSON ----------

    /** Aísla el objeto JSON aunque venga envuelto en ``` o con texto alrededor. */
    private fun objetoRaiz(crudo: String): JsonObject {
        val inicio = crudo.indexOf('{')
        val fin = crudo.lastIndexOf('}')
        if (inicio < 0 || fin <= inicio) return JsonObject(emptyMap())
        return runCatching {
            Json.parseToJsonElement(crudo.substring(inicio, fin + 1)) as? JsonObject
        }.getOrNull() ?: JsonObject(emptyMap())
    }

    private fun texto(valor: JsonElement?): String = when {
        valor == null || valor is JsonNull -> ""
        valor is JsonPrimitive -> valor.content.trim()
        valor is JsonArray -> valor.map { texto(it) }.filter { it.isNotEmpty() }.joinToString(", ")
        valor is JsonObject -> unir(valor.values.map { texto(it) })
        else -> ""
    }

    /** Un objeto donde se esperaba una frase: se pegan sus textos en vez de perderlos. */
    private fun unir(partes: List<String>): String {
        val limpias = partes.map { it.trim() }.filter { it.isNotEmpty() }
        if (limpias.size <= 1) return limpias.firstOrNull().orEmpty()
        return limpias.joinToString(". ") { it.trimEnd('.', ' ') }
    }

    private fun textos(valor: JsonElement?, separar: Boolean = false): List<String> {
        val crudos = when {
            valor == null || valor is JsonNull -> emptyList()
            valor is JsonArray -> valor.map { texto(it) }
            valor is JsonObject -> valor.values.map { texto(it) }
            else -> listOf(texto(valor))
        }
        val partidos = if (separar) crudos.flatMap { it.split(',', ';', '\n') } else crudos
        return partidos.map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun <T> lista(
        valor: JsonElement?,
        desdeObjeto: (JsonObject) -> T,
        desdeTexto: (String) -> T,
    ): List<T> = when {
        valor == null || valor is JsonNull -> emptyList()
        valor is JsonArray -> valor.mapNotNull { elemento ->
            when {
                elemento is JsonNull -> null
                elemento is JsonObject -> desdeObjeto(elemento)
                else -> texto(elemento).takeIf { it.isNotBlank() }?.let(desdeTexto)
            }
        }
        valor is JsonObject -> listOf(desdeObjeto(valor))
        else -> texto(valor).takeIf { it.isNotBlank() }?.let { listOf(desdeTexto(it)) } ?: emptyList()
    }

    private fun entero(valor: JsonElement?): Int =
        texto(valor).filter { it.isDigit() }.toIntOrNull() ?: 0

    private fun booleano(valor: JsonElement?, porDefecto: Boolean): Boolean =
        when (texto(valor).lowercase()) {
            "true", "sí", "si", "yes", "1" -> true
            "false", "no", "0" -> false
            else -> porDefecto
        }
}
