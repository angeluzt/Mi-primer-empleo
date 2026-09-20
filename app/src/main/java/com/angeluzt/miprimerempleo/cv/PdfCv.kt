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
 * El PDF se arma en el teléfono. La IA solo produce el contenido, nunca el archivo:
 * así editar un campo no cuesta otra generación y la exportación funciona sin internet.
 */
class PdfCv(private val context: Context) {

    private val ancho = 595   // A4 a 72 dpi
    private val alto = 842
    private val margen = 44f

    fun exportar(cv: Cv, plantilla: Plantilla, foto: Bitmap?, nombreArchivo: String): File {
        val documento = PdfDocument()
        val pagina = documento.startPage(PdfDocument.PageInfo.Builder(ancho, alto, 1).create())
        val lienzo = pagina.canvas

        var y = margen
        val anchoUtil = ancho - margen * 2

        val tNombre = pintar(19f, true, Color.parseColor("#0F172A"))
        val tPuesto = pintar(12f, false, Color.parseColor("#1D4ED8"))
        val tContacto = pintar(8.5f, false, Color.parseColor("#475569"))
        val tSeccion = pintar(10f, true, Color.parseColor("#1D4ED8"))
        val tCargo = pintar(10f, true, Color.parseColor("#0F172A"))
        val tMeta = pintar(8.5f, false, Color.parseColor("#64748B"))
        val tCuerpo = pintar(9.5f, false, Color.parseColor("#1E293B"))

        var anchoTexto = anchoUtil
        if (plantilla.conFoto && foto != null) {
            val lado = 74f
            val izq = ancho - margen - lado
            lienzo.drawBitmap(
                Bitmap.createScaledBitmap(foto, lado.toInt(), lado.toInt(), true),
                izq, y, null,
            )
            anchoTexto = anchoUtil - lado - 14f
        }

        lienzo.drawText(cv.datos.nombre, margen, y + 16f, tNombre)
        y += 32f
        lienzo.drawText(cv.datos.puesto, margen, y, tPuesto)
        y += 16f

        val contacto = listOfNotNull(
            cv.datos.ciudad.ifBlank { null },
            cv.datos.telefono.ifBlank { null },
            cv.datos.correo.ifBlank { null },
        ).joinToString("  •  ")
        if (contacto.isNotBlank()) {
            lienzo.drawText(contacto, margen, y, tContacto)
            y += 12f
        }
        val enlaces = listOfNotNull(
            cv.datos.linkedin.ifBlank { null },
            cv.datos.portafolio.ifBlank { null },
        ).joinToString("  •  ")
        if (enlaces.isNotBlank()) {
            lienzo.drawText(enlaces, margen, y, tContacto)
            y += 12f
        }

        y += 8f
        if (cv.resumen.isNotBlank()) {
            y = parrafo(lienzo, cv.resumen, margen, y, anchoTexto, tCuerpo)
            y += 8f
        }

        if (cv.proyectos.isNotEmpty()) {
            y = seccion(lienzo, tituloSeccion("proyectos", cv.idioma), y, tSeccion)
            cv.proyectos.forEach { p ->
                lienzo.drawText(p.nombre, margen, y, tCargo)
                y += 12f
                if (p.enlace.isNotBlank()) {
                    lienzo.drawText(p.enlace, margen, y, tMeta)
                    y += 11f
                }
                y = parrafo(lienzo, p.descripcion, margen, y, anchoUtil, tCuerpo)
                y = vinetas(lienzo, p.logros, y, anchoUtil, tCuerpo)
                y += 6f
            }
        }

        if (cv.experiencia.isNotEmpty()) {
            y = seccion(lienzo, tituloSeccion("experiencia", cv.idioma), y, tSeccion)
            cv.experiencia.forEach { e ->
                lienzo.drawText(e.puesto, margen, y, tCargo)
                y += 12f
                lienzo.drawText("${e.organizacion}  •  ${e.periodo}", margen, y, tMeta)
                y += 12f
                y = vinetas(lienzo, e.logros, y, anchoUtil, tCuerpo)
                y += 6f
            }
        }

        if (cv.formacion.isNotEmpty()) {
            y = seccion(lienzo, tituloSeccion("formacion", cv.idioma), y, tSeccion)
            cv.formacion.forEach { f ->
                lienzo.drawText(f.titulo, margen, y, tCargo)
                y += 12f
                lienzo.drawText("${f.institucion}  •  ${f.periodo}", margen, y, tMeta)
                y += 14f
            }
        }

        if (cv.certificaciones.isNotEmpty()) {
            y = seccion(lienzo, tituloSeccion("certificaciones", cv.idioma), y, tSeccion)
            cv.certificaciones.forEach { c ->
                lienzo.drawText("${c.nombre} — ${c.institucion}, ${c.anio}", margen, y, tCuerpo)
                y += 13f
            }
            y += 4f
        }

        if (cv.habilidades.isNotEmpty()) {
            y = seccion(lienzo, tituloSeccion("habilidades", cv.idioma), y, tSeccion)
            y = parrafo(lienzo, cv.habilidades.joinToString("  •  "), margen, y, anchoUtil, tCuerpo)
            y += 6f
        }

        if (cv.idiomas.isNotEmpty()) {
            y = seccion(lienzo, tituloSeccion("idiomas", cv.idioma), y, tSeccion)
            parrafo(
                lienzo,
                cv.idiomas.joinToString("  •  ") { "${it.idioma}: ${it.nivel}" },
                margen, y, anchoUtil, tCuerpo,
            )
        }

        documento.finishPage(pagina)

        val carpeta = File(context.cacheDir, "cv").apply { mkdirs() }
        val archivo = File(carpeta, "$nombreArchivo.pdf")
        archivo.outputStream().use { documento.writeTo(it) }
        documento.close()
        return archivo
    }

    private fun pintar(tamano: Float, negrita: Boolean, color: Int) = Paint().apply {
        this.color = color
        textSize = tamano
        isAntiAlias = true
        typeface = Typeface.create(Typeface.SANS_SERIF, if (negrita) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun seccion(lienzo: Canvas, texto: String, y: Float, pintura: Paint): Float {
        var cursor = y + 6f
        lienzo.drawText(texto.uppercase(), margen, cursor, pintura)
        cursor += 4f
        val linea = Paint().apply { color = Color.parseColor("#CBD5E1"); strokeWidth = 0.8f }
        lienzo.drawLine(margen, cursor, ancho - margen, cursor, linea)
        return cursor + 13f
    }

    private fun parrafo(
        lienzo: Canvas,
        texto: String,
        x: Float,
        y: Float,
        ancho: Float,
        pintura: Paint,
    ): Float {
        var cursor = y
        cortar(texto, ancho, pintura).forEach { linea ->
            lienzo.drawText(linea, x, cursor, pintura)
            cursor += pintura.textSize + 3.5f
        }
        return cursor
    }

    private fun vinetas(
        lienzo: Canvas,
        items: List<String>,
        y: Float,
        ancho: Float,
        pintura: Paint,
    ): Float {
        var cursor = y
        items.forEach { item ->
            val lineas = cortar(item, ancho - 12f, pintura)
            lineas.forEachIndexed { indice, linea ->
                val prefijo = if (indice == 0) "•  " else "    "
                lienzo.drawText(prefijo + linea, margen, cursor, pintura)
                cursor += pintura.textSize + 3.5f
            }
        }
        return cursor
    }

    private fun cortar(texto: String, ancho: Float, pintura: Paint): List<String> {
        val lineas = mutableListOf<String>()
        var actual = StringBuilder()
        val limites = Rect()
        texto.split(" ").forEach { palabra ->
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

    private fun tituloSeccion(clave: String, idioma: String): String {
        val es = idioma.startsWith("es", ignoreCase = true)
        return when (clave) {
            "proyectos" -> if (es) "Proyectos" else "Projects"
            "experiencia" -> if (es) "Experiencia" else "Experience"
            "formacion" -> if (es) "Formación académica" else "Education"
            "certificaciones" -> if (es) "Certificaciones" else "Certifications"
            "habilidades" -> if (es) "Habilidades" else "Skills"
            "idiomas" -> if (es) "Idiomas" else "Languages"
            else -> clave
        }
    }
}
