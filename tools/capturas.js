/**
 * Renderiza tools/pantallas.html en Chromium y exporta un PNG por pantalla,
 * más una lámina con todas juntas. Sirve para revisar diseño y para la ficha de Play Store.
 *
 * Uso: node tools/capturas.js
 */

const { chromium } = require("playwright-core");
const path = require("path");
const fs = require("fs");

const PANTALLAS = [
  ["p1", "01-tesis"],
  ["p2", "02-elegir-ruta"],
  ["p3", "03-mi-ruta"],
  ["p4", "04-estafas"],
  ["p5", "05-banderas-empresa"],
  ["p6", "06-muro-de-pago"],
  ["p7", "07-pase-completo"],
  ["p8", "08-cv-con-ia"],
  ["p9", "09-cursos"],
];

(async () => {
  const raiz = path.resolve(__dirname, "..");
  const salida = path.join(raiz, "docs", "capturas");
  fs.mkdirSync(salida, { recursive: true });

  const navegador = await chromium.launch({
    executablePath: process.env.CHROMIUM_PATH || "/opt/pw-browsers/chromium/chrome-linux/chrome",
  });
  const pagina = await navegador.newPage({ deviceScaleFactor: 2 });
  await pagina.goto("file://" + path.join(__dirname, "pantallas.html"));
  await pagina.waitForTimeout(400);

  for (const [id, nombre] of PANTALLAS) {
    const elemento = await pagina.$(`#${id}`);
    if (!elemento) continue;
    await elemento.screenshot({ path: path.join(salida, `${nombre}.png`) });
    console.log("✓", nombre);
  }

  await pagina.setViewportSize({ width: 1348, height: 1800 });
  await pagina.screenshot({
    path: path.join(salida, "00-todas.png"),
    fullPage: true,
  });
  console.log("✓ 00-todas");

  await navegador.close();
})();
