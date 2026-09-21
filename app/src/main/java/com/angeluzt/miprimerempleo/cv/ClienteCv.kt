package com.angeluzt.miprimerempleo.cv

import android.content.Context
import com.angeluzt.miprimerempleo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
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
private data class PeticionRevision(
    val purchaseToken: String,
    val cv: Cv,
)

/**
 * Habla con nuestro backend, que es quien guarda la llave de OpenAI y verifica la compra.
 *
 * En compilaciones de depuración acepta además una llave pegada en Ajustes y llama a
 * OpenAI directamente, para poder probar en un teléfono real sin desplegar el backend.
 * Ese camino no existe en release: `LLAVE_LOCAL_PERMITIDA` vale false y el compilador
 * elimina la rama entera.
 */
class ClienteCv(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun prompt(archivo: String): String =
        context.assets.open(archivo).bufferedReader().use { it.readText() }.trim()

    suspend fun generar(
        purchaseToken: String,
        respuestas: Map<String, String>,
        cvPegado: String = "",
        llaveLocal: String = "",
    ): Result<ParCv> =
        if (usaLlaveLocal(llaveLocal)) {
            val entrada = buildString {
                append("Respuestas de la entrevista:\n")
                append(json.encodeToString(respuestas))
                if (cvPegado.isNotBlank()) append("\n\nCV o texto que la persona pegó:\n$cvPegado")
            }
            directo(llaveLocal, prompt("generar_cv.txt"), entrada)
                .mapCatching { NormalizadorCv.aParCv(it, respuestas) }
        } else {
            llamar(
                ruta = "generarCv",
                cuerpo = json.encodeToString(PeticionCv(purchaseToken, respuestas, cvPegado)),
            ).mapCatching { NormalizadorCv.aParCv(it, respuestas) }
        }

    /**
     * Le pide a la IA que revise el CV ya armado: si se quedó corto, si sobra relleno,
     * si el correo se ve serio y qué falta por contar.
     *
     * No gasta una generación: cuesta una fracción de lo que cuesta redactar el CV y
     * la persona necesita poder revisarlo cuantas veces quiera mientras lo mejora.
     */
    suspend fun evaluar(
        purchaseToken: String,
        cv: Cv,
        llaveLocal: String = "",
    ): Result<RevisionCv> =
        if (usaLlaveLocal(llaveLocal)) {
            directo(llaveLocal, prompt("evaluar_cv.txt"), json.encodeToString(cv))
                .mapCatching { NormalizadorCv.aRevision(it) }
        } else {
            llamar(
                ruta = "evaluarCv",
                cuerpo = json.encodeToString(PeticionRevision(purchaseToken, cv)),
            ).mapCatching { NormalizadorCv.aRevision(it) }
        }

    private fun usaLlaveLocal(llave: String) =
        BuildConfig.LLAVE_LOCAL_PERMITIDA && llave.isNotBlank()

    /** Solo en depuración. Llama a OpenAI con la llave que el desarrollador pegó. */
    private suspend fun directo(
        llave: String,
        sistema: String,
        entrada: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cuerpo = buildJsonObject {
                put("model", "gpt-4o-mini")
                put("temperature", 0.3)
                put("response_format", buildJsonObject { put("type", "json_object") })
                put(
                    "messages",
                    buildJsonArray {
                        add(buildJsonObject { put("role", "system"); put("content", sistema) })
                        add(buildJsonObject { put("role", "user"); put("content", entrada) })
                    },
                )
            }

            val conexion = (URL("https://api.openai.com/v1/chat/completions")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $llave")
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 90_000
            }
            conexion.outputStream.use { it.write(cuerpo.toString().toByteArray()) }

            val codigo = conexion.responseCode
            val flujo = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            val respuesta = flujo?.bufferedReader()?.use(BufferedReader::readText).orEmpty()

            if (codigo !in 200..299) {
                error(
                    when (codigo) {
                        401 -> "La llave de OpenAI no es válida. Revísala en Ajustes."
                        429 -> "OpenAI dice que te pasaste del límite o no tienes saldo."
                        else -> "OpenAI respondió $codigo. $respuesta"
                    }
                )
            }

            Json.parseToJsonElement(respuesta)
                .jsonObject["choices"]!!.jsonArray[0]
                .jsonObject["message"]!!
                .jsonObject["content"]!!.jsonPrimitive.content
        }
    }

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
