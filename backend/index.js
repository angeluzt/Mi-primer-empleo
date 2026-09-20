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

admin.initializeApp();
const db = admin.firestore();

const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");

const PAQUETE = "com.angeluzt.miprimerempleo";
const PRODUCTO_PASE = "pase_completo";
const PRODUCTO_RECARGA = "recarga_cv_10";
const CVS_INCLUIDOS = 15;
const CVS_POR_RECARGA = 10;
const MODELO = "gpt-4o-mini";

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

async function llamarOpenAI(mensajes, esquemaNombre) {
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

/**
 * La regla que define el producto: la IA NUNCA agrega logros, cifras ni responsabilidades
 * que la persona no haya dicho. Un CV inflado se cae en la primera entrevista y quema
 * la reputación de quien lo usó.
 */
const REGLA_SIN_INVENTOS = `
Regla absoluta e inviolable: solo puedes reformular y ordenar información que la persona
haya proporcionado explícitamente. Está terminantemente prohibido:
- Inventar logros, cifras, porcentajes o resultados.
- Agregar tecnologías, herramientas o responsabilidades no mencionadas.
- Inflar títulos de puesto o duraciones.
- Rellenar secciones vacías con contenido genérico plausible.
Si falta información para una sección, deja esa sección vacía. Es preferible un CV corto
y verdadero que uno largo e inventado.
Puedes mejorar la redacción, usar verbos de acción y estructurar con claridad.
`.trim();

exports.siguientePregunta = onRequest(opciones, async (req, res) => {
  try {
    const { purchaseToken, respuestas } = req.body || {};

    // Armar el CV es gratis: aquí no se cobra ni se verifica compra.
    // El muro está en generarCv (exportar), que es donde la persona ya invirtió su trabajo.
    const sistema = `
Eres un asesor de carrera que entrevista a alguien para armar su CV.
Haz UNA pregunta a la vez, corta y concreta, en español mexicano neutro y tono cercano.
Si la persona no tiene experiencia laboral, pregunta por proyectos propios, servicio social,
prácticas, cursos y trabajos ajenos a su carrera. Nunca la hagas sentir mal por no tener experiencia.
Responde SOLO con JSON:
{"campo":"id_del_campo","pregunta":"...","ayuda":"pista corta opcional","sugerencias":["opción","opción"],"terminado":false}
Marca terminado:true cuando ya tengas: datos de contacto, formación, y al menos experiencia o proyectos,
habilidades e idiomas.
`.trim();

    const salida = await llamarOpenAI([
      { role: "system", content: sistema },
      { role: "user", content: `Respuestas hasta ahora:\n${JSON.stringify(respuestas || {}, null, 2)}` },
    ]);

    res.json(salida);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: "No pudimos continuar la conversación." });
  }
});

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

    const sistema = `
Eres un redactor experto en CVs para el mercado laboral de México y Latinoamérica.
${REGLA_SIN_INVENTOS}

Genera DOS versiones del mismo CV: español ("es") e inglés ("en").
La versión en inglés no es traducción literal: usa convenciones del mercado angloparlante
(verbos de acción, sin foto, sin datos personales como edad o estado civil).

Cada logro debe seguir: verbo de acción + qué se hizo + resultado, usando SOLO cifras
que la persona haya dado. Sin cifras, describe la acción sin inventar resultados.

El resumen son 2 o 3 líneas, en primera persona implícita, sin adjetivos vacíos
("proactivo", "responsable", "trabajo bajo presión").

Responde SOLO con JSON con esta forma exacta:
{"es":{"idioma":"es","datos":{"nombre":"","puesto":"","ciudad":"","telefono":"","correo":"","linkedin":"","portafolio":""},"resumen":"","proyectos":[{"nombre":"","descripcion":"","enlace":"","logros":[]}],"experiencia":[{"puesto":"","organizacion":"","periodo":"","logros":[]}],"formacion":[{"titulo":"","institucion":"","periodo":"","nota":""}],"certificaciones":[{"nombre":"","institucion":"","anio":"","enlace":""}],"habilidades":[],"idiomas":[{"idioma":"","nivel":""}]},"en":{ ...misma estructura con "idioma":"en"... }}
`.trim();

    const entrada = [
      `Respuestas de la entrevista:\n${JSON.stringify(respuestas || {}, null, 2)}`,
      cvPegado ? `\nCV o texto que la persona pegó:\n${cvPegado}` : "",
    ].join("");

    const salida = await llamarOpenAI([
      { role: "system", content: sistema },
      { role: "user", content: entrada },
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
