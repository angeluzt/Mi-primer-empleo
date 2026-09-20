package com.angeluzt.miprimerempleo.cv

import android.graphics.Color

/**
 * Tres motores de maquetado reales. No son variaciones de color de lo mismo:
 * cada uno coloca la información de forma distinta.
 */
enum class Diseno {
    /** Una columna, encabezado sobrio. La más segura para lectores automáticos. */
    CLASICA,

    /** Banda de color con el nombre arriba. Sigue siendo una columna, así que se lee bien. */
    BANDA,

    /** Barra lateral con contacto y habilidades. Se ve muy bien y los filtros la revuelven. */
    LATERAL,
}

data class Plantilla(
    val id: String,
    val nombre: String,
    val familia: String,
    val descripcion: String,
    val diseno: Diseno,
    val acento: Int,
    val conFoto: Boolean,
) {
    /**
     * Si es segura para los filtros automáticos (ATS) que usan las bolsas de trabajo
     * y casi todas las empresas grandes. Una plantilla a dos columnas se ve preciosa
     * y llega revuelta al otro lado, así que la app lo advierte en vez de esconderlo.
     */
    val aptaParaFiltros: Boolean get() = diseno != Diseno.LATERAL
}

object Plantillas {

    private val AZUL = Color.parseColor("#1D4ED8")
    private val VERDE = Color.parseColor("#047857")
    private val VINO = Color.parseColor("#9F1239")
    private val GRAFITO = Color.parseColor("#334155")

    private val acentos = listOf(
        "azul" to AZUL,
        "verde" to VERDE,
        "vino" to VINO,
        "grafito" to GRAFITO,
    )

    private val familias = listOf(
        Triple(Diseno.CLASICA, "Clásica", "Una columna, sin adornos. La que debes subir a bolsas de trabajo."),
        Triple(Diseno.BANDA, "Encabezado", "Banda de color con tu nombre. Se ve moderna y los filtros la leen bien."),
        Triple(Diseno.LATERAL, "Lateral", "Barra lateral con contacto y habilidades. Para entregar en mano o por correo."),
    )

    /** Cada diseño en cuatro colores, con y sin foto. */
    val catalogo: List<Plantilla> = familias.flatMap { (diseno, familia, descripcion) ->
        acentos.flatMap { (nombreColor, color) ->
            listOf(false, true).map { conFoto ->
                Plantilla(
                    id = "${diseno.name.lowercase()}_${nombreColor}${if (conFoto) "_foto" else ""}",
                    nombre = buildString {
                        append(familia)
                        append(" · ")
                        append(nombreColor.replaceFirstChar { it.uppercase() })
                        if (conFoto) append(" · con foto")
                    },
                    familia = familia,
                    descripcion = descripcion,
                    diseno = diseno,
                    acento = color,
                    conFoto = conFoto,
                )
            }
        }
    }

    val porDefecto: Plantilla = catalogo.first { it.diseno == Diseno.CLASICA && !it.conFoto }

    fun porId(id: String): Plantilla = catalogo.firstOrNull { it.id == id } ?: porDefecto

    /** Las que conviene subir a OCC, Computrabajo, LinkedIn y portales de empresas. */
    fun seguras(): List<Plantilla> = catalogo.filter { it.aptaParaFiltros }
}
