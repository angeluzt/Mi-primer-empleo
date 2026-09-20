/**
 * Backend de Mi Primer Empleo.
 *
 * Existe por una sola razón de seguridad: la API key de OpenAI no puede ir en el APK.
 * Un APK se descompila en minutos y te vacían la cuenta. Aquí además se verifica que
 * quien pide una generación realmente compró el Pase, antes de gastar un solo token.
 */

const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const { google } = require("googleapis");
const {
  sistemaGenerar,
  entradaGenerar,
  MODELO,
} = require("./prompts");

admin.initializeApp();
const db = admin.firestore();

const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");

const PAQUETE = "com.angeluzt.miprimerempleo";
const PRODUCTO_PASE = "pase_completo";
const PRODUCTO_RECARGA = "recarga_cv_10";
const CVS_INCLUIDOS = 15;
const CVS_POR_RECARGA = 10;

const opciones = { secrets: [OPENAI_API_KEY], cors: true, region: "us-central1" };

/**
 * Verifica el token de compra contra Google Play.
 * Sin esto, cualquiera puede llamar al endpoint y gastar tu saldo de OpenAI gratis.
 */
async function verificarCompra(purchaseToken) {
  if (!purchaseToken) return { valido: false, motivo: "sin_token" };

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
    // purchaseState 0 = comprado. 1 = cancelado, 2 = pendiente.
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

async function llamarOpenAI(mensajes) {
  const respuesta = await fetch("https://api.openai.com/v1/chat/completions", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${OPENAI_API_KEY.value()}`,
    },
    body: JSON.stringify({
      model: MODELO,
      messages: mensajes,
      temperature: 0.3,
      response_format: { type: "json_object" },
    }),
  });

  if (!respuesta.ok) {
    const detalle = await respuesta.text();
    throw new Error(`OpenAI respondió ${respuesta.status}: ${detalle}`);
  }

  const datos = await respuesta.json();
  return JSON.parse(datos.choices[0].message.content);
}



exports.generarCv = onRequest(opciones, async (req, res) => {
  try {
    const { purchaseToken, respuestas, cvPegado } = req.body || {};

    const compra = await verificarCompra(purchaseToken);
    if (!compra.valido) {
      return res.status(402).json({ error: "Se requiere el Pase Completo.", motivo: compra.motivo });
    }

    const credito = await consumirCredito(purchaseToken);
    if (!credito.permitido) {
      return res.status(429).json({ error: "Generaciones agotadas. Compra una recarga." });
    }

    const salida = await llamarOpenAI([
      { role: "system", content: sistemaGenerar() },
      { role: "user", content: entradaGenerar(respuestas, cvPegado) },
    ]);

    res.json({ ...salida, restantes: credito.restantes });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: "No pudimos generar tu CV." });
  }
});

/** Google Play llama aquí cuando alguien compra una recarga, para acreditarla. */
exports.acreditarRecarga = onRequest(opciones, async (req, res) => {
  try {
    const { purchaseToken, recargaToken } = req.body || {};

    const compra = await verificarCompra(purchaseToken);
    if (!compra.valido) return res.status(402).json({ error: "Pase no válido." });

    const auth = new google.auth.GoogleAuth({
      scopes: ["https://www.googleapis.com/auth/androidpublisher"],
    });
    const publisher = google.androidpublisher({ version: "v3", auth });
    const recarga = await publisher.purchases.products.get({
      packageName: PAQUETE,
      productId: PRODUCTO_RECARGA,
      token: recargaToken,
    });

    if (recarga.data.purchaseState !== 0) {
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
    console.error(error);
    res.status(500).json({ error: "No pudimos acreditar la recarga." });
  }
});
