package com.angeluzt.miprimerempleo.cv

import kotlinx.serialization.Serializable

/**
 * La IA devuelve este JSON, no un PDF ni texto libre.
 * Así el usuario puede editar cada campo sin volver a pagar una generación,
 * y la app maqueta el PDF en el teléfono.
 */
@Serializable
data class Cv(
    val idioma: String,
    val datos: Datos,
    val resumen: String,
    val proyectos: List<Proyecto> = emptyList(),
    val experiencia: List<Experiencia> = emptyList(),
    val formacion: List<Formacion> = emptyList(),
    val certificaciones: List<Certificacion> = emptyList(),
    val habilidades: List<String> = emptyList(),
    val idiomas: List<IdiomaNivel> = emptyList(),
)

@Serializable
data class Datos(
    val nombre: String,
    val puesto: String,
    val ciudad: String = "",
    val telefono: String = "",
    val correo: String = "",
    val linkedin: String = "",
    val portafolio: String = "",
)

@Serializable
data class Proyecto(
    val nombre: String,
    val descripcion: String,
    val enlace: String = "",
    val logros: List<String> = emptyList(),
)

@Serializable
data class Experiencia(
    val puesto: String,
    val organizacion: String,
    val periodo: String,
    val logros: List<String> = emptyList(),
)

@Serializable
data class Formacion(
    val titulo: String,
    val institucion: String,
    val periodo: String,
    val nota: String = "",
)

@Serializable
data class Certificacion(
    val nombre: String,
    val institucion: String,
    val anio: String,
    val enlace: String = "",
)

@Serializable
data class IdiomaNivel(val idioma: String, val nivel: String)

@Serializable
data class ParCv(val es: Cv, val en: Cv)

enum class Plantilla(val etiqueta: String, val conFoto: Boolean, val descripcion: String) {
    ATS("Clásica ATS", false, "Sin foto ni columnas. La que debes subir a bolsas de trabajo."),
    CON_FOTO("Con foto", true, "Para entrega en mano y empresas pequeñas."),
    COMPACTA("Compacta", false, "Cuando tienes mucho que contar y quieres que quepa en una página."),
}
