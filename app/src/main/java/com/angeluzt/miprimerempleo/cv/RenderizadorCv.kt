package com.angeluzt.miprimerempleo.cv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File

/**
 * Arma el PDF en el teléfono. La IA solo produce el contenido, nunca el archivo:
 * así cambiar de plantilla o editar un campo no cuesta otra generación.
 */
class RenderizadorCv(private val context: Context) {

    fun exportar(cv: Cv, plantilla: Plantilla, foto: Bitmap?, nombreArchivo: String): File {
        val documento = PdfDocument()
        Dibujo(documento, cv, plantilla, foto).ejecutar()

        val carpeta = File(context.cacheDir, "cv").apply { mkdirs() }
        val archivo = File(carpeta, "$nombreArchivo.pdf")
        archivo.outputStream().use { documento.writeTo(it) }
        documento.close()
        return archivo
    }
}

/**
 * Un dibujo por exportación. Antes el estado de la página vivía en el renderizador,
 * y dos vistas previas a la vez se pisaban la fuente activa.
 */
private class Dibujo(
    private val documento: PdfDocument,
    private val cv: Cv,
    private val plantilla: Plantilla,
    private val foto: Bitmap?,
) {

    private var pagina: PdfDocument.Page = abrirPagina(1)
    private var numeroDePagina = 1
    private var lienzo: Canvas = pagina.canvas

    /** Cursor vertical de la columna principal. */
    private var y = MARGEN

    private var xCuerpo = MARGEN
    private var anchoCuerpo = ANCHO - MARGEN * 2

    private val acento get() = plantilla.acento

    fun ejecutar() {
        when (plantilla.diseno.barra) {
            Barra.NINGUNA -> {
                xCuerpo = MARGEN
                anchoCuerpo = ANCHO - MARGEN * 2
            }
            Barra.IZQUIERDA -> {
                xCuerpo = BARRA_ANCHO + 26f
                anchoCuerpo = ANCHO - xCuerpo - MARGEN
            }
            Barra.DERECHA -> {
                xCuerpo = MARGEN
                anchoCuerpo = ANCHO - BARRA_ANCHO - 26f - MARGEN
            }
        }

        if (plantilla.diseno.barra == Barra.NINGUNA) {
            y = dibujarEncabezado()
        } else {
            dibujarBarraLateral()
            y = 44f
        }

        dibujarCuerpo()
        documento.finishPage(pagina)
    }

    // ---------- Encabezados ----------

    private fun dibujarEncabezado(): Float = when (plantilla.diseno.encabezado) {
        Encabezado.SIMPLE -> encabezadoSimple()
        Encabezado.BANDA -> encabezadoBanda()
        Encabezado.CENTRADO -> encabezadoCentrado()
        Encabezado.BLOQUE -> encabezadoBloque()
        Encabezado.MINIMAL -> encabezadoMinimal()
        Encabezado.LINEAS -> encabezadoLineas()
    }

    private fun encabezadoSimple(): Float {
        var cursor = MARGEN + 16f
        var ancho = anchoCuerpo
        if (usaFoto()) {
            val lado = 74f
            dibujarFoto(ANCHO - MARGEN - lado, MARGEN, lado, circular = false)
            ancho = anchoCuerpo - lado - 14f
        }
        lienzo.drawText(cv.datos.nombre, MARGEN, cursor, texto(19f, true, TINTA))
        cursor += 18f
        if (cv.datos.puesto.isNotBlank()) {
            lienzo.drawText(cv.datos.puesto, MARGEN, cursor, texto(12f, false, acento))
            cursor += 16f
        }
        lineasDeContacto().forEach {
            cursor = escribirEnvuelto(it, MARGEN, cursor, ancho, texto(8.5f, false, SUAVE))
        }
        return cursor + 10f
    }

    private fun encabezadoBanda(): Float {
        val alto = 116f
        lienzo.drawRect(0f, 0f, ANCHO.toFloat(), alto, relleno(acento))
        if (usaFoto()) {
            val lado = 72f
            dibujarFoto(ANCHO - MARGEN - lado, (alto - lado) / 2, lado, circular = true)
        }
        lienzo.drawText(cv.datos.nombre, MARGEN, 46f, texto(21f, true, Color.WHITE))
        if (cv.datos.puesto.isNotBlank()) {
            lienzo.drawText(cv.datos.puesto, MARGEN, 66f, texto(12f, false, aclarar(acento, 0.85f)))
        }
        var cursor = 88f
        lineasDeContacto().forEach {
            lienzo.drawText(it, MARGEN, cursor, texto(8.5f, false, aclarar(acento, 0.78f)))
            cursor += 13f
        }
        return alto + 26f
    }

    private fun encabezadoCentrado(): Float {
        val centro = ANCHO / 2f
        var cursor = MARGEN + 22f
        if (usaFoto()) {
            val lado = 76f
            dibujarFoto(centro - lado / 2, MARGEN, lado, circular = true)
            cursor = MARGEN + lado + 26f
        }
        lienzo.drawText(cv.datos.nombre, centro, cursor, centrado(texto(22f, true, TINTA)))
        cursor += 12f
        lienzo.drawLine(centro - 46f, cursor, centro + 46f, cursor, trazo(acento, 1.4f))
        cursor += 18f
        if (cv.datos.puesto.isNotBlank()) {
            lienzo.drawText(cv.datos.puesto, centro, cursor, centrado(texto(11.5f, false, acento)))
            cursor += 16f
        }
        lineasDeContacto().forEach {
            lienzo.drawText(it, centro, cursor, centrado(texto(8.5f, false, SUAVE)))
            cursor += 12f
        }
        return cursor + 12f
    }

    private fun encabezadoBloque(): Float {
        val alto = 112f
        lienzo.drawRect(0f, 0f, ANCHO.toFloat(), alto, relleno(TINTA))
        lienzo.drawRect(0f, alto - 5f, ANCHO.toFloat(), alto, relleno(acento))
        if (usaFoto()) {
            val lado = 70f
            dibujarFoto(ANCHO - MARGEN - lado, (alto - 5f - lado) / 2, lado, circular = true)
        }
        lienzo.drawText(cv.datos.nombre, MARGEN, 44f, texto(20f, true, Color.WHITE))
        if (cv.datos.puesto.isNotBlank()) {
            lienzo.drawText(
                cv.datos.puesto.uppercase(), MARGEN, 62f,
                espaciado(texto(9f, false, aclarar(acento, 0.6f)), 0.14f),
            )
        }
        var cursor = 84f
        lineasDeContacto().forEach {
            lienzo.drawText(it, MARGEN, cursor, texto(8.5f, false, Color.parseColor("#CBD5E1")))
            cursor += 12f
        }
        return alto + 26f
    }

    private fun encabezadoMinimal(): Float {
        var cursor = MARGEN + 34f
        lienzo.drawText(
            cv.datos.nombre.uppercase(), MARGEN, cursor,
            espaciado(texto(23f, true, TINTA), 0.06f),
        )
        cursor += 20f
        if (cv.datos.puesto.isNotBlank()) {
            lienzo.drawText(
                cv.datos.puesto.uppercase(), MARGEN, cursor,
                espaciado(texto(9.5f, false, SUAVE), 0.16f),
            )
            cursor += 20f
        }
        lienzo.drawLine(MARGEN, cursor, ANCHO - MARGEN, cursor, trazo(acento, 1.2f))
        cursor += 16f
        lineasDeContacto().forEach {
            cursor = escribirEnvuelto(it, MARGEN, cursor, anchoCuerpo, texto(8.5f, false, SUAVE))
        }
        return cursor + 18f
    }

    private fun encabezadoLineas(): Float {
        var cursor = MARGEN + 18f
        var ancho = anchoCuerpo
        if (usaFoto()) {
            val lado = 68f
            dibujarFoto(ANCHO - MARGEN - lado, MARGEN, lado, circular = true)
            ancho = anchoCuerpo - lado - 14f
        }
        lienzo.drawText(cv.datos.nombre, MARGEN, cursor, texto(20f, true, TINTA))
        cursor += 10f
        lienzo.drawLine(MARGEN, cursor, ANCHO - MARGEN, cursor, trazo(acento, 2f))
        cursor += 3.5f
        lienzo.drawLine(MARGEN, cursor, ANCHO - MARGEN, cursor, trazo(acento, 0.8f))
        cursor += 16f
        if (cv.datos.puesto.isNotBlank()) {
            lienzo.drawText(cv.datos.puesto, MARGEN, cursor, texto(11.5f, false, acento))
            cursor += 15f
        }
        lineasDeContacto().forEach {
            cursor = escribirEnvuelto(it, MARGEN, cursor, ancho, texto(8.5f, false, SUAVE))
        }
        return cursor + 12f
    }

    // ---------- Barra lateral ----------

    /**
     * La barra se pinta en cada página para que una segunda hoja no se vea partida.
     * Su contenido (contacto, habilidades, idiomas) solo va en la primera.
     */
    private fun pintarFondoDeBarra() {
        val izquierda = if (plantilla.diseno.barra == Barra.IZQUIERDA) 0f else ANCHO - BARRA_ANCHO
        lienzo.drawRect(izquierda, 0f, izquierda + BARRA_ANCHO, ALTO.toFloat(), relleno(acento))
    }

    private fun dibujarBarraLateral() {
        pintarFondoDeBarra()

        val origen = if (plantilla.diseno.barra == Barra.IZQUIERDA) 0f else ANCHO - BARRA_ANCHO
        val margen = origen + 22f
        val ancho = BARRA_ANCHO - 44f
        var cursor = 40f
        val tenue = Color.parseColor("#E2E8F0")

        if (usaFoto()) {
            val lado = 88f
            dibujarFoto(origen + (BARRA_ANCHO - lado) / 2, cursor, lado, circular = true)
            cursor += lado + 22f
        }

        cursor = escribirEnvuelto(cv.datos.nombre, margen, cursor, ancho, texto(16f, true, Color.WHITE))
        cursor += 4f
        if (cv.datos.puesto.isNotBlank()) {
            cursor = escribirEnvuelto(cv.datos.puesto, margen, cursor, ancho, texto(10f, false, tenue))
        }
        cursor += 18f

        cursor = tituloDeBarra(seccion("contacto"), margen, cursor, ancho)
        datosDeContacto().forEach {
            cursor = escribirEnvuelto(it, margen, cursor, ancho, texto(8.5f, false, tenue)) + 4f
        }

        if (cv.habilidades.isNotEmpty()) {
            cursor += 14f
            cursor = tituloDeBarra(seccion("habilidades"), margen, cursor, ancho)
            cv.habilidades.forEach {
                cursor = escribirEnvuelto("·  $it", margen, cursor, ancho, texto(8.5f, false, tenue)) + 3f
            }
        }

        if (cv.idiomas.isNotEmpty()) {
            cursor += 14f
            cursor = tituloDeBarra(seccion("idiomas"), margen, cursor, ancho)
            cv.idiomas.forEach {
                val linea = listOf(it.idioma, it.nivel).filter { parte -> parte.isNotBlank() }
                    .joinToString(": ")
                cursor = escribirEnvuelto(linea, margen, cursor, ancho, texto(8.5f, false, tenue)) + 3f
            }
        }
    }

    private fun tituloDeBarra(titulo: String, x: Float, y: Float, ancho: Float): Float {
        lienzo.drawText(titulo.uppercase(), x, y, texto(9f, true, Color.WHITE))
        val bajo = y + 4f
        lienzo.drawLine(x, bajo, x + ancho, bajo, trazo(Color.parseColor("#80FFFFFF"), 0.8f))
        return bajo + 13f
    }

    // ---------- Cuerpo ----------

    private fun dibujarCuerpo() {
        val hayBarra = plantilla.diseno.barra != Barra.NINGUNA
        val pCuerpo = texto(9.5f, false, CUERPO)
        val pCargo = texto(10f, true, TINTA)
        val pMeta = texto(8.5f, false, SUAVE)

        if (cv.resumen.isNotBlank()) {
            parrafo(cv.resumen, xCuerpo, anchoCuerpo, pCuerpo)
            y += 10f
        }

        if (cv.proyectos.isNotEmpty()) {
            tituloDeSeccion(seccion("proyectos"))
            cv.proyectos.forEach { p ->
                conRiel { x, ancho ->
                    reservar(34f)
                    lienzo.drawText(recortar(p.nombre, ancho, pCargo), x, y, pCargo)
                    y += 12f
                    if (p.enlace.isNotBlank()) {
                        parrafo(p.enlace, x, ancho, pMeta)
                        y += 1f
                    }
                    if (p.descripcion.isNotBlank()) parrafo(p.descripcion, x, ancho, pCuerpo)
                    dibujarVinetas(p.logros, x, ancho, pCuerpo)
                    y += 7f
                }
            }
        }

        if (cv.experiencia.isNotEmpty()) {
            tituloDeSeccion(seccion("experiencia"))
            cv.experiencia.forEach { e ->
                conRiel { x, ancho ->
                    reservar(34f)
                    lienzo.drawText(recortar(e.puesto, ancho, pCargo), x, y, pCargo)
                    y += 12f
                    val meta = listOf(e.organizacion, e.periodo).filter { it.isNotBlank() }
                        .joinToString("  ·  ")
                    if (meta.isNotBlank()) {
                        parrafo(meta, x, ancho, pMeta)
                        y += 2f
                    }
                    dibujarVinetas(e.logros, x, ancho, pCuerpo)
                    y += 7f
                }
            }
        }

        if (cv.formacion.isNotEmpty()) {
            tituloDeSeccion(seccion("formacion"))
            cv.formacion.forEach { f ->
                reservar(30f)
                lienzo.drawText(recortar(f.titulo, anchoCuerpo, pCargo), xCuerpo, y, pCargo)
                y += 12f
                val meta = listOf(f.institucion, f.periodo, f.nota).filter { it.isNotBlank() }
                    .joinToString("  ·  ")
                if (meta.isNotBlank()) parrafo(meta, xCuerpo, anchoCuerpo, pMeta)
                y += 8f
            }
        }

        if (cv.certificaciones.isNotEmpty()) {
            tituloDeSeccion(seccion("certificaciones"))
            cv.certificaciones.forEach { c ->
                val emisor = listOf(c.institucion, c.anio).filter { it.isNotBlank() }
                    .joinToString(", ")
                val linea = listOf(c.nombre, emisor).filter { it.isNotBlank() }.joinToString(" — ")
                parrafo(linea, xCuerpo, anchoCuerpo, pCuerpo)
                y += 3f
            }
            y += 6f
        }

        // Con barra lateral, habilidades e idiomas ya viven allá.
        if (!hayBarra) {
            if (cv.habilidades.isNotEmpty()) {
                tituloDeSeccion(seccion("habilidades"))
                parrafo(cv.habilidades.joinToString("  ·  "), xCuerpo, anchoCuerpo, pCuerpo)
                y += 8f
            }
            if (cv.idiomas.isNotEmpty()) {
                tituloDeSeccion(seccion("idiomas"))
                val linea = cv.idiomas.joinToString("  ·  ") {
                    listOf(it.idioma, it.nivel).filter { parte -> parte.isNotBlank() }
                        .joinToString(": ")
                }
                parrafo(linea, xCuerpo, anchoCuerpo, pCuerpo)
            }
        }
    }

    /**
     * La línea de tiempo del diseño Cronología. Si la entrada se partió entre dos
     * páginas no se dibuja el riel: una línea que empieza en una hoja y termina en
     * otra se ve como un error de impresión.
     */
    private fun conRiel(bloque: (Float, Float) -> Unit) {
        if (!plantilla.diseno.riel) {
            bloque(xCuerpo, anchoCuerpo)
            return
        }
        val sangria = 16f
        val paginaInicial = numeroDePagina
        val yInicio = y
        bloque(xCuerpo + sangria, anchoCuerpo - sangria)
        if (paginaInicial != numeroDePagina) return
        lienzo.drawLine(xCuerpo + 4f, yInicio - 2f, xCuerpo + 4f, y - 10f, trazo(aclarar(acento, 0.55f), 1.4f))
        lienzo.drawCircle(xCuerpo + 4f, yInicio - 4f, 3.6f, relleno(acento))
    }

    // ---------- Títulos de sección ----------

    private fun tituloDeSeccion(titulo: String) {
        reservar(48f)
        when (plantilla.diseno.estiloTitulo) {
            EstiloTitulo.SUBRAYADO -> {
                y += 8f
                lienzo.drawText(titulo.uppercase(), xCuerpo, y, texto(9.5f, true, acento))
                y += 4f
                lienzo.drawLine(xCuerpo, y, xCuerpo + anchoCuerpo, y, trazo(LINEA, 0.8f))
                y += 13f
            }
            EstiloTitulo.LIMPIO -> {
                y += 16f
                lienzo.drawText(
                    titulo.uppercase(), xCuerpo, y,
                    espaciado(texto(9f, true, TINTA), 0.14f),
                )
                y += 15f
            }
            EstiloTitulo.BARRA -> {
                y += 12f
                lienzo.drawRect(xCuerpo, y - 8.5f, xCuerpo + 3f, y + 1.5f, relleno(acento))
                lienzo.drawText(titulo.uppercase(), xCuerpo + 9f, y, texto(9.5f, true, TINTA))
                y += 14f
            }
            EstiloTitulo.CHIP -> {
                y += 12f
                val pintura = texto(8.5f, true, Color.WHITE)
                val ancho = pintura.measureText(titulo.uppercase()) + 18f
                lienzo.drawRoundRect(
                    RectF(xCuerpo, y - 9f, xCuerpo + ancho, y + 4f), 6.5f, 6.5f, relleno(acento),
                )
                lienzo.drawText(titulo.uppercase(), xCuerpo + 9f, y, pintura)
                y += 17f
            }
            EstiloTitulo.BLOQUE -> {
                y += 12f
                lienzo.drawRect(
                    xCuerpo, y - 10f, xCuerpo + anchoCuerpo, y + 4f, relleno(aclarar(acento, 0.86f)),
                )
                lienzo.drawRect(xCuerpo, y - 10f, xCuerpo + 3.5f, y + 4f, relleno(acento))
                lienzo.drawText(titulo.uppercase(), xCuerpo + 11f, y, texto(9f, true, acento))
                y += 18f
            }
        }
    }

    // ---------- Páginas ----------

    private fun abrirPagina(numero: Int): PdfDocument.Page =
        documento.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, numero).create())

    /** Si lo que viene no cabe, se pasa a la hoja siguiente en vez de recortarlo. */
    private fun reservar(alto: Float) {
        if (y + alto <= ALTO - MARGEN) return
        documento.finishPage(pagina)
        numeroDePagina += 1
        pagina = abrirPagina(numeroDePagina)
        lienzo = pagina.canvas
        if (plantilla.diseno.barra != Barra.NINGUNA) pintarFondoDeBarra()
        y = MARGEN + 12f
    }

    // ---------- Texto ----------

    private fun texto(tamano: Float, negrita: Boolean, color: Int) = Paint().apply {
        this.color = color
        textSize = tamano * plantilla.fuente.escala
        isAntiAlias = true
        typeface = Typeface.create(
            plantilla.fuente.familia,
            if (negrita) Typeface.BOLD else Typeface.NORMAL,
        )
    }

    private fun centrado(pintura: Paint) = pintura.apply { textAlign = Paint.Align.CENTER }

    private fun espaciado(pintura: Paint, espacio: Float) = pintura.apply { letterSpacing = espacio }

    private fun relleno(color: Int) = Paint().apply {
        this.color = color
        isAntiAlias = true
    }

    private fun trazo(color: Int, grosor: Float) = Paint().apply {
        this.color = color
        strokeWidth = grosor
        isAntiAlias = true
    }

    /**
     * Texto con su propio cursor, sin saltos de página. Lo usan el encabezado y la
     * barra lateral, que siempre caben en la primera hoja.
     */
    private fun escribirEnvuelto(
        contenido: String,
        x: Float,
        inicio: Float,
        ancho: Float,
        pintura: Paint,
    ): Float {
        var cursor = inicio
        partirEnLineas(contenido, ancho, pintura).forEach { linea ->
            lienzo.drawText(linea, x, cursor, pintura)
            cursor += pintura.textSize + 3.5f
        }
        return cursor
    }

    /** Texto de la columna principal: avanza el cursor de página y salta de hoja si no cabe. */
    private fun parrafo(contenido: String, x: Float, ancho: Float, pintura: Paint) {
        partirEnLineas(contenido, ancho, pintura).forEach { linea ->
            reservar(pintura.textSize + 6f)
            lienzo.drawText(linea, x, y, pintura)
            y += pintura.textSize + 3.5f
        }
    }

    private fun dibujarVinetas(items: List<String>, x: Float, ancho: Float, pintura: Paint) {
        items.forEach { item ->
            partirEnLineas(item, ancho - 12f, pintura).forEachIndexed { indice, linea ->
                reservar(pintura.textSize + 6f)
                lienzo.drawText(if (indice == 0) "•  $linea" else "     $linea", x, y, pintura)
                y += pintura.textSize + 3.5f
            }
        }
    }

    private fun partirEnLineas(contenido: String, ancho: Float, pintura: Paint): List<String> {
        val lineas = mutableListOf<String>()
        contenido.split("\n").forEach { crudo ->
            var actual = StringBuilder()
            crudo.split(" ").filter { it.isNotEmpty() }.forEach { palabra ->
                val prueba = if (actual.isEmpty()) palabra else "$actual $palabra"
                if (pintura.measureText(prueba) > ancho && actual.isNotEmpty()) {
                    lineas += actual.toString()
                    actual = StringBuilder(palabra)
                } else {
                    actual = StringBuilder(prueba)
                }
            }
            if (actual.isNotEmpty()) lineas += actual.toString()
        }
        return lineas
    }

    private fun recortar(contenido: String, ancho: Float, pintura: Paint): String {
        if (pintura.measureText(contenido) <= ancho) return contenido
        var corte = contenido.length
        while (corte > 1 && pintura.measureText(contenido.take(corte) + "…") > ancho) corte--
        return contenido.take(corte).trimEnd() + "…"
    }

    // ---------- Datos ----------

    private fun datosDeContacto(): List<String> = listOf(
        cv.datos.ciudad, cv.datos.telefono, cv.datos.correo,
        cv.datos.linkedin, cv.datos.portafolio,
    ).filter { it.isNotBlank() }

    /** Dos renglones: primero cómo contactarte, luego dónde verte. */
    private fun lineasDeContacto(): List<String> = listOfNotNull(
        listOf(cv.datos.ciudad, cv.datos.telefono, cv.datos.correo)
            .filter { it.isNotBlank() }.joinToString("  ·  ").ifBlank { null },
        listOf(cv.datos.linkedin, cv.datos.portafolio)
            .filter { it.isNotBlank() }.joinToString("  ·  ").ifBlank { null },
    )

    private fun usaFoto() = plantilla.conFoto && plantilla.admiteFoto && foto != null

    private fun dibujarFoto(x: Float, y: Float, lado: Float, circular: Boolean) {
        val original = foto ?: return
        // Recorte cuadrado desde el centro: escalar de frente deforma la cara.
        val corte = minOf(original.width, original.height)
        val cuadrada = Bitmap.createBitmap(
            original,
            (original.width - corte) / 2,
            (original.height - corte) / 2,
            corte,
            corte,
        )
        val escalada = Bitmap.createScaledBitmap(cuadrada, lado.toInt(), lado.toInt(), true)

        if (circular) {
            lienzo.save()
            lienzo.clipPath(
                Path().apply {
                    addCircle(x + lado / 2, y + lado / 2, lado / 2, Path.Direction.CW)
                },
            )
            lienzo.drawBitmap(escalada, x, y, null)
            lienzo.restore()
        } else {
            lienzo.drawBitmap(escalada, x, y, null)
        }
    }

    private fun aclarar(color: Int, factor: Float): Int = Color.rgb(
        (Color.red(color) + (255 - Color.red(color)) * factor).toInt().coerceIn(0, 255),
        (Color.green(color) + (255 - Color.green(color)) * factor).toInt().coerceIn(0, 255),
        (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt().coerceIn(0, 255),
    )

    private fun seccion(clave: String): String {
        val es = cv.idioma.startsWith("es", ignoreCase = true)
        return when (clave) {
            "proyectos" -> if (es) "Proyectos" else "Projects"
            "experiencia" -> if (es) "Experiencia" else "Experience"
            "formacion" -> if (es) "Formación" else "Education"
            "certificaciones" -> if (es) "Certificaciones" else "Certifications"
            "habilidades" -> if (es) "Habilidades" else "Skills"
            "idiomas" -> if (es) "Idiomas" else "Languages"
            "contacto" -> if (es) "Contacto" else "Contact"
            else -> clave
        }
    }

    private companion object {
        const val ANCHO = 595   // A4 a 72 dpi
        const val ALTO = 842
        const val MARGEN = 44f
        const val BARRA_ANCHO = 186f

        val TINTA: Int = Color.parseColor("#0F172A")
        val CUERPO: Int = Color.parseColor("#1E293B")
        val SUAVE: Int = Color.parseColor("#64748B")
        val LINEA: Int = Color.parseColor("#CBD5E1")
    }
}
