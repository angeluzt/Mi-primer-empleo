package com.angeluzt.miprimerempleo.cv

import java.text.Normalizer

/**
 * Señales de fraude en el texto de una vacante, revisadas en el teléfono, sin IA y sin red.
 *
 * Las estafas de empleo tienen un repertorio corto y repetido: cobrar por la "capacitación",
 * pedir identificación o datos bancarios antes de entrevistar, prometer dinero fácil, mover la
 * plática a WhatsApp, reclutar en cadena. El módulo 5 de la guía las cuenta una por una; esto
 * las busca en la vacante que la persona está por mandar su CV, antes de gastar una generación.
 *
 * La IA también las revisa al adaptar, con más criterio. Esta capa existe porque es gratis,
 * instantánea, no depende de que el modelo obedezca, y funciona aunque no haya saldo.
 */
data class SenalEstafa(val explicacion: String, val fragmento: String)

object DetectorEstafas {

    private class Regla(val patron: Regex, val explicacion: String)

    private fun regla(patron: String, explicacion: String) =
        Regla(Regex(patron, RegexOption.IGNORE_CASE), explicacion)

    private val reglas = listOf(
        regla(
            "(pago|cuota|costo|cobro)\\s+(unico|inicial|de\\s+(inscripcion|registro|recuperacion|capacitacion|curso|material|uniforme|kit|examen))",
            "Te piden pagar algo para empezar. Un empleo te paga a ti, nunca al revés.",
        ),
        regla(
            "(deposit[oa]r?|inversion\\s+inicial|invertir\\s+\\$?\\d|compra\\s+(de\\s+)?(tu\\s+)?(kit|material|uniforme|paquete))",
            "Hablan de depósitos, inversión o comprar un kit. Es la forma más común de estafa de empleo.",
        ),
        regla(
            "(envia|manda|enviar|mandar|adjunta|adjuntar|compartir|comparte)\\s+(tu\\s+|una\\s+|copia\\s+de\\s+(tu\\s+)?)?(ine|identificacion|curp|rfc|dni|cedula|pasaporte|comprobante\\s+de\\s+domicilio|datos\\s+bancarios|numero\\s+de\\s+(cuenta|tarjeta)|clabe|nip|contrasena)",
            "Piden documentos o datos bancarios antes de entrevistarte. Eso se entrega hasta firmar contrato, en persona.",
        ),
        regla(
            "gana(s|r)?\\s+(hasta\\s+)?\\$?\\s?\\d[\\d.,]*\\s*(mil\\s+)?(pesos\\s+)?(diarios|al\\s+dia|por\\s+dia|semanales|a\\s+la\\s+semana|por\\s+semana)",
            "Prometen una cantidad alta por día o por semana. Si fuera tan fácil, no necesitarían anunciarlo así.",
        ),
        regla(
            "(ingresos\\s+ilimitados|libertad\\s+financiera|se\\s+tu\\s+propio\\s+jefe|dinero\\s+facil|sin\\s+experiencia\\s+(y\\s+)?gana)",
            "Usan frases de dinero fácil. Las empresas serias describen el puesto, no tu libertad financiera.",
        ),
        regla(
            "(multinivel|red\\s+de\\s+mercadeo|recluta(r)?\\s+(a\\s+)?(personas|gente|amigos)|arma\\s+tu\\s+(equipo|red)|invita\\s+a\\s+(tus\\s+)?(amigos|conocidos))",
            "El ingreso depende de reclutar a más gente. Eso es venta multinivel, no un empleo.",
        ),
        regla(
            "((solo|unicamente|exclusivamente)\\s+(por|via)\\s+(whatsapp|telegram|mensaje))|(contacto|informes|entrevista)\\s+(por|via)\\s+(telegram)",
            "Todo el contacto es por WhatsApp o Telegram. Una empresa real tiene correo, página o recepción.",
        ),
        regla(
            "(no\\s+(se\\s+)?requiere\\s+(experiencia|estudios)).{0,80}(\\$\\s?\\d{2}[\\d.,]{3,}|\\d{2}[\\d.,]{3,}\\s*(pesos|mxn|usd))",
            "Sueldo alto sin pedir nada a cambio. Revisa cuánto pagan puestos parecidos antes de seguir.",
        ),
    )

    fun revisar(vacante: String): List<SenalEstafa> {
        val texto = sinAcentos(vacante)
        return reglas.mapNotNull { regla ->
            regla.patron.find(texto)?.let { encontrado ->
                SenalEstafa(regla.explicacion, recortar(vacante, encontrado.range))
            }
        }
    }

    /** El fragmento original (con acentos) alrededor de lo encontrado, para mostrarlo. */
    private fun recortar(original: String, rango: IntRange): String {
        val inicio = (rango.first - 20).coerceAtLeast(0)
        val fin = (rango.last + 20).coerceAtMost(original.length - 1)
        val pedazo = original.substring(inicio, fin + 1).replace(Regex("\\s+"), " ").trim()
        return (if (inicio > 0) "…" else "") + pedazo + (if (fin < original.length - 1) "…" else "")
    }

    // Letra por letra, para que cada posición siga correspondiendo a la del texto original y el
    // fragmento que se muestra salga con sus acentos. "ñ" queda como "n", igual que en las reglas.
    private fun sinAcentos(texto: String): String = buildString(texto.length) {
        texto.forEach { letra ->
            val base = Normalizer.normalize(letra.toString(), Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
            append(if (base.length == 1) base else letra)
        }
    }
}
