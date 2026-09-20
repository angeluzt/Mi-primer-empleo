#!/usr/bin/env node
/**
 * Prueba el generador de CV en tu máquina, sin desplegar nada y sin Android.
 * Usa los MISMOS prompts que la Cloud Function (backend/prompts.js).
 *
 *   export OPENAI_API_KEY=sk-...
 *   node tools/probar_cv.js                        # con el perfil de ejemplo
 *   node tools/probar_cv.js mis_respuestas.json    # con tus propios datos
 *
 * Deja dos archivos en salida_cv/: el JSON crudo y un HTML para verlo.
 */

const fs = require("fs");
const path = require("path");
const {
  sistemaGenerar,
  entradaGenerar,
  MODELO,
} = require("../backend/prompts");

const CLAVE = process.env.OPENAI_API_KEY;
const SALIDA = path.resolve(__dirname, "..", "salida_cv");

// Perfil típico de quien usa la app: sin experiencia formal, con un proyecto propio.
const RESPUESTAS_EJEMPLO = {
  nombre: "Ana López Ramírez",
  puesto_buscado: "Analista de datos junior",
  ciudad: "Guadalajara",
  telefono: "33 1234 5678",
  correo: "ana.lopez.datos@gmail.com",
  linkedin: "linkedin.com/in/analopezr",
  portafolio: "github.com/analopezr",
  formacion: "Ingeniería Industrial, Universidad de Guadalajara, 2020 a 2025",
  experiencia:
    "Practicante de mejora continua en Manufacturas del Valle, de enero a junio 2025. " +
    "Medí tiempos de una línea de empaque y propuse un reacomodo. El tiempo de ciclo bajó como 12%.",
  proyectos:
    "Le hice un control de inventario en hojas de cálculo a la papelería de mi tío. " +
    "Tenía como 120 productos. El desabasto de los más vendidos pasó de 8 casos al mes a 2. " +
    "Capacité a 3 personas para usarlo. Está en github.com/analopezr/inventario",
  cursos: "Certificado de Análisis de Datos de Google (2025). Excel Avanzado de Microsoft Learn (2025).",
  habilidades: "Excel avanzado, Power BI, SQL básico, Lean Manufacturing",
  idiomas: "Español nativo. Inglés B2, puedo tener una junta.",
};

async function llamar(mensajes) {
  const respuesta = await fetch("https://api.openai.com/v1/chat/completions", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${CLAVE}`,
    },
    body: JSON.stringify({
      model: MODELO,
      messages: mensajes,
      temperature: 0.3,
      response_format: { type: "json_object" },
    }),
  });

  if (!respuesta.ok) {
    throw new Error(`OpenAI respondió ${respuesta.status}: ${await respuesta.text()}`);
  }
  const datos = await respuesta.json();
  return {
    contenido: JSON.parse(datos.choices[0].message.content),
    uso: datos.usage,
  };
}

/** Lo que realmente cuesta generar un CV, para que no sea una sorpresa. */
function costo(uso) {
  if (!uso) return "desconocido";
  // Precios de gpt-4o-mini por millón de tokens. Verifícalos en la página de OpenAI.
  const usd = (uso.prompt_tokens / 1e6) * 0.15 + (uso.completion_tokens / 1e6) * 0.6;
  return `${uso.total_tokens} tokens ≈ $${usd.toFixed(5)} USD`;
}

/** Revisa que la IA no se haya inventado cosas que la persona nunca dijo. */
function auditarInventos(cv, respuestas) {
  const dicho = Object.values(respuestas).join(" ").toLowerCase();
  const sospechas = [];

  const numerosDichos = new Set(dicho.match(/\d+/g) || []);
  const revisar = (texto, donde) => {
    (texto.match(/\d+/g) || []).forEach((n) => {
      // Los años del periodo laboral suelen reescribirse, no son inventos.
      if (!numerosDichos.has(n) && n.length <= 4 && Number(n) > 1 && Number(n) < 1900) {
        sospechas.push(`${donde}: la cifra "${n}" no aparece en tus respuestas`);
      }
    });
  };

  revisar(cv.resumen || "", "resumen");
  (cv.experiencia || []).forEach((e, i) =>
    (e.logros || []).forEach((l) => revisar(l, `experiencia[${i}]`))
  );
  (cv.proyectos || []).forEach((p, i) =>
    (p.logros || []).forEach((l) => revisar(l, `proyectos[${i}]`))
  );
  return sospechas;
}

function aHtml(par) {
  const titulos = {
    es: ["Proyectos", "Experiencia", "Formación", "Certificaciones", "Habilidades", "Idiomas"],
    en: ["Projects", "Experience", "Education", "Certifications", "Skills", "Languages"],
  };

  const bloque = (cv) => {
    const [tProy, tExp, tForm, tCert, tHab, tIdi] =
      titulos[cv.idioma === "en" ? "en" : "es"];
    return `
  <section>
    <h1>${cv.datos.nombre}</h1>
    <p class="puesto">${cv.datos.puesto}</p>
    <p class="contacto">${[cv.datos.ciudad, cv.datos.telefono, cv.datos.correo, cv.datos.linkedin, cv.datos.portafolio]
      .filter(Boolean)
      .join(" · ")}</p>
    <p>${cv.resumen || ""}</p>
    ${seccion(tProy, (cv.proyectos || []).map((p) =>
      `<h3>${p.nombre}</h3><p class="meta">${p.enlace || ""}</p><p>${p.descripcion || ""}</p>${lista(p.logros)}`))}
    ${seccion(tExp, (cv.experiencia || []).map((e) =>
      `<h3>${e.puesto}</h3><p class="meta">${e.organizacion} · ${e.periodo}</p>${lista(e.logros)}`))}
    ${seccion(tForm, (cv.formacion || []).map((f) =>
      `<h3>${f.titulo}</h3><p class="meta">${f.institucion} · ${f.periodo}</p>`))}
    ${seccion(tCert, (cv.certificaciones || []).map((c) =>
      `<p>${c.nombre} — ${c.institucion}, ${c.anio}</p>`))}
    ${seccion(tHab, [(cv.habilidades || []).join(" · ")])}
    ${seccion(tIdi, [(cv.idiomas || []).map((i) => `${i.idioma}: ${i.nivel}`).join(" · ")])}
  </section>`;
  };

  const lista = (items) =>
    items && items.length ? `<ul>${items.map((i) => `<li>${i}</li>`).join("")}</ul>` : "";
  const seccion = (titulo, partes) => {
    const cuerpo = partes.filter((p) => p && p.trim()).join("");
    return cuerpo ? `<h2>${titulo}</h2>${cuerpo}` : "";
  };

  return `<!DOCTYPE html><html lang="es"><head><meta charset="utf-8">
<title>CV generado</title><style>
body{font-family:system-ui,sans-serif;background:#334155;margin:0;padding:30px;display:flex;gap:30px;flex-wrap:wrap;justify-content:center}
section{background:#fff;width:600px;padding:44px;border-radius:6px;box-shadow:0 10px 30px rgba(0,0,0,.35)}
h1{margin:0;font-size:26px;color:#0F172A}
.puesto{margin:4px 0;color:#1D4ED8;font-size:15px}
.contacto{margin:0 0 18px;color:#64748B;font-size:12px}
h2{font-size:12px;text-transform:uppercase;letter-spacing:.08em;color:#1D4ED8;border-bottom:1px solid #CBD5E1;padding-bottom:4px;margin:22px 0 10px}
h3{font-size:14px;margin:12px 0 2px;color:#0F172A}
.meta{margin:0 0 4px;color:#64748B;font-size:12px}
p,li{font-size:13px;line-height:1.55;color:#1E293B}
ul{margin:6px 0;padding-left:18px}
</style></head><body>${bloque(par.es)}${bloque(par.en)}</body></html>`;
}

/** Respuesta enlatada para revisar el HTML y la auditoría sin gastar tokens. */
function simulado() {
  const es = {
    idioma: "es",
    datos: {
      nombre: "Ana López Ramírez",
      puesto: "Analista de Datos Junior",
      ciudad: "Guadalajara",
      telefono: "33 1234 5678",
      correo: "ana.lopez.datos@gmail.com",
      linkedin: "linkedin.com/in/analopezr",
      portafolio: "github.com/analopezr",
    },
    resumen:
      "Ingeniera industrial recién egresada con proyectos propios de análisis de datos. " +
      "Busco mi primera oportunidad en un equipo donde pueda aprender y aportar.",
    proyectos: [
      {
        nombre: "Control de inventario para papelería local",
        descripcion: "Sistema en hojas de cálculo con tablero de rotación de producto.",
        enlace: "github.com/analopezr/inventario",
        logros: [
          "Ordené un catálogo de 120 productos y construí su control de existencias.",
          "Reduje el desabasto de los más vendidos de 8 a 2 casos por mes.",
          "Capacité a 3 personas para operarlo sin apoyo.",
        ],
      },
    ],
    experiencia: [
      {
        puesto: "Practicante de Mejora Continua",
        organizacion: "Manufacturas del Valle",
        periodo: "Ene 2025 – Jun 2025",
        logros: [
          "Medí tiempos de una línea de empaque y propuse un reacomodo.",
          "El tiempo de ciclo bajó alrededor de 12%.",
        ],
      },
    ],
    formacion: [
      {
        titulo: "Ingeniería Industrial",
        institucion: "Universidad de Guadalajara",
        periodo: "2020 – 2025",
        nota: "",
      },
    ],
    certificaciones: [
      { nombre: "Certificado de Análisis de Datos", institucion: "Google", anio: "2025", enlace: "" },
      { nombre: "Excel Avanzado", institucion: "Microsoft Learn", anio: "2025", enlace: "" },
    ],
    habilidades: ["Excel avanzado", "Power BI", "SQL básico", "Lean Manufacturing"],
    idiomas: [
      { idioma: "Español", nivel: "Nativo" },
      { idioma: "Inglés", nivel: "B2 intermedio-alto" },
    ],
  };
  const en = {
    ...es,
    idioma: "en",
    datos: { ...es.datos, puesto: "Junior Data Analyst" },
    resumen:
      "Industrial engineering graduate with self-directed data analysis projects. " +
      "Looking for a first role on a team where I can learn and contribute.",
  };
  return { contenido: { es, en }, uso: null };
}

async function main() {
  const simular = process.argv.includes("--simular");

  if (!CLAVE && !simular) {
    console.error("Falta la llave. Ejecuta:  export OPENAI_API_KEY=sk-...");
    console.error("O prueba el formato de salida sin gastar nada:  node tools/probar_cv.js --simular");
    process.exit(1);
  }

  const archivo = process.argv.find((a) => a.endsWith(".json"));
  const respuestas = archivo
    ? JSON.parse(fs.readFileSync(archivo, "utf8"))
    : RESPUESTAS_EJEMPLO;

  console.log(archivo ? `Usando ${archivo}` : "Usando el perfil de ejemplo");
  console.log("Generando CV en español e inglés…\n");

  const inicio = Date.now();
  const { contenido, uso } = simular
    ? simulado()
    : await llamar([
        { role: "system", content: sistemaGenerar() },
        { role: "user", content: entradaGenerar(respuestas, "") },
      ]);
  const segundos = ((Date.now() - inicio) / 1000).toFixed(1);

  fs.mkdirSync(SALIDA, { recursive: true });
  fs.writeFileSync(path.join(SALIDA, "cv.json"), JSON.stringify(contenido, null, 2));
  fs.writeFileSync(path.join(SALIDA, "cv.html"), aHtml(contenido));

  console.log(`Listo en ${segundos}s · ${costo(uso)}`);
  console.log(`  salida_cv/cv.json`);
  console.log(`  salida_cv/cv.html   ← ábrelo en el navegador\n`);

  const sospechas = auditarInventos(contenido.es, respuestas);
  if (sospechas.length) {
    console.log("⚠  Cifras que podrían ser inventadas (revísalas):");
    sospechas.forEach((s) => console.log("   ·", s));
  } else {
    console.log("✓ No se detectaron cifras inventadas.");
  }
}

main().catch((e) => {
  console.error("\nFalló:", e.message);
  process.exit(1);
});
