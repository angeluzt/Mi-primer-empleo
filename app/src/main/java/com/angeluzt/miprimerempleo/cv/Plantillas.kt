package com.angeluzt.miprimerempleo.cv

import android.graphics.Color

/**
 * El formato se arma con cuatro decisiones independientes en vez de elegir de una
 * lista cerrada. Así dos personas casi nunca entregan el mismo CV, y la persona
 * decide sobre cosas que entiende (diseño, color, letra, foto) en vez de sobre
 * nombres inventados de plantilla.
 *
 * Los diez diseños no son el mismo papel pintado de otro color: cada uno cambia
 * dónde va el encabezado, cómo se separan las secciones y cuánto aire queda.
 * Esas tres cosas son las que hacen que un CV "se sienta" distinto.
 */

/** Cómo se presenta el nombre y el contacto en la parte de arriba. */
enum class Encabezado { SIMPLE, BANDA, CENTRADO, BLOQUE, MINIMAL, LINEAS }

/** Columna de color con el contacto. Bonita, pero los filtros automáticos la revuelven. */
enum class Barra { NINGUNA, IZQUIERDA, DERECHA }

/** Cómo se marca el inicio de cada sección. */
enum class EstiloTitulo { SUBRAYADO, LIMPIO, BARRA, CHIP, BLOQUE }

enum class Diseno(
    val etiqueta: String,
    val descripcion: String,
    val encabezado: Encabezado,
    val barra: Barra,
    val estiloTitulo: EstiloTitulo,
    /** Línea vertical que enhebra las experiencias, como una historia. */
    val riel: Boolean = false,
) {
    CLASICA(
        "Clásica", "Una columna, sin adornos. La más segura para bolsas de trabajo.",
        Encabezado.SIMPLE, Barra.NINGUNA, EstiloTitulo.SUBRAYADO,
    ),
    BANDA(
        "Encabezado", "Banda de color con tu nombre arriba. Moderna y los filtros la leen bien.",
        Encabezado.BANDA, Barra.NINGUNA, EstiloTitulo.SUBRAYADO,
    ),
    MINIMA(
        "Mínima", "Sin rellenos de color. Tu nombre grande y mucho aire alrededor.",
        Encabezado.MINIMAL, Barra.NINGUNA, EstiloTitulo.LIMPIO,
    ),
    CRONOLOGIA(
        "Cronología", "Una línea vertical enhebra tu experiencia, como una historia.",
        Encabezado.SIMPLE, Barra.NINGUNA, EstiloTitulo.BARRA, riel = true,
    ),
    CENTRADA(
        "Centrada", "Nombre y contacto centrados, estilo carta formal.",
        Encabezado.CENTRADO, Barra.NINGUNA, EstiloTitulo.LIMPIO,
    ),
    BLOQUES(
        "Bloques", "Cada sección arranca con una barra de color de lado a lado.",
        Encabezado.SIMPLE, Barra.NINGUNA, EstiloTitulo.BLOQUE,
    ),
    EJECUTIVA(
        "Ejecutiva", "Recuadro oscuro arriba. Formal: banca, despachos, gobierno.",
        Encabezado.BLOQUE, Barra.NINGUNA, EstiloTitulo.SUBRAYADO,
    ),
    ETIQUETAS(
        "Etiquetas", "Los títulos van dentro de una etiqueta de color. Ordenada y actual.",
        Encabezado.LINEAS, Barra.NINGUNA, EstiloTitulo.CHIP,
    ),
    LATERAL(
        "Lateral", "Barra de color a la izquierda con tu contacto. Para entregar en mano.",
        Encabezado.SIMPLE, Barra.IZQUIERDA, EstiloTitulo.SUBRAYADO,
    ),
    LATERAL_DERECHA(
        "Lateral derecha", "La barra va del lado derecho y el texto empieza en el margen.",
        Encabezado.SIMPLE, Barra.DERECHA, EstiloTitulo.SUBRAYADO,
    ),
}

enum class Fuente(val etiqueta: String, val familia: String, val escala: Float) {
    MODERNA("Moderna", "sans-serif", 1.00f),
    SERIF("Con serifas", "serif", 1.00f),
    // Aprieta el texto sin encogerlo: sirve cuando el CV no cabe en una página.
    COMPACTA("Compacta", "sans-serif-condensed", 0.97f),
    // Lo contrario: cuando hay poco que contar, llenar la hoja se ve mejor que dejarla a medias.
    AMPLIA("Amplia", "sans-serif", 1.08f),
}

enum class ColorAcento(val etiqueta: String, val valor: Int) {
    AZUL("Azul", Color.rgb(0x1D, 0x4E, 0xD8)),
    MARINO("Marino", Color.rgb(0x1E, 0x3A, 0x8A)),
    GRAFITO("Grafito", Color.rgb(0x33, 0x41, 0x55)),
    VERDE("Verde", Color.rgb(0x04, 0x78, 0x57)),
    TURQUESA("Turquesa", Color.rgb(0x0E, 0x74, 0x90)),
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
    /**
     * La barra `|` separa porque algún diseño lleva guion bajo en el nombre.
     * Con `_` el id se partiría por la mitad al reconstruirlo.
     */
    val id: String
        get() = "${diseno.name}|${color.name}|${fuente.name}|${if (conFoto) "foto" else "sinfoto"}"

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

    /** Solo algunos diseños tienen dónde poner la foto sin deformar la página. */
    val admiteFoto: Boolean get() = diseno != Diseno.MINIMA && diseno != Diseno.BLOQUES

    /**
     * Si es segura para los filtros automáticos (ATS) que usan las bolsas de trabajo
     * y casi todas las empresas grandes. Una plantilla a dos columnas se ve preciosa
     * y llega revuelta al otro lado, así que la app lo advierte en vez de esconderlo.
     */
    val aptaParaFiltros: Boolean get() = diseno.barra == Barra.NINGUNA
}

object Plantillas {

    val porDefecto = Plantilla()

    val combinaciones: Int
        get() = Diseno.entries.size * ColorAcento.entries.size * Fuente.entries.size * 2

    /** Reconstruye la plantilla desde el id guardado en el progreso. */
    fun porId(id: String): Plantilla {
        if (id.isBlank()) return porDefecto
        // Los ids viejos usaban `_`; se siguen leyendo para no perder la elección de nadie.
        val partes = if (id.contains('|')) id.split('|') else id.split('_')
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
