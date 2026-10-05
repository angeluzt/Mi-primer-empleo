/**
 * Backend de Conseguir Trabajo: Mi primer empleo.
 *
 * Existe por una razón de seguridad: la API key de OpenAI no puede ir en el APK. Un APK se
 * descompila en minutos y te vacían la cuenta. Aquí además se verifica que quien pide una
 * generación realmente compró el Pase, antes de gastar un solo token, y se limita cuánto
 * puede pedir cada compra.
 */

const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const { google } = require("googleapis");
const {
  config,
  esquema,
  mensajesGenerar,
  mensajesEvaluar,
  mensajesAdaptar,
  MODELOS,
} = require("./prompts");

admin.initializeApp();
const db = admin.firestore();

const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");

const PAQUETE = "com.angeluzt.miprimerempleo";
const PRODUCTO_PASE = "pase_completo";
const PRODUCTO_RECARGA = "recarga_cv_10";
const CVS_INCLUIDOS = 15;
const CVS_POR_RECARGA = 10;
// Revisar no gasta generaciones, pero tampoco puede ser infinito: con un token válido
// alguien podría usar el endpoint como un ChatGPT gratis.
const REVISIONES_MAXIMAS = 150;
const ESPERA_OPENAI_MS = 60_000;

// cors: false. La app es nativa y no lo necesita; dejarlo abierto permitía llamar al
// backend desde cualquier página web con un token robado.
const opciones = { secrets: [OPENAI_API_KEY], cors: false, region: "us-central1" };

class ErrorHttp extends Error {
  constructor(estado, mensaje) {
    super(mensaje);
    this.estado = estado;
  }
}

/**
 * Verifica el token de compra contra Google Play.
 * Sin esto, cualquiera puede llamar al endpoint y gastar tu saldo de OpenAI gratis.
 */
async function verificarCompra(purchaseToken) {
  if (!purchaseToken || typeof purchaseToken !== "string") return { valido: false, motivo: "sin_token" };

  const auth = new google.auth.GoogleAuth({
    scopes: ["https://www.googleapis.com/auth/androidpublisher"],
  });
  const publisher = google.androidpublisher({ version: "v3", auth });

  try {
    const respuesta = await publisher.purchases.products.get({
      packageName: PAQUETE,
      productId: PRODUCTO_PASE,
      token: purchaseToken,
    });
    // purchaseState 0 = comprado. 1 = cancelado (reembolso incluido), 2 = pendiente.
    const comprado = respuesta.data.purchaseState === 0;
    return { valido: comprado, motivo: comprado ? null : "compra_no_activa" };
  } catch (error) {
    console.error("Verificación de compra falló:", error.message);
    return { valido: false, motivo: "token_invalido" };
  }
}

/** Cuenta generaciones por token de compra para que nadie abuse del endpoint. */
async function consumirCredito(purchaseToken) {
  const ref = db.collection("usos").doc(purchaseToken);
  return db.runTransaction(async (tx) => {
    const doc = await tx.get(ref);
    const usados = doc.exists ? doc.data().generaciones || 0 : 0;
    const recargas = doc.exists ? doc.data().recargas || 0 : 0;
    const limite = CVS_INCLUIDOS + recargas * CVS_POR_RECARGA;

    if (usados >= limite) return { permitido: false, restantes: 0 };

    tx.set(
      ref,
      {
        generaciones: usados + 1,
        recargas,
        actualizado: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );
    return { permitido: true, restantes: limite - usados - 1 };
  });
}

/** Devuelve la generación si OpenAI falló: la persona no pagó por un error nuestro. */
async function devolverCredito(purchaseToken) {
  await db
    .collection("usos")
    .doc(purchaseToken)
    .set({ generaciones: admin.firestore.FieldValue.increment(-1) }, { merge: true });
}

async function contarRevision(purchaseToken) {
  const ref = db.collection("usos").doc(purchaseToken);
  return db.runTransaction(async (tx) => {
    const doc = await tx.get(ref);
    const revisiones = doc.exists ? doc.data().revisiones || 0 : 0;
    if (revisiones >= REVISIONES_MAXIMAS) return false;
    tx.set(ref, { revisiones: revisiones + 1 }, { merge: true });
    return true;
  });
}

/**
 * Llama a OpenAI con salidas estructuradas: el modelo no puede devolver otra forma que la del
 * esquema. Prueba los modelos en orden; si uno no existe o no soporta el esquema, sigue.
 */
async function llamarOpenAI(mensajes, nombreEsquema, tarea) {
  let ultimo;
  for (const modelo of MODELOS) {
    const control = new AbortController();
    const reloj = setTimeout(() => control.abort(), ESPERA_OPENAI_MS);
    try {
      const respuesta = await fetch("https://api.openai.com/v1/chat/completions", {
        method: "POST",
        signal: control.signal,
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${OPENAI_API_KEY.value()}`,
        },
        body: JSON.stringify({
          model: modelo,
          messages: mensajes,
          temperature: config.temperatura[tarea] ?? 0.3,
          response_format: { type: "json_schema", json_schema: esquema(nombreEsquema) },
        }),
      });

      if (!respuesta.ok) {
        const detalle = await respuesta.text();
        const modeloAjeno =
          respuesta.status === 404 ||
          /model_not_found|does not exist|do not have access/.test(detalle) ||
          (/response_format/.test(detalle) && /not supported/.test(detalle));
        ultimo = new Error(`OpenAI respondió ${respuesta.status} con ${modelo}: ${detalle.slice(0, 300)}`);
        if (modeloAjeno) continue;
        throw ultimo;
      }

      const datos = await respuesta.json();
      const mensaje = datos.choices[0].message;
      if (mensaje.refusal) throw new ErrorHttp(422, "La IA no quiso procesar este texto.");
      return JSON.parse(mensaje.content);
    } finally {
      clearTimeout(reloj);
    }
  }
  throw ultimo || new Error("No hay modelos configurados.");
}

// ---------- Validación de entrada ----------

const largo = (valor) => JSON.stringify(valor ?? "").length;

function validarRespuestas(respuestas) {
  if (!respuestas || typeof respuestas !== "object" || Array.isArray(respuestas)) {
    throw new ErrorHttp(400, "Faltan las respuestas de la entrevista.");
  }
  if (Object.keys(respuestas).length > 30) throw new ErrorHttp(400, "Demasiados campos.");
  for (const valor of Object.values(respuestas)) {
    if (typeof valor !== "string") throw new ErrorHttp(400, "Las respuestas deben ser texto.");
  }
  if (largo(respuestas) > config.limites.respuestas) throw new ErrorHttp(413, "Las respuestas son demasiado largas.");
}

function validarCv(cv) {
  if (!cv || typeof cv !== "object" || !cv.datos) throw new ErrorHttp(400, "Falta el CV.");
  if (largo(cv) > config.limites.cv) throw new ErrorHttp(413, "El CV es demasiado largo.");
}

function responderError(res, error, mensajePorDefecto) {
  if (error instanceof ErrorHttp) return res.status(error.estado).json({ error: error.message });
  console.error(error);
  return res.status(500).json({ error: mensajePorDefecto });
}

function soloPost(req, res) {
  if (req.method !== "POST") {
    res.status(405).json({ error: "Solo POST." });
    return false;
  }
  return true;
}

// ---------- Endpoints ----------

exports.generarCv = onRequest(opciones, async (req, res) => {
  if (!soloPost(req, res)) return;
  const { purchaseToken, respuestas, pais, cvPegado } = req.body || {};
  let cobrado = false;
  try {
    validarRespuestas(respuestas);
    if (cvPegado && String(cvPegado).length > config.limites.vacante) {
      throw new ErrorHttp(413, "El texto pegado es demasiado largo.");
    }

    const compra = await verificarCompra(purchaseToken);
    if (!compra.valido) {
      return res.status(402).json({ error: "Se requiere el Pase Completo.", motivo: compra.motivo });
    }

    const credito = await consumirCredito(purchaseToken);
    if (!credito.permitido) {
      return res.status(429).json({ error: "Generaciones agotadas. Compra una recarga." });
    }
    cobrado = true;

    const salida = await llamarOpenAI(mensajesGenerar(respuestas, pais, cvPegado), "cv", "generar");
    res.json({ ...salida, restantes: credito.restantes });
  } catch (error) {
    if (cobrado) await devolverCredito(purchaseToken).catch(() => {});
    responderError(res, error, "No pudimos generar tu CV.");
  }
});

/**
 * Revisa un CV ya generado: si se quedó corto, si el correo se ve serio, qué falta.
 * No consume una generación a propósito: la persona necesita poder revisarlo cada vez que
 * le agrega algo. Tiene su propio tope para que no se use como un chat gratis.
 */
exports.evaluarCv = onRequest(opciones, async (req, res) => {
  if (!soloPost(req, res)) return;
  try {
    const { purchaseToken, cv } = req.body || {};
    validarCv(cv);

    const compra = await verificarCompra(purchaseToken);
    if (!compra.valido) {
      return res.status(402).json({ error: "Se requiere el Pase Completo.", motivo: compra.motivo });
    }
    if (!(await contarRevision(purchaseToken))) {
      return res.status(429).json({ error: "Llegaste al límite de revisiones." });
    }

    res.json(await llamarOpenAI(mensajesEvaluar(cv), "revision", "evaluar"));
  } catch (error) {
    responderError(res, error, "No pudimos revisar tu CV.");
  }
});

/**
 * Adapta el CV a una vacante. Gasta una generación: el trabajo y el costo son los de redactar
 * un CV entero. La vacante es texto de un tercero: va delimitada como datos y el prompt la
 * revisa también en busca de señales de fraude.
 */
exports.adaptarCv = onRequest(opciones, async (req, res) => {
  if (!soloPost(req, res)) return;
  const { purchaseToken, cv, vacante, pais } = req.body || {};
  let cobrado = false;
  try {
    validarCv(cv);
    if (!vacante || typeof vacante !== "string" || vacante.trim().length < 40) {
      throw new ErrorHttp(400, "Pega el texto completo de la vacante.");
    }
    if (vacante.length > config.limites.vacante) throw new ErrorHttp(413, "La vacante es demasiado larga.");

    const compra = await verificarCompra(purchaseToken);
    if (!compra.valido) {
      return res.status(402).json({ error: "Se requiere el Pase Completo.", motivo: compra.motivo });
    }
    const credito = await consumirCredito(purchaseToken);
    if (!credito.permitido) {
      return res.status(429).json({ error: "Generaciones agotadas. Compra una recarga." });
    }
    cobrado = true;

    const salida = await llamarOpenAI(mensajesAdaptar(cv, vacante, pais), "adaptacion", "adaptar");
    res.json({ ...salida, restantes: credito.restantes });
  } catch (error) {
    if (cobrado) await devolverCredito(purchaseToken).catch(() => {});
    responderError(res, error, "No pudimos adaptar tu CV.");
  }
});

/** La app llama aquí cuando alguien compra una recarga, antes de consumirla en Play. */
exports.acreditarRecarga = onRequest(opciones, async (req, res) => {
  if (!soloPost(req, res)) return;
  try {
    const { purchaseToken, recargaToken } = req.body || {};
    if (!recargaToken || typeof recargaToken !== "string") throw new ErrorHttp(400, "Falta la recarga.");

    const compra = await verificarCompra(purchaseToken);
    if (!compra.valido) return res.status(402).json({ error: "Pase no válido." });

    const auth = new google.auth.GoogleAuth({
      scopes: ["https://www.googleapis.com/auth/androidpublisher"],
    });
    const publisher = google.androidpublisher({ version: "v3", auth });
    const recarga = await publisher.purchases.products
      .get({ packageName: PAQUETE, productId: PRODUCTO_RECARGA, token: recargaToken })
      .catch(() => null);

    if (!recarga || recarga.data.purchaseState !== 0) {
      return res.status(402).json({ error: "Recarga no válida." });
    }

    // El token de la recarga es la llave de idempotencia: una recarga se acredita una sola vez.
    const refRecarga = db.collection("recargas").doc(recargaToken);
    const acreditada = await db.runTransaction(async (tx) => {
      if ((await tx.get(refRecarga)).exists) return false;
      tx.set(refRecarga, { purchaseToken, fecha: admin.firestore.FieldValue.serverTimestamp() });
      tx.set(
        db.collection("usos").doc(purchaseToken),
        { recargas: admin.firestore.FieldValue.increment(1) },
        { merge: true }
      );
      return true;
    });

    res.json({ acreditada });
  } catch (error) {
    responderError(res, error, "No pudimos acreditar la recarga.");
  }
});
