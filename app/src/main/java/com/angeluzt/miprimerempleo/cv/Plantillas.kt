package com.angeluzt.miprimerempleo.cv

import android.graphics.Color

/**
 * El formato se arma con cuatro decisiones independientes en vez de elegir de una
 * lista cerrada. Así dos personas casi nunca entregan el mismo CV, y la persona
 * decide sobre cosas que entiende (diseño, color, letra, foto) en vez de sobre
 * nombres inventados de plantilla.
 */
enum class Diseno(val etiqueta: String, val descripcion: String) {
    /** Una columna, encabezado sobrio. La más segura para lectores automáticos. */
    CLASICA("Clásica", "Una columna, sin adornos. La que debes subir a bolsas de trabajo."),

    /** Banda de color con el nombre arriba. Sigue siendo una columna. */
    BANDA("Encabezado", "Banda de color con tu nombre. Moderna y los filtros la leen bien."),

    /** Barra lateral con contacto y habilidades. Los filtros la revuelven. */
    LATERAL("Lateral", "Barra lateral con contacto. Para entregar en mano o por correo."),
}

enum class Fuente(val etiqueta: String, val familia: String, val escala: Float) {
    MODERNA("Moderna", "sans-serif", 1.00f),
    SERIF("Con serifas", "serif", 1.00f),
    // Aprieta el texto sin encogerlo: sirve cuando el CV no cabe en una página.
    COMPACTA("Compacta", "sans-serif-condensed", 0.97f),
}

enum class ColorAcento(val etiqueta: String, val valor: Int) {
    AZUL("Azul", Color.rgb(0x1D, 0x4E, 0xD8)),
    GRAFITO("Grafito", Color.rgb(0x33, 0x41, 0x55)),
    VERDE("Verde", Color.rgb(0x04, 0x78, 0x57)),
    VINO("Vino", Color.rgb(0x9F, 0x12, 0x39)),
    MORADO("Morado", Color.rgb(0x6D, 0x28, 0xD9)),
    TERRACOTA("Terracota", Color.rgb(0xC2, 0x41, 0x0C)),
}

data class Plantilla(
    val diseno: Diseno = Diseno.CLASICA,
    val color: ColorAcento = ColorAcento.AZUL,
    val fuente: Fuente = Fuente.MODERNA,
    val conFoto: Boolean = false,
) {
    val id: String
        get() = "${diseno.name}_${color.name}_${fuente.name}${if (conFoto) "_foto" else ""}"

    val acento: Int get() = color.valor

    val nombre: String
        get() = buildString {
            append(diseno.etiqueta)
            append(" · ")
            append(color.etiqueta)
            append(" · ")
            append(fuente.etiqueta)
            if (conFoto) append(" · con foto")
        }

    /**
     * Si es segura para los filtros automáticos (ATS) que usan las bolsas de trabajo
     * y casi todas las empresas grandes. Una plantilla a dos columnas se ve preciosa
     * y llega revuelta al otro lado, así que la app lo advierte en vez de esconderlo.
     */
    val aptaParaFiltros: Boolean get() = diseno != Diseno.LATERAL
}

object Plantillas {

    val porDefecto = Plantilla()

    val combinaciones: Int
        get() = Diseno.entries.size * ColorAcento.entries.size * Fuente.entries.size * 2

    /** Reconstruye la plantilla desde el id guardado en el progreso. */
    fun porId(id: String): Plantilla {
        val partes = id.split("_")
        if (partes.size < 3) return porDefecto
        return runCatching {
            Plantilla(
                diseno = Diseno.valueOf(partes[0]),
                color = ColorAcento.valueOf(partes[1]),
                fuente = Fuente.valueOf(partes[2]),
                conFoto = partes.size > 3 && partes[3] == "foto",
            )
        }.getOrDefault(porDefecto)
    }
}
