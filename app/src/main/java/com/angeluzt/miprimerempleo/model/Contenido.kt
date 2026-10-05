package com.angeluzt.miprimerempleo.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class Indice(
    val version: Int,
    val pais: String,
    val tesis: Tesis,
    val rutas: List<Ruta>,
    val paises: List<Pais> = emptyList(),
    val niveles: List<Nivel>,
    val modulos: List<ModuloMeta>,
)

@Serializable
data class Tesis(val titulo: String, val bloques: List<Bloque>)

@Serializable
data class Ruta(
    val id: String,
    val titulo: String,
    val subtitulo: String,
    val icono: String,
    val promesa: String,
    val mensaje: String,
    val orden: List<String>,
)

@Serializable
data class Nivel(
    val nivel: Int,
    val titulo: String,
    val descripcion: String,
    val puntosMinimos: Int,
)

@Serializable
data class ModuloMeta(
    val id: String,
    val archivo: String,
    val numero: Int,
    val titulo: String,
    val subtitulo: String,
    val icono: String,
    val gratis: Boolean,
)

@Serializable
data class Modulo(
    val id: String,
    val titulo: String,
    val intro: String,
    val capitulos: List<Capitulo>,
)

@Serializable
data class Capitulo(
    val id: String,
    val titulo: String,
    val minutos: Int,
    val gratis: Boolean,
    val bloques: List<Bloque>,
)

/**
 * Los bloques son la unidad de contenido. El mismo JSON alimenta la app y el libro PDF,
 * por eso cada tipo se serializa con su discriminador "tipo".
 */
@Serializable
@JsonClassDiscriminator("tipo")
sealed interface Bloque

@Serializable
@SerialName("parrafo")
data class Parrafo(val texto: String) : Bloque

@Serializable
@SerialName("titulo")
data class Subtitulo(val texto: String) : Bloque

@Serializable
@SerialName("cita")
data class Cita(val texto: String) : Bloque

@Serializable
@SerialName("lista")
data class Lista(val items: List<String>, val ordenada: Boolean = false) : Bloque

@Serializable
@SerialName("tabla")
data class Tabla(val encabezados: List<String>, val filas: List<List<String>>) : Bloque

@Serializable
@SerialName("alerta")
data class Alerta(val nivel: String, val titulo: String, val texto: String) : Bloque

@Serializable
@SerialName("banderas")
data class Banderas(val rojas: List<String>, val verdes: List<String>) : Bloque

@Serializable
@SerialName("accion")
data class AccionBloque(
    val accionId: String,
    val texto: String,
    val puntos: Int,
) : Bloque

@Serializable
@SerialName("recursos")
data class Recursos(val items: List<Recurso>) : Bloque

@Serializable
data class Recurso(
    val nombre: String,
    val descripcion: String,
    val url: String,
    val afiliado: Boolean = false,
    val gratis: Boolean = false,
)

/**
 * Contenido que cambia según el país: instituciones, prestaciones, bolsas de trabajo.
 * Se resuelve uno solo al renderizar, el del país elegido, con "generico" de respaldo.
 * Existe como bloque aparte para no llenar los otros nueve tipos de condicionales.
 */
@Serializable
@SerialName("regional")
data class Regional(
    val titulo: String,
    val porPais: Map<String, ContenidoPais>,
) : Bloque {
    fun para(pais: String): ContenidoPais? = porPais[pais] ?: porPais["generico"]
}

@Serializable
data class ContenidoPais(
    val texto: String = "",
    val items: List<String> = emptyList(),
    val recursos: List<Recurso> = emptyList(),
)

@Serializable
data class Pais(val codigo: String, val nombre: String, val moneda: String)

@Serializable
@SerialName("comparacion")
data class Comparacion(
    val titulo: String,
    val izquierda: Columna,
    val derecha: Columna,
) : Bloque

@Serializable
data class Columna(val nombre: String, val etiqueta: String, val puntos: List<String>)
