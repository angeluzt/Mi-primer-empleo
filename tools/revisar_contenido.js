/**
 * Revisa el contenido antes de publicar. Falla si encuentra algo que rompería la app
 * o que confundiría a quien lee.
 *
 * Uso: node tools/revisar_contenido.js
 */

const fs = require("fs");
const path = require("path");

const DIR = path.resolve(__dirname, "..", "app/src/main/assets/contenido");

const TIPOS_VALIDOS = new Set([
  "parrafo", "titulo", "cita", "lista", "tabla",
  "alerta", "banderas", "accion", "recursos", "comparacion",
]);
const NIVELES_ALERTA = new Set(["peligro", "aviso", "clave", "tip"]);

const errores = [];
const avisos = [];
const accionesVistas = new Map();
const puntosPorAccion = new Map();
let totalPalabras = 0;
let totalCapitulos = 0;
let totalMinutos = 0;

const contar = (t) => (t.match(/[\wáéíóúñÁÉÍÓÚÑ]+/g) || []).length;

function revisarBloque(bloque, donde) {
  if (!TIPOS_VALIDOS.has(bloque.tipo)) {
    errores.push(`${donde}: tipo de bloque desconocido "${bloque.tipo}"`);
    return;
  }

  const textos = [];
  switch (bloque.tipo) {
    case "parrafo":
    case "titulo":
    case "cita":
      textos.push(bloque.texto);
      break;
    case "lista":
      textos.push(...bloque.items);
      if (bloque.items.length === 0) errores.push(`${donde}: lista vacía`);
      break;
    case "tabla": {
      textos.push(...bloque.encabezados, ...bloque.filas.flat());
      const ancho = bloque.encabezados.length;
      bloque.filas.forEach((fila, i) => {
        if (fila.length !== ancho) {
          errores.push(`${donde}: fila ${i + 1} tiene ${fila.length} celdas, se esperaban ${ancho}`);
        }
      });
      break;
    }
    case "alerta":
      textos.push(bloque.titulo, bloque.texto);
      if (!NIVELES_ALERTA.has(bloque.nivel)) {
        errores.push(`${donde}: nivel de alerta inválido "${bloque.nivel}"`);
      }
      break;
    case "banderas":
      textos.push(...bloque.rojas, ...bloque.verdes);
      if (!bloque.rojas.length || !bloque.verdes.length) {
        avisos.push(`${donde}: banderas con un lado vacío`);
      }
      break;
    case "accion": {
      textos.push(bloque.texto);
      if (accionesVistas.has(bloque.accionId)) {
        errores.push(
          `${donde}: accionId duplicado "${bloque.accionId}" (ya está en ${accionesVistas.get(bloque.accionId)})`
        );
      }
      accionesVistas.set(bloque.accionId, donde);
      puntosPorAccion.set(bloque.accionId, bloque.puntos);
      if (!bloque.puntos || bloque.puntos <= 0) {
        errores.push(`${donde}: acción sin puntos`);
      }
      break;
    }
    case "recursos":
      bloque.items.forEach((r) => {
        textos.push(r.nombre, r.descripcion);
        if (!/^https:\/\//.test(r.url)) {
          errores.push(`${donde}: recurso "${r.nombre}" no usa https`);
        }
      });
      break;
    case "comparacion":
      textos.push(
        bloque.titulo,
        ...bloque.izquierda.puntos,
        ...bloque.derecha.puntos
      );
      break;
  }

  textos.filter(Boolean).forEach((t) => {
    totalPalabras += contar(t);
    // Los ** van en pares o Compose los imprime tal cual.
    const asteriscos = (t.match(/\*\*/g) || []).length;
    if (asteriscos % 2 !== 0) {
      errores.push(`${donde}: negritas sin cerrar en "${t.slice(0, 60)}…"`);
    }
    if (/\s{2,}/.test(t.replace(/\n/g, ""))) {
      avisos.push(`${donde}: espacios dobles en "${t.slice(0, 50)}…"`);
    }
    if (t.trim() !== t) {
      avisos.push(`${donde}: texto con espacios al inicio o final`);
    }
  });
}

const indice = JSON.parse(fs.readFileSync(path.join(DIR, "indice.json"), "utf8"));

// La tesis también es contenido y se renderiza con los mismos bloques.
indice.tesis.bloques.forEach((b, i) => revisarBloque(b, `tesis[${i}]`));

const idsModulos = new Set(indice.modulos.map((m) => m.id));

indice.rutas.forEach((ruta) => {
  const enOrden = new Set(ruta.orden);
  idsModulos.forEach((id) => {
    if (!enOrden.has(id)) errores.push(`ruta "${ruta.id}": no incluye el módulo "${id}"`);
  });
  ruta.orden.forEach((id) => {
    if (!idsModulos.has(id)) errores.push(`ruta "${ruta.id}": módulo desconocido "${id}"`);
  });
  if (ruta.orden.length !== new Set(ruta.orden).size) {
    errores.push(`ruta "${ruta.id}": tiene módulos repetidos`);
  }
});

let hayModuloGratis = false;

indice.modulos.forEach((meta) => {
  const ruta = path.join(DIR, meta.archivo);
  if (!fs.existsSync(ruta)) {
    errores.push(`módulo "${meta.id}": falta el archivo ${meta.archivo}`);
    return;
  }
  const modulo = JSON.parse(fs.readFileSync(ruta, "utf8"));

  if (modulo.id !== meta.id) {
    errores.push(`${meta.archivo}: el id "${modulo.id}" no coincide con el índice "${meta.id}"`);
  }
  if (meta.gratis) hayModuloGratis = true;

  const idsCapitulo = new Set();
  let hayCapituloGratis = false;

  modulo.capitulos.forEach((cap) => {
    totalCapitulos++;
    totalMinutos += cap.minutos;
    if (idsCapitulo.has(cap.id)) errores.push(`${meta.id}: capítulo duplicado "${cap.id}"`);
    idsCapitulo.add(cap.id);
    if (cap.gratis) hayCapituloGratis = true;
    if (!cap.minutos || cap.minutos <= 0) errores.push(`${meta.id}/${cap.id}: sin minutos`);
    if (!cap.bloques.length) errores.push(`${meta.id}/${cap.id}: sin bloques`);

    cap.bloques.forEach((b, i) => revisarBloque(b, `${meta.id}/${cap.id}[${i}]`));
  });

  // Regla de negocio: cada módulo de paga debe dejar ver algo real, o el muro ahuyenta.
  if (!meta.gratis && !hayCapituloGratis) {
    errores.push(`módulo "${meta.id}": no tiene ningún capítulo gratis`);
  }
  totalPalabras += contar(modulo.intro);
});

if (!hayModuloGratis) errores.push("ningún módulo es gratis por completo");

// El nivel máximo debe ser alcanzable leyendo y haciendo las acciones.
const PUNTOS_POR_CAPITULO = 2;
const puntosPosibles =
  totalCapitulos * PUNTOS_POR_CAPITULO +
  [...puntosPorAccion.values()].reduce((a, b) => a + b, 0);
const nivelMaximo = Math.max(...indice.niveles.map((n) => n.puntosMinimos));

if (puntosPosibles < nivelMaximo) {
  errores.push(
    `el nivel máximo pide ${nivelMaximo} puntos pero solo existen ${puntosPosibles} en toda la app`
  );
}

console.log("— Revisión de contenido —\n");
console.log(`Módulos:            ${indice.modulos.length}`);
console.log(`Capítulos:          ${totalCapitulos}`);
console.log(`Acciones:           ${accionesVistas.size}`);
console.log(`Palabras:           ${totalPalabras.toLocaleString("es-MX")}`);
console.log(`Lectura:            ${totalMinutos} min (~${Math.round(totalMinutos / 60 * 10) / 10} h)`);
console.log(`Puntos disponibles: ${puntosPosibles} (nivel máximo pide ${nivelMaximo})`);
console.log(`Páginas de libro:   ~${Math.round(totalPalabras / 280)}\n`);

if (avisos.length) {
  console.log(`Avisos (${avisos.length}):`);
  avisos.forEach((a) => console.log("  ·", a));
  console.log();
}

if (errores.length) {
  console.log(`ERRORES (${errores.length}):`);
  errores.forEach((e) => console.log("  ✗", e));
  process.exit(1);
}

console.log("✓ Sin errores.");
