/**
 * Prompts compartidos entre la Cloud Function, el script de pruebas y la app.
 *
 * El texto vive en /prompts/*.txt, un solo lugar que Gradle también empaqueta
 * dentro del APK. Si se duplicaran, al tercer ajuste estarían diciendo cosas
 * distintas y se probaría algo que no es lo que corre en producción.
 */

const fs = require("fs");
const path = require("path");

const DIR = path.resolve(__dirname, "..", "prompts");
const leer = (archivo) => fs.readFileSync(path.join(DIR, archivo), "utf8").trim();

const sistemaGenerar = () => leer("generar_cv.txt");

const entradaGenerar = (respuestas, cvPegado) =>
  [
    `Respuestas de la entrevista:\n${JSON.stringify(respuestas || {}, null, 2)}`,
    cvPegado ? `\nCV o texto que la persona pegó:\n${cvPegado}` : "",
  ].join("");

module.exports = {
  sistemaGenerar,
  entradaGenerar,
  MODELO: "gpt-4o-mini",
};
