package com.angeluzt.miprimerempleo.cv

import android.content.Context
import com.angeluzt.miprimerempleo.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

@Serializable
private data class PeticionCv(
    val purchaseToken: String,
    val respuestas: Map<String, String>,
    val pais: String = "",
    val cvPegado: String = "",
)

@Serializable
private data class PeticionRevision(
    val purchaseToken: String,
    val cv: Cv,
)

@Serializable
private data class PeticionAdaptacion(
    val purchaseToken: String,
    val cv: Cv,
    val vacante: String,
    val pais: String,
)

@Serializable
private data class PeticionRecarga(
    val purchaseToken: String,
    val recargaToken: String,
)

/** Lo que salió de la generación, y qué datos personales se quitaron antes de mandarla. */
data class Generacion(val par: ParCv, val tachados: List<DatoSensible>)

/**
 * Habla con nuestro backend, que es quien guarda la llave de OpenAI y verifica la compra.
 *
 * En compilaciones de depuración acepta además una llave pegada en Ajustes y llama a
 * OpenAI directamente, para poder probar en un teléfono real sin desplegar el backend.
 * Ese camino no existe en release: `LLAVE_LOCAL_PERMITIDA` vale false y el compilador
 * elimina la rama entera.
 *
 * Las protecciones viven aquí, donde no se pueden olvidar:
 * - Los identificadores personales (CURP, RFC, DNI…) se tachan antes de salir del teléfono.
 * - Nombre, teléfono, correo y enlaces no se mandan cuando la IA no los necesita, y se
 *   reponen aquí desde lo que escribió la persona.
 * - El texto de la persona y el de las vacantes va delimitado como datos, nunca como órdenes.
 * - La respuesta de la IA se pide con esquema estricto y además se lee con el normalizador.
 */
class ClienteCv(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun asset(archivo: String): String =
        context.assets.open(archivo).bufferedReader().use { it.readText() }.trim()

    private val config: JsonObject by lazy { Json.parseToJsonElement(asset("modelos.json")).jsonObject }

    private val modelos: List<String> by lazy {
        config["modelos"]?.jsonArray?.map { it.jsonPrimitive.content } ?: listOf("gpt-4o-mini")
    }

    private fun temperatura(tarea: String): Double =
        config["temperatura"]?.jsonObject?.get(tarea)?.jsonPrimitive?.doubleOrNull ?: 0.3

    private fun limite(nombre: String, porDefecto: Int): Int =
        config["limites"]?.jsonObject?.get(nombre)?.jsonPrimitive?.intOrNull ?: porDefecto

    /**
     * El esquema para las salidas estructuradas de OpenAI, con la definición del CV inyectada
     * desde cv.json: así hay una sola definición del CV para la app, el backend y las pruebas.
     */
    private fun esquema(nombre: String): JsonObject {
        val texto = asset("esquemas/$nombre.json")
        val base = Json.parseToJsonElement(texto).jsonObject
        if (nombre == "cv" || "#/\$defs/cv" !in texto) return base
        val definiciones = Json.parseToJsonElement(asset("esquemas/cv.json"))
            .jsonObject.getValue("schema").jsonObject.getValue("\$defs")
        val schema = base.getValue("schema").jsonObject
        return JsonObject(base + ("schema" to JsonObject(schema + ("\$defs" to definiciones))))
    }

    // ---------- Generar ----------

    suspend fun generar(
        purchaseToken: String,
        respuestas: Map<String, String>,
        pais: String,
        cvPegado: String = "",
        llaveLocal: String = "",
    ): Result<Generacion> {
        val maximo = limite("respuesta", 3000)
        val (limpias, tachados) = ProteccionDatos.prepararRespuestas(
            respuestas.mapValues { Delimitador.recortar(it.value, maximo) },
        )
        val pegado = ProteccionDatos.tachar(cvPegado).first
        // Nombre, teléfono, correo y enlaces no viajan: la IA no los necesita para redactar.
        val aEnviar = ProteccionDatos.sinContacto(limpias)

        return if (usaLlaveLocal(llaveLocal)) {
            val entrada = buildString {
                append("País donde busca trabajo: ").append(nombrePais(pais)).append("\n\n")
                append(Delimitador.envolver("respuestas", json.encodeToString(aEnviar)))
                if (pegado.isNotBlank()) append("\n\n").append(Delimitador.envolver("cv_pegado", pegado))
            }
            directo(llaveLocal, "generar", asset("generar_cv.txt"), esquema("cv"), entrada)
        } else {
            llamar("generarCv", json.encodeToString(PeticionCv(purchaseToken, aEnviar, pais, pegado)))
        }.mapCatching {
            val par = NormalizadorCv.aParCv(it, limpias)
            Generacion(NormalizadorCv.conContactoReal(par, limpias), tachados)
        }
    }

    // ---------- Revisar ----------

    /**
     * Le pide a la IA que revise el CV ya armado. No gasta una generación: cuesta una
     * fracción de redactarlo y la persona necesita revisarlo cada vez que le agrega algo.
     */
    suspend fun evaluar(
        purchaseToken: String,
        cv: Cv,
        llaveLocal: String = "",
    ): Result<RevisionCv> =
        if (usaLlaveLocal(llaveLocal)) {
            directo(
                llaveLocal, "evaluar", asset("evaluar_cv.txt"), esquema("revision"),
                Delimitador.envolver("cv", json.encodeToString(ProteccionDatos.paraRevisar(cv))),
            )
        } else {
            llamar("evaluarCv", json.encodeToString(PeticionRevision(purchaseToken, ProteccionDatos.paraRevisar(cv))))
        }.mapCatching { NormalizadorCv.aRevision(it) }

    // ---------- Adaptar a una vacante ----------

    /** Gasta una generación: el trabajo y el costo son los de redactar un CV entero. */
    suspend fun adaptar(
        purchaseToken: String,
        cv: Cv,
        vacante: String,
        pais: String,
        llaveLocal: String = "",
    ): Result<Adaptacion> {
        // La vacante también se tacha: a veces la gente pega el correo de respuesta con sus datos.
        val texto = ProteccionDatos.tachar(Delimitador.recortar(vacante.trim(), limite("vacante", 8000))).first
        // Para adaptar no hace falta ningún dato de contacto; el CV adaptado recupera los reales.
        val oculto = ProteccionDatos.paraAdaptar(cv)
        return if (usaLlaveLocal(llaveLocal)) {
            val entrada = buildString {
                append("País donde busca trabajo: ").append(nombrePais(pais)).append("\n\n")
                append(Delimitador.envolver("cv", json.encodeToString(oculto))).append("\n\n")
                append(Delimitador.envolver("vacante", texto))
            }
            directo(llaveLocal, "adaptar", asset("adaptar_cv.txt"), esquema("adaptacion"), entrada)
        } else {
            llamar("adaptarCv", json.encodeToString(PeticionAdaptacion(purchaseToken, oculto, texto, pais)))
        }.mapCatching {
            NormalizadorCv.aAdaptacion(it, cv, texto, UUID.randomUUID().toString(), System.currentTimeMillis())
        }
    }

    // ---------- Recargas ----------

    /**
     * Le avisa al backend que esta persona compró una recarga, para que suba su límite
     * de generaciones. El backend verifica la compra con Google y no la cuenta dos veces.
     */
    suspend fun acreditarRecarga(tokenPase: String, tokenRecarga: String): Result<Unit> =
        llamar(
            ruta = "acreditarRecarga",
            cuerpo = json.encodeToString(PeticionRecarga(tokenPase, tokenRecarga)),
        ).map { }

    // ---------- Transporte ----------

    private fun usaLlaveLocal(llave: String) =
        BuildConfig.LLAVE_LOCAL_PERMITIDA && llave.isNotBlank()

    private fun nombrePais(codigo: String) = when (codigo) {
        "MX" -> "México"
        "CO" -> "Colombia"
        "AR" -> "Argentina"
        "CL" -> "Chile"
        "PE" -> "Perú"
        "EC" -> "Ecuador"
        else -> "Latinoamérica (sin país específico)"
    }

    /**
     * Solo en depuración. Llama a OpenAI con la llave que el desarrollador pegó.
     * Prueba los modelos en orden: si uno no existe en esa cuenta o no soporta salidas
     * estructuradas, pasa al siguiente en vez de fallar.
     */
    private suspend fun directo(
        llave: String,
        tarea: String,
        sistema: String,
        esquema: JsonObject,
        entrada: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        var ultimo: Throwable = IllegalStateException("No hay modelos configurados.")
        for (modelo in modelos) {
            val intento = runCatching { pedirAOpenAi(llave, modelo, tarea, sistema, esquema, entrada) }
            val fallo = intento.exceptionOrNull() ?: return@withContext intento
            ultimo = fallo
            if (fallo !is ModeloNoDisponible) return@withContext intento
        }
        Result.failure(ultimo)
    }

    private class ModeloNoDisponible(mensaje: String) : Exception(mensaje)

    private fun pedirAOpenAi(
        llave: String,
        modelo: String,
        tarea: String,
        sistema: String,
        esquema: JsonObject,
        entrada: String,
    ): String {
        val cuerpo = buildJsonObject {
            put("model", modelo)
            put("temperature", temperatura(tarea))
            put(
                "response_format",
                buildJsonObject {
                    put("type", "json_schema")
                    put("json_schema", esquema)
                },
            )
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
            val modeloAjeno = codigo == 404 || respuesta.contains("model_not_found") ||
                respuesta.contains("does not exist") || respuesta.contains("do not have access") ||
                (respuesta.contains("response_format") && respuesta.contains("not supported"))
            if (modeloAjeno) throw ModeloNoDisponible("El modelo $modelo no está disponible.")
            error(
                when (codigo) {
                    401 -> "La llave de OpenAI no es válida. Revísala en Ajustes."
                    429 -> "OpenAI dice que te pasaste del límite o no tienes saldo."
                    else -> "OpenAI respondió $codigo. Inténtalo otra vez en un momento."
                }
            )
        }

        val mensaje = Json.parseToJsonElement(respuesta)
            .jsonObject["choices"]!!.jsonArray[0]
            .jsonObject["message"]!!.jsonObject
        // Con salidas estructuradas, si el modelo se niega viene "refusal" en vez de contenido.
        mensaje["refusal"]?.jsonPrimitive?.takeIf { it.isString }?.let {
            error("La IA no quiso procesar este texto. Revisa que no tenga contenido fuera de lugar.")
        }
        return mensaje["content"]!!.jsonPrimitive.content
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
                            402 -> "Necesitas el Pase Completo para usar la IA."
                            413 -> "El texto es demasiado largo. Recórtalo un poco e inténtalo otra vez."
                            429 -> "Se te acabaron las generaciones incluidas. Puedes comprar una recarga."
                            else -> "No pudimos completar la operación. Revisa tu conexión e inténtalo de nuevo."
                        }
                    )
                }
                respuesta
            }
        }
}
