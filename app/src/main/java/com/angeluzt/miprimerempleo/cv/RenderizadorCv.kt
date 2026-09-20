package com.angeluzt.miprimerempleo.cv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File

/**
 * Arma el PDF en el teléfono. La IA solo produce el contenido, nunca el archivo:
 * así cambiar de plantilla o editar un campo no cuesta otra generación.
 */
class RenderizadorCv(private val context: Context) {

    private companion object {
        const val ANCHO = 595   // A4 a 72 dpi
        const val ALTO = 842
        const val MARGEN = 44f
        const val LATERAL_ANCHO = 186f

        val TINTA = Color.parseColor("#0F172A")
        val CUERPO = Color.parseColor("#1E293B")
        val SUAVE = Color.parseColor("#64748B")
        val LINEA = Color.parseColor("#CBD5E1")
    }

    fun exportar(cv: Cv, plantilla: Plantilla, foto: Bitmap?, nombreArchivo: String): File {
        val documento = PdfDocument()
        val pagina = documento.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, 1).create())

        when (plantilla.diseno) {
            Diseno.CLASICA -> dibujarClasica(pagina.canvas, cv, plantilla, foto)
            Diseno.BANDA -> dibujarBanda(pagina.canvas, cv, plantilla, foto)
            Diseno.LATERAL -> dibujarLateral(pagina.canvas, cv, plantilla, foto)
        }

        documento.finishPage(pagina)

        val carpeta = File(context.cacheDir, "cv").apply { mkdirs() }
        val archivo = File(carpeta, "$nombreArchivo.pdf")
        archivo.outputStream().use { documento.writeTo(it) }
        documento.close()
        return archivo
    }

    // ---------- Diseño 1: una columna, sobrio ----------

    private fun dibujarClasica(lienzo: Canvas, cv: Cv, plantilla: Plantilla, foto: Bitmap?) {
        var y = MARGEN + 16f
        val anchoUtil = ANCHO - MARGEN * 2
        var anchoTexto = anchoUtil

        if (plantilla.conFoto && foto != null) {
            val lado = 74f
            dibujarFoto(lienzo, foto, ANCHO - MARGEN - lado, MARGEN, lado)
            anchoTexto = anchoUtil - lado - 14f
        }

        lienzo.drawText(cv.datos.nombre, MARGEN, y, texto(19f, true, TINTA))
        y += 18f
        lienzo.drawText(cv.datos.puesto, MARGEN, y, texto(12f, false, plantilla.acento))
        y += 16f
        y = dibujarContacto(lienzo, cv, MARGEN, y, anchoTexto)
        y += 10f

        y = dibujarCuerpo(lienzo, cv, plantilla, MARGEN, y, anchoUtil, incluirHabilidades = true)
        pieDePagina(lienzo, y)
    }

    // ---------- Diseño 2: banda de color en el encabezado ----------

    private fun dibujarBanda(lienzo: Canvas, cv: Cv, plantilla: Plantilla, foto: Bitmap?) {
        val altoBanda = 116f
        lienzo.drawRect(0f, 0f, ANCHO.toFloat(), altoBanda, Paint().apply { color = plantilla.acento })

        if (plantilla.conFoto && foto != null) {
            val lado = 72f
            dibujarFoto(lienzo, foto, ANCHO - MARGEN - lado, (altoBanda - lado) / 2, lado)
        }

        lienzo.drawText(cv.datos.nombre, MARGEN, 46f, texto(21f, true, Color.WHITE))
        lienzo.drawText(cv.datos.puesto, MARGEN, 66f, texto(12f, false, Color.parseColor("#E8EEFF")))

        val contacto = listOfNotNull(
            cv.datos.ciudad.ifBlank { null },
            cv.datos.telefono.ifBlank { null },
            cv.datos.correo.ifBlank { null },
        ).joinToString("  ·  ")
        if (contacto.isNotBlank()) {
            lienzo.drawText(contacto, MARGEN, 88f, texto(8.5f, false, Color.parseColor("#DCE5FF")))
        }
        val enlaces = listOfNotNull(
            cv.datos.linkedin.ifBlank { null },
            cv.datos.portafolio.ifBlank { null },
        ).joinToString("  ·  ")
        if (enlaces.isNotBlank()) {
            lienzo.drawText(enlaces, MARGEN, 101f, texto(8.5f, false, Color.parseColor("#DCE5FF")))
        }

        var y = altoBanda + 26f
        y = dibujarCuerpo(lienzo, cv, plantilla, MARGEN, y, ANCHO - MARGEN * 2, incluirHabilidades = true)
        pieDePagina(lienzo, y)
    }

    // ---------- Diseño 3: barra lateral ----------

    private fun dibujarLateral(lienzo: Canvas, cv: Cv, plantilla: Plantilla, foto: Bitmap?) {
        lienzo.drawRect(
            0f, 0f, LATERAL_ANCHO, ALTO.toFloat(),
            Paint().apply { color = plantilla.acento },
        )

        val margenLat = 22f
        val anchoLat = LATERAL_ANCHO - margenLat * 2
        var yl = 40f

        if (plantilla.conFoto && foto != null) {
            val lado = 88f
            dibujarFoto(lienzo, foto, (LATERAL_ANCHO - lado) / 2, yl, lado)
            yl += lado + 22f
        }

        val blanco = Color.WHITE
        val tenue = Color.parseColor("#E2E8F0")

        yl = parrafo(lienzo, cv.datos.nombre, margenLat, yl, anchoLat, texto(16f, true, blanco))
        yl += 4f
        yl = parrafo(lienzo, cv.datos.puesto, margenLat, yl, anchoLat, texto(10f, false, tenue))
        yl += 18f

        yl = tituloLateral(lienzo, seccion("contacto", cv.idioma), margenLat, yl, anchoLat)
        listOfNotNull(
            cv.datos.ciudad.ifBlank { null },
            cv.datos.telefono.ifBlank { null },
            cv.datos.correo.ifBlank { null },
            cv.datos.linkedin.ifBlank { null },
            cv.datos.portafolio.ifBlank { null },
        ).forEach {
            yl = parrafo(lienzo, it, margenLat, yl, anchoLat, texto(8.5f, false, tenue))
            yl += 4f
        }

        if (cv.habilidades.isNotEmpty()) {
            yl += 14f
            yl = tituloLateral(lienzo, seccion("habilidades", cv.idioma), margenLat, yl, anchoLat)
            cv.habilidades.forEach {
                yl = parrafo(lienzo, "· $it", margenLat, yl, anchoLat, texto(8.5f, false, tenue))
                yl += 3f
            }
        }

        if (cv.idiomas.isNotEmpty()) {
            yl += 14f
            yl = tituloLateral(lienzo, seccion("idiomas", cv.idioma), margenLat, yl, anchoLat)
            cv.idiomas.forEach {
                yl = parrafo(lienzo, "${it.idioma}: ${it.nivel}", margenLat, yl, anchoLat, texto(8.5f, false, tenue))
                yl += 3f
            }
        }

        val x = LATERAL_ANCHO + 26f
        val ancho = ANCHO - x - MARGEN
        var y = 44f
        y = dibujarCuerpo(lienzo, cv, plantilla, x, y, ancho, incluirHabilidades = false)
        pieDePagina(lienzo, y)
    }

    // ---------- Cuerpo compartido ----------

    private fun dibujarCuerpo(
        lienzo: Canvas,
        cv: Cv,
        plantilla: Plantilla,
        x: Float,
        inicio: Float,
        ancho: Float,
        incluirHabilidades: Boolean,
    ): Float {
        var y = inicio
        val pCuerpo = texto(9.5f, false, CUERPO)
        val pCargo = texto(10f, true, TINTA)
        val pMeta = texto(8.5f, false, SUAVE)

        if (cv.resumen.isNotBlank()) {
            y = parrafo(lienzo, cv.resumen, x, y, ancho, pCuerpo)
            y += 10f
        }

        if (cv.proyectos.isNotEmpty()) {
            y = tituloSeccion(lienzo, seccion("proyectos", cv.idioma), x, y, ancho, plantilla.acento)
            cv.proyectos.forEach { p ->
                lienzo.drawText(p.nombre, x, y, pCargo)
                y += 12f
                if (p.enlace.isNotBlank()) {
                    y = parrafo(lienzo, p.enlace, x, y, ancho, pMeta)
                    y += 1f
                }
                if (p.descripcion.isNotBlank()) y = parrafo(lienzo, p.descripcion, x, y, ancho, pCuerpo)
                y = vinetas(lienzo, p.logros, x, y, ancho, pCuerpo)
                y += 7f
            }
        }

        if (cv.experiencia.isNotEmpty()) {
            y = tituloSeccion(lienzo, seccion("experiencia", cv.idioma), x, y, ancho, plantilla.acento)
            cv.experiencia.forEach { e ->
                lienzo.drawText(e.puesto, x, y, pCargo)
                y += 12f
                y = parrafo(lienzo, "${e.organizacion}  ·  ${e.periodo}", x, y, ancho, pMeta)
                y += 2f
                y = vinetas(lienzo, e.logros, x, y, ancho, pCuerpo)
                y += 7f
            }
        }

        if (cv.formacion.isNotEmpty()) {
            y = tituloSeccion(lienzo, seccion("formacion", cv.idioma), x, y, ancho, plantilla.acento)
            cv.formacion.forEach { f ->
                lienzo.drawText(f.titulo, x, y, pCargo)
                y += 12f
                y = parrafo(lienzo, "${f.institucion}  ·  ${f.periodo}", x, y, ancho, pMeta)
                y += 8f
            }
        }

        if (cv.certificaciones.isNotEmpty()) {
            y = tituloSeccion(lienzo, seccion("certificaciones", cv.idioma), x, y, ancho, plantilla.acento)
            cv.certificaciones.forEach { c ->
                y = parrafo(lienzo, "${c.nombre} — ${c.institucion}, ${c.anio}", x, y, ancho, pCuerpo)
                y += 3f
            }
            y += 6f
        }

        if (incluirHabilidades) {
            if (cv.habilidades.isNotEmpty()) {
                y = tituloSeccion(lienzo, seccion("habilidades", cv.idioma), x, y, ancho, plantilla.acento)
                y = parrafo(lienzo, cv.habilidades.joinToString("  ·  "), x, y, ancho, pCuerpo)
                y += 8f
            }
            if (cv.idiomas.isNotEmpty()) {
                y = tituloSeccion(lienzo, seccion("idiomas", cv.idioma), x, y, ancho, plantilla.acento)
                y = parrafo(
                    lienzo,
                    cv.idiomas.joinToString("  ·  ") { "${it.idioma}: ${it.nivel}" },
                    x, y, ancho, pCuerpo,
                )
            }
        }
        return y
    }

    // ---------- Utilidades de dibujo ----------

    private fun texto(tamano: Float, negrita: Boolean, color: Int) = Paint().apply {
        this.color = color
        textSize = tamano
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SANS_SERIF, if (negrita) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun dibujarFoto(lienzo: Canvas, foto: Bitmap, x: Float, y: Float, lado: Float) {
        val escalada = Bitmap.createScaledBitmap(foto, lado.toInt(), lado.toInt(), true)
        lienzo.drawBitmap(escalada, x, y, null)
    }

    private fun dibujarContacto(lienzo: Canvas, cv: Cv, x: Float, inicio: Float, ancho: Float): Float {
        var y = inicio
        val pintura = texto(8.5f, false, SUAVE)
        val contacto = listOfNotNull(
            cv.datos.ciudad.ifBlank { null },
            cv.datos.telefono.ifBlank { null },
            cv.datos.correo.ifBlank { null },
        ).joinToString("  ·  ")
        if (contacto.isNotBlank()) y = parrafo(lienzo, contacto, x, y, ancho, pintura)
        val enlaces = listOfNotNull(
            cv.datos.linkedin.ifBlank { null },
            cv.datos.portafolio.ifBlank { null },
        ).joinToString("  ·  ")
        if (enlaces.isNotBlank()) y = parrafo(lienzo, enlaces, x, y, ancho, pintura)
        return y
    }

    private fun tituloSeccion(
        lienzo: Canvas,
        titulo: String,
        x: Float,
        y: Float,
        ancho: Float,
        acento: Int,
    ): Float {
        var cursor = y + 8f
        lienzo.drawText(titulo.uppercase(), x, cursor, texto(9.5f, true, acento))
        cursor += 4f
        lienzo.drawLine(x, cursor, x + ancho, cursor, Paint().apply { color = LINEA; strokeWidth = 0.8f })
        return cursor + 13f
    }

    private fun tituloLateral(lienzo: Canvas, titulo: String, x: Float, y: Float, ancho: Float): Float {
        lienzo.drawText(titulo.uppercase(), x, y, texto(9f, true, Color.WHITE))
        val bajo = y + 4f
        lienzo.drawLine(
            x, bajo, x + ancho, bajo,
            Paint().apply { color = Color.parseColor("#80FFFFFF"); strokeWidth = 0.8f },
        )
        return bajo + 13f
    }

    private fun parrafo(
        lienzo: Canvas,
        contenido: String,
        x: Float,
        y: Float,
        ancho: Float,
        pintura: Paint,
    ): Float {
        var cursor = y
        cortar(contenido, ancho, pintura).forEach { linea ->
            lienzo.drawText(linea, x, cursor, pintura)
            cursor += pintura.textSize + 3.5f
        }
        return cursor
    }

    private fun vinetas(
        lienzo: Canvas,
        items: List<String>,
        x: Float,
        y: Float,
        ancho: Float,
        pintura: Paint,
    ): Float {
        var cursor = y
        items.forEach { item ->
            cortar(item, ancho - 12f, pintura).forEachIndexed { indice, linea ->
                lienzo.drawText(if (indice == 0) "•  $linea" else "    $linea", x, cursor, pintura)
                cursor += pintura.textSize + 3.5f
            }
        }
        return cursor
    }

    private fun cortar(contenido: String, ancho: Float, pintura: Paint): List<String> {
        val lineas = mutableListOf<String>()
        var actual = StringBuilder()
        val limites = Rect()
        contenido.split(" ").forEach { palabra ->
            val prueba = if (actual.isEmpty()) palabra else "$actual $palabra"
            pintura.getTextBounds(prueba, 0, prueba.length, limites)
            if (limites.width() > ancho && actual.isNotEmpty()) {
                lineas += actual.toString()
                actual = StringBuilder(palabra)
            } else {
                actual = StringBuilder(prueba)
            }
        }
        if (actual.isNotEmpty()) lineas += actual.toString()
        return lineas
    }

    /** Avisa cuando el contenido ya no cupo en la página, en vez de recortarlo en silencio. */
    private fun pieDePagina(lienzo: Canvas, y: Float) {
        if (y <= ALTO - MARGEN) return
        val aviso = Paint().apply {
            color = Color.parseColor("#B91C1C")
            textSize = 8f
            isAntiAlias = true
        }
        lienzo.drawRect(
            0f, ALTO - 20f, ANCHO.toFloat(), ALTO.toFloat(),
            Paint().apply { color = Color.parseColor("#FEE2E2") },
        )
        lienzo.drawText(
            "El contenido no cabe en una página. Recorta texto o elige la plantilla Clásica.",
            MARGEN, ALTO - 7f, aviso,
        )
    }

    private fun seccion(clave: String, idioma: String): String {
        val es = idioma.startsWith("es", ignoreCase = true)
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
}
