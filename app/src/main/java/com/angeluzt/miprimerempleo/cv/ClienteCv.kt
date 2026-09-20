package com.angeluzt.miprimerempleo.cv

import com.angeluzt.miprimerempleo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

@Serializable
private data class PeticionCv(
    val purchaseToken: String,
    val respuestas: Map<String, String>,
    val cvPegado: String = "",
)

@Serializable
private data class PeticionPregunta(
    val purchaseToken: String,
    val respuestas: Map<String, String>,
)

@Serializable
data class SiguientePregunta(
    val campo: String,
    val pregunta: String,
    val ayuda: String = "",
    val sugerencias: List<String> = emptyList(),
    val terminado: Boolean = false,
)

/**
 * Habla con nuestro backend, nunca con OpenAI directamente:
 * la API key vive solo en el servidor y ahí se verifica la compra antes de gastar tokens.
 */
class ClienteCv {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun siguientePregunta(
        purchaseToken: String,
        respuestas: Map<String, String>,
    ): Result<SiguientePregunta> = llamar(
        ruta = "siguientePregunta",
        cuerpo = json.encodeToString(PeticionPregunta(purchaseToken, respuestas)),
    ).mapCatching { json.decodeFromString<SiguientePregunta>(it) }

    suspend fun generar(
        purchaseToken: String,
        respuestas: Map<String, String>,
        cvPegado: String = "",
    ): Result<ParCv> = llamar(
        ruta = "generarCv",
        cuerpo = json.encodeToString(PeticionCv(purchaseToken, respuestas, cvPegado)),
    ).mapCatching { json.decodeFromString<ParCv>(it) }

    private suspend fun llamar(ruta: String, cuerpo: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conexion = (URL("${BuildConfig.BACKEND_URL}/$ruta").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 15_000
                    readTimeout = 90_000
                }
                conexion.outputStream.use { it.write(cuerpo.toByteArray()) }

                val codigo = conexion.responseCode
                val flujo = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
                val respuesta = flujo?.bufferedReader()?.use(BufferedReader::readText).orEmpty()

                if (codigo !in 200..299) {
                    error(
                        when (codigo) {
                            402 -> "Necesitas el Pase Completo para generar tu CV."
                            429 -> "Se te acabaron las generaciones incluidas. Puedes comprar una recarga."
                            else -> "No pudimos generar tu CV. Revisa tu conexión e inténtalo de nuevo."
                        }
                    )
                }
                respuesta
            }
        }
}
