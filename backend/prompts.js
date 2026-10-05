/**
 * Prompts, esquemas y armado de mensajes, compartidos entre la Cloud Function y el script
 * de pruebas. La app empaqueta la misma carpeta /prompts dentro del APK.
 *
 * Si se duplicaran, al tercer ajuste estarían diciendo cosas distintas y se probaría algo
 * que no es lo que corre en producción.
 */

const fs = require("fs");
const path = require("path");

const DIR = path.resolve(__dirname, "..", "prompts");
const leer = (archivo) => fs.readFileSync(path.join(DIR, archivo), "utf8").trim();
const leerJson = (archivo) => JSON.parse(leer(archivo));

const config = leerJson("modelos.json");

/** El esquema de salidas estructuradas, con la definición del CV inyectada desde cv.json. */
function esquema(nombre) {
  const base = leerJson(`esquemas/${nombre}.json`);
  if (nombre === "cv" || !JSON.stringify(base).includes("#/$defs/cv")) return base;
  const cv = leerJson("esquemas/cv.json");
  base.schema.$defs = cv.schema.$defs;
  return base;
}

// ---------- Datos personales ----------
// Igual que ProteccionDatos.kt: solo lo inequívoco. La app ya los tacha antes de mandarlos;
// esto cubre a quien llame al backend sin pasar por ella.

const TACHADO = "[dato personal omitido]";
const PATRONES = [
  [/\b[A-Z][AEIOUX][A-Z]{2}\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])[HMX][A-Z]{2}[B-DF-HJ-NP-TV-Z]{3}[A-Z\d]\d\b/gi, 0],
  [/\b[A-ZÑ&]{4}\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])[A-Z\d]{2}[A\d]\b/gi, 0],
  [/\b\d{1,2}\.\d{3}\.\d{3}-[\dK]\b|\b\d{7,8}-[\dK]\b/gi, 0],
  [/(\bNSS\b|seguro social|\bIMSS\b)\D{0,15}(\d[\d -]{9,13}\d)/gi, 2],
  [/(\bD\.?N\.?I\.?)\D{0,10}(\d{1,2}\.?\d{3}\.?\d{3})\b/gi, 2],
  [/(c[ée]dula|\bC\.?C\.?)(\s+de\s+ciudadan[ií]a)?\D{0,10}(\d[\d.]{5,13}\d)\b/gi, 3],
  [/(pasaporte|passport)\W{0,10}([A-Z]{0,3}\d{6,9})\b/gi, 2],
];

function tachar(texto) {
  let limpio = String(texto || "");
  for (const [patron, grupo] of PATRONES) {
    limpio = limpio.replace(patron, (todo, ...grupos) =>
      grupo === 0 ? TACHADO : todo.replace(grupos[grupo - 1], TACHADO)
    );
  }
  return limpio;
}

// ---------- Minimización ----------
// Igual que ProteccionDatos.kt: la IA no necesita el nombre, el teléfono, el correo ni los
// enlaces para redactar, y la app los quita antes de mandarlos y los repone al volver. Se
// repite aquí para que los prompts siempre reciban lo mismo, y para que el script de pruebas
// mande exactamente lo que manda la app.

const OMITIDO = "[omitido]";
const marcar = (valor) => (String(valor ?? "").trim() ? OMITIDO : "");

function sinContacto(respuestas) {
  return Object.fromEntries(
    Object.entries(respuestas || {}).map(([campo, valor]) => [
      campo,
      ["nombre", "contacto", "enlaces"].includes(campo) ? marcar(valor) : valor,
    ])
  );
}

function ocultarDatos(cv, campos) {
  if (!cv || typeof cv !== "object" || !cv.datos) return cv || {};
  const datos = { ...cv.datos };
  for (const campo of campos) datos[campo] = marcar(datos[campo]);
  return { ...cv, datos };
}

/** El correo y los enlaces sí van a la revisión: juzgarlos es parte de revisar. */
const paraRevisar = (cv) => ocultarDatos(cv, ["telefono"]);
const paraAdaptar = (cv) => ocultarDatos(cv, ["nombre", "telefono", "correo", "linkedin", "portafolio"]);

// ---------- Delimitación ----------
// El texto de la persona y el de la vacante van entre etiquetas que el prompt declara como
// datos. Antes se quitan nuestras etiquetas de adentro, para que una vacante no pueda
// "cerrar" su bloque y hablarle al modelo como si fuera nuestra instrucción.

const ETIQUETAS = /<\/?\s*(respuestas|cv|cv_pegado|vacante)\s*>/gi;
const envolver = (etiqueta, contenido) => `<${etiqueta}>\n${String(contenido).replace(ETIQUETAS, " ")}\n</${etiqueta}>`;

const PAISES = { MX: "México", CO: "Colombia", AR: "Argentina", CL: "Chile", PE: "Perú", EC: "Ecuador" };
const nombrePais = (codigo) => PAISES[codigo] || "Latinoamérica (sin país específico)";

const recortar = (texto, maximo) =>
  texto.length <= maximo ? texto : texto.slice(0, maximo) + "\n[texto recortado]";

// ---------- Mensajes ----------

function mensajesGenerar(respuestas, pais, cvPegado) {
  const limpias = sinContacto(Object.fromEntries(
    Object.entries(respuestas || {}).map(([k, v]) => [k, tachar(recortar(String(v), config.limites.respuesta))])
  ));
  const partes = [
    `País donde busca trabajo: ${nombrePais(pais)}`,
    envolver("respuestas", JSON.stringify(limpias, null, 2)),
  ];
  if (cvPegado) partes.push(envolver("cv_pegado", tachar(cvPegado)));
  return [
    { role: "system", content: leer("generar_cv.txt") },
    { role: "user", content: partes.join("\n\n") },
  ];
}

function mensajesEvaluar(cv) {
  return [
    { role: "system", content: leer("evaluar_cv.txt") },
    { role: "user", content: envolver("cv", JSON.stringify(paraRevisar(cv), null, 2)) },
  ];
}

function mensajesAdaptar(cv, vacante, pais) {
  return [
    { role: "system", content: leer("adaptar_cv.txt") },
    {
      role: "user",
      content: [
        `País donde busca trabajo: ${nombrePais(pais)}`,
        envolver("cv", JSON.stringify(paraAdaptar(cv), null, 2)),
        envolver("vacante", tachar(recortar(String(vacante || ""), config.limites.vacante))),
      ].join("\n\n"),
    },
  ];
}

module.exports = {
  config,
  esquema,
  tachar,
  sinContacto,
  OMITIDO,
  envolver,
  mensajesGenerar,
  mensajesEvaluar,
  mensajesAdaptar,
  MODELOS: config.modelos,
};
