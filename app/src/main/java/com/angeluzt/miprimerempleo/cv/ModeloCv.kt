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

/**
 * CV de muestra para previsualizar plantillas antes de que la persona haya
 * armado el suyo. Es el perfil típico de quien usa esta app: sin experiencia
 * formal, con un proyecto propio y cursos.
 */
fun cvDeMuestra() = Cv(
    idioma = "es",
    datos = Datos(
        nombre = "Ana López Ramírez",
        puesto = "Analista de Datos Junior",
        ciudad = "Guadalajara, Jal.",
        telefono = "33 1234 5678",
        correo = "ana.lopez.datos@gmail.com",
        linkedin = "linkedin.com/in/analopezr",
        portafolio = "github.com/analopezr",
    ),
    resumen = "Recién egresada de Ingeniería Industrial con proyectos propios de análisis " +
        "de datos. Busco mi primera oportunidad en un equipo donde pueda aprender y aportar.",
    proyectos = listOf(
        Proyecto(
            nombre = "Control de inventario para papelería local",
            descripcion = "Sistema en hojas de cálculo con tablero de rotación de producto.",
            enlace = "github.com/analopezr/inventario",
            logros = listOf(
                "Reduje el desabasto de los 20 productos más vendidos de 8 a 2 casos por mes.",
                "Capacité a 3 personas para usarlo sin apoyo.",
            ),
        ),
    ),
    experiencia = listOf(
        Experiencia(
            puesto = "Practicante de Mejora Continua",
            organizacion = "Manufacturas del Valle",
            periodo = "Ene 2025 – Jun 2025",
            logros = listOf(
                "Medí tiempos de una línea de empaque y propuse un reacomodo.",
                "El cambio bajó el tiempo de ciclo cerca de 12%.",
            ),
        ),
    ),
    formacion = listOf(
        Formacion("Ingeniería Industrial", "Universidad de Guadalajara", "2020 – 2025"),
    ),
    certificaciones = listOf(
        Certificacion("Certificado de Análisis de Datos", "Google", "2025"),
        Certificacion("Excel Avanzado", "Microsoft Learn", "2025"),
    ),
    habilidades = listOf("Excel avanzado", "Power BI", "SQL básico", "Lean Manufacturing"),
    idiomas = listOf(IdiomaNivel("Español", "Nativo"), IdiomaNivel("Inglés", "B2 intermedio-alto")),
)
